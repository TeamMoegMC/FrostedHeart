package com.teammoeg.frostedheart.clusterserver;

import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
