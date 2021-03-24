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
package com.nawforce.common.cst

import com.nawforce.common.FileSystemHelper
import com.nawforce.common.api.{Org, ServerOps}
import com.nawforce.common.org.OrgImpl
import com.nawforce.common.path.PathLike
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite

class MethodShadowTest extends AnyFunSuite with BeforeAndAfter {

  before {
    ServerOps.setAutoFlush(false)
  }

  after {
    ServerOps.setAutoFlush(true)
  }

  def testMethods(files: Map[String, String], issue: String = ""): Unit = {
    FileSystemHelper.run(files) {
      root: PathLike =>
        val org = Org.newOrg().asInstanceOf[OrgImpl]
        org.newMDAPIPackageInternal(None, Seq(root), Seq())
        assert(org.issues.getMessages(root.join(files.head._1).toString) == issue)
    }
  }

  test("Override of public non-virtual") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { public void func() {} }",
          "SuperClass.cls" -> "public virtual class SuperClass { public void func() {}}"),
      "Error: line 1 at 52-56: Method 'func' can not override non-virtual method\n")
  }

  test("Override of public virtual without override") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { public void func() {} }",
          "SuperClass.cls" -> "public virtual class SuperClass { public virtual void func() {}}"),
      "Error: line 1 at 52-56: Method 'func' must use override keyword\n")
  }

  test("Override of missing method") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { public override void func2() {} }",
        "SuperClass.cls" -> "public virtual class SuperClass { private virtual void func() {}}"),
      "Error: line 1 at 61-66: Method 'func2' does not override a virtual or abstract method\n")
  }

  test("Override of private virtual") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { public override void func() {} }",
        "SuperClass.cls" -> "public virtual class SuperClass { private virtual void func() {}}"),
      "Error: line 1 at 61-65: Method 'func' does not override a virtual or abstract method\n")
  }

  test("Override of protected virtual") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { public override void func() {} }",
        "SuperClass.cls" -> "public virtual class SuperClass { protected virtual void func() {}}"),
      "")
  }

  test("Override of public virtual") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { public override void func() {} }",
        "SuperClass.cls" -> "public virtual class SuperClass { public virtual void func() {}}"),
      "")
  }

  test("Override of public virtual (with protected)") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { protected override void func() {} }",
        "SuperClass.cls" -> "public virtual class SuperClass { public virtual void func() {}}"),
      "Error: line 1 at 64-68: Method 'func' can not reduce visibility in override\n")
  }

  test("Override of private abstract") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { public override void func() {} }",
        "SuperClass.cls" -> "public abstract class SuperClass { private abstract void func();}"),
      "Error: line 1 at 61-65: Method 'func' does not override a virtual or abstract method\n")
  }

  test("Override of protected abstract") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { public override void func() {} }",
        "SuperClass.cls" -> "public abstract class SuperClass { protected abstract void func();}"),
      "")
  }

  test("Override of protected abstract (with private)") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { private override void func() {} }",
        "SuperClass.cls" -> "public abstract class SuperClass { protected abstract void func();}"),
      "Error: line 1 at 62-66: Method 'func' can not reduce visibility in override\n")
  }

  test("Override of public abstract") {
    testMethods(
      Map("Dummy.cls" -> "public class Dummy extends SuperClass { public override void func() {} }",
        "SuperClass.cls" -> "public abstract class SuperClass { public abstract void func();}"),
      "")
  }

  test("Override of private virtual (test visible)") {
    testMethods(
      Map("Dummy.cls" -> "@IsTest public class Dummy extends SuperClass { public override void func() {} }",
        "SuperClass.cls" -> "public virtual class SuperClass {@TestVisible private virtual void func() {}}"),
      "")
  }
}
