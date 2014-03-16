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

import scala.swing.{FlowPanel, CheckBox, TextField, ComboBox, GridPanel, Window, Dialog, Action, Button, Label, BorderPanel}
import scala.swing.event.ButtonClicked
import java.awt.GraphicsEnvironment


/**
 * Create a new atlas from a chosen font.  The user may also select
 * between normal antialiased transparency or a signed distance field.
 */
class NewAtlasDialog(owner: Window) extends Dialog(owner) {

  title = "New..."
  modal = true
  resizable = false

  private val ge: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment
  private val availableFonts: List[String] = ge.getAvailableFontFamilyNames.toList

  private val fontComboBox: ComboBox[String] = new ComboBox[String](availableFonts)
  fontComboBox.selection.item = "Verdana"  // if available

  private val distanceFieldCheckBox: CheckBox = new CheckBox("Signed Distance Field")

  private val upscaleLabel: Label = new Label("Upscale:") { enabled = false }
  private val upscaleTextField: TextField = new TextField {
    enabled = false
    columns = 12
  }

  private val spreadLabel: Label = new Label("Spread:") { enabled = false }
  private val spreadTextField: TextField = new TextField {
    enabled = false
    columns = 12
  }

  private val gridPanel: GridPanel = new GridPanel(4, 2) {
    contents += new Label("Font:")
    contents += fontComboBox

    contents += distanceFieldCheckBox
    listenTo(distanceFieldCheckBox)
    reactions += {
      case bc: ButtonClicked => if (bc.source == distanceFieldCheckBox) {
        val isDistanceField: Boolean = distanceFieldCheckBox.selected
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
  }

  contents = new BorderPanel {
    layout(gridPanel) = BorderPanel.Position.Center

    layout(
      new FlowPanel {
        contents += new Button(Action("Okay") {
          println("Okay clicked yay!")
          println("Font chosen is: " + fontComboBox.selection.item)
          println("Distance field: " + distanceFieldCheckBox.selected)
          println("Upscale: " + upscaleTextField.text)
          println("Spread: " + spreadTextField.text)
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
