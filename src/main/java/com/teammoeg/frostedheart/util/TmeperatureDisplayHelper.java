package com.teammoeg.frostedheart.util;

import java.util.Collection;

import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.climate.network.FHTemperatureDisplayPacket;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.PacketDistributor;
/**
 * A Helper for showing temperature in ui and message as well as convert them accordingly to client unit setting
 * 
 * */
public class TmeperatureDisplayHelper {

	public static void sendTemperature(ServerPlayerEntity pe,String format,int...temps) {
		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->pe),new FHTemperatureDisplayPacket(format,temps));
	}

	public static void sendTemperature(ServerPlayerEntity pe,String format,float...temps) {
		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->pe),new FHTemperatureDisplayPacket(format,temps));
	}

	public static void sendTemperature(Collection<ServerPlayerEntity> pe,String format,int...temps) {
		FHTemperatureDisplayPacket k=new FHTemperatureDisplayPacket(format,temps);
		for(ServerPlayerEntity p:pe)
		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->p),k);
	}

	public static void sendTemperature(Collection<ServerPlayerEntity> pe,String format,float...temps) {
		FHTemperatureDisplayPacket k=new FHTemperatureDisplayPacket(format,temps);
		for(ServerPlayerEntity p:pe)
		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->p),k);
	}

	public static void sendTemperatureStatus(ServerPlayerEntity pe,String format,boolean act,int...temps) {
		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->pe),new FHTemperatureDisplayPacket(format,act,temps));
	}

	public static void sendTemperatureStatus(ServerPlayerEntity pe,String format,boolean act,float...temps) {
		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->pe),new FHTemperatureDisplayPacket(format,act,temps));
	}

	public static void sendTemperatureStatus(Collection<ServerPlayerEntity> pe,String format,boolean act,int...temps) {
		FHTemperatureDisplayPacket k=new FHTemperatureDisplayPacket(format,act,temps);
		for(ServerPlayerEntity p:pe)
		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->p),k);
	}

	public static void sendTemperatureStatus(Collection<ServerPlayerEntity> pe,String format,boolean act,float...temps) {
		FHTemperatureDisplayPacket k=new FHTemperatureDisplayPacket(format,act,temps);
		for(ServerPlayerEntity p:pe)
		FHPacketHandler.send(PacketDistributor.PLAYER.with(()->p),k);
	}

	public static String toTemperatureIntString(float celsus) {
		celsus=Math.max(-273.15f, celsus);
		if(FHConfig.CLIENT.useFahrenheit.get())
			return ((int)(celsus*9/5+32))+" °F";
		return ((int)celsus)+" °C";
	}

	public static String toTemperatureFloatString(float celsus) {
		celsus=Math.max(-273.15f, celsus);
		if(FHConfig.CLIENT.useFahrenheit.get())
			return ((int)((celsus*9/5+32)*10))/10f+" °F";
		return ((int)(celsus*10))/10f+" °C";
	}

	public static String toTemperatureDeltaIntString(float celsus) {
		//celsus=Math.max(-273.15f, celsus);
		if(FHConfig.CLIENT.useFahrenheit.get())
			return ((int)(celsus*9/5))+" °F";
		return ((int)celsus)+" °C";
	}

	public static String toTemperatureDeltaFloatString(float celsus) {
		//celsus=Math.max(-273.15f, celsus);
		if(FHConfig.CLIENT.useFahrenheit.get())
			return ((int)((celsus*9/5)*10))/10f+" °F";
		return ((int)(celsus*10))/10f+" °C";
	}

}
