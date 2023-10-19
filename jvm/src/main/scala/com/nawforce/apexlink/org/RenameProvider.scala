/*
 * Copyright (c) 2023 Certinia Inc. All rights reserved.
 */
package com.nawforce.apexlink.org

import com.nawforce.apexlink.cst._
import com.nawforce.apexlink.org.TextOps.TestOpsUtils
import com.nawforce.apexlink.rpc.Rename
import com.nawforce.apexlink.types.apex.{ApexFullDeclaration, FullDeclaration, SummaryMethod}
import com.nawforce.pkgforce.names.Name
import com.nawforce.pkgforce.path.{Location, PathLike}

import scala.collection.mutable

trait RenameProvider extends SourceOps {
  this: OPM.PackageImpl =>

  def getRenameLocations(
    path: PathLike,
    line: Int,
    offset: Int,
    content: Option[String]
  ): Array[Rename] = {

    val sourceAndType =
      loadFullSourceAndType(path, None).getOrElse(return Array.empty)

    val methodDeclaration = getMethodDeclaration(sourceAndType._2, line, offset)

    methodDeclaration match {
      case Some(md) => return getMethodSymbolLocations(md)
      case _        =>
    }

    val searchSymbolLocation = sourceAndType._1
      .extractSymbolLocation(() => new IdentifierAndMethodLimiter, line, offset)

    searchSymbolLocation match {
      case Some(location) =>
        val editLocations =
          getVarLocations(sourceAndType._2.asInstanceOf[FullDeclaration], line, offset, location)
        Array(Rename(path.toString, editLocations))
      case None => Array(Rename(path.toString, Array.empty))
    }

  }

  private def getVarLocations(
    td: FullDeclaration,
    line: Int,
    offset: Int,
    searchSymbolLocation: Location
  ): Array[Location] = {
    val validationMap = td.getValidationMap(line, offset)

    // start by giving searchSymbolLocation otherwise it is missed when renaming 1 unused variable.
    var locations = Set(searchSymbolLocation)

    var requiresClassValidation = true

    val symbolDeclarationOption = {
      td.getValidationMapForMethodDeclarations.flatten.find(x => {
        if (
          x._1.startPosition == searchSymbolLocation.startPosition && x._1.startLine == searchSymbolLocation.startLine
        ) {
          true
        } else {
          x._2.result.locatable match {
            case Some(l: ApexFieldDeclaration) =>
              l.idLocation.startLine == searchSymbolLocation.startLine && l.idLocation.startPosition == searchSymbolLocation.startPosition
            case Some(l) =>
              l.location.location.startLine == searchSymbolLocation.startLine && l.location.location.startPosition == searchSymbolLocation.startPosition
            case _ =>
              false
          }
        }
      })
    }

    val symbolDeclaration = symbolDeclarationOption.getOrElse(return locations.toArray)

    validationMap.foreach(x => {
      x._2.result.locatable collect {
        case l: Id =>
          if (l == symbolDeclaration._2.result.locatable.get) {
            locations = locations + x._1 + l.location.location

            if (requiresClassValidation) {
              x._2.vars.foreach(scopeVarDefinition => {
                if (scopeVarDefinition.contains(Name(symbolDeclaration.toString))) {
                  requiresClassValidation = false
                }
              })
            }
          }
        case l: VariableDeclarator =>
          if (l == symbolDeclaration._2.result.locatable.get) {
            locations = locations + x._1 + l.id.location.location

            if (requiresClassValidation) {
              x._2.vars.foreach(scopeVarDefinition => {
                if (scopeVarDefinition.contains(Name(symbolDeclaration.toString))) {
                  requiresClassValidation = false
                }
              })
            }
          }
      }
    })

    if (requiresClassValidation) {
      td.getValidationMapForMethodDeclarations.flatten.foreach(x => {
        Some(x._2.result.locatable) collect {
          case Some(l: Id) =>
            if (l == symbolDeclaration._2.result.locatable.get) {
              locations = locations + x._1 + l.location.location
            }
          case Some(l: ApexFieldDeclaration) =>
            if (l == symbolDeclaration._2.result.locatable.get) {
              locations = locations + x._1 + l.idLocation
            }
        }
      })
    }

    locations.toArray
  }

