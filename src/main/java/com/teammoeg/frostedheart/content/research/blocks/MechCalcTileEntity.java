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

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.teammoeg.frostedheart.FHSounds;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MechCalcTileEntity extends KineticTileEntity implements IHaveGoggleInformation {
    int processMax = 6400;
    public int process = 0;
    int currentPoints = 0;
    int lastact;
    int maxPoints = 100;
    boolean doProduct = true;
    boolean requireUpdate;

    Direction last;

    int ticsSlp;//ticks since last sound play

    public MechCalcTileEntity() {
        super(FHTileTypes.MECH_CALC.get());
    }

    @Override
    public boolean addToGoggleTooltip(List<ITextComponent> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        boolean flag = true;
        float spd = MathHelper.abs(super.getSpeed());
        if (spd > 64) {
            tooltip.add(TranslateUtils.translateTooltip("mechanical_calculator.too_fast").withStyle(TextFormatting.RED));
            flag = false;
        }
        if (this.currentPoints >= maxPoints) {
            tooltip.add(TranslateUtils.translateTooltip("mechanical_calculator.full").withStyle(TextFormatting.RED));
            flag = false;
        }
        if (flag && spd > 0)
            tooltip.add(TranslateUtils.translateTooltip("mechanical_calculator.working").withStyle(TextFormatting.GREEN));
        tooltip.add(TranslateUtils.translateTooltip("mechanical_calculator.points", currentPoints, maxPoints));
        return true;
    }


    @Override
    public float calculateStressApplied() {
        float rspd = MathHelper.abs(super.getSpeed());
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
    protected void fromTag(BlockState state, CompoundNBT tag, boolean client) {
        super.fromTag(state, tag, client);
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

    public ActionResultType onClick(PlayerEntity pe) {
        if (!pe.level.isClientSide) {
            currentPoints = (int) ResearchDataAPI.getData(pe).doResearch(currentPoints);
            updatePoints();
        }
        return ActionResultType.sidedSuccess(pe.level.isClientSide);
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
    public boolean shouldRenderNormally() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            float spd = MathHelper.abs(super.getSpeed());

            if (spd > 0 && spd <= 64 && currentPoints <= maxPoints - 20) {
                process += (int) spd;
                int curact = process / 1067;
                if (lastact != curact) {
                    lastact = curact;
                    level.playSound(null, worldPosition, FHSounds.MC_BELL.get(), SoundCategory.BLOCKS, 0.1f, 1f);
                }
                if (process >= processMax) {
                    process = 0;
                    lastact = 0;
                    if (doProduct)
                        currentPoints += 20;
                    requireNetworkUpdate();
                }


                if (ticsSlp <= 0) {
                    float pitch = MathHelper.clamp((spd / 32f) + 0.5f, 0.5f, 2f);
                    level.playSound(null, worldPosition, FHSounds.MC_ROLL.get(), SoundCategory.BLOCKS, 0.3f, pitch);
                    ticsSlp = MathHelper.ceil(20 / pitch);
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
    protected void write(CompoundNBT tag, boolean client) {
        super.write(tag, client);
        tag.putInt("process", process);
        tag.putInt("pts", currentPoints);
        tag.putInt("last_calc", lastact);
        if (!doProduct)
            tag.putBoolean("prod", doProduct);
    }
}
