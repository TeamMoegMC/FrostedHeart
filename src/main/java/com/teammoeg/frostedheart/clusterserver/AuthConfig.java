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

package com.teammoeg.frostedheart.clusterserver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.teammoeg.chorda.io.FileUtil;

import net.minecraftforge.fml.loading.FMLPaths;

public class AuthConfig {
	public static class ServerEntry{
		public String address;
		public boolean hidden;
		public String name;
		public String desc;
	}
	public AuthConfig() {
	}
	public final static Gson gson=new Gson();
	public static Map<String,ServerEntry> servers=new LinkedHashMap<>();
	public static void reload() {
		File authConfig=new File(FMLPaths.CONFIGDIR.get().toFile(),"auth.json");
		if(authConfig.exists()) {
			try {
				JsonObject authCfg=JsonParser.parseString(FileUtil.readString(authConfig)).getAsJsonObject();
				SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
				PBEKeySpec spec = new PBEKeySpec(authCfg.get("key").getAsString().toCharArray(), "Frostedheart".getBytes(StandardCharsets.UTF_8), 65536, 256);
				SecretKey tmp = factory.generateSecret(spec);
				ServerConnectionHelper.currentKey = new SecretKeySpec(tmp.getEncoded(), "AES");
				ServerConnectionHelper.isAuthEnabled=authCfg.get("enabled").getAsBoolean();
				ServerConnectionHelper.timeout=authCfg.get("timeout").getAsLong();
				ServerConnectionHelper.loginServer=authCfg.get("loginServer").getAsString();
				servers.clear();
				if(authCfg.has("servers")) {
					for(JsonElement je:authCfg.get("servers").getAsJsonArray()) {
						ServerEntry se=gson.fromJson(je, ServerEntry.class);
						servers.put(se.name, se);
					}
				}
			} catch (JsonSyntaxException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
				e.printStackTrace();
			}
			
		}
	}
}
