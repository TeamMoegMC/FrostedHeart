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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.chorda.multiblock.MultiBlockAccess;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.chorda.util.IERecipeUtils;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.NonMirrorableWithActiveBlock;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public abstract class GeneratorLogic<T extends GeneratorLogic<T, ?>, R extends GeneratorState> extends HeatingLogic<T, R> implements OwnedLogic<R> {


    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private final List<IngredientWithSize> repair = Arrays.asList(new IngredientWithSize(Ingredient.of(ItemTags.create(new ResourceLocation("forge", "ingots/copper"))), 32), new IngredientWithSize(Ingredient.of(ItemTags.create(new ResourceLocation("forge", "stone"))), 8));

    private boolean hasFuel;//for rendering

    public GeneratorLogic() {
        super();
    }

    /**
     * Get the GeneratorData from context
     */
    public Optional<GeneratorData> getData(IMultiblockContext<R> ctx) {
        return ctx.getState().getData(CMultiblockHelper.getAbsoluteMaster(ctx));
    }

    /**
     * Upgrading Generator logic
     */
    public void onUpgradeMaintainClicked(IMultiblockContext<R> ctx, ServerPlayer player) {
        if (getData(ctx).map(t -> t.isBroken).orElse(false)) {
            repairStructure(ctx, player);
        } else {
            upgradeStructure(ctx, player);
        }
    }

    public void upgradeStructure(IMultiblockContext<R> ctx, ServerPlayer entityplayer) {
        if (!nextLevelHasValidStructure(ctx.getLevel().getRawLevel(), ctx))
            return;
        if (!ResearchListeners.hasMultiblock(ctx.getState().getOwner(), getNextLevelMultiblock()))
            return;
        if (!IERecipeUtils.costItems(entityplayer, getUpgradeCost(ctx.getLevel().getRawLevel(), ctx)))
            return;
        BlockPos negMasterOffset = CMultiblockHelper.getMasterPos(ctx).subtract(getNextLevelMultiblock().getMasterFromOriginOffset());
        Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, ctx.getLevel().getOrientation().front());
        ((MultiBlockAccess) getNextLevelMultiblock()).setPlayer(entityplayer);
        ((MultiBlockAccess) getNextLevelMultiblock()).callForm(ctx.getLevel().getRawLevel(), ctx.getLevel().toAbsolute(negMasterOffset), rot, Mirror.NONE, ctx.getLevel().getOrientation().front().getOpposite());
        for(ItemStack is:this.getPrice(ctx.getLevel().getRawLevel(), ctx))
        	CUtils.giveItem(entityplayer, is.copy());
    }

    public void repairStructure(IMultiblockContext<R> ctx, ServerPlayer entityplayer) {
        if (!getData(ctx).map(t -> t.isBroken).orElse(false))
            return;
        if (!IERecipeUtils.costItems(entityplayer, getRepairCost()))
            return;
        getData(ctx).ifPresent(t -> {
            t.isBroken = false;
            t.overdriveLevel = 0;
        });

    }

/*
    @Override
    public int[] getCurrentProcessesMax() {
        T master = master();
        if (master != this && master != null)
            return master.getCurrentProcessesMax();
        return new int[]{getData().map(t -> t.processMax).orElse(0)};
    }

    @Override
    public int[] getCurrentProcessesStep() {
        T master = master();
        if (master != this && master != null)
            return master.getCurrentProcessesStep();
        return new int[]{getData().map(t -> t.processMax - t.process).orElse(0)};
    }*/

    @Override
    public <C> LazyOptional<C> getCapability(IMultiblockContext<R> ctx, CapabilityPosition position, Capability<C> capability) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return getData(ctx).map(t -> t.invCap).orElse(LazyOptional.empty()).cast();
        }
        return super.getCapability(ctx, position, capability);
    }

    /*
        @Override
        public boolean isStackValid(int slot, ItemStack stack) {
            if (stack.isEmpty())
                return false;
            if (slot == INPUT_SLOT)
                return findRecipe(stack) != null;
            return false;
        }*/
    public GeneratorRecipe findRecipe(IMultiblockContext<R> ctx, ItemStack input) {
        for (GeneratorRecipe recipe : CUtils.filterRecipes(ctx.getLevel().getRawLevel().getRecipeManager(), GeneratorRecipe.TYPE))
            if (recipe.input.test(input))
                return recipe;
        return null;
    }
