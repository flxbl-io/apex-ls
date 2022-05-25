/*
 Copyright (c) 2019 Kevin Jones, All rights reserved.
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
package com.nawforce.apexlink.types.schema

import com.nawforce.apexlink.finding.TypeResolver
import com.nawforce.apexlink.names.TypeNames
import com.nawforce.apexlink.names.TypeNames._
import com.nawforce.apexlink.org.OPM
import com.nawforce.apexlink.types.core.{FieldDeclaration, TypeDeclaration, TypeId}
import com.nawforce.apexlink.types.platform.PlatformField
import com.nawforce.apexlink.types.synthetic.{CustomField, CustomFieldDeclaration}
import com.nawforce.pkgforce.names._

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

/* SObject field finding support. This is separated so it can be used by TypeDeclarations that can be used to
 * represent SObjects, the current cases being PlatformTypeDeclaration for standard objects and SObjectDeclarations
 * used for Custom SObjects and Standard SObjects when they are loaded into modules. It provides the handling needed
 * to support reflection of the fields that is unique to SObjects.
 * FUTURE: Remove this as part of refactor work on TypeDeclarations
 */
trait SObjectFieldFinder {
  this: TypeDeclaration =>

  val fields: ArraySeq[FieldDeclaration]
  private var relationshipFields: mutable.Map[Name, FieldDeclaration] = _

  def validate(withRelationshipCollection: Boolean): Unit

  def collectRelationshipFields(dependencyHolders: Set[TypeId]): Unit = {
    // Find SObject relationship fields that reference this one via dependency analysis
    val incomingObjects = this +: dependencyHolders.toSeq
      .flatMap(typeId => TypeResolver(typeId.typeName, typeId.module).toOption)
      .collect { case sobject: SObjectFieldFinder => sobject }
    val incomingObjectAndField: Seq[(TypeDeclaration, CustomField)] =
      incomingObjects.flatMap(sobject => {
        sobject.fields
          .collect { case field: CustomField => field }
          .filter(
            field =>
              field.relationshipName.nonEmpty &&
                field.typeName == typeName &&
                field.name.value
                  .endsWith("__r")
          )
          .map(field => (sobject.asInstanceOf[TypeDeclaration], field))
      })

    relationshipFields = this.moduleDeclaration
      .flatMap(_.nextModule)
      .flatMap(nextModule => {
        TypeResolver(typeName, nextModule).toOption
          .collect { case sobject: SObjectFieldFinder => sobject }
          .flatMap(sobject => Option(sobject.relationshipFields))
      })
      .getOrElse(mutable.Map())

    incomingObjectAndField.foreach(incoming => {
      val relationshipName = Name(incoming._2.relationshipName.get + "__r")
      val ns               = this.moduleDeclaration.flatMap(_.namespace)
      val targetFieldName =
        EncodedName(relationshipName).defaultNamespace(ns).fullName
      relationshipFields.put(
        targetFieldName,
        CustomFieldDeclaration(targetFieldName, TypeNames.recordSetOf(incoming._1.typeName), None)
      )
    })
  }

  def findSObjectField(name: Name, staticContext: Option[Boolean]): Option[FieldDeclaration] = {
    findFieldSObject(name, staticContext, this.fieldsByName.toMap)
      .orElse {
        Option(relationshipFields).flatMap(
          fields => findFieldSObject(name, staticContext, fields.toMap)
        )
      }
  }

  private def findFieldSObject(
    name: Name,
    staticContext: Option[Boolean],
    sObjectFields: Map[Name, FieldDeclaration]
  ): Option[FieldDeclaration] = {
    val fieldOption = sObjectFields.get(name)

    // Handle the synthetic static SObjectField or abort
    if (fieldOption.isEmpty) {
      if (name == Names.SObjectField && staticContext.contains(true))
        Some(CustomFieldDeclaration(Names.SObjectField, TypeNames.sObjectFields$(typeName), None))
      else
        None
    } else {
      val field = fieldOption.get
      if (staticContext.contains(field.isStatic)) {
        // Found a matching field
        fieldOption
      } else if (staticContext.contains(true)) {
        // Create an SObjectField version of this field
        val shareTypeName = if (typeName.isShare) Some(typeName) else None
        Some(getSObjectField(field, shareTypeName, moduleDeclaration, sObjectFields))
      } else {
        None
      }
    }
  }

  /** Construct a custom field declaration for the SObjectField of the passed field. If the field is for another
    * SObject then we use a generic to carry the SObject type and also make sure that it is loaded.
    */
  private def getSObjectField(
    field: FieldDeclaration,
    shareTypeName: Option[TypeName],
    module: Option[OPM.Module],
    sObjectFields: Map[Name, FieldDeclaration]
  ): FieldDeclaration = {
    field match {
      /* Relationship 'Id' fields can be used in place of the actual relationship field as must be typed as such */
      case field: PlatformField if isRelationshipField(field) =>
        val relationshipField =
          findFieldSObject(Name(field.name.value.dropRight(2)), Some(true), sObjectFields)
        relationshipField match {
          case Some(
                CustomFieldDeclaration(
                  _,
                  TypeName(Names.SObjectFields$, Seq(sObject), Some(TypeNames.Internal)),
                  _,
                  _
                )
              ) =>
            CustomFieldDeclaration(
              field.name,
              TypeNames.sObjectFields$(sObject),
              None,
              asStatic = true
            )
          case _ => field
        }

      case _ =>
        field.getSObjectStaticField(shareTypeName, module)
    }
  }

  /** As general rule relationship fields are typed differently and can be recognised by having an Id suffix on the
    * name, of course there has to be exceptions... *
    */
  private def isRelationshipField(field: PlatformField): Boolean = {
    if (field.name.value.endsWith("Id") && field.name.value.length > 2) {
      !SObjectFieldFinder.nonRelationshipIdFields.contains((typeName, field.name))
    } else {
      false
    }
  }
}

object SObjectFieldFinder {
  val nonRelationshipIdFields: Set[(TypeName, Name)] =
    Set((TypeName(Name("Opportunity"), Nil, Some(TypeNames.Schema)), Name("ContractId")))
}
