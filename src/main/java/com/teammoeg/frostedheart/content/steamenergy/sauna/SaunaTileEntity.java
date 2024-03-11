/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy.sauna;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatConsumerEndpoint;
import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class SaunaTileEntity extends IEBaseTileEntity implements ITickableTileEntity, FHBlockInterfaces.IActiveState, IIEInventory, IInteractionObjectIE {

    private static final int RANGE = 5;
    private static final int WALL_HEIGHT = 3;
    private static final Direction[] HORIZONTALS =
            new Direction[]{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};

    private int remainTime = 0;
    private int maxTime = 0;
    private Effect effect = null;
    private int effectDuration = 0;
    private int effectAmplifier = 0;
    private boolean formed = false;
    private int workPeriod;
    Set<BlockPos> floor = new HashSet<>();
    Set<BlockPos> edges = new HashSet<>();
    HeatConsumerEndpoint network = new HeatConsumerEndpoint(10,1);

    protected NonNullList<ItemStack> inventory;
    private LazyOptional<IItemHandler> insertionCap;

    public SaunaTileEntity() {
        super(FHTileTypes.SAUNA.get());
        this.inventory = NonNullList.withSize(1, ItemStack.EMPTY);
        this.insertionCap = LazyOptional.of(() -> new IEInventoryHandler(1, this));
    }

    @Override
    public boolean canUseGui(PlayerEntity playerEntity) {
        return true;
    }


    private boolean dist(BlockPos crn, BlockPos orig) {
        return MathHelper.abs(crn.getX() - orig.getX()) <= RANGE && MathHelper.abs(crn.getZ() - orig.getZ()) <= RANGE;
    }

    @Override
    public void doGraphicalUpdates() {

    }

    private void findNext(World l, BlockPos crn, BlockPos orig, Set<BlockPos> poss, Set<BlockPos> edges) {
        if (dist(crn, orig)) {
            if (poss.add(crn)) {
                for (Direction dir : HORIZONTALS) {
                    BlockPos act = crn.offset(dir);
                    // if crn connected to plank
                    if (l.isBlockPresent(act) && (l.getBlockState(act).isIn(BlockTags.PLANKS) || l.getBlockState(act).getBlock().matchesBlock(FHBlocks.sauna.get()))) {
                        findNext(l, act, orig, poss, edges);
                    }
                    // otherwise, crn is an edge block
                    else {
                        edges.add(crn);
                    }
                }
            }
        }
    }
    LazyOptional<HeatConsumerEndpoint> heatcap=LazyOptional.of(()->network);
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
    	if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
    		return this.insertionCap.cast();
		if(capability==FHCapabilities.HEAT_EP.capability()&&facing==Direction.DOWN) {
			return heatcap.cast();
		}
		return super.getCapability(capability, facing);
    }

    public EffectInstance getEffectInstance() {
        if (effect != null) {
            return new EffectInstance(effect, effectDuration, effectAmplifier, true, true);
        } else {
            return null;
        }
    }

    public float getEffectTimeFraction() {
        return (float) remainTime / (float) maxTime;
    }

    @Nullable
    @Override
    public IInteractionObjectIE getGuiMaster() {
        return this;
    }

    @Nullable
    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    public float getPowerFraction() {
        return network.getPower() / network.getMaxPower();
    }

    @Override
    public int getSlotLimit(int i) {
        return 64;
    }

    private void grantEffects(ServerPlayerEntity p) {
        // add effect only if armor is not equipped
        if (p.getArmorCoverPercentage() > 0.0F) {
            return;
        }
        UUID owner = IOwnerTile.getOwner(this);
        if (owner == null) return;
        UUID t = FHTeamDataManager.get(p).getId();
        if (t == null || !t.equals(owner)) return;
        // add wet effect
        if (world.getGameTime() % 200L == 0L) {
            p.addPotionEffect(new EffectInstance(FHEffects.WET.get(), 200, 0, true, false));
        }

        // add sauna effect
        if (world.getGameTime() % 1000L == 0L && !p.isPotionActive(FHEffects.SAUNA.get())) {
            // initial reward
            EnergyCore.addEnergy(p, 1000);
            // whole day reward
            p.addPotionEffect(new EffectInstance(FHEffects.SAUNA.get(), 23000, 0, true, false));
        }
        
        // add medical effect
        if (hasMedicine() && remainTime == 1) {
            p.addPotionEffect(getEffectInstance());
        }
    }

    public boolean hasMedicine() {
        return remainTime > 0 && effect != null;
    }

    @Override
    public boolean isStackValid(int slot, ItemStack itemStack) {
        return findRecipe(itemStack) != null;
    }

    public boolean isWorking() {
        return getIsActive();
    }

    public ActionResultType onClick(PlayerEntity player) {
        if (!player.world.isRemote) {
            if (formed) {
                // player.sendStatusMessage(GuiUtils.translateMessage("structure_formed"), true);
                NetworkHooks.openGui((ServerPlayerEntity) player, this, this.getPos());
            } else {
                player.sendStatusMessage(TranslateUtils.translateMessage("structure_not_formed"), true);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        remainTime = nbt.getInt("time");
        maxTime = nbt.getInt("maxTime");
        if (nbt.contains("effect")) {
            effect = Effect.get(nbt.getInt("effect"));
            effectDuration = nbt.getInt("duration");
            effectAmplifier = nbt.getInt("amplifier");
        } else {
            effect = null;
            effectDuration = 0;
            effectAmplifier = 0;
        }
        formed = nbt.getBoolean("formed");
        ListNBT floorNBT = nbt.getList("floor", Constants.NBT.TAG_COMPOUND);
        floor.clear();
        for (int i = 0; i < floorNBT.size(); i++) {
            BlockPos pos = NBTUtil.readBlockPos(floorNBT.getCompound(i));
            floor.add(pos);
        }
        this.inventory = NonNullList.withSize(1, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(nbt, this.inventory);
        network.load(nbt, descPacket);
    }

    @Override
    public void receiveMessageFromServer(CompoundNBT message) {
        super.receiveMessageFromServer(message);
    }

    private boolean structureIsValid() {
        floor.clear();
        edges.clear();
        // collect connected floor and edges
        findNext(this.getWorld(), this.getPos(), this.getPos(), floor, edges);
        // check wall exist for each edge block
        for (BlockPos pos : edges) {
            for (int y = 1; y <= WALL_HEIGHT; y++) {
                BlockState wall = world.getBlockState(pos.offset(Direction.UP, y));
                if (!wall.isIn(BlockTags.PLANKS) && !wall.isIn(BlockTags.DOORS)) {
                    return false;
                }
            }
            // remove edges from the actual floor player can stand on, since play can't stand on wall
            floor.remove(pos);
        }
        // check ceiling exist for each floor block
        for (BlockPos pos : floor) {
            BlockState ceiling = world.getBlockState(pos.offset(Direction.UP, WALL_HEIGHT));
            if (!ceiling.isIn(BlockTags.PLANKS) && !ceiling.isIn(BlockTags.TRAPDOORS)) {
                return false;
            }
        }
        return true;
    }

    public SaunaRecipe findRecipe(ItemStack input) {
        for (SaunaRecipe recipe : FHUtils.filterRecipes(this.getWorld().getRecipeManager(), SaunaRecipe.TYPE))
            if (recipe.input.test(input))
                return recipe;
        return null;
    }
    @Override
    public void tick() {
        // server side logic
        if (!world.isRemote) {
        	//Check formed
        	workPeriod--;
            if(workPeriod<0) {
            	workPeriod=200;
            	formed=this.structureIsValid();
            }
        	
            // power logic
            if (formed&&network.tryDrainHeat(1)) {
            	this.setActive(true);
                markDirty();
            } else this.setActive(false);

            // grant player effect if structure is valid
            if (getIsActive()) {
                // consume medcine time
            	if(remainTime>0) {
            		remainTime -= 1;
            	}else{
            		ItemStack medicine = this.inventory.get(0);
                    effect = null;
                    effectDuration = 0;
                    effectAmplifier = 0;
                    if (!medicine.isEmpty()) {
                        SaunaRecipe recipe =findRecipe(medicine);
                        if (recipe != null) {
                            maxTime = recipe.time;
                            remainTime += recipe.time;
                            medicine.shrink(1);
                            effect = recipe.effect;
                            effectDuration = recipe.duration;
                            effectAmplifier = recipe.amplifier;
                        }
                    }
                }

                markDirty();

                for (PlayerEntity p : this.getWorld().getPlayers()) {
                    if (floor.contains(p.getPosition().down()) || floor.contains(p.getPosition())) {
                        grantEffects((ServerPlayerEntity) p);
                    }
                }
            }
        }
        // client side render
        else if (getIsActive()) {
            ClientUtils.spawnSteamParticles(this.getWorld(), pos);
        }
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        nbt.putInt("time", remainTime);
        nbt.putInt("maxTime", maxTime);
        if (effect != null) {
            nbt.putInt("effect", Effect.getId(effect));
            nbt.putInt("effectDuration", effectDuration);
            nbt.putInt("effectAmplifier", effectAmplifier);
        } else {
            nbt.remove("effect");
            nbt.remove("effectDuration");
            nbt.remove("effectAmplifier");
        }
        nbt.putBoolean("formed", formed);
        ListNBT floorNBT = new ListNBT();
        for (BlockPos pos : floor) {
            CompoundNBT posNBT = NBTUtil.writeBlockPos(pos);
            floorNBT.add(posNBT);
        }
        ItemStackHelper.saveAllItems(nbt, this.inventory);
        nbt.put("floor", floorNBT);
        network.save(nbt, descPacket);
    }
}
