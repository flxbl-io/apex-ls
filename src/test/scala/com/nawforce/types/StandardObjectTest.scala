package com.nawforce.types

import java.nio.file.Files

import com.google.common.jimfs.{Configuration, Jimfs}
import com.nawforce.api.Org
import com.nawforce.names.Name
import org.scalatest.FunSuite

class StandardObjectTest extends FunSuite {

  def customObject(label: String, fields: Seq[(String, String, Option[String])]): String = {
    val fieldMetadata = fields.map(field => {
      s"""
         |    <fields>
         |        <fullName>${field._1}</fullName>
         |        <type>${field._2}</type>
         |        ${if (field._3.nonEmpty) s"<referenceTo>${field._3.get}</referenceTo>" else ""}
         |        ${if (field._3.nonEmpty) s"<relationshipName>${field._1.replaceAll("__c$", "")}</relationshipName>" else ""}
         |    </fields>
         |""".stripMargin
    })

    s"""<?xml version="1.0" encoding="UTF-8"?>
       |<CustomObject xmlns="http://soap.sforce.com/2006/04/metadata">
       |    <fullName>$label</fullName>
       |    $fieldMetadata
       |</CustomObject>
       |""".stripMargin
  }

  def customField(name: String, fieldType: String, relationshipName: Option[String]): String = {
    s"""<?xml version="1.0" encoding="UTF-8"?>
       |<CustomField xmlns="http://soap.sforce.com/2006/04/metadata">
       |    <fullName>$name</fullName>
       |    <type>$fieldType</type>
       |    ${if (relationshipName.nonEmpty) s"<referenceTo>${relationshipName.get}</referenceTo>" else ""}
       |    ${if (relationshipName.nonEmpty) s"<relationshipName>${name.replaceAll("__c$","")}</relationshipName>" else ""}
       |</CustomField>
       |""".stripMargin
  }

  def customFieldSet(name: String): String = {
    s"""<?xml version="1.0" encoding="UTF-8"?>
       |<FieldSet xmlns="http://soap.sforce.com/2006/04/metadata">
       |    <fullName>$name</fullName>
       |</FieldSet>
       |""".stripMargin
  }

