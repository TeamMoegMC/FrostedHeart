/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.climate.network;

import java.util.Collection;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHPacketHandler;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.PlayerTemperature;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

public class FHTemperatureDisplayPacket {
    private final int[] temp;
    private final String langKey;
    private final boolean isStatus;
    private final boolean isAction;
    public FHTemperatureDisplayPacket(String format,int...data) {
        this.langKey=format;
        this.temp=data;
        for(int i=0;i<temp.length;i++)
        	temp[i]*=10;
        isStatus=false;
        isAction=false;
    }
    public FHTemperatureDisplayPacket(String format,float...data) {
        this.langKey=format;
        temp=new int[data.length];
        for(int i=0;i<data.length;i++)
        	temp[i]=(int) (data[i]*10);
        isStatus=false;
        isAction=false;
    }
    public FHTemperatureDisplayPacket(String format,boolean isAction,int...data) {
        this.langKey=format;
        this.temp=data;
        for(int i=0;i<temp.length;i++)
        	temp[i]*=10;
        isStatus=true;
        this.isAction=isAction;
    }
    public FHTemperatureDisplayPacket(String format,boolean isAction,float...data) {
        this.langKey=format;
        temp=new int[data.length];
        for(int i=0;i<data.length;i++)
        	temp[i]=(int) (data[i]*10);
        isStatus=true;
        this.isAction=isAction;
    }
    public FHTemperatureDisplayPacket(PacketBuffer buffer) {
        langKey=buffer.readString();
        temp=buffer.readVarIntArray();
        boolean[] bs=SerializeUtil.readBooleans(buffer);
        isStatus=bs[0];
        isAction=bs[1];
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeString(langKey);
        buffer.writeVarIntArray(temp);
        SerializeUtil.writeBooleans(buffer,isStatus,isAction);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            PlayerEntity player = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> ClientUtils::getPlayer);
            Object[] ss=new Object[temp.length];
            for(int i=0;i<ss.length;i++) {
            	ss[i]=GuiUtils.toTemperatureIntString(temp[i]/10f);
            }
            TranslationTextComponent tosend=new TranslationTextComponent("message." + FHMain.MODID + "."+langKey,ss);
            if(isStatus)
            	player.sendStatusMessage(tosend, false);
            else
            	player.sendMessage(tosend,player.getUniqueID());
            
        });
        context.get().setPacketHandled(true);
    }
    public static void send(ServerPlayerEntity pe,String format,int...temps) {
    	FHPacketHandler.send(PacketDistributor.PLAYER.with(()->pe),new FHTemperatureDisplayPacket(format,temps));
    }
    public static void send(ServerPlayerEntity pe,String format,float...temps) {
    	FHPacketHandler.send(PacketDistributor.PLAYER.with(()->pe),new FHTemperatureDisplayPacket(format,temps));
    }
    public static void send(Collection<ServerPlayerEntity> pe,String format,int...temps) {
    	FHTemperatureDisplayPacket k=new FHTemperatureDisplayPacket(format,temps);
    	for(ServerPlayerEntity p:pe)
    	FHPacketHandler.send(PacketDistributor.PLAYER.with(()->p),k);
    }
    public static void send(Collection<ServerPlayerEntity> pe,String format,float...temps) {
    	FHTemperatureDisplayPacket k=new FHTemperatureDisplayPacket(format,temps);
    	for(ServerPlayerEntity p:pe)
    	FHPacketHandler.send(PacketDistributor.PLAYER.with(()->p),k);
    }
    public static void sendStatus(ServerPlayerEntity pe,String format,boolean act,int...temps) {
    	FHPacketHandler.send(PacketDistributor.PLAYER.with(()->pe),new FHTemperatureDisplayPacket(format,act,temps));
    }
    public static void sendStatus(ServerPlayerEntity pe,String format,boolean act,float...temps) {
    	FHPacketHandler.send(PacketDistributor.PLAYER.with(()->pe),new FHTemperatureDisplayPacket(format,act,temps));
    }
    public static void sendStatus(Collection<ServerPlayerEntity> pe,String format,boolean act,int...temps) {
    	FHTemperatureDisplayPacket k=new FHTemperatureDisplayPacket(format,act,temps);
    	for(ServerPlayerEntity p:pe)
    	FHPacketHandler.send(PacketDistributor.PLAYER.with(()->p),k);
    }
    public static void sendStatus(Collection<ServerPlayerEntity> pe,String format,boolean act,float...temps) {
    	FHTemperatureDisplayPacket k=new FHTemperatureDisplayPacket(format,act,temps);
    	for(ServerPlayerEntity p:pe)
    	FHPacketHandler.send(PacketDistributor.PLAYER.with(()->p),k);
    }
}
