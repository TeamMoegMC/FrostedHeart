package com.teammoeg.frostedheart.clusterserver;

import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.util.Base64;
import java.util.Date;

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
		info.addProperty("userName", name);
		info.addProperty("timeout", new Date().getTime()+timeout);
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
			cipher.init(Cipher.ENCRYPT_MODE, currentKey);
			AlgorithmParameters params = cipher.getParameters();
			return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception ex) {
			return null;
		}
	}
	public static String decode(String data) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, currentKey);
			return new String(cipher.doFinal(Base64.getDecoder().decode(data)), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			return null;
		}
	}
}
