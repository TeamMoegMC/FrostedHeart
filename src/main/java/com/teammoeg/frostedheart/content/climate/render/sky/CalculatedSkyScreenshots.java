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

package com.teammoeg.frostedheart.content.climate.render.sky;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.ClimateType;
import com.teammoeg.frostedheart.content.climate.player.PlayerWeatherCompatibilityModel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.literal;

/**
 * Development harness that records and takes sets of screenshots for comparing the calculated sky.
 * /calculatedSky beginSet opens a set, /calculatedSky pose saves the current place, view, time, and weather into it,
 * and /calculatedSky endSet closes it. /calculatedSky time and /calculatedSky weather hold what the client shows while
 * framing, and /calculatedSky screenshots takes a set.
 * A set is a JSON file in the sky-shots folder of the game directory that lists poses, times of day, weather, and
 * variants of the sky config, either once or in groups. Every combination within a group becomes one screenshot,
 * ordered by pose, weather, time, and variant.
 * The time, weather, and view direction are held on the client only, so the server's clock and climate keep running.
 * Poses with a position or offset move the player through the integrated server.
 * Each shot waits until the sky is built for its config and sun elevation, the terrain is drawn after a move, and fog
 * has settled, then saves the frame and a line of manifest.tsv in a new folder under screenshots/sky-shots.
 * The config, view, weather, and position the run started with are restored when it ends.
 * Its subcommands are part of /calculatedSky, so nothing here runs while CalculatedSkyClientCommand.ENABLED is false.
 */
@OnlyIn(Dist.CLIENT)
public final class CalculatedSkyScreenshots {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FOLDER = "sky-shots";
    private static final double MINIMUM_SETTLE_SECONDS = 1.0;
    private static final double WEATHER_SETTLE_SECONDS = 4.0;
    private static final double MOVE_SETTLE_SECONDS = 2.0;
    private static final double TIMEOUT_SECONDS = 20.0;
    private static final double ARRIVAL_DISTANCE_SQUARED = 0.25;
    private static final Set<String> SET_KEYS = Set.of("poses", "times", "weather", "variants", "groups", "hud");
    private static final Set<String> GROUP_KEYS = Set.of("poses", "times", "weather", "variants");
    private static final Set<String> POSE_KEYS = Set.of("name", "yaw", "pitch", "position", "offset", "times",
            "sunElevations", "sunBranch", "weather");
    private static final Set<String> VARIANT_KEYS = Stream.concat(Stream.of("name", "sky"),
            Arrays.stream(SkySetting.values()).map(SkySetting::key)).collect(Collectors.toUnmodifiableSet());
    private static final String MANIFEST_HEADER = manifestHeader();

    private static final Consumer<TickEvent.RenderTickEvent> HOLD_LISTENER = CalculatedSkyScreenshots::onHoldTick;
    private static final Consumer<ClientPlayerNetworkEvent.LoggingOut> LOGOUT_LISTENER =
            event -> onLoggingOut();

    @Nullable
    private static Run activeRun;
    @Nullable
    private static Recording recording;
    private static boolean listening;
    private static boolean timeHeld;
    private static long heldTime;
    @Nullable
    private static Weather heldWeather;
    private static float rainLevelBeforeHold;
    private static float thunderLevelBeforeHold;

    private CalculatedSkyScreenshots() {
    }

    /**
     * Whether a run is taking shots.
     */
    public static boolean isRunning() {
        return activeRun != null;
    }

    private enum Weather {
        WORLD("world", 0.0F, 0.0F),
        CLEAR("clear", 0.0F, 0.0F),
        SNOW("snow", 1.0F, 0.0F),
        STORM("storm", 1.0F, 1.0F),
        // The levels the server sends a player in snow and in a blizzard:
        SNOWFALL("snowfall", PlayerWeatherCompatibilityModel.fromClimate(ClimateType.SNOW)),
        BLIZZARD("blizzard", PlayerWeatherCompatibilityModel.fromClimate(ClimateType.BLIZZARD));

        private final String key;
        private final float rainLevel;
        private final float thunderLevel;

        Weather(String key, float rainLevel, float thunderLevel) {
            this.key = key;
            this.rainLevel = rainLevel;
            this.thunderLevel = thunderLevel;
        }

        Weather(String key, PlayerWeatherCompatibilityModel.VanillaWeatherState state) {
            this(key, state.rainStrength(), state.thunderStrength());
        }

        @Nullable
        static Weather byKey(String key) {
            for (Weather weather : values()) {
                if (weather.key.equals(key)) {
                    return weather;
                }
            }
            return null;
        }
    }

    /**
     * A view direction and where to stand. Without a position the pose is taken where the run started, moved by the
     * offset when one is given. Times and weather of the pose replace those of its group.
     */
    private record Pose(String name, float yaw, float pitch, @Nullable Vec3 position, @Nullable Vec3 offset,
            List<Long> times, List<Weather> weathers) {
        Vec3 target(Vec3 startPosition) {
            if (position != null) {
                return position;
            }
            return offset != null ? startPosition.add(offset) : startPosition;
        }
    }

    private record Variant(String name, boolean sky, SkySettings settings) {
        static Variant fromConfig(String name) {
            return new Variant(name, FHConfig.CLIENT.calculatedSky.get(), SkySettings.fromConfig());
        }

        Variant withSky(String newName, boolean newSky) {
            return new Variant(newName, newSky, settings);
        }

