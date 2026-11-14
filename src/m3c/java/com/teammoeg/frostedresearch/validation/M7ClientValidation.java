package com.teammoeg.frostedresearch.validation;

import com.google.gson.*;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.dataholders.team.*;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.client.dialog.ImageScreenDialog;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.api.*;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import com.teammoeg.frostedresearch.knowledge.*;
import com.teammoeg.frostedresearch.knowledge.client.*;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.experiment.*;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket.Action;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Two actual Minecraft processes. Files coordinate gestures; game facts come from server/client state and TCP. */
public final class M7ClientValidation {
    private static final String ROLE = System.getProperty("frostedresearch.m7Role", "");
    private static final boolean HOST = ROLE.equals("host"), ENABLED = HOST || ROLE.equals("peer");
    private static final Path ROOT = Path.of(System.getProperty("frostedresearch.m7Session",
        "D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Research-Playtest-20261112/.minecraft/build/m7-session"));
    private static final ResourceLocation PROJECT = id("the_winter_rescue:iron_bloomery"), IDEA = id("the_winter_rescue:iron_smelting");
    private static final ResourceLocation RESULT = id("the_winter_rescue:iron_bloomery_design"), PROTOTYPE = id("the_winter_rescue:research/prototype_tuyere");
    private static final ResourceLocation BLOOMERY = id("the_winter_rescue:research/main_bloomery"), TUYERE = id("the_winter_rescue:research/sandy_tuyere");
    private static final BlockPos DESK = new BlockPos(0,64,-2), BELLOWS = new BlockPos(-2,64,2), TARGET = BELLOWS.east();
    private static final int[] SOLUTION = {0,6,12,10,9,3,0,0,0};
    private static final long START = System.nanoTime();
    private static final Map<String,Object> facts = Collections.synchronizedMap(new LinkedHashMap<>());
    private static Path session;
    private static int phase, ticks, rotations, originalScale;
    private static String originalLanguage;
    private static boolean captured, originalPause, peerFocused;
    private static volatile boolean busy, finished, partyReady, sameTeam, peerOnline, placedVerified, blowArmed, blown, finalVerified, duplicateVerified;
    private static volatile int port, logins, logouts;
    private static volatile UUID peerId, placingPlayer;
    private static volatile long clickAt = -1;
    private static volatile Throwable failure;
    private static UUID run, firstRun;
    private static int[] waitingMasks;
    private static DrawDeskScreen desk;
    private static KnowledgeLayer layer;
    private static java.util.concurrent.CompletableFuture<Void> reload;

