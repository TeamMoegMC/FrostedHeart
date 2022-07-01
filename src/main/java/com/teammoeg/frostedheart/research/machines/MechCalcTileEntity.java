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

import java.util.List;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.client.util.ClientUtils;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

public class MechCalcTileEntity extends KineticTileEntity implements IHaveGoggleInformation {
	int processMax=6400;
	public int process=0;
	int currentPoints=0;
	int maxPoints=40;
	public MechCalcTileEntity() {
		super(FHTileTypes.MECH_CALC.get());
	}

	NetworkHolder network = new NetworkHolder();
	Direction last;

	public ActionResultType onClick(PlayerEntity pe) {
		if(!pe.world.isRemote) {
			currentPoints=(int) ResearchDataAPI.getData((ServerPlayerEntity) pe).doResearch(currentPoints);
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
/*
	@Override
	public boolean isCustomConnection(KineticTileEntity other, BlockState state, BlockState otherState) {
		return true;
	}
*/
	@Override
	public boolean shouldRenderNormally() {
		return true;
	}
	@Override
	public void tick() {
		super.tick();
		if(!world.isRemote) {
			float spd=MathHelper.abs(super.getSpeed());
			if(spd<=64&&currentPoints<maxPoints) {
				process+=spd;
				if(process>=processMax) {
					process=0;
					currentPoints+=20;
					this.needsSpeedUpdate();
				}
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
		if(this.getSpeed()>64||this.getSpeed()<-64) {
			tooltip.add(GuiUtils.translateTooltip("mechanical_calculator.too_fast").mergeStyle(TextFormatting.RED));
			flag=false;
		}
		if(this.currentPoints>=maxPoints) {
			tooltip.add(GuiUtils.translateTooltip("mechanical_calculator.full").mergeStyle(TextFormatting.RED));
			flag=false;
		}
		if(flag)
			tooltip.add(GuiUtils.translateTooltip("mechanical_calculator.working").mergeStyle(TextFormatting.GREEN));
		tooltip.add(GuiUtils.translateTooltip("mechanical_calculator.points",currentPoints,maxPoints));
		return true;
	}

	@Override
	protected void fromTag(BlockState state, CompoundNBT tag, boolean client) {
		super.fromTag(state, tag, client);
		process=tag.getInt("process");
		currentPoints=tag.getInt("pts");
	}

	@Override
	protected void write(CompoundNBT tag, boolean client) {
		super.write(tag, client);
		tag.putInt("process",process);
		tag.putInt("pts",currentPoints);
	}

/*
	public float propagateRotationTo(KineticTileEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff,
			boolean connectedViaAxes, boolean connectedViaCogs) {
		if (connectedViaAxes)
			return 1f;
		return 0f;
	}*/
}