        /**
         * Sets the client config to this variant without saving the config file.
         */
        void apply() {
            FHConfig.CLIENT.calculatedSky.set(sky);
            settings.applyToConfig();
        }
    }

    private record Shot(Pose pose, long time, Weather weather, Variant variant) {
    }

    private record ShotList(List<Shot> shots, boolean hud) {
    }

    /**
     * A set being recorded. Its file is written after every change, so the root always matches the file.
     */
    private record Recording(String name, Path file, JsonObject root) {
    }

    /**
     * Adds the harness's subcommands to /calculatedSky.
     */
    static void addCommands(LiteralArgumentBuilder<CommandSourceStack> command) {
        LiteralArgumentBuilder<CommandSourceStack> weather = literal("weather")
                .then(literal("off").executes(CalculatedSkyScreenshots::releaseWeatherCommand));
        for (Weather value : List.of(Weather.CLEAR, Weather.SNOW, Weather.STORM, Weather.SNOWFALL, Weather.BLIZZARD)) {
            weather.then(literal(value.key).executes(context -> holdWeather(context, value)));
        }
        command.then(literal("screenshots")
                        .executes(context -> start(context, null))
                        .then(literal("stop").executes(CalculatedSkyScreenshots::stop))
                        .then(setArgument()
                                .executes(context -> start(context, StringArgumentType.getString(context, "set")))))
                .then(literal("beginSet")
                        .then(setArgument()
                                .executes(context -> beginSet(context, StringArgumentType.getString(context, "set")))))
                .then(literal("endSet").executes(CalculatedSkyScreenshots::endSet))
                .then(literal("pose")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> recordPose(context, StringArgumentType.getString(context, "name")))))
                .then(literal("time")
                        .then(literal("off").executes(CalculatedSkyScreenshots::releaseTimeCommand))
                        .then(Commands.argument("ticks", LongArgumentType.longArg(0))
                                .executes(context -> holdTime(context, LongArgumentType.getLong(context, "ticks")))))
                .then(weather);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> setArgument() {
        return Commands.argument("set", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(setNames(), builder));
    }

    private static Path setFolder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FOLDER);
    }

    /**
     * Reads a set file, or reports why it cannot and returns null.
     */
    @Nullable
    private static JsonObject readSet(Path file, CommandSourceStack source) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
            source.sendFailure(Component.literal(file + " should hold one JSON object."));
        } catch (IOException | JsonParseException exception) {
            source.sendFailure(Component.literal("Cannot read " + file + ": " + exception.getMessage()));
        }
        return null;
    }

    private static boolean writeSet(Path file, JsonObject root, CommandSourceStack source) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n",
                    StandardCharsets.UTF_8);
            return true;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Cannot write " + file + ": " + exception.getMessage()));
            return false;
        }
    }

    /**
     * Listens to render ticks and logout while the time or weather is held or a set is recorded.
     */
    private static void updateListeners() {
        boolean needed = timeHeld || heldWeather != null || recording != null;
        if (needed && !listening) {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.RenderTickEvent.class,
                    HOLD_LISTENER);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ClientPlayerNetworkEvent.LoggingOut.class,
                    LOGOUT_LISTENER);
            listening = true;
        } else if (!needed && listening) {
            MinecraftForge.EVENT_BUS.unregister(HOLD_LISTENER);
            MinecraftForge.EVENT_BUS.unregister(LOGOUT_LISTENER);
            listening = false;
        }
    }

    private static void onHoldTick(TickEvent.RenderTickEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (event.phase != TickEvent.Phase.START || level == null) {
            return;
        }
        if (timeHeld) {
            level.getLevelData().setDayTime(heldTime);
        }
        if (heldWeather != null) {
            level.setRainLevel(heldWeather.rainLevel);
            level.setThunderLevel(heldWeather.thunderLevel);
        }
    }

    private static void onLoggingOut() {
        timeHeld = false;
        heldWeather = null;
        recording = null;
        updateListeners();
    }

    /**
     * Starts recording a set. An existing set is loaded and extended, and a new one starts with the sky off and on.
     */
    private static int beginSet(CommandContext<CommandSourceStack> context, String setName) {
        CommandSourceStack source = context.getSource();
        if (Minecraft.getInstance().level == null) {
            source.sendFailure(Component.literal("Sets are recorded in a world."));
            return 0;
        }
        Path file = setFolder().resolve(setName + ".json");
        JsonObject root;
        if (Files.isRegularFile(file)) {
            root = readSet(file, source);
            if (root == null) {
                return 0;
            }
        } else {
            root = new JsonObject();
            JsonArray variants = new JsonArray();
            variants.add(variantJson("vanilla", false));
            variants.add(variantJson("calculated", true));
            root.add("variants", variants);
            if (!writeSet(file, root, source)) {
                return 0;
            }
        }
        recording = new Recording(setName, file, root);
        updateListeners();
        int poseCount = root.has("poses") && root.get("poses").isJsonArray() ? root.getAsJsonArray("poses").size() : 0;
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Recording the set %s into %s, which has %d poses. /calculatedSky time and /calculatedSky weather hold "
                        + "what the client shows, /calculatedSky pose <name> saves the current place, view, time, and "
                        + "weather, and /calculatedSky endSet ends the recording.", setName, file, poseCount)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static JsonObject variantJson(String name, boolean sky) {
        JsonObject variant = new JsonObject();
        variant.addProperty("name", name);
        variant.addProperty("sky", sky);
        return variant;
    }

    private static int endSet(CommandContext<CommandSourceStack> context) {
        Recording ended = recording;
        if (ended == null) {
            context.getSource().sendFailure(Component.literal("No set is being recorded."));
            return 0;
        }
        recording = null;
        releaseTime();
        releaseWeather();
        int poseCount = ended.root().has("poses") && ended.root().get("poses").isJsonArray()
                ? ended.root().getAsJsonArray("poses").size() : 0;
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Ended the set %s with %d poses in %s. /calculatedSky screenshots %s takes it.", ended.name(),
                poseCount, ended.file(), ended.name())), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Holds the client's day time for framing poses, as a run does, until released, a run starts, or the world closes.
     */
    private static int holdTime(CommandContext<CommandSourceStack> context, long time) {
        CommandSourceStack source = context.getSource();
        ClientLevel level = Minecraft.getInstance().level;
        if (activeRun != null || level == null) {
            source.sendFailure(Component.literal("The time is held in a world while no screenshots are being taken."));
            return 0;
        }
        heldTime = time;
        timeHeld = true;
        updateListeners();
        level.getLevelData().setDayTime(time);
        double sunDegrees = Math.toDegrees(Math.asin(Math.cos(level.dimensionType().timeOfDay(time) * Math.PI * 2.0)));
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Holding the client's day time at %d, sun at %.1f degrees. The server clock keeps running. "
                        + "/calculatedSky time off releases it.", time, sunDegrees)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int releaseTimeCommand(CommandContext<CommandSourceStack> context) {
        boolean wasHeld = timeHeld;
        releaseTime();
        context.getSource().sendSuccess(() -> Component.literal(wasHeld
                ? "Released the day time. The server's next update sets it within a second."
                : "The day time was not held."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void releaseTime() {
        timeHeld = false;
        updateListeners();
    }

    /**
     * Holds the client's rain and thunder levels for framing poses, as a run does, until released, a run starts, or
     * the world closes. The levels from before the hold come back when it is released.
     */
    private static int holdWeather(CommandContext<CommandSourceStack> context, Weather weather) {
        CommandSourceStack source = context.getSource();
        ClientLevel level = Minecraft.getInstance().level;
        if (activeRun != null || level == null) {
            source.sendFailure(Component.literal(
                    "The weather is held in a world while no screenshots are being taken."));
            return 0;
        }
        if (heldWeather == null) {
            rainLevelBeforeHold = level.getRainLevel(1.0F);
            thunderLevelBeforeHold = level.getThunderLevel(1.0F);
        }
        heldWeather = weather;
        updateListeners();
        level.setRainLevel(weather.rainLevel);
        level.setThunderLevel(weather.thunderLevel);
        source.sendSuccess(() -> Component.literal("Holding the client's weather at " + weather.key
                + ". The server's climate keeps running. /calculatedSky weather off releases it."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int releaseWeatherCommand(CommandContext<CommandSourceStack> context) {
        boolean wasHeld = heldWeather != null;
        releaseWeather();
        context.getSource().sendSuccess(() -> Component.literal(wasHeld
                ? "Released the weather." : "The weather was not held."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void releaseWeather() {
        ClientLevel level = Minecraft.getInstance().level;
        if (heldWeather != null && level != null) {
            level.setRainLevel(rainLevelBeforeHold);
            level.setThunderLevel(thunderLevelBeforeHold);
        }
        heldWeather = null;
        updateListeners();
    }

    /**
     * The weather the client shows, held or from the levels the server sent.
     */
    private static Weather shownWeather(ClientLevel level) {
        if (heldWeather != null) {
            return heldWeather;
        }
        float rainLevel = level.getRainLevel(1.0F);
        boolean fullRain = rainLevel >= 0.9F;
        if (level.getThunderLevel(1.0F) >= 0.5F) {
            return fullRain ? Weather.STORM : Weather.BLIZZARD;
        }
        if (rainLevel >= 0.5F) {
            return fullRain ? Weather.SNOW : Weather.SNOWFALL;
        }
        return Weather.CLEAR;
    }

    /**
     * Saves where the player stands and looks, with the day time and weather the client shows, as a pose of the set
     * being recorded, replacing a pose of the same name. The pose goes into the set's own poses, which groups without
     * poses use.
     */
    private static int recordPose(CommandContext<CommandSourceStack> context, String poseName) {
        CommandSourceStack source = context.getSource();
        Recording current = recording;
        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        if (current == null) {
            source.sendFailure(Component.literal("No set is being recorded. /calculatedSky beginSet <set> starts one."));
            return 0;
        }
        if (player == null || level == null) {
            source.sendFailure(Component.literal("Poses are recorded in a world."));
            return 0;
        }
        long time = timeHeld ? heldTime : level.getDayTime();
        Weather weather = shownWeather(level);
        JsonObject root = current.root();

        JsonArray poses = new JsonArray();
        JsonElement existing = root.get("poses");
        if (existing != null && existing.isJsonArray()) {
            for (JsonElement entry : existing.getAsJsonArray()) {
                boolean sameName = entry.isJsonObject() && entry.getAsJsonObject().has("name")
                        && entry.getAsJsonObject().get("name").isJsonPrimitive()
                        && poseName.equals(entry.getAsJsonObject().get("name").getAsString());
                if (!sameName) {
                    poses.add(entry);
                }
            }
        }
        JsonObject pose = new JsonObject();
        pose.addProperty("name", poseName);
        pose.addProperty("yaw", rounded(Mth.wrapDegrees(player.getYRot()), 10.0));
        pose.addProperty("pitch", rounded(player.getXRot(), 10.0));
        JsonArray position = new JsonArray();
        position.add(rounded(player.getX(), 100.0));
        position.add(rounded(player.getY(), 100.0));
        position.add(rounded(player.getZ(), 100.0));
        pose.add("position", position);
        JsonArray times = new JsonArray();
        times.add(time);
        pose.add("times", times);
        JsonArray weathers = new JsonArray();
        weathers.add(weather.key);
        pose.add("weather", weathers);
        poses.add(pose);
        root.add("poses", poses);
        if (!writeSet(current.file(), root, source)) {
            return 0;
        }
        int poseCount = poses.size();
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Saved the pose %s at %.2f %.2f %.2f, yaw %.1f, pitch %.1f, time %d, %s. The set %s has %d poses.",
                poseName, player.getX(), player.getY(), player.getZ(), Mth.wrapDegrees(player.getYRot()),
                player.getXRot(), time, weather.key, current.name(), poseCount)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static double rounded(double value, double steps) {
        return Math.round(value * steps) / steps;
    }

    private static List<String> setNames() {
        List<String> names = new ArrayList<>();
        Path folder = setFolder();
        if (!Files.isDirectory(folder)) {
            return names;
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted()
                    .forEach(names::add);
        } catch (IOException exception) {
            LOGGER.warn("Cannot list the screenshot sets in {}", folder, exception);
        }
        return names;
    }

    private static int start(CommandContext<CommandSourceStack> context, @Nullable String setName) {
        CommandSourceStack source = context.getSource();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (activeRun != null) {
            source.sendFailure(Component.literal(
                    "Screenshots are already being taken. /calculatedSky screenshots stop ends the run."));
            return 0;
        }
        if (level == null || player == null || server == null) {
            source.sendFailure(Component.literal(
                    "Screenshots need a singleplayer world, since poses move the player through the integrated server."));
            return 0;
        }

        JsonObject root = new JsonObject();
        if (setName != null) {
            Path file = setFolder().resolve(setName + ".json");
            if (!Files.isRegularFile(file)) {
                source.sendFailure(Component.literal("There is no screenshot set " + file + ". Sets in that folder: "
                        + String.join(", ", setNames())));
                return 0;
            }
            JsonObject read = readSet(file, source);
            if (read == null) {
                return 0;
            }
            root = read;
        }

        List<String> warnings = new ArrayList<>();
        Pose startPose = new Pose("current", player.getYRot(), player.getXRot(), null, null, List.of(), List.of());
        ShotList shotList = parse(root, startPose, Variant.fromConfig("start"), level.getDayTime(), warnings);
        for (String warning : warnings) {
            source.sendSuccess(() -> Component.literal(warning).withStyle(ChatFormatting.YELLOW), false);
        }

        String setLabel = setName != null ? setName : "current";
        String folderName = setLabel + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT));
        Path output = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(FOLDER).resolve(folderName);
        try {
            Files.createDirectories(output);
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Cannot create " + output + ": " + exception.getMessage()));
            return 0;
        }

        releaseTime();
        releaseWeather();
        Run run = new Run(shotList, setLabel, output, FOLDER + "/" + folderName, minecraft, player, server);
        activeRun = run;
        if (shotList.shots().stream().anyMatch(shot -> shot.variant().sky())) {
            CalculatedSkyRenderer.clearFailure();
        }
        run.begin(0, minecraft, level, player);
        run.register();
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Taking %d screenshots into %s. Keep the window visible and the mouse still. "
                        + "/calculatedSky screenshots stop ends the run early.", shotList.shots().size(), output)),
                false);
        return Command.SINGLE_SUCCESS;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        Run run = activeRun;
        if (run == null) {
            context.getSource().sendFailure(Component.literal("No screenshots are being taken."));
            return 0;
        }
        run.finish("it was stopped", true);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Reads a set. Each group takes the combinations of its own poses, times, weather, and variants, and a group
     * without groups is the set itself. Lists a group leaves out come from the set, and lists the set leaves out from
     * the current view, time, and weather. Without variants each shot is taken with the sky off and on. Problems are
     * added to the warnings and the entry is skipped or given its default.
     */
    private static ShotList parse(JsonObject root, Pose startPose, Variant startVariant, long startTime,
            List<String> warnings) {
        warnUnknownKeys(root, SET_KEYS, "the set", warnings);
        List<Pose> setPoses = poses(root, startPose, warnings);
        List<Long> setTimes = times(root, warnings);
        List<Weather> setWeathers = weathers(root, warnings);
        List<Variant> setVariants = variants(root, startVariant, warnings);
        List<JsonObject> groups = objects(root, "groups", warnings);
        List<Shot> shots = new ArrayList<>();
        if (groups.isEmpty()) {
            addShots(setPoses, setTimes, setWeathers, setVariants, startPose, startVariant, startTime, shots);
        }
        for (JsonObject group : groups) {
            warnUnknownKeys(group, GROUP_KEYS, "a group", warnings);
            List<Pose> poses = poses(group, startPose, warnings);
            List<Long> times = times(group, warnings);
            List<Weather> weathers = weathers(group, warnings);
            List<Variant> variants = variants(group, startVariant, warnings);
            addShots(poses.isEmpty() ? setPoses : poses, times.isEmpty() ? setTimes : times,
                    weathers.isEmpty() ? setWeathers : weathers, variants.isEmpty() ? setVariants : variants,
                    startPose, startVariant, startTime, shots);
        }
        return new ShotList(shots, bool(root, "hud", false, warnings));
    }

    /**
     * Adds every combination in the order pose, weather, time, variant. Empty lists stand for the current pose,
     * time, and weather, and for the sky off and on with the current config.
     */
    private static void addShots(List<Pose> poses, List<Long> times, List<Weather> weathers, List<Variant> variants,
            Pose startPose, Variant startVariant, long startTime, List<Shot> shots) {
        List<Pose> usedPoses = poses.isEmpty() ? List.of(startPose) : poses;
        List<Long> usedTimes = times.isEmpty() ? List.of(startTime) : times;
        List<Weather> usedWeathers = weathers.isEmpty() ? List.of(Weather.WORLD) : weathers;
        List<Variant> usedVariants = variants.isEmpty()
                ? List.of(startVariant.withSky("vanilla", false), startVariant.withSky("calculated", true))
                : variants;
        for (Pose pose : usedPoses) {
            List<Long> poseTimes = pose.times().isEmpty() ? usedTimes : pose.times();
            List<Weather> poseWeathers = pose.weathers().isEmpty() ? usedWeathers : pose.weathers();
            for (Weather weather : poseWeathers) {
                for (long time : poseTimes) {
                    for (Variant variant : usedVariants) {
                        shots.add(new Shot(pose, time, weather, variant));
                    }
                }
            }
        }
    }

    private static List<Pose> poses(JsonObject object, Pose startPose, List<String> warnings) {
        List<Pose> poses = new ArrayList<>();
        for (JsonObject entry : objects(object, "poses", warnings)) {
            warnUnknownKeys(entry, POSE_KEYS, "a pose", warnings);
            String name = name(entry, "pose" + (poses.size() + 1), warnings);
            float yaw = (float) number(entry, "yaw", startPose.yaw(), warnings);
            float pitch = (float) clamped(entry, "pitch", startPose.pitch(), -90.0, 90.0, warnings);
            poses.add(new Pose(name, yaw, pitch, vector(entry, "position", warnings),
                    vector(entry, "offset", warnings), poseTimes(entry, warnings), weathers(entry, warnings)));
        }
        return poses;
    }

    /**
     * The pose's times followed by the day times of its sun elevations in degrees, on the branch that sunBranch names.
     */
    private static List<Long> poseTimes(JsonObject pose, List<String> warnings) {
        List<Long> times = times(pose, warnings);
        SunElevationTimes.Branch branch = sunBranch(pose, warnings);
        for (JsonElement element : array(pose, "sunElevations", warnings)) {
            if (!isNumber(element)) {
                warnings.add("Skipped a sun elevation that is not a number: " + element);
                continue;
            }
            double elevation = element.getAsDouble();
            double limited = Math.max(-90.0, Math.min(90.0, elevation));
            if (limited != elevation) {
                warnings.add("The sun elevation " + elevation + " is outside -90..90, so " + limited + " was used.");
            }
            times.add(SunElevationTimes.dayTime(limited, branch));
        }
        return times;
    }

    private static SunElevationTimes.Branch sunBranch(JsonObject pose, List<String> warnings) {
        JsonElement element = pose.get("sunBranch");
        if (element == null) {
            return SunElevationTimes.Branch.SETTING;
        }
        SunElevationTimes.Branch branch = element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                ? SunElevationTimes.Branch.byKey(element.getAsString()) : null;
        if (branch != null) {
            return branch;
        }
        warnings.add("\"sunBranch\" should be setting or rising, so setting was used: " + element);
        return SunElevationTimes.Branch.SETTING;
    }

    private static List<Long> times(JsonObject object, List<String> warnings) {
        List<Long> times = new ArrayList<>();
        for (JsonElement element : array(object, "times", warnings)) {
            if (isNumber(element)) {
                times.add(element.getAsLong());
            } else {
                warnings.add("Skipped a time that is not a number: " + element);
            }
        }
        return times;
    }

    private static List<Weather> weathers(JsonObject object, List<String> warnings) {
        List<Weather> weathers = new ArrayList<>();
        for (JsonElement element : array(object, "weather", warnings)) {
            Weather weather = element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                    ? Weather.byKey(element.getAsString()) : null;
            if (weather != null) {
                weathers.add(weather);
            } else {
                warnings.add("Skipped the weather " + element + ". Use world, clear, snow, storm, snowfall, or blizzard.");
            }
        }
        return weathers;
    }

    private static List<Variant> variants(JsonObject object, Variant startVariant, List<String> warnings) {
        List<Variant> variants = new ArrayList<>();
        for (JsonObject entry : objects(object, "variants", warnings)) {
            warnUnknownKeys(entry, VARIANT_KEYS, "a variant", warnings);
            String name = name(entry, "variant" + (variants.size() + 1), warnings);
            boolean sky = bool(entry, "sky", startVariant.sky(), warnings);
            SkySettings settings = startVariant.settings();
            for (SkySetting setting : SkySetting.values()) {
                double startValue = startVariant.settings().get(setting);
                double value;
                if (setting.isSwitch()) {
                    SkySetting manualValue = setting.manualValue();
                    // A variant that names the manual value of a switch uses it unless it turns the switch on:
                    boolean fallback = startValue != 0.0 && (manualValue == null || !entry.has(manualValue.key()));
                    value = bool(entry, setting.key(), fallback, warnings) ? 1.0 : 0.0;
                } else {
                    value = clamped(entry, setting.key(), startValue, setting.minimum(), setting.maximum(), warnings);
                }
                settings = settings.with(setting, value);
            }
            variants.add(new Variant(name, sky, settings));
        }
        return variants;
    }

    private static void warnUnknownKeys(JsonObject object, Set<String> knownKeys, String where,
            List<String> warnings) {
        for (String key : object.keySet()) {
            if (!knownKeys.contains(key)) {
                warnings.add("Ignored the unknown key \"" + key + "\" in " + where + ".");
            }
        }
    }

    private static List<JsonElement> array(JsonObject object, String key, List<String> warnings) {
        List<JsonElement> result = new ArrayList<>();
        JsonElement element = object.get(key);
        if (element == null) {
            return result;
        }
        if (!element.isJsonArray()) {
            warnings.add("Skipped \"" + key + "\", which should be a list.");
            return result;
        }
        element.getAsJsonArray().forEach(result::add);
        return result;
    }

    private static List<JsonObject> objects(JsonObject object, String key, List<String> warnings) {
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : array(object, key, warnings)) {
            if (element.isJsonObject()) {
                result.add(element.getAsJsonObject());
            } else {
                warnings.add("Skipped an entry of \"" + key + "\" that is not an object: " + element);
            }
        }
        return result;
    }

    private static boolean isNumber(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }

    private static double number(JsonObject object, String key, double fallback, List<String> warnings) {
        JsonElement element = object.get(key);
        if (element == null) {
            return fallback;
        }
        if (isNumber(element)) {
            return element.getAsDouble();
        }
        warnings.add("\"" + key + "\" should be a number, so " + fallback + " was used.");
        return fallback;
    }

    private static double clamped(JsonObject object, String key, double fallback, double minimum, double maximum,
            List<String> warnings) {
        double value = number(object, key, fallback, warnings);
        if (value < minimum || value > maximum) {
            double limited = Math.max(minimum, Math.min(maximum, value));
            warnings.add("\"" + key + "\" " + value + " is outside " + minimum + ".." + maximum + ", so " + limited
                    + " was used.");
            return limited;
        }
        return value;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback, List<String> warnings) {
        JsonElement element = object.get(key);
        if (element == null) {
            return fallback;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        warnings.add("\"" + key + "\" should be true or false, so " + fallback + " was used.");
        return fallback;
    }

    /**
     * The entry's name with characters that cannot go into a file name replaced by dashes.
     */
    private static String name(JsonObject object, String fallback, List<String> warnings) {
        JsonElement element = object.get("name");
        if (element == null) {
            return fallback;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                && !element.getAsString().isBlank()) {
            return element.getAsString().replaceAll("[^A-Za-z0-9_.-]", "-");
        }
        warnings.add("\"name\" should be text, so " + fallback + " was used.");
        return fallback;
    }

    @Nullable
    private static Vec3 vector(JsonObject object, String key, List<String> warnings) {
        JsonElement element = object.get(key);
        if (element == null) {
            return null;
        }
        if (element.isJsonArray() && element.getAsJsonArray().size() == 3) {
            JsonElement x = element.getAsJsonArray().get(0);
            JsonElement y = element.getAsJsonArray().get(1);
            JsonElement z = element.getAsJsonArray().get(2);
            if (isNumber(x) && isNumber(y) && isNumber(z)) {
                return new Vec3(x.getAsDouble(), y.getAsDouble(), z.getAsDouble());
            }
        }
        warnings.add("Skipped \"" + key + "\", which should be a list of three numbers: " + element);
        return null;
    }

    /**
     * The column names of manifest.tsv, in the order of the fields that Run.take writes.
     */
    private static String manifestHeader() {
        List<String> columns = new ArrayList<>(List.of("index", "file", "pose", "x", "y", "z", "yaw", "pitch",
                "time", "sunDegrees", "weather", "rain", "thunder", "variant", "sky"));
        for (SkySetting setting : SkySetting.values()) {
            columns.add(setting.key());
        }
        columns.addAll(List.of("drawnIceOpticalDepth", "drawnDustExtinction", "status", "framesPerSecond",
                "waitSeconds"));
        return String.join("\t", columns);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /**
     * One screenshot run. It listens to render ticks only while it runs, and every method runs on the render thread.
     */
    private static final class Run {
        private final List<Shot> shots;
        private final boolean hud;
        private final String setLabel;
        private final Path output;
        private final String screenshotFolder;
        private final long seed;
        private final int windowWidth;
        private final int windowHeight;
        private final Variant startVariant;
        private final Vec3 startPosition;
        private final float startYaw;
        private final float startPitch;
        private final float startRainLevel;
        private final float startThunderLevel;
        private final boolean startHideGui;
        private final boolean startPauseOnLostFocus;
        private final List<String> manifest = new ArrayList<>();
        private final Consumer<TickEvent.RenderTickEvent> renderTickListener = this::onRenderTick;
        private final Consumer<ClientPlayerNetworkEvent.LoggingOut> loggingOutListener = this::onLoggingOut;
        private int index;
        private long shotStartNanos;
        private int framesWaited;
        private boolean weatherChanged;
        private boolean moved;
        @Nullable
        private Vec3 moveTarget;

        Run(ShotList shotList, String setLabel, Path output, String screenshotFolder, Minecraft minecraft,
                LocalPlayer player, MinecraftServer server) {
            this.shots = shotList.shots();
            this.hud = shotList.hud();
            this.setLabel = setLabel;
            this.output = output;
            this.screenshotFolder = screenshotFolder;
            this.seed = server.getWorldData().worldGenOptions().seed();
            this.windowWidth = minecraft.getWindow().getWidth();
            this.windowHeight = minecraft.getWindow().getHeight();
            this.startVariant = Variant.fromConfig("start");
            this.startPosition = player.position();
            this.startYaw = player.getYRot();
            this.startPitch = player.getXRot();
            this.startRainLevel = player.level().getRainLevel(1.0F);
            this.startThunderLevel = player.level().getThunderLevel(1.0F);
            this.startHideGui = minecraft.options.hideGui;
            this.startPauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        }

        void register() {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.RenderTickEvent.class,
                    renderTickListener);
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ClientPlayerNetworkEvent.LoggingOut.class,
                    loggingOutListener);
        }

        private void unregister() {
            MinecraftForge.EVENT_BUS.unregister(renderTickListener);
            MinecraftForge.EVENT_BUS.unregister(loggingOutListener);
        }

        /**
         * Applies the config, weather, and position of a shot and starts waiting for it.
         */
        void begin(int shotIndex, Minecraft minecraft, ClientLevel level, LocalPlayer player) {
            Weather previousWeather = shotIndex > 0 ? shots.get(shotIndex - 1).weather() : Weather.WORLD;
            index = shotIndex;
            Shot shot = shots.get(shotIndex);
            shot.variant().apply();
            weatherChanged = shot.weather() != previousWeather;
            if (shot.weather() == Weather.WORLD) {
                level.setRainLevel(startRainLevel);
                level.setThunderLevel(startThunderLevel);
            }
            Vec3 target = shot.pose().target(startPosition);
            moveTarget = null;
            if (player.position().distanceToSqr(target) > ARRIVAL_DISTANCE_SQUARED) {
                moveTarget = target;
                moved = true;
                move(minecraft, player, target, shot.pose().yaw(), shot.pose().pitch());
            }
            shotStartNanos = System.nanoTime();
            framesWaited = 0;
        }

        /**
         * Teleports the player through the integrated server, flying when the player may fly so that poses in the air
         * stay where they are.
         */
        private static void move(Minecraft minecraft, LocalPlayer player, Vec3 target, float yaw, float pitch) {
            MinecraftServer server = minecraft.getSingleplayerServer();
            if (server == null) {
                return;
            }
            if (player.getAbilities().mayfly && !player.getAbilities().flying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            UUID playerId = player.getUUID();
            ResourceKey<Level> dimension = player.level().dimension();
            server.execute(() -> {
                ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerId);
                ServerLevel serverLevel = server.getLevel(dimension);
                if (serverPlayer != null && serverLevel != null) {
                    serverPlayer.teleportTo(serverLevel, target.x, target.y, target.z, yaw, pitch);
                }
            });
        }

        private void onRenderTick(TickEvent.RenderTickEvent event) {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            LocalPlayer player = minecraft.player;
            if (level == null || player == null) {
                finish("the world was closed", false);
                return;
            }
            Shot shot = shots.get(index);
            if (event.phase == TickEvent.Phase.START) {
                hold(minecraft, level, player, shot);
                return;
            }
            if (minecraft.screen != null) {
                shotStartNanos = System.nanoTime();
                framesWaited = 0;
                return;
            }
            framesWaited++;
            double waited = (System.nanoTime() - shotStartNanos) / 1.0e9;
            double settleSeconds = MINIMUM_SETTLE_SECONDS + (weatherChanged ? WEATHER_SETTLE_SECONDS : 0.0)
                    + (moveTarget != null ? MOVE_SETTLE_SECONDS : 0.0);
            boolean settling = waited < settleSeconds;
            boolean moving = moveTarget != null && player.position().distanceToSqr(moveTarget) > ARRIVAL_DISTANCE_SQUARED;
            boolean terrainPending = moveTarget != null && !minecraft.levelRenderer.hasRenderedAllChunks();
            boolean skyDrawn = shot.variant().sky() && CalculatedSkyRenderer.isAllowed(level);
            boolean skyPending = skyDrawn && !CalculatedSkyRenderer.isSettled(level, event.renderTickTime);
            boolean ready = !settling && !moving && !terrainPending && !skyPending;
            if (!ready && waited < TIMEOUT_SECONDS) {
                return;
            }

            String status;
            if (!ready) {
                StringJoiner pending = new StringJoiner(", ");
                if (moving) {
                    pending.add("the move");
                }
                if (terrainPending) {
                    pending.add("the terrain");
                }
                if (skyPending) {
                    pending.add("the sky");
                }
                status = "timed out waiting for " + pending;
            } else if (shot.variant().sky() && !skyDrawn) {
                status = "sky not drawn, " + CalculatedSkyRenderer.describeState(level);
            } else {
                status = "settled";
            }
            take(minecraft, level, player, shot, event.renderTickTime, status, waited);
            if (index + 1 < shots.size()) {
                begin(index + 1, minecraft, level, player);
            } else {
                finish(null, true);
            }
        }

        private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            finish("the world was closed", false);
        }

        /**
         * Holds the shot's time, weather, and view direction for the coming frame, after the client tick and the
         * mouse moved them.
         */
        private void hold(Minecraft minecraft, ClientLevel level, LocalPlayer player, Shot shot) {
            minecraft.options.hideGui = !hud;
            minecraft.options.pauseOnLostFocus = false;
            level.getLevelData().setDayTime(shot.time());
            if (shot.weather() != Weather.WORLD) {
                level.setRainLevel(shot.weather().rainLevel);
                level.setThunderLevel(shot.weather().thunderLevel);
            }
            look(player, shot.pose().yaw(), shot.pose().pitch());
        }

        private static void look(LocalPlayer player, float yaw, float pitch) {
            player.setYRot(yaw);
            player.yRotO = yaw;
            player.setXRot(pitch);
            player.xRotO = pitch;
            player.setYHeadRot(yaw);
            player.yHeadRotO = yaw;
            player.yBodyRot = yaw;
            player.yBodyRotO = yaw;
            player.yBob = yaw;
            player.yBobO = yaw;
            player.xBob = pitch;
            player.xBobO = pitch;
        }

        private void take(Minecraft minecraft, ClientLevel level, LocalPlayer player, Shot shot, float partialTick,
                String status, double waited) {
            String fileName = String.format(Locale.ROOT, "%03d_%s_%d_%s_%s.png", index + 1, shot.pose().name(),
                    shot.time(), shot.weather().key, shot.variant().name());
            Screenshot.grab(minecraft.gameDirectory, screenshotFolder + "/" + fileName, minecraft.getMainRenderTarget(),
                    message -> LOGGER.info("Calculated sky screenshot: {}", message.getString()));
            double sunDegrees = Math.toDegrees(Math.asin(Math.cos(level.getTimeOfDay(partialTick) * Math.PI * 2.0)));
            Vec3 position = player.position();
            Variant variant = shot.variant();
            List<String> fields = new ArrayList<>(List.of(Integer.toString(index + 1), fileName, shot.pose().name(),
                    decimal(position.x), decimal(position.y), decimal(position.z), decimal(shot.pose().yaw()),
                    decimal(shot.pose().pitch()), Long.toString(shot.time()), decimal(sunDegrees), shot.weather().key,
                    decimal(level.getRainLevel(partialTick)), decimal(level.getThunderLevel(partialTick)),
                    variant.name(), Boolean.toString(variant.sky())));
            for (SkySetting setting : SkySetting.values()) {
                double value = variant.settings().get(setting);
                fields.add(setting.isSwitch() ? Boolean.toString(value != 0.0) : decimal(value));
            }
            fields.addAll(List.of(decimal(IceWeatherDepth.update(partialTick)),
                    decimal(DiamondDustExtinction.lastValue()), status,
                    decimal(framesWaited / Math.max(waited, 1.0e-3)), decimal(waited)));
            manifest.add(String.join("\t", fields));
        }

        /**
         * Ends the run, restores what it changed, and writes the manifest. With the world still open the player is
         * moved back to where the run started.
         */
        void finish(@Nullable String reason, boolean worldOpen) {
            unregister();
            activeRun = null;
            Minecraft minecraft = Minecraft.getInstance();
            startVariant.apply();
            minecraft.options.hideGui = startHideGui;
            minecraft.options.pauseOnLostFocus = startPauseOnLostFocus;
            ClientLevel level = minecraft.level;
            LocalPlayer player = minecraft.player;
            if (worldOpen && level != null && player != null) {
                level.setRainLevel(startRainLevel);
                level.setThunderLevel(startThunderLevel);
                if (moved) {
                    move(minecraft, player, startPosition, startYaw, startPitch);
                }
                look(player, startYaw, startPitch);
            }

            List<String> lines = new ArrayList<>();
            lines.add("# set\t" + setLabel);
            lines.add("# seed\t" + seed);
            lines.add("# window\t" + windowWidth + "x" + windowHeight);
            lines.add(MANIFEST_HEADER);
            lines.addAll(manifest);
            Path manifestFile = output.resolve("manifest.tsv");
            String manifestProblem = null;
            try {
                Files.write(manifestFile, lines, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                LOGGER.error("Cannot write the screenshot manifest {}", manifestFile, exception);
                manifestProblem = exception.getMessage();
            }

            String summary = String.format(Locale.ROOT, "Took %d of %d screenshots into ", manifest.size(),
                    shots.size());
            String ending = reason != null ? ". The run ended early because " + reason + "." : ".";
            LOGGER.info("{}{}{}", summary, output, ending);
            if (player != null) {
                String folder = output.toString();
                player.displayClientMessage(Component.literal(summary)
                        .append(Component.literal(folder).withStyle(style -> style.withUnderlined(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, folder))))
                        .append(Component.literal(ending)), false);
                if (manifestProblem != null) {
                    player.displayClientMessage(Component.literal("The manifest could not be written: "
                            + manifestProblem).withStyle(ChatFormatting.RED), false);
                }
            }
        }
    }
}
