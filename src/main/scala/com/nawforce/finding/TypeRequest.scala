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
package com.nawforce.finding

import com.nawforce.names.TypeName
import com.nawforce.types.{PackageDeclaration, PlatformGetRequest, PlatformTypeDeclaration, TypeDeclaration}
import scalaz.{Failure, Success}

/** Helper for abstracting various ways of finding types based on context info, these are:
  *   None - Can only be used for platform type search
  *   PackageDeclaration - Package, dependant package & platform type search
  *   TypeDeclaration (where code is located) - Local, (package, dependant package if packaged) & platform type search
  *
  *   Note: Platform TypeDeclarations are not packaged & not all code (triggers, anon) comes from TypeDeclarations,
  *   unmanaged code is part of a special package declaration with no namespace
  **/
object TypeRequest {
  type TypeRequest = Either[String, TypeDeclaration]

  def apply(typeName: TypeName): TypeRequest = {
    PlatformTypeDeclaration.get(typeName, None) match {
      case Success(td) => Right(td)
      case Failure(e) => Left(e.head.toString)
    }
  }

  def apply(typeName: TypeName, pkg: PackageDeclaration): TypeRequest = {
    pkg.getType(PlatformGetRequest(typeName, None))
  }

  def apply(typeName: TypeName, from: TypeDeclaration): TypeRequest = {
    val pkg = from.packageDeclaration
    if (pkg.nonEmpty) {
      pkg.get.getTypeFor(typeName, from) match {
        case Some(td) => Right(td)
        case None => Left(s"No type declaration found for '$typeName'")
      }
    } else {
      PlatformTypeDeclaration.get(typeName, Some(from)) match {
        case Success(td) => Right(td)
        case Failure(e) => Left(e.head.toString)
      }
    }
  }

  def apply(typeName: TypeName, from: Option[TypeDeclaration], pkg: Option[PackageDeclaration]): TypeRequest = {
    if (from.nonEmpty)
      apply(typeName, from.get)
    else if (pkg.nonEmpty)
      apply(typeName, pkg.get)
    else
      apply(typeName)
  }
}
