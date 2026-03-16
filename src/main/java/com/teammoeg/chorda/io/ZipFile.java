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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP文件压缩工具类，支持将文件和目录添加到ZIP归档中，实现AutoCloseable接口。
 * <p>
 * ZIP file compression utility class that supports adding files and directories to a ZIP archive, implementing AutoCloseable.
 */
public class ZipFile implements AutoCloseable {
    Path fd;
    ZipOutputStream bkf;

    /**
     * 创建一个新的ZIP文件写入器。
     * <p>
     * Creates a new ZIP file writer.
     *
     * @param output ZIP输出文件 / the ZIP output file
     * @param indir 输入目录的基准路径，用于计算相对路径 / the base input directory path for computing relative paths
     * @throws IOException 创建ZIP输出流失败时抛出 / thrown when creating the ZIP output stream fails
     */
    public ZipFile(File output, Path indir) throws IOException {
        fd = indir.toAbsolutePath();
        bkf = new ZipOutputStream(Files.newOutputStream(output.toPath()));
    }

    /**
     * 将文件或目录递归添加到ZIP归档中。
     * <p>
     * Recursively adds a file or directory to the ZIP archive.
     *
     * @param f 要添加的文件或目录 / the file or directory to add
     * @throws IOException 添加失败时抛出 / thrown when adding fails
     */
    public void add(File f) throws IOException {
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            if (fs != null) {
                for (File file : fs) {
                    add(file);
                }
            }
        } else {
            addFile(f);
        }
    }

    /**
     * 将文件添加到ZIP归档中，并在添加后删除满足条件的源文件。
     * <p>
     * Adds files to the ZIP archive and deletes source files that match the predicate after adding.
     *
     * @param f 要添加的文件或目录 / the file or directory to add
     * @param p 文件过滤谓词，决定哪些文件被添加并删除 / the file filter predicate determining which files to add and delete
     * @throws IOException 操作失败时抛出 / thrown when the operation fails
     */
    public void addAndDel(File f, Predicate<File> p) throws IOException {
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            if (fs != null) {
                for (File file : fs) {
                	addAndDel(file, p);
                }
            }
            f.delete();
        } else {
            if (p.test(f)) {
                addFile(f);
                Files.delete(f.toPath());
            }
        }
    }

    // String normalize
    private void addFile(File f) throws IOException {
        if (f.exists()) {
            String rp = fd.relativize(FileSystems.getDefault().getPath(f.getAbsolutePath())).toString().replace("\\", "/");
            bkf.putNextEntry(new ZipEntry(rp));
            FileUtil.transfer(f, bkf);
            bkf.closeEntry();
        }
    }

    public void close() throws IOException {
        bkf.close();
    }
}
