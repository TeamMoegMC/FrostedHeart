package com.teammoeg.frostedresearch.knowledge.furnace;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedresearch.FRContents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public final class ControlledFurnaceBlockEntity extends CBlockEntity implements CTickableBlockEntity, MenuProvider {
    public static final int SAMPLE_SLOTS = 6;
    public static final int FUEL_SLOT = SAMPLE_SLOTS;
    public static final int SLOT_COUNT = SAMPLE_SLOTS + 1;
    public static final int MIN_TEMPERATURE = 600;
    public static final int MAX_TEMPERATURE = 1400;
    public static final int MAX_TIMER_SECONDS = 24 * 60 * 60;
    private static final int TICKS_PER_SECOND = 20;

    private record Firing(int slot, int duration, ItemStack product, int durationMax, ItemStack overfireResult) {}

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override public int getSlotLimit(int slot) { return slot == FUEL_SLOT ? 64 : 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return !running && (slot != FUEL_SLOT || stack.is(Items.CHARCOAL));
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return running ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final List<ItemStack> migratedItems = new ArrayList<>();
    private List<Firing> firing = List.of();
    private UUID team;
    private boolean running;
    private int temperature = 1000;
    private long elapsedTicks;
    private int limitTicks;
    private String message = "idle";

    public ControlledFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(FRContents.BlockEntityTypes.CONTROLLED_FURNACE.get(), pos, state);
    }

    public ItemStackHandler inventory() { return inventory; }
    public UUID team() { return team; }
    public boolean running() { return running; }
    public int temperature() { return temperature; }
    public long elapsedSeconds() { return elapsedTicks / TICKS_PER_SECOND; }
    public boolean timed() { return limitTicks > 0; }
    public int timerSeconds() { return (int) Math.max(0, (limitTicks - elapsedTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND); }
    public String message() { return message; }

    public boolean claim(ServerPlayer player) {
        UUID current = CTeamDataManager.get(player).getId();
        if (team == null) {
            team = current;
            setChanged();
        }
        return team.equals(current);
    }

    public void start(int setTemperature, int seconds) {
        if (running || setTemperature < MIN_TEMPERATURE || setTemperature > MAX_TEMPERATURE
                || seconds < 0 || seconds > MAX_TIMER_SECONDS || level == null || level.isClientSide) return;
        temperature = setTemperature;
        var recipes = level.getRecipeManager().getAllRecipesFor(FRContents.Recipes.CONTROLLED_FIRING.get()).stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString())).toList();
        List<Firing> results = new ArrayList<>();
        boolean loaded = false;
        for (int slot = 0; slot < SAMPLE_SLOTS; slot++) {
            ItemStack sample = inventory.getStackInSlot(slot);
            if (sample.isEmpty()) continue;
            loaded = true;
            for (var recipe : recipes) {
                if (!recipe.matches(sample)) continue;
                results.add(new Firing(slot, recipe.duration(), recipe.outcome(temperature),
                        recipe.durationMax(), recipe.overfireResult()));
                break;
            }
        }
        if (!loaded) {
            update("need_samples");
            return;
        }
        if (!inventory.getStackInSlot(FUEL_SLOT).is(Items.CHARCOAL)) {
            update("need_fuel");
            return;
        }
        inventory.extractItem(FUEL_SLOT, 1, false);
        firing = results;
        elapsedTicks = 0;
        limitTicks = seconds * TICKS_PER_SECOND;
        running = true;
        update("running");
    }

    public void stop() {
        if (!running) return;
        for (Firing sample : firing) {
            if (elapsedTicks <= sample.duration()) continue;
            boolean overfired = sample.durationMax() > 0 && elapsedTicks > sample.durationMax();
            inventory.setStackInSlot(sample.slot(), overfired ? sample.overfireResult() : sample.product());
        }
        firing = List.of();
        running = false;
        update("stopped");
    }

    @Override public void tick() {
        if (!running || level == null || level.isClientSide) return;
        elapsedTicks++;
        setChanged();
        if (timed() && elapsedTicks >= limitTicks) stop();
        else if (elapsedTicks % TICKS_PER_SECOND == 0) syncData();
    }

    private void update(String status) {
        message = status;
        setChanged();
        syncData();
    }

    /** Older furnaces can hold more items than the new specimen slots. */
    void returnMigratedItems(ServerPlayer player) {
        if (migratedItems.isEmpty()) return;
        migratedItems.forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
        migratedItems.clear();
        setChanged();
    }

    public List<ItemStack> settle() {
        List<ItemStack> drops = new ArrayList<>(migratedItems);
        migratedItems.clear();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) drops.add(stack);
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        firing = List.of();
        running = false;
        return drops;
    }

    @Override public void writeCustomNBT(CompoundTag data, boolean description) {
        if (team != null) data.putUUID("team", team);
        data.putInt("temperature", temperature);
        data.putBoolean("running", running);
        data.putLong("elapsed_ticks", elapsedTicks);
        data.putInt("limit_ticks", limitTicks);
        data.putString("message", message);
        if (description) return;
        data.put("specimen_inventory", inventory.serializeNBT());
        ListTag samples = new ListTag();
        for (Firing sample : firing) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", sample.slot());
            entry.putInt("duration", sample.duration());
            entry.put("product", sample.product().save(new CompoundTag()));
            if (sample.durationMax() > 0) {
                entry.putInt("duration_max", sample.durationMax());
                entry.put("overfire_result", sample.overfireResult().save(new CompoundTag()));
            }
            samples.add(entry);
        }
        data.put("firing", samples);
        ListTag surplus = new ListTag();
        migratedItems.forEach(stack -> surplus.add(stack.save(new CompoundTag())));
        data.put("migrated_items", surplus);
    }

    @Override public void readCustomNBT(CompoundTag data, boolean description) {
        team = data.hasUUID("team") ? data.getUUID("team") : null;
        int savedTemperature = data.contains("temperature") ? data.getInt("temperature") : 1000;
        temperature = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, savedTemperature));
        running = data.getBoolean("running");
        elapsedTicks = data.getLong("elapsed_ticks");
        limitTicks = data.getInt("limit_ticks");
        message = data.contains("message") ? data.getString("message") : "idle";
        if (description) return;
        firing = new ArrayList<>();
        migratedItems.clear();
        if (!data.contains("specimen_inventory")) {
            migrate(data);
            return;
        }
        inventory.deserializeNBT(data.getCompound("specimen_inventory"));
        for (Tag tag : data.getList("firing", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            firing.add(new Firing(entry.getInt("slot"), entry.getInt("duration"), ItemStack.of(entry.getCompound("product")),
                    entry.getInt("duration_max"), ItemStack.of(entry.getCompound("overfire_result"))));
        }
        for (Tag tag : data.getList("migrated_items", Tag.TAG_COMPOUND)) migratedItems.add(ItemStack.of((CompoundTag) tag));
    }

    private void migrate(CompoundTag data) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) inventory.setStackInSlot(slot, ItemStack.EMPTY);
        running = false;
        message = "idle";
        ItemStackHandler old = new ItemStackHandler(3);
        if (data.contains("inventory")) old.deserializeNBT(data.getCompound("inventory"));
        ItemStack fuel = old.getStackInSlot(1);
        if (fuel.is(Items.CHARCOAL)) inventory.setStackInSlot(FUEL_SLOT, fuel);
        else if (!fuel.isEmpty()) migratedItems.add(fuel);
        int slot = 0;
        for (ItemStack stack : List.of(old.getStackInSlot(2), ItemStack.of(data.getCompound("pending")),
                ItemStack.of(data.getCompound("refund")), old.getStackInSlot(0))) {
            while (!stack.isEmpty() && slot < SAMPLE_SLOTS) inventory.setStackInSlot(slot++, stack.split(1));
            if (!stack.isEmpty()) migratedItems.add(stack);
        }
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ControlledFurnaceMenu(id, inventory, this);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.frostedresearch.controlled_furnace"); }
}
