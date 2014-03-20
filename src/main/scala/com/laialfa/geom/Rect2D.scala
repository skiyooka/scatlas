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
package com.laialfa.geom

import scala.xml.{Elem, Node}


object Rect2D {

  def fromXML(node: Node): Rect2D = {
    assert(node.label == "rect2d")

    val x: Int = (node \ "@x").text.toInt
    val y: Int = (node \ "@y").text.toInt
    val width: Int = (node \ "@width").text.toInt
    val height: Int = (node \ "@height").text.toInt

    Rect2D(x, y, width, height)
  }
}


/**
 * Rectangle in 2D space.
 *
 * @param x         coordinate of anchor point
 * @param y         coordinate of anchor point
 * @param width     of rect
 * @param height    of rect
 */
case class Rect2D(x: Int, y: Int, width: Int, height: Int) {

  def toXML: Elem = {
      <rect2d x={x.toString} y={y.toString} width={width.toString} height={height.toString}/>
  }

  override def toString: String = {
    "[%d %d %d %d]".format(x, y, width, height)
  }
}
