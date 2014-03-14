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
import scala.swing.{Frame, SimpleSwingApplication}


/**
 * Add to VM options:
 *   -Xdock:name=Scatlas (on mac)
 */
object Main extends SimpleSwingApplication {

  private val log: Logger = Logger.getLogger(getClass)

  private val APP_TITLE: String = "Scatlas"

  // On macs, use screen menubar instead of a menu inside the frame
  System.setProperty("apple.laf.useScreenMenuBar", "true")

  override def top: Frame = new AtlasFrame {
    title = APP_TITLE
  }
}
