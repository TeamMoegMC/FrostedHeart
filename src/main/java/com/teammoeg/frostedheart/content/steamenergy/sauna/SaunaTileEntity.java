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

import blusunrize.immersiveengineering.common.blocks.IEBaseBlockEntity;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.block.FHTickableBlockEntity;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SaunaTileEntity extends IEBaseBlockEntity implements FHTickableBlockEntity, FHBlockInterfaces.IActiveState, IIEInventory, MenuProvider {

    private static final int RANGE = 5;
    private static final int WALL_HEIGHT = 3;
    private static final Direction[] HORIZONTALS =
            new Direction[]{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
    protected NonNullList<ItemStack> inventory;
    Set<BlockPos> floor = new HashSet<>();
    Set<BlockPos> edges = new HashSet<>();
    HeatEndpoint network = new HeatEndpoint(10, 10, 0, 1);
    LazyOptional<HeatEndpoint> heatcap = LazyOptional.of(() -> network);
    private int remainTime = 0;
    private int maxTime = 0;
    private MobEffect effect = null;
    private int effectDuration = 0;
    private int effectAmplifier = 0;
    private boolean formed = false;
    private int workPeriod;
    private LazyOptional<IItemHandler> insertionCap;


    public SaunaTileEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.SAUNA.get(), pos, state);
        this.inventory = NonNullList.withSize(1, ItemStack.EMPTY);
        this.insertionCap = LazyOptional.of(() -> new IEInventoryHandler(1, this));
    }

    private boolean dist(BlockPos crn, BlockPos orig) {
        return Mth.abs(crn.getX() - orig.getX()) <= RANGE && Mth.abs(crn.getZ() - orig.getZ()) <= RANGE;
    }

    @Override
    public void doGraphicalUpdates() {

    }

    private void findNext(Level l, BlockPos crn, BlockPos orig, Set<BlockPos> poss, Set<BlockPos> edges) {
        if (dist(crn, orig)) {
            if (poss.add(crn)) {
                for (Direction dir : HORIZONTALS) {
                    BlockPos act = crn.relative(dir);
                    // if crn connected to plank
                    if (l.isLoaded(act) && (l.getBlockState(act).is(BlockTags.PLANKS) || l.getBlockState(act).is(FHBlocks.SAUNA_VENT.get()))) {
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

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if (capability == ForgeCapabilities.ITEM_HANDLER)
            return this.insertionCap.cast();
        if (capability == FHCapabilities.HEAT_EP.capability() && facing == Direction.DOWN) {
            return heatcap.cast();
        }
        return super.getCapability(capability, facing);
    }

    public MobEffectInstance getEffectInstance() {
        if (effect != null) {
            return new MobEffectInstance(effect, effectDuration, effectAmplifier, true, true);
        } else {
            return null;
        }
    }

    public float getEffectTimeFraction() {
        return (float) remainTime / (float) maxTime;
    }

    @Nullable
    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    public float getPowerFraction() {
        return network.getHeat() / network.getCapacity();
    }

    @Override
    public int getSlotLimit(int i) {
        return 64;
    }

    private void grantEffects(ServerPlayer p) {
        // add effect only if armor is not equipped
        if (p.getArmorCoverPercentage() > 0.0F) {
            return;
        }
        UUID owner = IOwnerTile.getOwner(this);
        if (owner == null) return;
        UUID t = FHTeamDataManager.get(p).getId();
        if (t == null || !t.equals(owner)) return;
        // add wet effect
        if (level.getGameTime() % 200L == 0L) {
            p.addEffect(new MobEffectInstance(FHMobEffects.WET.get(), 200, 0, true, false));
        }

        // add sauna effect
        if (level.getGameTime() % 1000L == 0L && !p.hasEffect(FHMobEffects.SAUNA.get())) {
            // initial reward
            EnergyCore.addEnergy(p, 1000);
            // whole day reward
            p.addEffect(new MobEffectInstance(FHMobEffects.SAUNA.get(), 23000, 0, true, false));
        }

        // add medical effect
        if (hasMedicine() && remainTime == 1) {
            p.addEffect(getEffectInstance());
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

    public InteractionResult onClick(Player player) {
        if (!player.level().isClientSide) {
            if (formed) {
                // player.sendStatusMessage(GuiUtils.translateMessage("structure_formed"), true);
                NetworkHooks.openScreen((ServerPlayer) player, this, this.getBlockPos());
            } else {
                player.displayClientMessage(Lang.translateMessage("structure_not_formed"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        remainTime = nbt.getInt("time");
        maxTime = nbt.getInt("maxTime");
        if (nbt.contains("effect")) {
            effect = MobEffect.byId(nbt.getInt("effect"));
            effectDuration = nbt.getInt("duration");
            effectAmplifier = nbt.getInt("amplifier");
        } else {
            effect = null;
            effectDuration = 0;
            effectAmplifier = 0;
        }
        formed = nbt.getBoolean("formed");
        ListTag floorNBT = nbt.getList("floor", Tag.TAG_COMPOUND);
        floor.clear();
        for (int i = 0; i < floorNBT.size(); i++) {
            BlockPos pos = NbtUtils.readBlockPos(floorNBT.getCompound(i));
            floor.add(pos);
        }
        this.inventory = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, this.inventory);
        network.load(nbt, descPacket);
    }

    @Override
    public void receiveMessageFromServer(CompoundTag message) {
        super.receiveMessageFromServer(message);
    }

    private boolean structureIsValid() {
        floor.clear();
        edges.clear();
        // collect connected floor and edges
        findNext(this.getLevel(), this.getBlockPos(), this.getBlockPos(), floor, edges);
        // check wall exist for each edge block
        for (BlockPos pos : edges) {
            for (int y = 1; y <= WALL_HEIGHT; y++) {
                BlockState wall = level.getBlockState(pos.relative(Direction.UP, y));
                if (!wall.is(BlockTags.PLANKS) && !wall.is(BlockTags.DOORS)) {
                    return false;
                }
            }
            // remove edges from the actual floor player can stand on, since play can't stand on wall
            floor.remove(pos);
        }
        // check ceiling exist for each floor block
        for (BlockPos pos : floor) {
            BlockState ceiling = level.getBlockState(pos.relative(Direction.UP, WALL_HEIGHT));
            if (!ceiling.is(BlockTags.PLANKS) && !ceiling.is(BlockTags.TRAPDOORS)) {
                return false;
            }
        }
        return true;
    }

    public SaunaRecipe findRecipe(ItemStack input) {
        for (SaunaRecipe recipe : FHUtils.filterRecipes(this.getLevel().getRecipeManager(), SaunaRecipe.TYPE))
            if (recipe.input.test(input))
                return recipe;
        return null;
    }

    @Override
    public void tick() {
        // server side logic
        if (!level.isClientSide) {
            //Check formed
            workPeriod--;
            if (workPeriod < 0) {
                workPeriod = 200;
                formed = this.structureIsValid();
            }

            // power logic
            if (formed && network.tryDrainHeat(1)) {
                this.setActive(true);
                setChanged();
            } else this.setActive(false);

            // grant player effect if structure is valid
            if (getIsActive()) {
                // consume medcine time
                if (remainTime > 0) {
                    remainTime -= 1;
                } else {
                    ItemStack medicine = this.inventory.get(0);
                    effect = null;
                    effectDuration = 0;
                    effectAmplifier = 0;
                    if (!medicine.isEmpty()) {
                        SaunaRecipe recipe = findRecipe(medicine);
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

                setChanged();

                for (Player p : this.getLevel().players()) {
                    if (floor.contains(p.blockPosition().below()) || floor.contains(p.blockPosition())) {
                        grantEffects((ServerPlayer) p);
                    }
                }
            }
        }
        // client side render
        else if (getIsActive()) {
            ClientUtils.spawnSteamParticles(this.getLevel(), worldPosition);
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.putInt("time", remainTime);
        nbt.putInt("maxTime", maxTime);
        if (effect != null) {
            nbt.putInt("effect", MobEffect.getId(effect));
            nbt.putInt("effectDuration", effectDuration);
            nbt.putInt("effectAmplifier", effectAmplifier);
        } else {
            nbt.remove("effect");
            nbt.remove("effectDuration");
            nbt.remove("effectAmplifier");
        }
        nbt.putBoolean("formed", formed);
        ListTag floorNBT = new ListTag();
        for (BlockPos pos : floor) {
            CompoundTag posNBT = NbtUtils.writeBlockPos(pos);
            floorNBT.add(posNBT);
        }
        ContainerHelper.saveAllItems(nbt, this.inventory);
        nbt.put("floor", floorNBT);
        network.save(nbt, descPacket);
    }


    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new SaunaContainer(pContainerId, pPlayerInventory, this);
    }


    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

	@Override
	public void invalidateCaps() {
		heatcap.invalidate();
		super.invalidateCaps();
	}
}
