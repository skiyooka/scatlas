/**
 * Copyright 2024  Sumio Kiyooka
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

import com.typesafe.scalalogging.Logger
import scala.swing.Frame


/**
 * Add to VM options:
 *   -Xdock:name=Scatlas (on mac)
 */
object Main {
  private val log: Logger = Logger(getClass)

  private val AppTitle: String = "Scatlas"

  def main(args: Array[String]): Unit = {
    // On macs, use screen menubar instead of a menu inside the frame
    System.setProperty("apple.laf.useScreenMenuBar", "true")

    val frame: Frame = new AtlasFrame {
      title = AppTitle
    }

    frame.pack()
    frame.centerOnScreen()
    frame.open()
  }
}
