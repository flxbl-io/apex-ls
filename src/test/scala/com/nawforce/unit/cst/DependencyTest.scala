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
package com.nawforce.unit.cst

import java.io.ByteArrayInputStream
import java.nio.file.{Path, Paths}

import com.nawforce.api.Org
import com.nawforce.types.{TypeDeclaration, TypeName}
import com.nawforce.utils.{DotName, Name}
import org.scalatest.FunSuite

class DependencyTest extends FunSuite {
  private val defaultName: Name = Name("Dummy.cls")
  private val defaultPath: Path = Paths.get(defaultName.toString)
  private val defaultOrg: Org = new Org

  private val listClass = defaultOrg.getType(DotName(Seq(Name.System, Name.List))).get
  private val objectClass = defaultOrg.getType(DotName(Name.Object)).get
  private val typeClass = defaultOrg.getType(DotName(Seq(Name.System, Name.Type))).get
  private val booleanClass = defaultOrg.getType(DotName(Name.Boolean)).get
  private val queryLocatorClass = defaultOrg.getType(DotName(Seq(Name("Database"), Name("QueryLocator")))).get

  def typeDeclarations(classes: Map[String, String]): Seq[TypeDeclaration] = {
    defaultOrg.clear()
    val paths = classes.map(kv => {
      val fakePath = Paths.get(kv._1 + ".cls")
      defaultOrg.setInputStream(fakePath, new ByteArrayInputStream(kv._2.getBytes()))
      fakePath
    }).toSeq

    Org.current.withValue(defaultOrg) {
      defaultOrg.deployMetadata(Name.Empty, paths)
      defaultOrg.getTypes(classes.keys.map(k => DotName(k)).toSeq)
    }
  }

  test("Empty class has no imports") {
    val tds = typeDeclarations(Map("Dummy" -> "public class Dummy {}"))
    assert(tds.head.dependencies().isEmpty)
  }

