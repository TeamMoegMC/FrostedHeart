package com.teammoeg.frostedresearch.validation;

import com.google.gson.GsonBuilder;
import com.sun.jna.*;
import com.sun.jna.win32.StdCallLibrary;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.client.dialog.ImageScreenDialog;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.api.*;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedresearch.compat.JEICompat;
import com.teammoeg.frostedresearch.compat.ControlledFiringCategory;
import com.teammoeg.frostedresearch.knowledge.*;
import com.teammoeg.frostedresearch.knowledge.client.*;
import com.teammoeg.frostedresearch.knowledge.client.observation.ObservationRecordScreen;
import com.teammoeg.frostedresearch.knowledge.client.ui.*;
import com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding;
import com.teammoeg.frostedresearch.knowledge.furnace.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.*;

import java.nio.file.*;
import java.util.*;

/** Full M5 route, optionally followed by the final M6 brief in the same Minecraft process. */
public final class M5ClientValidation {
    private static final boolean ENABLED=Boolean.getBoolean("frostedresearch.m5Validation");
    private static final Path ROOT=Path.of(System.getProperty("frostedresearch.m5Root","build/m5-client"));
    private static final ResourceLocation PROJECT=id("the_winter_rescue:refractory_brick"), IDEA=id("the_winter_rescue:refractory_firing"),
        RESULT=id("the_winter_rescue:controlled_refractory_production"), BATCH=id("the_winter_rescue:research/refractory_brick_batch");
    private static final BlockPos DESK=new BlockPos(0,64,-2), TABLE=new BlockPos(2,64,-2), FURNACE=new BlockPos(2,64,1);
    private static final long CHAPTER=0x4A8E1F92C73B605DL;
    private static final long[] QUESTS={0x507C594CBE4EDD30L,0x56B3257EE1F12A5BL,0x564A135A9DC0E980L,0x594A74A3D9A1A554L,0x5A6E8872E26A30E3L};
    private static final Map<String,Object> facts=Collections.synchronizedMap(new LinkedHashMap<>());
    private static final long START=System.nanoTime();
    private static int phase,ticks,originalScale,observationReturn,craftReturn,craftDestination,questIndex,jeiReturn,readingReturn;
    private static String originalLanguage,observationLabel,craftRecipe,defaultMethod,readingShot;
    private static ResourceLocation observationItem;
    private static KnowledgeKey observationKey;
    private static boolean originalPause,captured,finished,keepObservation,keyHeld,jeiUnlocked,delegated;
    private static volatile boolean busy,initialChecked,serverChecked;
    private static volatile Throwable failure;
    private static volatile long linkStart,linkEnd;
    private static UUID run,firstRun;
    private static DrawDeskScreen desk;
    private static KnowledgeLayer layer;
    private static QuestScreen questScreen;
    private static java.util.concurrent.CompletableFuture<Void> reload;

