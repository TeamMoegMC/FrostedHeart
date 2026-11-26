package com.teammoeg.frostedheart.content.agriculture.tree;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;

/** Reloadable cultivation profiles resolving complete species configurations from the vanilla registry. */
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class CultivatedOakProfile extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "oak");
    private static CultivatedOakProfile instance;
    private final java.util.Map<ResourceLocation, Profile> profiles = new java.util.HashMap<>();

    private CultivatedOakProfile() { super(new Gson(), "cultivated_trees"); }

    @SubscribeEvent
    public static void reloadListener(AddReloadListenerEvent event) {
        instance = new CultivatedOakProfile();
        event.addListener(instance);
    }

    public static Profile get() { return get(ID); }
    public static Profile get(ResourceLocation id) { return instance == null ? null : instance.profiles.get(id); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        profiles.clear();
        files.forEach((id, json) -> {
            try { profiles.put(id, parse(json.getAsJsonObject())); }
            catch (RuntimeException ex) {
                FHMain.LOGGER.error("Cannot load cultivated tree {}; saplings will wait for a corrected reload", id, ex);
            }
        });
    }

    /** Parses embedded oak or one of the supported vanilla tree cultivation profiles. */
    public static Profile parse(JsonObject json) {
        ResourceLocation vanilla = json.has("vanilla_tree")
                ? new ResourceLocation(GsonHelper.getAsString(json, "vanilla_tree")) : null;
        TreeConfiguration tree = vanilla == null ? TreeConfiguration.CODEC.parse(JsonOps.INSTANCE, json.get("tree"))
                .getOrThrow(false, message -> FHMain.LOGGER.error(message)) : null;
        if (tree != null && (!(tree.trunkPlacer instanceof StraightTrunkPlacer)
                || !(tree.foliagePlacer instanceof BlobFoliagePlacer) || !tree.decorators.isEmpty()))
            throw new IllegalArgumentException("Prototype supports straight trunks, blob foliage and no decorators");
        JsonObject g = json.getAsJsonObject("growth"), y = json.getAsJsonObject("yield"), t = json.getAsJsonObject("temperature");
        String species = vanilla == null ? "oak" : vanilla.getPath();
        java.util.List<ResourceLocation> giants;
        int vanillaMax;
        if (vanilla != null && !vanilla.getNamespace().equals("minecraft"))
            throw new IllegalArgumentException("Expected a supported vanilla tree configuration");
        switch (species) {
            case "oak" -> { giants = java.util.List.of(); vanillaMax = 6; }
            case "spruce" -> { giants = java.util.List.of(new ResourceLocation("minecraft:mega_spruce"),
                    new ResourceLocation("minecraft:mega_pine")); vanillaMax = 8; }
            case "jungle_tree_no_vine" -> { giants = java.util.List.of(new ResourceLocation("minecraft:mega_jungle_tree")); vanillaMax = 12; }
            case "dark_oak" -> { giants = java.util.List.of(new ResourceLocation("minecraft:dark_oak")); vanillaMax = 9; }
            case "birch" -> { giants = java.util.List.of(); vanillaMax = 7; }
            case "acacia" -> { giants = java.util.List.of(); vanillaMax = 9; }
            case "mangrove" -> { giants = java.util.List.of(); vanillaMax = 12; }
            case "cherry" -> { giants = java.util.List.of(); vanillaMax = 8; }
            default -> throw new IllegalArgumentException("Unsupported cultivated tree " + vanilla);
        }
        Profile result = new Profile(tree, vanilla, giants, species.equals("oak"), species.equals("dark_oak"),
                GsonHelper.getAsInt(g, "progress_per_stage"), GsonHelper.getAsInt(g, "min_light"),
                GsonHelper.getAsFloat(g, "branch_probability", .35f),
                vanilla != null ? vanillaMax : GsonHelper.getAsInt(json.getAsJsonObject("tree").getAsJsonObject("trunk_placer"), "base_height")
                + GsonHelper.getAsInt(json.getAsJsonObject("tree").getAsJsonObject("trunk_placer"), "height_rand_a")
                + GsonHelper.getAsInt(json.getAsJsonObject("tree").getAsJsonObject("trunk_placer"), "height_rand_b"),
                GsonHelper.getAsFloat(y, "base_logs"), GsonHelper.getAsFloat(y, "logs_per_height"),
                GsonHelper.getAsFloat(y, "fixed_logs"), GsonHelper.getAsInt(y, "min_height"), GsonHelper.getAsInt(y, "max_height"),
                GsonHelper.getAsFloat(t, "min"), GsonHelper.getAsFloat(t, "ideal_min", 15), GsonHelper.getAsFloat(t, "ideal_max", 25), GsonHelper.getAsFloat(t, "max"),
                GsonHelper.getAsFloat(t, "minimum_yield_factor"), GsonHelper.getAsFloat(t, "preserved_degrees_per_grade"),
                vanilla != null ? 2 : GsonHelper.getAsInt(json.getAsJsonObject("tree").getAsJsonObject("foliage_placer"), "radius"),
                vanilla != null ? 0 : GsonHelper.getAsInt(json.getAsJsonObject("tree").getAsJsonObject("foliage_placer"), "offset"));
        if (!(result.branchProbability >= 0 && result.branchProbability <= 1) || result.clearanceRadius < 0 || result.clearanceRadius > 8 || result.canopyOffset < 0 || result.canopyOffset > 8
                || result.progressPerStage < 1 || result.progressPerStage > 127 || result.minLight < 0 || result.minLight > 15
                || !(result.baseLogs > 0) || !(result.logsPerHeight > 0) || !Float.isFinite(result.fixedLogs)
                || !Float.isFinite(result.baseLogs) || !Float.isFinite(result.logsPerHeight)
                || result.minHeight < 4 || result.maxHeight > 24 || result.maxHeight < result.minHeight
                || !(result.minTemp < result.idealMin && result.idealMin <= result.idealMax && result.idealMax < result.maxTemp)
                || !Float.isFinite(result.minTemp) || !Float.isFinite(result.maxTemp)
                || !(result.minimumYieldFactor >= 0 && result.minimumYieldFactor <= 1)
                || !(result.preservedDegrees >= 0 && Float.isFinite(result.preservedDegrees)))
            throw new IllegalArgumentException("Invalid cultivated tree growth/yield/temperature ranges");
        return result;
    }

    /** The earned-height value is now a per-sapling wood expectation, not a physical trunk height. */
    public record Profile(TreeConfiguration tree, ResourceLocation vanillaTree, java.util.List<ResourceLocation> giantTrees,
            boolean branches, boolean giantOnly, int progressPerStage, int minLight, float branchProbability, int vanillaMaxHeight,
            float baseLogs, float logsPerHeight, float fixedLogs, int minHeight, int maxHeight,
            float minTemp, float idealMin, float idealMax, float maxTemp, float minimumYieldFactor, float preservedDegrees,
            int clearanceRadius, int canopyOffset) {
        public int height(double logs) {
            return Math.max(1, (int)Math.ceil((logs - fixedLogs) / logsPerHeight));
        }
        /** Temperature yield factor; each outer band has the unprotected growing-range width. */
        public double suitability(float temperature, float coldProtection) {
            float min = minTemp - coldProtection;
            double width = maxTemp - minTemp;
            if (temperature < min) return minimumYieldFactor * Math.max(0, 1 - (min - temperature) / width);
            if (temperature > maxTemp) return minimumYieldFactor * Math.max(0, 1 - (temperature - maxTemp) / width);
            if (temperature < idealMin) return minimumYieldFactor + (1 - minimumYieldFactor) * (temperature - min) / (idealMin - min);
            if (temperature > idealMax) return minimumYieldFactor + (1 - minimumYieldFactor) * (maxTemp - temperature) / (maxTemp - idealMax);
            return 1;
        }
        /** Temperature never changes growth speed; fertilizer acceleration is applied by the ledger. */
        public double speed(float temperature, float coldProtection) { return 1; }
        /** Calibrates the two datapack height references without clamping earned yield. */
        public int earnedHeight(double factor) {
            double height = factor <= minimumYieldFactor
                    ? minHeight * factor / Math.max(1e-9, minimumYieldFactor)
                    : minHeight + (maxHeight - minHeight) * (factor - minimumYieldFactor) / (1 - minimumYieldFactor);
            return Math.max(0, (int)Math.floor(height));
        }
        /** Complete cherry forks have a nonzero native minimum; keep earning credit rather than inventing a pole. */
        public int minimumBudget() {
            if (giantOnly) return maxHeight;
            return vanillaTree != null && vanillaTree.getPath().equals("cherry")
                    ? (int)Math.ceil(CultivatedTreeYield.minimumLogs("cherry")) : 1;
        }
        /** Converts a per-sapling wood budget to a complete native tree configuration. */
        public TreeConfiguration atHeight(net.minecraft.server.level.ServerLevel level, net.minecraft.util.RandomSource random,
                int budget, boolean giant) {
            return atHeight(level, random, budget, giant, null);
        }
        /** Origin permits the native flower-dependent bee variants without querying unloaded positions. */
        public TreeConfiguration atHeight(net.minecraft.server.level.ServerLevel level, net.minecraft.util.RandomSource random,
                int budget, boolean giant, net.minecraft.core.BlockPos origin) {
            if (budget < minimumBudget() && !giant || budget < 1 || !giant && giantOnly || giant && giantTrees.isEmpty()) return null;
            String shape = vanillaTree == null ? "oak" : vanillaTree.getPath();
            ResourceLocation id = vanillaTree;
            if (giant) {
                id = giantTrees.get(random.nextInt(giantTrees.size()));
                shape = id.getPath();
            } else if (shape.equals("oak")) {
                // Above ordinary oak size, use real Fancy branches rather than synthetic horizontal logs.
                if (budget > vanillaMaxHeight || budget >= 4 && random.nextInt(10) == 0) shape = "fancy_oak";
                id = new ResourceLocation("minecraft", shape);
            } else if (shape.equals("mangrove") && random.nextFloat() < .85f) {
                shape = "tall_mangrove";
                id = new ResourceLocation("minecraft", shape);
            }
            if (!giant && origin != null && java.util.Set.of("oak", "fancy_oak", "birch", "cherry").contains(shape)
                    && flowersNearby(level, origin)) id = new ResourceLocation("minecraft", shape + "_bees_005");
            TreeConfiguration source = tree;
            if (id != null && !(tree != null && id.equals(new ResourceLocation("minecraft:oak")))) {
                var feature = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).get(id);
                if (feature == null || !(feature.config() instanceof TreeConfiguration configuration)) return null;
                source = configuration;
            }
            if (source == null) return null;
            double expectedLogs = (giant ? 4.0 : 1.0) * budget;
            int height = CultivatedTreeYield.height(shape, expectedLogs, random);
            var trunk = source.trunkPlacer;
            var builder = new TreeConfiguration.TreeConfigurationBuilder(source.trunkProvider,
                    new CultivatedNativeTrunkPlacer(trunk, height, giant), source.foliageProvider,
                    source.foliagePlacer, source.rootPlacer,
                    new net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize(0, 0, 0))
                    .dirt(source.dirtProvider).decorators(source.decorators);
            if (source.ignoreVines) builder.ignoreVines();
            if (source.forceDirt) builder.forceDirt();
            return builder.build();
        }
        private static boolean flowersNearby(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos origin) {
            for (var pos : net.minecraft.core.BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 1, 2)))
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(net.minecraft.tags.BlockTags.FLOWERS)) return true;
            return false;
        }
    }
}
