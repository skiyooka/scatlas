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

import java.awt.image.BufferedImage
import java.awt.{GraphicsConfiguration, FontMetrics, Color, RenderingHints, Transparency, Font, GraphicsEnvironment}
import java.nio.ByteBuffer

import scala.swing.Graphics2D


/**
 * Generate antialiased grey-scale java images suitable for creating OpenGL
 * textures.
 *
 * The smallest square on a touchscreen device that is easy to press with the
 * tip of a finger is about 9.2 mm (0.3622 inch).
 *
 * Cited from:
 * Target Size Study for One-Handed Thumb Use on Small Touchscreen Devices
 * - Parhi, Karlson, & Bederson 2006.
 *
 * PPI:
 * My MacBook Pro: 128
 * iPad: 132
 *
 * 0.3622 in * 128 pixels/inch = 46.3616 pixels
 *
 * The distance between the centers of 2 hexes = w * sqrt(3.0).  If this
 * distance is ideally 46.3616 pixels (as calculated above) then w0 = 26.769
 *
 * From some testing, a 36 pt sans-serif font works well for filling this hex.
 *
 * Thus a simple approximation from the 36 pt font:
 * w0 = font metric ascent * 0.75 = 26.25 (close to 26.769)
 */
class GlyphSheet(val spriteSize: Int = 64) {

  private val gc: GraphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice.getDefaultConfiguration

  private var ptSize = 0
  private var fontMetrics: FontMetrics = null
  private var image: BufferedImage = null

  val NUM_SPRITES_ALONG_EDGE = 16

  /**
   * @return maximum pt size used for a given tile
   */
  def getPtSize: Int = {
    if (ptSize == 0) {
      ptSize = getMaxPtSize(spriteSize)
    }
    ptSize
  }

  /**
   * @return font metrics representing getPtSize()
   */
  def getFontMetrics: FontMetrics = {
    if (fontMetrics == null) {
      val graphics = gc.createCompatibleImage(1, 1).getGraphics
      fontMetrics = graphics.getFontMetrics(new Font("SansSerif", Font.PLAIN, getPtSize))
    }
    fontMetrics
  }

  /**
   * @return the actual sprite sheet
   */
  def getImage: BufferedImage = {
    if (image == null) {
      image = createSpriteSheet(getPtSize, NUM_SPRITES_ALONG_EDGE, spriteSize)
    }
    image
  }

  // pre-calculate glyph widths in pixels
  private val numGlyphs = NUM_SPRITES_ALONG_EDGE * NUM_SPRITES_ALONG_EDGE
  private val glyphWidths: Array[Int] = Array.ofDim(numGlyphs)
  for (i <- 0 until numGlyphs) {
    glyphWidths(i) = getFontMetrics.charWidth(i)
  }

  /**
   * @return width in pixels of given glyph index
   */
  def getGlyphWidth(index: Int): Int = {
    glyphWidths(index)
  }

  /**
   * @return OpenGL glyph size on sprite sheet where 1.0 = entire texture
   */
  def getIncr: Float = {
    (1.0 / NUM_SPRITES_ALONG_EDGE).toFloat
  }

  /**
   * @param size    sprite square edge length
   *
   * @return maximum pt size that will fit in a square image of given size
   */
  private def getMaxPtSize(size: Int): Int = {
    val g2 = gc.createCompatibleImage(size, size).getGraphics.asInstanceOf[Graphics2D]

    var max_pt_size = size * 2  // starting size

    var fits = false
    while (!fits) {
      max_pt_size -= 1
      if (max_pt_size == 0) {
        throw new RuntimeException("max_pt_size is 0")
      }
      val fontMetrics = g2.getFontMetrics(new Font("SansSerif", Font.PLAIN, max_pt_size))
      fits = fontMetrics.getHeight <= size
    }
    g2.dispose()

    max_pt_size
  }

  /**
   * Create a square image of a glyph with the following properties:
   *   - antialiased
   *   - grey scale
   *   - alpha channel
   *
   * @param symbol          glyph to paint
   * @param ptSize          point size (usually from getMaxPtSize)
   * @param spriteSize      sprite square edge length
   *
   * @return BufferedImage
   */
  private def createImage(symbol: String, ptSize: Int, spriteSize: Int): BufferedImage = {
    // with TRANSLUCENT rgb is either 0 or 255 and alpha can vary between
    val bufferedImage = gc.createCompatibleImage(spriteSize, spriteSize, Transparency.TRANSLUCENT)
    val g2: Graphics2D = bufferedImage.createGraphics()

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setColor(Color.white)
    g2.setFont(new Font("SansSerif", Font.PLAIN, ptSize))

    val fm = g2.getFontMetrics
    g2.drawString(symbol, spriteSize/2 - fm.stringWidth(symbol)/2, fm.getAscent)
    g2.dispose()

    bufferedImage
  }

  /**
   * Place all glyph textures onto a single sheet containing 16 x 16 = 256
   * glyphs:
   *
   * 240 ...            255
   *
   * ...
   *
   *  16 17 18 19 20 ... 31
   *   0  1  2  3  4 ... 15
   *
   * This is the ordering that OpenGL expects.
   *
   * @param ptSize                 point size (usually from getMaxPtSize)
   * @param numSpritesAlongEdge    number of sprites on one edge of the sheet
   * @param spriteSize             square edge length of a single sprite/glyph
   */
  private def createSpriteSheet(ptSize: Int, numSpritesAlongEdge: Int, spriteSize: Int): BufferedImage = {
    val length = numSpritesAlongEdge * spriteSize
    val spriteSheet = gc.createCompatibleImage(length, length, Transparency.TRANSLUCENT)
    val g2: Graphics2D = spriteSheet.createGraphics()

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    for (i <- 0 to numSpritesAlongEdge * numSpritesAlongEdge) {
      val col = i % numSpritesAlongEdge
      val row = i / numSpritesAlongEdge
      val ch = i.toChar

      g2.drawImage(createImage(ch.toString, ptSize, spriteSize),
        col * spriteSize,
        (numSpritesAlongEdge - 1 - row) * spriteSize, null)
    }
    g2.dispose()

    spriteSheet
  }

  /**
   * Create a byte buffer of TYPE_INT_ARGB that is suitable for glTexImage2D
   * or gluBuild2DMipmaps.
   */
  def createByteBuffer(bi: BufferedImage): ByteBuffer = {
    val pixels: ByteBuffer = ByteBuffer.allocate(bi.getWidth * bi.getHeight * 4)

    // OpenGL pixel buffers start on the bottom row and work upwards i.e. the
    // origin is in the bottom left corner.
    for (y <- 0 until bi.getHeight) {
      for (x <- 0 until bi.getWidth) {
        val argb = bi.getRGB(x, bi.getHeight - 1 - y)  // TYPE_INT_ARGB

        val alpha: Byte = ((argb >> 24) & 0xff).toByte
        val   red: Byte = ((argb >> 16) & 0xff).toByte
        val green: Byte = ((argb >>  8) & 0xff).toByte
        val  blue: Byte =         (argb & 0xff).toByte

        // OpenGL texture takes RGBA
        pixels.put(red)
        pixels.put(green)
        pixels.put(blue)
        pixels.put(alpha)
      }
    }

    pixels.rewind()
    pixels
  }
}
