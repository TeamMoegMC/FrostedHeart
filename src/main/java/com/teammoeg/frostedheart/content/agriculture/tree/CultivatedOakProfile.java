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
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;

/** Reloadable prototype profile. Vanilla tree configuration is decoded by Minecraft's codec. */
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class CultivatedOakProfile extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation ID = new ResourceLocation(FHMain.MODID, "oak");
    private static CultivatedOakProfile instance;
    private Profile profile;

    private CultivatedOakProfile() { super(new Gson(), "cultivated_trees"); }

    @SubscribeEvent
    public static void reloadListener(AddReloadListenerEvent event) {
        instance = new CultivatedOakProfile();
        event.addListener(instance);
    }

    public static Profile get() { return instance == null ? null : instance.profile; }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        profile = null;
        if (!files.containsKey(ID)) return;
        try {
            profile = parse(files.get(ID).getAsJsonObject());
        } catch (RuntimeException ex) {
            FHMain.LOGGER.error("Cannot load cultivated oak profile {}; saplings will wait for a corrected reload", ID, ex);
        }
    }

    /** Parses the supported straight oak shape and its cultivation rules. */
    public static Profile parse(JsonObject json) {
        TreeConfiguration tree = TreeConfiguration.CODEC.parse(JsonOps.INSTANCE, json.get("tree"))
                .getOrThrow(false, message -> FHMain.LOGGER.error(message));
        if (!(tree.trunkPlacer instanceof StraightTrunkPlacer) || !(tree.foliagePlacer instanceof BlobFoliagePlacer)
                || !tree.decorators.isEmpty())
            throw new IllegalArgumentException("Prototype supports straight trunks, blob foliage and no decorators");
        JsonObject g = json.getAsJsonObject("growth"), y = json.getAsJsonObject("yield"), t = json.getAsJsonObject("temperature");
        Profile result = new Profile(tree,
                GsonHelper.getAsInt(g, "progress_per_stage"), GsonHelper.getAsInt(g, "min_light"),
                GsonHelper.getAsFloat(y, "base_logs"), GsonHelper.getAsFloat(y, "logs_per_height"),
                GsonHelper.getAsFloat(y, "fixed_logs"), GsonHelper.getAsInt(y, "min_height"), GsonHelper.getAsInt(y, "max_height"),
                GsonHelper.getAsFloat(t, "min"), GsonHelper.getAsFloat(t, "ideal"), GsonHelper.getAsFloat(t, "max"),
                GsonHelper.getAsFloat(t, "minimum_yield_factor"), GsonHelper.getAsFloat(t, "preserved_degrees_per_grade"),
                GsonHelper.getAsInt(json.getAsJsonObject("tree").getAsJsonObject("foliage_placer"), "radius"),
                GsonHelper.getAsInt(json.getAsJsonObject("tree").getAsJsonObject("foliage_placer"), "offset"));
        if (result.clearanceRadius < 0 || result.clearanceRadius > 8 || result.canopyOffset < 0 || result.canopyOffset > 8
                || result.progressPerStage < 1 || result.minLight < 0 || result.minLight > 15
                || !(result.baseLogs > 0) || !(result.logsPerHeight > 0) || !Float.isFinite(result.fixedLogs)
                || !Float.isFinite(result.baseLogs) || !Float.isFinite(result.logsPerHeight)
                || result.minHeight < 4 || result.maxHeight > 24 || result.maxHeight < result.minHeight
                || !(result.minTemp < result.idealTemp && result.idealTemp < result.maxTemp)
                || !Float.isFinite(result.minTemp) || !Float.isFinite(result.maxTemp)
                || !(result.minimumYieldFactor >= 0 && result.minimumYieldFactor <= 1)
                || !(result.preservedDegrees >= 0 && Float.isFinite(result.preservedDegrees)))
            throw new IllegalArgumentException("Invalid cultivated oak growth/yield/temperature ranges");
        return result;
    }

    /** Height = ceil((target logs - fixed logs) / logs per height), clamped to the cultivation range. */
    public record Profile(TreeConfiguration tree, int progressPerStage, int minLight,
            float baseLogs, float logsPerHeight, float fixedLogs, int minHeight, int maxHeight,
            float minTemp, float idealTemp, float maxTemp, float minimumYieldFactor, float preservedDegrees,
            int clearanceRadius, int canopyOffset) {
        public int height(double logs) {
            return Mth.clamp((int)Math.ceil((logs - fixedLogs) / logsPerHeight), minHeight, maxHeight);
        }
        public double suitability(float temperature, float coldProtection) {
            float min = minTemp - coldProtection;
            if (temperature < min || temperature > maxTemp) return -1;
            return temperature <= idealTemp ? (temperature - min) / (idealTemp - min)
                    : (maxTemp - temperature) / (maxTemp - idealTemp);
        }
        public TreeConfiguration atHeight(int height) {
            return new TreeConfiguration.TreeConfigurationBuilder(tree.trunkProvider,
                    new StraightTrunkPlacer(height, 0, 0), tree.foliageProvider, tree.foliagePlacer, tree.minimumSize)
                    .dirt(tree.dirtProvider).ignoreVines().build();
        }
    }
}
