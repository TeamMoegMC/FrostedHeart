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

/**
 * ASM字节码生成包。使用ObjectWeb ASM库在运行时动态生成类，
 * 通过{@code invokedynamic}实现高性能的构造函数访问，
 * 避免反射调用的性能开销。
 * <p>
 * ASM bytecode generation package. Uses the ObjectWeb ASM library to dynamically
 * generate classes at runtime, achieving high-performance constructor access via
 * {@code invokedynamic}, avoiding the performance overhead of reflective invocation.
 *
 * @see com.teammoeg.chorda.asm.ASMClassFactory
 * @see com.teammoeg.chorda.asm.NoArgConstructorFactory
 * @see com.teammoeg.chorda.asm.OneArgConstructorFactory
 */
package com.teammoeg.chorda.asm;
