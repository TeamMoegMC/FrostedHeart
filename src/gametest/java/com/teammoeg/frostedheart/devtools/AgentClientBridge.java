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

package com.teammoeg.frostedheart.devtools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.frostedheart.FHMain;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * 开发用客户端命令桥。仅在 {@code -Dfrostedheart.agentBridge=true} 时启用：每次从游戏目录
 * {@code agent/inbox} 取一条指令，在客户端线程上执行，再把 JSON 结果写入 {@code agent/outbox}，
 * 让脚本或 agent 一步一步驱动真实客户端（进服、按键、点击界面、截图）。
 * <p>
 * Development client command bridge, enabled only with {@code -Dfrostedheart.agentBridge=true}. It takes one
 * command at a time from {@code <gameDir>/agent/inbox}, runs it on the client thread and writes a JSON result to
 * {@code <gameDir>/agent/outbox}, so a script or agent can drive the real client step by step (join, press keys,
 * click GUIs, take screenshots). Driven by {@code Scripts/agent_client.py}, which lists the commands.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT)
public final class AgentClientBridge {
    private static final boolean ENABLED = Boolean.getBoolean("frostedheart.agentBridge");
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int DEFAULT_TIMEOUT_TICKS = 200;
    private static final int JOIN_TIMEOUT_TICKS = 20 * 300;
    private static final int COMMAND_REPLY_TICKS = 20;
    private static final int MESSAGE_HISTORY = 200;
    private static final int CONNECT_RETRIES = 2;
    /** Ticks after the loading overlay closes before the first command runs, so start-up work settles first. */
    private static final int STARTUP_GRACE_TICKS = 40;

    private static final List<String> messages = new ArrayList<>();
    private static long messageCount;
    private static int readyTicks;
    private static Path root;
    private static Job current;

    private AgentClientBridge() {}

    private interface Phase {
        /** @return true when the command has finished */
        boolean run(Minecraft mc, Job job) throws Exception;
    }

    private static final class Job {
        final String id;
        final String line;
        final Map<String, Object> out = new LinkedHashMap<>();
        Phase tick;
        Phase render;
        int age;
        int frames;
        int timeout = DEFAULT_TIMEOUT_TICKS;
        boolean stopAfter;