    public interface Input extends StdCallLibrary {
        Input INSTANCE=Native.load("user32",Input.class);
        boolean SetForegroundWindow(Pointer hwnd);
        boolean PostMessageW(Pointer hwnd,int msg,Pointer wp,Pointer lp);
    }
    private static Minecraft mc(){return Minecraft.getInstance();}
    private static ServerPlayer player(){return Objects.requireNonNull(mc().getSingleplayerServer()).getPlayerList().getPlayer(mc().player.getUUID());}
    private static KnowledgeService service(){return KnowledgeService.forPlayer(player());}
    private static ResourceLocation id(String text){return new ResourceLocation(text);}
    private static Item item(String text){return Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(id(text)));}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static Object read(Object object,String field)throws Exception{var f=object.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(object);}
    private static void save(String name,Object value)throws Exception{Files.createDirectories(ROOT);Files.writeString(ROOT.resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(value));}
    private static void go(int value)throws Exception{phase=value;ticks=0;save("progress.json",Map.of("phase",phase));}
    private static void next()throws Exception{go(phase+1);}
    private static void onServer(Runnable action){busy=true;mc().getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable e){failure=e;}finally{busy=false;}});}
    private static void shot(String name)throws Exception{Files.createDirectories(ROOT);try(var image=Screenshot.takeScreenshot(mc().getMainRenderTarget())){image.writeToFile(ROOT.resolve(name+".png"));}}
    private static KnowledgeJournalData data(){return new KnowledgeJournalData(KnowledgeClientState.snapshot());}
    private static KnowledgeJournalData.Project project(){return data().projects.stream().filter(p->p.id().equals(PROJECT.toString())).findFirst().orElseThrow();}
    private static KnowledgeJournalData.Run current(){return run==null?null:data().run(run.toString());}
    private static boolean archived(KnowledgeKey key){return data().archive.stream().anyMatch(e->e.key().equals(key)&&e.active());}
    private static boolean allowed(){return ClientKnowledgeDataAPI.technologyProjection().recipe(BATCH).allowed();}
    private static void use(BlockPos pos){mc().gameMode.useItemOn(mc().player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    private static void close(){if(mc().player!=null)mc().player.closeContainer();}
    private static boolean openDesk(){if(mc().screen==null)use(DESK);if(mc().screen instanceof CUIScreen cui&&cui.getPrimaryLayer() instanceof DrawDeskScreen d){desk=d;desk.showKnowledge();layer=desk.getKnowledgeLayer();return true;}return false;}
    private static void focus()throws Exception{var m=KnowledgeLayer.class.getDeclaredMethod("openResearch",String.class);m.setAccessible(true);m.invoke(layer,PROJECT.toString());}
    private static boolean projectView()throws Exception{if(!openDesk())return false;if(!PROJECT.toString().equals(read(layer,"focusedProject"))){focus();ticks=0;return false;}return ticks>=15;}
    private static JournalButton button(UILayer root,String title){return root.getElements().stream().filter(JournalButton.class::isInstance).map(JournalButton.class::cast).filter(b->b.getTitle().getString().equals(title)).findFirst().orElseThrow(()->new IllegalStateException("missing button "+title+" phase "+phase));}
    private static void press(JournalButton button)throws Exception{require(button.isEnabled(),"disabled "+button.getTitle().getString());((Runnable)read(button,"action")).run();}
    private static void click(String key)throws Exception{press(button(layer,Component.translatable("gui.frostedresearch.journal."+key).getString()));}
    private static void select(KnowledgeKey key)throws Exception{
        var list=(JournalList)read(layer,"list");var method=KnowledgeLayer.class.getDeclaredMethod("rowClicked",JournalList.Row.class,boolean.class);method.setAccessible(true);
        Set<Object> tried=new HashSet<>();
        for(int i=0;i<20;i++){
            @SuppressWarnings("unchecked") var rows=(List<JournalList.Row>)read(list,"rows");
            var target=rows.stream().filter(r->key.equals(r.value())).findFirst();if(target.isPresent()){method.invoke(layer,target.get(),false);return;}
            var group=rows.stream().filter(r->r.group()&&!tried.contains(r.value())).findFirst();require(group.isPresent(),"observation inbox row "+key);tried.add(group.get().value());method.invoke(layer,group.get(),false);
        }throw new IllegalStateException("row group traversal");
    }
    private static ControlledFurnaceMenu furnaceMenu(){return (ControlledFurnaceMenu)mc().player.containerMenu;}
    private static ControlledFurnaceBlockEntity furnace(){return furnaceMenu().getBlock();}
    private static boolean openFurnace(){if(mc().screen==null)use(FURNACE);return mc().player.containerMenu instanceof ControlledFurnaceMenu;}
    private static void furnaceClick(String key)throws Exception{require(mc().screen instanceof CUIScreen,"furnace screen");press(button(((CUIScreen)mc().screen).getPrimaryLayer(),Component.translatable("gui.frostedresearch.furnace."+key).getString()));}
    private static void slot(int slot){mc().gameMode.handleInventoryMouseClick(mc().player.containerMenu.containerId,slot,0,ClickType.PICKUP,mc().player);}
    private static void move(int from,int to){slot(from);slot(to);}
    private static void nativeN(boolean down){
        long window=mc().getWindow().getWindow();Pointer hwnd=Pointer.createConstant(GLFWNativeWin32.glfwGetWin32Window(window));
        if(down){GLFW.glfwFocusWindow(window);Input.INSTANCE.SetForegroundWindow(hwnd);}
        require(Input.INSTANCE.PostMessageW(hwnd,down?0x100:0x101,Pointer.createConstant(0x4E),Pointer.createConstant(down?0x00310001L:0xC0310001L)),"native N message");keyHeld=down;
    }
    private static void observe(String label,String target,boolean learn,int after,boolean supplied)throws Exception{
        observationLabel=label;observationItem=id(target);keepObservation=learn;observationReturn=after;observationKey=null;close();
        if(supplied)onServer(()->{player().getInventory().selected=0;player().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item(target)));player().inventoryMenu.broadcastChanges();});
        go(600);
    }
    private static void observationTick()throws Exception{
        switch(phase){
            case 600->{if(ticks<20||mc().screen!=null||!mc().player.getMainHandItem().is(item(observationItem.toString())))return;mc().player.setXRot(-85);mc().player.setYRot(180);go(601);}
            case 601->{if(ticks<15)return;require(mc().hitResult==null||mc().hitResult.getType()==HitResult.Type.MISS,"held observation aims at sky");nativeN(true);go(602);}
            case 602->{require(ticks<600,"normal observation preview timeout "+observationLabel+", active="+mc().isWindowActive()+", screen="+mc().screen);if(!(mc().screen instanceof ObservationRecordScreen record))return;nativeN(false);var observation=(Observation)read(record.getPrimaryLayer(),"observation");require(observation.type()==Observation.Type.ITEM&&observation.object().equals(observationItem),"real item preview identity");observationKey=KnowledgeKey.observation(observation.recordId());facts.put(observationLabel+"_preview_record",observation.recordId().toString());go(603);}
            case 603->{if(ticks<15)return;shot(observationLabel+"-preview");var root=((CUIScreen)mc().screen).getPrimaryLayer();press(button(root,Component.translatable("gui.frostedresearch.journal.observation."+(keepObservation?"keep":"discard")).getString()));go(604);}
            case 604->{if(ticks<15)return;if(!keepObservation){if(mc().screen==null)go(observationReturn);return;}if(mc().screen instanceof ObservationRecordScreen)return;if(!openDesk())return;if(!data().inbox.stream().anyMatch(e->e.key().equals(observationKey)))return;click("page.inbox");select(observationKey);go(605);}
            case 605->{if(ticks<15)return;shot(observationLabel+"-inbox");click("learn");go(606);}
            case 606->{if(ticks<15||!archived(observationKey))return;facts.put(observationLabel+"_kept_and_learned",true);shot(observationLabel+"-learned");close();go(observationReturn);}
        }
    }
    private static void craft(String recipe,int destination,int after)throws Exception{
        craftRecipe=recipe;craftDestination=destination;craftReturn=after;close();
        onServer(()->{var p=player();var ingredients=p.serverLevel().getRecipeManager().byKey(id(recipe)).orElseThrow().getIngredients();require(ingredients.size()==9,"nine real crafting ingredients");for(int i=0;i<9;i++){var choices=ingredients.get(i).getItems();require(choices.length>0,"resolved crafting ingredient");var stack=choices[0].copy();stack.setCount(1);p.getInventory().setItem(9+i,stack);}p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});go(700);
    }
    private static void craftTick()throws Exception{
        switch(phase){
            case 700->{if(ticks<15||mc().screen!=null)return;use(TABLE);go(701);}
            case 701->{if(ticks<15||!(mc().player.containerMenu instanceof CraftingMenu))return;for(int i=0;i<9;i++)move(10+i,1+i);go(702);}
            case 702->{if(ticks<20)return;var expected=mc().getSingleplayerServer().getRecipeManager().byKey(id(craftRecipe)).orElseThrow().getResultItem(mc().level.registryAccess());require(mc().player.containerMenu.getSlot(0).getItem().is(expected.getItem()),"actual crafting output "+craftRecipe);shot(craftRecipe.endsWith("hand_mix")?"14-real-hand-mix":"15-real-furnace-crafting");move(0,37+craftDestination);go(703);}
            case 703->{if(ticks<15)return;facts.put("crafted_"+craftRecipe,true);close();go(craftReturn);}
        }
    }
    private static void fixture(){
        var p=player();var level=p.serverLevel();require(service().state().research().isEmpty()&&!service().query(KnowledgeKey.idea(IDEA)).inArchive(),"fresh team without idea or research");
        FHScenario.get(p).pauseAct();p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.teleportTo(.5,64,.5);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        for(int x=-5;x<=5;x++)for(int z=-4;z<=5;z++){level.setBlockAndUpdate(new BlockPos(x,63,z),Blocks.STONE.defaultBlockState());for(int y=64;y<=68;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FRContents.Blocks.DRAWING_DESK.get()));
        require(p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(DESK.below()),Direction.UP,DESK.below(),false))).consumesAction(),"fixture drawing desk");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);level.setBlockAndUpdate(TABLE,Blocks.CRAFTING_TABLE.defaultBlockState());TeamResearchService.send(p,CTeamDataManager.get(p));initialChecked=true;
    }
    private static void supplyBatch(int count,int fuel){var p=player();p.getInventory().setItem(9,new ItemStack(item("frostedheart:fire_clay_ball"),count));p.getInventory().setItem(10,new ItemStack(Items.CHARCOAL,fuel));p.inventoryMenu.broadcastChanges();p.containerMenu.broadcastChanges();}
    private static long rewards(){return service().state().acquisitions().stream().filter(e->e.element().key().equals(KnowledgeKey.result(RESULT))).count();}
    private static void jei(boolean unlocked,int after)throws Exception{jeiUnlocked=unlocked;jeiReturn=after;close();go(800);}
    private static void jeiTick()throws Exception{
        if(phase==800){
            if(ticks<20||JEICompat.jei==null||JEICompat.man==null)return;
            var visible=JEICompat.man.createRecipeLookup(ControlledFiringCategory.TYPE).get().map(ControlledFiringRecipe::id).toList();
            require(visible.contains(id("the_winter_rescue:research/refractory_brick_trial"))&&visible.contains(BATCH)==jeiUnlocked,"JEI actual lookup follows trial/production access");
            require(JEICompat.man.createRecipeLookup(ControlledFiringCategory.TYPE).includeHidden().get().anyMatch(r->r.id().equals(BATCH)),"production is registered even when hidden");
            facts.put(jeiUnlocked?"jei_after_design_visible":"jei_before_design_visible",visible.stream().map(Object::toString).toList());
            JEICompat.jei.getRecipesGui().showTypes(List.of(ControlledFiringCategory.TYPE));go(801);
        }else if(phase==801&&ticks>=20){shot(jeiUnlocked?"21a-jei-trial-and-unlocked-batch":"16a-jei-trial-only-before-design");mc().setScreen(null);go(jeiReturn);}
    }
    private static void stageShot(String name,int after)throws Exception{
        readingShot=name;readingReturn=after;var reading=(JournalText)read(layer,"reading");
        double scale=mc().getWindow().getGuiScale();GLFW.glfwSetCursorPos(mc().getWindow().getWindow(),(reading.getScreenX()+reading.getWidth()/2.0)*scale,(reading.getScreenY()+reading.getHeight()/2.0)*scale);go(900);
    }
    private static void readingTick()throws Exception{
        if(phase==900&&ticks>=5){var reading=(JournalText)read(layer,"reading");@SuppressWarnings("unchecked") var paragraphs=(List<Component>)read(reading,"paragraphs");int line=0;String method=current().stage().method().getString();for(var paragraph:paragraphs){if(paragraph.getString().equals(method))break;line+=mc().font.split(paragraph,Math.max(1,reading.getWidth()-7)).size()+1;}for(int i=0;i<Math.max(0,(line-2)/3);i++)require(reading.onMouseScrolled(-1),"actual reading scroll hover");go(901);
        }else if(phase==901&&ticks>=15){shot(readingShot);go(readingReturn);}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!ENABLED||finished||delegated||event.phase!=TickEvent.Phase.END)return;
        try{
            require(System.nanoTime()-START<2_400_000_000_000L,"M5 client timeout phase "+phase);if(failure!=null)throw new IllegalStateException("server fixture/assertion",failure);if(busy||mc().getOverlay()!=null)return;
            if(mc().screen instanceof ImageScreenDialog){Screen screen=mc().screen;for(int i=0;i<5;i++)if(screen.shouldCloseOnEsc()){screen.onClose();break;}return;}
            ticks++;if(phase>=600&&phase<700){observationTick();return;}if(phase>=700&&phase<800){craftTick();return;}if(phase>=800&&phase<900){jeiTick();return;}if(phase>=900){readingTick();return;}
            switch(phase){
                case 0->{if(mc().level!=null||mc().screen==null)return;captured=true;originalScale=mc().options.guiScale().get();originalLanguage=mc().options.languageCode;originalPause=mc().options.pauseOnLostFocus;mc().options.pauseOnLostFocus=false;mc().options.guiScale().set(2);GLFW.glfwSetWindowSize(mc().getWindow().getWindow(),1280,800);mc().resizeDisplay();mc().options.languageCode="zh_cn";mc().getLanguageManager().setSelected("zh_cn");reload=originalLanguage.equals("zh_cn")?java.util.concurrent.CompletableFuture.completedFuture(null):mc().reloadResourcePacks();next();}
                case 1->{if(!reload.isDone())return;reload.join();mc().createWorldOpenFlows().createFreshLevel("m5-entry-"+System.currentTimeMillis(),new LevelSettings("M5 normal entry and controlled furnace",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(755111011717574880L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());next();}
                case 2->{if(mc().player==null||mc().getSingleplayerServer()==null||ticks<40)return;if(mc().screen!=null){mc().screen.onClose();return;}onServer(M5ClientValidation::fixture);next();}
                case 3->{if(!initialChecked||ticks<20||!openDesk())return;require(!archived(KnowledgeKey.idea(IDEA)),"no acquired idea before linking");facts.put("fresh_team_no_idea",true);click("page.research");next();}
                case 4->{if(ticks<15)return;shot("01-fresh-no-idea");click("page.archive");click("link.try");next();}
                case 5->{if(ticks<15)return;@SuppressWarnings("unchecked") var candidates=(List<net.minecraft.nbt.CompoundTag>)read(layer,"candidates");if(candidates.isEmpty())return;int index=(int)read(layer,"candidateIndex");if(!candidates.get(index).getString("rule").equals(IDEA.toString())){press(button(layer,"›"));ticks=0;return;}require(((Set<?>)read(layer,"selected")).isEmpty(),"zero-input candidate explicitly selected");shot("02-refractory-link-candidate");click("link.accept");onServer(()->linkStart=player().serverLevel().getGameTime());next();}
                case 6->{if(ticks<25||!read(layer,"linkState").equals("running"))return;facts.put("link_duration_ticks",read(layer,"linkDuration"));shot("03-real-link-wait");next();}
                case 7->{if(!archived(KnowledgeKey.idea(IDEA)))return;onServer(()->{linkEnd=player().serverLevel().getGameTime();require(linkEnd-linkStart>=195,"real ten-second link waiting");facts.put("link_observed_loaded_ticks",linkEnd-linkStart);});focus();next();}
                case 8->{if(ticks<20)return;require(project().startStatus().equals("NOT_UNDERSTOOD"),"idea visible but materials unmet");require(project().accessConditions().stream().filter(c->!c.met()).count()==3,"three unmet material observations");facts.put("idea_visible_three_materials_unmet",true);shot("04-visible-three-unmet-materials");observe("05-kaolin","frostedheart:kaolin",true,9,true);}
                case 9->observe("06-bauxite","frostedheart:bauxite",true,10,true);
                case 10->observe("07-clay","minecraft:clay_ball",true,11,true);
                case 11->{if(ticks<20||!openDesk())return;focus();next();}
                case 12->{if(ticks<20||!project().startStatus().equals("LEARNABLE"))return;require(project().accessConditions().stream().allMatch(KnowledgeJournalData.AccessCondition::met),"all start conditions via real observation and learning");shot("08-all-materials-ready");click("research.start");next();}
                case 13->{if(ticks<20||project().openRun().isBlank())return;run=firstRun=UUID.fromString(project().openRun());require(current().stage().id().equals("prepare")&&current().stage().ready(),"manual preparation stage");shot("09-prepare-stage");click("research.advance_stage");next();}
                case 14->{if(ticks<20||current()==null||!current().stage().id().equals("firing"))return;defaultMethod=current().stage().method().getString();require(defaultMethod.equals(Component.translatable("knowledge.the_winter_rescue.refractory_brick.method.default").getString()),"default hint without earlier reference");stageShot("10-default-firing-hint",145);}
                case 145->observe("11-reference-after-entry","frostedheart:refractory_brick",true,15,true);
                case 15->{if(!projectView())return;require(current().stage().method().getString().equals(defaultMethod)&&!current().stage().ready(),"reference learned after entry neither rerolls hint nor passes real sample task");facts.put("after_entry_reference_does_not_reroll_or_complete",true);stageShot("12-default-frozen-after-reference",155);}
                case 155->craft("the_winter_rescue:research/fire_clay_hand_mix",1,16);
                case 16->craft("the_winter_rescue:research/controlled_furnace",0,17);
                case 17->{if(ticks<20||mc().screen!=null)return;require(mc().player.getMainHandItem().is(item("frostedresearch:controlled_furnace")),"crafted furnace in hand");use(FURNACE.below());next();}
                case 18->{if(ticks<20||!mc().level.getBlockState(FURNACE).is(FRContents.Blocks.CONTROLLED_FURNACE.get()))return;onServer(()->{player().getInventory().setItem(9,new ItemStack(Items.CHARCOAL));player().inventoryMenu.broadcastChanges();});next();}
                case 19->{if(ticks<20||!openFurnace())return;require(!allowed(),"production locked before Design");move(4,0);move(12,1);furnaceClick("plus");furnaceClick("plus");furnaceClick("trial");next();}
                case 20->{if(ticks<20||furnace().trial())return;furnaceClick("start");next();}
                case 21->{if(ticks<20)return;require(furnace().message().equals("need_design")&&!furnace().displayedBusy()&&mc().player.containerMenu.getSlot(0).getItem().getCount()==1,"normal production UI refuses without Design and retains input");shot("16-production-locked");furnaceClick("production");jei(false,22);}
                case 22->{if(ticks<15||!openFurnace()||!furnace().trial())return;furnaceClick("start");next();}
                case 23->{if(ticks<15||!furnace().displayedBusy())return;require(furnace().duration()==400&&furnace().lockedTemperature()==1100,"real trial latches 1100 and 400 ticks");furnaceClick("plus");next();}
                case 24->{if(ticks<15)return;require(furnace().temperature()==1150&&furnace().lockedTemperature()==1100,"running adjustment affects next batch only");shot("17-running-current1100-next1150");next();}
                case 25->{if(ticks<15||mc().player.containerMenu.getSlot(2).getItem().isEmpty())return;var stack=mc().player.containerMenu.getSlot(2).getItem();require(stack.is(item("frostedheart:refractory_brick"))&&stack.getCount()==1&&ResearchSampleBinding.read(stack).orElseThrow().run().equals(firstRun),"real bound trial output");shot("18-real-trial-output");move(2,3);close();observe("19-trial-sample","frostedheart:refractory_brick",false,26,false);}
                case 26->{if(!projectView())return;if(current()==null||!current().stage().ready())return;require(mc().player.getMainHandItem().getCount()==1,"sample observation does not consume brick");facts.put("fresh_bound_observation_no_consumption",true);shot("20-trial-observation-ready");click("research.complete");next();}
                case 27->{if(ticks<20||!current().status().equals("completed")||!allowed())return;onServer(()->{require(rewards()==1&&ResearchHooks.canUseRecipe(service().teamId(),player().serverLevel().getRecipeManager().byKey(BATCH).orElseThrow()),"first result and actual recipe access");facts.put("first_run_rewards",rewards());supplyBatch(16,2);});shot("21-completed-production-unlocked");jei(true,28);}
                case 28->{if(ticks<20||!openFurnace())return;move(12,0);move(13,1);furnaceClick("trial");next();}
                case 29->{if(ticks<15||furnace().trial())return;furnaceClick("start");next();}
                case 30->{if(ticks<15||!furnace().displayedBusy())return;require(furnace().duration()==600&&furnace().lockedTemperature()==1150,"production real 600 ticks at inclusive upper boundary");shot("22-batch-production-running");next();}
                case 31->{if(ticks<15||mc().player.containerMenu.getSlot(2).getItem().isEmpty())return;var stack=mc().player.containerMenu.getSlot(2).getItem();require(stack.is(item("frostedheart:refractory_brick"))&&stack.getCount()==16&&ResearchSampleBinding.read(stack).isEmpty(),"real 16-brick production has no trial binding");shot("23-real-sixteen-brick-batch");move(2,5);facts.put("real_batch_output",16);close();onServer(()->{service().forget(KnowledgeKey.result(RESULT));require(rewards()==1,"forget does not create research rewards");facts.put("explicit_second_run_forget_fixture",true);TeamResearchService.send(player(),CTeamDataManager.get(player()));});next();}
                case 32->{if(!projectView()||!project().startStatus().equals("LEARNABLE"))return;click("research.start");next();}
                case 33->{if(ticks<20||project().openRun().isBlank()||project().openRun().equals(firstRun.toString()))return;run=UUID.fromString(project().openRun());click("research.advance_stage");next();}
                case 34->{if(ticks<20||!current().stage().id().equals("firing"))return;require(current().stage().method().getString().equals(Component.translatable("knowledge.the_winter_rescue.refractory_brick.method.reference").getString()),"pre-entry learned reference selects precise hint");facts.put("before_entry_reference_precise_hint",true);stageShot("24-second-run-precise-reference",345);}
                case 345->{close();onServer(()->supplyBatch(1,1));go(35);}
                case 35->{if(ticks<20||!openFurnace())return;move(3,6);move(12,0);move(13,1);furnaceClick("production");next();}
                case 36->{if(ticks<15||!furnace().trial())return;furnaceClick("start");next();}
                case 37->{if(ticks<15||mc().player.containerMenu.getSlot(2).getItem().isEmpty())return;require(ResearchSampleBinding.read(mc().player.containerMenu.getSlot(2).getItem()).orElseThrow().run().equals(run),"second run real sample");move(2,3);close();observe("25-second-trial-sample","frostedheart:refractory_brick",false,38,false);}
                case 38->{if(!projectView()||!current().stage().ready())return;click("research.complete");next();}
                case 39->{if(ticks<20||!current().status().equals("completed")||!allowed())return;onServer(()->{require(rewards()==2,"one result per legitimately completed run");facts.put("two_legitimate_runs_rewards",rewards());});close();go(Boolean.getBoolean("frostedresearch.m5WithM6")?390:40);}
                case 390->{if(ticks<15)return;facts.put("m5_gameplay_completed_before_m6",true);synchronized(facts){save("m5-gameplay-checkpoint.json",facts);}delegated=true;M6ClientValidation.startAfterM5(error->{if(error!=null){finish(error);return;}try{facts.put("m6_brief_completed_same_jvm",true);go(40);delegated=false;}catch(Exception e){finish(e);}});}
                case 40->{if(ticks<20||ClientQuestFile.INSTANCE==null)return;var file=ClientQuestFile.INSTANCE;Chapter chapter=file.getChapter(CHAPTER);require(chapter!=null&&chapter.getQuests().size()==5&&chapter.isVisible(file.selfTeamData),"actual FTB chapter loaded and visible");require(file.getVisibleChapters(file.selfTeamData).contains(chapter),"normal visible chapter list includes tutorial");require(dev.ftb.mods.ftbquests.quest.QuestObjectBase.getID(chapter.getGroup())==0x121B12860A7D323AL,"existing rebuild group");facts.put("ftb_actual_guide_title",chapter.getTitle().getString());for(long q:QUESTS)require(file.getQuest(q)!=null&&file.getQuest(q).isVisible(file.selfTeamData),"actual visible tutorial quest "+Long.toHexString(q));questScreen=ClientQuestFile.openGui();questScreen.selectChapter(chapter);next();}
                case 41->{if(ticks<20)return;shot("26-ftb-guide-chapter");questIndex=0;questScreen.viewQuest(ClientQuestFile.INSTANCE.getQuest(QUESTS[questIndex]));next();}
                case 42->{if(ticks<20)return;shot("27-ftb-guide-card-"+(questIndex+1));if(++questIndex<QUESTS.length){questScreen.closeQuest();questScreen.viewQuest(ClientQuestFile.INSTANCE.getQuest(QUESTS[questIndex]));ticks=0;return;}facts.put("ftb_five_actual_cards_opened",true);mc().options.languageCode="en_us";mc().getLanguageManager().setSelected("en_us");reload=mc().reloadResourcePacks();next();}
                case 43->{if(!reload.isDone()||ticks<20)return;reload.join();questScreen.closeQuest();questScreen.viewQuest(ClientQuestFile.INSTANCE.getQuest(QUESTS[4]));next();}
                case 44->{if(ticks<20)return;shot("28-ftb-guide-refractory-english");facts.put("ftb_read_only_no_checkmarks_or_progress_reset",true);finish(null);}
            }
        }catch(Throwable e){finish(e);}
    }
    private static void finish(Throwable error){
        if(finished)return;finished=true;
        try{if(keyHeld)nativeN(false);facts.put("status",error==null?"passed":"failed");facts.put("phase",phase);if(firstRun!=null)facts.put("first_run",firstRun.toString());if(run!=null)facts.put("last_run",run.toString());facts.put("scope",(Boolean.getBoolean("frostedresearch.m5WithM6")?"final M6 production and full M5 route, then chained multi-run brief; ":"M5 full route; ")+"real client UI callbacks, native directed N key messages, normal C2S observation/keep/learn/link/crafting/furnace/FTB UI; supplied materials/world and second-run forget fixture; no survival or physical-human-input claim");if(error!=null){var root=error;while(root.getCause()!=null)root=root.getCause();facts.put("failure",root.toString());try{shot("failure-phase-"+phase);}catch(Exception ignored){}}synchronized(facts){save("result.json",facts);}if(error==null)FRMain.LOGGER.info("M5_CLIENT_PASSED {}",ROOT);else FRMain.LOGGER.error("M5_CLIENT_FAILED phase "+phase,error);
        }catch(Throwable e){FRMain.LOGGER.error("M5 result",e);}finally{if(captured){mc().options.guiScale().set(originalScale);mc().options.pauseOnLostFocus=originalPause;mc().options.languageCode=originalLanguage;mc().getLanguageManager().setSelected(originalLanguage);}mc().stop();}
    }
}
