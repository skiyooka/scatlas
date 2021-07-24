/**
 * Copyright 2021  Sumio Kiyooka
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

import com.laialfa.geom.Rect2D
import scala.xml.{Node, Elem}


object GlyphMetrics {

  def fromXML(node: Node): GlyphMetrics = {
    val typeface: String = (node \ "typeface").text
    val ptSize: Int = (node \ "ptSize").text.toInt
    val spriteSize: Int = (node \ "spriteSize").text.toInt
    val height: Int = (node \ "height").text.toInt
    val ascent: Int = (node \ "ascent").text.toInt
    val descent: Int = (node \ "descent").text.toInt
    val numGlyphs: Int = (node \ "numGlyphs").text.toInt

    val codePoints: Seq[Int] =
      for {
        glyphNode: Node <- (node \ "glyphs" \ "glyph")
        if glyphNode.label == "glyph"
      } yield {
        val hexFormat: String = (glyphNode \ "@codePoint").text  // e.g. U+0020
        Integer.parseInt(hexFormat.substring(2), 16)
      }

    // The rects are within this class instead of GlyphSheet in order to
    // consolidate loading/saving to one class.
    val boundingRects: Seq[Rect2D] =
      for {
        glyphNode: Node <- (node \ "glyphs" \ "glyph")
        if glyphNode.label == "glyph"
      } yield {
        val x: Int = (glyphNode \ "@x").text.toInt
        val y: Int = (glyphNode \ "@y").text.toInt
        val width: Int = (glyphNode \ "@width").text.toInt
        val height: Int = (glyphNode \ "@height").text.toInt
        Rect2D(x, y, width, height)
      }

    val advances: Seq[Int] =
      for {
        glyphNode: Node <- (node \ "glyphs" \ "glyph")
        if glyphNode.label == "glyph"
      } yield {
        (glyphNode \ "@advance").text.toInt
      }

    GlyphMetrics(typeface, ptSize, spriteSize, height, ascent, descent,
      numGlyphs, codePoints.toArray, boundingRects.toArray, advances.toArray)
  }
}


/**
 * Contains information about the font, Unicode code points, and their
 * respective advances used to generate the GlyphSheet.
 *
 * @param typeface         font name
 * @param ptSize           ptSize used when generating glyphSheet
 * @param spriteSize       length of square edge (pixels)
 * @param height           font height
 * @param ascent           above the baseline
 * @param descent          below the baseline (might not include leading)
 * @param numGlyphs        number of characters in the GlyphSheet - usually 256
 * @param codePoints       Unicode code points
 * @param boundingRects    location on GlyphSheet
 * @param advances         character widths in pixels
 */
case class GlyphMetrics(typeface: String,
                          ptSize: Int,
                      spriteSize: Int,
                          height: Int,
                          ascent: Int,
                         descent: Int,
                       numGlyphs: Int,
                      codePoints: Array[Int],
                   boundingRects: Array[Rect2D],
                        advances: Array[Int]) {

  def toXML: Elem = {
    <glyphMetrics>
      <typeface>{typeface}</typeface>
      <ptSize>{ptSize}</ptSize>
      <spriteSize>{spriteSize}</spriteSize>
      <height>{height}</height>
      <ascent>{ascent}</ascent>
      <descent>{descent}</descent>
      <numGlyphs>{numGlyphs}</numGlyphs>
      <glyphs>
        {
          for (i: Int <- 0 until numGlyphs) yield {
            val hexFormat: String = "U+%04x".format(codePoints(i))
            val advance: Int = advances(i)
            val rect: Rect2D = boundingRects(i)
            <glyph index={i.toString}
               codePoint={hexFormat}
                       x={rect.x.toString}
                       y={rect.y.toString}
                   width={rect.width.toString}
                  height={rect.height.toString}
                 advance={advance.toString}/>
          }
        }
      </glyphs>
    </glyphMetrics>
  }
}
