/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

class FHConfigMigrationTest {
    @Test
    void remoteServerFallbackConfigIsNotPersistent() {
        assertFalse(ConfigMigrationSupport.isPersistent(CommentedConfig.inMemory()));
    }

    @Test
    void worldServerConfigIsPersistent(@TempDir Path directory) {
        try (CommentedFileConfig config = CommentedFileConfig.of(directory.resolve("frostedheart-server.toml"))) {
            assertTrue(ConfigMigrationSupport.isPersistent(config));
        }
    }
}
