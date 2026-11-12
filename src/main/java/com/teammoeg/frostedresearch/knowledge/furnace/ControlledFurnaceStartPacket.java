package com.teammoeg.frostedresearch.knowledge.furnace;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.Optional;
import java.util.function.Supplier;

/** The ordinary CUI start carries the mode/target that was displayed when its button was built. */
public record ControlledFurnaceStartPacket(int containerId,BlockPos pos,boolean trial,Optional<ResearchSampleBinding> target) implements CMessage {
    public ControlledFurnaceStartPacket(FriendlyByteBuf b){
        this(b.readVarInt(),b.readBlockPos(),b.readBoolean(),b.readOptional(in -> new ResearchSampleBinding(in.readUUID(),in.readUUID(),in.readUtf(64),in.readResourceLocation())));
    }
    @Override public void encode(FriendlyByteBuf b){
        b.writeVarInt(containerId);b.writeBlockPos(pos);b.writeBoolean(trial);
        b.writeOptional(target,(out,t)->{out.writeUUID(t.team());out.writeUUID(t.run());out.writeUtf(t.requirement(),64);out.writeResourceLocation(t.experiment());});
    }
    @Override public void handle(Supplier<NetworkEvent.Context> context){
        context.get().enqueueWork(()->{var p=context.get().getSender();if(p!=null)apply(p);});context.get().setPacketHandled(true);
    }
    public void apply(ServerPlayer player){
        if(player.containerMenu instanceof ControlledFurnaceMenu menu && menu.containerId==containerId && menu.getBlock().getBlockPos().equals(pos))
            menu.startSeen(player,trial,target);
    }
}
