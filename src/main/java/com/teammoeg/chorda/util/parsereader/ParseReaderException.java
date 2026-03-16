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

package com.teammoeg.chorda.util.parsereader;

/**
 * 解析读取器异常。在DSL解析过程中遇到语法或结构错误时抛出。
 * <p>
 * Parse reader exception. Thrown when syntax or structural errors are encountered during DSL parsing.
 */
public class ParseReaderException extends RuntimeException {

    /**
	 * 序列化版本号。
	 * <p>
	 * Serialization version UID.
	 */
	private static final long serialVersionUID = -2921829910823307588L;

	/**
	 * 构造一个无参的解析读取器异常。
	 * <p>
	 * Construct a parse reader exception with no arguments.
	 */
	public ParseReaderException() {
    }

    /**
     * 使用指定消息构造解析读取器异常。
     * <p>
     * Construct a parse reader exception with the specified message.
     *
     * @param message 错误消息 / the error message
     */
    public ParseReaderException(String message) {
        super(message);
    }

    /**
     * 使用指定消息和原因构造解析读取器异常。
     * <p>
     * Construct a parse reader exception with the specified message and cause.
     *
     * @param message 错误消息 / the error message
     * @param cause 导致此异常的原因 / the cause of this exception
     */
    public ParseReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用指定消息、原因、是否启用抑制和是否可写栈追踪构造解析读取器异常。
     * <p>
     * Construct a parse reader exception with the specified message, cause, suppression enabled flag and writable stack trace flag.
     *
     * @param message 错误消息 / the error message
     * @param cause 导致此异常的原因 / the cause of this exception
     * @param enableSuppression 是否启用抑制 / whether suppression is enabled
     * @param writableStackTrace 栈追踪是否可写 / whether the stack trace is writable
     */
    public ParseReaderException(String message, Throwable cause, boolean enableSuppression,
                                      boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * 使用指定原因构造解析读取器异常。
     * <p>
     * Construct a parse reader exception with the specified cause.
     *
     * @param cause 导致此异常的原因 / the cause of this exception
     */
    public ParseReaderException(Throwable cause) {
        super(cause);
    }

}
