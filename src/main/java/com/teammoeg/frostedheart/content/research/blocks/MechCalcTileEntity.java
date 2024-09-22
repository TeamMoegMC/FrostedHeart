/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.blocks;

import java.util.List;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.teammoeg.frostedheart.FHSounds;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MechCalcTileEntity extends KineticBlockEntity implements IHaveGoggleInformation {
    int processMax = 6400;
    public int process = 0;
    int currentPoints = 0;
    int lastact;
    int maxPoints = 100;
    boolean doProduct = true;
    boolean requireUpdate;

    Direction last;

    int ticsSlp;//ticks since last sound play

    public MechCalcTileEntity(BlockPos pos,BlockState state) {
        super(FHTileTypes.MECH_CALC.get(), pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        boolean flag = true;
        float spd = Mth.abs(super.getSpeed());
        if (spd > 64) {
            tooltip.add(TranslateUtils.translateTooltip("mechanical_calculator.too_fast").withStyle(ChatFormatting.RED));
            flag = false;
        }
        if (this.currentPoints >= maxPoints) {
            tooltip.add(TranslateUtils.translateTooltip("mechanical_calculator.full").withStyle(ChatFormatting.RED));
            flag = false;
        }
        if (flag && spd > 0)
            tooltip.add(TranslateUtils.translateTooltip("mechanical_calculator.working").withStyle(ChatFormatting.GREEN));
        tooltip.add(TranslateUtils.translateTooltip("mechanical_calculator.points", currentPoints, maxPoints));
        return true;
    }


    @Override
    public float calculateStressApplied() {
        float rspd = Mth.abs(super.getSpeed());
        if (currentPoints < maxPoints && rspd <= 64) {
            this.lastStressApplied = 64;
            return 64;
        }
        this.lastStressApplied = 0;
        return 0;
    }

    public void doNetworkUpdate() {
        if (this.hasNetwork())
            this.getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
    }

    @Override
    protected void read(CompoundTag tag, boolean client) {
    	super.read(tag, client);
        process = tag.getInt("process");
        currentPoints = tag.getInt("pts");
        lastact = tag.getInt("last_calc");
        if (tag.contains("prod"))
            doProduct = tag.getBoolean("prod");
    }

    public Axis getAxis() {
        return this.getDirection().getClockWise().getAxis();
    }

    public Direction getDirection() {
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public InteractionResult onClick(Player pe) {
        if (!pe.level().isClientSide) {
            currentPoints = (int) ResearchDataAPI.getData(pe).doResearch(currentPoints);
            updatePoints();
        }
        return InteractionResult.sidedSuccess(pe.level().isClientSide);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        requireNetworkUpdate();
    }

    public void requireNetworkUpdate() {
        requireUpdate = true;
    }



    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            float spd = Mth.abs(super.getSpeed());

            if (spd > 0 && spd <= 64 && currentPoints <= maxPoints - 20) {
                process += (int) spd;
                int curact = process / 1067;
                if (lastact != curact) {
                    lastact = curact;
                    level.playSound(null, worldPosition, FHSounds.MC_BELL.get(), SoundSource.BLOCKS, 0.1f, 1f);
                }
                if (process >= processMax) {
                    process = 0;
                    lastact = 0;
                    if (doProduct)
                        currentPoints += 20;
                    requireNetworkUpdate();
                }


                if (ticsSlp <= 0) {
                    float pitch = Mth.clamp((spd / 32f) + 0.5f, 0.5f, 2f);
                    level.playSound(null, worldPosition, FHSounds.MC_ROLL.get(), SoundSource.BLOCKS, 0.3f, pitch);
                    ticsSlp = Mth.ceil(20 / pitch);
                } else ticsSlp--;
                this.notifyUpdate();
            }
            if (requireUpdate)
                doNetworkUpdate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void tickAudio() {


    }

    public void updatePoints() {
        process = 0;
        this.notifyUpdate();
        requireNetworkUpdate();
    }

    @Override
    protected void write(CompoundTag tag, boolean client) {
        super.write(tag, client);
        tag.putInt("process", process);
        tag.putInt("pts", currentPoints);
        tag.putInt("last_calc", lastact);
        if (!doProduct)
            tag.putBoolean("prod", doProduct);
    }
}
