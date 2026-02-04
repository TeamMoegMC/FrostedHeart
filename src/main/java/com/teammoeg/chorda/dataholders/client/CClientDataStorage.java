/*
 * Copyright (c) 2024 TeamMoeg
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

public class CClientDataStorage {
	private static ClientDataHolder holder=new ClientDataHolder();
	public static File clientDataFile=new File(FMLPaths.GAMEDIR.get().toFile(),"chorda.client.dat") ;
	public static File clientDataOldFile=new File(FMLPaths.GAMEDIR.get().toFile(),"chorda.client.dat.old");
	public static Object ioLock=new Object();
	public static ClientDataHolder getData() {
		return holder;
	}
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
