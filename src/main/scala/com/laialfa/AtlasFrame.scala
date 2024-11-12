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
package com.laialfa

import com.laialfa.geom.Rect2D
import com.laialfa.glyph.{GlyphMetrics, GlyphSheet}
import java.awt.{FontMetrics, Dimension, Font, Graphics2D, Color, Stroke, BasicStroke}
import java.awt.image.BufferedImage
import java.io.{FileWriter, IOException, File}
import javax.imageio.ImageIO
import javax.swing.filechooser.FileNameExtensionFilter
import scala.swing.event.ButtonClicked
import scala.swing.Dialog.Result
import scala.swing.{Panel, CheckMenuItem, Separator, FileChooser, MenuItem, Menu, MenuBar, MainFrame, Dialog, Action}
import scala.xml.{XML, PrettyPrinter}


class AtlasFrame extends MainFrame {

  background = Color.black
  preferredSize = new Dimension(800, 600)
  visible = true

  val SPRITE_SIZE: Int = 64  // pixels

  private var optGlyphSheet: Option[GlyphSheet] = None

  def setGlyphSheet(glyphSheet: GlyphSheet): Unit = {
    optGlyphSheet = Some(glyphSheet)
  }

  private var drawGridLines: Boolean = false

  private var optCurrentFile: Option[File] = None

  def clearCurrentFile(): Unit = {
    optCurrentFile = None
  }

  private val SAVE_AS = "Save As..."

  menuBar = new MenuBar {
    contents += new Menu("File") {
      contents += new MenuItem(Action("New...") {
        new NewAtlasDialog(AtlasFrame.this) }
      )
      contents += new MenuItem(Action("Open...") {
        val fc: FileChooser = new FileChooser
        if (fc.showOpenDialog(AtlasFrame.this.contents.head) == FileChooser.Result.Approve) {
          load(fc.selectedFile)
        }
      })
      contents += new Separator()
      contents += new MenuItem(Action("Save") {
        optCurrentFile match {
          case Some(f) => save(f)
          case None => saveAs()
        }
      })
      contents += new MenuItem(Action(SAVE_AS) {
        saveAs()
      })
    }
    contents += new Menu("View") {
      contents += new MenuItem("Glyph Preview...")
      contents += new CheckMenuItem("Grid Lines") {
        reactions += {
          case e: ButtonClicked => {
            drawGridLines = e.source.selected
            AtlasFrame.this.repaint()
          }
        }
      }
    }
  }

  contents = new Panel {
    override def paint(g: Graphics2D): Unit = {
      g.setColor(Color.white)

      if (optGlyphSheet.isEmpty) {
        drawStringJava(g, "Create a new glyph atlas via File menu -> New...", Color.white, 14, 64, 64)
        return
      }

      val glyphSheet: GlyphSheet = optGlyphSheet.get

      if (drawGridLines) {
        val g2d: Graphics2D = g.create().asInstanceOf[Graphics2D]  // copy of the Graphics instance
        val dashed: Stroke = new BasicStroke(0.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, Array(4.0f, 8.0f), 0)
        g2d.setStroke(dashed);

        for (i: Int <- 0 until glyphSheet.metrics.numGlyphs) {
          val rect: Rect2D = glyphSheet.metrics.boundingRects(i)
          g2d.drawRect(rect.x, rect.y, rect.width, rect.height)
        }
        g2d.dispose()
      }
      g.drawImage(glyphSheet.image, 0, 0, null)

      var ptSize: Int = glyphSheet.metrics.ptSize
      var message: String = "The quick brown fox"
      drawString(g, message, Color.green, ptSize, 100, 64)
      drawStringJava(g, message, Color.green, ptSize, 100, 128)

      drawStringJava(g, "width " +     stringWidth(g, message, ptSize), Color.green, 12, 800, 64)
      drawStringJava(g, "width " + stringWidthJava(g, message, ptSize), Color.green, 12, 800, 128)

      var baseline: Int = 100
      for (p <- 6 to 18) {
        val scaledHeight: Int = (glyphSheet.metrics.height * (p / glyphSheet.metrics.height.toDouble)).toInt

        message = p.toString + " jumped over the lazy dogs!  The advance of a String is not necessarily the sum of the advances of its characters."
        drawString(g, message, Color.green, p, 100, baseline)
        drawStringJava(g, "width " +     stringWidth(g, message, p), Color.green, 12, 800, baseline)
        baseline += scaledHeight

        drawStringJava(g, message, Color.green, p, 100, baseline)
        drawStringJava(g, "width " + stringWidthJava(g, message, p), Color.green, 12, 800, baseline)
        baseline += scaledHeight
      }

      ptSize = 108
      message = "Bigger bigger"

      val scaledHeight: Int = (glyphSheet.metrics.height * (ptSize / glyphSheet.metrics.height.toDouble)).toInt
      baseline += 50
      drawString(g, message, Color.green, ptSize, 100, baseline)
      drawStringJava(g, "width " +     stringWidth(g, message, ptSize), Color.green, 12, 800, baseline)
      baseline += scaledHeight

      drawStringJava(g, message, Color.green, ptSize, 100, baseline)
      drawStringJava(g, "width " + stringWidthJava(g, message, ptSize), Color.green, 12, 800, baseline)
      baseline += scaledHeight
    }
  }

  /////////////////////
  // private methods //
  /////////////////////

  /**
   * @param pngFile    .png file
   *
   * @return NAME-metrics.xml file
   */
  private def getMetricsFile(pngFile: File): File = {
    require(pngFile.getName.toLowerCase.endsWith(".png"))

    val len: Int = pngFile.getCanonicalPath.length
    val metricsFilepath: String = pngFile.getCanonicalPath.substring(0, len-4) + "-metrics.xml"
    new File(metricsFilepath)
  }

