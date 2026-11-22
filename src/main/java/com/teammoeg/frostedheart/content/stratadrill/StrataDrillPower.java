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

package com.teammoeg.frostedheart.content.stratadrill;

import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.common.register.IEFluids;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import javax.annotation.Nullable;

/** Storage and accounting for the installed S04 drive; no ticking fuel or shared engine reserve. */
public final class StrataDrillPower {
    // Portable charge points, not the charger's own points or fluid mB.
    public static final int STEAM_CAPACITY = 32_000;
    public static final int STEAM_PER_BLOCK = 25;
    public static final int STEAM_PER_WHEEL_NEIGHBOR = 20;
    private static final int CHARGER_POINTS_PER_STEAM_POINT = 34;
    // mB of IE biodiesel.
    public static final int DIESEL_CAPACITY = 16_800;
    public static final int DIESEL_PER_BLOCK = 7;
    public static final int DIESEL_PER_WHEEL_NEIGHBOR = 6;
    public static final float DIESEL_SPEED_MULTIPLIER = 1.25F;
    private static final String STEAM_KEY = "frostedheart:stratadrill_steam";
    private static final String DIESEL_KEY = "frostedheart:stratadrill_diesel";
    private static final int AFFORDABLE_STEAM = 1;
    private static final int AFFORDABLE_DIESEL = 2;
    // Only synchronous server wheel callbacks (including Forge harvest and the native loot copy).
    private static final ThreadLocal<StrataDrillWorkAccess> WHEEL_NEIGHBOR = new ThreadLocal<>();

    private StrataDrillPower() {}

    /** Non-mutating read so checking a held item cannot change vanilla mining's stack identity. */
    public static float steamStored(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        float stored = tag == null ? 0 : tag.getFloat(STEAM_KEY);
        return Float.isFinite(stored) ? Mth.clamp(stored, 0, STEAM_CAPACITY) : 0;
    }

    private static void setSteam(ItemStack stack, float value) {
        if (value > 0) stack.getOrCreateTag().putFloat(STEAM_KEY, value);
        else stack.removeTagKey(STEAM_KEY);
    }

    /** Resolve the assembly when a charger takes ownership, not on every charging tick. */
    public static boolean canDockSteam(ItemStack stack) {
        return hasDrive(stack, true);
    }

    /** Adds charge to the validated server-side docked stack and returns the exact charger debit.
     * New charge stops at a whole portable point. Unspent charger points stay in the machine,
     * and a fractional point already in an old stack counts toward the next whole point.
     */
    public static float chargeDockedSteam(ItemStack stack, float chargerPoints) {
        float stored = steamStored(stack);
        int filled = ChargerTransfer.wholePointsAfterCharge(stored, STEAM_CAPACITY,
                CHARGER_POINTS_PER_STEAM_POINT, chargerPoints);
        if (filled <= stored) return 0F;
        setSteam(stack, filled);
        StrataDrillAssembly.identifyAfterRefill(stack);
        return ChargerTransfer.stationDebit(stored, filled, CHARGER_POINTS_PER_STEAM_POINT);
    }

    /** Non-mutating read, independent of the steam reserve and currently installed drive. */
    public static int dieselStored(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Mth.clamp(tag.getInt(DIESEL_KEY), 0, DIESEL_CAPACITY);
    }

    private static void setDiesel(ItemStack stack, int value) {
        if (value > 0) stack.getOrCreateTag().putInt(DIESEL_KEY, value);
        else stack.removeTagKey(DIESEL_KEY);
    }

    private static boolean isBiodiesel(FluidStack resource) {
        return !resource.isEmpty() && resource.getFluid().is(IETags.fluidBiodiesel);
    }

    /** Shared accounting for a valid diesel drive, regardless of which machine supplies it. */
    private static int fillDiesel(ItemStack stack, FluidStack resource, FluidAction action) {
        if (!isBiodiesel(resource)) return 0;
        int stored = dieselStored(stack);
        int accepted = Math.min(DIESEL_CAPACITY - stored, resource.getAmount());
        if (accepted > 0 && action.execute()) {
            StrataDrillAssembly.identifyAfterRefill(stack);
            setDiesel(stack, stored + accepted);
        }
        return accepted;
    }

