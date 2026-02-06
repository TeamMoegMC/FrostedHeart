/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.scenario.client.gui.layered.font;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class UnihexParser {
	public static record OverrideRange(int from, int to, int left, int right) {
		public OverrideRange(JsonObject range) {
			this(range.get("from").getAsString().codePointAt(0), range.get("to").getAsString().codePointAt(0), range.get("left").getAsInt(), range.get("right").getAsInt());
		}

	}

	static void readFromStream(InputStream pStream, BiConsumer<Integer, GlyphData> pOutput, List<OverrideRange> overrides) throws IOException {
		int i = 0;
		ByteList bytelist = new ByteArrayList(128);

		while (true) {
			boolean flag = copyUntil(pStream, bytelist, 58);
			int j = bytelist.size();
			if (j == 0 && !flag) {
				return;
			}

			if (!flag || j != 4 && j != 5 && j != 6) {
				throw new IllegalArgumentException("Invalid entry at line " + i + ": expected 4, 5 or 6 hex digits followed by a colon");
			}

			int cp = 0;

			for (int l = 0; l < j; ++l) {
				cp = cp << 4 | decodeHex(i, bytelist.getByte(l));
			}
			BufferedImage image;
			bytelist.clear();
			copyUntil(pStream, bytelist, 10);
			int i1 = bytelist.size();
			LineData unihexprovider$linedata1 = LineData.readn(i, bytelist, i1 / 4);
			int pLeft = 0;
			int pRight = 32 - (i1 / 4);
			for (OverrideRange rov : overrides) {
				if (cp <= rov.to && cp >= rov.from) {
					pLeft = rov.left;
					pRight = rov.right;

					break;
				}

			}
			int width = pRight - pLeft + 1;
			image = new BufferedImage(width, 16, BufferedImage.TYPE_INT_ARGB);
			int ml = 32 - pLeft - 1;
			int mr = 32 - pRight - 1;
			for (int l = 0; l < 16; l++) {
				for (int k = ml; k >= mr; --k) {
					if (k < 32 && k >= 0) {
						boolean isPixel = (unihexprovider$linedata1.line(l) >> k & 1) != 0;
						image.setRGB(ml - k, l, isPixel ? 0xFFFFFFFF : 0x0);
					} else {
						image.setRGB(ml - k, l, 0x0);
					}
				}
			}
			GlyphData data = new GlyphData();
			data.isUnicode = true;
			data.height = 16;
			data.width = width;
			data.advance = width + 1;
			data.image = image;
			pOutput.accept(cp, data);
			bytelist.clear();
		}
	}

	private static int decodeHex(int pLineNumber, ByteList pByteList, int pIndex) {
		return decodeHex(pLineNumber, pByteList.getByte(pIndex));
	}

	private static int decodeHex(int pLineNumber, byte pData) {
		byte b0;
		switch (pData) {
		case 48:
			b0 = 0;
			break;
		case 49:
			b0 = 1;
			break;
		case 50:
			b0 = 2;
			break;
		case 51:
			b0 = 3;
			break;
		case 52:
			b0 = 4;
			break;
		case 53:
			b0 = 5;
			break;
		case 54:
			b0 = 6;
			break;
		case 55:
			b0 = 7;
			break;
		case 56:
			b0 = 8;
			break;
		case 57:
			b0 = 9;
			break;
		case 58:
		case 59:
		case 60:
		case 61:
		case 62:
		case 63:
		case 64:
		default:
			throw new IllegalArgumentException("Invalid entry at line " + pLineNumber + ": expected hex digit, got " + (char) pData);
		case 65:
			b0 = 10;
			break;
		case 66:
			b0 = 11;
			break;
		case 67:
			b0 = 12;
			break;
		case 68:
			b0 = 13;
			break;
		case 69:
			b0 = 14;
			break;
		case 70:
			b0 = 15;
		}

		return b0;
	}

	private static boolean copyUntil(InputStream pStream, ByteList pByteList, int p_285177_) throws IOException {
		while (true) {
			int i = pStream.read();
			if (i == -1) {
				return false;
			}

			if (i == p_285177_) {
				return true;
			}

			pByteList.add((byte) i);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static record LineData(int[] contents, int bitWidth) {

		public int line(int pIndex) {
			return this.contents[pIndex];
		}

		static LineData readn(int pIndex, ByteList pByteList, int range) {
			int[] aint = new int[16];
			int j = 0;

			for (int k = 0; k < 16; ++k) {
				int lineValue = 0;
				for (int i = 0; i < (range / 4); i++) {
					lineValue <<= 4;
					lineValue |= decodeHex(pIndex, pByteList, j++);
				}
				aint[k] = lineValue << 32 - range;
			}

			return new LineData(aint, range);
		}
	}

}
