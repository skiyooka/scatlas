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
import java.awt.image.BufferedImage
import java.awt.{GraphicsConfiguration, FontMetrics, Color, RenderingHints}
import java.awt.{Transparency, Font, GraphicsEnvironment}
import scala.swing.Graphics2D


object GlyphSheet {

  /**
   * Generate translucent glyphs.  This is a pure function.
   *
   * Place all glyph textures onto a single sheet containing 16 x 16 = 256
   * glyphs:
   *
   *     0  1  2  3  4 ... 15
   *    16 17 18 19 20 ... 31
   *
   *   ...
   *
   *   240 ...            255
   *
   * @param typeface      font name
   * @param spriteSize    of each glyph square (e.g. 64)
   * @param antialias     true to smooth
   */
  def generate(typeface: String,
             spriteSize: Int,
              antialias: Boolean): GlyphSheet = {

    val graphics: GraphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment.
          getDefaultScreenDevice.getDefaultConfiguration

    val ptSize: Int = getMaxPtSize(graphics, typeface, spriteSize, 0)

    val numSpritesAlongEdge: Int = 16  // 256 total glyphs

    val sheetWidth: Int = numSpritesAlongEdge * spriteSize
    val sheetHeight: Int = numSpritesAlongEdge * spriteSize
    val spriteSheet: BufferedImage = graphics.createCompatibleImage(sheetWidth, sheetHeight, Transparency.TRANSLUCENT)

    // pre-calculate glyph widths in pixels
    val numGlyphs: Int = numSpritesAlongEdge * numSpritesAlongEdge
    val boundingRects: Array[Rect2D] = Array.ofDim(numGlyphs)
    val codePoints: Array[Int] = Array.ofDim(numGlyphs)
    val advances: Array[Int] = Array.ofDim(numGlyphs)

    val fontMetrics: FontMetrics = graphics.createCompatibleImage(1, 1).getGraphics
          .getFontMetrics(new Font(typeface, Font.PLAIN, ptSize))

    val g2: Graphics2D = spriteSheet.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    for (i: Int <- 0 until numGlyphs) {
      val col: Int = i % numSpritesAlongEdge
      val row: Int = i / numSpritesAlongEdge
      val ch: Char = i.toChar

      val sprite: BufferedImage = generateImage(graphics, typeface, ch.toString, ptSize, spriteSize, antialias=true)
      g2.drawImage(sprite, col * spriteSize, row * spriteSize, null)
      codePoints(i) = i
      boundingRects(i) = Rect2D(col * spriteSize, row * spriteSize, spriteSize, spriteSize)
      advances(i) = fontMetrics.charWidth(i)
    }
    g2.dispose()

    val metrics: GlyphMetrics = GlyphMetrics(typeface, ptSize, sheetWidth, sheetHeight, spriteSize,
      fontMetrics.getHeight, fontMetrics.getAscent, fontMetrics.getDescent,
      numGlyphs, codePoints, boundingRects, advances)

    GlyphSheet(metrics, spriteSheet)
  }

