/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileUtil {
    public static void transfer(InputStream i, OutputStream os) throws IOException {
        int nRead;
        byte[] data = new byte[4096];

        try {
            while ((nRead = i.read(data, 0, data.length)) != -1) {
                os.write(data, 0, nRead);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw e;
        }
    }

    public static void transfer(File i, File o) throws IOException {
        if (!i.isDirectory()) {
            try (FileInputStream fis = new FileInputStream(i)) {
                transfer(fis, o);
            }
        } else {
            for (File f : i.listFiles()) {
                transfer(i, new File(o, f.getName()));
            }
        }
    }

    public static void transfer(File i, OutputStream os) throws IOException {
        try (FileInputStream fis = new FileInputStream(i)) {
            transfer(fis, os);
        }
    }

    public static void transfer(InputStream i, File f) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            transfer(i, fos);
        }
    }

    public static void transfer(String i, File os) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(os)) {
            fos.write(i.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static byte[] readAll(InputStream i) throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream(16384);
        int nRead;
        byte[] data = new byte[4096];

        try {
            while ((nRead = i.read(data, 0, data.length)) != -1) {
                ba.write(data, 0, nRead);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw e;
        }

        return ba.toByteArray();
    }

    public static String readString(InputStream i) throws IOException {
        return new String(readAll(i), StandardCharsets.UTF_8);
    }

    public static String readString(File f) throws IOException {
        return new String(readAll(f), StandardCharsets.UTF_8);
    }

    public static byte[] readAll(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return readAll(fis);
        }
    }
}