/*
    @Override
    public boolean triggerEvent(int id, int arg) {
        if (id == 0)
            this.formed = arg == 1;
        setChanged();
        this.markContainingBlockForUpdate(null);
        return true;
    }*/

    public final List<IngredientWithSize> getRepairCost() {
        return repair;
    }

    public List<IngredientWithSize> getUpgradeCost(Level level, IMultiblockContext<R> ctx) {
        IETemplateMultiblock ietm = getNextLevelMultiblock();
        if (ietm != null) {
        	R state=ctx.getState();
            if (state.upgrade == null) {
                List<StructureBlockInfo> structure = ietm.getStructure(level);
                NonNullList<ItemStack> materials = NonNullList.create();
                for (StructureBlockInfo info : structure) {
                    // Skip dummy blocks in total
                    if (info.state().hasProperty(IEProperties.MULTIBLOCKSLAVE) && info.state().getValue(IEProperties.MULTIBLOCKSLAVE))
                        continue;
                    ItemStack picked = Utils.getPickBlock(info.state());
                    boolean added = false;
                    for (ItemStack existing : materials)
                        if (ItemStack.isSameItem(existing, picked)) {
                            existing.grow(1);
                            added = true;
                            break;
                        }
                    if (!added)
                        materials.add(picked.copy());
                }
                if (materials.isEmpty()) return null;
                state.upgrade = materials.stream().filter(Ingredient.of(FHBlocks.GENERATOR_CORE_T1.get()).negate()).map(IngredientWithSize::of).collect(Collectors.toList());
            }
            return state.upgrade;
        }
        return null;
    }
    public List<ItemStack> getPrice(Level level, IMultiblockContext<R> ctx) {
        IETemplateMultiblock ietm = getNextLevelMultiblock();
        if (ietm != null) {
        	R state=ctx.getState();
            if (state.price == null) {
                List<StructureBlockInfo> structure = ietm.getStructure(level);
                NonNullList<ItemStack> materials = NonNullList.create();
                for (StructureBlockInfo info : structure) {
                    // Skip dummy blocks in total
                    if (info.state().hasProperty(IEProperties.MULTIBLOCKSLAVE) && info.state().getValue(IEProperties.MULTIBLOCKSLAVE))
                        continue;
                    ItemStack picked = Utils.getPickBlock(info.state());
                    boolean added = false;
                    for (ItemStack existing : materials)
                        if (ItemStack.isSameItem(existing, picked)) {
                            existing.grow(1);
                            added = true;
                            break;
                        }
                    if (!added)
                        materials.add(picked.copy());
                }
                if (materials.isEmpty()) return null;
                state.price = materials.stream().filter(Ingredient.of(FHBlocks.GENERATOR_CORE_T1.get()).negate()).collect(Collectors.toList());
            }
            return state.price;
        }
        return null;
    }
    public abstract IETemplateMultiblock getNextLevelMultiblock();

    public boolean nextLevelHasValidStructure(Level level, IMultiblockContext<R> ctx) {
        IETemplateMultiblock ietm = getNextLevelMultiblock();
        MultiblockRegistration<?> curmb=CMultiblockHelper.getMultiblock(ctx);
        if (ietm == null)
            return false;
        Vec3i csize = curmb.size(level);
        BlockPos masterOrigin = curmb.masterPosInMB();
        Vec3i nsize = ietm.getSize(level);
        BlockPos masterOffset = ietm.getMasterFromOriginOffset().subtract(masterOrigin);
        BlockPos negMasterOffset = masterOrigin.subtract(ietm.getMasterFromOriginOffset());
        AABB aabb = new AABB(masterOffset, masterOffset.offset(csize));

        for (int x = 0; x < nsize.getX(); x++) {
            for (int y = 0; y < nsize.getY(); y++) {
                for (int z = 0; z < nsize.getZ(); z++) {
                    if (aabb.contains(x, y, z))
                        continue;
                    BlockPos cpos = negMasterOffset.offset(x, y, z);
                    if (ctx.getLevel().getBlockState(cpos).getBlock() != Blocks.AIR) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void tryRegist(IMultiblockContext<R> ctx) {
        ctx.getState().tryRegist(ctx.getLevel().getRawLevel(), CMultiblockHelper.getAbsoluteMaster(ctx));
    }

    public void regist(IMultiblockContext<R> ctx) {
        ctx.getState().regist(ctx.getLevel().getRawLevel(), CMultiblockHelper.getAbsoluteMaster(ctx));
    }

    /**
     * Implements the core tick logic from GeneratorData
     * @param ctx
     * @return
     */
    @Override
    protected boolean tickFuel(IMultiblockContext<R> ctx) {
    	R state=ctx.getState();
        Optional<GeneratorData> data = getData(ctx);
        boolean lastIsBroken = data.map(t -> t.isBroken).orElse(false);
        boolean curHasFuel=data.map(t->t.hasFuel()).orElse(false);
        if(state.hasFuel!=curHasFuel) {
        	state.hasFuel=curHasFuel;
        	ctx.requestMasterBESync();
        }
        // Tick the GeneratorData
        state.tickData(ctx.getLevel().getRawLevel(), CMultiblockHelper.getAbsoluteMaster(ctx));
        boolean isActive = data.map(t -> t.isActive).orElse(false);

        // If newly broken, start exploding for 100 ticks
        boolean isBroken = data.map(t -> t.isBroken).orElse(false);
        if (lastIsBroken != isBroken && isBroken) {
            state.explodeTicks = 100;
        }

        Level level = ctx.getLevel().getRawLevel();
        if (state.explodeTicks > 0) {
            Vec3i size = CMultiblockHelper.getSize(ctx);
            // Every 5 ticks, send explosion packet to nearby players
            if (state.explodeTicks % 5 == 0) {
                BlockPos pos = ctx.getLevel().toAbsolute(new BlockPos(level.random.nextInt(size.getX()),
                        level.random.nextInt(size.getY()),
                        level.random.nextInt(size.getZ())));
                for (Player player : level.players()) {
                    if (player.blockPosition().distSqr(pos) < 4096.0D) {
                        ((ServerPlayer) player).connection.send(new ClientboundExplodePacket(pos.getX(), pos.getY(), pos.getZ(), 8, Arrays.asList(), null));
                    }
                }
            }
            // Reduce the explode ticks
            state.explodeTicks--;
        }

        // Tick the drives
        tickDrives(ctx, isActive);
        return isActive;
    }

    /**
     * For driving other machines.
     * @param ctx
     * @param active
     */
    protected void tickDrives(IMultiblockContext<R> ctx, boolean active) {

    }

    public boolean hasFuel() {
        return hasFuel;
    }

    @Override
    public void onOwnerChange(IMultiblockContext<R> ctx) {
        regist(ctx);
    }

    @Override
    public void tickEffects(IMultiblockContext<R> ctx, BlockPos pos, boolean isActive) {
        RandomSource rand = ctx.getLevel().getRawLevel().random;
        if (isActive && rand.nextInt(50) == 0) {
            ctx.getLevel().getRawLevel().playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D,
                    SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.5F + rand.nextFloat(),
                    rand.nextFloat() * 0.7F + 0.6F, false);
        }
    }

    @Override
    protected void tickShutdown(IMultiblockContext<R> ctx) {

    }

    @Override
    public void tickHeat(IMultiblockContext<R> ctx, boolean isActive) {

    }
    @Override
	public void onActiveStateChange(IMultiblockContext<R> ctx, boolean active) {
		NonMirrorableWithActiveBlock.setActive(ctx.getLevel(), getMultiblock(), active);
	}

    public abstract TemplateMultiblock getMultiblock() ;
}
