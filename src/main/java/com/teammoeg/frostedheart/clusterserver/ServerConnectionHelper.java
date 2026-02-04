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

package com.teammoeg.frostedheart.clusterserver;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.clusterserver.network.S2CRedirectPacket;

import net.minecraftforge.network.NetworkEvent;

public class ServerConnectionHelper {
	public static ThreadLocal<String> currentToken = ThreadLocal.withInitial(() -> null);

	public static SecretKey currentKey;
	public static boolean isAuthEnabled;
	public static String loginServer;
	public static long timeout;
	public static final String HEADER="\uFF33\uFF51";
	public ServerConnectionHelper() {

	}

	public static void sendRedirect(NetworkEvent.Context ctx,String str,boolean red) {
		FHNetwork.INSTANCE.get().reply(new S2CRedirectPacket(str,red), ctx);
	}
	public static String constructAuthMessage(String name) {
		JsonObject info=new JsonObject();
		//pad head and end with random data to improve security
		info.addProperty("_ps", UUID.randomUUID().toString());
		
		info.addProperty("userName", name);
		info.addProperty("timeout", new Date().getTime()+timeout);
		info.addProperty("es_", UUID.randomUUID().toString());
		return HEADER+"\uFF00"+encode(info.toString());
	}
	public static String constructBackMessage(String name) {
		return HEADER+"\uFF01";
	}
	public static String constructRedirectMessage(String ip,boolean temp) {
		return HEADER+(temp?"\uFF03":"\uFF02")+ip;
	}

	public static String encode(String data) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, currentKey,new IvParameterSpec(new byte[cipher.getBlockSize()]));
			return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	public static String decode(String data) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, currentKey,new IvParameterSpec(new byte[cipher.getBlockSize()]));
			return new String(cipher.doFinal(Base64.getDecoder().decode(data)), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
