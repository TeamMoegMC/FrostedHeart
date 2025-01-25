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

package com.teammoeg.frostedheart.content.steamenergy.charger;

import blusunrize.immersiveengineering.common.blocks.IEBaseBlockEntity;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.teammoeg.chorda.block.CBlockInterfaces;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.recipe.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import com.teammoeg.frostedheart.util.client.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static net.minecraft.ChatFormatting.GRAY;

public class ChargerTileEntity extends IEBaseBlockEntity implements CTickableBlockEntity, CBlockInterfaces.IActiveState, IHaveGoggleInformation, HeatNetworkProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    HeatEndpoint network = new HeatEndpoint(-10, 200, 0, 5);
    float power;
    LazyOptional<HeatEndpoint> heatcap = LazyOptional.of(() -> network);


    public ChargerTileEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.CHARGER.get(), pos, state);
    }

    private static void splitAndSpawnExperience(Level world, BlockPos pos, float experience) {
        int i = Mth.floor(experience);
        float f = Mth.frac(experience);
        if (f != 0.0F && Math.random() < f) {
            ++i;
        }

        while (i > 0) {
            int j = ExperienceOrb.getExperienceValue(i);
            i -= j;
            world.addFreshEntity(new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), j));
        }

    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction dir) {
        Direction bd = this.getBlockState().getValue(BlockStateProperties.FACING);
        if (cap == FHCapabilities.HEAT_EP.capability() && (dir == bd || (bd != Direction.DOWN && dir == Direction.DOWN) || (bd == Direction.UP && dir == Direction.NORTH) || (bd == Direction.DOWN && dir == Direction.SOUTH))) {
            return heatcap.cast();
        }
        return super.getCapability(cap, dir);
    }

    public void drawEffect() {
        if (level != null && level.isClientSide) {
            FHClientUtils.spawnSteamParticles(level, this.getBlockPos());
        }
    }

    public Direction getDirection() {
        return this.getBlockState().getValue(BlockStateProperties.FACING);
    }

    public float getMaxPower() {
        return 20000F;
    }

    public ChargerRecipe findRecipe(ItemStack is) {
        for (ChargerRecipe cr : CUtils.filterRecipes(this.getLevel().getRecipeManager(), ChargerRecipe.TYPE)) {
            if (cr.input.test(is)) {
                return cr;
            }
        }
        return null;
    }

    public InteractionResult onClick(Player pe, ItemStack is) {
        if (is != null) {
            Item it = is.getItem();
            if (it instanceof IChargable) {
                power -= ((IChargable) it).charge(is, power);
                drawEffect();
                return InteractionResult.SUCCESS;
            }
            ChargerRecipe cr = findRecipe(is);
            if (cr != null) {
                if (power >= cr.cost && is.getCount() >= cr.input.getCount()) {
                    if (!level.isClientSide) {
                        power -= cr.cost;
                        is.setCount(is.getCount() - cr.input.getCount());
                        ItemStack gain = cr.output.copy();
                        CUtils.giveItem(pe, gain);
                        setChanged();
                        this.markContainingBlockForUpdate(null);
                    }
                    drawEffect();
                    return InteractionResult.SUCCESS;
                }
            }

            if (power >= 100) {
                List<SmokingRecipe> irs = this.level.getRecipeManager().getAllRecipesFor(RecipeType.SMOKING);
                for (SmokingRecipe sr : irs) {
                    if (sr.getIngredients().iterator().next().test(is)) {
                        if (!level.isClientSide) {
                            power -= (float) sr.getCookingTime() / 20;
                            splitAndSpawnExperience(pe.getCommandSenderWorld(), pe.blockPosition(), sr.getExperience());
                            is.setCount(is.getCount() - 1);
                            ItemStack gain = sr.assemble(null, this.level.registryAccess()).copy();
                            CUtils.giveItem(pe, gain);
                            setChanged();
                            this.markContainingBlockForUpdate(null);
                        }
                        drawEffect();
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            {
                Collection<CampfireDefrostRecipe> irs = CampfireDefrostRecipe.recipeList.values();
                for (CampfireDefrostRecipe sr : irs) {
                    if (sr.getIngredient().test(is)) {
                        if (!level.isClientSide) {
                            power -= (float) sr.getCookingTime() / 80;
                            splitAndSpawnExperience(pe.getCommandSenderWorld(), pe.blockPosition(), sr.getExperience());
                            is.setCount(is.getCount() - 1);
                            ItemStack gain = sr.assemble(null, this.level.registryAccess()).copy();
                            CUtils.giveItem(pe, gain);
                            setChanged();
                            this.markContainingBlockForUpdate(null);
                        }
                        drawEffect();
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        power = nbt.getFloat("power");
        network.load(nbt, descPacket);
    }

    @Override
    public void tick() {
        if (!level.isClientSide) {
            float actual = network.drainHeat(Math.min(200, (getMaxPower() - power) / 0.8F));
            if (actual > 0) {
                power += (float) (actual * 8);
                this.setActive(true);
                setChanged();
                this.markContainingBlockForUpdate(null);
            } else
                this.setActive(false);
        } else if (getIsActive()) {
            FHClientUtils.spawnSteamParticles(this.getLevel(), worldPosition);
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.putFloat("power", power);
        network.save(nbt, descPacket);
    }
	@Override
	public void invalidateCaps() {
		heatcap.invalidate();
		super.invalidateCaps();
	}

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        float output = 0;
        float intake = 0;

        Lang.tooltip("heat_stats").forGoggles(tooltip);

        if (network.hasValidNetwork()) {
            output = network.getNetwork().getTotalEndpointOutput();
            intake = network.getNetwork().getTotalEndpointIntake();
            Lang.translate("tooltip", "pressure")
                    .style(GRAY)
                    .forGoggles(tooltip);
        } else {
            Lang.translate("tooltip", "pressure.no_network")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        }

        Lang.number(intake)
                .translate("generic", "unit.pressure")
                .style(ChatFormatting.AQUA)
                .space()
                .add(Lang.translate("tooltip", "pressure.intake")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        Lang.number(output)
                .translate("generic", "unit.pressure")
                .style(ChatFormatting.AQUA)
                .space()
                .add(Lang.translate("tooltip", "pressure.output")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        return true;

    }

    @Override
    public @Nullable HeatNetwork getNetwork() {
        return network.getNetwork();
    }
}
