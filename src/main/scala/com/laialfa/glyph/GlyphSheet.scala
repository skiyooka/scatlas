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
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
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
    val glyphs: Array[GlyphInfo] = Array.ofDim(numGlyphs)

    val fontMetrics: FontMetrics = graphics.createCompatibleImage(1, 1).getGraphics
        .getFontMetrics(new Font(typeface, Font.PLAIN, ptSize))

    val g2: Graphics2D = spriteSheet.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    for (i: Int <- 0 until numGlyphs) {
      val col: Int = i % numSpritesAlongEdge
      val row: Int = i / numSpritesAlongEdge
      val ch: Char = this.lookupChar(i)

      val sprite: BufferedImage = generateImage(graphics, typeface, ch.toString, ptSize, spriteSize, antialias=true)
      g2.drawImage(sprite, col * spriteSize, row * spriteSize, null)

      val codePoint = ch.toInt
      val boundingRect = Rect2D(col * spriteSize, row * spriteSize, spriteSize, spriteSize)
      val advance = fontMetrics.charWidth(codePoint)
      glyphs(i) = GlyphInfo(i, codePoint, boundingRect, advance)
    }
    g2.dispose()

    val metrics: GlyphMetrics = GlyphMetrics(typeface, ptSize, sheetWidth, sheetHeight, spriteSize,
      fontMetrics.getHeight, fontMetrics.getAscent, fontMetrics.getDescent,
      numGlyphs, glyphs)

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
    val glyphs: Array[GlyphInfo] = Array.ofDim(numGlyphs)

    val fontMetrics: FontMetrics = graphics.createCompatibleImage(1, 1).getGraphics
        .getFontMetrics(new Font(typeface, Font.PLAIN, ptSize))

    val g2: Graphics2D = spriteSheet.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val upscalePtSize: Int = ptSize * upscale
    val upscaleLength: Int = spriteSize * upscale

    println("Generating " + numGlyphs + " glyphs...")
    for (i: Int <- 0 until numGlyphs) {
      print("  ")
      print(i)
      val col: Int = i % numSpritesAlongEdge
      val row: Int = i / numSpritesAlongEdge
      val ch: Char = this.lookupChar(i)

      val upscaleImage: BufferedImage = generateImage(graphics, typeface, ch.toString, upscalePtSize, upscaleLength, antialias=false)
      val sprite: BufferedImage = generateDistanceField(graphics, upscaleImage, upscale, spread, spriteSize)
      g2.drawImage(sprite, col * spriteSize, row * spriteSize, null)

      val codePoint = ch.toInt
      val boundingRect = Rect2D(col * spriteSize, row * spriteSize, spriteSize, spriteSize)
      val advance = fontMetrics.charWidth(codePoint)
      glyphs(i) = GlyphInfo(i, codePoint, boundingRect, advance)
    }
    println(" done!")
    g2.dispose()


    val metrics: GlyphMetrics = GlyphMetrics(typeface, ptSize, sheetWidth, sheetHeight, spriteSize,
        fontMetrics.getHeight, fontMetrics.getAscent, fontMetrics.getDescent,
        numGlyphs, glyphs)

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
    val glyphs: Array[GlyphInfo] = Array.ofDim(numGlyphs)

    val fontMetrics: FontMetrics = graphics.createCompatibleImage(1, 1).getGraphics
        .getFontMetrics(new Font(typeface, Font.PLAIN, ptSize))

    val g2: Graphics2D = spriteSheet.createGraphics()
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val upscalePtSize: Int = ptSize * upscale
    val upscaleLength: Int = spriteSize * upscale

    println("Generating " + numGlyphs + " glyphs...")
    for (i: Int <- 0 until numGlyphs) {
      print("  ")
      print(i)
      val col: Int = i % numSpritesAlongEdge
      val row: Int = i / numSpritesAlongEdge
      val ch: Char = this.lookupChar(i)

      val upscaleImage: BufferedImage = generateImage(graphics, typeface, ch.toString, upscalePtSize, upscaleLength, antialias=false)
      val sprite: BufferedImage = generateDownscaleFromDistanceField(graphics, upscaleImage, upscale, spread, spriteSize)
      g2.drawImage(sprite, col * spriteSize, row * spriteSize, null)

      val codePoint = ch.toInt
      val boundingRect = Rect2D(col * spriteSize, row * spriteSize, spriteSize, spriteSize)
      val advance = fontMetrics.charWidth(codePoint)
      glyphs(i) = GlyphInfo(i, codePoint, boundingRect, advance)
    }
    println(" done!")
    g2.dispose()


    val metrics: GlyphMetrics = GlyphMetrics(typeface, ptSize, sheetWidth, sheetHeight, spriteSize,
      fontMetrics.getHeight, fontMetrics.getAscent, fontMetrics.getDescent,
      numGlyphs, glyphs)

    GlyphSheet(metrics, spriteSheet)
  }

  /////////////////////
  // private methods //
  /////////////////////

  private def lookupChar(index: Int): Char = {
    // Codepage 437
    val mapping = Array(
      '\u0000', '\u263A', '\u263B', '\u2665', '\u2666', '\u2663', '\u2660', '\u2022',
      '\u25D8', '\u25CB', '\u25D9', '\u2642', '\u2640', '\u266A', '\u266B', '\u263C',
      '\u25BA', '\u25C4', '\u2195', '\u203C', '\u00B6', '\u00A7', '\u25AC', '\u21A8',
      '\u2191', '\u2193', '\u2192', '\u2190', '\u221F', '\u2194', '\u25B2', '\u25BC',
    ) ++ Array(
      ' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?'
    ) ++ Array(
      '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
      'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', '\\', ']', '^', '_'
    ) ++ Array(
      '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
      'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', '\u2302'
    ) ++ Array(
      '\u00C7', '\u00FC', '\u00E9', '\u00E2', '\u00E4', '\u00E0', '\u00E5', '\u00E7',
      '\u00EA', '\u00EB', '\u00E8', '\u00EF', '\u00EE', '\u00EC', '\u00C4', '\u00C5',
      '\u00C9', '\u00E6', '\u00C6', '\u00F4', '\u00F6', '\u00F2', '\u00FB', '\u00F9',
      '\u00FF', '\u00D6', '\u00DC', '\u00A2', '\u00A3', '\u00A5', '\u20A7', '\u0192',
      '\u00E1', '\u00ED', '\u00F3', '\u00FA', '\u00F1', '\u00D1', '\u00AA', '\u00BA',
      '\u00BF', '\u2310', '\u00AC', '\u00BD', '\u00BC', '\u00A1', '\u00AB', '\u00BB',
      '\u2591', '\u2592', '\u2593', '\u2502', '\u2524', '\u2561', '\u2562', '\u2556',
      '\u2555', '\u2563', '\u2551', '\u2557', '\u255D', '\u255C', '\u255B', '\u2510',
      '\u2514', '\u2534', '\u252C', '\u251C', '\u2500', '\u253C', '\u255E', '\u255F',
      '\u255A', '\u2554', '\u2569', '\u2566', '\u2560', '\u2550', '\u256C', '\u2567',
      '\u2568', '\u2564', '\u2565', '\u2559', '\u2558', '\u2552', '\u2553', '\u256B',
      '\u256A', '\u2518', '\u250C', '\u2588', '\u2584', '\u258C', '\u2590', '\u2580',
      '\u03B1', '\u00DF', '\u0393', '\u03C0', '\u03A3', '\u03C3', '\u00B5', '\u03C4',
      '\u03A6', '\u0398', '\u03A9', '\u03B4', '\u221E', '\u03C6', '\u03B5', '\u2229',
      '\u2261', '\u00B1', '\u2265', '\u2264', '\u2320', '\u2321', '\u00F7', '\u2248',
      '\u00B0', '\u2219', '\u00B7', '\u221A', '\u207F', '\u00B2', '\u25A0', '\u00A0'
    )

    return mapping(index)
  }

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
    val font: Font = new Font(typeface, Font.PLAIN, ptSize)
    g2.setFont(font)

    // Different typefaces have varying levels of accurate font metrics
    // especially for non-standard chars. For example, Arial is more complete
    // than Verdana on mac for non-standard codepage 437 characters.
    //
    // TextLayout can return accurate measurements.
    val affine: AffineTransform = new AffineTransform()
    val frc: FontRenderContext = new FontRenderContext(affine, false, false)
    val layout: TextLayout = new TextLayout(symbol, font, frc)
    val charWidth: Double = layout.getBounds().getWidth()
    val fm: FontMetrics = g2.getFontMetrics

    print(" generateImage(")
    print(symbol)
    print(",")
    print(ptSize)
    print(",")
    print(spriteSize)
    print(",")
    print(antialias)
    print(")  fm.stringWidth:")
    print(fm.stringWidth(symbol))
    print("  charWidth:")
    print(charWidth)
    println("")

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
