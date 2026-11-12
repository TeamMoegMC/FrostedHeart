package com.teammoeg.frostedresearch.validation;

import com.google.gson.GsonBuilder;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.client.dialog.ImageScreenDialog;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.api.*;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedresearch.knowledge.*;
import com.teammoeg.frostedresearch.knowledge.client.*;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.experiment.ResearchSampleBinding;
import com.teammoeg.frostedresearch.knowledge.furnace.*;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ClickType;
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
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

/** Final M6 only: visible run choices and the real CUI trial-start endpoint. M5 full entry is separate. */
public final class M6ClientValidation {
    private static final boolean ENABLED=Boolean.getBoolean("frostedresearch.m6Validation");
    private static final Path ROOT=Path.of(System.getProperty("frostedresearch.m6Root",System.getProperty("frostedresearch.m5Root","build/m6-client")+"/m6-brief"));
    private static final ResourceLocation PROJECT=id("the_winter_rescue:refractory_brick"),IDEA=id("the_winter_rescue:refractory_firing"),RESULT=id("the_winter_rescue:controlled_refractory_production"),BATCH=id("the_winter_rescue:research/refractory_brick_batch");
    private static final BlockPos DESK=new BlockPos(0,64,-2),FURNACE=new BlockPos(2,64,1);
    private static final Map<String,Object> facts=Collections.synchronizedMap(new LinkedHashMap<>());
    private static final long START=System.nanoTime();
    private static int phase,ticks,originalScale;
    private static String originalLanguage;
    private static boolean captured,finished,originalPause,chained;
    private static java.util.function.Consumer<Throwable> continuation;
    private static volatile boolean busy,checked;
    private static volatile Throwable failure;
    private static volatile UUID first,second;
    private static DrawDeskScreen desk;
    private static KnowledgeLayer layer;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static Minecraft mc(){return Minecraft.getInstance();}
    private static ResourceLocation id(String value){return new ResourceLocation(value);}
    private static Item item(String value){return Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(id(value)));}
    private static ServerPlayer player(){return mc().getSingleplayerServer().getPlayerList().getPlayer(mc().player.getUUID());}
    private static KnowledgeService service(){return KnowledgeService.forPlayer(player());}
    private static void require(boolean value,String text){if(!value)throw new IllegalStateException(text);}
    private static Object read(Object value,String field)throws Exception{var f=value.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(value);}
    private static void save(String file,Object value)throws Exception{Files.createDirectories(ROOT);Files.writeString(ROOT.resolve(file),new GsonBuilder().setPrettyPrinting().create().toJson(value));}
    private static void next()throws Exception{phase++;ticks=0;save("progress.json",Map.of("phase",phase));}
    private static void shot(String name)throws Exception{Files.createDirectories(ROOT);try(var image=Screenshot.takeScreenshot(mc().getMainRenderTarget())){image.writeToFile(ROOT.resolve(name+".png"));}}
    private static void onServer(Runnable action){busy=true;mc().getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable e){failure=e;}finally{busy=false;}});}
    private static KnowledgeJournalData data(){return new KnowledgeJournalData(KnowledgeClientState.snapshot());}
    private static KnowledgeJournalData.Run run(UUID id){return data().run(id.toString());}
    private static void use(BlockPos pos){mc().gameMode.useItemOn(mc().player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    private static void close(){mc().player.closeContainer();}
    private static boolean openDesk(){if(mc().screen==null)use(DESK);if(mc().screen instanceof CUIScreen cui&&cui.getPrimaryLayer() instanceof DrawDeskScreen d){desk=d;desk.showKnowledge();layer=desk.getKnowledgeLayer();return true;}return false;}
    private static void focus()throws Exception{var m=KnowledgeLayer.class.getDeclaredMethod("openResearch",String.class);m.setAccessible(true);m.invoke(layer,PROJECT.toString());}
    private static JournalButton button(UILayer root,String title){return root.getElements().stream().filter(JournalButton.class::isInstance).map(JournalButton.class::cast).filter(b->b.getTitle().getString().equals(title)).findFirst().orElseThrow(()->new IllegalStateException("missing "+title+" phase "+phase));}
    private static void press(JournalButton b)throws Exception{require(b.isEnabled(),"disabled "+b.getTitle().getString());((Runnable)read(b,"action")).run();}
    private static void click(String key)throws Exception{press(button(layer,Component.translatable("gui.frostedresearch.journal."+key).getString()));}
    private static void arrow(String text)throws Exception{press(button(layer,text));}
    private static UUID selected()throws Exception{return UUID.fromString(((Map<?,?>)read(layer,"selectedRuns")).get(PROJECT.toString()).toString());}
    private static ControlledFurnaceBlockEntity furnace(){return ((ControlledFurnaceMenu)mc().player.containerMenu).getBlock();}
    private static boolean openFurnace(){if(mc().screen==null)use(FURNACE);return mc().player.containerMenu instanceof ControlledFurnaceMenu;}
    private static void furnaceClick(String key)throws Exception{press(button(((CUIScreen)mc().screen).getPrimaryLayer(),Component.translatable("gui.frostedresearch.furnace."+key).getString()));}
    private static void target()throws Exception{var root=((CUIScreen)mc().screen).getPrimaryLayer();press(root.getElements().stream().filter(JournalButton.class::isInstance).map(JournalButton.class::cast).filter(b->b.getWidth()==285&&b.getHeight()==19).findFirst().orElseThrow());}
    private static void slot(int i){mc().gameMode.handleInventoryMouseClick(mc().player.containerMenu.containerId,i,0,ClickType.PICKUP,mc().player);}
    private static void move(int a,int b){slot(a);slot(b);}
    public static void startAfterM5(java.util.function.Consumer<Throwable> callback){
        continuation=callback;chained=true;
        mc().level.disconnect();mc().clearLevel(new TitleScreen());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(M6ClientValidation.class);
    }
    private static void fixture(){
        var p=player();var level=p.serverLevel();FHScenario.get(p).pauseAct();p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.teleportTo(.5,64,.5);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){level.setBlockAndUpdate(new BlockPos(x,63,z),Blocks.STONE.defaultBlockState());for(int y=64;y<=68;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
        var source=new AcquisitionSource("m6_client_fixture",Optional.of(p.getUUID()),level.getGameTime());service().produce(KnowledgeElement.idea(IDEA),source);
        for(String raw:List.of("frostedheart:kaolin","frostedheart:bauxite","minecraft:clay_ball"))service().learn(ItemRetrieval.observe(p,new ItemStack(item(raw))),source,KnowledgeService.Grant.NORMAL);
        first=service().startResearch(PROJECT).orElseThrow();require(service().advanceStage(first,"prepare").status()==KnowledgeService.Status.SUCCESS,"fixture enters firing");
        var original=service().state().research().get(first);second=UUID.randomUUID();
        service().state().recordResearch(new ResearchHistory(second,original.project(),original.origins(),original.time()+1,false,List.of(),false,Map.of(),Map.of(),Map.of(),Map.of(),original.stageState()));
        for(var entry:Map.of(DESK,FRContents.Blocks.DRAWING_DESK.get(),FURNACE,FRContents.Blocks.CONTROLLED_FURNACE.get()).entrySet()){
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(entry.getValue()));require(p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(entry.getKey().below()),Direction.UP,entry.getKey().below(),false))).consumesAction(),"fixture place "+entry.getKey());
        }p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.getInventory().setItem(9,new ItemStack(item("frostedheart:fire_clay_ball"),2));p.getInventory().setItem(10,new ItemStack(Items.CHARCOAL,2));p.inventoryMenu.broadcastChanges();TeamResearchService.send(p,CTeamDataManager.get(p));
        facts.put("constructed_two_history_fixture",true);facts.put("first_run",first.toString());facts.put("second_run",second.toString());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if((!ENABLED&&!chained)||finished||event.phase!=TickEvent.Phase.END)return;
        try{
            require(System.nanoTime()-START<1_800_000_000_000L,"M6 client timeout phase "+phase);if(failure!=null)throw new IllegalStateException("server assertion",failure);if(busy||mc().getOverlay()!=null)return;
            if(mc().screen instanceof ImageScreenDialog){var s=mc().screen;for(int i=0;i<5;i++)if(s.shouldCloseOnEsc()){s.onClose();break;}return;}ticks++;
            switch(phase){
                case 0->{if(mc().level!=null||mc().screen==null)return;captured=true;originalScale=mc().options.guiScale().get();originalLanguage=mc().options.languageCode;originalPause=mc().options.pauseOnLostFocus;mc().options.pauseOnLostFocus=false;mc().options.guiScale().set(2);GLFW.glfwSetWindowSize(mc().getWindow().getWindow(),1280,800);mc().resizeDisplay();mc().options.languageCode="zh_cn";mc().getLanguageManager().setSelected("zh_cn");reload=originalLanguage.equals("zh_cn")?java.util.concurrent.CompletableFuture.completedFuture(null):mc().reloadResourcePacks();next();}
                case 1->{if(!reload.isDone())return;reload.join();mc().createWorldOpenFlows().createFreshLevel("m6-brief-"+System.currentTimeMillis(),new LevelSettings("M6 two retained runs and normal furnace start",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(755111011717574881L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());next();}
                case 2->{if(mc().player==null||mc().getSingleplayerServer()==null||ticks<40)return;if(mc().screen!=null){mc().screen.onClose();return;}onServer(M6ClientValidation::fixture);next();}
                case 3->{if(ticks<20||!openDesk())return;focus();next();}
                case 4->{if(ticks<20||run(first)==null||run(second)==null)return;require(run(first).number()==1&&run(second).number()==2&&run(first).copies()==2&&run(second).copies()==2,"client visible ordinals for same project");require(selected().equals(first),"default oldest run selection");shot("01-first-run-ordinal");arrow("›");next();}
                case 5->{if(ticks<15)return;require(selected().equals(second),"actual next callback selects second run");shot("02-second-run-ordinal");arrow("‹");next();}
                case 6->{if(ticks<15)return;require(selected().equals(first),"actual previous callback restores first run");facts.put("journal_two_run_ordinals_and_switch",true);shot("03-first-run-restored");close();next();}
                case 7->{if(ticks<20||!openFurnace()||furnace().targetCount()!=2)return;move(12,0);move(13,1);furnaceClick("start");next();}
                case 8->{if(ticks<20)return;require(furnace().message().equals("choose_target")&&!furnace().displayedBusy()&&mc().player.containerMenu.getSlot(0).getItem().getCount()==2,"normal CUI requires target when two runs");shot("04-explicit-target-required");target();next();}
                case 9->{if(ticks<20||furnace().selectedTarget().isEmpty())return;require(furnace().selectedTarget().orElseThrow().run().equals(first),"first device target matches first journal run");shot("05-first-furnace-target");target();next();}
                case 10->{if(ticks<20)return;require(furnace().selectedTarget().orElseThrow().run().equals(second),"second explicit device target");shot("06-second-furnace-target");furnaceClick("plus");furnaceClick("plus");next();}
                case 11->{if(ticks<20||furnace().temperature()!=1100)return;furnaceClick("start");next();}
                case 12->{if(ticks<20||!furnace().displayedBusy())return;require(furnace().lockedTemperature()==1100&&furnace().duration()==400,"new real CUI start endpoint starts normal batch");if(!checked){onServer(()->{var f=(ControlledFurnaceBlockEntity)player().serverLevel().getBlockEntity(FURNACE);require(f.binding().orElseThrow().run().equals(second),"server batch binds clicked second target");checked=true;});return;}shot("07-normal-new-endpoint-running");next();}
                case 13->{if(ticks<20||mc().player.containerMenu.getSlot(2).getItem().isEmpty())return;var stack=mc().player.containerMenu.getSlot(2).getItem();require(stack.is(item("frostedheart:refractory_brick"))&&stack.getCount()==1&&ResearchSampleBinding.read(stack).orElseThrow().run().equals(second),"actual normal output belongs to chosen second run");shot("08-real-second-run-output");move(2,3);close();mc().player.setXRot(-85);mc().player.setYRot(180);next();}
                case 14->{if(ticks<20||mc().screen!=null)return;FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.HELD_ITEM,BlockPos.ZERO,0));next();}
                case 15->{if(ticks<80||!openDesk()||!run(second).stage().ready())return;require(!run(first).stage().ready()&&mc().player.getMainHandItem().getCount()==1,"fresh observation credits only selected run without consumption");FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.DISCARD));next();}
                case 16->{if(ticks<20||!openDesk())return;focus();next();}
                case 17->{if(ticks<20)return;if(!selected().equals(second)){arrow("›");ticks=0;return;}shot("09-only-second-run-ready");click("research.complete");next();}
                case 18->{if(ticks<20||!run(second).status().equals("completed")||!run(first).status().equals("standby"))return;require(ClientKnowledgeDataAPI.technologyProjection().recipe(BATCH).allowed(),"actual production access after completion");shot("10-completed-second-run-and-standby-counter");facts.put("normal_start_selected_output_and_standby",true);onServer(()->require(service().state().acquisitions().stream().filter(e->e.element().key().equals(KnowledgeKey.result(RESULT))).count()==1,"one final design event"));next();}
                case 19->{if(ticks<20)return;facts.put("standby_history_retained_not_actionable",true);next();}
                case 20->{if(ticks<10)return;finish(null);}
            }
        }catch(Throwable e){finish(e);}
    }
    private static void finish(Throwable error){
        if(finished)return;finished=true;
        try{facts.put("status",error==null?"passed":"failed");facts.put("phase",phase);facts.put("scope","final M6 actual UI callbacks/C2S/new controlled furnace start packet and real loaded ticks; two history, knowledge, world and material fixtures; direct fresh held-item sampling without preview claim; no real team merge or survival claim");if(error!=null){var root=error;while(root.getCause()!=null)root=root.getCause();facts.put("failure",root.toString());try{shot("failure-phase-"+phase);}catch(Exception ignored){}}synchronized(facts){save("result.json",facts);}if(error==null)FRMain.LOGGER.info("M6_CLIENT_PASSED {}",ROOT);else FRMain.LOGGER.error("M6_CLIENT_FAILED phase "+phase,error);
        }catch(Throwable e){FRMain.LOGGER.error("M6 result",e);if(error==null)error=e;}finally{if(captured){mc().options.guiScale().set(originalScale);mc().options.pauseOnLostFocus=originalPause;mc().options.languageCode=originalLanguage;mc().getLanguageManager().setSelected(originalLanguage);}if(chained)continuation.accept(error);else mc().stop();}
    }
}
