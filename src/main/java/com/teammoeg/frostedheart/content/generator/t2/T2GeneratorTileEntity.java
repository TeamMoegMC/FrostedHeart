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

package com.teammoeg.frostedheart.content.generator.t2;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.content.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.generator.MasterGeneratorTileEntity;
import com.teammoeg.frostedheart.content.steamenergy.*;
import com.teammoeg.frostedheart.research.data.ResearchVariant;
import com.teammoeg.frostedheart.town.GeneratorData;
import com.teammoeg.frostedheart.util.ReferenceValue;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

public class T2GeneratorTileEntity extends MasterGeneratorTileEntity<T2GeneratorTileEntity>
        implements HeatController, INetworkConsumer {
    @Override
    public void disassemble() {
        if (sen != null)
            sen.invalidate();
        super.disassemble();
    }

    public T2GeneratorTileEntity.GeneratorUIData guiData = new T2GeneratorTileEntity.GeneratorUIData();
    HeatProviderManager manager = new HeatProviderManager(this, c -> {
        Direction dir = this.getFacing();

        c.accept(getBlockPosForPos(networkTile).offset(dir.getOpposite()), dir);

    });

    public T2GeneratorTileEntity() {
        super(FHMultiblocks.GENERATOR_T2, FHTileTypes.GENERATOR_T2.get(), false);
    }

    float power = 0;
    SteamEnergyNetwork sen = null;
    float spowerMod = 0;
    float slevelMod = 1;
    int liquidtick = 0;
    int noliquidtick = 0;
    int tickUntilStopBoom = 20;
    int notFullPowerTick = 0;
    final int nextBoom = 200; //10s

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        power = nbt.getFloat("steam_power");
        slevelMod = nbt.getFloat("steam_temp");
        spowerMod = nbt.getFloat("steam_product");
        liquidtick = nbt.getInt("liquid_tick");
        tank.readFromNBT(nbt.getCompound("fluid"));

    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.putFloat("steam_power", power);
        CompoundNBT tankx = new CompoundNBT();
        tank.writeToNBT(tankx);
        nbt.putFloat("steam_temp", slevelMod);
        nbt.putFloat("steam_product", spowerMod);
        nbt.putFloat("liquid_tick", liquidtick);
        nbt.put("fluid", tankx);
    }

    public FluidTank tank = new FluidTank(200 * FluidAttributes.BUCKET_VOLUME,
            f -> GeneratorSteamRecipe.findRecipe(f) != null);

    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        T2GeneratorTileEntity master = master();
        if (master != null && side == this.getFacing() && this.posInMultiblock.equals(fluidIn))
            return new FluidTank[]{master.tank};
        return new FluidTank[0];
    }

    private static final BlockPos fluidIn = new BlockPos(1, 0, 2);

    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
        if (side == this.getFacing() && this.posInMultiblock.equals(fluidIn))
            return true;
        return false;
    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }

    protected double getHeatEfficiency() {
        ReferenceValue<Double> eff = new ReferenceValue<>(1d);
        getTeamData().ifPresent(t -> {
            eff.map(n -> n + t.getVariantDouble(ResearchVariant.GENERATOR_HEAT));
        });
        return eff.getVal();
    }

    protected void tickLiquid() {
        Optional<GeneratorData> data = getData();
        this.power = data.map(t -> t.power).orElse(0f);
        this.liquidtick = data.map(t -> t.steamLevel).orElse(0);
        if (!this.getIsActive())
            return;
        float rt = this.getTemperatureLevel();
        if (rt == 0) {
            this.spowerMod = 0;
            this.slevelMod = 1;
        }
        if (noliquidtick > 0) {
            noliquidtick--;
            return;
        }
        double eff = getHeatEfficiency();

        int liquidtick = data.map(t -> t.steamLevel).orElse(0);
        if (liquidtick >= rt) {
            data.ifPresent(t -> t.steamLevel -= rt);
            this.fillHeat((float) (this.spowerMod * rt * eff));
            return;
        }
        GeneratorSteamRecipe sgr = GeneratorSteamRecipe.findRecipe(this.tank.getFluid());
        if (sgr != null) {
            int rdrain = (int) (20 * super.getTemperatureLevel() * sgr.level);
            int actualDrain = rdrain * sgr.input.getAmount();
            FluidStack fs = this.tank.drain(actualDrain, FluidAction.SIMULATE);
            if (fs.getAmount() >= actualDrain) {
                if (this.slevelMod != sgr.level)
                    this.markChanged(true);
                this.spowerMod = sgr.power;
                this.fillHeat((float) (this.spowerMod * rt * eff));
                this.slevelMod = sgr.level;
                data.ifPresent(t -> t.steamLevel = rdrain);
                final FluidStack fs2 = this.tank.drain(actualDrain, FluidAction.EXECUTE);
                data.ifPresent(t -> t.fluid = fs2.getFluid());
                return;
            }
        }
        noliquidtick = 40;
        this.markChanged(true);
        this.spowerMod = 0;
        this.slevelMod = 1;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2, pos.getX() + 2, pos.getY() + 6,
                pos.getZ() + 2);
    }

    @Override
    public void tickHeat() {
        super.tickHeat();
        this.setTemperatureLevel(super.getHeated() / 100F);
        this.setRangeLevel(1 * slevelMod);
    }

    @Override
    protected void tickFuel() {
        super.tickFuel();
        this.tickLiquid();
        manager.tick();
    }

    @Override
    protected void tickEffects(boolean isActive) {
        if (isActive) {
            BlockPos blockpos = this.getPos().offset(Direction.UP, 5);
            Random random = world.rand;
            float particleProbability = 0.3F;
            if (isOverdrive()) {
                particleProbability = 0.6F;
            }

            if (random.nextFloat() < particleProbability) {
                // for (int i = 0; i < random.nextInt(2)+1; ++i) {
                if (this.liquidtick != 0 && random.nextFloat() < 0.06F) {
                    ClientUtils.spawnSteamParticles(world, blockpos);
                }
                ClientUtils.spawnT2FireParticles(world, blockpos);
                ClientUtils.spawnSmokeParticles(world, blockpos);
                // }
            }

            if (this.isWorking() && this.getHeated() == getMaxHeated() && this.tickUntilStopBoom > 0) {
                ClientUtils.spawnSteamParticles(world, blockpos);
                this.tickUntilStopBoom--;
                this.notFullPowerTick = 0;
            }
            if (this.getHeated() < getMaxHeated()) {
                this.notFullPowerTick++;
                if (this.notFullPowerTick > this.nextBoom) {
                    this.notFullPowerTick = this.nextBoom;
                    this.tickUntilStopBoom = 20;
                }
            }
        }
    }

    private static final BlockPos networkTile = new BlockPos(1, 0, 0);

    @Override
    public SteamEnergyNetwork getNetwork() {
        BlockPos actualPos = getBlockPosForPos(networkTile);
        TileEntity te = Utils.getExistingTileEntity(world, actualPos);
        if (te instanceof T2GeneratorTileEntity) {
            if (((T2GeneratorTileEntity) te).sen != null) {
                sen = ((T2GeneratorTileEntity) te).sen;
            }
        }

        if (sen == null) {
            sen = new SteamEnergyNetwork(this);
            if (te instanceof T2GeneratorTileEntity) {
                ((T2GeneratorTileEntity) te).sen = sen;
            }

        }
        return sen;
    }

    @Override
    public float getMaxHeat() {
        return super.getData().map(t -> t.power).orElse(0f);
    }

    public float getMaxPower() {
        return 20000;
    }

    @Override
    public float drainHeat(float value) {
        if (master() != null) {
            Optional<GeneratorData> data = super.getData();
            float pow = data.map(t -> t.power).orElse(0f);
            final float actual = Math.min(value, pow);
            data.ifPresent(t -> t.power -= actual);
            return actual;
        }
        return 0;
    }

    @Override
    public boolean connect(Direction to, int dist) {
        return false;
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return to == this.getFacing().getOpposite() && this.posInMultiblock.equals(networkTile);
    }

    @Override
    public SteamNetworkHolder getHolder() {
        return null;
    }

    private static final BlockPos redstone = new BlockPos(1, 1, 2);

    @Override
    protected void tickControls() {
        super.tickControls();


        int power = this.world.getStrongPower(getBlockPosForPos(redstone));
        if (power > 0) {
            if (power > 10) {
                if (!this.isOverdrive()) this.setOverdrive(true);
                if (!this.isWorking()) this.setWorking(true);
            } else if (power > 5) {
                if (this.isOverdrive()) this.setOverdrive(false);
                if (!this.isWorking()) this.setWorking(true);
            } else {
                if (this.isWorking()) this.setWorking(false);
            }
        }
    }

    @Override
    public float fillHeat(float value) {
        Optional<GeneratorData> data = super.getData();
        final float maxfill = this.getMaxPower() - data.map(t -> t.power).orElse(this.getMaxPower());
        if (maxfill > value) {

            data.ifPresent(t -> t.power += value);
            return 0;
        }
        data.ifPresent(t -> t.power += maxfill);
        return value - maxfill;
    }

    @Override
    public int getUpperBound() {
        int distanceToTowerTop = 5;
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
    protected void callBlockConsumerWithTypeCheck(Consumer<T2GeneratorTileEntity> consumer, TileEntity te) {
        if (te instanceof T2GeneratorTileEntity)
            consumer.accept((T2GeneratorTileEntity) te);
    }

    @Override
    public TileEntity getEntity() {
        return this;
    }
}
