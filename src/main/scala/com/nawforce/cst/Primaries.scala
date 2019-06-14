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
 3. the name of the author may not be used to endorse or promote products
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

import com.nawforce.parsers.ApexParser._
import com.nawforce.types.TypeName
import com.nawforce.utils.Name

sealed abstract class Primary extends CST {
  def verify(context: VerifyContext): Unit = {}
}

final case class ExpressionPrimary(expression: Expression) extends Primary {
  override def children(): List[CST] = expression :: Nil

  override def verify(context: VerifyContext): Unit = {
    expression.verify(context)
  }

  override def getType(ctx: TypeContext): TypeName = expression.getType(ctx)
}

final case class ThisPrimary() extends Primary {
  override def children(): List[CST] = Nil

  override def getType(ctx: TypeContext): TypeName = ctx.thisType
}

final case class SuperPrimary() extends Primary {
  override def children(): List[CST] = Nil

  override def getType(ctx: TypeContext): TypeName = ctx.superType
}

final case class LiteralPrimary(literal: Literal) extends Primary {
  override def children(): List[CST] = literal :: Nil

  override def getType(ctx: TypeContext): TypeName = literal.getType(ctx)
}

final case class QualifiedNamePrimary(typeName: TypeName) extends Primary {
  override def children(): List[CST] = Nil

  override def verify(context: VerifyContext): Unit = {
    // TODO: Check not var & below
    val isClassRef = typeName.name == Name.Class && typeName.outer.nonEmpty
    context.addImport(if (isClassRef) typeName.outer.get.asClassOf else typeName)
  }

  override def getType(ctx: TypeContext): TypeName = typeName
}

// Fake node for linking variable refs to their declarations
final case class VarRef(varDeclaration: VarDeclaration) extends Primary {
  override def children(): List[CST] = List(varDeclaration.id)
}

final case class SOQL(soql: String) extends Primary {
  override def children(): List[CST] = Nil
}

object Primary {
  def construct(from: PrimaryContext, context: ConstructContext): Primary = {
    val cst =
      from match {
        case ctx: SubPrimaryContext =>
          ExpressionPrimary(Expression.construct(ctx.expression(), context))
        case _: ThisPrimaryContext =>
          ThisPrimary()
        case _: SuperPrimaryContext =>
          SuperPrimary()
        case ctx: LiteralPrimaryContext =>
          LiteralPrimary(Literal.construct(ctx.literal(), context))
        case ctx: RefPrimaryContext =>
          QualifiedNamePrimary(TypeRef.construct(ctx.typeRef(), context))
        case ctx: SoqlPrimaryContext =>
          SOQL(ctx.soqlLiteral().getText)
      }
    cst.withContext(from, context)
  }
}