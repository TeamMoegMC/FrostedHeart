package com.teammoeg.frostedresearch.validation;

import com.google.gson.GsonBuilder;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.cui.base.*;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.client.dialog.ImageScreenDialog;
import com.teammoeg.frostedresearch.*;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.drawdesk.*;
import com.teammoeg.frostedresearch.gui.drawdesk.game.*;
import com.teammoeg.frostedresearch.knowledge.*;
import com.teammoeg.frostedresearch.knowledge.client.*;
import com.teammoeg.frostedresearch.knowledge.client.ui.*;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

/** Opt-in loaded-pack UI/C2S test. Only the initial scene and archive knowledge are fixtures. */
public final class LinkClientValidation {
    private static final Path OUT = Path.of(System.getProperty("frostedresearch.linkRoot")).resolve("build/link-client");
    private static final BlockPos DESK = new BlockPos(0,64,-2);
    private static final KnowledgeKey IDEA = KnowledgeKey.idea(new ResourceLocation("the_winter_rescue:earthen_smelting"));
    private static final Map<String,Object> checks = new LinkedHashMap<>();
    private static int phase, ticks, originalScale, originalFps, moveIndex, remaining = -1;
    private static boolean captured, originalPause, finished;
    private static String originalLanguage;
    private static volatile boolean busy;
    private static volatile Throwable failure;
    private static long phaseAt = System.nanoTime();
    private static DrawDeskScreen desk;
    private static KnowledgeLayer layer;
    private static List<Pair<CardPos,CardPos>> moves;
    private static Object read(Object value,String field) throws Exception { var f=value.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(value); }
    private static void require(boolean yes,String message) { if(!yes)throw new IllegalStateException(message); }
    private static ServerPlayer player() { return Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().get(0); }
    private static KnowledgeService service() { return KnowledgeService.forPlayer(player()); }
    private static void server(Runnable action) {
        busy=true;Minecraft.getInstance().getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable e){failure=e;}finally{busy=false;}});
    }
    private static void next() throws Exception { phase++;ticks=0;phaseAt=System.nanoTime();Files.createDirectories(OUT);Files.writeString(OUT.resolve("progress.json"),"{\"phase\":"+phase+"}"); }
    private static void shot(String name) throws Exception { Files.createDirectories(OUT);try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUT.resolve(name+".png"));} }
    private static void press(UIElement element) {
        pressAt(element,element.getWidth()/2.0,element.getHeight()/2.0);
    }
    private static void pressAt(UIElement element,double dx,double dy) {
        var mc=Minecraft.getInstance();
        double x=(mc.getWindow().getGuiScaledWidth()-desk.getWidth())/2.0+element.getScreenX()+dx;
        double y=(mc.getWindow().getGuiScaledHeight()-desk.getHeight())/2.0+element.getScreenY()+dy;
        desk.updateGui((mc.getWindow().getGuiScaledWidth()-desk.getWidth())/2,(mc.getWindow().getGuiScaledHeight()-desk.getHeight())/2,x,y,-1);
        desk.updateMouseOver();
        require(mc.screen.mouseClicked(x,y,0),"Screen accepted click");mc.screen.mouseReleased(x,y,0);
    }
    private static void click(String key) {
        String label=Component.translatable("gui.frostedresearch.journal."+key).getString();
        var button=layer.getElements().stream().filter(JournalButton.class::isInstance).map(JournalButton.class::cast)
            .filter(b->b.getTitle().getString().equals(label)).findFirst().orElseThrow(()->new IllegalStateException("missing button "+label));
        require(button.isEnabled(),"button enabled: "+label);press(button);
    }
    private static LinkGamePanel panel() { return layer.getElements().stream().filter(LinkGamePanel.class::isInstance).map(LinkGamePanel.class::cast).findFirst().orElse(null); }
    private static ResearchGame board() throws Exception { return (ResearchGame)read(layer,"linkGame"); }
    private static int count(ResearchGame board) { int count=0;for(int x=0;x<9;x++)for(int y=0;y<9;y++)if(board.get(x,y).isShow())count++;return count; }
    private static List<Pair<CardPos,CardPos>> solve(ResearchGame board,Set<String> visited) {
        if(board.isFinished())return new ArrayList<>();
        if(!visited.add(Arrays.toString(board.serialize().getIntArray("cards"))+board.serialize().getInt("cur")))return null;
        var positions=new ArrayList<CardPos>();for(int x=0;x<9;x++)for(int y=0;y<9;y++)if(board.isTouchable(x,y))positions.add(CardPos.valueOf(x,y));
        for(int a=0;a<positions.size();a++)for(int b=a+1;b<positions.size();b++){
            var first=positions.get(a);var second=positions.get(b);if(!board.canCombine(first,second))continue;
            var copy=new ResearchGame(board);copy.tryCombine(first,second);var tail=solve(copy,visited);
            if(tail!=null){tail.add(0,Pair.of(first,second));return tail;}
        }
        var copy=new ResearchGame(board);if(copy.tryCombine(ResearchGame.FINAL_CARD,null)&&copy.isFinished())return new ArrayList<>();
        return null;
    }
    private static void card(CardPos pos) throws Exception {
        var client=(ClientResearchGame)read(panel(),"client");
        for(var child:panel().getElements())if(child instanceof CardButton){
            var field=CardButton.class.getDeclaredField("card");field.setAccessible(true);
            if(pos.equals(field.get(child))){require(client.isTouchable(pos),"ordinary card is exposed");press(child);return;}
        }
        throw new IllegalStateException("missing card widget");
    }
    private static void prepare() {
        var p=player();var level=p.serverLevel();FHScenario.get(p).pauseAct();
        level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            level.setBlockAndUpdate(new BlockPos(x,63,z),Blocks.STONE.defaultBlockState());
            for(int y=64;y<=68;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        p.teleportTo(.5,64,.5);p.getInventory().clearContent();
        var source=new AcquisitionSource("client_fixture",Optional.of(p.getUUID()),level.getGameTime());
        service().learn(ItemRetrieval.observe(p,new ItemStack(Items.RAW_COPPER)),source,KnowledgeService.Grant.NORMAL);
        require(!service().query(IDEA).inArchive()&&!service().query(IDEA).inInbox(),"fresh idea remains unknown");
        require(!p.hasPermissions(2),"normal completion must not use the operator shortcut");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FRContents.Blocks.DRAWING_DESK.get()));
        require(p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(DESK.below()),Direction.UP,DESK.below(),false))).consumesAction(),"fixture desk placed");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);TeamResearchService.send(p,CTeamDataManager.get(p));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||finished)return;
        var mc=Minecraft.getInstance();ticks++;
        try{
            if(failure!=null)throw new IllegalStateException("server fixture",failure);
            if(busy||mc.getOverlay()!=null)return;
            if(phase>=2)require(System.nanoTime()-phaseAt<120_000_000_000L,"phase timeout "+phase);
            if(mc.screen instanceof ImageScreenDialog dialog){if(dialog.shouldCloseOnEsc())dialog.onClose();if(mc.screen instanceof ImageScreenDialog)mc.setScreen(null);return;}
            switch(phase){
                case 0 -> {if(mc.screen==null)return;originalScale=mc.options.guiScale().get();originalFps=mc.options.framerateLimit().get();originalPause=mc.options.pauseOnLostFocus;originalLanguage=mc.options.languageCode;captured=true;
                    mc.options.guiScale().set(3);mc.options.framerateLimit().set(60);mc.options.pauseOnLostFocus=false;mc.options.languageCode="zh_cn";mc.getLanguageManager().setSelected("zh_cn");mc.reloadResourcePacks();next();}
                case 1 -> {if(ticks<20)return;mc.createWorldOpenFlows().createFreshLevel("link-client-"+System.currentTimeMillis(),new LevelSettings("Idea link UI validation",GameType.CREATIVE,false,Difficulty.PEACEFUL,false,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(7131117L,false,false),registries->registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());next();}
                case 2 -> {if(mc.player==null||mc.getSingleplayerServer()==null||mc.screen!=null||ticks<30)return;server(LinkClientValidation::prepare);next();}
                case 3 -> {if(mc.screen==null)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(DESK),Direction.UP,DESK,false));if(!(mc.screen instanceof CUIScreen cui)||!(cui.getPrimaryLayer() instanceof DrawDeskScreen view))return;desk=view;layer=desk.getKnowledgeLayer();if(layer==null)return;next();}
                case 4 -> {if(ticks<15)return;click("page.archive");next();}
                case 5 -> {if(ticks<5)return;var list=layer.getElements().stream().filter(JournalList.class::isInstance).findFirst().orElseThrow();pressAt(list,40,14);next();}
                case 6 -> {if(ticks<5)return;click("tray.add");click("link.try");next();}
                case 7 -> {if(ticks<10)return;shot("01-candidate-two-numbers");click("link.accept");next();}
                case 8 -> {if(ticks<10||panel()==null)return;require((Integer)read(layer,"linkNumbers")==2,"two missing auxiliaries select difficulty two");shot("02-two-number-board");mc.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE,1,0);next();}
                case 9 -> {if(ticks<10||panel()!=null)return;checks.put("escape_cancelled",true);server(()->require(KnowledgeLinkSessions.session(player().getUUID()).isEmpty()&&!service().query(IDEA).inArchive(),"Esc cancels with no idea"));click("link.try");next();}
                case 10 -> {if(ticks<10)return;click("link.accept");next();}
                case 11 -> {if(ticks<10||panel()==null)return;server(()->{var source=new AcquisitionSource("client_fixture",Optional.of(player().getUUID()),player().level().getGameTime());for(var item:List.of(Items.CLAY_BALL,Items.SAND))service().learn(ItemRetrieval.observe(player(),new ItemStack(item)),source,KnowledgeService.Grant.NORMAL);TeamResearchService.send(player(),CTeamDataManager.get(player()));});next();}
                case 12 -> {if(ticks<10||panel()==null)return;require((Integer)read(layer,"linkNumbers")==2,"active board difficulty remains fixed");click("link.restart");next();}
                case 13 -> {if(ticks<10||panel()==null||(Integer)read(layer,"linkNumbers")!=0)return;require(count(board())==9,"zero-number board has nine tiles");shot("03-zero-number-after-reset");moves=solve(new ResearchGame(board()),new HashSet<>());require(moves!=null&&moves.size()==4,"independent client search found four symbol pairs");checks.put("reset_recomputed_to_zero",true);moveIndex=0;next();}
                case 14 -> {if(ticks<4||panel()==null)return;if(remaining>=0){if(count(board())!=remaining)return;remaining=-1;moveIndex++;}if(moveIndex<moves.size()){var pair=moves.get(moveIndex);remaining=count(board())-2;card(pair.getFirst());card(pair.getSecond());ticks=0;return;}require(count(board())==1,"all pairs cleared by Screen events and C2S");card(ResearchGame.FINAL_CARD);next();}
                case 15 -> {if(ticks<15||panel()!=null)return;shot("04-idea-acquired");server(()->{require(service().isActive(IDEA),"normal final click grants the idea");require(KnowledgeLinkSessions.session(player().getUUID()).isEmpty(),"finished session removed");var tile=(DrawingDeskTileEntity)player().level().getBlockEntity(DESK);require(tile.getGame().isEmpty(),"legacy desk game untouched");for(int n=0;n<3;n++)require(tile.getInventory().getStackInSlot(n).isEmpty(),"no paper or ink consumed");});next();}
                case 16 -> {if(ticks<10)return;checks.put("ordinary_C2S_win",true);finish(null);}
            }
        }catch(Throwable error){finish(error);}
    }
    private static void finish(Throwable error){
        finished=true;var mc=Minecraft.getInstance();
        try{Files.createDirectories(OUT);checks.put("status",error==null?"passed":"failed");checks.put("phase",phase);checks.put("input","real Screen mouse/key events and ordinary C2S; fixture world and archive; no physical OS input or survival claim");if(error!=null){checks.put("error",error.toString());shot("failed");}Files.writeString(OUT.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(checks));if(error!=null)FRMain.LOGGER.error("LINK_CLIENT_FAILED",error);}
        catch(Exception logging){FRMain.LOGGER.error("Link client result",logging);}
        finally{if(captured){mc.options.guiScale().set(originalScale);mc.options.framerateLimit().set(originalFps);mc.options.pauseOnLostFocus=originalPause;mc.options.languageCode=originalLanguage;mc.getLanguageManager().setSelected(originalLanguage);}mc.stop();}
    }
}
