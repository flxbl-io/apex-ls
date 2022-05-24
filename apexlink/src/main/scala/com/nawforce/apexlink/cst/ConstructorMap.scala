/*
 * Copyright (c) 2022 FinancialForce.com, inc. All rights reserved
 */

package com.nawforce.apexlink.cst
import com.nawforce.apexlink.cst.AssignableSupport.isAssignable
import com.nawforce.apexlink.finding.{RelativeTypeContext, RelativeTypeName}
import com.nawforce.apexlink.names.TypeNames
import com.nawforce.apexlink.types.apex.{
  ApexClassDeclaration,
  ApexConstructorLike,
  ApexDeclaration,
  FullDeclaration
}
import com.nawforce.apexlink.types.core.{ConstructorDeclaration, TypeDeclaration}
import com.nawforce.pkgforce.diagnostics.Duplicates.IterableOps
import com.nawforce.pkgforce.diagnostics.{Diagnostic, ERROR_CATEGORY, Issue}
import com.nawforce.pkgforce.modifiers._
import com.nawforce.pkgforce.names.{Name, TypeName}
import com.nawforce.pkgforce.path.PathLocation

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

final case class ConstructorMap(
  typeName: Option[TypeName],
  td: Option[ApexClassDeclaration],
  constructorsByParam: Map[Int, Array[ConstructorDeclaration]],
  superConstructorsByParam: Option[ConstructorMap],
  errors: List[Issue]
) {

  def allConstructors: ArraySeq[ConstructorDeclaration] = {
    val buffer = new mutable.ArrayBuffer[ConstructorDeclaration]()
    constructorsByParam.values.foreach(ctor => buffer.addAll(ctor))
    ArraySeq.unsafeWrapArray(buffer.toArray)
  }

  def findConstructorByParams(
    params: ArraySeq[TypeName],
    context: VerifyContext
  ): Either[String, ConstructorDeclaration] = {
    val matched = constructorsByParam
      .get(params.length)

    if (matched.isEmpty)
      return Left(s"No constructor defined with ${params.length} arguments")

    findPotentialMatch(matched.get, params, context)

  }

  private def findPotentialMatch(
    matches: Array[ConstructorDeclaration],
    params: ArraySeq[TypeName],
    context: VerifyContext
  ): Either[String, ConstructorDeclaration] = {
    def getCtorString: String = {
      typeName match {
        case Some(name) =>
          s"$name.<constructor>(${params.mkString(",")})"
        case None => s"<constructor>(${params.mkString(",")})"
      }
    }

    val potential = findMostSpecificMatch(strict = true, matches, params, context)
      .orElse(findMostSpecificMatch(strict = false, matches, params, context))

    potential match {
      case Some(Right(ctor)) =>
        if (canAccessCtor(ctor, context)) {
          Right(ctor)
        } else {
          //Check the rest of assignable for accessible ctors, if not return the original error
          findPotentialMatch(matches.filterNot(_ == ctor), params, context) match {
            case Right(ctor) => Right(ctor)
            case _           => Left(s"Constructor is not visible: $getCtorString")
          }
        }
      case Some(Left(error)) => Left(s"$error: $getCtorString")
      case None              => Left(s"Constructor not defined: $getCtorString")
    }
  }

  private def findMostSpecificMatch(
    strict: Boolean,
    matches: Array[ConstructorDeclaration],
    params: ArraySeq[TypeName],
    context: VerifyContext
  ): Option[Either[String, ConstructorDeclaration]] = {
    val assignable = matches.filter(c => {
      val argZip = c.parameters.map(_.typeName).zip(params)
      argZip.forall(argPair => isAssignable(argPair._1, argPair._2, strict, context))
    })
    if (assignable.isEmpty)
      None
    else if (assignable.length == 1)
      Some(Right(assignable.head))
    else {
      Some(
        assignable
          .find(
            ctor =>
              assignable.forall(
                c =>
                  c == ctor || ctor
                    .hasMoreSpecificParams(c.parameters, params, context)
                    .contains(true)
              )
          )
          .map(Right(_))
          .getOrElse(Left("Ambiguous constructor call"))
      )
    }
  }

  private def areInSameApexFile(
    ctor: ConstructorDeclaration,
    calledFrom: TypeDeclaration
  ): Boolean = {
    (ctor, calledFrom) match {
      case (acl: ApexConstructorLike, ad: ApexDeclaration) => ad.location.path == acl.location.path
      case _                                               => false
    }
  }

  private def canAccessCtor(ctor: ConstructorDeclaration, context: VerifyContext): Boolean = {
    lazy val isTestVisible           = ctor.isTestVisible && context.thisType.inTest
    lazy val isAccessedInThisContext = areInSameApexFile(ctor, context.thisType)
    lazy val isAccessedInSuperContext =
      context.superType.nonEmpty && areInSameApexFile(ctor, context.superType.get)

    ctor.visibility match {
      case PUBLIC_MODIFIER => true
      case GLOBAL_MODIFIER => true
      case PROTECTED_MODIFIER =>
        isAccessedInThisContext || isAccessedInSuperContext || isTestVisible
      case PRIVATE_MODIFIER => isAccessedInThisContext || isTestVisible
      case _                => false
    }
  }

}

