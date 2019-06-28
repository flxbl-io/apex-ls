/*
 [The "BSD licence"]
 Copyright (c) 2019 Kevin Jones
 All rights reserved.

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

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.nawforce.types

import java.lang.reflect.{Modifier => JavaModifier}

import com.nawforce.api.Org
import com.nawforce.cst.ConstructContext
import com.nawforce.documents.TextRange
import com.nawforce.parsers.ApexParser.{AnnotationContext, IdContext, ModifierContext, PropertyBlockContext}

sealed abstract class Modifier(val name: String, val order: Integer=0) {
  override def toString: String = name
}

case object WEBSERVICE_MODIFIER extends Modifier("webservice", order = 4)
case object GLOBAL_MODIFIER extends Modifier("global", order = 3)
case object PUBLIC_MODIFIER extends Modifier("public", order = 2)
case object PROTECTED_MODIFIER extends Modifier("protected", order = 1)
case object PRIVATE_MODIFIER extends Modifier("private", order = 0)
case object TEST_CLASS_MODIFIER extends Modifier("@isTest")

case object STATIC_MODIFIER extends Modifier("static")
case object ABSTRACT_MODIFIER extends Modifier("abstract")
case object FINAL_MODIFIER extends Modifier("final")
case object OVERRIDE_MODIFIER extends Modifier("override")
case object VIRTUAL_MODIFIER extends Modifier("virtual")
case object TRANSIENT_MODIFIER extends Modifier("transient")

case object WITH_SHARING_MODIFIER extends Modifier("with sharing")
case object WITHOUT_SHARING_MODIFIER extends Modifier("without sharing")
case object INHERITED_SHARING_MODIFIER extends Modifier("inherited sharing")

case object AURA_ENABLED_ANNOTATION extends Modifier("@AuraEnabled")
case object DEPRECATED_ANNOTATION extends Modifier("@Deprecated")
case object FUTURE_ANNOTATION extends Modifier("@Future")
case object INVOCABLE_METHOD_ANNOTATION extends Modifier("@InvocableMethod")
case object INVOCABLE_VARIABLE_ANNOTATION extends Modifier("@InvocableField")
case object ISTEST_ANNOTATION extends Modifier("@IsTest")
case object READ_ONLY_ANNOTATION extends Modifier("@ReadOnly")
case object REMOTE_ACTION_ANNOTATION extends Modifier("@RemoteAction")
case object SUPPRESS_WARNINGS_ANNOTATION extends Modifier("@SuppressWarnings")
case object TEST_SETUP_ANNOTATION extends Modifier("@TestSetup")
case object TEST_VISIBLE_ANNOTATION extends Modifier("@TestVisible")
case object NAMESPACE_ACCESSIBLE_ANNOTATION extends Modifier("@NamespaceAccessible")

case object REST_RESOURCE_ANNOTATION extends Modifier("@RestResource")
case object HTTP_DELETE_ANNOTATION extends Modifier("@HttpDelete")
case object HTTP_GET_ANNOTATION extends Modifier("@HttpGet")
case object HTTP_PATCH_ANNOTATION extends Modifier("@HttpPatch")
case object HTTP_POST_ANNOTATION extends Modifier("@HttpPost")
case object HTTP_PUT_ANNOTATION extends Modifier("@HttpPut")

// TODO: Validate arguments of the annotations
// TODO: Cross modifier/annotation checking

object ApexModifiers {
  val allVisibilityModifiers: Seq[Modifier] = Seq(WEBSERVICE_MODIFIER, GLOBAL_MODIFIER, PUBLIC_MODIFIER, PROTECTED_MODIFIER, PRIVATE_MODIFIER)
  private val typeVisibilityModifiers: Seq[Modifier] = Seq(GLOBAL_MODIFIER, PUBLIC_MODIFIER, PRIVATE_MODIFIER)
  private val sharingModifiers: Seq[Modifier] = Seq(WITH_SHARING_MODIFIER, WITHOUT_SHARING_MODIFIER, INHERITED_SHARING_MODIFIER)

  def classModifiers(modifierContexts: Seq[ModifierContext], context: ConstructContext,
                     outer: Boolean, idContext: IdContext)
    : Seq[Modifier] = {

    val mods = modifierContexts.flatMap(modifierContext =>
      if (modifierContext.annotation() != null) {
        classAnnotation(modifierContext.annotation())
      } else {
        modifierContext.getText.toLowerCase match {
          case "global" => Some(GLOBAL_MODIFIER)
          case "public" => Some(PUBLIC_MODIFIER)
          case "private" => Some(PRIVATE_MODIFIER)
          case "withsharing" => Some(WITH_SHARING_MODIFIER)
          case "withoutsharing" => Some(WITHOUT_SHARING_MODIFIER)
          case "inheritedsharing" => Some(INHERITED_SHARING_MODIFIER)
          case "abstract" => Some(ABSTRACT_MODIFIER)
          case "virtual" => Some(VIRTUAL_MODIFIER)
          case _ =>
            Org.logMessage(TextRange(modifierContext),
                s"Modifier '${modifierContext.getText}' is not supported on classes")
            None
        }
      }
    )

    if (mods.size == modifierContexts.size) {
      val duplicates = mods.groupBy(identity).collect { case (_, List(_, y, _*)) => y }
      if (duplicates.nonEmpty) {
        Org.logMessage(TextRange(idContext), s"Modifier '${duplicates.head.toString}' is used more than once")
        mods.toSet.toSeq
      } else if (outer && !mods.contains(ISTEST_ANNOTATION) && mods.contains(PRIVATE_MODIFIER) ) {
        Org.logMessage(TextRange(idContext),
          s"Private modifier is not allowed on outer classes")
        mods.filterNot(_ == PRIVATE_MODIFIER)
      } else if (outer && !mods.contains(ISTEST_ANNOTATION) && !(mods.contains(GLOBAL_MODIFIER) || mods.contains(PUBLIC_MODIFIER))) {
        Org.logMessage(TextRange(idContext), s"Outer classes must be declared either 'global' or 'public'")
        PUBLIC_MODIFIER +: mods
      } else if (mods.intersect(typeVisibilityModifiers).size > 1) {
        Org.logMessage(TextRange(idContext),
          s"Only one visibility modifier from 'global', 'public' & 'private' may be used on classes")
        PUBLIC_MODIFIER +: mods.diff(typeVisibilityModifiers)
      } else if (mods.intersect(sharingModifiers).size > 1) {
        Org.logMessage(TextRange(idContext),
          s"Only one sharing modifier from 'with sharing', 'without sharing' & 'inherited sharing' may be used on classes")
        mods.diff(sharingModifiers)
      } else {
        mods
      }
    } else {
      mods
    }
  }

  def interfaceModifiers(modifierContexts: Seq[ModifierContext], context: ConstructContext,
                     outer: Boolean, idContext: IdContext)
  : Seq[Modifier] = {

    val mods = modifierContexts.flatMap(modifierContext =>
      if (modifierContext.annotation() != null) {
        typeAnnotation(modifierContext.annotation())
      } else {
        modifierContext.getText.toLowerCase match {
          case "global" => Some(GLOBAL_MODIFIER)
          case "public" => Some(PUBLIC_MODIFIER)
          case "private" => Some(PRIVATE_MODIFIER)
          case "virtual" => Some(VIRTUAL_MODIFIER)
          case _ =>
            Org.logMessage(TextRange(modifierContext),
              s"Modifier '${modifierContext.getText}' is not supported on interfaces")
            None
        }
      }
    )

    if (mods.size == modifierContexts.size) {
      val duplicates = mods.groupBy(identity).collect { case (_, List(_, y, _*)) => y }
      if (duplicates.nonEmpty) {
        Org.logMessage(TextRange(idContext), s"Modifier '${duplicates.head.toString}' is used more than once")
        mods.toSet.toSeq
      } else if (outer && mods.contains(PRIVATE_MODIFIER)) {
        Org.logMessage(TextRange(idContext),
          s"Private modifier is not allowed on outer interfaces")
        mods.filterNot(_ == PRIVATE_MODIFIER)
      } else if (outer && !(mods.contains(GLOBAL_MODIFIER) || mods.contains(PUBLIC_MODIFIER))) {
        Org.logMessage(TextRange(idContext), s"Outer interfaces must be declared either 'global' or 'public'")
        PUBLIC_MODIFIER +: mods
      } else if (mods.intersect(typeVisibilityModifiers).size > 1) {
        Org.logMessage(TextRange(idContext),
          s"Only one visibility modifier from 'global', 'public' & 'private' may be used on interfaces")
        PUBLIC_MODIFIER +: mods.diff(typeVisibilityModifiers)
      } else {
        mods
      }
    } else {
      mods
    }
  }

  def enumModifiers(modifierContexts: Seq[ModifierContext], context: ConstructContext,
                         outer: Boolean, idContext: IdContext)
  : Seq[Modifier] = {

    val mods = modifierContexts.flatMap(modifierContext =>
      if (modifierContext.annotation() != null) {
        typeAnnotation(modifierContext.annotation())
      } else {
        modifierContext.getText.toLowerCase match {
          case "global" => Some(GLOBAL_MODIFIER)
          case "public" => Some(PUBLIC_MODIFIER)
          case "private" => Some(PRIVATE_MODIFIER)
          case _ =>
            Org.logMessage(TextRange(modifierContext),
              s"Modifier '${modifierContext.getText}' is not supported on enums")
            None
        }
      }
    )

    if (mods.size == modifierContexts.size) {
      val duplicates = mods.groupBy(identity).collect { case (_, List(_, y, _*)) => y }
      if (duplicates.nonEmpty) {
        Org.logMessage(TextRange(idContext), s"Modifier '${duplicates.head.toString}' is used more than once")
        mods.toSet.toSeq
      } else if (outer && mods.contains(PRIVATE_MODIFIER)) {
        Org.logMessage(TextRange(idContext),
          s"Private modifier is not allowed on outer enums")
        mods.filterNot(_ == PRIVATE_MODIFIER)
      } else if (outer && !(mods.contains(GLOBAL_MODIFIER) || mods.contains(PUBLIC_MODIFIER))) {
        Org.logMessage(TextRange(idContext), s"Outer enums must be declared either 'global' or 'public'")
        PUBLIC_MODIFIER +: mods
      } else if (mods.intersect(typeVisibilityModifiers).size > 1) {
        Org.logMessage(TextRange(idContext),
          s"Only one visibility modifier from 'global', 'public' & 'private' may be used on enums")
        PUBLIC_MODIFIER +: mods.diff(typeVisibilityModifiers)
      } else {
        mods
      }
    } else {
      mods
    }
  }

  def fieldModifiers(modifierContexts: Seq[ModifierContext], context: ConstructContext, idContext: IdContext)
    : Seq[Modifier] = {

    val mods = modifierContexts.flatMap(modifierContext =>
      if (modifierContext.annotation() != null) {
        fieldAnnotation(modifierContext.annotation())
      } else {
        modifierContext.getText.toLowerCase match {
          case "global" => Some(GLOBAL_MODIFIER)
          case "public" => Some(PUBLIC_MODIFIER)
          case "protected" => Some(PROTECTED_MODIFIER)
          case "private" => Some(PRIVATE_MODIFIER)
          case "final" => Some(FINAL_MODIFIER)
          case "static" => Some(STATIC_MODIFIER)
          case "transient" => Some(TRANSIENT_MODIFIER)
          case "webservice" => Some(WEBSERVICE_MODIFIER)
          case _ =>
            Org.logMessage(TextRange(modifierContext),
              s"Modifier '${modifierContext.getText}' is not supported on fields")
            None
        }
      }
    )

    val duplicates = mods.groupBy(identity).collect { case (_, List(_, y, _*)) => y }
    if (duplicates.nonEmpty) {
      Org.logMessage(TextRange(idContext), s"Modifier '${duplicates.head.toString}' is used more than once")
      mods.toSet.toSeq
    } else if (mods.intersect(allVisibilityModifiers).size > 1) {
      Org.logMessage(TextRange(idContext),
        s"Only one visibility modifier from 'webservice', 'global', 'public', 'protected' & 'private' may be used on fields")
      PUBLIC_MODIFIER +: mods.diff(allVisibilityModifiers)
    } else if (mods.intersect(allVisibilityModifiers).isEmpty) {
      PRIVATE_MODIFIER +: mods
    } else {
      mods
    }
  }

  def propertyBlockModifiers(modifierContexts: Seq[ModifierContext], context: ConstructContext, idContext: PropertyBlockContext)
  : Seq[Modifier] = {

    val mods = modifierContexts.flatMap(modifierContext =>
        modifierContext.getText.toLowerCase match {
          case "global" => Some(GLOBAL_MODIFIER)
          case "public" => Some(PUBLIC_MODIFIER)
          case "protected" => Some(PROTECTED_MODIFIER)
          case "private" => Some(PRIVATE_MODIFIER)
          case _ =>
            Org.logMessage(TextRange(modifierContext),
              s"Modifier '${modifierContext.getText}' is not supported on property set/get")
            None
      }
    )

    val duplicates = mods.groupBy(identity).collect { case (_, List(_, y, _*)) => y }
    if (duplicates.nonEmpty) {
      Org.logMessage(TextRange(idContext), s"Modifier '${duplicates.head.toString}' is used more than once")
      mods.toSet.toSeq
    } else if (mods.intersect(allVisibilityModifiers).size > 1) {
      Org.logMessage(TextRange(idContext),
        s"Only one visibility modifier from 'global, 'public', 'protected' & 'private' may be used on property set/get")
      mods.diff(allVisibilityModifiers)
    } else {
      mods
    }
  }

  private def classAnnotation(context: AnnotationContext): Option[Modifier] = {
    context.qualifiedName().getText.toLowerCase match {
      case "deprecated" => Some(DEPRECATED_ANNOTATION)
      case "istest" => Some(ISTEST_ANNOTATION)
      case "testvisible" => Some(TEST_VISIBLE_ANNOTATION)
      case "suppresswarnings" => Some(SUPPRESS_WARNINGS_ANNOTATION)
      case "restresource" => Some(REST_RESOURCE_ANNOTATION)
      case "namespaceaccessible" => Some(NAMESPACE_ACCESSIBLE_ANNOTATION)
      case _ =>
        Org.logMessage(TextRange(context),
          s"Unexpected annotation '${context.qualifiedName().getText}' on class declaration")
        None
    }
  }

  private def typeAnnotation(context: AnnotationContext): Option[Modifier] = {
    context.qualifiedName().getText.toLowerCase match {
      case "deprecated" => Some(DEPRECATED_ANNOTATION)
      case "testvisible" => Some(TEST_VISIBLE_ANNOTATION)
      case "suppresswarnings" => Some(SUPPRESS_WARNINGS_ANNOTATION)
      case "namespaceaccessible" => Some(NAMESPACE_ACCESSIBLE_ANNOTATION)
      case _ =>
        Org.logMessage(TextRange(context),
          s"Unexpected annotation '${context.qualifiedName().getText}' on interface declaration")
        None
    }
  }

  private def fieldAnnotation(context: AnnotationContext): Option[Modifier] = {
    // TODO: Validate arguments of the annotations
    context.qualifiedName().getText.toLowerCase match {
      case "auraenabled" => Some(AURA_ENABLED_ANNOTATION)
      case "deprecated" => Some(DEPRECATED_ANNOTATION)
      case "invocablevariable" => Some(INVOCABLE_VARIABLE_ANNOTATION)
      case "testvisible" => Some(TEST_VISIBLE_ANNOTATION)
      case "suppresswarnings" => Some(SUPPRESS_WARNINGS_ANNOTATION)
      case _ =>
        Org.logMessage(TextRange(context),
          s"Unexpected annotation '${context.qualifiedName().getText}' on field/property declaration")
        None
    }
  }

  def construct(modifiers: Seq[ModifierContext], context: ConstructContext): Seq[Modifier] = {
    modifiers.map(_.getText.toLowerCase).flatMap {
      case "public" => Some(PUBLIC_MODIFIER)
      case _ => None
    }
  }
}

object PlatformModifiers {
  def typeModifiers(javaBits: Int, nature: Nature): Seq[Modifier] = {
    assert(JavaModifier.isPublic(javaBits))
    if (nature == CLASS_NATURE) assert(!JavaModifier.isAbstract(javaBits))
    if (nature != ENUM_NATURE) assert(!JavaModifier.isFinal(javaBits))
    assert(!JavaModifier.isTransient(javaBits))
    assert(!JavaModifier.isVolatile(javaBits))
    assert(!JavaModifier.isSynchronized(javaBits))
    assert(!JavaModifier.isNative(javaBits))
    assert(!JavaModifier.isStrict(javaBits))

    (if (nature == CLASS_NATURE) Seq(VIRTUAL_MODIFIER) else Seq()) ++
    (if (JavaModifier.isStatic(javaBits)) Seq(STATIC_MODIFIER) else Seq()) ++
    Seq(PUBLIC_MODIFIER)
  }

  def fieldOrMethodModifiers(javaBits: Int): Seq[Modifier] = {
    assert(JavaModifier.isPublic(javaBits))
    assert(!JavaModifier.isAbstract(javaBits))
    assert(!JavaModifier.isFinal(javaBits))
    assert(!JavaModifier.isTransient(javaBits))
    assert(!JavaModifier.isVolatile(javaBits))
    assert(!JavaModifier.isSynchronized(javaBits))
    assert(!JavaModifier.isNative(javaBits))
    assert(!JavaModifier.isStrict(javaBits))

    if (JavaModifier.isStatic(javaBits))
      Seq(PUBLIC_MODIFIER, STATIC_MODIFIER)
    else
      Seq(PUBLIC_MODIFIER)
  }

  def methodModifiers(javaBits: Int, nature: Nature): Seq[Modifier] = {
    assert(JavaModifier.isPublic(javaBits))
    if (nature == INTERFACE_NATURE)
      assert(JavaModifier.isAbstract(javaBits))
    else
      assert(!JavaModifier.isAbstract(javaBits))
    assert(!JavaModifier.isFinal(javaBits))
    assert(!JavaModifier.isTransient(javaBits))
    assert(!JavaModifier.isVolatile(javaBits))
    assert(!JavaModifier.isSynchronized(javaBits))
    assert(!JavaModifier.isNative(javaBits))
    assert(!JavaModifier.isStrict(javaBits))

    if (JavaModifier.isStatic(javaBits))
      Seq(PUBLIC_MODIFIER, STATIC_MODIFIER)
    else
      Seq(PUBLIC_MODIFIER)
  }

}
