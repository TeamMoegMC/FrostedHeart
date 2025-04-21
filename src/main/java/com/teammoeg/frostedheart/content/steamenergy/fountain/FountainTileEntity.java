/*
 * Copyright (c) 2024 TeamMoeg
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

import blusunrize.immersiveengineering.common.blocks.IEBaseBlockEntity;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.teammoeg.chorda.block.CBlockInterfaces;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHAttributes;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public class FountainTileEntity extends CBlockEntity implements CTickableBlockEntity,
        CBlockInterfaces.IActiveState, HeatNetworkProvider, IHaveGoggleInformation {

    public static final int RANGE_PER_NOZZLE = 1;
    public static final int MAX_HEIGHT = 5;
    private static final UUID WARMTH_EFFECT_UUID = UUID.fromString("95c1f024-8f3a-4828-aaa7-a86733cffbf2");
    private static final float POWER_CAP = 400;
    private static final float REFILL_THRESHOLD = 200;
    HeatEndpoint network = HeatEndpoint.consumer(10, 1);;
    LazyOptional<HeatEndpoint> heatcap = LazyOptional.of(() -> network);
    private float power = 0;
    private boolean refilling = false;
    private int height = 0;
    private int heatRange = 0;
    private boolean heatAdjusted = false;
    private float lastTemp;

    public FountainTileEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.FOUNTAIN.get(), pos, state);
    }

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if (capability == FHCapabilities.HEAT_EP.capability() && facing == Direction.DOWN) {
            return heatcap.cast();
        }
        return super.getCapability(capability, facing);
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
    public void tick() {
        // server side logic
        if (!level.isClientSide) {
            // power logic
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
                                        FHBlocks.FOUNTAIN_NOZZLE.get().defaultBlockState()
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
                this.syncData();

            // grant player effect if structure is valid
            if (height > 0 && power > 0) {
                setChanged();
                this.syncData();
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
            if (level.random.nextInt(10) == 0) {
                BlockPos water = findWater();
                if (water != null) {
                    FHClientUtils.spawnSteamParticles(level, water);
                }
            }
        }
    }

    private BlockPos findWater() {
        if (getRange() == 0) return null;

        BlockState state = getBlock();
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
            if (level.getBlockState(nozzle).getBlock() != FHBlocks.FOUNTAIN_NOZZLE.get())
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
        float networkTemp = network.getTempLevel();
        if (lastTemp == networkTemp && heatAdjusted && range == heatRange) return;

        lastTemp = networkTemp;

        removeHeat();
        ChunkHeatData.addPillarTempAdjust(level, worldPosition, (heatRange = range), height + 1, 1, (int) lastTemp * 15);
        heatAdjusted = true;
    }

    public void refill() {
        refilling = true;
    }


    @Override
    public void onRemoved() {
        super.onRemoved();
        removeHeat();

        for (int i = 0; i < MAX_HEIGHT; i++) {
            BlockPos nozzle = worldPosition.relative(Direction.UP, i + 1);
            if (level.getBlockState(nozzle).getBlock() == FHBlocks.FOUNTAIN_NOZZLE.get()) {
                level.setBlock(nozzle,
                        FHBlocks.FOUNTAIN_NOZZLE.get().defaultBlockState()
                                .setValue(FountainNozzleBlock.HEIGHT, 0),
                        Block.UPDATE_ALL_IMMEDIATE
                );
            }
        }
    }

    private void removeHeat() {

        ChunkHeatData.removeTempAdjust(level, worldPosition);
        heatAdjusted = false;
    }
	@Override
	public void invalidateCaps() {
		heatcap.invalidate();
		super.invalidateCaps();
	}

    @Override
    public @Nullable HeatNetwork getNetwork() {
        return network.getNetwork();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return TemperatureGoogleRenderer.addHeatNetworkInfoToTooltip(tooltip, isPlayerSneaking, worldPosition);
    }
}
