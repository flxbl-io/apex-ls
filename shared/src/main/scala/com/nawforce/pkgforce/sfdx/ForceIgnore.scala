/*
 Copyright (c) 2020 Kevin Jones, All rights reserved.
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
package com.nawforce.pkgforce.sfdx

import com.nawforce.pkgforce.diagnostics._
import com.nawforce.pkgforce.path.{Location, PathLike}
import com.nawforce.runtime.platform.Path

import java.util.regex.{Matcher, Pattern}
import scala.collection.compat.immutable.ArraySeq
import scala.collection.mutable

class ForceIgnore(rootPath: PathLike, ignoreRules: Seq[IgnoreRule]) {
  private val rootPathNative = rootPath.toString
  private val rootPathLength = {
    rootPathNative.length + (if (rootPathNative.endsWith(Path.separator)) 0 else 1)
  }

  def includeDirectory(path: PathLike): Boolean = {
    include(path, directory = true)
  }

  def includeFile(path: PathLike): Boolean = {
    include(path, directory = false)
  }

  private def include(path: PathLike, directory: Boolean): Boolean = {
    if (!rootPath.isParentOf(path))
      return false

    val relativePath = path.toString.substring(rootPathLength)
    var include      = true
    ignoreRules.foreach(rule => {
      if (directory || !rule.dirOnly) {
        if (include != rule.negation) {
          try {
            rule.matcher.reset(relativePath)
            if (rule.matcher.matches()) {
              include = !include
            }
          } catch {
            case ex: Throwable =>
              // Catch JVM bug exception and log, see https://github.com/financialforcedev/ff-apex-ls/issues/170
              LoggerOps.info(s"force ignore regex '${rule.regex}' on path '$relativePath'", ex)
          }
        }
      }
    })
    include
  }
}

object ForceIgnore {
  def apply(path: PathLike): IssuesAnd[Option[ForceIgnore]] = {
    if (path.isFile) {
      path.read() match {
        case Left(err) =>
          IssuesAnd(ArraySeq(Issue(path, Diagnostic(ERROR_CATEGORY, Location.empty, err))), None)
        case Right(data) =>
          IssuesAnd(ArraySeq(), Some(new ForceIgnore(path.parent, IgnoreRule.read(data))))
      }
    } else {
      IssuesAnd(None)
    }
  }
}

case class IgnoreRule(dirOnly: Boolean, negation: Boolean, pattern: String) {

  lazy val matcher: Matcher = Pattern.compile(regex).matcher("")

  // Convert a pattern to a regex
  // See https://github.com/snark/ignorance/blob/master/ignorance/utils.py for reference
  lazy val regex: String = {
    val builder = new mutable.StringBuilder()
    var i       = 0
    val n       = pattern.length
    while (i < n) {
      val c = pattern(i)
      i = i + 1
      if (c == '*') {
        if (i < n && pattern(i) == '*') {
          i = i + 1
          builder.append(".*")
          if (i < n && pattern(i) == '/') {
            i = i + 1
            builder.append(IgnoreRule.escape(Path.separator) + "?")
          }
        } else {
          builder.append(IgnoreRule.nonSep + '*')
        }
      } else if (c == '?') {
        builder.append(IgnoreRule.nonSep)
      } else if (c == '/') {
        builder.append(IgnoreRule.escape(Path.separator))
      } else if (c == '[') {
        var j = i
        if (j < n && pattern(j) == '!')
          j = j + 1
        if (j < n && pattern(j) == ']')
          j = j + 1
        while (j <= n && pattern(j) != ']') {
          j = j + 1
        }
        if (j >= n)
          builder.append("\\[")
        else {
          var stuff = pattern.substring(i, j).replace("\\", "\\\\")
          i = j + 1
          if (stuff(0) == '!')
            stuff = s"^ ${stuff.substring(1)}"
          else if (stuff(0) == '^')
            stuff = s"\\ ${stuff.substring(1)}"
          builder.append(s"[$stuff]")
        }
      } else {
        builder.append(IgnoreRule.escapeChar(c))
      }
    }
    builder.append("$")
    builder.toString()
  }
}

object IgnoreRule {
  val nonSep: String = "[^" + escape(Path.separator) + "]"

  /*
   * Read rules from ignore file
   * See https://github.com/snark/ignorance/blob/master/ignorance/git.py for reference
   */
  def read(content: String): Seq[IgnoreRule] = {
    content
      .split("\n")
      .map(_.trim)
      .filter(_.nonEmpty)
      .filterNot(_.startsWith("#"))
      .filterNot(_.contains("***"))
      .filter(l => l.split("\\*\\*", -1).length == l.split("/\\*\\*/", -1).length)
      .filterNot(_ == "/")
      .map(l => {
        var pattern  = l
        var dirOnly  = pattern.endsWith("/")
        val negation = pattern.startsWith("!")
        if (negation)
          pattern = pattern.substring(1)
        if (pattern.startsWith("/"))
          pattern = pattern.substring(1)
        if (pattern.startsWith("**")) {
          dirOnly = false
          pattern = pattern.substring(2)
        }
        if (pattern.startsWith("/"))
          pattern = pattern.substring(1)
        if (pattern.endsWith("/"))
          pattern = pattern.substring(0, pattern.length - 1)
        IgnoreRule(dirOnly, negation, pattern)
      })
      .toIndexedSeq
  }

  def escape(s: String): String = {
    s.map(escapeChar).mkString
  }

  def escapeChar(c: Char): String = {
    c match {
      case '-'  => "\\-"
      case '/'  => "\\/"
      case '\\' => "\\\\"
      case '{'  => "\\{"
      case '}'  => "\\}"
      case '('  => "\\("
      case ')'  => "\\)"
      case '*'  => "\\*"
      case '+'  => "\\+"
      case '?'  => "\\?"
      case '.'  => "\\."
      case ','  => "\\,"
      case '^'  => "\\^"
      case '$'  => "\\$"
      case '|'  => "\\|"
      case '#'  => "\\#"
      case _    => c.toString
    }
  }
}
