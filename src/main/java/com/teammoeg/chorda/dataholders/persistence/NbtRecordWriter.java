package com.teammoeg.chorda.dataholders.persistence;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPOutputStream;

import net.minecraft.nbt.NbtIo;

/** Writes a detached record; never accesses a server, component or live world. */
public final class NbtRecordWriter implements PersistenceCoordinator.Writer {
    @Override
    public long write(PersistenceCoordinator.RecordSnapshot snapshot) throws IOException {
        IOException failure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return writeOnce(snapshot);
            } catch (IOException ex) {
                failure = ex;
                if (attempt == 2) break;
                try {
                    Thread.sleep(attempt == 0 ? 50 : 200);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    ex.addSuppressed(interrupted);
                    break;
                }
            }
        }
        throw failure;
    }

    private long writeOnce(PersistenceCoordinator.RecordSnapshot snapshot) throws IOException {
        Path target = snapshot.target();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".chorda-tmp");
        try {
            try (var file = Files.newOutputStream(temporary);
                 var gzip = new GZIPOutputStream(file, 64 * 1024) {{ def.setLevel(1); }};
                 var output = new DataOutputStream(gzip)) {
                NbtIo.write(snapshot.root(), output);
            }
            long bytes = Files.size(temporary);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return bytes;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
