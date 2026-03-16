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

/**
 * 文件输入输出工具类，提供文件读取、写入、数据传输和HTTP获取等功能。
 * <p>
 * File I/O utility class providing file reading, writing, data transfer, and HTTP fetching capabilities.
 */
public class FileUtil {
    /**
     * 读取文件的全部内容为字节数组。
     * <p>
     * Reads the entire contents of a file into a byte array.
     *
     * @param f 要读取的文件 / the file to read
     * @return 文件内容的字节数组 / a byte array containing the file contents
     * @throws IOException 读取失败时抛出 / thrown when reading fails
     */
    public static byte[] readAll(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return readAll(fis);
        }
    }

    /**
     * 读取输入流的全部内容为字节数组。
     * <p>
     * Reads the entire contents of an input stream into a byte array.
     *
     * @param i 输入流 / the input stream to read
     * @return 输入流内容的字节数组 / a byte array containing the stream contents
     * @throws IOException 读取失败时抛出 / thrown when reading fails
     */
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

    /**
     * 以UTF-8编码读取文件的全部内容为字符串。
     * <p>
     * Reads the entire contents of a file as a UTF-8 encoded string.
     *
     * @param f 要读取的文件 / the file to read
     * @return 文件内容字符串 / the file contents as a string
     * @throws IOException 读取失败时抛出 / thrown when reading fails
     */
    public static String readString(File f) throws IOException {
        return new String(readAll(f), StandardCharsets.UTF_8);
    }

    /**
     * 以UTF-8编码读取输入流的全部内容为字符串。
     * <p>
     * Reads the entire contents of an input stream as a UTF-8 encoded string.
     *
     * @param i 输入流 / the input stream to read
     * @return 流内容字符串 / the stream contents as a string
     * @throws IOException 读取失败时抛出 / thrown when reading fails
     */
    public static String readString(InputStream i) throws IOException {
        return new String(readAll(i), StandardCharsets.UTF_8);
    }

    /**
     * 将文件或目录内容传输到目标文件。若源为目录，则递归传输。
     * <p>
     * Transfers file or directory contents to a target file. Recursively transfers if source is a directory.
     *
     * @param i 源文件或目录 / the source file or directory
     * @param o 目标文件或目录 / the target file or directory
     * @throws IOException 传输失败时抛出 / thrown when transfer fails
     */
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

    /**
     * 将文件内容传输到输出流。
     * <p>
     * Transfers file contents to an output stream.
     *
     * @param i 源文件 / the source file
     * @param os 目标输出流 / the target output stream
     * @throws IOException 传输失败时抛出 / thrown when transfer fails
     */
    public static void transfer(File i, OutputStream os) throws IOException {
        try (FileInputStream fis = new FileInputStream(i)) {
            transfer(fis, os);
        }
    }

    /**
     * 将输入流内容传输到文件。
     * <p>
     * Transfers input stream contents to a file.
     *
     * @param i 源输入流 / the source input stream
     * @param f 目标文件 / the target file
     * @throws IOException 传输失败时抛出 / thrown when transfer fails
     */
    public static void transfer(InputStream i, File f) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            transfer(i, fos);
        }
    }

    /**
     * 将输入流内容传输到输出流。
     * <p>
     * Transfers input stream contents to an output stream.
     *
     * @param i 源输入流 / the source input stream
     * @param os 目标输出流 / the target output stream
     * @throws IOException 传输失败时抛出 / thrown when transfer fails
     */
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

    /**
     * 将字符串内容以UTF-8编码写入文件。
     * <p>
     * Writes string contents to a file using UTF-8 encoding.
     *
     * @param i 要写入的字符串 / the string to write
     * @param os 目标文件 / the target file
     * @throws IOException 写入失败时抛出 / thrown when writing fails
     */
    public static void transfer(String i, File os) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(os)) {
            fos.write(i.getBytes(StandardCharsets.UTF_8));
        }
    }
	/**
	 * 通过HTTP GET请求获取指定URL的输入流。
	 * <p>
	 * Fetches an input stream from the specified URL via HTTP GET request.
	 *
	 * @param url 要获取的URL地址 / the URL to fetch
	 * @return URL响应的输入流 / the input stream of the URL response
	 * @throws IOException HTTP请求失败或非200响应时抛出 / thrown when the HTTP request fails or returns non-200 status
	 */
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