  private def getMethodDeclaration(
    classDeclaration: ApexFullDeclaration,
    requestLine: Int,
    requestOffset: Int
  ): Option[ApexMethodDeclaration] = {
    val validation = locateFromValidation(classDeclaration, requestLine, requestOffset)
    validation._2 match {
      case Some(location) =>
        val vr = validation._1(location)
        vr.cst match {
          case methodCall: MethodCallWithId =>
            methodCall.cachedMethod match {
              case Some(amd: ApexMethodDeclaration) => Some(amd)
              case Some(sm: SummaryMethod) =>
                reValidate(Set(sm.thisTypeId) ++ sm.getDependencyHolders.collect {
                  case dh if dh.thisTypeIdOpt.isDefined => dh.thisTypeIdOpt.get
                })
                loadTypeFromModule(sm.location.path) match {
                  case Some(reloadedClassDec: ClassDeclaration) =>
                    reloadedClassDec.bodyDeclarations
                      .find {
                        case amd: ApexMethodDeclaration =>
                          amd.idPathLocation == sm.idPathLocation
                        case _ => false
                      }
                      .map(cbd => cbd.asInstanceOf[ApexMethodDeclaration])
                  case _ => None
                }
              case _ => None
            }
          case _ => None
        }

      case None =>
        classDeclaration match {
          case cd: ClassDeclaration =>
            cd.bodyDeclarations
              .foreach {
                case md: ApexMethodDeclaration =>
                  if (md.idLocation.contains(requestLine, requestOffset)) {
                    return Some(md)
                  }
                case _ =>
              }
          case _ =>
        }
        None
    }
  }

  private def getMethodSymbolLocations(md: ApexMethodDeclaration): Array[Rename] = {
    val calloutLocations = md.getDependencyHolders.collect {
      case holdingMethod: ApexMethodDeclaration =>
        val currentClassPath = holdingMethod.location.path
        val methodRenameLocations: mutable.Set[Location] = holdingMethod.block match {
          case Some(block: Block) => getLocationsFromStatements(block.statements(), md)
          case _                  => mutable.Set.empty
        }

        Rename(currentClassPath.toString, methodRenameLocations.toArray)

      case holdingConstructor: ApexConstructorDeclaration =>
        val currentClassPath = holdingConstructor.location.path
        val methodRenameLocations: mutable.Set[Location] =
          getLocationsFromStatements(holdingConstructor.block.statements(), md)

        Rename(currentClassPath.toString, methodRenameLocations.toArray)

      case fieldDeclaration: ApexFieldDeclaration =>
        val currentClassPath                             = fieldDeclaration.location.path
        val methodRenameLocations: mutable.Set[Location] = mutable.Set.empty
        fieldDeclaration.variableDeclarator.init match {
          case Some(exp) => methodRenameLocations.addAll(getMethodLocationsFromExpression(exp, md))
          case None      =>
        }

        Rename(currentClassPath.toString, methodRenameLocations.toArray)

      case holdingInitializerBlock: ApexInitializerBlock =>
        val currentClassPath = holdingInitializerBlock.location.path
        val methodRenameLocations: mutable.Set[Location] =
          getLocationsFromStatements(holdingInitializerBlock.block.statements(), md)

        Rename(currentClassPath.toString, methodRenameLocations.toArray)
    }.toArray

    calloutLocations :+ Rename(md.location.path.toString, Array(md.idLocation))
  }

