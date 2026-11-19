package com.teammoeg.frostedheart.content.agriculture;

import java.util.*;

import com.teammoeg.frostedheart.content.climate.data.PlantTempData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Resolves multi-block crops and accounts fertilizer only after successful growth. */
@Mod.EventBusSubscriber(modid = "frostedheart")
public final class CropFertilization {
    public static final TagKey<Block> VERTICAL = TagKey.create(Registries.BLOCK,
        new ResourceLocation("frostedheart", "vertical_fertilizable_crops"));
    private static final Map<ServerLevel, Set<BlockPos>> REMOVED = new WeakHashMap<>();
    private static final Map<ServerLevel, Snapshot> PENDING = new WeakHashMap<>();
    private record Snapshot(BlockPos pos, BlockState state, BlockState above, BlockPos soil, BlockState fertilizer) {}

    /** Only tagged, identical vertical plant blocks may share a soil position. */
    public static BlockPos soil(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos root = pos;
        if (state.is(VERTICAL))
            while (root.getY() > level.getMinBuildHeight() && level.getBlockState(root.below()).is(state.getBlock()))
                root = root.below();
        return root.below();
    }

    public static boolean fertilized(BlockState state) {
        return state.hasProperty(FertilizedDirt.FERTILIZER) && state.hasProperty(FertilizedDirt.STORAGE);
    }

    /** Preserved fertilizer lowers only the three minimum temperatures, in degrees Celsius. */
    public static PlantTempData temperatureData(PlantTempData data, BlockState soil) {
        if (!fertilized(soil) || soil.getValue(FertilizedDirt.FERTILIZER) != Fertilizer.FertilizerType.PRESERVED)
            return data;
        int grade = soil.hasProperty(FertilizedDirt.GRADE) ? soil.getValue(FertilizedDirt.GRADE).preserve : 1;
        int reduction = 5 * grade;
        return new PlantTempData(data.block(), data.growTimeDays(), data.minFertilize() - reduction,
                data.minGrow() - reduction, data.minSurvive() - reduction, data.maxFertilize(), data.maxGrow(),
                data.maxSurvive(), data.snowVulnerable(), data.blizzardVulnerable(), data.dead(), data.willDie(),
                data.heatCapacity(), data.minSkylight(), data.maxSkylight());
    }

    /** Snapshot before an attempt; a denied attempt never charges fertilizer. */
    public static void before(ServerLevel level, BlockPos pos) {
        BlockPos soil = soil(level, pos);
        PENDING.put(level, new Snapshot(pos.immutable(), level.getBlockState(pos),
            level.getBlockState(pos.above()), soil, level.getBlockState(soil)));
    }