        Job(String id, String line) {
            this.id = id;
            this.line = line;
            out.put("id", id);
            out.put("command", line);
            out.put("ok", false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        Job job = current;
        try {
            if (root == null) start(mc);
            if (readyTicks < STARTUP_GRACE_TICKS) {
                readyTicks++;
                return;
            }
            if (job == null) {
                job = current = poll(mc);
                if (job == null) return;
            }
            if (job.tick != null && job.tick.run(mc, job)) {
                finish(mc, job, null);
                return;
            }
            if (++job.age > job.timeout) throw new IllegalStateException("timed out after " + job.timeout + " ticks");
        } catch (Throwable failure) {
            if (job != null) finish(mc, job, failure);
            else LOG.error("Agent bridge could not read its inbox", failure);
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        Job job = current;
        if (!ENABLED || event.phase != TickEvent.Phase.END || job == null || job.render == null) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (job.render.run(mc, job)) finish(mc, job, null);
        } catch (Throwable failure) {
            finish(mc, job, failure);
        }
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        if (!ENABLED) return;
        messages.add(event.getMessage().getString());
        messageCount++;
        if (messages.size() > MESSAGE_HISTORY) messages.remove(0);
    }

    private static void start(Minecraft mc) throws Exception {
        root = FMLPaths.GAMEDIR.get().resolve("agent").toAbsolutePath();
        Files.createDirectories(root.resolve("inbox"));
        Files.createDirectories(root.resolve("outbox"));
        Files.createDirectories(root.resolve("screenshots"));
        // The agent's window is usually unfocused; an automatic pause menu would swallow every step.
        mc.options.pauseOnLostFocus = false;
        LOG.info("Agent bridge reading commands from {}", root.resolve("inbox"));
    }

    private static Job poll(Minecraft mc) throws Exception {
        Path next;
        try (Stream<Path> files = Files.list(root.resolve("inbox"))) {
            next = files.filter(p -> p.getFileName().toString().endsWith(".cmd")).sorted().findFirst().orElse(null);
        }
        if (next == null) return null;
        String name = next.getFileName().toString();
        Job job = new Job(name.substring(0, name.length() - 4), Files.readString(next, StandardCharsets.UTF_8).strip());
        Files.deleteIfExists(next);
        LOG.info("Agent bridge [{}] {}", job.id, job.line);
        try {
            configure(job);
        } catch (Throwable failure) {
            finish(mc, job, failure);
            return null;
        }
        return job;
    }

    private static void finish(Minecraft mc, Job job, Throwable failure) {
        if (current == job) current = null;
        job.out.put("ok", failure == null);
        if (failure != null) {
            job.out.put("error", String.valueOf(failure));
            LOG.warn("Agent bridge [{}] failed: {}", job.id, failure.toString());
        }
        try {
            job.out.put("state", state(mc));
        } catch (Throwable stateFailure) {
            job.out.put("state", "unavailable: " + stateFailure);
        }
        try {
            Path outbox = root.resolve("outbox");
            Path temp = outbox.resolve(job.id + ".json.tmp");
            Files.writeString(temp, GSON.toJson(job.out), StandardCharsets.UTF_8);
            Files.move(temp, outbox.resolve(job.id + ".json"), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception writeFailure) {
            LOG.error("Agent bridge [{}] could not write its result", job.id, writeFailure);
        }
        if (job.stopAfter) mc.stop();
    }

    private static void configure(Job job) {
        String line = job.line;
        int space = line.indexOf(' ');
        String verb = (space < 0 ? line : line.substring(0, space)).toLowerCase(Locale.ROOT);
        String rest = space < 0 ? "" : line.substring(space + 1).strip();
        String[] args = rest.isEmpty() ? new String[0] : rest.split("\\s+");
        switch (verb) {
            case "roads" -> {
                var result = TownRoadClientTools.run(Minecraft.getInstance(), rest);
                job.timeout = 1200;
                job.tick = (mc, j) -> {
                    if (!result.isDone()) return false;
                    j.out.put("roads", result.join()); return true;
                };
            }
            case "state" -> job.tick = (mc, j) -> true;
            case "wait" -> {
                int ticks = Integer.parseInt(arg(args, 0, "ticks"));
                job.timeout = ticks + 20;
                job.tick = (mc, j) -> j.age >= ticks;
            }
            case "connect" -> {
                String address = arg(args, 0, "host[:port]");
                List<String> retries = new ArrayList<>();
                Runnable connect = () -> ConnectScreen.startConnecting(new TitleScreen(), Minecraft.getInstance(),
                        ServerAddress.parseString(address), new ServerData("Agent", address, false), false);
                job.timeout = JOIN_TIMEOUT_TICKS;
                job.tick = (mc, j) -> {
                    if (j.age == 0) {
                        requireOutOfWorld(mc);
                        mc.tell(connect);
                        return false;
                    }
                    // A dedicated server occasionally never answers the first login after the client starts;
                    // retry such failures and keep their reasons visible in the result.
                    if (mc.screen instanceof DisconnectedScreen screen && retries.size() < CONNECT_RETRIES) {
                        retries.add(disconnectReason(screen));
                        j.out.put("retries", retries);
                        mc.tell(connect);
                        return false;
                    }
                    return joined(mc);
                };
            }
            case "world" -> {
                String level = arg(args, 0, "save folder");
                job.timeout = JOIN_TIMEOUT_TICKS;
                job.tick = (mc, j) -> {
                    if (j.age == 0) {
                        requireOutOfWorld(mc);
                        mc.tell(() -> mc.createWorldOpenFlows().loadLevel(new TitleScreen(), level));
                        return false;
                    }
                    return joined(mc);
                };
            }
            case "newworld" -> {
                String level = arg(args, 0, "save folder");
                boolean flat = args.length < 2 || !"normal".equalsIgnoreCase(args[1]);
                long seed = args.length > 2 ? Long.parseLong(args[2]) : new Random().nextLong();
                job.timeout = JOIN_TIMEOUT_TICKS;
                job.out.put("seed", seed);
                job.tick = (mc, j) -> {
                    if (j.age == 0) {
                        requireOutOfWorld(mc);
                        if (Files.exists(FMLPaths.GAMEDIR.get().resolve("saves").resolve(level)))
                            throw new IllegalStateException("saves/" + level + " already exists; use 'world " + level + "'");
                        mc.tell(() -> mc.createWorldOpenFlows().createFreshLevel(level,
                                new LevelSettings(level, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                        new GameRules(), WorldDataConfiguration.DEFAULT),
                                new WorldOptions(seed, true, false),
                                registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                        .getHolderOrThrow(flat ? WorldPresets.FLAT : WorldPresets.NORMAL).value()
                                        .createWorldDimensions()));
                        return false;
                    }
                    return joined(mc);
                };
            }
            case "disconnect" -> {
                job.timeout = 20 * 120;
                job.tick = (mc, j) -> {
                    if (j.age == 0) {
                        if (mc.level == null) return true;
                        mc.tell(() -> disconnect(mc));
                        return false;
                    }
                    return mc.level == null && mc.screen != null && !(mc.screen instanceof GenericDirtMessageScreen);
                };
            }
            case "command" -> {
                if (rest.isEmpty()) throw new IllegalArgumentException("usage: command /<command> | command <chat text>");
                long[] mark = new long[1];
                job.timeout = COMMAND_REPLY_TICKS + 40;
                job.tick = (mc, j) -> {
                    if (j.age == 0) {
                        LocalPlayer player = requirePlayer(mc);
                        mark[0] = messageCount;
                        if (rest.startsWith("/")) player.connection.sendCommand(rest.substring(1));
                        else player.connection.sendChat(rest);
                        return false;
                    }
                    if (j.age < COMMAND_REPLY_TICKS) return false;
                    j.out.put("messages", messagesSince(mark[0]));
                    return true;
                };
            }
            case "messages" -> job.tick = (mc, j) -> {
                j.out.put("messages", new ArrayList<>(messages));
                return true;
            };
            case "look" -> {
                float yaw = Float.parseFloat(arg(args, 0, "yaw"));
                float pitch = Float.parseFloat(arg(args, 1, "pitch"));
                job.tick = (mc, j) -> {
                    if (j.age == 0) {
                        LocalPlayer player = requirePlayer(mc);
                        player.setYRot(yaw);
                        player.setXRot(pitch);
                        syncRotation(player);
                    }
                    return j.age >= 2;
                };
            }
            case "lookat" -> {
                Vec3 target = new Vec3(Math.floor(Double.parseDouble(arg(args, 0, "x"))) + 0.5,
                        Math.floor(Double.parseDouble(arg(args, 1, "y"))) + 0.5,
                        Math.floor(Double.parseDouble(arg(args, 2, "z"))) + 0.5);
                job.tick = (mc, j) -> {
                    if (j.age == 0) {
                        LocalPlayer player = requirePlayer(mc);
                        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
                        syncRotation(player);
                    }
                    return j.age >= 2;
                };
            }
            case "key" -> {
                InputConstants.Key key = parseKey(arg(args, 0, "key name"));
                job.tick = (mc, j) -> {
                    if (j.age == 0) press(mc, key);
                    return j.age >= 2;
                };
            }
            case "type" -> {
                if (rest.isEmpty()) throw new IllegalArgumentException("usage: type <text>");
                job.tick = (mc, j) -> {
                    if (j.age == 0) {
                        requireScreen(mc);
                        rest.codePoints().forEach(c -> invoke(KEYBOARD_CHAR, mc.keyboardHandler, window(mc), c, 0));
                    }
                    return j.age >= 2;
                };
            }
            case "click" -> {
                double x = Double.parseDouble(arg(args, 0, "x"));
                double y = Double.parseDouble(arg(args, 1, "y"));
                int button = args.length > 2 ? parseButton(args[2]) : GLFW.GLFW_MOUSE_BUTTON_LEFT;
                hoverThen(job, true, x, y, mc -> {
                    invoke(MOUSE_PRESS, mc.mouseHandler, window(mc), button, GLFW.GLFW_PRESS, 0);
                    invoke(MOUSE_PRESS, mc.mouseHandler, window(mc), button, GLFW.GLFW_RELEASE, 0);
                });
            }
            case "drag" -> {
                // Press at the start, move in eight steps so the screen sees mouseDragged, release at the end.
                double x1 = Double.parseDouble(arg(args, 0, "x1")), y1 = Double.parseDouble(arg(args, 1, "y1"));
                double x2 = Double.parseDouble(arg(args, 2, "x2")), y2 = Double.parseDouble(arg(args, 3, "y2"));
                int steps = 8;
                job.render = (mc, j) -> {
                    j.frames++;
                    return false;
                };
                job.tick = (mc, j) -> {
                    requireScreen(mc);
                    int age = j.age;
                    if (age == 0) moveMouse(mc, x1, y1);
                    else if (age == 1) invoke(MOUSE_PRESS, mc.mouseHandler, window(mc), GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
                    else if (age <= 1 + steps) {
                        double t = (age - 1) / (double) steps;
                        moveMouse(mc, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t);
                    } else if (age == 2 + steps)
                        invoke(MOUSE_PRESS, mc.mouseHandler, window(mc), GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
                    return age >= 4 + steps;
                };
            }
            case "scroll" -> {
                double x = Double.parseDouble(arg(args, 0, "x"));
                double y = Double.parseDouble(arg(args, 1, "y"));
                double delta = Double.parseDouble(arg(args, 2, "delta"));
                // Without a screen the wheel scrolls the hotbar, as for a real mouse.
                hoverThen(job, false, x, y, mc -> invoke(MOUSE_SCROLL, mc.mouseHandler, window(mc), 0.0, delta));
            }
            case "move" -> {
                double x = Double.parseDouble(arg(args, 0, "x"));
                double y = Double.parseDouble(arg(args, 1, "y"));
                job.tick = (mc, j) -> {
                    if (j.age == 0) {
                        // Without a screen a grabbed mouse would turn the camera; 'look' covers that case.
                        requireScreen(mc);
                        moveMouse(mc, x, y);
                    }
                    return j.age >= 2;
                };
            }
            case "screen" -> job.tick = (mc, j) -> {
                j.out.put("screen", describeScreen(mc));
                return true;
            };
            case "screenshot" -> {
                String name = args.length > 0 ? args[0].replaceAll("[^A-Za-z0-9_-]", "_") : "shot";
                job.render = (mc, j) -> {
                    // Let at least two fresh frames render so the result reflects the preceding step.
                    if (++j.frames < 3) return false;
                    Path file = root.resolve("screenshots").resolve(j.id + "-" + name + ".png");
                    try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                        image.writeToFile(file);
                        j.out.put("file", file.toString());
                        j.out.put("pixels", List.of(image.getWidth(), image.getHeight()));
                    }
                    j.out.put("guiScale", mc.getWindow().getGuiScale());
                    return true;
                };
            }
            case "quit" -> {
                job.stopAfter = true;
                job.tick = (mc, j) -> true;
            }
            default -> throw new IllegalArgumentException("unknown command '" + verb + "'; see Scripts/agent_client.py");
        }
    }

    private interface Action {
        void run(Minecraft mc) throws Exception;
    }

    /**
     * Moves the cursor (when a screen is open), lets frames render, then acts. CUI and vanilla screens resolve
     * clicks against the hover state of the last rendered frame, so pressing in the same tick as the move would
     * hit whatever was under the previous cursor position.
     */
    private static void hoverThen(Job job, boolean needsScreen, double x, double y, Action action) {
        int[] actedAt = {-1};
        job.render = (mc, j) -> {
            j.frames++;
            return false;
        };
        job.tick = (mc, j) -> {
            if (j.age == 0) {
                if (needsScreen) requireScreen(mc);
                if (mc.screen != null) moveMouse(mc, x, y);
                j.frames = 0;
                return false;
            }
            if (actedAt[0] < 0) {
                if (j.frames < 2) return false;
                action.run(mc);
                actedAt[0] = j.age;
                return false;
            }
            return j.age >= actedAt[0] + 2;
        };
    }

    private static String arg(String[] args, int index, String name) {
        if (index >= args.length) throw new IllegalArgumentException("missing argument: " + name);
        return args[index];
    }

    private static void requireOutOfWorld(Minecraft mc) {
        if (mc.level != null) throw new IllegalStateException("already in a world; run 'disconnect' first");
    }

    private static LocalPlayer requirePlayer(Minecraft mc) {
        if (mc.player == null) throw new IllegalStateException("not in a world");
        return mc.player;
    }

    private static Screen requireScreen(Minecraft mc) {
        if (mc.screen == null) throw new IllegalStateException("no screen is open");
        return mc.screen;
    }

    private static boolean joined(Minecraft mc) {
        if (mc.screen instanceof DisconnectedScreen screen)
            throw new IllegalStateException("disconnected: " + disconnectReason(screen));
        Screen screen = mc.screen;
        boolean loading = screen instanceof ReceivingLevelScreen || screen instanceof LevelLoadingScreen
                || screen instanceof ProgressScreen || screen instanceof ConnectScreen
                || screen instanceof GenericDirtMessageScreen;
        return mc.player != null && mc.level != null && mc.player.tickCount >= 20 && !loading;
    }

    private static String disconnectReason(DisconnectedScreen screen) {
        String title = screen.getTitle().getString();
        try {
            Field reason = DisconnectedScreen.class.getDeclaredField("reason");
            reason.setAccessible(true);
            return title + ": " + ((Component) reason.get(screen)).getString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return title;
        }
    }

    private static void disconnect(Minecraft mc) {
        if (mc.level == null) return;
        boolean local = mc.isLocalServer();
        mc.level.disconnect();
        if (local) mc.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        else mc.clearLevel();
        mc.setScreen(new TitleScreen());
    }

    private static void syncRotation(LocalPlayer player) {
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
        player.setYHeadRot(player.getYRot());
        player.yHeadRotO = player.getYRot();
    }

    private static InputConstants.Key parseKey(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (!key.startsWith("key.")) key = key.startsWith("mouse.") ? "key." + key : "key.keyboard." + key;
        return InputConstants.getKey(key);
    }

    private static int parseButton(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "left" -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case "right" -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case "middle" -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            default -> Integer.parseInt(name);
        };
    }

    // Vanilla's GLFW callback targets, so Forge input events (mod hotkeys, JEI, CUI) see the same calls as real input.
    // Official names; this class only runs in the development environment.
    private static final Method MOUSE_PRESS = declared(MouseHandler.class, "onPress", long.class, int.class, int.class, int.class);
    private static final Method MOUSE_SCROLL = declared(MouseHandler.class, "onScroll", long.class, double.class, double.class);
    private static final Method MOUSE_MOVE = declared(MouseHandler.class, "onMove", long.class, double.class, double.class);
    private static final Method KEYBOARD_CHAR = declared(KeyboardHandler.class, "charTyped", long.class, int.class, int.class);

    private static Method declared(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("agent bridge needs " + owner.getSimpleName() + "#" + name, e);
        }
    }

    private static void invoke(Method method, Object target, Object... arguments) {
        try {
            method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof RuntimeException runtime ? runtime : new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static long window(Minecraft mc) {
        return mc.getWindow().getWindow();
    }

    private static void press(Minecraft mc, InputConstants.Key key) {
        if (key.getType() == InputConstants.Type.KEYSYM) {
            int scanCode = GLFW.glfwGetKeyScancode(key.getValue());
            mc.keyboardHandler.keyPress(window(mc), key.getValue(), scanCode, GLFW.GLFW_PRESS, 0);
            mc.keyboardHandler.keyPress(window(mc), key.getValue(), scanCode, GLFW.GLFW_RELEASE, 0);
        } else if (mc.screen != null) {
            invoke(MOUSE_PRESS, mc.mouseHandler, window(mc), key.getValue(), GLFW.GLFW_PRESS, 0);
            invoke(MOUSE_PRESS, mc.mouseHandler, window(mc), key.getValue(), GLFW.GLFW_RELEASE, 0);
        } else {
            // In the world a real press would first grab the OS cursor; clicking the bound KeyMapping avoids
            // taking the user's mouse while keeping the vanilla use/attack path (consumed in the next tick).
            KeyMapping.click(key);
        }
    }

    /** Moves the in-game cursor to GUI coordinates (hover, tooltips, drags); the OS cursor is not moved. */
    private static void moveMouse(Minecraft mc, double guiX, double guiY) {
        var window = mc.getWindow();
        invoke(MOUSE_MOVE, mc.mouseHandler, window(mc), guiX * window.getScreenWidth() / window.getGuiScaledWidth(),
                guiY * window.getScreenHeight() / window.getGuiScaledHeight());
    }

    private static List<String> messagesSince(long mark) {
        long first = messageCount - messages.size();
        int from = (int) Math.min(messages.size(), Math.max(0, mark - first));
        return new ArrayList<>(messages.subList(from, messages.size()));
    }

    private static Map<String, Object> state(Minecraft mc) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("user", mc.getUser().getName());
        state.put("screen", mc.screen == null ? null : typeName(mc.screen.getClass()));
        state.put("inWorld", mc.level != null && mc.player != null);
        if (mc.getCurrentServer() != null) state.put("server", mc.getCurrentServer().ip);
        else if (mc.hasSingleplayerServer()) state.put("server", "singleplayer");
        LocalPlayer player = mc.player;
        if (player != null && mc.level != null) {
            state.put("dimension", mc.level.dimension().location().toString());
            state.put("pos", List.of(round(player.getX()), round(player.getY()), round(player.getZ())));
            state.put("yaw", round(player.getYRot()));
            state.put("pitch", round(player.getXRot()));
            if (mc.gameMode != null) state.put("gameMode", mc.gameMode.getPlayerMode().getName());
            state.put("health", player.getHealth());
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = block.getBlockPos();
                Map<String, Object> target = new LinkedHashMap<>();
                target.put("block", String.valueOf(ForgeRegistries.BLOCKS.getKey(mc.level.getBlockState(pos).getBlock())));
                target.put("pos", List.of(pos.getX(), pos.getY(), pos.getZ()));
                target.put("face", block.getDirection().getName());
                state.put("target", target);
            } else if (hit instanceof EntityHitResult entity) {
                state.put("target", Map.of("entity", String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getEntity().getType()))));
            }
        }
        return state;
    }

