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

package com.teammoeg.frostedheart.content.generator.t1;

import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.content.generator.MasterGeneratorTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Random;
import java.util.function.Consumer;

public final class T1GeneratorTileEntity extends MasterGeneratorTileEntity<T1GeneratorTileEntity> {
    public T1GeneratorTileEntity.GeneratorUIData guiData = new T1GeneratorTileEntity.GeneratorUIData();
    public boolean hasFuel;

    public T1GeneratorTileEntity() {
        super(FHMultiblocks.GENERATOR, FHTileTypes.GENERATOR_T1.get(), false);
    }


    @Override
    protected void tickFuel() {
        this.hasFuel = !this.getInventory().get(INPUT_SLOT).isEmpty();
        super.tickFuel();
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        hasFuel = nbt.getBoolean("hasFuel");
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.putBoolean("hasFuel", hasFuel);
    }

    @Override
    protected void tickEffects(boolean isActive) {
        if (isActive) {
            BlockPos blockpos = this.getPos();
            Random random = world.rand;
            if (random.nextFloat() < 0.2F) {
                //for (int i = 0; i < random.nextInt(2) + 2; ++i) {
                ClientUtils.spawnSmokeParticles(world, blockpos.offset(Direction.UP, 1));
                ClientUtils.spawnSmokeParticles(world, blockpos);
                ClientUtils.spawnFireParticles(world, blockpos);
                //}
            }
        }
    }

    @Override
    public void shutdownTick() {
        boolean invState = !this.getInventory().get(INPUT_SLOT).isEmpty();
        if (invState != hasFuel) {
            hasFuel = invState;
            this.markContainingBlockForUpdate(null);
        }

    }

    @Override
    public void tickHeat() {
        super.tickHeat();
        this.setTemperatureLevel(getHeated() / 100F);
        this.setRangeLevel(1);
    }

    @Override
    public int getUpperBound() {
        int distanceToTowerTop = 2;
        int extra = MathHelper.ceil(getRangeLevel() * 2);
        return distanceToTowerTop + extra;
    }

    @Override
    public int getLowerBound() {
        int distanceToGround = 2;
        int extra = MathHelper.ceil(getRangeLevel());
        return distanceToGround + extra;
    }


    @Override
    protected void callBlockConsumerWithTypeCheck(Consumer<T1GeneratorTileEntity> consumer, TileEntity te) {
        if (te instanceof T1GeneratorTileEntity)
            consumer.accept((T1GeneratorTileEntity) te);
    }
}
