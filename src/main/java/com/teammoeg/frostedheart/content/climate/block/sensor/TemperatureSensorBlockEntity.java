/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block.sensor;

import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TemperatureSensorBlockEntity extends CBlockEntity implements MenuProvider, CTickableBlockEntity {
    private float targetTemperature;
    private boolean inverted;
    private float temperature;
    private int ticks;
    public TemperatureSensorBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.TEMPERATURE_SENSOR.get(), pos, state);
    }
    public float getTargetTemperature() { return targetTemperature; }
    public float getTemperature() { return temperature; }
    public boolean isInverted() { return inverted; }
    public boolean isPowered() { return getBlockState().getValue(BlockStateProperties.POWERED); }
    public void configure(float target, boolean invert) {
        if (!Float.isFinite(target)) return;
        targetTemperature = target;
        inverted = invert;
        setChanged();
        refresh();
    }
    @Override public void tick() {
        if (level != null && !level.isClientSide && ticks++ % 20 == 0) refresh();
    }
    private void refresh() {
        if (level == null || level.isClientSide) return;
        temperature = WorldTemperature.block(level, worldPosition);
        boolean powered = inverted ? temperature < targetTemperature : temperature >= targetTemperature;
        if (powered != isPowered()) {
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.POWERED, powered), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values())
                level.updateNeighborsAt(worldPosition.relative(side), getBlockState().getBlock());
        }
    }
    @Override public void writeCustomNBT(CompoundTag tag, boolean descPacket) {
        tag.putFloat("TargetTemperature", targetTemperature);
        tag.putBoolean("Inverted", inverted);
    }
    @Override public void readCustomNBT(CompoundTag tag, boolean descPacket) {
        float target = tag.getFloat("TargetTemperature");
        targetTemperature = Float.isFinite(target) ? target : 0;
        inverted = tag.getBoolean("Inverted");
        ticks = 0;
    }
    @Override public Component getDisplayName() { return Component.translatable("block.frostedheart.temperature_sensor"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TemperatureSensorMenu(id, inventory, this);
    }
}
