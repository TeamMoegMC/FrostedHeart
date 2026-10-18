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
