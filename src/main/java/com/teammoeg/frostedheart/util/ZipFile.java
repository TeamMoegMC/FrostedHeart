package com.teammoeg.frostedheart.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

public class ZipFile implements AutoCloseable{
	Path fd;
	ZipOutputStream bkf;
	public ZipFile(File output,Path indir) throws ZipException, IOException {
		fd=indir.toAbsolutePath();
		bkf=new ZipOutputStream(new FileOutputStream(output));
	}
	public void close() throws IOException {
		bkf.close();
	}
	public void addAndDel(File f,Predicate<File> p) throws IOException {
		if(f.isDirectory()&&p.test(f)) {
		    File[] fs = f.listFiles();
		    if (fs != null) {
		        for (File file : fs) {
		        	addAndDel(file,p);
		        }
		    }
		    f.delete();
		}else {
			if(p.test(f)) {
				addFile(f);
				f.delete();
			}
		}
	}
	public void add(File f) throws IOException {
		if(f.isDirectory()) {
		    File[] fs = f.listFiles();
		    if (fs != null) {
		        for (File file : fs) {
		        	add(file);
		        }
		    }
		}else {
			addFile(f);
		}
	}
	// String normalize
	private void addFile(File f) throws IOException {
		if(f.exists()) {
			String rp=fd.relativize(FileSystems.getDefault().getPath(f.getAbsolutePath())).toString().replace("\\","/");
			bkf.putNextEntry(new ZipEntry(rp));
			FileUtil.transfer(f, bkf);
			bkf.closeEntry();
		}
	}
}
