package com.teammoeg.frostedheart.content.steamenergy.creative;

import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class HeatManagerBlockEntity extends HeatBlockEntity {
    HeatNetwork manager;
    public HeatManagerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        manager = new HeatNetwork( () -> {
            for (Direction d : Direction.values()) {
                manager.connectTo(level, worldPosition.relative(d),getBlockPos(), d.getOpposite());
            }
        });
        endpoint = new HeatEndpoint(-1, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if(!endpoint.hasValidNetwork())
            manager.addEndpoint(heatcap.cast(), 0, getLevel(), getBlockPos());
        manager.tick(level);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        manager.save(tag, false);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        manager.load(tag, false);
    }
}