    /** Pull biodiesel from the clicked face into the same held drill. No reverse transfer. */
    public static InteractionResult refuelDiesel(ItemStack stack, UseOnContext context) {
        if (context.getPlayer() == null || !hasDrive(stack, false)) return InteractionResult.PASS;

        int space = DIESEL_CAPACITY - dieselStored(stack);
        if (space == 0) return InteractionResult.PASS;

        Level level = context.getLevel();
        IFluidHandler source = FluidUtil.getFluidHandler(level, context.getClickedPos(), context.getClickedFace()).orElse(null);
        if (source == null) return InteractionResult.PASS;
        for (int tank = 0; tank < source.getTanks(); ++tank) {
            FluidStack candidate = source.getFluidInTank(tank);
            if (!isBiodiesel(candidate)) continue;

            // Suppress an offhand interaction when a refill is expected, without predicting a
            // drain or a swing. The server decides whether this face allows extraction.
            if (level.isClientSide) return InteractionResult.CONSUME_PARTIAL;

            FluidStack request = new FluidStack(candidate, space);
            FluidStack simulated = source.drain(request, FluidAction.SIMULATE);
            if (simulated.isEmpty() || !request.isFluidEqual(simulated)) continue;

            FluidStack drained = source.drain(simulated, FluidAction.EXECUTE);
            if (drained.isEmpty() || !request.isFluidEqual(drained)) return InteractionResult.PASS;

            return fillDiesel(stack, drained, FluidAction.EXECUTE) > 0
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    /** Require a valid Tetra assembly and, outside Creative, one block's worth of active fuel. */
    public static boolean canRun(ItemStack stack, boolean creative) {
        return poweredAssembly(stack, creative) != null;
    }

    /** Resolve once at the caller's validation boundary; no persistent or mutable-stack cache. */
    @Nullable
    public static StrataDrillAssembly poweredAssembly(ItemStack stack, boolean creative) {
        return poweredAssembly(stack, creative, false);
    }

    /** Check affordability before the server attempts an extra cell from an already paid center. */
    @Nullable
    public static StrataDrillAssembly poweredWheelNeighbor(ItemStack stack) {
        return poweredAssembly(stack, false, true);
    }

    /** Cheap dry-path rejection only; the installed drive and durability are checked separately. */
    static boolean hasAnyAffordableReserve(ItemStack stack) {
        return affordableFuel(stack, false) != 0;
    }

    @Nullable
    private static StrataDrillAssembly poweredAssembly(ItemStack stack, boolean creative, boolean wheelNeighbor) {
        if (!(stack.getItem() instanceof StrataDrillAssembly.ItemView drill)) return null;
        if (creative) return drill.resolveAssembly(stack, true);
        int affordable = affordableFuel(stack, wheelNeighbor);
        if (affordable == 0) return null;
        StrataDrillAssembly assembly = drill.resolveAssembly(stack, true);
        return assembly != null && (affordable & (assembly.steam() ? AFFORDABLE_STEAM : AFFORDABLE_DIESEL)) != 0
                ? assembly : null;
    }

    private static int affordableFuel(ItemStack stack, boolean wheelNeighbor) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        float steam = tag.getFloat(STEAM_KEY);
        int diesel = tag.getInt(DIESEL_KEY);
        boolean finiteSteam = Float.isFinite(steam);
        boolean hasSteam = finiteSteam && steam >= STEAM_PER_BLOCK;
        boolean hasDiesel = diesel >= DIESEL_PER_BLOCK;
        boolean steamAtNeighborPrice = !hasSteam && finiteSteam && steam >= STEAM_PER_WHEEL_NEIGHBOR;
        boolean dieselAtNeighborPrice = !hasDiesel && diesel >= DIESEL_PER_WHEEL_NEIGHBOR;
        // Only a reserve between the two prices needs to look up the scoped native call.
        if ((steamAtNeighborPrice || dieselAtNeighborPrice) && (wheelNeighbor || isScopedWheelNeighbor(stack))) {
            hasSteam |= steamAtNeighborPrice;
            hasDiesel |= dieselAtNeighborPrice;
        }
        return (hasSteam ? AFFORDABLE_STEAM : 0) | (hasDiesel ? AFFORDABLE_DIESEL : 0);
    }

    /** Bind one authorized synchronous wheel batch; the caller restores any enclosing batch in finally. */
    @Nullable
    public static StrataDrillWorkAccess enterWheelNeighborScope(StrataDrillWorkAccess current) {
        StrataDrillWorkAccess previous = WHEEL_NEIGHBOR.get();
        WHEEL_NEIGHBOR.set(current);
        return previous;
    }

    public static void restoreWheelNeighborScope(@Nullable StrataDrillWorkAccess previous) {
        if (previous == null) WHEEL_NEIGHBOR.remove();
        else WHEEL_NEIGHBOR.set(previous);
    }

    private static boolean isScopedWheelNeighbor(ItemStack stack) {
        StrataDrillWorkAccess scope = WHEEL_NEIGHBOR.get();
        return scope != null && scope.fh$isDiscountedWheelStack(stack);
    }

    /** Price picked before wear but only charged if the corresponding block is actually removed. */
    public record Removal(ItemStack stack, BlockPos pos, boolean steam, boolean wheelNeighbor, int wheelDepth,
                          @Nullable Removal previous) {}

    /** Called only after Forge's actual block removal succeeds, even if that hit wore out the tool. */
    public static void spend(Removal removal) {
        ItemStack stack = removal.stack();
        if (removal.steam()) {
            setSteam(stack, steamStored(stack) - (removal.wheelNeighbor() ? STEAM_PER_WHEEL_NEIGHBOR : STEAM_PER_BLOCK));
        } else {
            setDiesel(stack, dieselStored(stack) - (removal.wheelNeighbor() ? DIESEL_PER_WHEEL_NEIGHBOR : DIESEL_PER_BLOCK));
        }
    }

    /** Diesel still uses Forge fluid filling; the FH dock is the only steam charging path. */
    public static ICapabilityProvider makeCapabilities(ItemStack stack) {
        return new ICapabilityProvider() {
            private final LazyOptional<IFluidHandlerItem> diesel = LazyOptional.of(() -> new DieselFill(stack));

            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
                if (capability == ForgeCapabilities.FLUID_HANDLER_ITEM && hasDrive(stack, false)) {
                    return diesel.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    private static boolean hasDrive(ItemStack stack, boolean steam) {
        StrataDrillAssembly assembly = StrataDrillAssembly.operable(stack);
        return assembly != null && assembly.steam() == steam;
    }

    /** A standard Forge tank for machines such as Create's Spout, not a portable fuel source. */
    private record DieselFill(ItemStack container) implements IFluidHandlerItem {
        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0 || !hasDrive(container, false)) return FluidStack.EMPTY;
            int amount = dieselStored(container);
            return amount > 0 ? new FluidStack(IEFluids.BIODIESEL.getStill(), amount) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 && hasDrive(container, false) ? DIESEL_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack resource) {
            return tank == 0 && hasDrive(container, false) && isBiodiesel(resource);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return hasDrive(container, false) ? fillDiesel(container, resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
