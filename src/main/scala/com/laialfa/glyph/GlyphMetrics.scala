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
package com.laialfa.glyph

import scala.xml.{Node, Elem}


object GlyphMetrics {

  def fromXML(node: Node): GlyphMetrics = {
    val typeface: String = (node \ "typeface").text
    val ptSize: Int = (node \ "ptSize").text.toInt
    val height: Int = (node \ "height").text.toInt
    val ascent: Int = (node \ "ascent").text.toInt
    val descent: Int = (node \ "descent").text.toInt
    val numGlyphs: Int = (node \ "numGlyphs").text.toInt

    val codePoints: Seq[Int] =
      for {
        codePointNode: Node <- (node \ "codePoints" \ "codePoint")
        if codePointNode.label == "codePoint"
      } yield {
        codePointNode.text.toInt
      }

    val advances: Seq[Int] =
      for {
        codePointNode: Node <- (node \ "codePoints" \ "codePoint")
        if codePointNode.label == "codePoint"
      } yield {
        (codePointNode \ "@advance").text.toInt
      }

    GlyphMetrics(typeface, ptSize, height, ascent, descent, numGlyphs, codePoints.toArray, advances.toArray)
  }
}


/**
 * Contains information about the font, Unicode code points, and their
 * respective advances used to generate the GlyphSheet.
 *
 * @param typeface      font name
 * @param ptSize        ptSize used when generating glyphSheet
 * @param height        font height
 * @param ascent        above the baseline
 * @param descent       below the baseline (might not include leading)
 * @param numGlyphs     number of characters in the GlyphSheet - usually 256
 * @param codePoints    Unicode code points
 * @param advances      character widths in pixels
 */
case class GlyphMetrics(typeface: String,
                          ptSize: Int,
                          height: Int,
                          ascent: Int,
                         descent: Int,
                       numGlyphs: Int,
                      codePoints: Array[Int],
                        advances: Array[Int]) {

  def toXML: Elem = {
    <glyphMetrics>
      <typeface>{typeface}</typeface>
      <ptSize>{ptSize}</ptSize>
      <height>{height}</height>
      <ascent>{ascent}</ascent>
      <descent>{descent}</descent>
      <numGlyphs>{numGlyphs}</numGlyphs>
      <codePoints>
        {
          var i: Int = 0
          for (codePoint: Int <- codePoints) yield {
            // string attribute is more readable but it not used during load
            val hexFormat: String = "U+%x4".format(codePoint)
            val advance: Int = advances(i)
            i += 1
            <codePoint hex={hexFormat} advance={advance.toString}>{codePoint}</codePoint>
          }
        }
      </codePoints>
    </glyphMetrics>
  }
}