  /**
   * Load image and associated font attributes.
   *
   * @param file    to image file
   */
  private def load(file: File): Unit = {
    val metricsFile: File = getMetricsFile(file)

    try {
      val image: BufferedImage = ImageIO.read(file)
      val metrics: GlyphMetrics = GlyphMetrics.fromXML(XML.loadFile(metricsFile))

      optCurrentFile = Some(file)
      optGlyphSheet = Some(GlyphSheet(metrics, image))
      title = file.getName
      repaint()
    } catch {
      case e: IOException => println("Error: " + e.getMessage)
    }
  }

  /**
   * Save image and associated font attributes.
   *
   * @param file    to image file
   */
  private def save(file: File): Unit = {
    if (optGlyphSheet.isEmpty) {
      Dialog.showMessage(contents.head, "No glyph atlas to save.\nCreate or open one first.")
      return
    }

    if (!file.getName.toLowerCase.endsWith(".png")) {
      Dialog.showMessage(contents.head, "Error: filename must end with .png\nChoose a different name.")
      return
    }

    val glyphSheet: GlyphSheet = optGlyphSheet.get

    try {
      ImageIO.write(glyphSheet.image, "png", file)

      val fw = new FileWriter(getMetricsFile(file))
      fw.write(new PrettyPrinter(120, 2).format(glyphSheet.metrics.toXML))
      fw.write("\n")
      fw.close()

      optCurrentFile = Some(file)
      title = file.getName
    } catch {
      case e: IOException => println("Error: " + e.getMessage)
    }
  }

  private def saveAs(): Unit = {
    if (optGlyphSheet.isEmpty) {
      Dialog.showMessage(contents.head, "No glyph atlas to save.\nCreate or open one first.")
      return
    }

    val fc: FileChooser = new FileChooser {
      title = SAVE_AS
      fileFilter = new FileNameExtensionFilter("PNG file", "png")
    }
    if (fc.showSaveDialog(contents.head) == FileChooser.Result.Approve) {
      val file: File = fc.selectedFile
      if (file.exists()) {
        val prompt: StringBuilder = new StringBuilder
        prompt.append("Overwrite ")
        prompt.append(file.getName)

        val metricsFile: File = getMetricsFile(file)
        if (metricsFile.exists()) {
          prompt.append(" and ")
          prompt.append(metricsFile.getName)
        }
        prompt.append("?")

        Dialog.showConfirmation(contents.head, prompt, SAVE_AS) match {
          case Result.Yes => save(file)
          case _ =>
        }
      } else {
        save(file)
      }
    }
  }

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
  private def drawString(g: Graphics2D, text: String, color: Color, ptSize: Int, x: Int, y: Int): Unit = {
    val glyphSheet: GlyphSheet = optGlyphSheet.get

    val scaleFactor: Double = ptSize.toDouble / glyphSheet.metrics.ptSize

    val destDimen: Int = math.round(scaleFactor * SPRITE_SIZE).toInt
    val destAscent: Int = math.round(scaleFactor * glyphSheet.metrics.ascent).toInt

    var unscaledAnchorX: Int = 0  // will advance each iteration
    val yDest: Int = y - destAscent  // constant (will be -ve for OpenGL)

    for (charPos: Int <- 0 until text.length()) {
      val ch: Char = text.charAt(charPos)
      val unscaledCharWidth: Int = glyphSheet.metrics.advances(ch.toInt)

      val srcRect: Rect2D = glyphSheet.metrics.boundingRects(ch.toInt)

      val shim: Double = (SPRITE_SIZE - unscaledCharWidth) / 2.0

      val xDest: Int = x + math.round(scaleFactor * (unscaledAnchorX - shim)).toInt

      g.drawImage(glyphSheet.image,
                  xDest,
                  yDest,
                  xDest + destDimen,
                  yDest + destDimen,

                  srcRect.x,
                  srcRect.y,
                  srcRect.x + srcRect.width,
                  srcRect.y + srcRect.height,
                  null)

      unscaledAnchorX += unscaledCharWidth  // horizontal advance includes spacing
    }
  }

  /**
   * String width in pixels using pre-calculated metrics.
   */
  private def stringWidth(g: Graphics2D, text: String, ptSize: Int): Int = {
    val glyphSheet: GlyphSheet = optGlyphSheet.get

    var sum: Int = 0

    for (charPos: Int <- 0 until text.length()) {
      sum += glyphSheet.metrics.advances(text.charAt(charPos))
    }

    math.round((ptSize.toDouble / glyphSheet.metrics.ptSize) * sum).toInt
  }

  /**
   * Font metrics version for comparison purposes.
   */
  private def drawStringJava(g: Graphics2D, text: String, color: Color, ptSize: Int, x: Int, y: Int): Unit = {
    g.setColor(color)
    val typeface: String =
      if (optGlyphSheet.isDefined) {
        optGlyphSheet.get.metrics.typeface
      } else {
        "SansSerif"
      }
    g.setFont(new Font(typeface, Font.PLAIN, ptSize))
    g.drawString(text, x, y)
  }

  /**
   * Graphics2D version for comparison purposes.
   */
  private def stringWidthJava(g: Graphics2D, text: String, ptSize: Int): Int = {
    val typeface: String =
      if (optGlyphSheet.isDefined) {
        optGlyphSheet.get.metrics.typeface
      } else {
        "SansSerif"
      }
    val fm: FontMetrics = g.getFontMetrics(new Font(typeface, Font.PLAIN, ptSize))
    fm.stringWidth(text)
  }
}
