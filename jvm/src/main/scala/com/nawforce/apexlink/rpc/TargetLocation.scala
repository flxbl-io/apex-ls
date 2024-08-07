/*
 * Copyright (c) 2022 FinancialForce.com, inc. All rights reserved
 */
package com.nawforce.apexlink.rpc

import com.nawforce.pkgforce.path.Location
import io.github.shogowada.scala.jsonrpc.serializers.JSONRPCPickler.{macroRW, ReadWriter => RW}

case class TargetLocation(targetPath: String, range: Location)

object TargetLocation {
  implicit val rw: RW[TargetLocation]   = macroRW
  implicit val rwLocation: RW[Location] = macroRW
}
