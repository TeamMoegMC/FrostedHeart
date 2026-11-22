/*
 * Copyright (c) 2026 TeamMoeg
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

import static net.minecraft.ChatFormatting.GRAY;

import java.util.List;
import java.util.Objects;

import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import com.teammoeg.frostedheart.content.steamenergy.ClientHeatNetworkData;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.teammoeg.chorda.block.CBlockInterfaces;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.reference.FHParticleTypes;
import com.teammoeg.frostedheart.content.climate.recipe.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatStorageCapability;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillAssembly;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillPower;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class ChargerTileEntity extends CBlockEntity implements CTickableBlockEntity, CBlockInterfaces.IActiveState,
        IHaveGoggleInformation, HeatNetworkProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final float CHARGER_POINTS_PER_HEAT = 1000F;
    // Preserve the save key already used by docked S04 drills.
    private static final String DOCKED_ITEM = "dockedDrill";
    private static final String DOCK_STATE = "drillDockState";
    private static final String DOCK_APPEARANCE = "drillDockAppearance";
    private static final byte DOCK_EMPTY = 0, DOCK_OCCUPIED = 1, DOCK_FULL = 2;

    // On clients the stack is a model-only projection, never the actual dock inventory.
    private ItemStack dockedItem = ItemStack.EMPTY;
    @Nullable private HeatStorageCapability dockedHeat;
    @Nullable private CompoundTag dockAppearance;
    @Nullable private AABB dockRenderBounds;
    private boolean dockNeedsCharge;
    private float lastUnaffordableDockBalance = Float.NaN;
    private byte clientDockState;

    HeatEndpoint network =HeatEndpoint.consumer(-10, 5);
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

    public InteractionResult onClick(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean client = level.isClientSide;
        if (held.isEmpty()) {
            // An empty offhand can also be tried when the player clicks with an unrelated main-hand item.
            if (hand != InteractionHand.MAIN_HAND || (client ? clientDockState == DOCK_EMPTY : dockedItem.isEmpty())) {
                return InteractionResult.PASS;
            }
            if (!client) {
                player.setItemInHand(hand, takeDockedItem());
                syncClientState();
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (StrataDrillAssembly.isDrill(held)) {
            if (!StrataDrillPower.canDockSteam(held)) return InteractionResult.PASS;
            return putInDock(player, hand, held, true);
        }
        HeatStorageCapability heat = FHCapabilities.ITEM_HEAT.getCapability(held).orElse(null);
        if (heat != null && heat.usesChargerDock()) return putInDock(player, hand, held, false);
        return useExistingCharge(player, held, heat);
    }

    private InteractionResult putInDock(Player player, InteractionHand hand, ItemStack held, boolean steamDrill) {
        boolean client = level.isClientSide;
        if (!client) {
            if (dockedItem.isEmpty()) {
                setDockedItem(held.split(1), steamDrill);
                if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
                player.displayClientMessage(Lang.translateMessage("charger.dock_inserted"), true);
                syncClientState();
            } else {
                player.displayClientMessage(Lang.translateMessage("charger.dock_busy"), true);
            }
        }
        return InteractionResult.sidedSuccess(client);
    }

    /** Resolve the owned stack's charging method once on insertion or save load. */
    private void setDockedItem(ItemStack stack, boolean steamDrill) {
        dockedItem = stack;
        HeatStorageCapability heat = steamDrill || stack.isEmpty() ? null
                : FHCapabilities.ITEM_HEAT.getCapability(stack).orElse(null);
        dockedHeat = heat != null && heat.usesChargerDock() ? heat : null;
        dockNeedsCharge = (steamDrill || dockedHeat != null) && !dockFull();
        lastUnaffordableDockBalance = Float.NaN;
        dockAppearance = StrataDrillAssembly.dockAppearance(stack);
        if (dockAppearance == null && dockedHeat != null) {
            // The other dockable items use their stock model without any item NBT.
            dockAppearance = new CompoundTag();
            dockAppearance.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            dockAppearance.putByte("Count", (byte) 1);
        }
    }

    /** Remove the original stack before handing it back or dropping it on block replacement. */
    ItemStack takeDockedItem() {
        ItemStack result = dockedItem;
        dockedItem = ItemStack.EMPTY;
        dockedHeat = null;
        dockAppearance = null;
        dockNeedsCharge = false;
        return result;
    }

    /** The model inputs saved in the update tag, with no fuel or inventory state. */
    public ItemStack getDockedDisplay() {
        return level != null && level.isClientSide ? dockedItem : ItemStack.EMPTY;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return dockRenderBounds == null ? super.getRenderBoundingBox() : dockRenderBounds;
    }

    private byte dockState() {
        if (dockedItem.isEmpty()) return DOCK_EMPTY;
        return dockFull() ? DOCK_FULL : DOCK_OCCUPIED;
    }

    private boolean dockFull() {
        return dockedHeat != null ? dockedHeat.isFull()
                : StrataDrillPower.steamStored(dockedItem) >= StrataDrillPower.STEAM_CAPACITY;
    }

    /** Save internal contents and synchronize the visible state without a comparator/neighbor scan. */
    private void syncClientState() {
        level.blockEntityChanged(worldPosition);
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    private InteractionResult useExistingCharge(Player player, ItemStack input, @Nullable HeatStorageCapability heat) {
        boolean client = level.isClientSide;
        if (heat != null) {
            if (!client) {
                float actual = heat.receiveEnergy(power, false);
                if (actual > 0F) {
                    power -= actual;
                    finishLegacyCharge(player);
                }
            }
            return InteractionResult.sidedSuccess(client);
        }

        // Partial dock updates are not sent every tick. Claim a matching input without predicting its
        // affordability so vanilla does not also use the other hand or the item in the air.
        boolean pendingDock = client ? clientDockState == DOCK_OCCUPIED : dockNeedsCharge;
        boolean matched = false;
        ChargerRecipe recipe = findRecipe(input);
        if (recipe != null && recipe.cost >= 0F && input.getCount() >= recipe.input.getCount()) {
            if (client && pendingDock) return InteractionResult.CONSUME;
            if (power >= recipe.cost) {
                if (!client) {
                    power -= recipe.cost;
                    input.shrink(recipe.input.getCount());
                    CUtils.giveItem(player, recipe.output.copy());
                    finishLegacyCharge(player);
                }
                return InteractionResult.SUCCESS;
            }
            matched = true;
        }

        if (power >= 100F || pendingDock) {
            for (SmokingRecipe smoking : level.getRecipeManager().getAllRecipesFor(RecipeType.SMOKING)) {
                if (!smoking.getIngredients().get(0).test(input)) continue;
                float cost = smoking.getCookingTime() / 20F;
                if (cost >= 0F) {
                    if (client && pendingDock) return InteractionResult.CONSUME;
                    if (power >= 100F && power >= cost) return finishCooking(player, input, smoking, cost);
                    matched = true;
                }
                break;
            }
        }
        for (CampfireDefrostRecipe defrost : CampfireDefrostRecipe.recipeList.values()) {
            if (defrost.getIngredient().test(input)) {
                float cost = defrost.getCookingTime() / 80F;
                if (cost >= 0F) {
                    if (client && pendingDock) return InteractionResult.CONSUME;
                    if (power >= cost) return finishCooking(player, input, defrost, cost);
                    matched = true;
                }
                break;
            }
        }
        return pendingDock && matched ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    private InteractionResult finishCooking(Player player, ItemStack input, AbstractCookingRecipe recipe, float cost) {
        if (!level.isClientSide) {
            power -= cost;
            splitAndSpawnExperience(level, player.blockPosition(), recipe.getExperience());
            input.shrink(1);
            CUtils.giveItem(player, recipe.assemble(null, level.registryAccess()).copy());
            finishLegacyCharge(player);
        }
        return InteractionResult.SUCCESS;
    }

    /** Successful legacy uses report one authoritative steam puff to the initiating player. */
    private void finishLegacyCharge(Player player) {
        syncClientState();
        if (level instanceof ServerLevel server && player instanceof ServerPlayer viewer && viewer.connection != null) {
            server.sendParticles(viewer, FHParticleTypes.STEAM.get(), false,
                    worldPosition.getX() + 0.5D, worldPosition.getY() + 0.4D, worldPosition.getZ() + 0.5D,
                    0, 0D, 0.05D, 0D, 1D);
        }
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket) {
        // Old unfunded cooking could leave a negative value, never usable as station credit.
        power = Math.max(0F, nbt.getFloat("power"));
        network.load(nbt, descPacket);
        if (descPacket) {
            clientDockState = nbt.getByte(DOCK_STATE);
            CompoundTag appearance = nbt.contains(DOCK_APPEARANCE, Tag.TAG_COMPOUND) ? nbt.getCompound(DOCK_APPEARANCE) : null;
            if (!Objects.equals(dockAppearance, appearance)) {
                dockAppearance = appearance;
                dockedItem = appearance == null ? ItemStack.EMPTY : ItemStack.of(appearance);
                dockRenderBounds = dockedItem.isEmpty() ? null : new AABB(worldPosition).inflate(0.2);
            }
        } else {
            ItemStack stack = nbt.contains(DOCKED_ITEM, Tag.TAG_COMPOUND)
                    ? ItemStack.of(nbt.getCompound(DOCKED_ITEM)) : ItemStack.EMPTY;
            setDockedItem(stack, !stack.isEmpty() && StrataDrillPower.canDockSteam(stack));
        }
    }

    @Override
    public void tick() {
        if (level == null) {
            return;
        }
        if (!level.isClientSide) {
            boolean charging = dockNeedsCharge;
            float actual = 0;
            if (power < getMaxPower()) {
                float requested = Math.min(network.getMaxIntake(),
                        (getMaxPower() - power) / CHARGER_POINTS_PER_HEAT);
                float storedHeat = network.getHeat();
                actual = Math.min(storedHeat, requested);
                if (actual > 0F) {
                    // Endpoint load grace can replay a saved reading for active heating, not mint durable station points.
                    network.setHeat(storedHeat - actual);
                    power = Math.min(power + actual * CHARGER_POINTS_PER_HEAT, getMaxPower());
                }
            }
            float spent = 0F;
            if (charging && power > 0F && power != lastUnaffordableDockBalance) {
                spent = dockedHeat != null ? dockedHeat.chargeAtDock(power)
                        : StrataDrillPower.chargeDockedSteam(dockedItem, power);
                // Retry only when idle balance changes or the slot is installed/reloaded.
                lastUnaffordableDockBalance = spent > 0F ? Float.NaN : power;
            }
            if (spent > 0F) {
                power -= spent;
                dockNeedsCharge = !dockFull();
            }
            setActive(actual > 0F || spent > 0F);
            if (charging) {
                // Persist live progress; no explicit progress packets for an occupied slot.
                if (!dockNeedsCharge) syncClientState();
                else if (actual > 0F || spent > 0F) level.blockEntityChanged(worldPosition);
            } else if (actual > 0F) {
                syncClientState();
            }
        } else if (getIsActive()) {
            if (level.random.nextFloat() < 0.01F) {
                FHClientUtils.spawnSteamParticles(level, worldPosition);
            }
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        nbt.putFloat("power", power);
        network.save(nbt, descPacket);
        if (descPacket) {
            nbt.putByte(DOCK_STATE, dockState());
            if (dockAppearance != null) nbt.put(DOCK_APPEARANCE, dockAppearance);
        } else if (!dockedItem.isEmpty()) {
            nbt.put(DOCKED_ITEM, dockedItem.save(new CompoundTag()));
        }
    }
	@Override
	public void invalidateCaps() {
		heatcap.invalidate();
		super.invalidateCaps();
	}

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Lang.tooltip("heat_stats").forGoggles(tooltip);
        if (clientDockState != DOCK_EMPTY) {
            Lang.tooltip(clientDockState == DOCK_FULL ? "charger.dock_ready" : "charger.dock_occupied")
                    .forGoggles(tooltip);
        }

        if (TemperatureGoogleRenderer.hasHeatNetworkData()) {
            ClientHeatNetworkData data = TemperatureGoogleRenderer.getHeatNetworkData();

            Lang.translate("tooltip", "pressure.network")
                    .style(GRAY)
                    .forGoggles(tooltip);

            Lang.number(data.totalEndpointIntake)
                    .translate("generic", "unit.pressure")
                    .style(ChatFormatting.AQUA)
                    .space()
                    .add(Lang.translate("tooltip", "pressure.intake")
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            Lang.number(data.totalEndpointOutput)
                    .translate("generic", "unit.pressure")
                    .style(ChatFormatting.AQUA)
                    .space()
                    .add(Lang.translate("tooltip", "pressure.output")
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            // show number of endpoints
            Lang.number(data.endpoints.size())
                    .style(ChatFormatting.AQUA)
                    .space()
                    .add(Lang.translate("tooltip", "pressure.endpoints")
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);

            Lang.translate("tooltip", "pressure.endpoint")
                    .style(GRAY)
                    .forGoggles(tooltip);

            // stream through endpoints, filter by pos
            data.endpoints.stream()
                    .filter(e -> e.getPos().equals(worldPosition))
                    .forEach(e -> {
                        float maxIntake = e.getMaxIntake();
                        float maxOutput = e.getMaxOutput();
                        float avgIntake = e.getAvgIntake();
                        float avgOutput = e.getAvgOutput();

                        if (maxIntake > 0)
                            Lang.number(e.getMaxIntake())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(Lang.translate("tooltip", "pressure.max_intake")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);

                        if (maxOutput > 0)
                            Lang.number(e.getMaxOutput())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(Lang.translate("tooltip", "pressure.max_output")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);

                        if (avgIntake > 0)
                            Lang.number(e.getAvgIntake())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(Lang.translate("tooltip", "pressure.average_intake")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);

                        if (avgOutput > 0)
                            Lang.number(e.getAvgOutput())
                                    .translate("generic", "unit.pressure")
                                    .style(ChatFormatting.AQUA)
                                    .space()
                                    .add(Lang.translate("tooltip", "pressure.average_output")
                                            .style(ChatFormatting.DARK_GRAY))
                                    .forGoggles(tooltip, 1);
                    });

        } else {
            Lang.translate("tooltip", "pressure.no_network")
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip);
        }

        return true;

    }

    @Override
    public @Nullable HeatNetwork getNetwork() {
        return network.getNetwork();
    }
}