  /**
   * Generate a signed distance field.  This is a pure function.
   *
   * Depending on the parameters, this method can take quite a bit of time:
   *   upscale of 2 = tolerable speed, 8 = high quality but slow.
   *
   * Place all glyph textures onto a single sheet containing 16 x 16 = 256
   * glyphs:
   *
   *     0  1  2  3  4 ... 15
   *    16 17 18 19 20 ... 31
   *
   *   ...
   *
   *   240 ...            255
   *
   * @param typeface      font name
   * @param spriteSize    of each glyph square (e.g. 64)
   * @param upscale       scale factor of high-res upscale version (e.g. 8)
   * @param spread        spread from edge (pixels e.g. 4)
   */
  def generate(typeface: String,
             spriteSize: Int,
                upscale: Int,
                 spread: Int): GlyphSheet = {
    require(spriteSize * upscale <= 8192)  // don't allow anything too ridiculous

    val graphics: GraphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment.
        getDefaultScreenDevice.getDefaultConfiguration

    val ptSize: Int = getMaxPtSize(graphics, typeface, spriteSize, spread)

    val numSpritesAlongEdge: Int = 16  // 256 total glyphs

    val sheetWidth: Int = numSpritesAlongEdge * spriteSize
    val sheetHeight: Int = numSpritesAlongEdge * spriteSize
    val spriteSheet: BufferedImage = graphics.createCompatibleImage(sheetWidth, sheetHeight, Transparency.TRANSLUCENT)

    // pre-calculate glyph widths in pixels
    val numGlyphs: Int = numSpritesAlongEdge * numSpritesAlongEdge
    val boundingRects: Array[Rect2D] = Array.ofDim(numGlyphs)
    val codePoints: Array[Int] = Array.ofDim(numGlyphs)
    val advances: Array[Int] = Array.ofDim(numGlyphs)

    val fontMetrics: FontMetrics = graphics.createCompatibleImage(1, 1).getGraphics
        .getFontMetrics(new Font(typeface, Font.PLAIN, ptSize))

    val g2: Graphics2D = spriteSheet.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val upscalePtSize: Int = ptSize * upscale
    val upscaleLength: Int = spriteSize * upscale

    print("Generating " + numGlyphs + " glyphs...")
    for (i: Int <- 0 until numGlyphs) {
      print(" ")
      print(i)
      if (i % 32 == 0) {
        println()
      }
      val col: Int = i % numSpritesAlongEdge
      val row: Int = i / numSpritesAlongEdge
      val ch: Char = i.toChar

      val upscaleImage: BufferedImage = generateImage(graphics, typeface, ch.toString, upscalePtSize, upscaleLength, antialias=false)
      val sprite: BufferedImage = generateDistanceField(graphics, upscaleImage, upscale, spread, spriteSize)
      g2.drawImage(sprite, col * spriteSize, row * spriteSize, null)
      codePoints(i) = i
      boundingRects(i) = Rect2D(col * spriteSize, row * spriteSize, spriteSize, spriteSize)
      advances(i) = fontMetrics.charWidth(i)
    }
    println(" done!")
    g2.dispose()


    val metrics: GlyphMetrics = GlyphMetrics(typeface, ptSize, sheetWidth, sheetHeight, spriteSize,
        fontMetrics.getHeight, fontMetrics.getAscent, fontMetrics.getDescent,
        numGlyphs, codePoints, boundingRects, advances)

    GlyphSheet(metrics, spriteSheet)
  }

