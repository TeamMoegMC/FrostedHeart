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

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IProcessBE;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
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
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandler;

public abstract class MasterGeneratorTileEntity<T extends MasterGeneratorTileEntity<T>> extends ZoneHeatingMultiblockTileEntity<T> implements IIEInventory,
        FHBlockInterfaces.IActiveState, IEBlockInterfaces.IInteractionObjectIE, IProcessBE, IEBlockInterfaces.IBlockBounds,IOwnerChangeListener {
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
    boolean isBroken;
    //local inventory, prevent lost
    NonNullList<ItemStack> linventory = NonNullList.withSize(2, ItemStack.EMPTY);

    LazyOptional<IItemHandler> invHandler = registerConstantCap(
            new IEInventoryHandler(2, this, 0, new boolean[]{true, false},
                    new boolean[]{false, true})
    );
    List<IngredientWithSize> upgrade;
    
    public MasterGeneratorTileEntity(IETemplateMultiblock multiblockInstance, BlockEntityType<T> type, boolean hasRSControl) {
        super(multiblockInstance, type, hasRSControl);
        

    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }

    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
        return false;
    }

    @Override
    public boolean canUseGui(Player player) {
        return formed;
    }

    @Override
    public void disassemble() {
        if (master() != null)
            master().unregist();
        super.disassemble();
    }

    @Override
    public void doGraphicalUpdates() {

    }

    @Nonnull
    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        return new IFluidTank[0];
    }

    @Nonnull
    @Override
    public VoxelShape getBlockBounds(@Nullable CollisionContext ctx) {
        return Shapes.block();
    }

    @Nonnull
    @Override
    public <X> LazyOptional<X> getCapability(@Nonnull Capability<X> capability, Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            T master = master();
            if (master != null)
                return master.invHandler.cast();
        }
        return super.getCapability(capability, facing);
    }

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
    }
    public final Optional<GeneratorData> getDataNoCheck() {
        return getTeamData().map(t -> t.getData(SpecialDataTypes.GENERATOR_DATA));
    }
    public final Optional<GeneratorData> getData() {
        return getTeamData().map(t -> t.getData(SpecialDataTypes.GENERATOR_DATA)).filter(t -> master().worldPosition.equals(t.actualPos));
    }

    @Nullable
    @Override
    public IEBlockInterfaces.IInteractionObjectIE getGuiMaster() {
        return master();
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        T master = master();
        return Optional.ofNullable(master).flatMap(MasterGeneratorTileEntity::getData).map(GeneratorData::getInventory).orElseGet(() -> master != null ? master.linventory : this.linventory);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    public boolean isDataPresent() {
        T master = master();
        return Optional.ofNullable(master).flatMap(MasterGeneratorTileEntity::getData).isPresent();
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (slot == INPUT_SLOT)
            return findRecipe(stack) != null;
        return false;
    }
    public GeneratorRecipe findRecipe(ItemStack input) {
        for (GeneratorRecipe recipe : FHUtils.filterRecipes(this.getLevel().getRecipeManager(), GeneratorRecipe.TYPE))
            if (recipe.input.test(input))
                return recipe;
        return null;
    }
    @Override
    public void onShutDown() {
    }
    @Override
    public void shutdownTick() {
        boolean invState = !this.getInventory().get(INPUT_SLOT).isEmpty()||this.guiData.get(PROCESS)>0;
        if (invState != hasFuel) {
            hasFuel = invState;
            this.markContainingBlockForUpdate(null);
        }

    }
    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        ContainerHelper.loadAllItems(nbt, linventory);
        hasFuel = nbt.getBoolean("hasFuel");
        isBroken = nbt.getBoolean("isBroken");
        if(!descPacket&&this.getLevel() instanceof ServerLevel) {
            Optional<GeneratorData> data = this.getData();
            data.ifPresent(t -> {
                this.isOverdrive=t.isOverdrive;
                this.isWorking=t.isWorking;
            });
        }
    }

    @Override
    public boolean triggerEvent(int id, int arg) {
        if (id == 0)
            this.formed = arg == 1;
        setChanged();
        this.markContainingBlockForUpdate(null);
        return true;
    }
    public void onUpgradeMaintainClicked(ServerPlayer player) {
    	if(isBroken) {
    		repairStructure(player);
    	} else {
    		upgradeStructure(player);
    	}
    };
    private final List<IngredientWithSize> repair=Arrays.asList(new IngredientWithSize(Ingredient.of(ItemTags.createOptional(new ResourceLocation("forge","ingots/copper"))),32),new IngredientWithSize(Ingredient.of(ItemTags.createOptional(new ResourceLocation("forge","stone"))),8));
    public final List<IngredientWithSize> getRepairCost(){
    	return repair;
    };
    public List<IngredientWithSize> getUpgradeCost(){
    	IETemplateMultiblock ietm=getNextLevelMultiblock();
        if(ietm!=null) {
        	if(upgrade==null) {
        		upgrade=Arrays.stream(ietm.getTotalMaterials()).filter(Ingredient.of(FHBlocks.generator_core_t1.get()).negate()).map(IngredientWithSize::of).collect(Collectors.toList());
        	}
        	return upgrade;
        }
    	return null;
    };
    public abstract IETemplateMultiblock getNextLevelMultiblock();
    public boolean isValidStructure() { 
    	IETemplateMultiblock ietm=getNextLevelMultiblock();
    	if(ietm==null)
    		return false;
    	Vec3i csize=this.multiblockInstance.getSize(level);
    	Vec3i nsize=ietm.getSize(level);
    	BlockPos masterOffset=ietm.getMasterFromOriginOffset().subtract(this.multiblockInstance.getMasterFromOriginOffset());
    	BlockPos negMasterOffset=this.multiblockInstance.getMasterFromOriginOffset().subtract(ietm.getMasterFromOriginOffset());
    	AABB aabb=new AABB(masterOffset,masterOffset.offset(csize));
    	
    	for(int x=0;x<nsize.getX();x++) {
    		for(int y=0;y<nsize.getY();y++) {
    			for(int z=0;z<nsize.getZ();z++) {
    				if(aabb.contains(x,y,z))
    					continue;
    				BlockPos cpos=negMasterOffset.offset(x, y, z);
    				BlockPos actual=this.getBlockPosForPos(cpos);
    				if(level.getBlockState(actual).getBlock()!=Blocks.AIR) {
    					return false;
    				}
    	    	}
        	}
    	}
    	return true;
    }
    public void upgradeStructure(ServerPlayer entityplayer) {
    	if(!isValidStructure())
    		return;
    	if(!ResearchListeners.hasMultiblock(getOwner(), getNextLevelMultiblock()))
    		return;
    	if(!FHUtils.costItems(entityplayer, getUpgradeCost()))
    		return;
    	BlockPos negMasterOffset=this.multiblockInstance.getMasterFromOriginOffset().subtract(getNextLevelMultiblock().getMasterFromOriginOffset());
        Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH, getFacing().getOpposite());
        ((MultiBlockAccess) getNextLevelMultiblock()).setPlayer(entityplayer);
        ((MultiBlockAccess) getNextLevelMultiblock()).callForm(level, getBlockPosForPos(negMasterOffset), rot, Mirror.NONE, getFacing());

    }
    public void repairStructure(ServerPlayer entityplayer) {
    	if(!isBroken)
    		return;
    	if(!FHUtils.costItems(entityplayer, getRepairCost()))
    		return;
    	isBroken=false;
    	getData().ifPresent(t->{t.isBroken=false;t.overdriveLevel=0;});

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

    public void regist() {
    	getDataNoCheck().ifPresent(t -> {
        	if(!master().worldPosition.equals(t.actualPos))
        		t.onPosChange();
        	this.setWorking(t.isWorking);
        	this.setOverdrive(t.isOverdrive);
            t.actualPos = master().worldPosition;
            t.dimension = this.level.dimension();
        });
    }
    
    public void tryRegist() {
    	getDataNoCheck().ifPresent(t -> {
    		if(BlockPos.ZERO.equals(t.actualPos)) {
	        	if(!master().worldPosition.equals(t.actualPos))
	        		t.onPosChange();
	        	this.setWorking(t.isWorking);
	        	this.setOverdrive(t.isOverdrive);
	            t.actualPos = master().worldPosition;
	            t.dimension = this.level.dimension();
    		}
        });
    }

	@Override
	public void onOwnerChange() {
		if(!this.isDummy())
			regist();
	}
    @Override
    public void tick() {

        super.tick();

    }

    @Override
    protected void tickEffects(boolean isActive) {

    }
    protected void tickDrives(boolean isActive) {

    }
    int remTicks;
    @Override
    protected boolean tickFuel() {
        // just finished process or during process
    	tryRegist();
        Optional<GeneratorData> data = this.getData();
        data.ifPresent(t -> {
            t.isOverdrive = this.isOverdrive;
            t.isWorking = this.isWorking;
        });
        data.ifPresent(t -> t.tick(this.getLevel()));
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
    public void unregist() {
        getDataNoCheck().ifPresent(t -> {
            t.actualPos = BlockPos.ZERO;
            t.dimension = null;
        });
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        if(!this.isDummy()||descPacket) {
	        ContainerHelper.saveAllItems(nbt, linventory);
	        nbt.putBoolean("hasFuel", hasFuel);
	        nbt.putBoolean("isBroken", isBroken);
        }
    }


    @Override
    public int getLowerBound() {
        return Mth.ceil(getRangeLevel()*2+1);
    }

    @Override
    public int getUpperBound() {
        return Mth.ceil(getRangeLevel() * 4+1);
    }
	@Override
	protected void callBlockConsumerWithTypeCheck(Consumer<T> consumer, BlockEntity te) {
	}

	@Override
	public void setRangeLevel(float f) {
		super.setRangeLevel(f);
	}

	@Override
	public void setTemperatureLevel(float temperatureLevel) {
		super.setTemperatureLevel(temperatureLevel);
	}

	public boolean hasFuel() {
		return hasFuel;
	}
}