object ConstructorMap {
  private val emptyIssues: ArraySeq[Issue]           = ArraySeq.empty
  private val emptyParams: ArraySeq[FormalParameter] = ArraySeq.empty
  private val publicModifierResult                   = new ModifierResults(ArraySeq(PUBLIC_MODIFIER), emptyIssues)

  type WorkingMap = mutable.HashMap[Int, List[ConstructorDeclaration]]

  def apply(td: TypeDeclaration): ConstructorMap = {
    val workingMap = new WorkingMap()
    td.constructors.foreach(ctor => {
      val key = ctor.parameters.length
      workingMap.put(key, ctor :: workingMap.getOrElse(key, Nil))
    })
    new ConstructorMap(Some(td.typeName), None, toMap(workingMap), None, Nil)
  }

  def apply(
    td: TypeDeclaration,
    location: Option[PathLocation],
    ctors: ArraySeq[ConstructorDeclaration],
    superClassMap: ConstructorMap
  ): ConstructorMap = {
    val workingMap = new WorkingMap()
    val errors     = mutable.Buffer[Issue]()

    val deduped = deDupeConstructors(ctors, errors)

    deduped.foreach(ctor => {
      val key                      = ctor.parameters.length
      val ctorsWithSameParamLength = workingMap.getOrElse(key, Nil)
      val platformGenericDupes =
        ctorsWithSameParamLength.find(
          x => x.hasSameParameters(ctor, allowPlatformGenericEquivalence = true)
        )
      if (platformGenericDupes.nonEmpty)
        setConstructorDuplicateError(ctor, platformGenericDupes.head, errors)
      workingMap.put(key, ctor :: ctorsWithSameParamLength)
    })

    applySyntheticsCtors(td, workingMap, superClassMap, errors)

    td match {
      case td: ApexClassDeclaration =>
        new ConstructorMap(
          Some(td.typeName),
          Some(td),
          toMap(workingMap),
          Some(superClassMap),
          errors.toList
        )
      case td: TypeDeclaration =>
        new ConstructorMap(
          Some(td.typeName),
          None,
          toMap(workingMap),
          Some(superClassMap),
          errors.toList
        )
    }
  }

  def empty: ConstructorMap = {
    new ConstructorMap(None, None, Map(), None, Nil)
  }

  private def applySyntheticsCtors(
    td: TypeDeclaration,
    workingMap: WorkingMap,
    superClassMap: ConstructorMap,
    errors: mutable.Buffer[Issue]
  ): Unit = {
    if (td.isCustomException) applyCustomExceptionsCtors(td, workingMap)
    else applyDefaultCtor(td, workingMap, superClassMap, errors)
  }

