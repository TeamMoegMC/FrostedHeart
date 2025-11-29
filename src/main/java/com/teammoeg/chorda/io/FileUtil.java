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

package com.teammoeg.chorda.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.teammoeg.chorda.Chorda;

public class FileUtil {
    public static byte[] readAll(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return readAll(fis);
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
            Chorda.LOGGER.error("Error reading input stream", e);
        }

        return ba.toByteArray();
    }

    public static String readString(File f) throws IOException {
        return new String(readAll(f), StandardCharsets.UTF_8);
    }

    public static String readString(InputStream i) throws IOException {
        return new String(readAll(i), StandardCharsets.UTF_8);
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

    public static void transfer(InputStream i, OutputStream os) throws IOException {
        int nRead;
        byte[] data = new byte[4096];

        try {
            while ((nRead = i.read(data, 0, data.length)) != -1) {
                os.write(data, 0, nRead);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error transferring input stream", e);
        }
    }

    public static void transfer(String i, File os) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(os)) {
            fos.write(i.getBytes(StandardCharsets.UTF_8));
        }
    }
	public static InputStream fetch(String url) throws IOException {
		HttpURLConnection huc2 = (HttpURLConnection) new URL(url).openConnection();
		huc2.setRequestMethod("GET");
		huc2.setDoOutput(true);
		huc2.setDoInput(true);
		huc2.connect();
		if(huc2.getResponseCode()==200)
			return huc2.getInputStream();
		throw new IOException("HTTP"+huc2.getResponseCode()+" "+huc2.getResponseMessage()+" got while fetching "+url);
	}
}