  /**
   * Generate heavily antialiased translucent glyphs from a signed distance
   * field suitable for downscaling only.
   *
   * This is a pure function.
   *
   * Depending on the parameters, this method can take quite a bit of time:
   *   upscale of 2 = tolerable speed, 8 = high quality but slow.
   *
   * Place all glyph textures onto a single sheet containing 16 x 16 = 256
   * glyphs:
   *
   *     0  1  2  3  4 ... 15
   *    16 17 18 19 20 ... 31
   *
   *   ...
   *
   *   240 ...            255
   *
   * @param typeface      font name
   * @param spriteSize    of each glyph square (e.g. 64)
   * @param upscale       scale factor of high-res upscale version (e.g. 8)
   * @param spread        spread from edge (pixels e.g. 4)
   */
  def generateDownscale(typeface: String,
                      spriteSize: Int,
                         upscale: Int,
                          spread: Int): GlyphSheet = {
    require(spriteSize * upscale <= 8192)  // don't allow anything too ridiculous

    val graphics: GraphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment.
        getDefaultScreenDevice.getDefaultConfiguration

    val ptSize: Int = getMaxPtSize(graphics, typeface, spriteSize, spread)

    val numSpritesAlongEdge: Int = 16  // 256 total glyphs

    val sheetWidth: Int = numSpritesAlongEdge * spriteSize
    val sheetHeight: Int = numSpritesAlongEdge * spriteSize
    val spriteSheet: BufferedImage = graphics.createCompatibleImage(sheetWidth, sheetHeight, Transparency.TRANSLUCENT)

    // pre-calculate glyph widths in pixels
    val numGlyphs: Int = numSpritesAlongEdge * numSpritesAlongEdge
    val boundingRects: Array[Rect2D] = Array.ofDim(numGlyphs)
    val codePoints: Array[Int] = Array.ofDim(numGlyphs)
    val advances: Array[Int] = Array.ofDim(numGlyphs)

    val fontMetrics: FontMetrics = graphics.createCompatibleImage(1, 1).getGraphics
        .getFontMetrics(new Font(typeface, Font.PLAIN, ptSize))

    val g2: Graphics2D = spriteSheet.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val upscalePtSize: Int = ptSize * upscale
    val upscaleLength: Int = spriteSize * upscale

    print("Generating " + numGlyphs + " glyphs...")
    for (i: Int <- 0 until numGlyphs) {
      print(" ")
      print(i)
      if (i % 32 == 0) {
        println()
      }
      val col: Int = i % numSpritesAlongEdge
      val row: Int = i / numSpritesAlongEdge
      val ch: Char = i.toChar

      val upscaleImage: BufferedImage = generateImage(graphics, typeface, ch.toString, upscalePtSize, upscaleLength, antialias=false)
      val sprite: BufferedImage = generateDownscaleFromDistanceField(graphics, upscaleImage, upscale, spread, spriteSize)
      g2.drawImage(sprite, col * spriteSize, row * spriteSize, null)

      codePoints(i) = i
      boundingRects(i) = Rect2D(col * spriteSize, row * spriteSize, spriteSize, spriteSize)
      advances(i) = fontMetrics.charWidth(i)
    }
    println(" done!")
    g2.dispose()


    val metrics: GlyphMetrics = GlyphMetrics(typeface, ptSize, sheetWidth, sheetHeight, spriteSize,
      fontMetrics.getHeight, fontMetrics.getAscent, fontMetrics.getDescent,
      numGlyphs, codePoints, boundingRects, advances)

    GlyphSheet(metrics, spriteSheet)
  }

  /////////////////////
  // private methods //
  /////////////////////

  /**
   * Create a square image of a glyph with the following properties:
   *   - antialiased
   *   - grey scale
   *   - alpha channel
   *
   * This is a pure function.
   *
   * @param graphics      graphics configuration
   * @param typeface      font
   * @param symbol        glyph to paint
   * @param ptSize        point size (usually from getMaxPtSize)
   * @param spriteSize    sprite square edge length
   * @param antialias     true to antialias
   *
   * @return BufferedImage
   */
  private def generateImage(graphics: GraphicsConfiguration,
                            typeface: String,
                              symbol: String,
                              ptSize: Int,
                          spriteSize: Int,
                           antialias: Boolean): BufferedImage = {
    // with TRANSLUCENT rgb is either 0 or 255 and alpha can vary between
    val bufferedImage: BufferedImage = graphics.createCompatibleImage(spriteSize, spriteSize, Transparency.TRANSLUCENT)
    val g2: Graphics2D = bufferedImage.createGraphics()

    if (antialias) {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }
    g2.setColor(Color.white)
    g2.setFont(new Font(typeface, Font.PLAIN, ptSize))

    val fm: FontMetrics = g2.getFontMetrics
    g2.drawString(symbol, spriteSize/2 - fm.stringWidth(symbol)/2, fm.getAscent)
    g2.dispose()

    bufferedImage
  }

  private case class Point(x: Int, y: Int)

  /**
   * This is a pure function.
   *
   * @param p1    point 1
   * @param p2    point 2
   *
   * @return square of the distance
   */
  private def squareDist(p1: Point, p2: Point): Int = {
    val dx: Int = p2.x - p1.x
    val dy: Int = p2.y - p1.y

    dx*dx + dy*dy
  }

