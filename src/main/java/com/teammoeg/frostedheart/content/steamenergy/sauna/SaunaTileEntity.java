/*
 * Copyright (c) 2022 TeamMoeg
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

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHEffects;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.TemperatureCore;
import com.teammoeg.frostedheart.content.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.SteamNetworkHolder;
import com.teammoeg.frostedheart.research.inspire.EnergyCore;
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
import net.minecraft.tileentity.TileEntity;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class SaunaTileEntity extends IEBaseTileEntity implements
        INetworkConsumer, ITickableTileEntity, FHBlockInterfaces.IActiveState, IIEInventory, IInteractionObjectIE {

    private static final float POWER_CAP = 400;
    private static final float REFILL_THRESHOLD = 200;
    private static final int RANGE = 5;
    private static final int WALL_HEIGHT = 3;
    private static final Direction[] HORIZONTALS =
            new Direction[] { Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH };

    private float power = 0;
    private int remainTime = 0;
    private int maxTime = 0;
    private Effect effect = null;
    private int effectDuration = 0;
    private int effectAmplifier = 0;
    private boolean refilling = false;
    private boolean formed = false;
    Set<BlockPos> floor = new HashSet<>();
    Set<BlockPos> edges = new HashSet<>();
    SteamNetworkHolder network = new SteamNetworkHolder();

    protected NonNullList<ItemStack> inventory;
    private LazyOptional<IItemHandler> insertionCap;

    public SaunaTileEntity() {
        super(FHTileTypes.SAUNA.get());
        this.inventory = NonNullList.withSize(1, ItemStack.EMPTY);
        this.insertionCap = LazyOptional.of(() -> new IEInventoryHandler(15, this));
    }

    public float getPowerFraction() {
        return power / POWER_CAP;
    }

    public boolean isWorking() {
        return formed && power > 0;
    }

    public boolean hasMedicine() {
        return remainTime > 0 && effect != null;
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

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        power = nbt.getFloat("power");
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
        refilling = nbt.getBoolean("refilling");
        formed = nbt.getBoolean("formed");
        ListNBT floorNBT = nbt.getList("floor", Constants.NBT.TAG_COMPOUND);
        floor.clear();
        for (int i = 0; i < floorNBT.size(); i++) {
            BlockPos pos = NBTUtil.readBlockPos(floorNBT.getCompound(i));
            floor.add(pos);
        }
        this.inventory = NonNullList.withSize(1, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(nbt, this.inventory);
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        nbt.putFloat("power", power);
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
        nbt.putBoolean("refilling", refilling);
        nbt.putBoolean("formed", formed);
        ListNBT floorNBT = new ListNBT();
        for (BlockPos pos : floor) {
            CompoundNBT posNBT = NBTUtil.writeBlockPos(pos);
            floorNBT.add(posNBT);
        }
        ItemStackHelper.saveAllItems(nbt, this.inventory);
        nbt.put("floor", floorNBT);
    }

    @Override
    public boolean connect(Direction to, int dist) {
        return network.reciveConnection(world, pos, to, dist);
    }

    @Override
    public boolean canConnectAt(Direction to) {
        return to == Direction.UP;
    }

    @Override
    public SteamNetworkHolder getHolder() {
        return network;
    }

    @Override
    public void tick() {
        // server side logic
        if (!world.isRemote) {
            // power logic
            if (network.isValid()) {
                network.tick();
                // start refill if power is below REFILL_THRESHOLD
                // keep refill until network is full
                if (refilling || power < REFILL_THRESHOLD) {
                    float actual = network.drainHeat(Math.min(200, (POWER_CAP - power) / 0.8F));
                    // during refill, grow power and show steam
                    if (actual > 0) {
                        refilling = true;
                        power += actual * 0.8F;
                        this.setActive(true);
                    }
                    // finished refill, check structure, grant effects, happens every 200 ticks
                    else {
                        formed = structureIsValid();
                        refilling = false;
                        this.setActive(false);
                    }
                }
                // if not refilling, consume power
                else {
                    power--;
                }
                markDirty();
                this.markContainingBlockForUpdate(null);
            } else this.setActive(false);

            // grant player effect if structure is valid
            if (formed && power > 0) {
                // consume medcine time
                remainTime = Math.max(0, remainTime - 1);
                // refill time if medicine exists
                ItemStack medicine = this.inventory.get(0);
                if (remainTime == 0) {
                    effect = null;
                    effectDuration = 0;
                    effectAmplifier = 0;
                    if (!medicine.isEmpty()) {
                        SaunaRecipe recipe = SaunaRecipe.findRecipe(medicine);
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
                this.markContainingBlockForUpdate(null);

                for (PlayerEntity p : this.getWorld().getPlayers()) {
                    if (floor.contains(p.getPosition().offset(Direction.DOWN))) {
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

    public ActionResultType onClick(PlayerEntity player) {
        if (!player.world.isRemote) {
            if (formed) {
                // player.sendStatusMessage(GuiUtils.translateMessage("structure_formed"), true);
                NetworkHooks.openGui((ServerPlayerEntity) player, this, this.getPos());
            } else {
                player.sendStatusMessage(GuiUtils.translateMessage("structure_not_formed"), true);
            }
        }
        return ActionResultType.SUCCESS;
    }

    private void grantEffects(ServerPlayerEntity p) {
        // add effect only if armor is not equipped
        if (p.getArmorCoverPercentage() > 0.0F) {
            return;
        }
        // add wet effect
        if (world.getGameTime() % 200L == 0L) {
            p.addPotionEffect(new EffectInstance(FHEffects.WET, 200, 0, true, false));
        }
        // add sauna effect
        if (world.getGameTime() % 1000L == 0L && !p.isPotionActive(FHEffects.SAUNA)) {
            // initial reward
            EnergyCore.addEnergy(p, 1000);
            // whole day reward
            p.addPotionEffect(new EffectInstance(FHEffects.SAUNA, 23000, 0, true, false));
        }
        // add temperature
        float lenvtemp = TemperatureCore.getEnvTemperature(p);//get a smooth change in display
        float lbodytemp = TemperatureCore.getBodyTemperature(p);
        TemperatureCore.setTemperature(p, 1.01f * .01f + lbodytemp * .99f, 65 * .1f + lenvtemp * .9f);
        // add medical effect
        if (hasMedicine() && remainTime == 1) {
            p.addPotionEffect(getEffectInstance());
        }
    }

    private boolean dist(BlockPos crn, BlockPos orig) {
        return MathHelper.abs(crn.getX() - orig.getX()) <= RANGE && MathHelper.abs(crn.getZ() - orig.getZ()) <= RANGE;
    }

    private void findNext(World l, BlockPos crn, BlockPos orig, Set<BlockPos> poss, Set<BlockPos> edges) {
        if (dist(crn, orig)) {
            if (poss.add(crn)) {
                for (Direction dir : HORIZONTALS) {
                    BlockPos act = crn.offset(dir);
                    // if crn connected to plank
                    if (l.isBlockPresent(act) && (l.getBlockState(act).isIn(BlockTags.PLANKS) || l.getBlockState(act).getBlock().matchesBlock(FHBlocks.sauna))) {
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

    @Nullable
    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public boolean isStackValid(int slot, ItemStack itemStack) {
        return true;
    }

    @Override
    public int getSlotLimit(int i) {
        return 64;
    }

    @Override
    public void doGraphicalUpdates() {

    }

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? this.insertionCap.cast() : super.getCapability(capability, facing);
    }

    @Nullable
    @Override
    public IInteractionObjectIE getGuiMaster() {
        return this;
    }

    @Override
    public boolean canUseGui(PlayerEntity playerEntity) {
        return true;
    }

    @Override
    public void receiveMessageFromServer(CompoundNBT message) {
        super.receiveMessageFromServer(message);
    }
}