    @SubscribeEvent
    public static void after(BlockEvent.CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Snapshot old = PENDING.remove(level);
        if (old == null || !event.getPos().equals(old.pos) && !event.getPos().equals(old.pos.above())) return;
        BlockState now = level.getBlockState(old.pos);
        BlockState above = level.getBlockState(old.pos.above());
        boolean extended = above.is(old.state.getBlock()) && !old.above.is(old.state.getBlock());
        int delta = 0;
        int maximum = 1;
        if (old.state.getBlock() instanceof CropBlock crop && now.is(crop)) {
            delta = Math.max(0, crop.getAge(now) - crop.getAge(old.state));
            maximum = crop.getMaxAge();
        } else if (old.state.is(Blocks.SUGAR_CANE) && now.is(Blocks.SUGAR_CANE)) {
            delta = Math.max(0, now.getValue(SugarCaneBlock.AGE) - old.state.getValue(SugarCaneBlock.AGE));
            maximum = 16;
        }
        if (delta == 0 && !extended) return;
        boolean fed = fertilized(old.fertilizer);
        Fertilizer.FertilizerType type = fed ? old.fertilizer.getValue(FertilizedDirt.FERTILIZER) : null;
        if (type == Fertilizer.FertilizerType.INCREASING || extended && old.state.is(Blocks.SUGAR_CANE)) {
            double bonus = type == Fertilizer.FertilizerType.INCREASING ? old.fertilizer.getValue(FertilizedDirt.GRADE).productivity - 1 : 0;
            Records records = records(level);
            if (old.state.is(Blocks.SUGAR_CANE) && extended) {
                double accumulated = records.take(old.pos, old.state);
                records.add(old.pos.above(), above, accumulated + bonus / maximum);
            } else {
                if (delta > 0) records.add(old.pos, now, bonus * delta / maximum);
                if (extended) records.add(old.pos.above(), above, bonus);
            }
        }
        // Preserve the established average charge, but only for a real transition.
        if (fed && level.random.nextInt(8) == 0 && level.getBlockState(old.soil).equals(old.fertilizer)) {
            int remaining = old.fertilizer.getValue(FertilizedDirt.STORAGE) - 1;
            BlockState next = remaining > 0 ? old.fertilizer.setValue(FertilizedDirt.STORAGE, remaining)
                : old.fertilizer.getBlock() instanceof FarmBlock
                    ? Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, old.fertilizer.getValue(FarmBlock.MOISTURE))
                    : Blocks.DIRT.defaultBlockState();
            level.setBlock(old.soil, next, 2);
        }
    }

    /** Uses Chorda's existing common loot event so other drop listeners keep working. */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void drops(com.teammoeg.chorda.events.BlockDropsEvent event) {
        List<ItemStack> result = harvest(event.getLevel(), event.getPos(), event.getState(), event.getDrops());
        if (result != event.getDrops()) {
            event.getDrops().clear();
            event.getDrops().addAll(result);
        }
    }

    /** Applies accrued growth credit through the common block loot path, including Create. */
    public static List<ItemStack> harvest(ServerLevel level, BlockPos pos, BlockState state, List<ItemStack> drops) {
        double bonus = records(level).peek(pos, state);
        boolean mature = !(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state);
        if (!mature || bonus <= 0) return drops;
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : drops) {
            double extra = stack.getCount() * bonus;
            int count = stack.getCount() + (int) extra + (level.random.nextDouble() < extra % 1 ? 1 : 0);
            while (count > 0) {
                ItemStack copy = stack.copy();
                copy.setCount(Math.min(count, copy.getMaxStackSize()));
                result.add(copy);
                count -= copy.getCount();
            }
        }
        return result;
    }

    /** Invalidates replacement/reset crops; removal waits until vanilla has generated its drops. */
    public static void changed(ServerLevel level, BlockPos pos, BlockState previous, BlockState state) {
        boolean wasCrop = previous.getBlock() instanceof CropBlock || previous.is(Blocks.SUGAR_CANE);
        boolean isCrop = state.getBlock() instanceof CropBlock || state.is(Blocks.SUGAR_CANE);
        if (!wasCrop && !isCrop) return;
        if (isCrop && (!state.is(previous.getBlock()) || state.getBlock() instanceof CropBlock crop
                && crop.getAge(state) < crop.getAge(previous))) {
            records(level).take(pos, state);
        }
        if (wasCrop && !state.is(previous.getBlock()))
            REMOVED.computeIfAbsent(level, key -> new HashSet<>()).add(pos.immutable());
    }

    @SubscribeEvent
    public static void endTick(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        Set<BlockPos> removed = REMOVED.remove(level);
        if (removed != null) for (BlockPos pos : removed) records(level).take(pos, level.getBlockState(pos));
        PENDING.remove(level);
    }

    @SubscribeEvent
    public static void planted(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level)
            records(level).take(event.getPos(), event.getPlacedBlock());
    }

    private static Records records(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Records::load, Records::new, "frostedheart_crop_fertilization");
    }

    /** Persistent credit belongs to the growing crop position, never to newly applied soil. */
    public static final class Records extends SavedData {
        private final Map<Long, CompoundTag> entries = new HashMap<>();
        static Records load(CompoundTag tag) {
            Records data = new Records();
            for (var entry : tag.getList("crops", 10)) {
                CompoundTag crop = (CompoundTag) entry;
                data.entries.put(crop.getLong("pos"), crop);
            }
            return data;
        }
        void add(BlockPos pos, BlockState state, double bonus) {
            double previous = take(pos, state);
            CompoundTag tag = new CompoundTag();
            tag.putLong("pos", pos.asLong());
            tag.putString("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            tag.putDouble("bonus", previous + bonus);
            entries.put(pos.asLong(), tag);
            setDirty();
        }
        double peek(BlockPos pos, BlockState state) {
            CompoundTag entry = entries.get(pos.asLong());
            return entry != null && entry.getString("block").equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
                ? entry.getDouble("bonus") : 0;
        }
        double take(BlockPos pos, BlockState state) {
            CompoundTag entry = entries.remove(pos.asLong());
            if (entry == null) return 0;
            setDirty();
            return entry.getString("block").equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
                ? entry.getDouble("bonus") : 0;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            entries.values().forEach(list::add);
            tag.put("crops", list);
            return tag;
        }
    }
}
