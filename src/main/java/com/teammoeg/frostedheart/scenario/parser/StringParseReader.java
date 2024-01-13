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
    public final String str;
    int idx = -1;
    int srecord = 0;

    public StringParseReader(String str) {
        super();
        this.str = str;
    }

    public String fromStart() {
        return str.substring(srecord, idx);
    }

    public boolean hasNext() {
        return idx < str.length() - 1;
    }

    public boolean isBegin() {
        return idx == -1;
    }

    public char last() {
        return str.charAt(idx);
    }

    public void loadIndex() {
        idx = srecord;
    }

    public char next() {
        return str.charAt(++idx);
    }

    public char peek() {
        return str.charAt(idx + 1);
    }

    public boolean eat(char ch) {
        if (peek() == ch) {
            next();
            return true;
        }
        return false;
    }

    public char peekLast() {
        if (idx < 0)
            return str.charAt(0);
        return str.charAt(idx);
    }

    public void saveIndex() {
        srecord = idx + 1;
    }

    public void skipWhitespace() {
        boolean hasChangedIndex = false;
        while (Character.isWhitespace(str.charAt(idx)) && hasNext()) {
            idx++;
            hasChangedIndex = true;
        }
        if (hasChangedIndex)
            idx--;
    }
}
