/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.research.machines;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.teammoeg.frostedheart.FHSounds;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.content.steamenergy.NetworkHolder;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
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

import java.util.List;

public class MechCalcTileEntity extends KineticTileEntity implements IHaveGoggleInformation {
	int processMax=6400;
	public int process=0;
	int currentPoints=0;
	int lastact;
	int maxPoints=80;
	boolean doProduct=true;
	public MechCalcTileEntity() {
		super(FHTileTypes.MECH_CALC.get());
	}

	NetworkHolder network = new NetworkHolder();
	Direction last;

	public ActionResultType onClick(PlayerEntity pe) {
		if(!pe.world.isRemote) {
			currentPoints=(int) ResearchDataAPI.getData((ServerPlayerEntity) pe).doResearch(currentPoints);
			this.notifyUpdate();
		}
		return ActionResultType.func_233537_a_(pe.world.isRemote);
	}

	public void drawEffect() {
		world.getBlockState(pos);
	}

	public Direction getDirection() {
		return this.getBlockState().get(BlockStateProperties.HORIZONTAL_FACING);
	}
	public Axis getAxis() {
		return this.getDirection().rotateY().getAxis();
	}
	@Override
	public boolean shouldRenderNormally() {
		return true;
	}
	@Override
	public void tick() {
		super.tick();
		if(!world.isRemote) {
			float spd=MathHelper.abs(super.getSpeed());
			if(spd>0&&spd<=64&&currentPoints<maxPoints) {
				process+=spd;
				int curact=process/1067;
				if(lastact!=curact) {
					lastact=curact;
					world.playSound(null, pos,FHSounds.MC_BELL.get(),SoundCategory.BLOCKS,0.3f,1f);
				}
				if(process>=processMax) {
					process=0;
					lastact=0;
					if(doProduct)
						currentPoints+=20;
					this.needsSpeedUpdate();
				}
				
				
				if (ticsSlp<=0) {
					float pitch = MathHelper.clamp((spd / 32f)+0.5f, 0.5f, 2f);
					world.playSound(null,pos,FHSounds.MC_ROLL.get(),SoundCategory.BLOCKS,0.3f, pitch);
					ticsSlp=MathHelper.ceil(20/pitch);
				}else ticsSlp--;
				this.notifyUpdate();
			}
		}
	}
    @Override
    public float calculateStressApplied() {
    	if(currentPoints<maxPoints&&MathHelper.abs(super.getSpeed())<=64) {
			this.lastStressApplied = 64;
			return 64;
    	}
		this.lastStressApplied = 0;
		return 0;
    }

	@Override
	public boolean addToGoggleTooltip(List<ITextComponent> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		boolean flag=true;
		float spd=MathHelper.abs(super.getSpeed());
		if(spd>64) {
			tooltip.add(GuiUtils.translateTooltip("mechanical_calculator.too_fast").mergeStyle(TextFormatting.RED));
			flag=false;
		}
		if(this.currentPoints>=maxPoints) {
			tooltip.add(GuiUtils.translateTooltip("mechanical_calculator.full").mergeStyle(TextFormatting.RED));
			flag=false;
		}
		if(flag && spd>0)
			tooltip.add(GuiUtils.translateTooltip("mechanical_calculator.working").mergeStyle(TextFormatting.GREEN));
		tooltip.add(GuiUtils.translateTooltip("mechanical_calculator.points",currentPoints,maxPoints));
		return true;
	}

	@Override
	protected void fromTag(BlockState state, CompoundNBT tag, boolean client) {
		super.fromTag(state, tag, client);
		process=tag.getInt("process");
		currentPoints=tag.getInt("pts");
		lastact=tag.getInt("last_calc");
		if(tag.contains("prod"))
			doProduct=tag.getBoolean("prod");
	}

	@Override
	protected void write(CompoundNBT tag, boolean client) {
		super.write(tag, client);
		tag.putInt("process",process);
		tag.putInt("pts",currentPoints);
		tag.putInt("last_calc",lastact);
		if(!doProduct)
			tag.putBoolean("prod",doProduct);
	}
	@OnlyIn(Dist.CLIENT)
	int ticsSlp;//ticks since last sound play
	@OnlyIn(Dist.CLIENT)
	public void tickAudio() {

		
	}
}
