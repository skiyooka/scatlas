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

import com.laialfa.glyph.GlyphSheet
import java.awt.{Dimension, Font, Graphics2D, Color}
import scala.swing.{Panel, MainFrame}


class AtlasFrame extends MainFrame {

  background = Color.black
  preferredSize = new Dimension(800, 800)
  visible = true

  private val glyphSheet = new GlyphSheet()

  /**
   * From the JDK source: the advance of a String is not necessarily the
   * sum of the advances of its characters.
   *
   * @param g         graphic context
   * @param text      text to draw
   * @param color     color
   * @param ptSize    point size
   * @param x         screen coordinate (left side baseline)
   * @param y         screen coordinate (baseline)
   */
  private def drawString(g: Graphics2D, text: String, color: Color, ptSize: Int, x: Int, y: Int) {
    val scaleFactor = ptSize.toDouble / glyphSheet.getPtSize

    val destDimen: Int = math.round(scaleFactor * glyphSheet.spriteSize).toInt
    val destAscent: Int = math.round(scaleFactor * glyphSheet.getFontMetrics.getAscent).toInt

    var unscaledAnchorX = 0  // will advance each iteration
    val yDest = y - destAscent  // constant (will be -ve for OpenGL)

    for (charPos <- 0 until text.length()) {
      val ch = text.charAt(charPos)
      val unscaledCharWidth = glyphSheet.getGlyphWidth(ch.toInt)

      val col = ch % glyphSheet.NUM_SPRITES_ALONG_EDGE
      val row = ch / glyphSheet.NUM_SPRITES_ALONG_EDGE

      // copy template from sprite sheet
      val xSrc = col * glyphSheet.spriteSize
      // this craziness because 0 is bottom of image in OpenGL
      val ySrc = (glyphSheet.NUM_SPRITES_ALONG_EDGE - 1 - row) * glyphSheet.spriteSize

      val shim = (glyphSheet.spriteSize - unscaledCharWidth) / 2.0

      val xDest = x + math.round(scaleFactor * (unscaledAnchorX - shim)).toInt

      g.drawImage(glyphSheet.getImage,
        xDest,
        yDest,
        xDest + destDimen,
        yDest + destDimen,

        xSrc,
        ySrc,
        xSrc + glyphSheet.spriteSize,
        ySrc + glyphSheet.spriteSize,
        null)

      unscaledAnchorX += unscaledCharWidth  // horizontal advance includes spacing
    }
  }

  /**
   * String width in pixels using pre-calculated metrics.
   */
  private def stringWidth(g: Graphics2D, text: String, ptSize: Int): Int = {
    var sum = 0

    for (charPos <- 0 until text.length()) {
      sum += glyphSheet.getGlyphWidth(text.charAt(charPos))
    }

    math.round((ptSize.toDouble / glyphSheet.getPtSize) * sum).toInt
  }

  /**
   * Font metrics version for comparison purposes.
   */
  private def drawStringJava(g: Graphics2D, text: String, color: Color, ptSize: Int, x: Int, y: Int) {
    g.setColor(color)
    g.setFont(new Font("SansSerif", Font.PLAIN, ptSize))
    g.drawString(text, x, y)
  }

  /**
   * Graphics2D version for comparison purposes.
   */
  private def stringWidthJava(g: Graphics2D, text: String, ptSize: Int): Int = {
    val fm = g.getFontMetrics(new Font("SansSerif", Font.PLAIN, ptSize))
    fm.stringWidth(text)
  }

  contents = new Panel {
    override def paint(g: Graphics2D) {
      g.setColor(Color.white)

      for (i <- 0 to glyphSheet.NUM_SPRITES_ALONG_EDGE * glyphSheet.NUM_SPRITES_ALONG_EDGE) {
        val col = i % glyphSheet.NUM_SPRITES_ALONG_EDGE
        val row = i / glyphSheet.NUM_SPRITES_ALONG_EDGE
        g.drawRect(col * glyphSheet.spriteSize,
          (glyphSheet.NUM_SPRITES_ALONG_EDGE - 1 - row) * glyphSheet.spriteSize,
          glyphSheet.spriteSize,
          glyphSheet.spriteSize)
      }
      g.drawImage(glyphSheet.getImage, 0, 0, null)

      var ptSize = glyphSheet.getPtSize
      var message = "The quick brown fox"
      drawString(g, message, Color.green, ptSize, 100, 64)
      drawStringJava(g, message, Color.green, ptSize, 100, 128)

      drawStringJava(g, "width " +     stringWidth(g, message, ptSize), Color.green, 12, 800, 64)
      drawStringJava(g, "width " + stringWidthJava(g, message, ptSize), Color.green, 12, 800, 128)

      ptSize = 12
      message = "jumped over the lazy dogs!  The advance of a String is not necessarily the sum of the advances of its characters."
      drawString(g, message, Color.green, ptSize, 100, 192)
      drawStringJava(g, message, Color.green, ptSize, 100, 256)

      drawStringJava(g, "width " +     stringWidth(g, message, ptSize), Color.green, 12, 800, 192)
      drawStringJava(g, "width " + stringWidthJava(g, message, ptSize), Color.green, 12, 800, 256)

      ptSize = 108
      message = "Bigger bigger"
      drawString(g, message, Color.green, ptSize, 100, 384)
      drawStringJava(g, message, Color.green, ptSize, 100, 512)

      drawStringJava(g, "width " +     stringWidth(g, message, ptSize), Color.green, 12, 800, 384)
      drawStringJava(g, "width " + stringWidthJava(g, message, ptSize), Color.green, 12, 800, 512)
    }
  }
}
