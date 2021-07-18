/*
 Copyright (c) 2021 Kevin Jones, All rights reserved.
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
package com.nawforce.apexlink.types.other

import com.nawforce.apexlink.cst._
import com.nawforce.apexlink.diagnostics.IssueOps
import com.nawforce.apexlink.finding.TypeResolver
import com.nawforce.apexlink.org.{Module, OrgImpl}
import com.nawforce.apexlink.types.apex.ApexDeclaration
import com.nawforce.apexlink.types.core.{DependencyHolder, Dependent}
import com.nawforce.pkgforce.diagnostics.PathLocation
import com.nawforce.pkgforce.names.TypeName
import com.nawforce.pkgforce.stream.VFEvent

class VFContainer(module: Module, event: VFEvent) extends DependencyHolder {

  def validate(): Set[Dependent] = {
    val controllers = getControllers
    val controllerContexts = controllers.map(controller => {
      val context = new TypeVerifyContext(None, controller)
      context.addDependency(controller)
      context
    })

    controllerContexts
      .map(_.dependencies.toIterable)
      .foldLeft(Set.empty[Dependent])((acc, cur) => {
        acc ++ cur.toSet
      })
  }

  private def getControllers: Array[ApexDeclaration] = {
    event.controllers.flatMap(controller => {
      val controllerType = TypeName(controller.value)
      TypeResolver(controllerType, module) match {
        case Left(_) =>
          OrgImpl.log(
            IssueOps.noTypeDeclaration(PathLocation(event.sourceInfo.path.toString, controller.location),
                                       controllerType))
          None
        case Right(td: ApexDeclaration) => Some(td)
        case _                          => None
      }
    })
  }

}
