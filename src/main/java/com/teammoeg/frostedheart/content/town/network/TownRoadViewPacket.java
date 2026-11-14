package com.teammoeg.frostedheart.content.town.network;

import java.util.*;
import java.util.function.Supplier;
import com.teammoeg.frostedheart.content.town.block.blockscanner.TownConnectivity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

/** Spanning-forest witnesses only; each adjacent step occupies one byte. */
public final class TownRoadViewPacket implements TownViewPacket {
    public final UUID holder, version;
    public final ResourceLocation dimension;
    public final TownConnectivity.Result view;
    public TownRoadViewPacket(UUID holder, ResourceLocation dimension, UUID version, TownConnectivity.Result view) {
        this.holder = holder; this.dimension = dimension; this.version = version; this.view = view;
    }
    public TownRoadViewPacket(FriendlyByteBuf buf) {
        holder = buf.readUUID(); dimension = buf.readResourceLocation(); version = buf.readUUID();
        Set<BlockPos> connected = new HashSet<>(buf.readList(FriendlyByteBuf::readBlockPos));
        Set<BlockPos> working = new HashSet<>(buf.readList(FriendlyByteBuf::readBlockPos));
        List<TownConnectivity.Route> routes = buf.readList(input -> {
            BlockPos from = input.readBlockPos(), to = input.readBlockPos();
            boolean powered = input.readBoolean();
            int length = input.readVarInt();
            List<BlockPos> steps = new ArrayList<>();
            if (length > 0) {
                BlockPos pos = input.readBlockPos(); steps.add(pos);
                for (int i = 1; i < length; i++) {
                    int step = input.readUnsignedByte();
                    pos = pos.offset((step & 3) - 1, ((step >> 2) & 3) - 1, ((step >> 4) & 3) - 1);
                    steps.add(pos);
                }
            }
            return new TownConnectivity.Route(from, to, steps, powered);
        });
        view = new TownConnectivity.Result(connected, working, routes);
    }
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(holder); buf.writeResourceLocation(dimension); buf.writeUUID(version);
        buf.writeCollection(view.connected(), FriendlyByteBuf::writeBlockPos);
        buf.writeCollection(view.working(), FriendlyByteBuf::writeBlockPos);
        buf.writeCollection(view.routes(), (output, route) -> {
            output.writeBlockPos(route.from()); output.writeBlockPos(route.to()); output.writeBoolean(route.powered());
            output.writeVarInt(route.steps().size());
            BlockPos previous = null;
            for (BlockPos pos : route.steps()) {
                if (previous == null) output.writeBlockPos(pos);
                else output.writeByte((pos.getX() - previous.getX() + 1)
                        | ((pos.getY() - previous.getY() + 1) << 2)
                        | ((pos.getZ() - previous.getZ() + 1) << 4));
                previous = pos;
            }
        });
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> {
            if (com.teammoeg.frostedheart.content.town.client.TownClientUpdates.canApply(ctx.getNetworkManager()))
                com.teammoeg.frostedheart.content.town.render.TownRoadRenderer.accept(this);
        });
        ctx.setPacketHandled(true);
    }
}
