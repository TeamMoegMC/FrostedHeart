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

package com.teammoeg.chorda.util.struct;

import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
/**
 * 基于tick间隔的懒执行工作器，每隔指定tick数执行一次工作。
 * 支持静态和动态间隔，以及NBT序列化。
 * <p>
 * Lazy tick-based worker that executes work at specified tick intervals.
 * Supports static and dynamic intervals, as well as NBT serialization.
 */
public class LazyTickWorker {
    public int tMax;
    public int tCur = 0;
    private boolean isStaticMax;
    public Supplier<Boolean> work;
    /**
     * 使用固定间隔和Runnable工作构造工作器。
     * <p>
     * Construct a worker with a fixed interval and a Runnable task.
     *
     * @param tMax 最大tick间隔 / the maximum tick interval
     * @param work 要执行的工作 / the work to execute
     */
    public LazyTickWorker(int tMax, Runnable work) {
        this(tMax,()->{
        	work.run();
        	return true;
        });
    }

    /**
     * 使用固定间隔和返回布尔值的工作构造工作器。
     * <p>
     * Construct a worker with a fixed interval and a Boolean-returning task.
     *
     * @param tMax 最大tick间隔 / the maximum tick interval
     * @param work 要执行的工作，返回是否成功 / the work to execute, returning success status
     */
    public LazyTickWorker(int tMax, Supplier<Boolean> work) {
        super();
        this.tMax = tMax;
        this.work = work;
        isStaticMax = true;
    }

    /**
     * 使用动态间隔和Runnable工作构造工作器。
     * <p>
     * Construct a worker with a dynamic interval and a Runnable task.
     *
     * @param work 要执行的工作 / the work to execute
     */
    public LazyTickWorker(Runnable work) {
        this(()->{
        	work.run();
        	return true;
        });
    }

    /**
     * 使用动态间隔和返回布尔值的工作构造工作器。
     * <p>
     * Construct a worker with a dynamic interval and a Boolean-returning task.
     *
     * @param work 要执行的工作，返回是否成功 / the work to execute, returning success status
     */
    public LazyTickWorker(Supplier<Boolean> work) {
        super();
        this.work = work;
        isStaticMax = false;
    }

    /**
     * 将当前计数设为最大值，使下一次tick时立即执行。
     * <p>
     * Set the current counter to the maximum, so the work executes on the next tick.
     */
    public void enqueue() {
        tCur = tMax;
    }

    /**
     * 从NBT标签读取工作器状态。
     * <p>
     * Read the worker state from an NBT tag.
     *
     * @param cnbt NBT标签 / the NBT tag
     */
    public void read(CompoundTag cnbt) {
        if (!isStaticMax)
            tMax = cnbt.getInt("max");
        tCur = cnbt.getInt("cur");
    }

    /**
     * 从NBT标签中以指定前缀键读取工作器状态。
     * <p>
     * Read the worker state from an NBT tag with the specified key prefix.
     *
     * @param cnbt NBT标签 / the NBT tag
     * @param key 键前缀 / the key prefix
     */
    public void read(CompoundTag cnbt, String key) {
        if (!isStaticMax)
            tMax = cnbt.getInt(key + "max");
        tCur = cnbt.getInt(key);
    }

    /**
     * 重置计数器为0。
     * <p>
     * Reset the counter to 0.
     */
    public void rewind() {
        tCur = 0;
    }

    /**
     * 执行一次tick计数。当计数达到最大值时执行工作并重置计数器。
     * <p>
     * Perform one tick count. When the counter reaches the maximum, execute the work and reset.
     *
     * @return 工作的执行结果，未执行时返回false / the work execution result, false if not executed
     */
    public boolean tick() {
        if (tMax != 0) {
            tCur++;
            if (tCur >= tMax) {
                tCur = 0;
                return work.get();
            }
        }
        return false;
    }

    /**
     * 将工作器状态写入NBT标签。
     * <p>
     * Write the worker state to an NBT tag.
     *
     * @param cnbt NBT标签 / the NBT tag
     * @return 写入后的NBT标签 / the NBT tag after writing
     */
    public CompoundTag write(CompoundTag cnbt) {
        if (!isStaticMax)
            cnbt.putInt("max", tMax);
        cnbt.putInt("cur", tCur);
        return cnbt;
    }

    /**
     * 将工作器状态以指定前缀键写入NBT标签。
     * <p>
     * Write the worker state to an NBT tag with the specified key prefix.
     *
     * @param cnbt NBT标签 / the NBT tag
     * @param key 键前缀 / the key prefix
     * @return 写入后的NBT标签 / the NBT tag after writing
     */
    public CompoundTag write(CompoundTag cnbt, String key) {
        if (!isStaticMax)
            cnbt.putInt(key + "max", tMax);
        cnbt.putInt(key, tCur);
        return cnbt;
    }
}
