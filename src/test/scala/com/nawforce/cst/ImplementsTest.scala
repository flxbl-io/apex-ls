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
package com.nawforce.cst

import java.io.ByteArrayInputStream
import java.nio.file.{Path, Paths}

import com.nawforce.api.Org
import com.nawforce.types.TypeDeclaration
import com.nawforce.utils.{DotName, Name}
import org.scalatest.FunSuite

class ImplementsTest extends FunSuite {

  private val defaultName: Name = Name("Dummy")
  private val defaultPath: Path = Paths.get(defaultName.toString + ".cls")
  private val defaultOrg: Org = new Org

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

  test("Missing class interface") {
    assert(typeDeclarations(Map("Dummy" -> "global class Dummy implements A {}")).nonEmpty)
    assert(defaultOrg.issues.getMessages(defaultPath) ==
      "line 1 at 13-18: No declaration found for interface 'A'\n")
  }

  test("Missing class second interface") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "global class Dummy implements A, B {}",
      "A" -> "public interface A {}"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) ==
      "line 1 at 13-18: No declaration found for interface 'B'\n")
  }

  test("Class implements class") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "global class Dummy implements A {}",
      "A" -> "public class A {}"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) ==
      "line 1 at 13-18: Type 'A' must be an interface\n")
  }

  test("Class implements enum") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "global class Dummy implements A {}",
      "A" -> "public enum A {}"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) ==
      "line 1 at 13-18: Type 'A' must be an interface\n")
  }

  test("Interface extends class") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "global interface Dummy extends A {}",
      "A" -> "public class A {}"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) ==
      "line 1 at 17-22: Type 'A' must be an interface\n")
  }

  test("Interface extends enum") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "global interface Dummy extends A {}",
      "A" -> "public enum A {}"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) ==
      "line 1 at 17-22: Type 'A' must be an interface\n")
  }

  test("Class implements Database.Batchable<sObject>") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "global class Dummy implements Database.Batchable<sObject> {}"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "")
  }

  test("Class implements Database.Batchable<Object>") {
    val tds = typeDeclarations(Map(
      "Dummy" -> "global class Dummy implements Database.Batchable<Object> {}"
    ))
    assert(defaultOrg.issues.getMessages(defaultPath) == "")
  }

}