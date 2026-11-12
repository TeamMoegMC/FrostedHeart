package com.teammoeg.frostedresearch.validation;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.teammoeg.chorda.client.cui.base.*;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.archive.ResearchWorkspaceState;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedresearch.knowledge.*;
import com.teammoeg.frostedresearch.knowledge.client.*;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.experiment.*;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

/** Opt-in client rendering and real C2S desk actions. Never part of the production JAR. */
@Mod.EventBusSubscriber(modid = FRMain.MODID, value = Dist.CLIENT)
public final class M3ClientValidation {
    private static final boolean ENABLED = Boolean.getBoolean("frostedresearch.m3cValidation");
    private static final boolean FORMAL = Boolean.getBoolean("frostedresearch.m3cFormal");
    private static final Path ROOT = Path.of(System.getProperty("frostedresearch.m3cRoot", "."));
    private static final Path OUT = ROOT.resolve("build/m3c-" + (FORMAL ? "formal" : "dev"));
    private static final ResourceLocation PROJECT = new ResourceLocation("the_winter_rescue:earthen_smelter");
    private static final ResourceLocation IDEA = new ResourceLocation("the_winter_rescue:earthen_smelting");
    private static final ResourceLocation RECIPE = new ResourceLocation("the_winter_rescue:research/alloybrick");
    private static final ResourceLocation MULTIBLOCK = new ResourceLocation("immersiveengineering:multiblocks/alloy_smelter");
    private static final BlockPos DESK = new BlockPos(0, 64, -2);
    private static final List<BlockPos> KILNS = List.of(new BlockPos(-2,64,4),new BlockPos(0,64,4),new BlockPos(2,64,4));
    private static final long START = System.nanoTime();
    private static final Map<String,Object> checks = new LinkedHashMap<>();
    private static int stage, ticks, originalScale, originalFps;
    private static boolean finished, originalPause, captured, readyCaptured;
    private static String originalLanguage;
    private static volatile boolean busy;
    private static volatile Throwable failure;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static UUID run;
    private static DrawDeskScreen desk;
    private static KnowledgeLayer layer;
    private static int inventorySampleSlot;

