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

package com.teammoeg.chorda.dataholders.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.teammoeg.chorda.Chorda;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * 客户端数据存储管理器。负责客户端本地数据的加载、保存和持久化。
 * 数据以压缩 NBT 格式存储在游戏目录下的 chorda.client.dat 文件中，
 * 并维护一个 .old 备份文件以防数据损坏时的回退。
 * <p>
 * Client data storage manager. Handles loading, saving, and persistence of client-local data.
 * Data is stored in compressed NBT format in the chorda.client.dat file under the game directory,
 * with a .old backup file maintained for fallback in case of data corruption.
 */
public class CClientDataStorage {
	private static ClientDataHolder holder=new ClientDataHolder();
	public static File clientDataFile=new File(FMLPaths.GAMEDIR.get().toFile(),"chorda.client.dat") ;
	public static File clientDataOldFile=new File(FMLPaths.GAMEDIR.get().toFile(),"chorda.client.dat.old");
	public static Object ioLock=new Object();
	/**
	 * 获取客户端数据持有者实例。
	 * <p>
	 * Gets the client data holder instance.
	 *
	 * @return 客户端数据持有者 / the client data holder
	 */
	public static ClientDataHolder getData() {
		return holder;
	}
	/**
	 * 从磁盘加载客户端数据。如果主文件不存在或损坏，尝试加载备份文件。
	 * <p>
	 * Loads client data from disk. Falls back to the backup file if the main file does not exist or is corrupted.
	 */
	public static void load() {
		Chorda.LOGGER.info("Loading client stored data");
		File toread=clientDataFile;
		if(!toread.exists())
			toread=clientDataOldFile;
		if(toread.exists())
			try {
				CompoundTag nbt = NbtIo.readCompressed(toread);
				holder.load(nbt, false);
			} catch (Exception e) {
				
				e.printStackTrace();
				if(toread!=clientDataOldFile) {
					Chorda.LOGGER.error("Can not load client stored data, trying to load older one");
					toread=clientDataOldFile;
					try {
						CompoundTag nbt = NbtIo.readCompressed(toread);
						holder.load(nbt, false);
						return;
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					
				}
				Chorda.LOGGER.error("Can not load client stored data");
			}
	}
	/**
	 * 检查数据是否有变更，如果有则异步保存到磁盘。
	 * 保存前会将当前文件备份为 .old 文件，IO 操作在独立线程池中执行。
	 * <p>
	 * Checks if data has been modified and saves to disk asynchronously if so.
	 * Backs up the current file as .old before saving. IO operations are executed in a separate thread pool.
	 */
	public static void checkAndSave() {
		CompoundTag saved=null;
		synchronized(holder.lock) {
			if(holder.isDirty) {
				saved=holder.serializeNBT();
				holder.isDirty=false;
			}
		}
		if(saved!=null){
			final CompoundTag fsaved=saved;
			Util.ioPool().submit(()->{
				synchronized(ioLock) {
					Chorda.LOGGER.info("Saving client stored data");
					if(clientDataFile.exists())
					try {
						Files.move(clientDataFile.toPath(), clientDataOldFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						e.printStackTrace();
						Chorda.LOGGER.error("Can not save client stored data: backup failed");
					}
					try {
						NbtIo.writeCompressed(fsaved, clientDataFile);
					} catch (IOException e) {
						e.printStackTrace();
						Chorda.LOGGER.error("Can not save client stored data: IO failed");
					}
				}
			});
		}
		
		
	}
}
