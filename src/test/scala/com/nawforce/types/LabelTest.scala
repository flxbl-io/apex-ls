/*
 [The "BSD licence"]
 Copyright (c) 2017 Kevin Jones
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

import java.nio.file.Files

import com.google.common.jimfs.{Configuration, Jimfs}
import com.nawforce.api.Org
import com.nawforce.utils.Name
import org.scalatest.FunSuite

class LabelTest extends FunSuite {

  test("Empty labels file") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    val labels = fs.getPath("CustomLabels.labels")
    Files.createFile(labels)

    val org = new Org()
    org.addPackageInternal(Name.Empty, Seq(fs.getPath("/")), Seq())
    assert(org.issues.getMessages(fs.getPath("/work/CustomLabels.labels")) ==
      "line 1 at 1: Premature end of file.\n")
  }

  test("Minimal labels file") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    val labels = fs.getPath("CustomLabels.labels")
    Files.write(labels, "<CustomLabels xmlns=\"http://soap.sforce.com/2006/04/metadata\"/>".getBytes())

    val org = new Org()
    org.addPackageInternal(Name.Empty, Seq(fs.getPath("/")), Seq())
    assert(!org.issues.hasMessages)
  }

  test("Valid label") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    val labelsContent =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<CustomLabels xmlns="http://soap.sforce.com/2006/04/metadata">
        |    <labels>
        |        <fullName>TestLabel</fullName>
        |        <language>en_US</language>
        |        <protected>true</protected>
        |        <shortDescription>TestLabel Description</shortDescription>
        |        <value>TestLabel Value</value>
        |    </labels>
        |</CustomLabels>
        |""".stripMargin
    Files.write(fs.getPath("CustomLabels.labels"), labelsContent.getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {String a = Label.TestLabel;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(Name.Empty, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Valid label (case insensitive)") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    val labelsContent =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<CustomLabels xmlns="http://soap.sforce.com/2006/04/metadata">
        |    <labels>
        |        <fullName>TestLabel</fullName>
        |        <language>en_US</language>
        |        <protected>true</protected>
        |        <shortDescription>TestLabel Description</shortDescription>
        |        <value>TestLabel Value</value>
        |    </labels>
        |</CustomLabels>
        |""".stripMargin
    Files.write(fs.getPath("CustomLabels.labels"), labelsContent.getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {String a = laBel.TeStLaBel;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(Name.Empty, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Missing label") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    val labelsContent =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<CustomLabels xmlns="http://soap.sforce.com/2006/04/metadata">
        |    <labels>
        |        <fullName>TestLabel</fullName>
        |        <language>en_US</language>
        |        <protected>true</protected>
        |        <shortDescription>TestLabel Description</shortDescription>
        |        <value>TestLabel Value</value>
        |    </labels>
        |</CustomLabels>
        |""".stripMargin
    Files.write(fs.getPath("CustomLabels.labels"), labelsContent.getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {String a = Label.TestLabel2;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(Name.Empty, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/Dummy.cls")) ==
      "line 1 at 33-49: Label does not exists for 'TestLabel2'\n")
  }

  test("Missing label (case insensitive)") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    val labelsContent =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<CustomLabels xmlns="http://soap.sforce.com/2006/04/metadata">
        |    <labels>
        |        <fullName>TestLabel</fullName>
        |        <language>en_US</language>
        |        <protected>true</protected>
        |        <shortDescription>TestLabel Description</shortDescription>
        |        <value>TestLabel Value</value>
        |    </labels>
        |</CustomLabels>
        |""".stripMargin
    Files.write(fs.getPath("CustomLabels.labels"), labelsContent.getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {String a = laBel.TestLaBel2;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(Name.Empty, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/Dummy.cls")) ==
      "line 1 at 33-49: Label does not exists for 'TestLaBel2'\n")
  }

  test("Base package label") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    val labelsContent =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<CustomLabels xmlns="http://soap.sforce.com/2006/04/metadata">
        |    <labels>
        |        <fullName>TestLabel</fullName>
        |        <language>en_US</language>
        |        <protected>false</protected>
        |        <shortDescription>TestLabel Description</shortDescription>
        |        <value>TestLabel Value</value>
        |    </labels>
        |</CustomLabels>
        |""".stripMargin
    Files.createDirectory(fs.getPath("pkg1"))
    Files.write(fs.getPath("pkg1/CustomLabels.labels"), labelsContent.getBytes())
    Files.createDirectory(fs.getPath("pkg2"))
    Files.write(fs.getPath("pkg2/Dummy.cls"),"public class Dummy { {String a = label.pkg1.TestLabel;} }".getBytes())

    val org = new Org()
    val pkg1 = org.addPackageInternal(Name("pkg1"), Seq(fs.getPath("/work/pkg1")), Seq())
    val pkg2 = org.addPackageInternal(Name("pkg2"), Seq(fs.getPath("/work/pkg2")), Seq(pkg1))
    pkg2.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Base package label protected") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    val labelsContent =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<CustomLabels xmlns="http://soap.sforce.com/2006/04/metadata">
        |    <labels>
        |        <fullName>TestLabel</fullName>
        |        <language>en_US</language>
        |        <protected>true</protected>
        |        <shortDescription>TestLabel Description</shortDescription>
        |        <value>TestLabel Value</value>
        |    </labels>
        |</CustomLabels>
        |""".stripMargin
    Files.createDirectory(fs.getPath("pkg1"))
    Files.write(fs.getPath("pkg1/CustomLabels.labels"), labelsContent.getBytes())
    Files.createDirectory(fs.getPath("pkg2"))
    Files.write(fs.getPath("pkg2/Dummy.cls"),"public class Dummy { {String a = label.pkg1.TestLabel;} }".getBytes())

    val org = new Org()
    val pkg1 = org.addPackageInternal(Name("pkg1"), Seq(fs.getPath("/work/pkg1")), Seq())
    val pkg2 = org.addPackageInternal(Name("pkg2"), Seq(fs.getPath("/work/pkg2")), Seq(pkg1))
    pkg2.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/pkg2/Dummy.cls")) ==
      "line 1 at 33-53: Label 'pkg1.TestLabel' is protected so can not be accessed externally\n")
  }
}