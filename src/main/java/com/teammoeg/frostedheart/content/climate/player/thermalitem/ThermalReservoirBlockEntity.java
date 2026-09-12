/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.player.thermalitem;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persists one complete reservoir item and exchanges only while the block is ticking. */
public class ThermalReservoirBlockEntity extends BlockEntity {
    private static final String ITEM_KEY = "ReservoirItem";
    // All server block ticks run on the server thread, as do dropped-item hooks.
    private static final DroppedReservoirExchangeHandler EXCHANGE = new DroppedReservoirExchangeHandler();
    private static final ThermalEnvironmentSample ENVIRONMENT = new ThermalEnvironmentSample();
    private ItemStack storedStack = ItemStack.EMPTY;
    private int loadedTicks;

    public ThermalReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.THERMAL_RESERVOIR.get(), pos, state);
    }

    public void setStoredStack(ItemStack stack) {
        storedStack = stack.copyWithCount(1);
        syncChanged();
    }

    public ItemStack copyStoredStack() {
        return storedStack.isEmpty() ? new ItemStack(getBlockState().getBlock()) : storedStack.copy();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ThermalReservoirBlockEntity reservoir) {
        if (!(level instanceof ServerLevel server)) return;
        // No elapsed-time catch-up after chunk unload; distribute work across positions.
        if (++reservoir.loadedTicks < DroppedReservoirExchangeHandler.CADENCE_TICKS
                || Math.floorMod(level.getGameTime() + pos.asLong(), DroppedReservoirExchangeHandler.CADENCE_TICKS) != 0) {
            return;
        }
        if (reservoir.storedStack.isEmpty()) {
            reservoir.storedStack = new ItemStack(state.getBlock());
        }
        if (!(reservoir.storedStack.getItem() instanceof WearableThermalReservoir thermal)) return;
        double natural = WorldTemperature.naturalAir(server, pos);
        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(server, pos, natural, ENVIRONMENT);
        DroppedReservoirExchangeHandler.Status status = EXCHANGE.exchangeInto(reservoir.storedStack, thermal,
                ENVIRONMENT.airAvailable() ? ENVIRONMENT.airTemperatureC() : natural,
                ENVIRONMENT.radiantFluxWPerM2(), DroppedReservoirExchangeHandler.ELAPSED_SECONDS);
        if (status.stackWriteCount() > 0) reservoir.syncChanged();
    }

    private void syncChanged() {
        setChanged();
        if (level instanceof ServerLevel) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ITEM_KEY, copyStoredStack().save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedStack = ItemStack.of(tag.getCompound(ITEM_KEY));
        if (!storedStack.isEmpty()) storedStack.setCount(1);
        loadedTicks = 0;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