  private def getLocationsFromStatements(
    statements: Seq[Statement],
    md: ApexMethodDeclaration
  ): mutable.Set[Location] = {
    val methodRenameLocations: mutable.Set[Location] =
      mutable.Set.empty

    statements.foreach {
      case expressionStatement: ExpressionStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(expressionStatement.expression, md)
        )

      case varDecStatement: LocalVariableDeclarationStatement =>
        varDecStatement.localVariableDeclaration.variableDeclarators.declarators.foreach(
          varDeclarator =>
            varDeclarator.init match {
              case Some(exp: Expression) =>
                methodRenameLocations.addAll(getMethodLocationsFromExpression(exp, md))
              case _ =>
            }
        )

      case returnStatement: ReturnStatement =>
        returnStatement.expression match {
          case Some(exp: Expression) =>
            methodRenameLocations.addAll(getMethodLocationsFromExpression(exp, md))
          case _ =>
        }

      case ifStatement: IfStatement =>
        methodRenameLocations.addAll(getMethodLocationsFromExpression(ifStatement.expression, md))

        ifStatement.statements.foreach {
          case block: Block =>
            methodRenameLocations.addAll(getLocationsFromStatements(block.statements(), md))
          case _ =>
        }

      case forStatement: ForStatement =>
        forStatement.control match {
          case Some(control: BasicForControl) =>
            control.forInit match {
              case Some(forInit: LocalVariableForInit) =>
                forInit.variable.variableDeclarators.declarators.foreach(varDeclarator =>
                  varDeclarator.init match {
                    case Some(exp: Expression) =>
                      methodRenameLocations.addAll(getMethodLocationsFromExpression(exp, md))
                    case _ =>
                  }
                )
              case Some(forInit: ExpressionListForInit) =>
                forInit.expressions.foreach(expression =>
                  methodRenameLocations.addAll(getMethodLocationsFromExpression(expression, md))
                )
              case _ =>
            }

            control.expression match {
              case Some(expression: Expression) =>
                methodRenameLocations.addAll(getMethodLocationsFromExpression(expression, md))
              case _ =>
            }

            control.forUpdate match {
              case Some(forUpdate: ForUpdate) =>
                forUpdate.expressions.foreach(expression =>
                  methodRenameLocations.addAll(getMethodLocationsFromExpression(expression, md))
                )
              case _ =>
            }

          case Some(control: EnhancedForControl) =>
            methodRenameLocations.addAll(getMethodLocationsFromExpression(control.expression, md))

          case _ =>
        }

        forStatement.statement match {
          case Some(block: Block) =>
            methodRenameLocations.addAll(getLocationsFromStatements(block.statements(), md))
          case _ =>
        }

      case whileStatement: WhileStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(whileStatement.expression, md)
        )

        whileStatement.statement match {
          case Some(block: Block) =>
            methodRenameLocations.addAll(getLocationsFromStatements(block.statements(), md))
          case _ =>
        }

