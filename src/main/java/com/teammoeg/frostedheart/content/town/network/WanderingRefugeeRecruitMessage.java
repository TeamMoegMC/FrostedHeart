package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public class WanderingRefugeeRecruitMessage implements CMessage {
    final int refugeeID;


    public WanderingRefugeeRecruitMessage(int refugeeID) {
        this.refugeeID = refugeeID;
    }

    /**
     * Decodes the message
     */
    public WanderingRefugeeRecruitMessage(FriendlyByteBuf buffer){
        this.refugeeID = buffer.readVarInt();
    }


    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(refugeeID);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Entity entity = player.level().getEntity(this.refugeeID);

                if (entity instanceof WanderingRefugee refugee) {
                    if(player.distanceTo(entity) > 16.0D){
                        player.displayClientMessage(Component.translatable("message.frostedheart.wandering_refugee.too_far_to_recruit"), false);
                    } else{
                        TeamTown town = TeamTown.from(player);
                        town.addResident(refugee.getFirstName(), refugee.getLastName());
                        //随便产生点粒子效果吧
                        {
                            for (int i = 0; i < 16; i++) {
                                player.level().addParticle(ParticleTypes.EXPLOSION, refugee.getX(), refugee.getY(), refugee.getZ(), Math.random(), Math.random(), 0.01D);
                                player.level().addParticle(ParticleTypes.END_ROD, refugee.getX(), refugee.getY(), refugee.getZ(), Math.random(), Math.random(), 0.01D);
                            }
                        }
                        player.displayClientMessage(Component.translatable("message.frostedheart.wandering_refugee.recruited"), false);
                        refugee.remove(Entity.RemovalReason.DISCARDED);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
