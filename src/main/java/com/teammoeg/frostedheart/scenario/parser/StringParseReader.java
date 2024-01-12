/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.scenario.parser;

public class StringParseReader {
    String str;
    int idx = -1;
    int srecord;

    public StringParseReader(String str) {
        super();
        this.str = str;
    }

    boolean hasNext() {
        return idx < str.length() - 1;
    }

    char next() {
        return str.charAt(idx++);
    }

    char peek() {
        return str.charAt(idx + 1);
    }

    void saveIndex() {
        srecord = idx;
    }

    void loadIndex() {
        idx = srecord;
    }

    String fromStart() {
        return str.substring(srecord, idx);
    }

    char peekLast() {
        if (idx < 0)
            return str.charAt(0);
        return str.charAt(idx - 1);
    }

    char last() {
        return str.charAt(idx - 1);
    }

    void skipWhitespace() {
        while (Character.isWhitespace(str.charAt(idx)) && hasNext()) {
            idx++;
        }
    }
}
