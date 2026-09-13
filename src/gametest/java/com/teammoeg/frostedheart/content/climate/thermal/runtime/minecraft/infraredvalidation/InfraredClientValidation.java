/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.infraredvalidation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import com.teammoeg.frostedheart.content.climate.render.infrared.*;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.InfraredRasterValidation;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.BufferUtils;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.lwjgl.opengl.GL33C.*;

/** Isolated developer client. Exercises actual transformed backend classes and the real render loop. */
@Mod.EventBusSubscriber(modid="frostedheart",value=Dist.CLIENT)
public final class InfraredClientValidation {
    private static final AtomicInteger OWNED_QUADS=new AtomicInteger();
    private static final ModelProperty<BlockPos> POSITION=new ModelProperty<>();
    private static final long START=System.nanoTime();
    private static final ShortBuffer FIELD=BufferUtils.createShortBuffer(144*144*144);
    private static int stage, frames, scopeBaseline;
    private static boolean sceneRequested, rasterDone, finished;
    private static java.util.concurrent.CompletableFuture<Void> reload;

    @Mod.EventBusSubscriber(modid="frostedheart",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Models {
        @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult event) {
            if(!Boolean.getBoolean("frostedheart.infraredValidation")) return;
            event.getModels().replaceAll((id,model) -> id.getNamespace().equals("minecraft")
                    && (id.getPath().equals("gold_block")||id.getPath().equals("oak_stairs")) ? new ScopeModel(model) : model);
        }
    }

