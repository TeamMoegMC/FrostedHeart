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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.multiblock.components.OwnerState;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.util.FHMultiblockHelper;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.mixin.IOwnerChangeListener;
import com.teammoeg.frostedheart.util.mixin.MultiBlockAccess;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IProcessBE;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.items.HammerItem;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandler;

public abstract class MasterGeneratorTileEntity<T extends MasterGeneratorTileEntity<T,?>,R extends MasterGeneratorState> extends ZoneHeatingMultiblockLogic<T,R> implements IIEInventory, OwnedMultiblockLogic<R> {
	public static final int PROCESS=0;
	public static final int PROCESS_MAX=1;
	public static final int OVERDRIVE=2;
	public static final int POWER=3;
	public static final int TLEVEL=4;
	public static final int RLEVEL=5;
	public static final int ISBROKEN=6;
    

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private boolean hasFuel;//for rendering
    
    //local inventory, prevent lost
    List<IngredientWithSize> upgrade;
    
    public MasterGeneratorTileEntity() {
        super();
    }
    public Optional<GeneratorData> getData(IMultiblockContext<R> ctx){
    	return ctx.getState().getData(FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel()));
    }
    public void onUpgradeMaintainClicked(IMultiblockContext<R> ctx,ServerPlayer player) {
    	if(getData(ctx).map(t->t.isBroken).orElse(false)) {
    		repairStructure(ctx,player);
    	} else {
    		upgradeStructure(ctx,player);
    	}
    	HammerItem item;;
    };
    public abstract IETemplateMultiblock getNextLevel();
    public void upgradeStructure(IMultiblockContext<R> ctx,ServerPlayer entityplayer) {
    	if(!isValidStructure(ctx))
    		return;
    	if(!ResearchListeners.hasMultiblock(ctx.getState().getOwner(), getNextLevel()))
    		return;
    	if(!FHUtils.costItems(entityplayer, getUpgradeCost(ctx)))
    		return;
    	BlockPos negMasterOffset=FHMultiblockHelper.getMasterPos(ctx.getLevel()).subtract(getNextLevelMultiblock().getMasterFromOriginOffset());
        Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, ctx.getLevel().getOrientation().front());
        ((MultiBlockAccess) getNextLevelMultiblock()).setPlayer(entityplayer);
        ((MultiBlockAccess) getNextLevelMultiblock()).callForm(ctx.getLevel().getRawLevel(), ctx.getLevel().toAbsolute(negMasterOffset), rot, Mirror.NONE, ctx.getLevel().getOrientation().front());

    }
    public void repairStructure(IMultiblockContext<R> ctx,ServerPlayer entityplayer) {
    	if(!getData(ctx).map(t->t.isBroken).orElse(false))
    		return;
    	if(!FHUtils.costItems(entityplayer, getRepairCost()))
    		return;
    	getData(ctx).ifPresent(t->{t.isBroken=false;t.overdriveLevel=0;});

    }

	@Override
    public void doGraphicalUpdates() {

    }

    @Override
	public <C> LazyOptional<C> getCapability(IMultiblockContext<R> ctx, CapabilityPosition position, Capability<C> capability) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return getData(ctx).map(t->t.invCap).orElse(LazyOptional.empty()).cast();
        }
        return super.getCapability(ctx,position,capability);
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



