/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.infrastructure.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

/** Identifies server configuration instances that can safely persist migrations. */
final class ConfigMigrationSupport {
	private ConfigMigrationSupport() {
	}

	static boolean isPersistent(CommentedConfig configData) {
		return configData instanceof CommentedFileConfig;
	}
}