    private static final class ScopeModel extends BakedModelWrapper<BakedModel> {
        ScopeModel(BakedModel original) { super(original); }
        @Override public ModelData getModelData(BlockAndTintGetter world,BlockPos pos,BlockState state,ModelData data) {
            return super.getModelData(world,pos,state,data).derive().with(POSITION,pos.immutable()).build();
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction face,RandomSource random,ModelData data,RenderType layer) {
            BlockPos pos=data.get(POSITION);
            if(pos!=null) {
                int expected=4096+(pos.getX()&15)+16*(pos.getZ()&15)+256*(pos.getY()&15);
                if(BlockOwnerScope.current()!=expected) throw new AssertionError("Model owner mismatch at "+pos);
                OWNED_QUADS.incrementAndGet();
            }
            return super.getQuads(state,face,random,data,layer);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("frostedheart.infraredValidation")||finished||event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        try {
            if((System.nanoTime()-START)/1_000_000_000L>240) throw new AssertionError("Client validation timeout, stage="+stage);
            if(stage==0 && mc.getOverlay()==null && mc.screen instanceof TitleScreen) {
                stage=1;
                SodiumClientMod.options().performance.useCompactVertexFormat=true;
                mc.createWorldOpenFlows().createFreshLevel("infrared-"+System.currentTimeMillis(),
                        new LevelSettings("Infrared validation",GameType.SPECTATOR,false,Difficulty.PEACEFUL,true,
                                new GameRules(),WorldDataConfiguration.DEFAULT),
                        new WorldOptions(0,false,false),registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            }
            if(stage==1 && mc.level!=null && mc.player!=null && !sceneRequested) {
                sceneRequested=true;
                mc.getSingleplayerServer().execute(() -> {
                    var level=mc.getSingleplayerServer().overworld();
                    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,mc.getSingleplayerServer());
                    level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,mc.getSingleplayerServer());
                    level.setDayTime(6000);
                    for(int x=-3;x<=3;x++) for(int y=78;y<=82;y++)
                        level.setBlockAndUpdate(new BlockPos(x,y,0),(x&1)==0?Blocks.GOLD_BLOCK.defaultBlockState():Blocks.OAK_STAIRS.defaultBlockState());
                    var stand=new net.minecraft.world.entity.decoration.ArmorStand(level,0.5,80,3);
                    stand.setNoGravity(true);
                    level.addFreshEntity(stand);
                    var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    player.teleportTo(0.5,80,9.5);player.setYRot(180);player.setXRot(0);
                });
                InfraredViewRenderer.toggleInfraredView();
            }
            if(sceneRequested && mc.player!=null) {
                mc.player.setYRot(180);mc.player.setXRot(0);
                mc.setCameraEntity(mc.player);
            }
        } catch(Throwable failure) { finish(failure); }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if(!Boolean.getBoolean("frostedheart.infraredValidation")||finished||!sceneRequested) return;
        try {
            if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY) {
                int texture=(int)field(InfraredViewRenderer.class,null,"temperatureTexture");
                if(texture!=0) {
                    FIELD.clear();
                    while(FIELD.hasRemaining()) {
                        int z=FIELD.position()/(144*144);
                        FIELD.put((short)(z>=66&&z<=68?0:FIELD.position()%144<64?-80:80));
                    }
                    FIELD.flip();
                    int active=glGetInteger(GL_ACTIVE_TEXTURE);RenderSystem.activeTexture(GL_TEXTURE2);
                    int previous=glGetInteger(GL_TEXTURE_BINDING_3D);glBindTexture(GL_TEXTURE_3D,texture);
                    int[] keys={GL_UNPACK_ROW_LENGTH,GL_UNPACK_SKIP_ROWS,GL_UNPACK_SKIP_PIXELS,
                            GL_UNPACK_IMAGE_HEIGHT,GL_UNPACK_SKIP_IMAGES,GL_UNPACK_ALIGNMENT};
                    int[] saved=new int[keys.length];
                    for(int i=0;i<keys.length;i++) {
                        saved[i]=glGetInteger(keys[i]);glPixelStorei(keys[i],i==keys.length-1?2:0);
                    }
                    try { glTexSubImage3D(GL_TEXTURE_3D,0,0,0,0,144,144,144,GL_RED_INTEGER,GL_SHORT,FIELD); }
                    finally { for(int i=0;i<keys.length;i++)glPixelStorei(keys[i],saved[i]); }
                    glBindTexture(GL_TEXTURE_3D,previous);RenderSystem.activeTexture(active);
                }
            }
            if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
            if(reload!=null && !reload.isDone()) return;
            if(reload!=null) { reload.join();reload=null; }
            if(++frames<80) return;
            if(stage==5) {
                if((float)field(InfraredViewRenderer.class,null,"radius")>0) return;
                if(field(InfraredViewRenderer.class,null,"surfaceTarget")!=null)
                    throw new AssertionError("Screen targets survived completed shrink");
                saveScreenshot(5);
                InfraredViewRenderer.toggleInfraredView();stage=6;frames=0;
                System.out.println("IR CLIENT shrink/release PASS");
                return;
            }
            if((stage==1||stage==6) && (float)field(InfraredViewRenderer.class,null,"radius")<64) return;
            if(OWNED_QUADS.get()<=scopeBaseline) return;
            var manager=(RenderSectionManager)field(SodiumWorldRenderer.class,SodiumWorldRenderer.instance(),"renderSectionManager");
            var renderer=field(RenderSectionManager.class,manager,"chunkRenderer");
            if(!(renderer instanceof InfraredChunkRenderer)) throw new AssertionError("Factory bridge did not install the renderer");
            var expected=stage==1?OwnedChunkVertexType.COMPACT:OwnedChunkVertexType.HIGH_PRECISION;
            if(manager.getVertexType()!=expected || ((InfraredChunkRenderer)renderer).getVertexType()!=expected)
                throw new AssertionError("Vertex type disagrees with the renderer");
            var surface=(InfraredSurfaceTarget)field(InfraredViewRenderer.class,null,"surfaceTarget");
            if(surface==null || surface.temperatureTexture()==0) throw new AssertionError("No real surface capture");
            int previous=glGetInteger(GL_TEXTURE_BINDING_2D);
            glBindTexture(GL_TEXTURE_2D,surface.temperatureTexture());
            int width=glGetTexLevelParameteri(GL_TEXTURE_2D,0,GL_TEXTURE_WIDTH),height=glGetTexLevelParameteri(GL_TEXTURE_2D,0,GL_TEXTURE_HEIGHT);
            var pixels=BufferUtils.createShortBuffer(width*height);glGetTexImage(GL_TEXTURE_2D,0,GL_RED_INTEGER,GL_SHORT,pixels);
            glBindTexture(GL_TEXTURE_2D,previous);
            long warm=0;for(int i=0;i<pixels.limit();i++)if(pixels.get(i)==80)warm++;
            if(warm==0) return; // Wait for the asynchronous scene upload and the matching display-window response.
            if(glGetError()!=GL_NO_ERROR) throw new AssertionError("Real client GL error");
            System.out.println("IR CLIENT PASS stride="+expected.getVertexFormat().getStride()+", model callbacks="+OWNED_QUADS.get()+", warm pixels="+warm);
            if(field(InfraredViewRenderer.class,null,"infraredProgram")==null)
                throw new AssertionError("Final infrared blend never ran");
            saveScreenshot(stage);
            if(stage==1) {
                stage=2;frames=0;scopeBaseline=OWNED_QUADS.get();
                SodiumClientMod.options().performance.useCompactVertexFormat=false;
                Minecraft.getInstance().levelRenderer.allChanged();
            } else if(stage==2) {
                stage=3;frames=0;
                Minecraft.getInstance().getWindow().setWindowed(800,450);
            } else if(stage==3) {
                stage=4;frames=0;
                reload=Minecraft.getInstance().reloadResourcePacks();
            } else if(stage==4) {
                stage=5;frames=0;InfraredViewRenderer.toggleInfraredView();
            } else if(!rasterDone) {
                rasterDone=true;
                compareFinalTint(pixels,width,height);
                verifyBlendState(event);
                // Resources are loaded from the repository root by the same standalone regression entry.
                InfraredRasterValidation.validateFromClient();
                finish(null);
            }
        } catch(Throwable failure) { finish(failure); }
    }

    private static Object field(Class<?> type,Object instance,String name)throws Exception {
        var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(instance);
    }
    private static void verifyBlendState(RenderLevelStageEvent event) {
        var shader=net.minecraft.client.renderer.GameRenderer.getPositionShader();
        int previousProgram=glGetInteger(GL_CURRENT_PROGRAM);
        var viewport=BufferUtils.createIntBuffer(4);glGetIntegerv(GL_VIEWPORT,viewport);
        var scissorBox=BufferUtils.createIntBuffer(4);glGetIntegerv(GL_SCISSOR_BOX,scissorBox);
        boolean scissor=glIsEnabled(GL_SCISSOR_TEST);
        try {
            shader.apply();
            int expectedProgram=glGetInteger(GL_CURRENT_PROGRAM);
            if(expectedProgram==0)throw new AssertionError("No active vanilla shader for state test");
            RenderSystem.viewport(2,4,320,180);
            RenderSystem.enableScissor(3,5,7,11);
            InfraredViewRenderer.setCameraPose(event.getPoseStack());
            if(!InfraredViewRenderer.visitTerrainPass())throw new AssertionError("No main frame for state test");
            InfraredViewRenderer.renderInfraredView();
            // Applying the same ShaderInstance can skip glUseProgram because Minecraft caches its id.
            shader.apply();
            if(glGetInteger(GL_CURRENT_PROGRAM)!=expectedProgram)
                throw new AssertionError("Infrared blend lost the active vanilla shader");
            var restored=BufferUtils.createIntBuffer(4);glGetIntegerv(GL_VIEWPORT,restored);
            if(restored.get(0)!=2||restored.get(1)!=4||restored.get(2)!=320||restored.get(3)!=180
                    ||!glIsEnabled(GL_SCISSOR_TEST))throw new AssertionError("Infrared blend lost viewport/scissor state");
            System.out.println("IR CLIENT active shader cache/viewport/scissor restore PASS");
        } finally {
            shader.clear();glUseProgram(previousProgram);
            RenderSystem.viewport(viewport.get(0),viewport.get(1),viewport.get(2),viewport.get(3));
            RenderSystem.enableScissor(scissorBox.get(0),scissorBox.get(1),scissorBox.get(2),scissorBox.get(3));
            if(!scissor)RenderSystem.disableScissor();
        }
    }
    private static void saveScreenshot(int stage)throws Exception {
        try(var screenshot=net.minecraft.client.Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            screenshot.writeToFile(Path.of(System.getProperty("frostedheart.validationRoot"),"build","infrared-client-stage-"+stage+".png"));
        }
    }
    private static void compareFinalTint(ShortBuffer temperatures,int width,int height)throws Exception {
        Path output=Path.of(System.getProperty("frostedheart.validationRoot"),"build");
        var original=javax.imageio.ImageIO.read(output.resolve("infrared-client-stage-5.png").toFile());
        var infrared=javax.imageio.ImageIO.read(output.resolve("infrared-client-stage-6.png").toFile());
        var depths=BufferUtils.createFloatBuffer(width*height);
        int previous=glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D,Minecraft.getInstance().getMainRenderTarget().getDepthTextureId());
        glGetTexImage(GL_TEXTURE_2D,0,GL_DEPTH_COMPONENT,GL_FLOAT,depths);glBindTexture(GL_TEXTURE_2D,previous);
        // The stand is at camera distance ~6.5; every terrain face is at least 8.5 away.
        var clip=RenderSystem.getProjectionMatrix().transform(new org.joml.Vector4f(0,0,-8,1));
        float foregroundLimit=clip.z/clip.w*.5f+.5f;
        int checked=0,foreground=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            short temperature=temperatures.get(y*width+x);
            if(temperature!=80&&temperature!=-80)continue;
            boolean entity=depths.get(y*width+x)<foregroundLimit;
            if(entity)foreground++;
            int base=original.getRGB(x,height-y-1),actual=infrared.getRGB(x,height-y-1);
            for(int channel=0;channel<3;channel++) {
                int shift=channel*8;
                double heat=entity ? (channel==2?1:channel==1?.5+(.5-.33)/.33*.5:0)
                        : channel==(temperature==-80?0:2)?1:0;
                int expected=(int)Math.round(((base>>>shift)&255)*.57+heat*255*.43);
                if(Math.abs(((actual>>>shift)&255)-expected)>1)
                    throw new AssertionError("Real final blend mismatch at "+x+","+y+", channel="+channel);
            }
            if((base>>>24)!=(actual>>>24))throw new AssertionError("Real destination alpha changed");
            checked++;
        }
        if(checked==0)throw new AssertionError("No real blend coverage");
        if(foreground==0)throw new AssertionError("No foreground entity coverage");
        System.out.println("IR CLIENT final RGB/alpha/local environment PASS pixels="+checked+", entity pixels="+foreground);
    }
    private static void finish(Throwable failure) {
        finished=true;
        try {
            Path result=Path.of(System.getProperty("frostedheart.validationRoot"),"build","infrared-client-result.txt");
            Files.writeString(result,failure==null?"PASS\n":"FAIL: "+failure+"\n");
        } catch(Exception writeFailure) { writeFailure.printStackTrace(); }
        if(failure!=null) failure.printStackTrace();
        System.out.println(failure==null?"IR CLIENT VALIDATION PASSED":"IR CLIENT VALIDATION FAILED");
        Minecraft.getInstance().stop();
    }
}
