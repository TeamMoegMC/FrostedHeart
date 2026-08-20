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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;

public class UnihexParser {
	public static record OverrideRange(int from, int to, int left, int right) {
		public OverrideRange(JsonObject range) {
			this(range.get("from").getAsString().codePointAt(0), range.get("to").getAsString().codePointAt(0), range.get("left").getAsInt(), range.get("right").getAsInt());
		}

	}

	static void readFromStream(InputStream stream, UnihexGlyphStore.Builder output, List<OverrideRange> overrides) throws IOException {
		int lineNumber = 0;
		ByteList bytes = new ByteArrayList(128);
		int[] rows = new int[UnihexGlyphStore.GLYPH_HEIGHT];

		while (true) {
			boolean foundColon = copyUntil(stream, bytes, ':');
			int codePointDigits = bytes.size();
			if (codePointDigits == 0 && !foundColon) {
				return;
			}

			if (!foundColon || codePointDigits != 4 && codePointDigits != 5 && codePointDigits != 6) {
				throw new IllegalArgumentException("Invalid entry at line " + lineNumber
						+ ": expected 4, 5 or 6 hex digits followed by a colon");
			}

			int codePoint = 0;
			for (int digit = 0; digit < codePointDigits; digit++) {
				codePoint = codePoint << 4 | decodeHex(lineNumber, bytes.getByte(digit));
			}

			bytes.clear();
			copyUntil(stream, bytes, '\n');
			int bitWidth = switch (bytes.size()) {
				case 32 -> 8;
				case 64 -> 16;
				case 96 -> 24;
				case 128 -> 32;
				default -> throw new IllegalArgumentException("Invalid entry at line " + lineNumber
						+ ": expected 32, 64, 96 or 128 bitmap hex digits");
			};
			readRows(lineNumber, bytes, bitWidth, rows);

			int left = 0;
			int right = 32 - bitWidth;
			for (OverrideRange override : overrides) {
				if (codePoint >= override.from && codePoint <= override.to) {
					left = override.left;
					right = override.right;
					break;
				}
			}
			output.add(codePoint, rows, left, right);
			lineNumber++;
			bytes.clear();
		}
	}

	private static void readRows(int lineNumber, ByteList bytes, int bitWidth, int[] rows) {
		int digitsPerRow = bitWidth / 4;
		int byteIndex = 0;
		for (int row = 0; row < UnihexGlyphStore.GLYPH_HEIGHT; row++) {
			int value = 0;
			for (int digit = 0; digit < digitsPerRow; digit++) {
				value = value << 4 | decodeHex(lineNumber, bytes.getByte(byteIndex++));
			}
			rows[row] = value << (Integer.SIZE - bitWidth);
		}
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

	private static boolean copyUntil(InputStream stream, ByteList bytes, int delimiter) throws IOException {
		while (true) {
			int next = stream.read();
			if (next == -1) {
				return false;
			}

			if (next == delimiter) {
				return true;
			}

			bytes.add((byte) next);
		}
	}

}
