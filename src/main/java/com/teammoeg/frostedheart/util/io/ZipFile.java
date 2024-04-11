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

package com.teammoeg.frostedheart.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFile implements AutoCloseable {
    Path fd;
    ZipOutputStream bkf;

    public ZipFile(File output, Path indir) throws IOException {
        fd = indir.toAbsolutePath();
        bkf = new ZipOutputStream(Files.newOutputStream(output.toPath()));
    }

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

    public void addAndDel(File f, Predicate<File> p) throws IOException {
        if (f.isDirectory() && p.test(f)) {
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
                f.delete();
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
