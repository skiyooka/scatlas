/**
 * Copyright 2014  Sumio Kiyooka
 *
 * This file is part of Laialfa.
 *
 * Laialfa is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Laialfa is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Laialfa.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.laialfa

import org.apache.log4j.Logger


/**
 * Add to VM options:
 *   -Xdock:name=Scatlas (on mac)
 */
object Main {

  private val log = Logger.getLogger(getClass)

  private val APP_TITLE: String = "Scatlas"

  def main(args: Array[String]) {
    log.info("Hello world from Main")
  }
}