  test("Class depends on superclass") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy extends A {}",
      "A" -> "public virtual class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.dependencies() == tds.tail.toSet)
  }

  test("Class depends on interface") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy implements A, B {}",
      "A" -> "public interface A {}",
      "B" -> "public interface B {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.dependencies() == tds.tail.toSet)
  }

  test("Interface depends on interface") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public interface Dummy extends A, B {}",
      "A" -> "public interface A {}",
      "B" -> "public interface B {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.dependencies() == tds.tail.toSet)
  }

  test("Empty inner class has no dependencies") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner {} }"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.dependencies().isEmpty)
  }

  test("Inner class depends on superclass") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner extends A {} }",
      "A" -> "public virtual class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.dependencies() == tds.tail.toSet)
  }

  test("Inner class depends on interface") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner implements A,B {} }",
      "A" -> "public interface A {}",
      "B" -> "public interface B {}",
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.dependencies() == tds.tail.toSet)
  }

  test("Inner interface depends on interface") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { interface Inner extends A, B {} }",
      "A" -> "public interface A {}",
      "B" -> "public interface B {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.dependencies() == tds.tail.toSet)
  }

  test("Class reference creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { {Type t = A.class;} }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.blocks.head.dependencies() == Set(tds.tail.head, typeClass))
  }

  test("Class self-reference creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { {Type t = Dummy.class;} }"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.blocks.head.dependencies() == Set(tds.head, typeClass))
  }

  test("Class reference via super types create dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy extends A { {Type t = A.C.class;} }",
      "A" -> "public virtual class A extends B {}",
      "B" -> "public virtual class B {public class C {} }"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.blocks.head.dependencies() == Set(tds(2).nestedTypes.head, typeClass))
  }

  test("Class reference with ambiguous name") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Database {Type t = Database.QueryLocator.class;} }",
    ))
    assert(!defaultOrg.issues.hasMessages)
    Org.current.withValue(defaultOrg) {
      assert(tds.head.nestedTypes.head.fields.head.dependencies() == Set(typeClass, queryLocatorClass))
    }
  }

  test("Class reference for component") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { Type t = Component.Apex.OutputText.class; }",
    ))
    assert(!defaultOrg.issues.hasMessages)
    Org.current.withValue(defaultOrg) {
      val cmp = tds.head.fields.head.dependencies().filterNot(_ == typeClass)
      assert(cmp.head.asInstanceOf[TypeDeclaration].typeName.toString == "Component.Apex.OutputText")
    }
  }

  test("Method return creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { A func() {return null;} }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.methods.head.dependencies() == tds.tail.toSet)
  }

  test("Unknown method return") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { A func() {return null;} }"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 23-27: No type declaration found for 'A'\n")
    assert(tds.head.methods.head.dependencies().isEmpty)
  }

  test("Method parameter creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { void func(A a) {} }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.methods.head.dependencies() == tds.tail.toSet)
  }

  test("Unknown method parameter") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { void func(A a) {} }"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 33-34: No type declaration found for 'A'\n")
    assert(tds.head.methods.head.dependencies().isEmpty)
  }

  test("Field type creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {A a;}",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    Org.current.withValue(defaultOrg) {
      assert(tds.head.fields.head.dependencies() == tds.tail.toSet)
    }
  }

  test("Unknown Field type") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {A a;}"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 22-23: No type declaration found for 'A'\n")
    Org.current.withValue(defaultOrg) {
      assert(tds.head.fields.head.dependencies().isEmpty)
    }
  }

  test("Property type creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {A a {get;} }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    Org.current.withValue(defaultOrg) {
      assert(tds.head.fields.head.dependencies() == tds.tail.toSet)
    }
  }

  test("Unknown Property type") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {A a {get;} }"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 22-23: No type declaration found for 'A'\n")
    Org.current.withValue(defaultOrg) {
      assert(tds.head.fields.head.dependencies().isEmpty)
    }
  }

  test("Local var creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {static {A a;} }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.blocks.head.dependencies() == tds.tail.toSet)
  }

  test("Unknown local var type") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {static {A a;} }"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 28-31: No type declaration found for 'A'\n")
    assert(tds.head.blocks.head.dependencies().isEmpty)
  }

  test("Cast creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {static {Object a=(A)null;} }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.blocks.head.dependencies() == Set(objectClass, tds.tail.head))
  }

  test("Unknown cast type") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {static {Object a=(A)null;} }"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 37-44: No type declaration found for 'A'\n")
    assert(tds.head.blocks.head.dependencies() == Set(objectClass))
  }

  test("For control creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { void func() { for(A a;;) {}} }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.methods.head.dependencies() == tds.tail.toSet)
  }

  test("Unknown for control type") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { void func() { for(A a;;) {}} }"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 39-42: No type declaration found for 'A'\n")
    assert(tds.head.methods.head.dependencies().isEmpty)
  }

  test("Catch creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { void func() { try {} catch(A a){} } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.methods.head.dependencies() == tds.tail.toSet)
  }

  test("Unknown catch type") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { void func() { try {} catch(A a){} } }"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 48-49: No type declaration found for 'A'\n")
    assert(tds.head.methods.head.dependencies().isEmpty)
  }

  test("New creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { void func() { Object a = new A(); } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.methods.head.dependencies() == Set(tds.tail.head, objectClass))
  }

  test("Complex New creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { void func() { Object a = new List<A>(); } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.methods.head.dependencies() == Set(tds.tail.head, listClass, objectClass))
  }

  test("Unknown new type") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { void func() { Object a = new A(); } }"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 50-51: No type declaration found for 'A'\n")
    assert(tds.head.methods.head.dependencies() == Set(objectClass))
  }

  test("InstanceOf creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { { Boolean a = null instanceOf A; } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.blocks.head.dependencies() == Set(tds.tail.head, booleanClass))
  }

  test("Unknown instanceOf type") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { { Boolean a = null instanceOf A; } }"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "line 1 at 35-52: No type declaration found for 'A'\n")
    assert(tds.head.blocks.head.dependencies() == Set(booleanClass))
  }

  test("Class reference in Inner creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner { {Type t = A.class;} } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)

    assert(tds.head.nestedTypes.head.blocks.head.dependencies() == Set(tds.tail.head, typeClass))
  }

  test("Method return in Inner creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner { A func() {return null;} } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.methods.head.dependencies() == tds.tail.toSet)
  }

  test("Method parameter in Inner creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner { void func(A a) {} } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.methods.head.dependencies() == tds.tail.toSet)
  }

  test("Inner Field type creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {class Inner {A a;}}",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    Org.current.withValue(defaultOrg) {
      assert(tds.head.nestedTypes.head.fields.head.dependencies() == tds.tail.toSet)
    }
  }

  test("Inner Property type creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {class Inner {A a {get;}}}",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    Org.current.withValue(defaultOrg) {
      assert(tds.head.nestedTypes.head.fields.head.dependencies() == tds.tail.toSet)
    }
  }

  test("Inner Local var creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {class Inner {static {A a;} } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.blocks.head.dependencies() == tds.tail.toSet)
  }

  test("Inner Cast creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy {class Inner {static {Object a=(A)null;} } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.blocks.head.dependencies() == Set(tds.tail.head, objectClass))
  }

  test("Inner For control creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner {void func() { for(A a;;) {}} } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.methods.head.dependencies() == tds.tail.toSet)
  }

  test("Inner Catch creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner {void func() { try {} catch(A a){} } } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.methods.head.dependencies() == tds.tail.toSet)
  }

  test("Inner New creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner {void func() { Object a = new A(); } } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.methods.head.dependencies() == Set(tds.tail.head, objectClass))
  }

  test("Inner Complex New creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner {void func() { Object a = new List<A>(); } } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.methods.head.dependencies() == Set(tds.tail.head,listClass, objectClass))
  }

  test("Inner instanceOf creates dependency") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "public class Dummy { class Inner {{ Boolean a = null instanceOf A;} } }",
      "A" -> "public class A {}"
    ))
    assert(!defaultOrg.issues.hasMessages)
    assert(tds.head.nestedTypes.head.blocks.head.dependencies() == Set(tds.tail.head, booleanClass))
  }
}