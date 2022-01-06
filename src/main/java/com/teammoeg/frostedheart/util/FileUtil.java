package com.teammoeg.frostedheart.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileUtil {
	public static void transfer(InputStream i,OutputStream os) throws IOException {
		int nRead;
		byte[] data = new byte[4096];
	
		try {
			while ((nRead = i.read(data, 0, data.length)) != -1) { os.write(data, 0, nRead); }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	public static void transfer(File i,OutputStream os) throws IOException {
		try (FileInputStream fis=new FileInputStream(i)){
			transfer(fis,os);
		}
	}
	public static void transfer(InputStream i,File f) throws IOException {
		try (FileOutputStream fos=new FileOutputStream(f)){
			transfer(i,fos);
		}
	}
	public static void transfer(String i,File os) throws IOException {
		try (FileOutputStream fos=new FileOutputStream(os)){
			fos.write(i.getBytes(StandardCharsets.UTF_8));
		}
	}
	public static byte[] readAll(InputStream i) throws IOException {
		ByteArrayOutputStream ba = new ByteArrayOutputStream(16384);
		int nRead;
		byte[] data = new byte[4096];
	
		try {
			while ((nRead = i.read(data, 0, data.length)) != -1) { ba.write(data, 0, nRead); }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	
		return ba.toByteArray();
	}
	public static String readString(InputStream i) throws IOException {
		return new String(readAll(i));
	}
	public static String readString(File f) throws IOException {
		return new String(readAll(f));
	}
	public static byte[] readAll(File f) throws IOException {
		try(FileInputStream fis=new FileInputStream(f)){
			return readAll(fis);
		}
	}
}