    private static ResourceLocation id(String value) { return new ResourceLocation(value); }
    private static void require(boolean value,String text) { if(!value) throw new IllegalStateException(text); }
    private static Minecraft mc() { return Minecraft.getInstance(); }
    private static IntegratedServer server() { return mc().getSingleplayerServer(); }
    private static ServerPlayer host() { return Objects.requireNonNull(server().getPlayerList().getPlayerByName("M7Host")); }
    private static ServerPlayer peer() { return server().getPlayerList().getPlayerByName("M7Peer"); }
    private static KnowledgeService service() { return KnowledgeService.forPlayer(host()); }
    private static Block block(String value) { return Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(id(value))); }
    private static Item prototypeItem() { Block value = FRContents.Blocks.PROTOTYPE_TUYERE.get(); return value.asItem(); }
    private static boolean allowed(ResourceLocation value) { return TechnologyAccessResolver.isRecipeUnlocked(service().teamId(),value); }
    private static boolean clientAllowed(ResourceLocation value) { return ClientKnowledgeDataAPI.technologyProjection().recipe(value).allowed(); }
    private static long rewards() { return service().state().acquisitions().stream().filter(e -> e.element().key().equals(KnowledgeKey.result(RESULT))).count(); }
    private static void onServer(Runnable action) {
        busy=true; server().execute(() -> { try {action.run();} catch(Throwable e) {failure=e;} finally {busy=false;} });
    }
    private static Object read(Object object,String field) throws Exception { var f=object.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(object); }
    private static void write(Path file,Object value) throws Exception {
        Files.createDirectories(file.getParent()); Path temp=file.resolveSibling(file.getFileName()+".tmp");
        Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(value));
        Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    private static JsonObject state(String role) {
        if(session==null) return new JsonObject();
        try {return JsonParser.parseString(Files.readString(session.resolve(role+".json"))).getAsJsonObject();}
        catch(java.io.IOException|JsonParseException e) {return new JsonObject();}
    }
    private static String text(JsonObject value,String key) { return value.has(key) ? value.get(key).getAsString() : ""; }
    private static boolean step(String role,String value) { return text(state(role),"step").equals(value); }
    private static void signal(String value) throws Exception {
        Map<String,Object> message=new LinkedHashMap<>();message.put("step",value);message.put("phase",phase);message.put("port",port);
        if(run!=null)message.put("run",run.toString());if(firstRun!=null)message.put("first_run",firstRun.toString());
        write(session.resolve(ROLE+".json"),message);
    }
    private static void next() throws Exception { phase++;ticks=0;peerFocused=false;if(session!=null)write(session.resolve(ROLE+"-progress.json"),Map.of("phase",phase)); }
    private static void shot(String name) throws Exception {
        Files.createDirectories(session.resolve(ROLE));
        try(var image=Screenshot.takeScreenshot(mc().getMainRenderTarget())) {image.writeToFile(session.resolve(ROLE).resolve(name+".png"));}
    }
    private static KnowledgeJournalData data() { return new KnowledgeJournalData(KnowledgeClientState.snapshot()); }
    private static KnowledgeJournalData.Project project() { return data().projects.stream().filter(p -> p.id().equals(PROJECT.toString())).findFirst().orElseThrow(); }
    private static KnowledgeJournalData.Run current() { return data().run(run.toString()); }
    private static void focus() throws Exception {
        desk.showKnowledge();layer=desk.getKnowledgeLayer();var method=KnowledgeLayer.class.getDeclaredMethod("openResearch",String.class);
        method.setAccessible(true);method.invoke(layer,PROJECT.toString());
    }
    private static boolean openDesk() {
        if(mc().screen==null)use(DESK,Direction.UP);
        if(mc().screen instanceof CUIScreen cui && cui.getPrimaryLayer() instanceof DrawDeskScreen screen) {desk=screen;return true;}return false;
    }
    private static void press(JournalButton button) throws Exception { require(button.isEnabled(),"disabled "+button.getTitle().getString());((Runnable)read(button,"action")).run(); }
    private static void click(String key) throws Exception {
        String title=Component.translatable("gui.frostedresearch.journal."+key).getString();
        var surface=desk.getAirflowPanel()==null ? layer : desk.getAirflowPanel();
        press(surface.getElements().stream().filter(JournalButton.class::isInstance).map(JournalButton.class::cast).filter(b -> b.getTitle().getString().equals(title)).findFirst().orElseThrow());
    }
    private static void action(Action action,UUID target,String expectedStage) {
        FRNetwork.INSTANCE.sendToServer(new KnowledgeWorkbenchActionPacket(DESK,action,List.of(),target.toString(),"","",expectedStage));
    }
    private static void command(String value) { facts.put("command_"+value,true);mc().getConnection().sendCommand(value); }
    private static void use(BlockPos pos,Direction face) { mc().gameMode.useItemOn(mc().player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false)); }
    private static void slot(int value) { mc().gameMode.handleInventoryMouseClick(mc().player.containerMenu.containerId,value,0,ClickType.PICKUP,mc().player); }
    private static void look() {
        Vec3 delta=Vec3.atCenterOf(TARGET).subtract(mc().player.getEyePosition());
        mc().player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
        mc().player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
    }
    private static boolean solve() throws Exception {
        var board=Objects.requireNonNull(desk.getAirflowPanel()).board();int[] masks=board.getIntArray("masks");
        if(board.getBoolean("won")) return current()!=null && current().stage().ready();
        if(masks.length!=9 || (waitingMasks!=null && Arrays.equals(masks,waitingMasks)))return false;
        var buttons=desk.getAirflowPanel().getElements().stream().filter(JournalButton.class::isInstance).map(JournalButton.class::cast)
            .filter(b -> b.getWidth()==30 && b.getHeight()==30).sorted(Comparator.comparingInt(JournalButton::getY).thenComparingInt(JournalButton::getX)).toList();
        require(buttons.size()==9,"airflow cells");
        for(int i=0;i<9;i++)if(masks[i]!=SOLUTION[i]) {waitingMasks=masks.clone();press(buttons.get(i));rotations++;break;}
        return false;
    }
    /** Same integrated login/network service as LAN; loopback binding and no LAN advertisement are deliberate fixture settings. */
    private static void publishLoopback() throws Exception {
        InetAddress loopback=InetAddress.getByName("127.0.0.1");
        try(var socket=new ServerSocket(0,0,loopback)) {port=socket.getLocalPort();}
        server().setUsesAuthentication(false);
        server().getConnection().startTcpServerListener(loopback,port);
        ObfuscationReflectionHelper.findField(IntegratedServer.class,"f_120017_").setInt(server(),port); // publishedPort
        ObfuscationReflectionHelper.findField(IntegratedServer.class,"f_174966_").set(server(),GameType.SURVIVAL); // publishedGameType
        server().getPlayerList().setAllowCheatsForAllPlayers(true);
        require(server().isPublished() && server().getPort()==port,"integrated published state");
        facts.put("publication","ServerConnectionListener TCP loopback; integrated published fields; no LAN pinger");
        facts.put("endpoint","127.0.0.1:"+port);facts.put("offline_auth_fixture",true);
    }
    private static void fixture() {
        require(TeamsAPI.getAPI().getClass().getName().equals("com.teammoeg.chorda.compat.ftb.FTBTeamsAPIProvider"),"actual FTB provider");
        var a=host();var b=Objects.requireNonNull(peer());var manager=FTBTeamsAPI.api().getManager();
        require(manager.arePlayersInSameTeam(a.getUUID(),b.getUUID()),"actual FTB party membership");
        require(CTeamDataManager.get(a).getId().equals(CTeamDataManager.get(b).getId()),"actual shared Chorda holder");
        require(service().state().research().isEmpty() && KnowledgeService.forPlayer(b).state().research().isEmpty(),"join before research, no research merge fixture");
        facts.put("server_ftb_team",manager.getTeamForPlayer(a).orElseThrow().getId().toString());
        facts.put("server_chorda_team",service().teamId().toString());facts.put("same_team_before_research",true);
        var level=a.serverLevel();level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        for(int x=-5;x<=5;x++)for(int z=-4;z<=7;z++) {level.setBlockAndUpdate(new BlockPos(x,63,z),Blocks.STONE.defaultBlockState());for(int y=64;y<=68;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
        for(var player:List.of(a,b)) {FHScenario.get(player).pauseAct();player.getInventory().clearContent();}
        a.teleportTo(.5,64,.5);b.teleportTo(1.5,64,.5);b.setGameMode(GameType.SURVIVAL);b.setInvulnerable(true);
        facts.put("peer_inventory_fixture","SURVIVAL UI with invulnerability and supplied ingredients; not survival progression");
        var source=new AcquisitionSource("m7_fixture",Optional.of(a.getUUID()),level.getGameTime());
        service().produce(KnowledgeElement.idea(IDEA),source);
        for(var item:List.of(Items.COPPER_INGOT,Items.COAL))service().learn(ItemRetrieval.observe(a,new ItemStack(item)),source,KnowledgeService.Grant.NORMAL);
        a.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FRContents.Blocks.DRAWING_DESK.get()));
        require(a.getMainHandItem().useOn(new UseOnContext(a,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(DESK.below()),Direction.UP,DESK.below(),false))).consumesAction(),"fixture desk placement");
        a.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        Block bellows=block("charcoal_pit:bellows");level.setBlockAndUpdate(BELLOWS,bellows.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        TeamResearchService.send(a,CTeamDataManager.get(a));TeamResearchService.send(b,CTeamDataManager.get(b));
    }
    private static void materials() {
        var p=Objects.requireNonNull(peer());var values=p.serverLevel().getRecipeManager().byKey(PROTOTYPE).orElseThrow().getIngredients();
        require(values.size()==3,"prototype material count");
        for(int i=0;i<3;i++) {var choices=values.get(i).getItems();require(choices.length>0,"resolved material");var stack=choices[0].copy();stack.setCount(1);p.getInventory().setItem(9+i,stack);}
        p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        p.inventoryMenu.broadcastChanges();p.containerMenu.broadcastChanges();
    }
    private static void connect() {
        String address="127.0.0.1:"+port;
        ConnectScreen.startConnecting(new TitleScreen(),mc(),ServerAddress.parseString(address),new ServerData("M7 local fixture",address,true),false);
    }
    private static void disconnectPeer() { mc().level.disconnect();mc().clearLevel(new TitleScreen()); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!ENABLED||!HOST||!(event.getEntity() instanceof ServerPlayer p)||!p.getGameProfile().getName().equals("M7Peer"))return;
        peerId=p.getUUID();peerOnline=true;logins++;facts.put("peer_login_events",logins);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if(!ENABLED||!HOST||!event.getEntity().getGameProfile().getName().equals("M7Peer"))return;
        peerOnline=false;logouts++;facts.put("peer_logout_events",logouts);
    }
    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) {
        if(ENABLED&&HOST&&event.getPos().equals(TARGET)&&event.getEntity() instanceof ServerPlayer p)placingPlayer=p.getUUID();
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if(ENABLED&&HOST&&blowArmed&&event.getEntity() instanceof ServerPlayer p&&event.getPos().equals(BELLOWS))clickAt=p.level().getGameTime();
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if(!ENABLED||!HOST||finished||event.phase!=TickEvent.Phase.END||!blowArmed||blown||clickAt<0)return;
        try {
            var level=host().serverLevel();long elapsed=level.getGameTime()-clickAt;
            if(level.getBlockState(TARGET).is(block("charcoal_pit:sandy_tuyere"))) {
                require(elapsed>=10,"actual delayed bellows tick");require(peer()==null,"placer remains offline during blow");
                require(PendingResearchProofs.get(level).at(TARGET).orElseThrow().run().equals(firstRun),"trial proof");
                facts.put("delayed_blow_ticks",elapsed);blown=true;
            } else require(elapsed<100,"bellows did not convert prototype");
        }catch(Throwable e){failure=e;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!ENABLED||finished||event.phase!=TickEvent.Phase.END)return;
        try {
            require(System.nanoTime()-START<2_400_000_000_000L,"M7 session timeout phase "+phase);
            if(failure!=null)throw new IllegalStateException("host server action",failure);
            if(busy||mc().getOverlay()!=null)return;
            if(session!=null&&step(HOST?"peer":"host","failed"))throw new IllegalStateException("other client failed; inspect its result");
            if(mc().screen instanceof ImageScreenDialog) {Screen screen=mc().screen;for(int i=0;i<5;i++)if(screen.shouldCloseOnEsc()){screen.onClose();break;}return;}
            ticks++;
            if(phase==0) {
                if(mc().level!=null||mc().screen==null)return;
                captured=true;originalScale=mc().options.guiScale().get();originalPause=mc().options.pauseOnLostFocus;originalLanguage=mc().options.languageCode;
                mc().options.guiScale().set(2);mc().options.pauseOnLostFocus=false;GLFW.glfwSetWindowSize(mc().getWindow().getWindow(),1280,800);mc().resizeDisplay();
                mc().options.languageCode="zh_cn";mc().getLanguageManager().setSelected("zh_cn");reload=mc().reloadResourcePacks();
                if(HOST){session=ROOT.resolve("session-"+System.currentTimeMillis());Files.createDirectories(session);}
                next();return;
            }
            if(phase==1) {
                if(!reload.isDone())return;reload.join();
                if(HOST)mc().createWorldOpenFlows().createFreshLevel("m7-host-"+System.currentTimeMillis(),new LevelSettings("M7 fixture collaboration",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                    new WorldOptions(755111011717574879L,false,false),r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                next();return;
            }
            if(HOST)hostTick();else peerTick();
        }catch(Throwable e){finish(e);}
    }
    private static void hostTick() throws Exception {
        switch(phase) {
            case 2 -> {if(mc().player==null||server()==null||mc().screen!=null||ticks<30)return;require(mc().player.getGameProfile().getName().equals("M7Host"),"launch host with offline name M7Host");onServer(() -> {try{publishLoopback();}catch(Exception e){throw new IllegalStateException(e);}});next();}
            case 3 -> {command("ftbteams party create M7Study");signal("waiting-peer");write(ROOT.resolve("active.json"),Map.of("session",session.getFileName().toString(),"port",port));next();}
            case 4 -> {
                if(ticks%20==0)onServer(() -> partyReady=FTBTeamsAPI.api().getManager().getTeamForPlayer(host()).map(t -> t.isPartyTeam()).orElse(false));
                if(!partyReady||!peerOnline||!step("peer","connected"))return;command("ftbteams party invite M7Peer");signal("join");next();
            }
            case 5 -> {
                if(!sameTeam) {if(ticks%20==0)onServer(() -> {if(peer()!=null&&FTBTeamsAPI.api().getManager().arePlayersInSameTeam(host().getUUID(),peer().getUUID())){fixture();sameTeam=true;}});return;}next();
            }
            case 6 -> {if(ticks<15||!openDesk())return;focus();next();}
            case 7 -> {if(ticks<15||!project().startStatus().equals("LEARNABLE"))return;shot("01-same-team-before-start");click("research.start");next();}
            case 8 -> {if(ticks<15||project().openRun().isBlank())return;run=firstRun=UUID.fromString(project().openRun());signal("airflow");click("research.airflow_start");next();}
            case 9 -> {if(ticks<5)return;if(!solve()){ticks=0;return;}shot("02-host-won");click("research.airflow_back");next();}
            case 10 -> {if(ticks<20||!step("peer","airflow-seen"))return;signal("advance");click("research.advance_stage");next();}
            case 11 -> {
                if(ticks<20||current()==null||!current().stage().id().equals("trial")||!step("peer","advanced"))return;
                onServer(() -> {require(service().state().research().size()==1&&!service().state().research().get(run).completed(),"duplicate advance did not skip/complete");materials();});signal("craft");next();
            }
            case 12 -> {
                if(!step("peer","placed"))return;
                if(!placedVerified){onServer(() -> {var be=host().serverLevel().getBlockEntity(TARGET);require(be instanceof PrototypeTuyereBlockEntity,"real peer world placement");
                    var binding=((PrototypeTuyereBlockEntity)be).binding().orElseThrow();require(binding.team().equals(service().teamId())&&binding.run().equals(run),"shared run binding");
                    require(peerId.equals(placingPlayer),"default placement event came from Peer");facts.put("placer_uuid",peerId.toString());placedVerified=true;});return;}
                signal("disconnect");next();
            }
            case 13 -> {if(peerOnline||logouts<1||!step("peer","offline"))return;onServer(() -> require(peer()==null&&host()!=null,"only placer left, integrated host still present"));mc().player.closeContainer();next();}
            case 14 -> {if(ticks<15||mc().screen!=null)return;look();shot("03-peer-offline-prototype");blowArmed=true;use(BELLOWS,Direction.UP);next();}
            case 15 -> {if(!blown||ticks<25)return;shot("04-host-blow-normal-tuyere");FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.BLOCK,TARGET,-1));next();}
            case 16 -> {if(ticks<80||current()==null||!current().stage().ready())return;onServer(() -> {require(peer()==null&&PendingResearchProofs.get(host().serverLevel()).at(TARGET).isEmpty(),"host sampled peer trial while placer offline");facts.put("host_observed_peer_trial_while_peer_offline",true);});FRNetwork.INSTANCE.sendToServer(new ObservationActionPacket(ObservationActionPacket.Action.DISCARD));next();}
            case 17 -> {if(ticks<10||!openDesk())return;focus();next();}
            case 18 -> {if(ticks<15)return;shot("05-host-ready-to-finish");click("research.complete");action(Action.COMPLETE_RESEARCH,firstRun,"");next();}
            case 19 -> {
                if(ticks<30||current()==null||!current().status().equals("completed"))return;
                if(!finalVerified){onServer(() -> {require(rewards()==1&&allowed(BLOOMERY)&&allowed(TUYERE)&&!allowed(PROTOTYPE),"one Design, both permanent recipes, no temporary source");facts.put("first_run_rewards",rewards());finalVerified=true;});return;}
                shot("06-host-normal-completed");signal("reconnect");next();
            }
            case 20 -> {if(!step("peer","reconnected-checked")||!peerOnline||logins<2)return;onServer(() -> {require(FTBTeamsAPI.api().getManager().arePlayersInSameTeam(host().getUUID(),peer().getUUID()),"party survives reconnect");service().forget(KnowledgeKey.result(RESULT));facts.put("fixture_forgot_design_for_second_run",true);TeamResearchService.send(host(),CTeamDataManager.get(host()));});next();}
            case 21 -> {if(ticks<20)return;focus();if(!project().startStatus().equals("LEARNABLE"))return;click("research.start");next();}
            case 22 -> {if(ticks<20||project().openRun().isBlank()||project().openRun().equals(firstRun.toString()))return;run=UUID.fromString(project().openRun());waitingMasks=null;signal("airflow2");click("research.airflow_start");next();}
            case 23 -> {if(ticks<5)return;if(!solve()){ticks=0;return;}click("research.airflow_back");next();}
            case 24 -> {if(ticks<20||!step("peer","second-seen"))return;signal("advance2");click("research.advance_stage");next();}
            case 25 -> {
                if(ticks<30||!step("peer","duplicates-sent")||current()==null||!current().stage().id().equals("trial"))return;
                if(!duplicateVerified){onServer(() -> {var value=service().state().research().get(run);require(value.open()&&!value.stageState().visits().get("trial").ready(),"second run advanced exactly once");
                    require(service().state().research().get(firstRun).completed()&&rewards()==1&&!service().query(KnowledgeKey.result(RESULT)).inArchive(),"stale first-run finish did not issue reward again");
                    facts.put("second_run_duplicate_advance_and_stale_finish",true);duplicateVerified=true;});return;}
                shot("07-second-run-no-duplicate-reward");click("research.abandon");click("research.abandon.confirm");next();
            }
            case 26 -> {if(ticks<20||current()==null||!current().status().equals("abandoned"))return;onServer(() -> require(rewards()==1&&!allowed(PROTOTYPE)&&!allowed(BLOOMERY)&&!allowed(TUYERE),"abandoned fixture leaves no new permission/reward"));signal("fixture-end");next();}
            case 27 -> {if(!step("peer","fixture-checked"))return;signal("finish");next();}
            case 28 -> {if(!step("peer","passed")||peerOnline)return;facts.put("total_ui_rotations",rotations);finish(null);}
        }
    }
    private static void peerTick() throws Exception {
        switch(phase) {
            case 2 -> {JsonObject active;try{active=JsonParser.parseString(Files.readString(ROOT.resolve("active.json"))).getAsJsonObject();}catch(java.io.IOException|JsonParseException e){return;}
                session=ROOT.resolve(active.get("session").getAsString());port=active.get("port").getAsInt();connect();next();}
            case 3 -> {if(mc().player==null||mc().level==null||mc().screen!=null||ticks<30)return;require(mc().player.getGameProfile().getName().equals("M7Peer"),"launch Peer with offline name M7Peer");
                require(!mc().getConnection().getConnection().isMemoryConnection(),"Peer uses actual network connection");facts.put("tcp_remote",mc().getConnection().getConnection().getRemoteAddress().toString());signal("connected");next();}
            case 4 -> {if(!step("host","join"))return;command("ftbteams party join M7Host");next();}
            case 5 -> {
                if(!FTBTeamsAPI.api().isClientManagerLoaded()||!FTBTeamsAPI.api().getClientManager().selfTeam().isPartyTeam()||!step("host","airflow"))return;
                run=firstRun=UUID.fromString(text(state("host"),"run"));if(!openDesk()||current()==null)return;
                if(!peerFocused){focus();peerFocused=true;ticks=0;return;}if(ticks<15)return;
                facts.put("client_ftb_team",FTBTeamsAPI.api().getClientManager().selfTeam().getId().toString());facts.put("client_chorda_team",CClientTeamDataManager.INSTANCE.getInstance().getId().toString());
                shot("01-peer-shared-airflow");signal("airflow-seen");next();
            }
            case 6 -> {if(!step("host","advance"))return;action(Action.ADVANCE_STAGE,run,"airflow");action(Action.ADVANCE_STAGE,run,"airflow");signal("advanced");next();}
            case 7 -> {if(!step("host","craft")||current()==null||!current().stage().id().equals("trial"))return;mc().player.closeContainer();next();}
            case 8 -> {if(ticks<15||mc().screen!=null)return;require(mc().gameMode.getPlayerMode()==GameType.SURVIVAL,"Peer fixture uses normal inventory UI");mc().setScreen(new InventoryScreen(mc().player));for(int i=0;i<3;i++){slot(9+i);slot(1+i);}next();}
            case 9 -> {if(ticks<20)return;require(mc().player.inventoryMenu.getSlot(0).getItem().is(prototypeItem()),"real peer crafting output");shot("02-peer-real-crafting-grid");slot(0);slot(36);next();}
            case 10 -> {if(ticks<15)return;require(mc().player.getMainHandItem().is(prototypeItem()),"crafted prototype in peer hand");mc().player.closeContainer();look();use(TARGET.below(),Direction.UP);next();}
            case 11 -> {if(ticks<25||!mc().level.getBlockState(TARGET).is(FRContents.Blocks.PROTOTYPE_TUYERE.get()))return;shot("03-peer-placed-prototype");signal("placed");next();}
            case 12 -> {if(!step("host","disconnect"))return;disconnectPeer();next();}
            case 13 -> {if(mc().level!=null||mc().getConnection()!=null)return;signal("offline");next();}
            case 14 -> {if(!step("host","reconnect"))return;connect();next();}
            case 15 -> {
                if(mc().player==null||mc().level==null||ticks<30||(mc().screen!=null&&!(mc().screen instanceof CUIScreen)))return;
                require(!mc().getConnection().getConnection().isMemoryConnection(),"reconnect remains TCP");
                if(data().run(firstRun.toString())==null||!data().run(firstRun.toString()).status().equals("completed")||!clientAllowed(BLOOMERY)||!clientAllowed(TUYERE)||clientAllowed(PROTOTYPE))return;
                if(!openDesk())return;if(!peerFocused){focus();peerFocused=true;ticks=0;return;}
                if(ticks<15)return;shot("04-peer-reconnected-completed");facts.put("reconnected_stage_and_recipe_permissions",true);signal("reconnected-checked");next();
            }
            case 16 -> {if(!step("host","airflow2"))return;run=UUID.fromString(text(state("host"),"run"));focus();if(current()==null)return;signal("second-seen");next();}
            case 17 -> {if(!step("host","advance2"))return;action(Action.ADVANCE_STAGE,run,"airflow");action(Action.ADVANCE_STAGE,run,"airflow");action(Action.COMPLETE_RESEARCH,firstRun,"");action(Action.COMPLETE_RESEARCH,firstRun,"");signal("duplicates-sent");next();}
            case 18 -> {if(!step("host","fixture-end")||current()==null||!current().status().equals("abandoned")||clientAllowed(PROTOTYPE)||clientAllowed(BLOOMERY)||clientAllowed(TUYERE))return;shot("05-peer-second-run-abandoned");facts.put("second_run_and_permission_sync",true);signal("fixture-checked");next();}
            case 19 -> {if(!step("host","finish"))return;finish(null);}
        }
    }
    private static void finish(Throwable error) {
        if(finished)return;finished=true;
        try {
            if(session==null)session=ROOT.resolve("unattached-"+ROLE+"-"+System.currentTimeMillis());
            facts.put("status",error==null?"passed":"failed");facts.put("role",ROLE);facts.put("phase",phase);
            if(firstRun!=null)facts.put("first_run",firstRun.toString());if(run!=null)facts.put("last_run",run.toString());
            facts.put("scope","two real clients, actual TCP/default player events/FTB party commands/C2S; fixture world, knowledge and materials; no survival or physical input claim");
            if(error!=null){Throwable root=error;while(root.getCause()!=null)root=root.getCause();facts.put("failure",root.toString());}
            synchronized(facts){write(session.resolve(ROLE+"-result.json"),facts);}
            signal(error==null?"passed":"failed");if(error!=null)FRMain.LOGGER.error("M7_"+ROLE+"_FAILED",error);else FRMain.LOGGER.info("M7_{}_PASSED {}",ROLE,session);
        }catch(Exception e){FRMain.LOGGER.error("M7 result",e);}
        finally {
            if(captured){mc().options.guiScale().set(originalScale);mc().options.pauseOnLostFocus=originalPause;mc().options.languageCode=originalLanguage;mc().getLanguageManager().setSelected(originalLanguage);}
            mc().stop();
        }
    }
}