    private static Map<String, Object> describeScreen(Minecraft mc) {
        Map<String, Object> out = new LinkedHashMap<>();
        Screen screen = mc.screen;
        if (screen == null) {
            out.put("open", false);
            return out;
        }
        out.put("type", typeName(screen.getClass()));
        out.put("title", screen.getTitle().getString());
        out.put("size", List.of(screen.width, screen.height));
        out.put("guiScale", mc.getWindow().getGuiScale());
        int offX = 0;
        int offY = 0;
        if (screen instanceof AbstractContainerScreen<?> container) {
            offX = container.getGuiLeft();
            offY = container.getGuiTop();
            out.put("guiOrigin", List.of(offX, offY));
            out.put("guiSize", List.of(container.getXSize(), container.getYSize()));
            List<Object> slots = new ArrayList<>();
            List<Slot> menuSlots = container.getMenu().slots;
            for (int i = 0; i < menuSlots.size(); i++) {
                Slot slot = menuSlots.get(i);
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("index", i);
                s.put("xy", List.of(offX + slot.x, offY + slot.y));
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    s.put("item", String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())));
                    s.put("count", stack.getCount());
                }
                slots.add(s);
            }
            out.put("slots", slots);
        }
        if (screen instanceof CUIScreen cui && cui.getPrimaryLayer() != null) {
            PrimaryLayer layer = cui.getPrimaryLayer();
            if (!(screen instanceof AbstractContainerScreen<?>)) {
                offX = (screen.width - layer.getWidth()) / 2;
                offY = (screen.height - layer.getHeight()) / 2;
            }
            out.put("cui", element(layer, offX, offY, 0));
        }
        List<Object> widgets = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget widget)) continue;
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("type", typeName(widget.getClass()));
            w.put("text", widget.getMessage().getString());
            w.put("rect", List.of(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()));
            if (!widget.active) w.put("active", false);
            if (!widget.visible) w.put("visible", false);
            widgets.add(w);
        }
        if (!widgets.isEmpty()) out.put("widgets", widgets);
        return out;
    }

    /** Coordinates are absolute GUI coordinates, i.e. directly usable with 'click'. */
    private static Map<String, Object> element(UIElement element, int offX, int offY, int depth) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", typeName(element.getClass()));
        try {
            String title = element.getTitle().getString();
            if (!title.isBlank()) out.put("title", title);
            out.put("rect", List.of(offX + element.getScreenX(), offY + element.getScreenY(), element.getWidth(),
                    element.getHeight()));
            if (!element.isEnabled()) out.put("enabled", false);
            if (!element.isVisible()) {
                out.put("visible", false);
                return out;
            }
            if (element instanceof UILayer layer && !layer.getElements().isEmpty()) {
                if (depth >= 16) {
                    out.put("children", "depth limit");
                    return out;
                }
                List<Object> children = new ArrayList<>();
                for (UIElement child : layer.getElements()) children.add(element(child, offX, offY, depth + 1));
                out.put("children", children);
            }
        } catch (RuntimeException e) {
            out.put("error", e.toString());
        }
        return out;
    }

    private static String typeName(Class<?> type) {
        String name = type.getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
