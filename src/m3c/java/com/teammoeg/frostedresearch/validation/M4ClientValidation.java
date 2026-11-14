package com.teammoeg.frostedresearch.validation;

import com.google.gson.GsonBuilder;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.client.dialog.ImageScreenDialog;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.archive.ResearchWorkspaceState;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedresearch.knowledge.*;
import com.teammoeg.frostedresearch.knowledge.client.*;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.experiment.PendingResearchProofs;
import com.teammoeg.frostedresearch.knowledge.experiment.PrototypeTuyereBlockEntity;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.nio.file.*;
import java.util.*;

/**
 * Opt-in, loaded-pack M4 client driver. Fixture materials/knowledge are explicit; this is not a survival run.
 * UI callbacks, inventory clicks, block use and observation requests travel over the real C2S paths.
 * Never calls recordMinigameWin, advanceStage, tryBlow or observeResearchTarget to manufacture success.
 */
public final class M4ClientValidation {
    private static final boolean ENABLED = Boolean.getBoolean("frostedresearch.m4Validation");
    private static final boolean DESK_ONLY = Boolean.getBoolean("frostedresearch.m4DeskOnly");
    private static final Path OUT = Path.of(System.getProperty("frostedresearch.m4Root", ".")).resolve("build/m4-client");
    private static final ResourceLocation PROJECT = id("the_winter_rescue:iron_bloomery");
    private static final ResourceLocation IDEA = id("the_winter_rescue:iron_smelting");
    private static final ResourceLocation PROTOTYPE = id("the_winter_rescue:research/prototype_tuyere");
    private static final ResourceLocation BLOOMERY = id("the_winter_rescue:research/main_bloomery");
    private static final ResourceLocation TUYERE = id("the_winter_rescue:research/sandy_tuyere");
    private static final BlockPos DESK = new BlockPos(0, 64, -2);
    private static final BlockPos BELLOWS = new BlockPos(-2, 64, 2);
    private static final BlockPos TARGET = BELLOWS.east();
    private static final BlockPos ORDINARY = new BlockPos(2, 64, 3);
    private static final int[] SOLUTION = {0, 6, 12, 10, 9, 3, 0, 0, 0};
    private static final long START = System.nanoTime();
    private static final Map<String, Object> checks = Collections.synchronizedMap(new LinkedHashMap<>());
    private static int phase, ticks, readingTicks, rotations, originalScale, originalFps;
    private static boolean captured, originalPause;
    private static volatile boolean busy, finished, bellowsArmed, blown, observationArmed, observed;
    private static volatile long clickAt = -1, observationAt = -1;
    private static volatile Throwable failure;
    private static long phaseStarted = START;
    private static String originalLanguage;
    private static UUID run;
    private static DrawDeskScreen desk;
    private static KnowledgeLayer layer;
    private static int[] pendingMasks;
    private static int[] reopenedMasks;
    private static java.util.concurrent.CompletableFuture<Void> reload;