    private static ResourceLocation id(String value) { return new ResourceLocation(value); }
    private static void require(boolean yes, String text) { if (!yes) throw new IllegalStateException(text); }
    private static boolean formingEventAllowed() {
        var multiblock=blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.getByUniqueName(MULTIBLOCK);
        require(multiblock!=null,"actual IE alloy smelter registration");
        var hammer=new ItemStack(Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id("immersiveengineering:hammer"))));
        var event=new blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.MultiblockFormEvent(player(),multiblock,DESK,hammer);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }
    private static void next() { stage++; ticks = 0; }
    private static ServerPlayer player() { return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0); }
    private static KnowledgeService service() { return KnowledgeService.forPlayer(player()); }
    private static void server(Runnable action) {
        busy = true;
        Minecraft.getInstance().getSingleplayerServer().execute(() -> { try { action.run(); } catch (Throwable e) { failure=e; } finally { busy=false; } });
    }
    private static <T> T fixture(String path, Codec<T> codec) {
        try (var reader = new java.io.InputStreamReader(Objects.requireNonNull(M3ClientValidation.class.getResourceAsStream("/"+path+".json")), java.nio.charset.StandardCharsets.UTF_8)) {
            return codec.parse(JsonOps.INSTANCE,JsonParser.parseReader(reader)).getOrThrow(false, e -> {});
        } catch (Exception e) { throw new IllegalStateException(path,e); }
    }
    private static void prepare() {
        var p=player(); var level=p.serverLevel(); p.setGameMode(GameType.CREATIVE);
        level.setDayTime(6000); level.setWeatherParameters(6000,0,false,false);
        for(int x=-5;x<=5;x++) for(int z=-4;z<=7;z++) {
            level.setBlockAndUpdate(new BlockPos(x,63,z),Blocks.STONE.defaultBlockState());
            for(int y=64;y<=68;y++) level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        if(!FORMAL) {
            // Dev exercises the exact formal codec contract with a clearly labelled stand-in recipe target.
            var recipes=new ArrayList<>(level.getRecipeManager().getRecipes());
            recipes.removeIf(r -> r.getId().equals(RECIPE));
            recipes.add(new net.minecraft.world.item.crafting.ShapelessRecipe(RECIPE,"",net.minecraft.world.item.crafting.CraftingBookCategory.BUILDING,
                new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id("immersiveengineering:alloybrick")),4),
                NonNullList.of(net.minecraft.world.item.crafting.Ingredient.EMPTY,net.minecraft.world.item.crafting.Ingredient.of(Items.BRICK))));
            level.getRecipeManager().replaceRecipes(recipes);
            Map<ResourceLocation,IdeaDefinition> ideas=new HashMap<>();
            Map<ResourceLocation,ResultDefinition> results=new HashMap<>();
            Map<ResourceLocation,ProjectDefinition> projects=new HashMap<>();
            ideas.put(IDEA,fixture("m3b-formal-fixtures/ideas/earthen_smelting",IdeaDefinition.CODEC));
            for(String name:List.of("earthen_smelter_design","earthen_smelter_construction")) results.put(id("the_winter_rescue:"+name),fixture("m3b-formal-fixtures/results/"+name,ResultDefinition.CODEC));
            projects.put(PROJECT,fixture("m3b-formal-fixtures/projects/earthen_smelter",ProjectDefinition.CODEC));
            for(String name:List.of("i","j")) ideas.put(id("example:"+name),fixture("m1-fixtures/ideas/"+name,IdeaDefinition.CODEC));
            results.put(id("example:joint_finding"),fixture("m1-fixtures/results/joint_finding",ResultDefinition.CODEC));
            projects.put(id("example:joint_study"),fixture("m1-fixtures/projects/joint_study",ProjectDefinition.CODEC));
            var definitions=new KnowledgeDefinitions.Snapshot(0,ideas,results,Map.of(),Map.of(),projects,List.of());
            require(KnowledgeDefinitions.validate(definitions).isEmpty(),"dev fixture definitions invalid");
            KnowledgeDefinitions.install(definitions); LegacyResearchBridge.refreshDefinitions();
        }
        var recipe=level.getRecipeManager().byKey(RECIPE).orElseThrow(() -> new IllegalStateException("formal stable alloybrick recipe missing"));
        checks.put("recipe_id",recipe.getId().toString()); checks.put("recipe_class",recipe.getClass().getName());
        checks.put("recipe_ingredients",recipe.getIngredients().stream().map(i -> i.toJson().toString()).toList());
        checks.put("recipe_count",recipe.getResultItem(level.registryAccess()).getCount());
        if(FORMAL) {
            require(recipe.getResultItem(level.registryAccess()).getCount()==4,"alloybrick output count");
            require(recipe.getIngredients().stream().filter(i -> !i.isEmpty()).count()==8,"formal recipe needs four bricks and four plates");
            var ids=level.getRecipeManager().getRecipes().stream().filter(r -> r.getResultItem(level.registryAccess()).getItem()==recipe.getResultItem(level.registryAccess()).getItem()).map(r -> r.getId().toString()).toList();
            checks.put("alloybrick_recipe_ids",ids); require(ids.size()==1 && ids.contains(RECIPE.toString()),"unexpected competing alloybrick recipes");
        }
        var team=CTeamDataManager.get(p); var s=service();
        checks.put("recipe_before",TechnologyAccessResolver.isRecipeUnlocked(team.getId(),RECIPE));
        checks.put("forming_before",TechnologyAccessResolver.canFormMultiblock(team.getId(),MULTIBLOCK));
        require(!TechnologyAccessResolver.isRecipeUnlocked(team.getId(),RECIPE),"fresh team recipe should be locked");
        require(!TechnologyAccessResolver.canFormMultiblock(team.getId(),MULTIBLOCK),"fresh team forming should be locked");
        boolean formBefore=formingEventAllowed();checks.put("ie_form_event_before",formBefore);
        require(!formBefore,"actual IE form event should cancel before completion");
        s.produce(KnowledgeElement.idea(IDEA),new AcquisitionSource("client_fixture",Optional.of(p.getUUID()),level.getGameTime()));
        s.learn(ItemRetrieval.observe(p,new ItemStack(Items.CLAY_BALL)),new AcquisitionSource("client_fixture",Optional.empty(),0),KnowledgeService.Grant.NORMAL);
        if(!FORMAL) {
            s.learn(KnowledgeElement.idea(id("example:i")),new AcquisitionSource("client_fixture",Optional.empty(),0),KnowledgeService.Grant.NORMAL);
            s.learn(ItemRetrieval.observe(p,new ItemStack(Items.CHARCOAL)),new AcquisitionSource("client_fixture",Optional.empty(),0),KnowledgeService.Grant.NORMAL);
        }
        p.teleportTo(.5,64,.5); p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FRContents.Blocks.DRAWING_DESK.get()));
        require(p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(DESK.below()),Direction.UP,DESK.below(),false))).consumesAction(),"desk placement");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        List<net.minecraft.world.level.block.Block> blocks=List.of(FRContents.Blocks.TRIAL_KILN_CLAY_RICH.get(),FRContents.Blocks.TRIAL_KILN_SAND_RICH.get(),FRContents.Blocks.TRIAL_KILN_BALANCED.get());
        for(int i=0;i<3;i++) level.setBlockAndUpdate(KILNS.get(i),blocks.get(i).defaultBlockState());
        TeamResearchService.send(p,team);
    }
    private static void openDesk() {
        var mc=Minecraft.getInstance();
        if(mc.screen==null && mc.level.getBlockEntity(DESK) instanceof DrawingDeskTileEntity)
            mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(DESK),Direction.UP,DESK,false));
    }
    private static boolean deskReady() {
        if(Minecraft.getInstance().screen instanceof CUIScreen cui && cui.getPrimaryLayer() instanceof DrawDeskScreen view) {desk=view; return true;} return false;
    }
    private static Object read(Object target,String name) throws Exception { var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target); }
    private static void invoke(Object target,String name) throws Exception {var m=target.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(target);}
    private static void openProject(String value) throws Exception {var m=KnowledgeLayer.class.getDeclaredMethod("openResearch",String.class);m.setAccessible(true);m.invoke(layer,value);}
    private static void button(String key) throws Exception {
        String title=Component.translatable("gui.frostedresearch.journal."+key).getString();
        JournalButton button=layer.getElements().stream().filter(JournalButton.class::isInstance).map(JournalButton.class::cast).filter(b -> b.getTitle().getString().equals(title)).findFirst().orElseThrow(() -> new IllegalStateException("button missing "+title));
        require(button.isEnabled(),"disabled button "+title); ((Runnable)read(button,"action")).run();
    }
    private static KnowledgeJournalData.Project project() {return new KnowledgeJournalData(KnowledgeClientState.snapshot()).projects.stream().filter(p -> p.id().equals(PROJECT.toString())).findFirst().orElseThrow();}
    private static void shot(String name) throws Exception {Files.createDirectories(OUT);try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUT.resolve(name+".png"));}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!ENABLED||finished||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        try {
            require(System.nanoTime()-START<1_200_000_000_000L,"M3 client timeout at stage "+stage);
            if(failure!=null)throw new IllegalStateException("server action",failure);
            if(busy||mc.getOverlay()!=null)return;
            ticks++;
            switch(stage) {
                case 0 -> {
                    if(mc.level!=null||mc.screen==null)return;
                    Files.deleteIfExists(OUT.resolve("result.json"));
                    captured=true; originalScale=mc.options.guiScale().get();originalFps=mc.options.framerateLimit().get();originalPause=mc.options.pauseOnLostFocus;originalLanguage=mc.options.languageCode;
                    mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);mc.options.framerateLimit().set(60);
                    GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),1280,800);mc.resizeDisplay();
                    mc.options.languageCode="zh_cn";mc.getLanguageManager().setSelected("zh_cn"); reload=mc.reloadResourcePacks(); next();
                }
                case 1 -> {
                    if(!reload.isDone())return;
                    mc.createWorldOpenFlows().createFreshLevel("m3c-"+(FORMAL?"formal-":"dev-")+System.currentTimeMillis(),new LevelSettings("M3 isolated validation",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                        new WorldOptions(755111011717574879L,false,false),registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());next();
                }
                case 2 -> {if(mc.player==null||mc.getSingleplayerServer()==null||mc.screen!=null||ticks<30)return;server(M3ClientValidation::prepare);next();}
                case 3 -> {openDesk();if(!deskReady())return;desk.showKnowledge();layer=desk.getKnowledgeLayer();openProject(FORMAL?PROJECT.toString():"example:joint_study");next();}
                case 4 -> {if(ticks<20)return;
                    if(!FORMAL){var p=new KnowledgeJournalData(KnowledgeClientState.snapshot()).projects.stream().filter(x->x.id().equals("example:joint_study")).findFirst().orElseThrow();require(p.accessConditions().size()==5&&p.accessConditions().stream().filter(KnowledgeJournalData.AccessCondition::hidden).count()==2,"J/C individual placeholders");shot("01-m1-two-hidden");}
                    openProject(PROJECT.toString());next();}
                case 5 -> {if(ticks<15)return;require(project().startStatus().equals("NOT_UNDERSTOOD"),"missing sand status");shot("02-missing-sand");server(()->service().learn(ItemRetrieval.observe(player(),new ItemStack(Items.SAND)),new AcquisitionSource("client_fixture",Optional.empty(),0),KnowledgeService.Grant.NORMAL));next();}
                case 6 -> {
                    if(ticks<20||!project().startStatus().equals("LEARNABLE"))return;
                    if(!readyCaptured) {
                        readyCaptured=true;ticks=20;
                        shot("03-ready");
                        if(FORMAL) {
                            checks.put("translated_project_title",project().title().getString());
                            checks.put("translated_project_body",project().body().getString());
                            var titles=project().accessConditions().stream().map(c->c.title().getString()).toList();
                            checks.put("translated_access_conditions",titles);
                            require(project().title().getString().equals("土制冶炼炉") && project().body().getString().contains("三、提交"),"formal project translations");
                            require(titles.size()==3 && titles.stream().noneMatch(t->t.contains("尚未读懂")||t.startsWith("knowledge.")),"formal three condition translations");
                            GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),900,350);
                        }
                    }
                    if(FORMAL) {
                        if(ticks>=30 && ticks<=70 && ticks%5==0) shot("03-ready-scroll-"+((ticks-25)/5));
                        if(ticks>=25 && ticks<=65 && ticks%5==0) mc.screen.mouseScrolled(450,175,-1);
                        if(ticks<71)return;
                        checks.put("reading_scroll_lines",read(read(layer,"reading"),"scroll"));
                        require((int)read(read(layer,"reading"),"scroll")>0,"formal long reading scroll");
                    }
                    button("research.start");next();}
                case 7 -> {if(ticks<20||project().openRun().isBlank())return;run=UUID.fromString(project().openRun());shot("04-started");mc.player.closeContainer();next();}
                case 8 -> {if(ticks<20||mc.screen!=null)return;mc.player.setYRot(0);mc.player.setXRot(17);shot("05-three-raw-models");server(()->{for(var pos:KILNS){player().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,4));var state=player().level().getBlockState(pos);state.use(player().level(),player(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}});next();}
                case 9 -> {if(ticks<20)return;shot("06-three-burning-models");next();}
                case 10 -> {if(ticks<410)return;require(mc.level.getBlockState(KILNS.get(2)).getValue(TrialKilnBlock.PHASE)==2,"intact client model state");require(mc.level.getBlockState(KILNS.get(0)).getValue(TrialKilnBlock.PHASE)==3,"broken client model state");shot("07-two-bad-one-good");server(()->{player().setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);var pos=KILNS.get(2);player().level().getBlockState(pos).use(player().level(),player(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));});next();}
                case 11 -> {if(ticks<20)return;openDesk();if(!deskReady())return;desk.showKnowledge();layer=desk.getKnowledgeLayer();button("tools");next();}
                case 12 -> {if(ticks<15)return;inventorySampleSlot=-1;for(int i=3;i<mc.player.containerMenu.slots.size();i++)if(mc.player.containerMenu.getSlot(i).getItem().is(FRContents.Items.FIRED_TRIAL_KILN.get())){inventorySampleSlot=i;break;}require(inventorySampleSlot>=0,"sample inventory slot");mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,inventorySampleSlot,0,ClickType.PICKUP,mc.player);next();}
                case 13 -> {if(ticks<10)return;mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,2,0,ClickType.PICKUP,mc.player);next();}
                case 14 -> {if(ticks<15)return;require(mc.player.containerMenu.getSlot(2).getItem().is(FRContents.Items.FIRED_TRIAL_KILN.get()),"real examination-slot sample");shot("08-tools-actual-sample-slot");layer.goBack();openProject(PROJECT.toString());next();}
                case 15 -> {if(ticks<10)return;shot("09-before-submit");button("research.sample.submit");next();}
                case 16 -> {if(ticks<20)return;require(mc.player.containerMenu.getSlot(2).getItem().isEmpty(),"server consumed physical slot");shot("10-sample-done");button("research.complete");next();}
                case 17 -> {if(ticks<25)return;shot("11-completed");server(()->{var team=CTeamDataManager.get(player());checks.put("recipe_after",TechnologyAccessResolver.isRecipeUnlocked(team.getId(),RECIPE));checks.put("forming_after",TechnologyAccessResolver.canFormMultiblock(team.getId(),MULTIBLOCK));require(TechnologyAccessResolver.isRecipeUnlocked(team.getId(),RECIPE)&&TechnologyAccessResolver.canFormMultiblock(team.getId(),MULTIBLOCK),"both real access gates after normal completion");boolean formAfter=formingEventAllowed();checks.put("ie_form_event_after",formAfter);require(formAfter,"actual IE form event should allow after completion");
                        for(var result:KnowledgeDefinitions.current().projects().get(PROJECT).results())service().forget(KnowledgeKey.result(result));
                        UUID retained=service().startResearch(PROJECT).orElseThrow();team.getData(FRSpecialDataTypes.RESEARCH_DATA).getData("alloy_kiln").setFinished(true);LegacyResearchBridge.reconcile(team,service());require(service().state().research().get(retained).open(),"legacy takeover retains server history");checks.put("retained_run",retained.toString());TeamResearchService.send(player(),team);});next();}
                case 18 -> {if(ticks<25||!project().startStatus().equals("LEGACY_COMPLETED"))return;var retained=new KnowledgeJournalData(KnowledgeClientState.snapshot()).run((String)checks.get("retained_run"));require(retained!=null && retained.status().equals("legacy_superseded"),"retained run projects as superseded");openProject(PROJECT.toString());shot("12-legacy-completed-with-open-run");checks.put("legacy_list_status",invokeStatus());desk.showTechTree();var state=(ResearchWorkspaceState)read(desk,"workspaceState");state.selectResearch("alloy_kiln");state.setProjectWorkspaceOpen(true);desk.refresh();next();}
                case 19 -> {if(ticks<20)return;shot("13-old-entry-replacement");LegacyResearchClient.open("alloy_kiln");layer=desk.getKnowledgeLayer();require(layer!=null && PROJECT.toString().equals(read(layer,"focusedProject")),"old replacement navigation callback");checks.put("old_navigation_callback",true);next();}
                case 20 -> {if(ticks<15)return;shot("14-knowledge-navigation");finish(null);}
            }
        }catch(Throwable e){finish(e);}
    }
    private static String invokeStatus() throws Exception {
        var method=KnowledgeLayer.class.getDeclaredMethod("researchState",KnowledgeJournalData.Project.class);method.setAccessible(true);
        String text=((Component)method.invoke(layer,project())).getString();checks.put("legacy_status_text",text);return text;
    }
    private static void finish(Throwable error) {
        if(finished)return;finished=true;var mc=Minecraft.getInstance();
        try {Files.createDirectories(OUT);checks.put("status",error==null?"passed":"failed");checks.put("mode",FORMAL?"formal-loaded-pack":"dev-formal-codec-with-stand-in-recipe");checks.put("stage",stage);checks.put("failure",error==null?null:error.toString());checks.put("input","JournalButton callbacks and actual Minecraft container C2S; no physical keyboard/mouse");Files.writeString(OUT.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(checks));if(error!=null)FRMain.LOGGER.error("M3C_FAILED stage="+stage,error);else FRMain.LOGGER.info("M3C_PASSED {}",OUT);
        }catch(Exception e){FRMain.LOGGER.error("M3C result",e);}finally {if(captured){mc.options.guiScale().set(originalScale);mc.options.framerateLimit().set(originalFps);mc.options.pauseOnLostFocus=originalPause;mc.options.languageCode=originalLanguage;mc.getLanguageManager().setSelected(originalLanguage);}mc.stop();}
    }
}