/*
    @Override
    public boolean isStackValid(int slot, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (slot == INPUT_SLOT)
            return findRecipe(stack) != null;
        return false;
    }*/
    public GeneratorRecipe findRecipe(IMultiblockContext<R> ctx,ItemStack input) {
        for (GeneratorRecipe recipe : FHUtils.filterRecipes(ctx.getLevel().getRawLevel().getRecipeManager(), GeneratorRecipe.TYPE))
            if (recipe.input.test(input))
                return recipe;
        return null;
    }
    @Override
    public void shutdownTick(IMultiblockContext<R> ctx) {
        boolean invState = !this.getInventory().get(INPUT_SLOT).isEmpty()||ctx.getState().guiData.get(PROCESS)>0;
        if (invState != hasFuel) {
            hasFuel = invState;
            ctx.requestMasterBESync();
        }

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

    private final List<IngredientWithSize> repair=Arrays.asList(new IngredientWithSize(Ingredient.of(ItemTags.create(new ResourceLocation("forge","ingots/copper"))),32),new IngredientWithSize(Ingredient.of(ItemTags.create(new ResourceLocation("forge","stone"))),8));
    public final List<IngredientWithSize> getRepairCost(){
    	return repair;
    };
    public List<IngredientWithSize> getUpgradeCost(IMultiblockContext<R> ctx){
    	IETemplateMultiblock ietm=getNextLevelMultiblock();
        if(ietm!=null) {
        	if(upgrade==null) {
    			List<StructureBlockInfo> structure = FHMultiblockHelper.getBEHelper(ctx.getLevel()).map(t->t.getMultiblock().getStructure().apply(ctx.getLevel().getRawLevel())).orElse(ImmutableList.of());
    			NonNullList<ItemStack> materials = NonNullList.create();
    			for(StructureBlockInfo info : structure)
    			{
    				// Skip dummy blocks in total
    				if(info.state().hasProperty(IEProperties.MULTIBLOCKSLAVE)&&info.state().getValue(IEProperties.MULTIBLOCKSLAVE))
    					continue;
    				ItemStack picked = Utils.getPickBlock(info.state());
    				boolean added = false;
    				for(ItemStack existing : materials)
    					if(ItemStack.isSameItem(existing, picked))
    					{
    						existing.grow(1);
    						added = true;
    						break;
    					}
    				if(!added)
    					materials.add(picked.copy());
    			}
    			if(materials.isEmpty())return null;
        		upgrade=materials.stream().filter(Ingredient.of(FHBlocks.generator_core_t1.get()).negate()).map(IngredientWithSize::of).collect(Collectors.toList());
        	}
        	return upgrade;
        }
    	return null;
    };
    public abstract IETemplateMultiblock getNextLevelMultiblock();
    public boolean isValidStructure(IMultiblockContext<R> ctx) { 
    	IETemplateMultiblock ietm=getNextLevelMultiblock();
    	if(ietm==null)
    		return false;
    	Vec3i csize=FHMultiblockHelper.getSize(ctx.getLevel());
    	BlockPos masterOrigin=FHMultiblockHelper.getMasterPos(ctx.getLevel());
    	Vec3i nsize=ietm.getSize(ctx.getLevel().getRawLevel());
    	BlockPos masterOffset=ietm.getMasterFromOriginOffset().subtract(masterOrigin);
    	BlockPos negMasterOffset=masterOrigin.subtract(ietm.getMasterFromOriginOffset());
    	AABB aabb=new AABB(masterOffset,masterOffset.offset(csize));
    	
    	for(int x=0;x<nsize.getX();x++) {
    		for(int y=0;y<nsize.getY();y++) {
    			for(int z=0;z<nsize.getZ();z++) {
    				if(aabb.contains(x,y,z))
    					continue;
    				BlockPos cpos=negMasterOffset.offset(x, y, z);
    				if(ctx.getLevel().getBlockState(cpos).getBlock()!=Blocks.AIR) {
    					return false;
    				}
    	    	}
        	}
    	}
    	return true;
    }
    public void tryRegist(IMultiblockContext<R> ctx) {
    	ctx.getState().tryRegist(ctx.getLevel().getRawLevel(),FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel()));
    }
    public void regist(IMultiblockContext<R> ctx) {
    	ctx.getState().regist(ctx.getLevel().getRawLevel(),FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel()));
    }
    @Override
    protected boolean tickFuel(IMultiblockContext<R> ctx) {
        // just finished process or during process
    	
        Optional<GeneratorData> data = getData(ctx);
        boolean lastIsBroken=data.map(t->t.isBroken).orElse(false);
        data.ifPresent(t -> t.tick(ctx.getLevel().getRawLevel()));
        boolean isWorking=data.map(t -> t.isActive).orElse(false);
       
        ctx.getState().guiData.set(PROCESS, data.map(t -> t.process).orElse(0));
        ctx.getState().guiData.set(PROCESS_MAX, data.map(t -> t.processMax).orElse(0));
        ctx.getState().guiData.set(OVERDRIVE, data.map(t -> t.overdriveLevel*1000/t.getMaxOverdrive()).orElse(0));
        ctx.getState().guiData.set(POWER, (int)(float)data.map(t->t.power).orElse(0F));
        ctx.getState().guiData.set(TLEVEL, (int) (ctx.getState().getTemperatureLevel()*100));
        ctx.getState().guiData.set(RLEVEL, (int) (ctx.getState().getRangeLevel()*100));
        ctx.getState().guiData.set(ISBROKEN, data.map(t->t.isBroken).orElse(false)?1:0);
        boolean isBroken=data.map(t->t.isBroken).orElse(false);
        if(lastIsBroken!=isBroken&&isBroken) {
        	ctx.getState().remTicks=100;
        }
        Level level=ctx.getLevel().getRawLevel();
        if(ctx.getState().remTicks>0) {
        	Vec3i size=FHMultiblockHelper.getSize(ctx.getLevel());
        	if(ctx.getState().remTicks%5==0) {
        		BlockPos pos=ctx.getLevel().toAbsolute(new BlockPos(level.random.nextInt(size.getX()),
					level.random.nextInt(size.getY()),
					level.random.nextInt(size.getZ())));
	            for(Player serverplayerentity : level.players()) {
	                if (serverplayerentity.blockPosition().distSqr(pos) < 4096.0D) {
	                   ((ServerPlayer)serverplayerentity).connection.send(new ClientboundExplodePacket(pos.getX(), pos.getY(), pos.getZ(), 8, Arrays.asList(), null));
	                }
	             }
        	}
        	ctx.getState().remTicks--;
        }
        tickDrives(ctx,isWorking);
        return isWorking;
    	/*if(this.getIsActive())
    		this.markContainingBlockForUpdate(null);*/
    }
    protected void tickDrives(IMultiblockContext<R> ctx,boolean active) {
    	
    }
    public void tickHeat(boolean isWorking) {
    }


	public boolean hasFuel() {
		return hasFuel;
	}
	@Override
	public void onOwnerChange(IMultiblockContext<R> ctx) {
		regist(ctx);
	}
}
