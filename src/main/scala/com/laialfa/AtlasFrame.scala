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
import java.awt.{Rectangle, FontMetrics, Dimension, Font, Graphics2D, Color}
import java.io.{IOException, File}
import javax.imageio.ImageIO
import scala.swing.event.ButtonClicked
import scala.swing.Dialog.Result
import scala.swing.{Panel, CheckMenuItem, Separator, FileChooser, MenuItem, Menu, MenuBar, MainFrame, Dialog, Action}


class AtlasFrame extends MainFrame {

  background = Color.black
  preferredSize = new Dimension(800, 600)
  visible = true

  private val spriteSize: Int = 64
  private val numSpritesAlongEdge: Int = 16
  private val numSprites: Int = 256

  var glyphSheet: GlyphSheet = null

  private var drawGridLines: Boolean = false

  private var optCurrentFile: Option[File] = None

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
    override def paint(g: Graphics2D) {
      g.setColor(Color.white)

      if (glyphSheet == null) {
        drawStringJava(g, "Create a new glyph atlas via File menu -> New...", Color.white, 14, 64, 64)
        return
      }

      if (drawGridLines) {
        for (i: Int <- 0 until numSprites) {
          val rect: Rectangle = glyphSheet.getRect(i)
          g.drawRect(rect.x, rect.y, rect.width, rect.height)
        }
      }
      g.drawImage(glyphSheet.getImage, 0, 0, null)

      var ptSize: Int = glyphSheet.getPtSize
      var message: String = "The quick brown fox"
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

  /////////////////////
  // private methods //
  /////////////////////

  /**
   * Load image and associated font attributes.
   *
   * @param file    to image file
   */
  private def load(file: File) {
    println("TODO: load " + file)
  }

  /**
   * Save image and associated font attributes.
   *
   * @param file    to image file
   */
  private def save(file: File) {
    if (glyphSheet == null) {
      Dialog.showMessage(contents.head, "Error: no glyph atlas to save\nCreate a new one first.")
      return
    }

    if (!file.getName.toLowerCase.endsWith(".png")) {
      Dialog.showMessage(contents.head, "Error: filename must end with .png\nChoose a different name.")
      return
    }

    try {
      ImageIO.write(glyphSheet.getImage, "png", file)
    } catch {
      case e: IOException => println("Error: " + e.getMessage)
    }

    println("TODO: save font metrics in .fnt file too")

    title = file.getName
    optCurrentFile = Some(file)
  }

  private def saveAs() {
    if (glyphSheet == null) {
      Dialog.showMessage(contents.head, "Error: no glyph atlas to save\nCreate a new one first.")
      return
    }

    val fc: FileChooser = new FileChooser { title = SAVE_AS }
    if (fc.showSaveDialog(contents.head) == FileChooser.Result.Approve) {
      val file: File = fc.selectedFile
      if (file.exists()) {
        Dialog.showConfirmation(contents.head, "Overwrite?", SAVE_AS) match {
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
  private def drawString(g: Graphics2D, text: String, color: Color, ptSize: Int, x: Int, y: Int) {
    val scaleFactor: Double = ptSize.toDouble / glyphSheet.getPtSize

    val destDimen: Int = math.round(scaleFactor * spriteSize).toInt
    val destAscent: Int = math.round(scaleFactor * glyphSheet.getFontMetrics.getAscent).toInt

    var unscaledAnchorX: Int = 0  // will advance each iteration
    val yDest: Int = y - destAscent  // constant (will be -ve for OpenGL)

    for (charPos: Int <- 0 until text.length()) {
      val ch: Char = text.charAt(charPos)
      val unscaledCharWidth: Int = glyphSheet.getGlyphWidth(ch.toInt)

      val row: Int = ch / numSpritesAlongEdge

      val srcRect: Rectangle = glyphSheet.getRect(ch.toInt)

      // this craziness because 0 is bottom of image in OpenGL
      val ySrc: Int = (numSpritesAlongEdge - 1 - row) * spriteSize

      val shim: Double = (spriteSize - unscaledCharWidth) / 2.0

      val xDest: Int = x + math.round(scaleFactor * (unscaledAnchorX - shim)).toInt

      g.drawImage(glyphSheet.getImage,
                  xDest,
                  yDest,
                  xDest + destDimen,
                  yDest + destDimen,

                  srcRect.x,
                  ySrc,
                  srcRect.x + srcRect.width,
                  ySrc + srcRect.height,
                  null)

      unscaledAnchorX += unscaledCharWidth  // horizontal advance includes spacing
    }
  }

  /**
   * String width in pixels using pre-calculated metrics.
   */
  private def stringWidth(g: Graphics2D, text: String, ptSize: Int): Int = {
    var sum: Int = 0

    for (charPos: Int <- 0 until text.length()) {
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
    val fm: FontMetrics = g.getFontMetrics(new Font("SansSerif", Font.PLAIN, ptSize))
    fm.stringWidth(text)
  }
}