  test("Not a standard object") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Foo.object"), customObject("Foo", Seq(("Bar__c", "Text", None))).getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/Foo.object")) ==
      "line 0: No SObject declaration found for 'Schema.Foo'\n")
  }

  test("Not a sObject") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("String.object"), customObject("String", Seq(("Bar__c", "Text", None))).getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/String.object")) ==
      "line 0: No SObject declaration found for 'Schema.String'\n")
  }

  test("UserRecordAccess available") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {Account a; Boolean x = a.UserRecordAccess.HasDeleteAccess;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Custom field") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Account.object"), customObject("Account", Seq(("Bar__c", "Text", None))).getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {Account a; a.Bar__c = '';} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Custom field (wrong name)") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Account.object"), customObject("Account", Seq(("Bar__c", "Text", None))).getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {Account a; a.Baz__c = '';} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/Dummy.cls")) ==
      "line 1 at 33-41: Unknown field or type 'Baz__c' on 'Schema.Account'\n")
  }

  test("Custom base package field") {
    val fs = Jimfs.newFileSystem(Configuration.unix)

    Files.createDirectory(fs.getPath("pkg1"))
    Files.write(fs.getPath("pkg1/Account.object"), customObject("Account", Seq(("Bar__c", "Text", None))).getBytes())
    Files.createDirectory(fs.getPath("pkg2"))
    Files.write(fs.getPath("pkg2/Dummy.cls"),"public class Dummy { {Account a; a.pkg1__Bar__c = '';} }".getBytes())

    val org = new Org()
    val pkg1 = org.addPackageInternal(Some(Name("pkg1")), Seq(fs.getPath("/work/pkg1")), Seq())
    val pkg2 = org.addPackageInternal(Some(Name("pkg2")), Seq(fs.getPath("/work/pkg2")), Seq(pkg1))
    pkg1.deployAll()
    pkg2.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Custom base package without namespace") {
    val fs = Jimfs.newFileSystem(Configuration.unix)

    Files.createDirectory(fs.getPath("pkg1"))
    Files.write(fs.getPath("pkg1/Account.object"), customObject("Account", Seq(("Bar__c", "Text", None))).getBytes())
    Files.createDirectory(fs.getPath("pkg2"))
    Files.write(fs.getPath("pkg2/Dummy.cls"),"public class Dummy { {Account a; a.Bar__c = '';} }".getBytes())

    val org = new Org()
    val pkg1 = org.addPackageInternal(Some(Name("pkg1")), Seq(fs.getPath("/work/pkg1")), Seq())
    val pkg2 = org.addPackageInternal(Some(Name("pkg2")), Seq(fs.getPath("/work/pkg2")), Seq(pkg1))
    pkg1.deployAll()
    pkg2.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/pkg2/Dummy.cls")) ==
      "line 1 at 33-41: Unknown field or type 'Bar__c' on 'Schema.Account'\n")
  }

  test("RecordTypeId field") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {Account a; a.RecordTypeId = '';} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Standard field reference") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {SObjectField a = Account.Fax;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Custom field reference") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Account.object"), customObject("Account", Seq(("Bar__c", "Text", None))).getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {SObjectField a = Account.Bar__c;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Invalid field reference") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Account.object"), customObject("Account", Seq(("Bar__c", "Text", None))).getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {SObjectField a = Account.Baz__c;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/Dummy.cls")) ==
      "line 1 at 39-53: Unknown field or type 'Baz__c' on 'Schema.Account'\n")
  }

  test("Lookup related list") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Foo__c.object"), customObject("Foo", Seq(("Lookup__c", "Lookup", Some("Account")))).getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {SObjectField a = Account.Lookup__r;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Lookup related list (packaged)") {
    val fs = Jimfs.newFileSystem(Configuration.unix)

    Files.createDirectory(fs.getPath("pkg1"))
    Files.write(fs.getPath("pkg1/Foo__c.object"), customObject("Foo", Seq(("Lookup__c", "Lookup", Some("Account")))).getBytes())
    Files.createDirectory(fs.getPath("pkg2"))
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {SObjectField a = Account.pkg1__Lookup__r;} }".getBytes())

    val org = new Org()
    val pkg1 = org.addPackageInternal(Some(Name("pkg1")), Seq(fs.getPath("/work/pkg1")), Seq())
    val pkg2 = org.addPackageInternal(None, Seq(fs.getPath("/work/pkg2")), Seq(pkg1))
    pkg2.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Object describable") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {DescribeSObjectResult a = SObjectType.Account;} }".getBytes())
    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Unknown Object describe error") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {DescribeSObjectResult a = SObjectType.Foo;} }".getBytes())
    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/Dummy.cls")) ==
      "line 1 at 48-63: Unknown field or type 'Foo' on 'Schema.SObjectType'\n")
  }

  test("Field describable") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {DescribeSObjectResult a = SObjectType.Account.Fields.Fax;} }".getBytes())
    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Field describable via Object") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {DescribeFieldResult a = Contact.SObjectType.Fields.Fax;} }".getBytes())
    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Unknown Field describe error") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {DescribeSObjectResult a = SObjectType.Account.Fields.Foo;} }".getBytes())
    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/Dummy.cls")) ==
      "line 1 at 48-78: Unknown field or type 'Foo' on 'Schema.SObjectType.Account.Fields'\n")
  }

  test("Unknown FieldSet describe error") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {DescribeSObjectResult a = SObjectType.Account.FieldSets.Foo;} }".getBytes())
    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(org.issues.getMessages(fs.getPath("/work/Dummy.cls")) ==
      "line 1 at 48-81: Unknown field or type 'Foo' on 'Schema.SObjectType.Account.FieldSets'\n")
  }

  test("Sfdx field reference") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.createDirectory(fs.getPath("Account"))
    Files.createDirectory(fs.getPath("Account/fields"))
    Files.write(fs.getPath("Account/Account.object-meta.xml"), customObject("Account", Seq()).getBytes())
    Files.write(fs.getPath("Account/fields/Bar__c.field-meta.xml"), customField("Bar__c", "Text", None).getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {SObjectField a = Account.Bar__c;} }".getBytes())

    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Sfdx FieldSet describable") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.createDirectory(fs.getPath("Account"))
    Files.createDirectory(fs.getPath("Account/fieldSets"))
    Files.write(fs.getPath("Account/Account.object-meta.xml"), customObject("Account", Seq()).getBytes())
    Files.write(fs.getPath("Account/fieldSets/TestFS.fieldSet-meta.xml"), customFieldSet("TestFS").getBytes())
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {DescribeSObjectResult a = SObjectType.Account.FieldSets.TestFS;} }".getBytes())
    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }

  test("Schema sObject access describable") {
    val fs = Jimfs.newFileSystem(Configuration.unix)
    Files.write(fs.getPath("Dummy.cls"),"public class Dummy { {SObjectType a = Schema.Account.SObjectType;} }".getBytes())
    val org = new Org()
    val pkg = org.addPackageInternal(None, Seq(fs.getPath("/")), Seq())
    pkg.deployAll()
    assert(!org.issues.hasMessages)
  }
}
