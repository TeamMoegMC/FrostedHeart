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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 字段注解，用于指定序列化时使用的自定义名称，替代默认的字段名。
 * <p>
 * Field annotation to specify a custom name for serialization, overriding the default field name.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface SerializeName {
	/**
	 * 序列化时使用的名称。
	 * <p>
	 * The name to use during serialization.
	 *
	 * @return 自定义序列化名称 / the custom serialization name
	 */
	String value();
}