  /**
   * Convert signed distance to ARGB.  This is a pure function.
   *
   * @param signedDistance    of a pixel from the glyph
   * @param spread            typically 4
   *
   * @return ARGB value suitable for BufferedImage.setRGB
   */
  private def signedDistanceAsARGB(signedDistance: Double, spread: Int): Int = {
    val alpha: Double = 0.5 + 0.5 * (signedDistance / spread)

    val clampedAlpha: Double = math.min(1.0, math.max(0.0, alpha))

    val alphaByte: Int = (clampedAlpha * 0xff).toInt

    (alphaByte << 24) | 0xffffff  // alpha only as glyph is white
  }

  /**
   * Convert signed distance to antialiased ARGB for downscaling only.
   *
   * This is a pure function.
   *
   * @param signedDistance    of a pixel from the glyph
   * @param spread            typically 4
   *
   * @return ARGB value suitable for BufferedImage.setRGB
   */
  private def downscaleFromSignedDistanceAsARGB(signedDistance: Double, spread: Int): Int = {
    val alpha: Double = 0.6 + (signedDistance / spread)

    val clampedAlpha: Double = math.min(1.0, math.max(0.0, alpha))

    val alphaByte: Int = (clampedAlpha * 0xff).toInt

    (alphaByte << 24) | 0xffffff  // alpha only as glyph is white
  }

  /**
   * Brute-force calculate the signed distance.  This is a pure function.
   *
   * @param p          point
   * @param bitmap     2D matrix (y,x) with true = white (i.e. inside glyph)
   * @param upscale    typically 8
   * @param spread     typically 4
   *
   * @return signed distance with units = num pixels
   */
  private def findSignedDistance(p: Point,
                            bitmap: Array[Array[Boolean]],
                           upscale: Int,
                            spread: Int): Double = {

    val width: Int = bitmap(0).length
    val height: Int = bitmap.length

    require(p.x >= 0 && p.x < width)
    require(p.y >= 0 && p.y < height)

    // determine search extent
    val maxDistance: Int = upscale * spread

    val startX: Int = math.max(0, p.x - maxDistance)
    val endX: Int = math.min(width - 1, p.x + maxDistance)
    val startY: Int = math.max(0, p.y - maxDistance)
    val endY: Int = math.min(height - 1, p.y + maxDistance)

    var closestSquareDistance: Int = maxDistance * maxDistance

    val inside: Boolean = bitmap(p.y)(p.x)

    // search for closest pixel that is opposite
    for (y: Int <- startY to endY) {
      for (x: Int <- startX to endX) {
        if (inside != bitmap(y)(x)) {
          val squareDistance: Int = squareDist(p, Point(x, y))
          if (squareDistance < closestSquareDistance) {
            closestSquareDistance = squareDistance
          }
        }
      }
    }

    val closestDistance: Double = math.sqrt(closestSquareDistance)
    if (inside) {
      closestDistance / upscale
    } else {
      -closestDistance / upscale
    }
  }

  /**
   * Generate distance field for spriteSize from larger sample.
   *
   * This is a pure function.
   *
   * @param graphics      graphics configuration
   * @param image         binary input image with black=transparent and
   *                      white=opaque
   * @param upscale       scale factor of high-res upscale version (e.g. 8)
   * @param spread        spread from edge (pixels e.g. 4)
   * @param spriteSize    resulting sprite size (pixels)
   *
   * @return distance field
   */
  private def generateDistanceField(graphics: GraphicsConfiguration,
                                       image: BufferedImage,
                                     upscale: Int,
                                      spread: Int,
                                  spriteSize: Int): BufferedImage = {
    require(spriteSize * upscale == image.getWidth)
    require(spriteSize * upscale == image.getHeight)

    val output: BufferedImage = graphics.createCompatibleImage(spriteSize, spriteSize, Transparency.TRANSLUCENT)

    // note coordinates y,x
    val bitmap: Array[Array[Boolean]] = Array.ofDim(image.getHeight, image.getWidth)
    for (y: Int <- 0 until image.getHeight) {
      for (x: Int <- 0 until image.getWidth) {
        bitmap(y)(x) = (image.getRGB(x, y) & 0x808080) != 0
      }
    }

    for (y: Int <- 0 until spriteSize) {
      for (x: Int <- 0 until spriteSize) {
        val signedDistance: Double = findSignedDistance(
          Point((x * upscale) + (upscale / 2), (y * upscale) + (upscale / 2)),
          bitmap, upscale, spread)
        val argb: Int = signedDistanceAsARGB(signedDistance, spread)
        output.setRGB(x, y, argb)
      }
    }

    output
  }

