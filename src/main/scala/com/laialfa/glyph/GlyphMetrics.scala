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
import upickle.default._


/**
 * Contains information about a single glyph.
 *
 * @param index        0 based
 * @param codePoint    Unicode code point
 * @param rect         bounding rect location on GlyphSheet
 * @param advance      character width in pixels
 */
case class GlyphInfo(index: Int,
                 codePoint: Int,
                      rect: Rect2D,
                   advance: Int) derives ReadWriter;


/**
 * Contains information about the font, Unicode code points, and their
 * respective advances used to generate the GlyphSheet.
 *
 * @param typeface       font name
 * @param ptSize         ptSize used when generating glyphSheet
 * @param sheetWidth     width of sprite sheet (pixels)
 * @param sheetHeight    height of sprite sheet (pixels)
 * @param spriteSize     length of square edge (pixels)
 * @param height         font height
 * @param ascent         above the baseline
 * @param descent        below the baseline (might not include leading)
 * @param numGlyphs      number of characters in the GlyphSheet - usually 256
 * @param glyphs         codePoint, bounding rect, and advance
 */
case class GlyphMetrics(typeface: String,
                          ptSize: Int,
                      sheetWidth: Int,
                     sheetHeight: Int,
                      spriteSize: Int,
                          height: Int,
                          ascent: Int,
                         descent: Int,
                       numGlyphs: Int,
                          glyphs: Array[GlyphInfo]) derives ReadWriter;