  private def applyDefaultCtor(
    td: TypeDeclaration,
    workingMap: WorkingMap,
    superClassMap: ConstructorMap,
    errors: mutable.Buffer[Issue]
  ) = {
    if (workingMap.keys.isEmpty) {
      td match {
        case fd: FullDeclaration =>
          if (superClassMap.td.isEmpty)
            workingMap.put(0, List(toCtor(fd)))
          else
            superClassMap
              .findConstructorByParams(
                ArraySeq.empty,
                new TypeVerifyContext(None, fd, None)
              ) match {
              case Left(error) =>
                val msg =
                  if (superClassMap.constructorsByParam.contains(0)) error
                  else
                    s"No default constructor available in super type: ${superClassMap.typeName.get}"
                setClassError(td, errors, msg)
              case _ => workingMap.put(0, List(toCtor(fd)))
            }
        case _ =>
      }
    }
  }

  private def applyCustomExceptionsCtors(td: TypeDeclaration, workingMap: WorkingMap): Unit = {
    def toParam(id: String, typeCntxt: RelativeTypeContext, typeName: TypeName): FormalParameter = {
      FormalParameter(publicModifierResult, RelativeTypeName(typeCntxt, typeName), Id(Name(id)))
    }
    val synthetics = td match {
      case fd: FullDeclaration =>
        ArraySeq(
          toCtor(fd),
          toCtor(fd, ArraySeq(toParam("param1", fd.typeContext, TypeNames.String))),
          toCtor(fd, ArraySeq(toParam("param1", fd.typeContext, TypeNames.Exception))),
          toCtor(
            fd,
            ArraySeq(
              toParam("param1", fd.typeContext, TypeNames.String),
              toParam("param2", fd.typeContext, TypeNames.Exception)
            )
          )
        )
      case _ => ArraySeq.empty
    }

    synthetics.foreach(s => {
      val key = s.parameters.length
      workingMap.put(key, s :: workingMap.getOrElse(key, Nil))
    })

  }

  private def deDupeConstructors(
    ctors: ArraySeq[ConstructorDeclaration],
    errors: mutable.Buffer[Issue]
  ): ArraySeq[ConstructorDeclaration] = {
    val dupes = ctors.duplicates(_.parameters.map(_.typeName.toString()).mkString(","))
    dupes.foreach(duplicates => {
      duplicates._2.foreach(dup => {
        setConstructorDuplicateError(dup, duplicates._1, errors)
      })
    })
    ctors.filterNot(dupes.values.flatten.toSeq.contains)
  }

  private def setConstructorDuplicateError(
    constructor: ConstructorDeclaration,
    duplicateOf: ConstructorDeclaration,
    errors: mutable.Buffer[Issue]
  ): Unit = {
    (constructor, duplicateOf) match {
      case (ac: ApexConstructorLike, dup: ApexConstructorLike) =>
        errors.append(
          new Issue(
            ac.location.path,
            Diagnostic(
              ERROR_CATEGORY,
              ac.idLocation,
              s"Constructor is a duplicate of an earlier constructor at ${dup.idLocation.displayPosition}"
            )
          )
        )
      case _ =>
    }
  }

  private def setClassError(td: TypeDeclaration, errors: mutable.Buffer[Issue], message: String) = {
    td match {
      case ad: ApexDeclaration =>
        errors.append(
          new Issue(ad.location.path, Diagnostic(ERROR_CATEGORY, ad.idLocation, message))
        )
      case _ =>
    }
  }

  private def toCtor(
    fd: FullDeclaration,
    params: ArraySeq[FormalParameter] = emptyParams
  ): ApexConstructorDeclaration = {
    lazy val qNames = fd.outerTypeName.map(x => List(x.name)).getOrElse(Nil) ++ List(fd.name)
    val location    = fd.id.location
    ApexConstructorDeclaration(
      publicModifierResult,
      QualifiedName(qNames).withLocation(location),
      params,
      fd.inTest,
      EagerBlock.empty
    ).withLocation(location)
  }

  private def toMap(workingMap: WorkingMap): Map[Int, Array[ConstructorDeclaration]] = {
    workingMap.map(kv => (kv._1, kv._2.toArray)).toMap
  }
}