  /**
   * Generate antialiased translucent glyph from signed distance field for
   * spriteSize from larger sample.
   *
   * This is a pure function.
   *
   * @param graphics      graphics configuration
   * @param image         binary input image with black=transparent and
   *                      white=opaque
   * @param upscale       scale factor of high-res upscale version (e.g. 8)
   * @param spread        spread from edge (pixels e.g. 4)
   * @param spriteSize    resulting sprite size (pixels)
   *
   * @return distance field
   */
  private def generateDownscaleFromDistanceField(graphics: GraphicsConfiguration,
                                                    image: BufferedImage,
                                                  upscale: Int,
                                                   spread: Int,
                                               spriteSize: Int): BufferedImage = {
    require(spriteSize * upscale == image.getWidth)
    require(spriteSize * upscale == image.getHeight)

    val output: BufferedImage = graphics.createCompatibleImage(spriteSize, spriteSize, Transparency.TRANSLUCENT)

    // note coordinates y,x
    val bitmap: Array[Array[Boolean]] = Array.ofDim(image.getHeight, image.getWidth)
    for (y: Int <- 0 until image.getHeight) {
      for (x: Int <- 0 until image.getWidth) {
        bitmap(y)(x) = (image.getRGB(x, y) & 0x808080) != 0
      }
    }

    for (y: Int <- 0 until spriteSize) {
      for (x: Int <- 0 until spriteSize) {
        val signedDistance: Double = findSignedDistance(
          Point((x * upscale) + (upscale / 2), (y * upscale) + (upscale / 2)),
          bitmap, upscale, spread)
        val argb: Int = downscaleFromSignedDistanceAsARGB(signedDistance, spread)
        output.setRGB(x, y, argb)
      }
    }

    output
  }

  /**
   * This is a pure function.
   *
   * @param graphics      graphics configuration
   * @param typeface      font
   * @param spriteSize    sprite square edge length
   * @param spread        > 0 for signed distance fields
   *
   * @return maximum pt size that will fit in a square image of given size
   */
  private def getMaxPtSize(graphics: GraphicsConfiguration,
                           typeface: String,
                         spriteSize: Int,
                             spread: Int): Int = {
    val image: BufferedImage = graphics.createCompatibleImage(spriteSize, spriteSize)
    val g2: Graphics2D = image.getGraphics.asInstanceOf[Graphics2D]

    var largestPtSize: Int = spriteSize * 2  // starting size

    // shrink until fit
    var fits: Boolean = false
    while (!fits) {
      largestPtSize -= 1
      if (largestPtSize == 0) {
        throw new RuntimeException("largestPtSize is 0")
      }
      val fontMetrics = g2.getFontMetrics(new Font(typeface, Font.PLAIN, largestPtSize))
      fits = fontMetrics.getHeight + spread <= spriteSize
    }
    g2.dispose()

    largestPtSize
  }
}


/**
 * A spritesheet containing glyphs for drawing onto an OpenGL canvas as
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
 *
 * @param metrics    GlyphMetrics used during generation
 * @param image      actual atlas/sprite sheet
 */
case class GlyphSheet(metrics: GlyphMetrics, image: BufferedImage) {

  /** @return width in pixels */
  def width: Int = image.getWidth

  /** @return height in pixels */
  def height: Int = image.getHeight
}
