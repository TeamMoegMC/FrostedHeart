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

import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.mixin.IOwnerChangeListener;
import com.teammoeg.frostedheart.util.mixin.MultiBlockAccess;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IProcessBE;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
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

public abstract class MasterGeneratorTileEntity<T extends MasterGeneratorTileEntity<T,?>,R extends MasterGeneratorState> extends ZoneHeatingMultiblockLogic<T,R> implements IIEInventory {
	public static final int PROCESS=0;
	public static final int PROCESS_MAX=1;
	public static final int OVERDRIVE=2;
	public static final int POWER=3;
	public static final int TLEVEL=4;
	public static final int RLEVEL=5;
    public ContainerData guiData = new ContainerData() {

    	ContainerData base=new SimpleContainerData(4);
		@Override
		public int get(int index) {
			if(index<base.getCount())
				return base.get(index);
			index-=base.getCount();
			switch(index) {
			case 0:return (int) (getTemperatureLevel()*100);
			case 1:return (int) (getRangeLevel()*100);
			case 2:return isBroken?1:0;
			}
			return 0;
		}

		@Override
		public void set(int index, int value) {
			
			if(index<base.getCount()) {
				base.set(index,value);
				return;
			}
			index-=base.getCount();
			switch(index) {
			case 0:setTemperatureLevel(value/100f);break;
			case 1:setRangeLevel(value/100f);break;
			case 2:isBroken=value!=0;
			}
		}

		@Override
		public int getCount() {
			return base.getCount()+3;
		}
    	
    };

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private boolean hasFuel;//for rendering
    
    //local inventory, prevent lost
    List<IngredientWithSize> upgrade;
    
    public MasterGeneratorTileEntity(IETemplateMultiblock multiblockInstance) {
        super(multiblockInstance);
    }

    @Override
    public void doGraphicalUpdates() {

    }

    @Override
	public <C> LazyOptional<C> getCapability(IMultiblockContext<R> ctx, CapabilityPosition position, Capability<C> capability) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return ctx.getState().getData().map(t->t.invCap).orElse(LazyOptional.empty()).cast();
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
        boolean invState = !this.getInventory().get(INPUT_SLOT).isEmpty()||this.guiData.get(PROCESS)>0;
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
    			List<StructureBlockInfo> structure = multiblock.getStructure(ctx.getLevel().getRawLevel());
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
    	Vec3i csize=this.multiblock.getSize(ctx.getLevel().getRawLevel());
    	Vec3i nsize=ietm.getSize(ctx.getLevel().getRawLevel());
    	BlockPos masterOffset=ietm.getMasterFromOriginOffset().subtract(this.multiblock.getMasterFromOriginOffset());
    	BlockPos negMasterOffset=this.multiblock.getMasterFromOriginOffset().subtract(ietm.getMasterFromOriginOffset());
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
    public void upgradeStructure(IMultiblockContext<R> ctx,ServerPlayer entityplayer) {
    	if(!isValidStructure(ctx))
    		return;
    	if(!ResearchListeners.hasMultiblock(ctx.getState().getOwner(), getNextLevelMultiblock()))
    		return;
    	if(!FHUtils.costItems(entityplayer, getUpgradeCost(ctx)))
    		return;
    	BlockPos negMasterOffset=this.multiblock.getMasterFromOriginOffset().subtract(getNextLevelMultiblock().getMasterFromOriginOffset());
        Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, ctx.getLevel().getOrientation().front());
        ((MultiBlockAccess) getNextLevelMultiblock()).setPlayer(entityplayer);
        ((MultiBlockAccess) getNextLevelMultiblock()).callForm(ctx.getLevel().getRawLevel(), ctx.getLevel().toAbsolute(negMasterOffset), rot, Mirror.NONE, ctx.getLevel().getOrientation().front());

    }
    public void repairStructure(IMultiblockContext<R> ctx,ServerPlayer entityplayer) {
    	if(!ctx.getState().isBroken)
    		return;
    	if(!FHUtils.costItems(entityplayer, getRepairCost()))
    		return;
    	ctx.getState().isBroken=false;
    	ctx.getState().getData().ifPresent(t->{t.isBroken=false;t.overdriveLevel=0;});

    }
    @Override
    public void receiveMessageFromClient(CompoundTag message) {
        super.receiveMessageFromClient(message);
        if (message.contains("isWorking", Tag.TAG_BYTE))
            setWorking(message.getBoolean("isWorking"));
        if (message.contains("isOverdrive", Tag.TAG_BYTE))
            setOverdrive(message.getBoolean("isOverdrive"));
        this.markContainingBlockForUpdate(null);
        this.setChanged();
       /* if (message.contains("temperatureLevel", Tag.TAG_INT))
            setTemperatureLevel(message.getInt("temperatureLevel"));
        if (message.contains("rangeLevel", Tag.TAG_INT))
            setRangeLevel(message.getInt("rangeLevel"));*/
    }




    int remTicks;
    @Override
    protected boolean tickFuel(IMultiblockContext<R> ctx) {
        // just finished process or during process
    	ctx.getState().tryRegist(ctx.getLevel().getRawLevel());
        Optional<GeneratorData> data = ctx.getState().getData();
        data.ifPresent(t -> t.tick(ctx.getLevel().getRawLevel()));
        boolean isWorking=data.map(t -> t.isActive).orElse(false);
        setTemperatureLevel(data.map(t -> t.TLevel).orElse(0F));
        setRangeLevel(data.map(t -> t.RLevel).orElse(0F));
        boolean lastIsBroken=isBroken;
        guiData.set(PROCESS, data.map(t -> t.process).orElse(0));
        guiData.set(PROCESS_MAX, data.map(t -> t.processMax).orElse(0));
        guiData.set(OVERDRIVE, data.map(t -> t.overdriveLevel*1000/t.getMaxOverdrive()).orElse(0));
        guiData.set(POWER, (int)(float)data.map(t->t.power).orElse(0F));
        isBroken = data.map(t->t.isBroken).orElse(false);
        if(lastIsBroken!=isBroken&&isBroken) {
        	remTicks=100;
        }
        if(remTicks>0) {
        	if(remTicks%5==0) {
	        	BlockPos pos=this.getBlockPosForPos(
	        			new BlockPos(level.random.nextInt(multiblockInstance.getSize(level).getX()),
	        						level.random.nextInt(multiblockInstance.getSize(level).getY()),
	        						level.random.nextInt(multiblockInstance.getSize(level).getZ())));
	            for(Player serverplayerentity : this.level.players()) {
	                if (serverplayerentity.blockPosition().distSqr(pos) < 4096.0D) {
	                   ((ServerPlayer)serverplayerentity).connection.send(new ClientboundExplodePacket(pos.getX(), pos.getY(), pos.getZ(), 8, Arrays.asList(), null));
	                }
	             }
        	}
        	remTicks--;
        }
        tickDrives(isWorking);
        return isWorking;
    	/*if(this.getIsActive())
    		this.markContainingBlockForUpdate(null);*/
    }

    public void tickHeat(boolean isWorking) {
    }


	public boolean hasFuel() {
		return hasFuel;
	}
}
