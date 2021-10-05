/*
 Copyright (c) 2021 Kevin Jones & FinancialForce, All rights reserved.
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.
 */
package com.nawforce.apexlink.org

import com.nawforce.apexlink.cst.ClassDeclaration
import com.nawforce.apexlink.finding.TypeResolver
import com.nawforce.apexlink.names.TypeNames._
import com.nawforce.apexlink.names.{TypeNames, XNames}
import com.nawforce.apexlink.types.core.TypeDeclaration
import com.nawforce.apexlink.types.extended.GenericTypeDeclaration
import com.nawforce.pkgforce.names.{Name, Names, TypeName}

import scala.collection.mutable

trait GenericTypeFactory {
  this: Module =>

  private var creating = mutable.Set[TypeName]()

  def getOrCreateExtendedGeneric(typeName: TypeName, from: TypeDeclaration, genericBase: ClassDeclaration): Option[TypeDeclaration] = {
    if (creating.contains(typeName)) {
      None
    } else {
      try {
        creating.add(typeName)
        val typeArgs = constructTypeNames(typeName.name.value.split('_').tail.map(Name(_)).toList, from, genericBase.typeArguments.size)
        if (typeArgs._1.isEmpty && typeArgs._2.size == genericBase.typeArguments.length) {
          Some(new GenericTypeDeclaration(this, genericBase.typeName.withParams(typeArgs._2), genericBase))
        } else {
          None
        }
      } finally {
        creating.remove(typeName)
      }
    }
  }

  /** Construct a TypeName from some args using a rather specific model for resolving. */
  private def constructTypeName(typeArgs: List[Name], from: TypeDeclaration): Option[(List[Name], TypeName)] = {
    if (typeArgs.nonEmpty) {
      return constructNoneGeneric(typeArgs, from)
        .orElse(constructExtendedGeneric(typeArgs, from))
        .orElse(constructPlatformGeneric(typeArgs, from))
    }
    None
  }

  private def constructNoneGeneric(typeArgs: List[Name], from: TypeDeclaration): Option[(List[Name], TypeName)] = {
    val typeName = constructTypeName(typeArgs.head)
    if (typeName.outer.isEmpty && namespace.nonEmpty) {
      val td = TypeResolver(typeName.withNamespace(namespace), from).toOption
      if (td.nonEmpty && asCompanion(td.get).isEmpty)
        return Some((typeArgs.tail, td.get.typeName))
    }

    val td = TypeResolver(typeName, from).toOption
    if (td.nonEmpty && asCompanion(td.get).isEmpty)
      return Some((typeArgs.tail, td.get.typeName))

    None
  }

  private def constructExtendedGeneric(typeArgs: List[Name], from: TypeDeclaration): Option[(List[Name], TypeName)] = {
    var typeName = constructTypeName(typeArgs.head)
    if (typeName.outer.isEmpty) {
      // Add package namespace if not explicit
      typeName = typeName.withNamespace(namespace)
    }

    val td = findPackageType(typeName, Some(from))
    val companion = td.flatMap(asCompanion)
    if (td.nonEmpty && companion.nonEmpty) {
      val requiredArgs = companion.get.typeArguments.length
      val params = constructTypeNames(typeArgs.tail, from, requiredArgs)
      if (params._2.length == requiredArgs)
        return Some((params._1, td.get.typeName.withParams(params._2)))
    }
    None
  }

  private def constructPlatformGeneric(typeArgs: List[Name], from: TypeDeclaration): Option[(List[Name], TypeName)] = {

    var typeName = constructTypeName(typeArgs.head)
    if (typeName.outer.isEmpty) {
      // Non-namespace can only be for a System generic
      typeName = typeName.withOuter(Some(TypeNames.System))
    }

    GenericTypeFactory.platformGenerics.get(typeName.asDotName.names).flatMap(requiredArgs => {
      val params = constructTypeNames(typeArgs.tail, from, requiredArgs)
      if (params._2.length == requiredArgs)
        Some((params._1, typeName.withParams(params._2)))
      else
        None
    })
  }

  /** Construct a set number of TypeNames form some args, may not be possible, returns any unused type args. */
  private def constructTypeNames(typeArgs: List[Name], from: TypeDeclaration, count: Int): (List[Name], List[TypeName]) = {
    if (count == 0) {
      (typeArgs, List())
    } else {
      constructTypeName(typeArgs, from) match {
        case Some((residual, typeName)) =>
          val rest = constructTypeNames(residual, from, count - 1)
          (rest._1, typeName :: rest._2)
        case None =>
          (typeArgs, List())
      }
    }
  }

  private def constructTypeName(typeArg: Name): TypeName = {
    namespaces.find(ns => typeArg.value.matches(s"(?i)${ns.value}.+"))
      .map(ns => TypeName(Name(typeArg.value.substring(ns.value.length)), Seq(), Some(TypeName(ns))))
      .getOrElse(TypeName(typeArg))
  }

  /** Are we dealing with an Extended Apex companion type declaration. */
  private def asCompanion(td: TypeDeclaration): Option[ClassDeclaration] = {
    td match {
      case cd: ClassDeclaration if cd.typeArguments.nonEmpty => Some(cd)
      case _ => None
    }
  }
}

object GenericTypeFactory {
  val platformGenerics = Map(
    Seq(Names.System, XNames.List$) -> 1,
    Seq(Names.System, XNames.Iterator) -> 1,
    Seq(Names.System, XNames.Map$) -> 2,
    Seq(Names.System, XNames.Set$) -> 1,
    Seq(Names.System, XNames.Iterable) -> 1,
    Seq(Names.Database, XNames.Batchable) -> 1,
  )
}