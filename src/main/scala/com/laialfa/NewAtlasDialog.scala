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
import scala.swing.{FlowPanel, CheckBox, TextField, ComboBox, GridPanel, Dialog, Action, Button, Label, BorderPanel}
import scala.swing.event.ButtonClicked
import java.awt.GraphicsEnvironment


/**
 * Create a new atlas from a chosen font.  The user may also select
 * between normal antialiased transparency or a signed distance field.
 */
class NewAtlasDialog(owner: AtlasFrame) extends Dialog(owner) {

  title = "New..."
  modal = true
  resizable = false

  private val ge: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment
  private val availableFonts: List[String] = ge.getAvailableFontFamilyNames.toList

  private val fontComboBox: ComboBox[String] = new ComboBox[String](availableFonts)
  fontComboBox.selection.item = "Verdana"  // if available

  private val distanceFieldCheckBox: CheckBox = new CheckBox("Signed Distance Field")

  private val antialiasForDownscaleCheckBox: CheckBox = new CheckBox("Antialias SDF for Downscale only") {
    enabled = false
  }

  private val upscaleLabel: Label = new Label("Upscale (2=fast, 8=quality):") { enabled = false }
  private val upscaleTextField: TextField = new TextField {
    enabled = false
    columns = 12
    text = "2"
  }

  private val spreadLabel: Label = new Label("Spread (pixels):") { enabled = false }
  private val spreadTextField: TextField = new TextField {
    enabled = false
    columns = 12
    text = "4"
  }

  private val gridPanel: GridPanel = new GridPanel(5, 2) {
    contents += new Label("Font:")
    contents += fontComboBox

    contents += distanceFieldCheckBox
    listenTo(distanceFieldCheckBox)
    reactions += {
      case bc: ButtonClicked => if (bc.source == distanceFieldCheckBox) {
        val isDistanceField: Boolean = distanceFieldCheckBox.selected
        antialiasForDownscaleCheckBox.enabled = isDistanceField
        upscaleLabel.enabled = isDistanceField
        upscaleTextField.enabled = isDistanceField
        spreadLabel.enabled = isDistanceField
        spreadTextField.enabled = isDistanceField
      }
    }
    contents += new Label()  // shim for empty 2nd column

    contents += upscaleLabel
    contents += upscaleTextField

    contents += spreadLabel
    contents += spreadTextField

    contents += antialiasForDownscaleCheckBox
    contents += new Label()  // shim for empty 2nd column
  }

  contents = new BorderPanel {
    layout(gridPanel) = BorderPanel.Position.Center

    layout(
      new FlowPanel {
        contents += new Button(Action("Okay") {
          val typeface: String = fontComboBox.selection.item

          println("Font chosen is: " + typeface)
          println("Distance field: " + distanceFieldCheckBox.selected)
          println("Upscale: " + upscaleTextField.text)
          println("Spread: " + spreadTextField.text)

          if (distanceFieldCheckBox.selected) {
            val upscale: Int = upscaleTextField.text.toInt
            val spread: Int = spreadTextField.text.toInt

            if (antialiasForDownscaleCheckBox.selected) {
              owner.setGlyphSheet(GlyphSheet.generateDownscale(typeface, owner.SPRITE_SIZE, upscale, spread))
            } else {
              owner.setGlyphSheet(GlyphSheet.generate(typeface, owner.SPRITE_SIZE, upscale, spread))
            }
          } else {
            owner.setGlyphSheet(GlyphSheet.generate(typeface, owner.SPRITE_SIZE, antialias=true))
          }

          owner.clearCurrentFile()
          owner.title = "Untitled"
          owner.repaint()

          dispose()
        })
        contents += new Button(Action("Cancel") {
          dispose()
        })
      }
    ) = BorderPanel.Position.South
  }

  open()
}