      case doWhileStatement: DoWhileStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(doWhileStatement.expression, md)
        )

        methodRenameLocations.addAll(
          getLocationsFromStatements(doWhileStatement.block.statements(), md)
        )

      case tryStatement: TryStatement =>
        tryStatement.block match {
          case block: Block =>
            methodRenameLocations.addAll(getLocationsFromStatements(block.statements(), md))
        }

        tryStatement.catches.foreach(catchStatement =>
          catchStatement.block match {
            case Some(block: Block) =>
              methodRenameLocations.addAll(getLocationsFromStatements(block.statements(), md))
            case _ =>
          }
        )

        tryStatement.finallyBlock match {
          case Some(block: Block) =>
            methodRenameLocations.addAll(getLocationsFromStatements(block.statements(), md))
          case _ =>
        }

      case throwStatement: ThrowStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(throwStatement.expression, md)
        )

      case insertStatement: InsertStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(insertStatement.expression, md)
        )

      case updateStatement: UpdateStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(updateStatement.expression, md)
        )

      case deleteStatement: DeleteStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(deleteStatement.expression, md)
        )

      case undeleteStatement: UndeleteStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(undeleteStatement.expression, md)
        )

      case upsertStatement: UpsertStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(upsertStatement.expression, md)
        )

      case mergeStatement: MergeStatement =>
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(mergeStatement.expression1, md)
        )
        methodRenameLocations.addAll(
          getMethodLocationsFromExpression(mergeStatement.expression2, md)
        )

      case runAsStatement: RunAsStatement =>
        runAsStatement.expressions.foreach(exp =>
          methodRenameLocations.addAll(getMethodLocationsFromExpression(exp, md))
        )

        runAsStatement.block match {
          case Some(block: Block) =>
            methodRenameLocations.addAll(getLocationsFromStatements(block.statements(), md))
          case _ =>
        }
      case _ =>
    }

    methodRenameLocations
  }

  private def validateMethodCall(methodCall: MethodCallWithId): Option[MethodCallWithId] = {
    val sourceAndType = loadFullSourceAndType(methodCall.location.path, None)
    val validation = locateFromValidation(
      sourceAndType.get._2,
      methodCall.location.location.startLine,
      methodCall.location.location.startPosition
    )

    validation
      ._1(validation._2.getOrElse(return None))
      .cst match {
      case validatedMethodCall: MethodCallWithId => Some(validatedMethodCall)
      case _                                     => None
    }
  }

  private def getLocationFromMethodCall(
    methodCall: MethodCallWithId,
    md: ApexMethodDeclaration
  ): Option[Location] = {
    if (methodCall.cachedMethod.isEmpty) {
      val validatedMethodCall = validateMethodCall(methodCall)
      validatedMethodCall match {
        case Some(methodCall: MethodCallWithId) =>
          methodCall.getTargetLocationForMethodCallOut(md)
        case _ => None
      }
    } else {
      methodCall.getTargetLocationForMethodCallOut(md)
    }
  }

  private def getMethodLocationsFromExpression(
    expression: Expression,
    md: ApexMethodDeclaration
  ): mutable.Set[Location] = {
    val methodCallLocations: mutable.Set[Location] = mutable.Set.empty
    expression match {
      case methodCall: MethodCallWithId =>
        methodCall.arguments.foreach(exp =>
          methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
        )

        getLocationFromMethodCall(methodCall, md) match {
          case Some(l: Location) => methodCallLocations.add(l)
          case _                 =>
        }

      case dotExpression: DotExpressionWithMethod =>
        dotExpression.target match {
          case Some(exp: Expression) =>
            methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
          case _ =>
        }
        methodCallLocations.addAll(getMethodLocationsFromExpression(dotExpression.expression, md))

      case binaryExpression: BinaryExpression =>
        binaryExpression.rhs match {
          case exp: Expression =>
            methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
          case _ =>
        }
        binaryExpression.lhs match {
          case exp: Expression =>
            methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
          case _ =>
        }

      case primaryExpression: PrimaryExpression =>
        primaryExpression.primary match {
          case soql: SOQL =>
            soql.boundExpressions.foreach(exp =>
              methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
            )
          case sosl: SOSL =>
            sosl.boundExpressions.foreach(exp =>
              methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
            )
          case _ =>
        }

      case arrayExpression: ArrayExpression =>
        methodCallLocations.addAll(getMethodLocationsFromExpression(arrayExpression.expression, md))
        methodCallLocations.addAll(
          getMethodLocationsFromExpression(arrayExpression.arrayExpression, md)
        )

      case castExpression: CastExpression =>
        methodCallLocations.addAll(getMethodLocationsFromExpression(castExpression.expression, md))

      case newExpression: NewExpression =>
        newExpression.creator.creatorRest match {
          case Some(cr: SetOrListCreatorRest) =>
            cr.parts.foreach(exp =>
              methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
            )
          case Some(cr: ArrayCreatorRest) =>
            cr.indexExpression match {
              case Some(exp: Expression) =>
                methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
              case _ =>
            }
            cr.arrayInitializer match {
              case Some(initialiser) =>
                initialiser.expressions.foreach(exp =>
                  methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
                )
              case _ =>
            }
          case Some(cr: MapCreatorRest) =>
            cr.pairs.foreach(pair => {
              methodCallLocations.addAll(getMethodLocationsFromExpression(pair.from, md))
              methodCallLocations.addAll(getMethodLocationsFromExpression(pair.to, md))
            })
          case Some(cr: ClassCreatorRest) =>
            cr.arguments.foreach(exp =>
              methodCallLocations.addAll(getMethodLocationsFromExpression(exp, md))
            )
          case _ =>
        }

      case negationExpression: NegationExpression =>
        methodCallLocations.addAll(
          getMethodLocationsFromExpression(negationExpression.expression, md)
        )

      case subExpression: SubExpression =>
        methodCallLocations.addAll(getMethodLocationsFromExpression(subExpression.expression, md))

      case prefixExpression: PrefixExpression =>
        methodCallLocations.addAll(
          getMethodLocationsFromExpression(prefixExpression.expression, md)
        )

      case postfixExpression: PostfixExpression =>
        methodCallLocations.addAll(
          getMethodLocationsFromExpression(postfixExpression.expression, md)
        )

      case instanceOfExpression: InstanceOfExpression =>
        methodCallLocations.addAll(
          getMethodLocationsFromExpression(instanceOfExpression.expression, md)
        )

      case _ =>
    }
    methodCallLocations
  }

}
