/*
 * Copyright (c) 2022-2024 TeamMoeg
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
 *
 */

package com.teammoeg.frostedheart.content.steamenergy.fountain;

import com.teammoeg.frostedheart.FHAttributes;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHBlockEntityTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.block.FHTickableBlockEntity;
import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import blusunrize.immersiveengineering.common.blocks.IEBaseBlockEntity;

import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatConsumerEndpoint;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import javax.annotation.Nonnull;
import java.util.UUID;

public class FountainTileEntity extends IEBaseBlockEntity implements
        INetworkConsumer, FHTickableBlockEntity, FHBlockInterfaces.IActiveState {

    private static final UUID WARMTH_EFFECT_UUID = UUID.fromString("95c1f024-8f3a-4828-aaa7-a86733cffbf2");
    private static final float POWER_CAP = 400;
    private static final float REFILL_THRESHOLD = 200;
    public static final int RANGE_PER_NOZZLE = 1;
    public static final int MAX_HEIGHT = 5;

    private float power = 0;
    private boolean refilling = false;
    private int height = 0;
    private int heatRange = 0;
    private boolean heatAdjusted = false;
    private float lastTemp;

    HeatConsumerEndpoint network = new HeatConsumerEndpoint(10, 10, 1);

    public FountainTileEntity(BlockPos pos,BlockState state) {
        super(FHBlockEntityTypes.FOUNTAIN.get(),pos,state);
    }

    LazyOptional<HeatConsumerEndpoint> heatcap=LazyOptional.of(()->network);
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if(capability== FHCapabilities.HEAT_EP.capability()&&facing==Direction.DOWN) {
            return heatcap.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return to == Direction.DOWN;
    }

    @Override
    public boolean connect(HeatEnergyNetwork network, Direction d, int distance) {
        return false;
    }

    public boolean isWorking() {
        return height > 0 && power > 0;
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        power = nbt.getFloat("power");
        refilling = nbt.getBoolean("refilling");
        height = nbt.getInt("height");
        heatAdjusted = nbt.getBoolean("heatAdjusted");
        lastTemp = nbt.getFloat("lastTemp");
    }

    @Override
    public void receiveMessageFromServer(CompoundTag message) {
        super.receiveMessageFromServer(message);
    }

    @Override
    public void tick() {
        // server side logic
        if (!level.isClientSide) {
            // power logic
            if (network.hasValidNetwork()) {
                // start refill if power is below REFILL_THRESHOLD
                // keep refill until network is full
                if (refilling || power < REFILL_THRESHOLD) {
                    float actual = network.drainHeat(Math.min(200, (POWER_CAP - power) / 0.8F));
                    // during refill, grow power and show steam
                    if (actual > 0) {
                        refilling = true;
                        power += actual * 0.8F;
                        this.setActive(true);
                    } else {
                        // finished refill, check structure, grant effects, happens every 200 ticks
                        height = findNozzleHeight();

                        if (height != 0) {


                            // Configure blockstates for nozzles
                            for (int i = 0; i < height; i++) {
                                BlockPos nozzle = worldPosition.relative(Direction.UP, i + 1);
                                level.setBlock(nozzle,
                                        FHBlocks.fountain_nozzle.get().defaultBlockState()
                                                .setValue(FountainNozzleBlock.HEIGHT, i + 1),
                                        Block.UPDATE_ALL_IMMEDIATE
                                );
                            }


                            refilling = false;
                            this.setActive(false);
                        }
                    }
                } else {
                    // if not refilling, consume power
                    power--;
                }
                setChanged();
                this.markContainingBlockForUpdate(null);
            } else this.setActive(false);

            // grant player effect if structure is valid
            if (height > 0 && power > 0) {
                setChanged();
                this.markContainingBlockForUpdate(null);
                adjustHeat(getRange());

                for (Player p : this.getLevel().players()) {
                    removeWarmth((ServerPlayer) p);

                    if (p.distanceToSqr(
                            this.getBlockPos().getX() + 0.5,
                            this.getBlockPos().getY() + 0.5,
                            this.getBlockPos().getZ() + 0.5) < (getRange() * getRange()) &&
                            p.isInWater() &&
                            p.getY() > this.getBlockPos().getY() - 0.5 &&
                            p.getY() < this.getBlockPos().getY() + height + 0.5
                    ) {
                        grantWarmth((ServerPlayer) p);
                    }
                }
            } else {
                removeHeat();
            }
        } else {
            // make water steamy
            if (level.random.nextInt(4) == 0) {
                BlockPos water = findWater();
                if (water != null) {
                    ClientUtils.spawnSteamParticles(level, water);
                }
            }
        }
    }

    private BlockPos findWater() {
        if (getRange() == 0) return null;

        BlockState state = getState();
        BlockPos pos = null;
        int tries = 0;

        while (!state.getFluidState().is(FluidTags.WATER)) {
            if (tries > 2) return null;

            tries++;
            state = level.getBlockState(
                    pos = getBlockPos()
                            .north(level.random.nextInt(getRange() * 2) - getRange())
                            .east(level.random.nextInt(getRange() * 2) - getRange())
            );
        }

        return pos;
    }

    public int getRange() {
        return height * RANGE_PER_NOZZLE + 1; // +1 for the edge
    }

    private void grantWarmth(ServerPlayer player) {
        player.getAttribute(FHAttributes.ENV_TEMPERATURE.get()).addTransientModifier(
                new AttributeModifier(WARMTH_EFFECT_UUID, "fountain warmth", lastTemp * 25, AttributeModifier.Operation.ADDITION)
        );
    }

    private void removeWarmth(ServerPlayer player) {
        player.getAttribute(FHAttributes.ENV_TEMPERATURE.get()).removeModifier(WARMTH_EFFECT_UUID);
    }


    private int findNozzleHeight() {
        assert level != null;

        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos nozzle = worldPosition.relative(Direction.UP, i + 1);
            if (level.getBlockState(nozzle).getBlock() != FHBlocks.fountain_nozzle.get())
                return i;
        }

        return MAX_HEIGHT;
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.putFloat("power", power);
        nbt.putBoolean("refilling", refilling);
        nbt.putInt("height", height);
        nbt.putBoolean("heatAdjusted", heatAdjusted);
    }

    private void adjustHeat(int range) {
        float networkTemp = network.getTemperatureLevel();
        if (lastTemp == networkTemp && heatAdjusted && range == heatRange) return;

        lastTemp = networkTemp;

        removeHeat();
        ChunkHeatData.addPillarTempAdjust(level, worldPosition, (heatRange = range), height + 1,1, (int) lastTemp * 15);
        heatAdjusted = true;
    }

    public void refill() {
        refilling = true;
    }


	@Override
    public void setRemovedIE() {
        super.setRemovedIE();
        removeHeat();

        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos nozzle = worldPosition.relative(Direction.UP, i + 1);
            if (level.getBlockState(nozzle).getBlock() == FHBlocks.fountain_nozzle.get()) {
                level.setBlock(nozzle,
                        FHBlocks.fountain_nozzle.get().defaultBlockState()
                                .setValue(FountainNozzleBlock.HEIGHT, 0),
                        Block.UPDATE_ALL_IMMEDIATE
                );
            }
        }
    }

    private void removeHeat() {
        if (!heatAdjusted) return;

        ChunkHeatData.removeTempAdjust(level, worldPosition);
        heatAdjusted = false;
    }
}
