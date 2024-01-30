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
    int idx = 0;
    int srecord = 0;

    public StringParseReader(String str) {
        super();
        this.str = str;
    }

    public String fromStart() {
        return str.substring(srecord, idx);
    }

    public boolean hasNext() {
        return idx < str.length()-1;
    }

    public boolean isBegin() {
        return idx == 0;
    }

    public char read() {
        return str.charAt(idx);
    }

    public void loadIndex() {
        idx = srecord;
    }

    public char eat() {
        return str.charAt(idx++);
    }

    public boolean eat(char ch) {
        if (read() == ch) {
            eat();
            return true;
        }
        return false;
    }


    public void saveIndex() {
        srecord = idx;
    }

    public void skipWhitespace() {
        boolean hasChangedIndex = false;
        while (hasNext()&&Character.isWhitespace(read())) {
            idx++;
            hasChangedIndex = true;
        }
       // if (hasChangedIndex&&hasNext())
       //     idx--;
    }
}
