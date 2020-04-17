package intent.macros

/*
 * The Position macro is based on code in ScalaTest (https://github.com/scalatest/scalatest)
 * with the following license information:

 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import scala.quoted._

/**
 * Represents a position in test code. Used to get the position of an
 * `expect` call.
 *
 * @param filePath the full path to the file that contains the `expect`
 * @param lineNumber0 zero-based line number of the `expect` call
 * @param columnNumber0 zero-based column number of the `expect` call
 */
case class Position(filePath: String, lineNumber0: Int, columnNumber0: Int)

/**
 * Companion object for <code>Position</code> that defines an implicit
 * method that uses a macro to grab the enclosing position.
 */
object Position:

  /**
   * Implicit method, implemented with a macro, that returns the enclosing
   * source position where it is invoked.
   *
   * @return the enclosing source position
   */
  implicit inline def here: Position = ${ genPosition }

  /**
   * Helper method for Position macro.
   */
  private def genPosition(implicit qctx: QuoteContext): Expr[Position] =
    import qctx.tasty._

    val file = rootPosition.sourceFile
    val filePath: String = file.toString
    val lineNo: Int = rootPosition.startLine
    val colNo: Int = rootPosition.startColumn
    '{ Position(${Expr(filePath)}, ${Expr(lineNo)}, ${Expr(colNo)}) }