    private static ResourceLocation id(String text) { return new ResourceLocation(text); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
    private static ServerPlayer player() { return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0); }
    private static KnowledgeService service() { return KnowledgeService.forPlayer(player()); }
    private static boolean allowed(ResourceLocation recipe) { return TechnologyAccessResolver.isRecipeUnlocked(CTeamDataManager.get(player()).getId(), recipe); }
    private static Block block(String name) { return Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(id(name)), name); }
    private static Item prototypeItem() { Block prototype = FRContents.Blocks.PROTOTYPE_TUYERE.get(); return prototype.asItem(); }
    private static void server(Runnable action) {
        busy = true;
        Minecraft.getInstance().getSingleplayerServer().execute(() -> {
            try { action.run(); } catch (Throwable error) { failure = error; } finally { busy = false; }
        });
    }
    private static Object read(Object object, String field) throws Exception {
        var member = object.getClass().getDeclaredField(field); member.setAccessible(true); return member.get(object);
    }
    private static void openProject() throws Exception {
        var method = KnowledgeLayer.class.getDeclaredMethod("openResearch", String.class);
        method.setAccessible(true); method.invoke(layer, PROJECT.toString());
    }
    private static KnowledgeJournalData data() { return new KnowledgeJournalData(KnowledgeClientState.snapshot()); }
    private static KnowledgeJournalData.Project project() { return data().projects.stream().filter(p -> p.id().equals(PROJECT.toString())).findFirst().orElseThrow(); }
    private static KnowledgeJournalData.Run current() { return Objects.requireNonNull(data().run(run.toString()), "client run"); }
    private static void next() throws Exception {
        phase++; ticks = readingTicks = 0; phaseStarted = System.nanoTime();
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve("progress.json"), "{\"phase\":" + phase + "}");
    }
    private static void shot(String name) throws Exception {
        Files.createDirectories(OUT);
        try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(OUT.resolve(name + ".png")); }
    }
    private static void press(JournalButton button) throws Exception {
        require(button.isEnabled(), "disabled UI button: " + button.getTitle().getString());
        ((Runnable) read(button, "action")).run();
    }
    private static JournalButton button(String key) {
        String title = Component.translatable("gui.frostedresearch.journal." + key).getString();
        var surface = desk.getAirflowPanel() == null ? layer : desk.getAirflowPanel();
        return surface.getElements().stream().filter(JournalButton.class::isInstance).map(JournalButton.class::cast)
            .filter(b -> b.getTitle().getString().equals(title)).findFirst().orElseThrow(() -> new IllegalStateException("missing UI button " + title));
    }
    private static void click(String key) throws Exception { press(button(key)); }
    private static void slot(int slot) {
        var mc = Minecraft.getInstance();
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 0, ClickType.PICKUP, mc.player);
    }
    private static void use(BlockPos pos, Direction face) {
        var mc = Minecraft.getInstance();
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
    }
    private static boolean openDesk() {
        var mc = Minecraft.getInstance();
        if (mc.screen == null) use(DESK, Direction.UP);
        if (mc.screen instanceof CUIScreen cui && cui.getPrimaryLayer() instanceof DrawDeskScreen view) {
            desk = view; return true;
        }
        return false;
    }
    private static void focusProject() throws Exception { desk.showKnowledge(); layer = desk.getKnowledgeLayer(); openProject(); }
    private static void look(BlockPos pos) {
        var p = Minecraft.getInstance().player; Vec3 delta = Vec3.atCenterOf(pos).subtract(p.getEyePosition());
        p.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        p.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
    }
    private static List<String> paragraphs() throws Exception {
        @SuppressWarnings("unchecked") var text = (List<Component>) read(read(layer, "reading"), "paragraphs");
        return text.stream().map(Component::getString).toList();
    }
    private static void translated(String value) {
        require(!value.isBlank() && !value.contains("gui.frostedresearch.") && !value.contains("knowledge.the_winter_rescue.")
            && !value.contains("knowledge.frostedresearch.") && !value.contains("尚未读懂"), "untranslated UI text: " + value);
    }
    private static void checkStageText() throws Exception {
        var stage = current().stage(); var content = paragraphs();
        for (var text : List.of(stage.title(), stage.objective(), stage.method())) {
            translated(text.getString()); require(content.contains(text.getString()), "stage text absent from reading: " + text.getString());
        }
        for (var conclusion : stage.conclusions()) {
            translated(conclusion.getString()); require(content.contains(conclusion.getString()), "conclusion absent from reading");
        }
        checks.put("stage_" + stage.id() + "_ready_" + stage.ready(), content);
    }
    /** Capture top and overlapping scroll pages; does not set a private scroll field. */
    private static boolean readingShots(String name) throws Exception {
        var mc = Minecraft.getInstance();
        if (readingTicks == 0) { shot(name + "-top"); GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 900, 350); }
        readingTicks++;
        if (readingTicks >= 10 && readingTicks <= 50 && readingTicks % 5 == 0) shot(name + "-scroll-" + ((readingTicks - 5) / 5));
        if (readingTicks >= 5 && readingTicks <= 45 && readingTicks % 5 == 0) mc.screen.mouseScrolled(450, 175, -1);
        return readingTicks >= 50;
    }
    private static void recipe(ResourceLocation id, String output, int ingredients) {
        var level = player().serverLevel(); var value = level.getRecipeManager().byKey(id).orElseThrow();
        require(ForgeRegistries.ITEMS.getKey(value.getResultItem(level.registryAccess()).getItem()).toString().equals(output), "recipe output " + id);
        require(value.getResultItem(level.registryAccess()).getCount() == 1 && value.getIngredients().stream().filter(i -> !i.isEmpty()).count() == ingredients, "recipe count/ingredients " + id);
        var aliases = level.getRecipeManager().getRecipes().stream().filter(r -> r.getResultItem(level.registryAccess()).is(value.getResultItem(level.registryAccess()).getItem())).map(r -> r.getId().toString()).sorted().toList();
        require(aliases.equals(List.of(id.toString())), "competing output recipe: " + aliases);
        checks.put("recipe_" + id.getPath(), Map.of("class", value.getClass().getName(), "ingredients", value.getIngredients().stream().map(i -> i.toJson().toString()).toList(), "aliases", aliases));
    }
    private static void prepare() {
        var p = player(); var level = p.serverLevel();
        FHScenario.get(p).pauseAct(); checks.put("fixture_scenario_paused", true);
        level.setDayTime(6000); level.setWeatherParameters(6000, 0, false, false);
        for (int x = -5; x <= 5; x++) for (int z = -4; z <= 7; z++) {
            level.setBlockAndUpdate(new BlockPos(x, 63, z), Blocks.STONE.defaultBlockState());
            for (int y = 64; y <= 68; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        recipe(PROTOTYPE, "frostedresearch:prototype_tuyere", 3);
        recipe(BLOOMERY, "charcoal_pit:main_bloomery", 9);
        recipe(TUYERE, "charcoal_pit:sandy_tuyere", 3);
        require(!allowed(PROTOTYPE) && !allowed(BLOOMERY) && !allowed(TUYERE), "fresh team recipe locks");
        checks.put("initial_all_three_locked", true);
        var s = service(); var source = new AcquisitionSource("client_fixture", Optional.of(p.getUUID()), level.getGameTime());
        s.produce(KnowledgeElement.idea(IDEA), source);
        for (var item : List.of(Items.RAW_IRON, Items.COPPER_INGOT, Items.COAL)) s.learn(ItemRetrieval.observe(p, new ItemStack(item)), source, KnowledgeService.Grant.NORMAL);
        p.teleportTo(.5, 64, .5); p.getInventory().clearContent();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FRContents.Blocks.DRAWING_DESK.get()));
        require(p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(DESK.below()), Direction.UP, DESK.below(), false))).consumesAction(), "desk placement");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        TeamResearchService.send(p, CTeamDataManager.get(p));
    }
    private static void materialsAndWorld() {
        var p = player(); var level = p.serverLevel();
        var ingredients = level.getRecipeManager().byKey(PROTOTYPE).orElseThrow().getIngredients();
        require(ingredients.size() == 3, "prototype is a three-material shapeless recipe");
        for (int i = 0; i < 3; i++) {
            var choices = ingredients.get(i).getItems(); require(choices.length > 0, "unresolved prototype material " + ingredients.get(i).toJson());
            var stack = choices[0].copy(); stack.setCount(1); p.getInventory().setItem(9 + i, stack);
        }
        p.getInventory().selected = 0; p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); p.inventoryMenu.broadcastChanges();
        Block bellows = block("charcoal_pit:bellows");
        level.setBlockAndUpdate(BELLOWS, bellows.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        level.setBlockAndUpdate(ORDINARY, block("charcoal_pit:sandy_tuyere").defaultBlockState());
        require(PendingResearchProofs.get(level).at(ORDINARY).isEmpty(), "ordinary reference has no trial proof");
    }
    private static void checkBinding() {
        var level = player().serverLevel();
        require(level.getBlockEntity(TARGET) instanceof PrototypeTuyereBlockEntity, "placed actual prototype BE");
        var binding = ((PrototypeTuyereBlockEntity) level.getBlockEntity(TARGET)).binding().orElseThrow();
        require(binding.team().equals(service().teamId()) && binding.run().equals(run) && binding.requirement().equals("observe_tuyere"), "placement bound current team/run/task");
        require(!level.getBlockState(TARGET).is(block("charcoal_pit:sandy_tuyere")), "prototype not converted before use");
        checks.put("actual_item_placement_bound", true);
    }
    @SubscribeEvent public static void clickedBellows(PlayerInteractEvent.RightClickBlock event) {
        if (!ENABLED || !bellowsArmed || !(event.getEntity() instanceof ServerPlayer p) || !event.getPos().equals(BELLOWS)) return;
        clickAt = p.level().getGameTime();
        try { checkBinding(); checks.put("bellows_real_c2s_interaction", true); } catch (Throwable error) { failure = error; }
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END || Minecraft.getInstance().getSingleplayerServer() == null) return;
        try {
            if (bellowsArmed && clickAt >= 0 && !blown) {
                var level = player().serverLevel(); long elapsed = level.getGameTime() - clickAt;
                if (level.getBlockState(TARGET).is(block("charcoal_pit:sandy_tuyere"))) {
                    require(elapsed >= 10, "use must not convert before the real delayed blow");
                    var proof = PendingResearchProofs.get(level).at(TARGET).orElseThrow();
                    require(proof.run().equals(run) && proof.team().equals(service().teamId()), "real blow proof binding");
                    require(level.getBlockEntity(TARGET) == null && !level.getBlockState(TARGET).hasProperty(BlockStateProperties.HORIZONTAL_FACING), "normal target remains the dependency block without invented BE/facing");
                    checks.put("bellows_delayed_server_ticks", elapsed); blown = true;
                } else { require(elapsed <= 100, "real bellows use did not convert prototype"); }
            }
            if (observationArmed && !observed && service().state().research().get(run).stageState().completedTasks().contains("observe_tuyere")) {
                long elapsed = player().level().getGameTime() - observationAt;
                require(elapsed >= 60, "fresh observation requires normal sampling time");
                require(PendingResearchProofs.get(player().serverLevel()).at(TARGET).isEmpty(), "fresh observation consumes pending proof");
                checks.put("fresh_observation_ticks_since_marker", elapsed); observed = true;
            }
        } catch (Throwable error) { failure = error; }
    }
    private static void observe(BlockPos pos) { FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.BLOCK, pos, -1)); }
    private static void discard() { FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.DISCARD)); }
    private static CompoundTag board() { return Objects.requireNonNull(desk.getAirflowPanel(), "desk airflow panel").board(); }
    private static void rotateNext() throws Exception {
        int[] masks = board().getIntArray("masks");
        if (masks.length != 9 || (pendingMasks != null && Arrays.equals(masks, pendingMasks))) return;
        pendingMasks = null;
        var cells = desk.getAirflowPanel().getElements().stream().filter(JournalButton.class::isInstance).map(JournalButton.class::cast)
            .filter(b -> b.getWidth() == 30 && b.getHeight() == 30).sorted(Comparator.comparingInt(JournalButton::getY).thenComparingInt(JournalButton::getX)).toList();
        require(cells.size() == 9, "actual airflow UI cells");
        for (int i = 0; i < 9; i++) if (masks[i] != SOLUTION[i]) { pendingMasks = masks.clone(); press(cells.get(i)); rotations++; return; }
        throw new IllegalStateException("canonical pipe path is not accepted as connected");
    }
    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            require(System.nanoTime() - START < 1_200_000_000_000L, "M4 total timeout");
            if (failure != null) throw new IllegalStateException("server fixture/monitor", failure);
            if (busy || mc.getOverlay() != null) return;
            if (phase >= 3) require(System.nanoTime() - phaseStarted < 120_000_000_000L, "M4 phase timeout " + phase);
            if (mc.screen instanceof ImageScreenDialog) {
                checks.put("fixture_intro_dialog_closed", true);
                Screen dialog = mc.screen;
                for (int i = 0; i < 5; i++) if (dialog.shouldCloseOnEsc()) { dialog.onClose(); break; }
                if (mc.getSingleplayerServer() != null && mc.player != null) server(() -> FHScenario.get(player()).pauseAct());
                return;
            }
            ticks++;
            switch (phase) {
                case 0 -> {
                    if (mc.level != null || mc.screen == null) return;
                    Files.deleteIfExists(OUT.resolve("result.json"));
                    captured = true; originalScale = mc.options.guiScale().get(); originalFps = mc.options.framerateLimit().get();
                    originalPause = mc.options.pauseOnLostFocus; originalLanguage = mc.options.languageCode;
                    mc.options.pauseOnLostFocus = false; mc.options.guiScale().set(2); mc.options.framerateLimit().set(60);
                    GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 800); mc.resizeDisplay();
                    mc.options.languageCode = "zh_cn"; mc.getLanguageManager().setSelected("zh_cn"); reload = mc.reloadResourcePacks(); next();
                }
                case 1 -> {
                    if (!reload.isDone()) return;
                    reload.join();
                    mc.createWorldOpenFlows().createFreshLevel("m4-client-" + System.currentTimeMillis(), new LevelSettings("M4 isolated validation", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(755111011717574879L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()); next();
                }
                case 2 -> { if (mc.player == null || mc.getSingleplayerServer() == null || mc.screen != null || ticks < 30) return; server(M4ClientValidation::prepare); next(); }
                case 3 -> { if (!openDesk()) return; focusProject(); next(); }
                case 4 -> {
                    if (ticks < 15) return; require(project().staged() && project().startStatus().equals("LEARNABLE"), "formal staged project start");
                    translated(project().title().getString()); require(project().accessConditions().size() == 4, "four formal start conditions");
                    project().accessConditions().forEach(c -> translated(c.title().getString()));
                    if (!DESK_ONLY && !readingShots("01-formal-entry")) return; click("research.start"); next();
                }
                case 5 -> {
                    if (ticks < 15 || project().openRun().isBlank()) return; run = UUID.fromString(project().openRun());
                    require(current().stage().id().equals("airflow") && !current().stage().ready(), "initial airflow stage"); if (!DESK_ONLY) checkStageText();
                    require(!button("research.advance_stage").isEnabled(), "advance disabled before win");
                    if (!DESK_ONLY && !readingShots("02-airflow-stage")) return; click("research.airflow_start"); next();
                }
                case 6 -> {
                    if (ticks < 15 || board().getIntArray("masks").length != 9) return;
                    require(!board().getBoolean("won"), "unsolved initial board"); shot("03-airflow-board");
                    if (DESK_ONLY) {
                        require(desk.getKnowledgeLayer() == null && desk.getWidth() == 387 && desk.getHeight() == 203, "old drawing-desk surface, not journal details");
                        var saved = board();
                        reopenedMasks = saved.getIntArray("masks");
                        desk.onKeyPressed(GLFW.GLFW_KEY_ESCAPE, 1, 0);
                        require(desk.getAirflowPanel() == null && desk.getKnowledgeLayer() == layer && (Boolean) read(layer,"projectDetails"), "Esc returns to same project details");
                        require((Integer) read(layer,"projectTab") == 2, "task tab retained");
                        click("research.airflow_start");
                        checks.put("reopened_board", saved.toString());
                    }
                    next();
                }
                case 7 -> {
                    if (ticks < 5) return;
                    if (reopenedMasks != null) {
                        if (board().getIntArray("masks").length != 9) return;
                        require(Arrays.equals(reopenedMasks,board().getIntArray("masks")), "reopening retains the server board");
                        reopenedMasks = null; checks.put("board_retained_on_reopen",true);
                    }
                    if (board().getBoolean("won")) {
                        if (!current().stage().ready()) return;
                        checks.put("actual_ui_rotations", rotations); shot("04-airflow-won"); next();
                    } else { rotateNext(); ticks = 0; }
                }
                case 8 -> {
                    if (ticks < 25) return; require(current().stage().id().equals("airflow"), "win does not auto-advance");
                    server(() -> { require(allowed(PROTOTYPE) && !allowed(BLOOMERY) && !allowed(TUYERE), "temporary-only access immediately after win"); checks.put("temporary_before_advance", true); });
                    click("research.airflow_back"); next();
                }
                case 9 -> { if (ticks < 15) return;
                    if (DESK_ONLY) {
                        require(desk.getAirflowPanel() == null && desk.getKnowledgeLayer() == layer && (Boolean) read(layer,"projectDetails"), "back button returns to same details after winning");
                        require(current().stage().id().equals("airflow") && current().stage().ready(), "server win recorded without auto advance");
                        checks.put("desk_only",true); checks.put("esc_and_button_return",true);
                        shot("05-returned-to-project-tasks"); finish(null); return;
                    }
                    checkStageText(); require(!current().stage().conclusions().isEmpty(), "airflow conclusion visible"); if (!readingShots("05-airflow-conclusion-and-advance")) return; click("research.advance_stage"); next(); }
                case 10 -> {
                    if (ticks < 15 || !current().stage().id().equals("trial")) return; checkStageText(); require(!current().stage().ready() && !button("research.complete").isEnabled(), "trial initially unfinished");
                    if (!readingShots("06-trial-stage")) return; mc.player.closeContainer(); server(M4ClientValidation::materialsAndWorld); next();
                }
                case 11 -> { if (ticks < 15 || mc.screen != null) return; mc.setScreen(new InventoryScreen(mc.player)); for (int i = 0; i < 3; i++) { slot(9 + i); slot(1 + i); } next(); }
                case 12 -> {
                    if (ticks < 20) return; require(mc.player.inventoryMenu.getSlot(0).getItem().is(prototypeItem()), "actual temporary recipe crafting result");
                    shot("07-real-prototype-crafting"); slot(0); slot(36); next();
                }
                case 13 -> { if (ticks < 15) return; require(mc.player.getMainHandItem().is(prototypeItem()), "crafted prototype in hand"); mc.player.closeContainer(); next(); }
                case 14 -> { if (ticks < 15 || mc.screen != null) return; look(TARGET); use(TARGET.below(), Direction.UP); next(); }
                case 15 -> { if (ticks < 20) return; require(mc.level.getBlockState(TARGET).is(FRContents.Blocks.PROTOTYPE_TUYERE.get()), "client placed prototype"); server(M4ClientValidation::checkBinding); next(); }
                case 16 -> { if (ticks < 10) return; shot("08-bound-prototype-before-use"); bellowsArmed = true; use(BELLOWS, Direction.UP); next(); }
                case 17 -> {
                    if (!blown || ticks < 15) return; require(mc.level.getBlockState(TARGET).is(block("charcoal_pit:sandy_tuyere")), "client actual normal tuyere");
                    shot("09-delayed-blow-converted"); require(!current().stage().ready(), "blow alone does not complete observation task"); look(ORDINARY); observe(ORDINARY); next();
                }
                case 18 -> {
                    if (ticks < 90) return; require(!current().stage().ready(), "fresh ordinary tuyere observation does not count");
                    server(() -> {
                        require(!service().state().research().get(run).stageState().completedTasks().contains("observe_tuyere"), "ordinary reference did not complete server task");
                        require(PendingResearchProofs.get(player().serverLevel()).at(TARGET).isPresent(), "ordinary reference did not consume proof");
                        checks.put("fresh_ordinary_observation_rejected_for_task", true);
                    });
                    discard(); next();
                }
                case 19 -> { if (ticks < 10) return; look(TARGET); server(() -> { observationAt = player().level().getGameTime(); observationArmed = true; }); next(); }
                case 20 -> { observe(TARGET); next(); }
                case 21 -> { if (!observed || ticks < 70 || !current().stage().ready()) return; shot("10-fresh-bound-observation-completed"); discard(); next(); }
                case 22 -> { if (ticks < 10 || !openDesk()) return; focusProject(); next(); }
                case 23 -> {
                    if (ticks < 20) return; require(current().stage().id().equals("trial") && current().stage().ready() && current().open(), "trial remains open until manual finish"); checkStageText();
                    require(current().stage().conclusions().size() == 2, "both stage conclusions visible");
                    if (!readingShots("11-trial-conclusions-and-finish")) return;
                    server(() -> require(allowed(PROTOTYPE) && !allowed(BLOOMERY) && !allowed(TUYERE), "temporary-only access before manual finish"));
                    click("research.complete"); next();
                }
                case 24 -> {
                    if (ticks < 20 || !current().status().equals("completed")) return; shot("12-normal-completion");
                    server(() -> { require(!allowed(PROTOTYPE) && allowed(BLOOMERY) && allowed(TUYERE), "final Design replaces temporary recipe with both permanent recipes"); checks.put("final_two_recipes_allowed_prototype_revoked", true); }); next();
                }
                case 25 -> {
                    server(() -> { var team = CTeamDataManager.get(player()); team.getData(FRSpecialDataTypes.RESEARCH_DATA).getData("iron").setFinished(true); LegacyResearchBridge.reconcile(team, service()); TeamResearchService.send(player(), team); }); next();
                }
                case 26 -> {
                    if (ticks < 20 || !project().startStatus().equals("LEGACY_COMPLETED")) return; openProject(); next();
                }
                case 27 -> {
                    if (ticks < 15) return; var method = KnowledgeLayer.class.getDeclaredMethod("researchState", KnowledgeJournalData.Project.class); method.setAccessible(true);
                    String status = ((Component) method.invoke(layer, project())).getString(); translated(status); require(status.equals("旧研究承接完成"), "Chinese legacy list status");
                    var lines = paragraphs(); lines.forEach(M4ClientValidation::translated); require(lines.contains(status), "Chinese legacy explanation page");
                    checks.put("legacy_status_text", status); checks.put("legacy_reading", lines); shot("13-chinese-legacy-completion");
                    desk.showTechTree(); var workspace = (ResearchWorkspaceState) read(desk, "workspaceState"); workspace.selectResearch("iron"); workspace.setProjectWorkspaceOpen(true); desk.refresh(); next();
                }
                case 28 -> { if (ticks < 20) return; shot("14-old-iron-entry"); LegacyResearchClient.open("iron"); layer = desk.getKnowledgeLayer(); require(PROJECT.toString().equals(read(layer, "focusedProject")), "old iron navigation"); next(); }
                case 29 -> { if (ticks < 15) return; shot("15-legacy-navigation"); finish(null); }
            }
        } catch (Throwable error) { finish(error); }
    }
    private static void finish(Throwable error) {
        if (finished) return; finished = true; var mc = Minecraft.getInstance();
        try {
            Files.createDirectories(OUT); checks.put("status", error == null ? "passed" : "failed"); checks.put("phase", phase);
            if (error != null) { Throwable root = error; while (root.getCause() != null) root = root.getCause(); checks.put("failure", root.toString()); }
            checks.put("mode", "formal-loaded-pack");
            checks.put("input", "UI callbacks and real inventory/block-use/observation C2S; fixture knowledge/materials; no physical keyboard/mouse or survival claim");
            checks.put("observation_scope", "BLOCK C2S and normal server sampling ticks; no synthetic record grant and no physical held-key/preview-UI claim");
            synchronized (checks) { Files.writeString(OUT.resolve("result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(checks)); }
            if (error == null) FRMain.LOGGER.info("M4_CLIENT_PASSED {}", OUT); else FRMain.LOGGER.error("M4_CLIENT_FAILED phase=" + phase, error);
        } catch (Exception logging) { FRMain.LOGGER.error("M4 client result", logging); }
        finally {
            if (captured) { mc.options.guiScale().set(originalScale); mc.options.framerateLimit().set(originalFps); mc.options.pauseOnLostFocus = originalPause; mc.options.languageCode = originalLanguage; mc.getLanguageManager().setSelected(originalLanguage); }
            mc.stop();
        }
    }
}
