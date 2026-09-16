/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL13C.*;
import static org.lwjgl.opengl.GL14C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.*;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import com.teammoeg.frostedheart.content.climate.render.infrared.BlockOwnerScope;
import com.teammoeg.frostedheart.content.climate.render.infrared.InfraredCaptureShader;
import com.teammoeg.frostedheart.content.climate.render.infrared.InfraredSurfaceTarget;
import com.teammoeg.frostedheart.content.climate.render.infrared.OwnedChunkVertexType;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;

/** Real GPU tests: production encoders, attributes, capture GLSL and surface target. No edge exclusions. */
public final class InfraredRasterValidation {
    private static final int WIDTH=480, HEIGHT=270, SIZE=144;
    private static final String QUAD="""
            #version 150
            void main() {
                vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);
                gl_Position=vec4(p*2.0-1.0,0,1);
            }
            """;

    public static void main(String[] args) throws Exception { offscreen(args.length==0, true); }

    /** The development client can run the same tests without disturbing its GL objects or window. */
    public static void validateFromClient() throws Exception {
        long previous=glfwGetCurrentContext();
        var capabilities=GL.getCapabilities();
        try { offscreen(true, false); }
        finally { glfwMakeContextCurrent(previous); GL.setCapabilities(capabilities); }
    }

    private static void offscreen(boolean planes, boolean terminate) throws Exception {
        if(!glfwInit()) throw new IllegalStateException("GLFW initialization");
        glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3); glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);
        long window=glfwCreateWindow(WIDTH,HEIGHT,"Infrared raster validation",0,0);
        if(window==0) throw new IllegalStateException("Hidden GL context");
        try {
            glfwMakeContextCurrent(window); GL.createCapabilities();
            validate(planes);
        } finally {
            glfwDestroyWindow(window);
            if(terminate) glfwTerminate();
        }
    }

    private static void validate(boolean planes) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        String finalSource=Files.readString(Path.of(System.getProperty("frostedheart.validationRoot","."))
                .resolve("src/main/resources/assets/frostedheart/shaders/infrared_view.fsh"));
        String finalOutput="FragColor = vec4(temperatureToColor(temperature) * 0.43, 0.43);";
        if(!finalSource.contains(finalOutput)) throw new AssertionError("Final-output diagnostic marker changed");
        int diagnostic=program(QUAD,finalSource.replace("uniform float radius;","uniform float radius;\nuniform sampler2D referenceTexture;")
                .replace(finalOutput,"FragColor = value == INVALID_TEMPERATURE ? vec4(0,0,1,1)"
                        + " : value != int(round(texelFetch(referenceTexture,pixel,0).r*255.0)) ? vec4(1,0,0,1) : vec4(1,1,1,1);"));
        int scene=glGenFramebuffers(); glBindFramebuffer(GL_FRAMEBUFFER,scene);
        int color=texture(GL_RGBA8,GL_RGBA,GL_UNSIGNED_BYTE), depth=texture(GL_DEPTH_COMPONENT24,GL_DEPTH_COMPONENT,GL_FLOAT);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,color,0);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,depth,0);
        int resultFbo=glGenFramebuffers(); glBindFramebuffer(GL_FRAMEBUFFER,resultFbo);
        int result=texture(GL_RGBA8,GL_RGBA,GL_UNSIGNED_BYTE);
        glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,result,0);
        InfraredSurfaceTarget surface=new InfraredSurfaceTarget();
        int temperatures=glGenTextures(); glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_3D,temperatures);
        glTexParameteri(GL_TEXTURE_3D,GL_TEXTURE_MIN_FILTER,GL_NEAREST); glTexParameteri(GL_TEXTURE_3D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        ShortBuffer values=BufferUtils.createShortBuffer(SIZE*SIZE*SIZE);
        ByteBuffer pixels=BufferUtils.createByteBuffer(WIDTH*HEIGHT*4);
        int white=atlas(false), cutout=atlas(true);
        long failures=0, classified=0;
        int cases=0;
        for(OwnedChunkVertexType type:new OwnedChunkVertexType[]{OwnedChunkVertexType.COMPACT,OwnedChunkVertexType.HIGH_PRECISION}) {
            verifyEncoding(type);
            for(boolean masked:new boolean[]{false,true}) {
                // Shader specialization only; real RenderType state is tested by the development client.
                var pass=new TerrainRenderPass(null,false,masked);
                var options=new ChunkShaderOptions(ChunkFogMode.SMOOTH,pass,type.base());
                String defines=String.join("\n",options.constants().getDefineStrings())+"\n";
                String vertex=InfraredCaptureShader.vertex(read("sodium","blocks/block_layer_opaque.vsh"),
                        read("frostedheart","infrared_capture.glsl"));
                String fragment=InfraredCaptureShader.fragment(read("sodium","blocks/block_layer_opaque.fsh"));
                int capture=program(withDefines(expand(vertex),defines),withDefines(expand(fragment),defines));
                Material material=new Material(pass,masked?AlphaCutoffParameter.HALF:AlphaCutoffParameter.ZERO,true);
                glUseProgram(capture);
                integer(capture,"u_BlockTex",0); integer(capture,"u_LightTex",1); integer(capture,"fhTemperatureTexture",2);
                integer(capture,"fhCaptureEnabled",1); integer(capture,"u_FogShape",0);
                glUniform4f(glGetUniformLocation(capture,"u_FogColor"),0,0,0,1);
                scalar(capture,"u_FogStart",8192); scalar(capture,"u_FogEnd",16384);
                long groupFailures=0, groupPixels=0; int groupCases=0;
                for(int format:new int[]{GL_DEPTH_COMPONENT24,GL_DEPTH_COMPONENT32F}) {
                    glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,depth);
                    glTexImage2D(GL_TEXTURE_2D,0,format,WIDTH,HEIGHT,0,GL_DEPTH_COMPONENT,GL_FLOAT,0L);
                    surface.close();
                    surface.ensure(WIDTH,HEIGHT,color,depth);
                    if(planes&&!masked) for(int axis=0;axis<3;axis++) for(int sign:new int[]{-1,1}) {
                        try(Meshes meshes=new Meshes(type,material)) {
                            int[] block=new int[3]; int u=(axis+1)%3,v=(axis+2)%3;
                            block[axis]=sign>0?-1:0;
                            for(int a=-80;a<80;a++) for(int b=-80;b<80;b++) {
                                block[u]=a; block[v]=b;
                                meshes.face(block[0],block[1],block[2],axis,sign>0?1:0,new float[]{0,0,0},new float[]{1,1,1},0);
                            }
                            meshes.upload();
                            Matrix4f rotation=new Matrix4f();
                            if(axis==0) rotation.rotationZ(-sign*(float)java.lang.Math.PI/2);
                            else if(axis==2) rotation.rotationX(sign*(float)java.lang.Math.PI/2);
                            else if(sign<0) rotation.rotationX((float)java.lang.Math.PI);
                            for(float height:new float[]{.1f,.49f,1.01f,1.62f,4.2f,16.5f,32.4f,48.5f}) {
                                Vector3f camera=rotation.transformPosition(new Vector3f(5.23f,height,-6.53f));
                                int[] origin=origin(camera.x,camera.y,camera.z);
                                fill(values,origin,axis,sign>0?-1:0); upload(temperatures,values);
                                for(float pitch:new float[]{5,15,35,65,89}) for(float yaw:new float[]{0,17,47}) {
                                    Matrix4f view=new Matrix4f().rotateX(radians(pitch)).rotateY(radians(yaw))
                                            .mul(new Matrix4f(rotation).transpose());
                                    long[] count=draw(meshes,capture,diagnostic,surface,scene,resultFbo,color,depth,white,white,temperatures,
                                            new CameraTransform(camera.x,camera.y,camera.z),view,origin,pixels);
                                    groupFailures+=count[0]; groupPixels+=count[1]; groupCases++;
                                }
                            }
                        }
                    }
                    for(boolean stairs:new boolean[]{false,true}) {
                        int[] origin={-64,-48,-64};
                        fill(values,origin,-1,0); upload(temperatures,values);
                        try(Meshes meshes=new Meshes(type,material)) {
                            for(int z=0;z<3;z++) for(int y=0;y<8;y++) for(int x=0;x<4;x++) {
                                if(!solid(x,y,z)) continue;
                                for(int part=0;part<(stairs?2:1);part++) {
                                    float[] low={0,stairs&&part==1?.5f:0,stairs&&part==1?.5f:0};
                                    float[] high={1,stairs&&part==0?.5f:1,1};
                                    for(int face=0;face<6;face++) {
                                        int axis=face/2,side=face&1,delta=side==0?-1:1;
                                        if(!stairs&&solid(x+(axis==0?delta:0),y+(axis==1?delta:0),z+(axis==2?delta:0))) continue;
                                        meshes.face(x,y,z,axis,side,low,high,value(x,y,z));
                                    }
                                }
                            }
                            meshes.upload();
                            for(float distance:new float[]{3,7,16,40,60}) for(float height:new float[]{1.25f,3.75f,8.5f})
                                for(int angle=0;angle<360;angle+=15) {
                                    double yaw=java.lang.Math.toRadians(angle);
                                    double x=2.03+distance*java.lang.Math.cos(yaw),z=1.61+distance*java.lang.Math.sin(yaw);
                                    Matrix4f view=new Matrix4f().lookAt(new Vector3f(),new Vector3f((float)(2-x),3-height,(float)(1.5-z)),new Vector3f(0,1,0));
                                    long[] count=draw(meshes,capture,diagnostic,surface,scene,resultFbo,color,depth,masked?cutout:white,white,temperatures,
                                            new CameraTransform(x,height,z),view,origin,pixels);
                                    groupFailures+=count[0]; groupPixels+=count[1]; groupCases++;
                                }
                        }
                    }
                }
                System.out.println("stride="+type.getVertexFormat().getStride()+", cutout="+masked+", cases="+groupCases
                        +", classified="+groupPixels+", wrong ownership="+groupFailures);
                cases+=groupCases; failures+=groupFailures; classified+=groupPixels;
                glDeleteProgram(capture);
            }
        }
        verifyBlendAndClear(finalSource, surface, scene, color, depth, pixels);
        verifyNativeDepth(diagnostic, resultFbo, pixels);
        verifyScopeThreads();
        int captureFbo=surface.captureFramebuffer(), blendFbo=surface.blendFramebuffer(), heat=surface.temperatureTexture();
        int terrainDepth=surface.terrainDepthTexture();
        surface.close();
        if(glIsFramebuffer(captureFbo)||glIsFramebuffer(blendFbo)||glIsTexture(heat)||glIsTexture(terrainDepth)||!glIsTexture(color)||!glIsTexture(depth))
            throw new AssertionError("Surface target ownership/lifetime");
        int error=glGetError();
        System.out.println("GPU="+glGetString(GL_RENDERER)+", cases="+cases+", classified="+classified+", failures="+failures+", GL error="+error);
        if(failures!=0||classified==0||error!=GL_NO_ERROR) throw new AssertionError("Infrared raster validation failed");
    }

    private static void verifyBlendAndClear(String source, InfraredSurfaceTarget surface, int scene,
            int color, int depth, ByteBuffer pixels) {
        int shader=program(QUAD,source);
        int vao=glGenVertexArrays();glBindVertexArray(vao);
        int[] temperatures={-32768,-80,-40,0,40,80};
        ShortBuffer heat=BufferUtils.createShortBuffer(WIDTH*HEIGHT);
        for(int y=0;y<HEIGHT;y++) for(int x=0;x<WIDTH;x++) heat.put((short)temperatures[x*6/WIDTH]);
        heat.flip();
        glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,surface.temperatureTexture());
        glTexSubImage2D(GL_TEXTURE_2D,0,0,0,WIDTH,HEIGHT,GL_RED_INTEGER,GL_SHORT,heat);
        glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,depth);
        glUseProgram(shader);integer(shader,"depthTexture",0);integer(shader,"surfaceTemperature",1);
        integer(shader,"environmentTemperature",5);integer(shader,"hasEnvironmentTemperature",0);
        integer(shader,"terrainDepthTexture",2);integer(shader,"hasTerrainDepth",1);
        glActiveTexture(GL_TEXTURE2);glBindTexture(GL_TEXTURE_2D,surface.terrainDepthTexture());
        matrix(shader,"u_InverseViewProjectionMatrix",new Matrix4f());
        glViewport(0,0,WIDTH,HEIGHT);glDisable(GL_SCISSOR_TEST);glDisable(GL_DEPTH_TEST);
        for(float radius:new float[]{64,1.5f,0}) {
            glBindFramebuffer(GL_FRAMEBUFFER,scene);glDepthMask(true);glClearDepth(.5);
            glClearColor(.2f,.4f,.6f,.7f);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
            surface.captureTerrainDepth();
            pixels.clear();glReadPixels(0,0,1,1,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
            int originalAlpha=pixels.get(3)&255;
            glBindFramebuffer(GL_FRAMEBUFFER,surface.blendFramebuffer());glDepthMask(false);
            glEnable(GL_BLEND);glBlendEquationSeparate(GL_FUNC_ADD,GL_FUNC_ADD);
            glBlendFuncSeparate(GL_ONE,GL_ONE_MINUS_SRC_ALPHA,GL_ZERO,GL_ONE);
            scalar(shader,"radius",radius);glDrawArrays(GL_TRIANGLES,0,3);
            pixels.clear();glReadPixels(0,0,WIDTH,HEIGHT,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
            for(int y=0;y<HEIGHT;y++) for(int x=0;x<WIDTH;x++) {
                double u=(x+.5)/WIDTH*2-1,v=(y+.5)/HEIGHT*2-1,d=Math.sqrt(u*u+v*v);
                double[] expected={51,102,153};
                if(d<radius) {
                    if(d>radius-3) {
                        double t=(d-radius+3)/3, edge=t*t*(3-2*t);
                        for(int c=0;c<3;c++)expected[c]+=255*edge;
                    } else {
                        double t=Math.max(0,Math.min(1,(temperatures[x*6/WIDTH]*.25+20)/40));
                        double[] low=t<.33?new double[]{0,0,1}:t<.66?new double[]{1,.5,0}:new double[]{1,1,0};
                        double[] high=t<.33?new double[]{1,.5,0}:t<.66?new double[]{1,1,0}:new double[]{1,0,0};
                        double f=t<.33?t/.33:t<.66?(t-.33)/.33:(t-.66)/.34;
                        for(int c=0;c<3;c++)expected[c]=expected[c]*.57+255*(low[c]+(high[c]-low[c])*f)*.43;
                    }
                }
                int offset=(y*WIDTH+x)*4;
                for(int c=0;c<3;c++) if(Math.abs((pixels.get(offset+c)&255)-Math.min(255,Math.round(expected[c])))>1)
                    throw new AssertionError("Final RGB mismatch, radius="+radius+", x="+x+", y="+y
                            +", channel="+c+", actual="+(pixels.get(offset+c)&255)+", expected="+expected[c]+", GL="+glGetError());
                if((pixels.get(offset+3)&255)!=originalAlpha) throw new AssertionError("Destination alpha changed");
            }
        }
        // A front rectangle occludes alternating hot/cold samples; its entire color must use the same blue tint.
        glBindFramebuffer(GL_FRAMEBUFFER,scene);glDepthMask(true);glClearDepth(.5);
        glClearColor(.2f,.4f,.6f,.7f);glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        surface.captureTerrainDepth();
        glEnable(GL_SCISSOR_TEST);glScissor(0,0,WIDTH,HEIGHT/2);glClearDepth(.25);glClear(GL_DEPTH_BUFFER_BIT);
        glDisable(GL_SCISSOR_TEST);glDepthMask(false);
        glBindFramebuffer(GL_FRAMEBUFFER,surface.blendFramebuffer());scalar(shader,"radius",64);
        glDrawArrays(GL_TRIANGLES,0,3);
        pixels.clear();glReadPixels(0,0,WIDTH,HEIGHT,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
        for(int i=0;i<WIDTH*(HEIGHT/2);i++) {
            int[] expected={29,58,197};
            for(int c=0;c<3;c++)if(Math.abs((pixels.get(i*4+c)&255)-expected[c])>1)
                throw new AssertionError("Occluder inherited background heat at pixel "+i);
        }
        System.out.println("Foreground occludes all background temperature bands PASS");
        int environment=glGenTextures();glActiveTexture(GL_TEXTURE5);glBindTexture(GL_TEXTURE_3D,environment);
        glTexParameteri(GL_TEXTURE_3D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_3D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glTexImage3D(GL_TEXTURE_3D,0,GL_R16I,2,2,2,0,GL_RED_INTEGER,GL_SHORT,BufferUtils.createShortBuffer(8));
        integer(shader,"hasEnvironmentTemperature",1);
        glUniform3f(glGetUniformLocation(shader,"cameraToTemperatureOrigin"),1,1,1);
        glBindFramebuffer(GL_FRAMEBUFFER,scene);glClear(GL_COLOR_BUFFER_BIT);
        glBindFramebuffer(GL_FRAMEBUFFER,surface.blendFramebuffer());glDrawArrays(GL_TRIANGLES,0,3);
        pixels.clear();glReadPixels(0,0,WIDTH,HEIGHT,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
        for(int i=0;i<WIDTH*(HEIGHT/2);i++) {
            int[] expected={139,141,87};
            for(int c=0;c<3;c++)if(Math.abs((pixels.get(i*4+c)&255)-expected[c])>1)
                throw new AssertionError("Occluder did not use its local environment at pixel "+i);
        }
        glDeleteTextures(environment);
        System.out.println("Foreground reads its own zero-degree environment, independent of background bands PASS");
        glDisable(GL_BLEND);
        glEnable(GL_SCISSOR_TEST);glScissor(0,0,1,1);glColorMaski(1,false,false,false,false);
        surface.clear();
        var restoredMask=BufferUtils.createByteBuffer(4);glGetBooleani_v(GL_COLOR_WRITEMASK,1,restoredMask);
        if(!glIsEnabled(GL_SCISSOR_TEST)||restoredMask.get(0)!=0)
            throw new AssertionError("Clear did not restore scissor/color mask");
        glDisable(GL_SCISSOR_TEST);glColorMaski(1,true,true,true,true);
        glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,surface.temperatureTexture());
        heat.clear();glGetTexImage(GL_TEXTURE_2D,0,GL_RED_INTEGER,GL_SHORT,heat);
        while(heat.hasRemaining())if(heat.get()!=-32768)throw new AssertionError("Stale surface after clear");
        int oldTemperature=surface.temperatureTexture();
        surface.ensure(WIDTH/2,HEIGHT/2,color,depth);
        if(surface.temperatureTexture()!=oldTemperature)throw new AssertionError("Resize leaked texture object");
        surface.ensure(WIDTH,HEIGHT,color,depth);
        glDeleteProgram(shader);
        glDeleteVertexArrays(vao);
        System.out.println("Final blend/INVALID/zero/negative/front/alpha, clear-state and resize PASS");
    }

    /** Native fixed-point depth can differ from gl_FragCoord.z, including near quantization boundaries. */
    private static void verifyNativeDepth(int diagnostic,int resultFbo,ByteBuffer pixels) {
        int shader=program("""
                #version 330 core
                uniform vec2 range;
                void main() {
                    vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);
                    gl_Position=vec4(p*2-1,mix(range.x,range.y,p.x)*2-1,1);
                }
                ""","""
                #version 330 core
                layout(location=0) out vec4 color;
                layout(location=1) out int heat;
                void main() { color=vec4(80.0/255.0,0,0,1);heat=80; }
                """);
        int vao=glGenVertexArrays();glBindVertexArray(vao);
        int color=texture(GL_RGBA8,GL_RGBA,GL_UNSIGNED_BYTE);
        int depth=texture(GL_DEPTH_COMPONENT24,GL_DEPTH_COMPONENT,GL_FLOAT);
        long checked=0;
        try(InfraredSurfaceTarget target=new InfraredSurfaceTarget()) {
            for(int format:new int[]{GL_DEPTH_COMPONENT,GL_DEPTH_COMPONENT24,GL_DEPTH_COMPONENT32F}) {
                glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,depth);
                glTexImage2D(GL_TEXTURE_2D,0,format,WIDTH,HEIGHT,0,GL_DEPTH_COMPONENT,GL_FLOAT,0L);
                target.close();target.ensure(WIDTH,HEIGHT,color,depth);
                for(int step=0;step<32;step++) {
                    glBindFramebuffer(GL_FRAMEBUFFER,target.captureFramebuffer());
                    glViewport(0,0,WIDTH,HEIGHT);glDisable(GL_BLEND);glDisable(GL_CULL_FACE);glDisable(GL_SCISSOR_TEST);
                    glEnable(GL_DEPTH_TEST);glDepthFunc(GL_ALWAYS);glDepthMask(true);glUseProgram(shader);
                    float low=step<16?step/32f:.9f+(step-16)*.006f;
                    glUniform2f(glGetUniformLocation(shader,"range"),low,Math.min(.99999f,low+.1f));
                    glDrawArrays(GL_TRIANGLES,0,3);target.captureTerrainDepth();
                    glBindFramebuffer(GL_FRAMEBUFFER,resultFbo);glDisable(GL_DEPTH_TEST);glDepthMask(false);
                    glUseProgram(diagnostic);matrix(diagnostic,"u_InverseViewProjectionMatrix",new Matrix4f());
                    integer(diagnostic,"referenceTexture",0);integer(diagnostic,"depthTexture",1);
                    integer(diagnostic,"surfaceTemperature",3);integer(diagnostic,"terrainDepthTexture",4);
                    integer(diagnostic,"environmentTemperature",5);integer(diagnostic,"hasEnvironmentTemperature",0);
                    integer(diagnostic,"hasTerrainDepth",1);scalar(diagnostic,"radius",64);
                    glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,color);
                    glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,depth);
                    glActiveTexture(GL_TEXTURE3);glBindTexture(GL_TEXTURE_2D,target.temperatureTexture());
                    glActiveTexture(GL_TEXTURE4);glBindTexture(GL_TEXTURE_2D,target.terrainDepthTexture());
                    glDrawArrays(GL_TRIANGLES,0,3);
                    pixels.clear();glReadPixels(0,0,WIDTH,HEIGHT,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
                    for(int i=0;i<WIDTH*HEIGHT;i++) {
                        for(int c=0;c<4;c++)if((pixels.get(i*4+c)&255)!=255)
                            throw new AssertionError("Native depth classified terrain as an occluder: format="+format+", step="+step+", pixel="+i);
                        checked++;
                    }
                }
            }
        } finally {
            glDepthFunc(GL_LESS);glDeleteTextures(color);glDeleteTextures(depth);
            glDeleteProgram(shader);glDeleteVertexArrays(vao);
        }
        System.out.println("Native default/D24/D32F depth equality PASS pixels="+checked);
    }

    private static void verifyScopeThreads() throws Exception {
        var barrier=new java.util.concurrent.CyclicBarrier(2);
        var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread[] threads=new Thread[2];
        for(int i=0;i<threads.length;i++) {
            final int x=i;
            threads[i]=new Thread(() -> {
                int previous=BlockOwnerScope.enter(x,15,-1);
                try {
                    barrier.await();
                    if(BlockOwnerScope.current()!=(4096|x|240|3840))throw new AssertionError("Worker owner crossed threads");
                    int nested=BlockOwnerScope.enter(7,8,9);
                    try { throw new IllegalArgumentException("Deliberate model failure"); }
                    catch(IllegalArgumentException expected) { /* Exercise the same finally contract as model emission. */ }
                    finally { BlockOwnerScope.restore(nested); }
                    if(BlockOwnerScope.current()!=nested)throw new AssertionError("Nested owner not restored");
                } catch(Throwable e) { failure.set(e); }
                finally { BlockOwnerScope.restore(previous); }
                if(BlockOwnerScope.current()!=0)failure.set(new AssertionError("Worker owner leaked"));
            });
            threads[i].start();
        }
        for(Thread thread:threads)thread.join();
        if(failure.get()!=null)throw new AssertionError("Worker scope",failure.get());
        System.out.println("Concurrent/nested/exception owner scope PASS");
    }

    private static long[] draw(Meshes meshes,int capture,int diagnostic,InfraredSurfaceTarget surface,int scene,int target,
            int color,int depth,int diffuse,int light,int temperatures,CameraTransform camera,Matrix4f view,int[] origin,ByteBuffer pixels) {
        Matrix4f projection=new Matrix4f().perspective(radians(70),WIDTH/(float)HEIGHT,.05f,256);
        glBindFramebuffer(GL_FRAMEBUFFER,scene); glViewport(0,0,WIDTH,HEIGHT); glDisable(GL_BLEND); glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST); glDepthMask(true); glClearColor(0,0,0,0); glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        surface.clear(); glBindFramebuffer(GL_FRAMEBUFFER,surface.captureFramebuffer()); glUseProgram(capture);
        matrix(capture,"u_ProjectionMatrix",projection); matrix(capture,"u_ModelViewMatrix",view);
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,diffuse);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D,light);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_3D,temperatures);
        for(var entry:meshes.regions.entrySet()) {
            Region r=entry.getKey(); Mesh mesh=entry.getValue();
            glUniform3f(glGetUniformLocation(capture,"u_RegionOffset"),(r.x-camera.intX)-camera.fracX,(r.y-camera.intY)-camera.fracY,(r.z-camera.intZ)-camera.fracZ);
            glUniform3i(glGetUniformLocation(capture,"fhRegionToTextureOrigin"),r.x-origin[0],r.y-origin[1],r.z-origin[2]);
            glBindVertexArray(mesh.vao); glDrawElements(GL_TRIANGLES,mesh.vertices/4*6,GL_UNSIGNED_INT,0);
        }
        surface.captureTerrainDepth();
        glBindFramebuffer(GL_FRAMEBUFFER,target); glDisable(GL_DEPTH_TEST); glDepthMask(false); glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(diagnostic); matrix(diagnostic,"u_InverseViewProjectionMatrix",new Matrix4f(projection).mul(view).invert());
        integer(diagnostic,"referenceTexture",0); integer(diagnostic,"depthTexture",1); integer(diagnostic,"surfaceTemperature",3); scalar(diagnostic,"radius",64);
        integer(diagnostic,"terrainDepthTexture",4);integer(diagnostic,"hasTerrainDepth",1);
        integer(diagnostic,"environmentTemperature",5);integer(diagnostic,"hasEnvironmentTemperature",0);
        glActiveTexture(GL_TEXTURE4);glBindTexture(GL_TEXTURE_2D,surface.terrainDepthTexture());
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,color);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D,depth);
        glActiveTexture(GL_TEXTURE3); glBindTexture(GL_TEXTURE_2D,surface.temperatureTexture());
        glDrawArrays(GL_TRIANGLES,0,3);
        pixels.clear(); glReadPixels(0,0,WIDTH,HEIGHT,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
        long wrong=0,classified=0;
        for(int i=0;i<WIDTH*HEIGHT;i++) if((pixels.get(i*4+3)&255)==255) {
            classified++;
            if((pixels.get(i*4)&255)<240||(pixels.get(i*4+1)&255)<240||(pixels.get(i*4+2)&255)<240) wrong++;
        }
        return new long[]{wrong,classified};
    }

    private static void verifyEncoding(OwnedChunkVertexType type) {
        long a=MemoryUtil.nmemAlloc(64),b=MemoryUtil.nmemAlloc(64);
        var vertex=new ChunkVertexEncoder.Vertex(); vertex.x=3.125f;vertex.y=4.5f;vertex.z=2.75f;
        vertex.u=.25f;vertex.v=.75f;vertex.color=0xaabbccdd;
        var material=new Material(null,AlphaCutoffParameter.HALF,true);
        try {
            for(int owner=0;owner<4096;owner++) for(int light:new int[]{0,0x00100001,0x00f000f0,0x00ff00ff}) {
                vertex.light=light;
                int x=owner%16,z=owner/16%16,y=owner/256;
                int previous=BlockOwnerScope.enter(x,y,z);
                try {
                    type.base().getEncoder().write(a,material,vertex,255);
                    long end=type.getEncoder().write(b,material,vertex,255);
                    if(end-b!=type.getVertexFormat().getStride()) throw new AssertionError("Writer stride");
                    int prefix=type==OwnedChunkVertexType.COMPACT?16:28;
                    for(int i=0;i<prefix;i++) if(MemoryUtil.memGetByte(a+i)!=MemoryUtil.memGetByte(b+i)) throw new AssertionError("Base attributes changed");
                    int offset=type==OwnedChunkVertexType.COMPACT?18:28;
                    if(Short.toUnsignedInt(MemoryUtil.memGetShort(b+offset))!=4096+owner) throw new AssertionError("Owner encoding");
                    if(offset==18&&((MemoryUtil.memGetByte(b+16)&255)!=(light&255)||(MemoryUtil.memGetByte(b+17)&255)!=((light>>>16)&255)))
                        throw new AssertionError("Light channel changed");
                } finally { BlockOwnerScope.restore(previous); }
            }
            if(BlockOwnerScope.current()!=0) throw new AssertionError("Owner scope leaked");
        } finally { MemoryUtil.nmemFree(a); MemoryUtil.nmemFree(b); }
    }

    private static final class Meshes implements AutoCloseable {
        final OwnedChunkVertexType type; final Material material;
        final Map<Region,Mesh> regions=new LinkedHashMap<>();
        final ChunkVertexEncoder.Vertex vertex=new ChunkVertexEncoder.Vertex();
        Meshes(OwnedChunkVertexType type,Material material){this.type=type;this.material=material;}
        void face(int x,int y,int z,int axis,int side,float[] low,float[] high,int id) {
            Region region=new Region(Math.floorDiv(x,128)*128,Math.floorDiv(y,64)*64,Math.floorDiv(z,128)*128);
            Mesh mesh=regions.computeIfAbsent(region,r->new Mesh(type));
            int section=((x>>4)&7)<<5|((y>>4)&3)|(((z>>4)&7)<<2);
            int previous=BlockOwnerScope.enter(x,y,z);
            try {
                int u=(axis+1)%3,v=(axis+2)%3;
                for(int corner=0;corner<4;corner++) {
                    float[] point={x&15,y&15,z&15};
                    point[axis]+=side==0?low[axis]:high[axis];
                    int cu=corner==1||corner==2?1:0,cv=corner>=2?1:0;
                    point[u]+=low[u]+(high[u]-low[u])*cu; point[v]+=low[v]+(high[v]-low[v])*cv;
                    vertex.x=point[0];vertex.y=point[1];vertex.z=point[2];vertex.u=cu;vertex.v=cv;
                    vertex.color=0xff808000|id;vertex.light=0x00f000f0;
                    mesh.add(vertex,material,section);
                }
            } finally { BlockOwnerScope.restore(previous); }
        }
        void upload(){regions.values().forEach(Mesh::upload);}
        @Override public void close(){regions.values().forEach(Mesh::close);}
    }
    private record Region(int x,int y,int z) {}
    private static final class Mesh implements AutoCloseable {
        final OwnedChunkVertexType type;
        ByteBuffer bytes; int vertices,vao,vbo,ebo;
        Mesh(OwnedChunkVertexType type){this.type=type;bytes=MemoryUtil.memAlloc(4096*type.getVertexFormat().getStride());}
        void add(ChunkVertexEncoder.Vertex vertex,Material material,int section) {
            int stride=type.getVertexFormat().getStride();
            if((vertices+1)*stride>bytes.capacity()) bytes=MemoryUtil.memRealloc(bytes,bytes.capacity()*2);
            type.getEncoder().write(MemoryUtil.memAddress(bytes)+(long)vertices*stride,material,vertex,section); vertices++;
        }
        void upload() {
            vao=glGenVertexArrays();vbo=glGenBuffers();ebo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);
            bytes.limit(vertices*type.getVertexFormat().getStride());glBufferData(GL_ARRAY_BUFFER,bytes,GL_STATIC_DRAW);MemoryUtil.memFree(bytes);bytes=null;
            var indices=MemoryUtil.memAllocInt(vertices/4*6);
            for(int i=0;i<vertices;i+=4) indices.put(i).put(i+1).put(i+2).put(i).put(i+2).put(i+3);
            indices.flip();glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);glBufferData(GL_ELEMENT_ARRAY_BUFFER,indices,GL_STATIC_DRAW);MemoryUtil.memFree(indices);
            for(var a:type.bindings()) {
                glEnableVertexAttribArray(a.getIndex());
                if(a.isIntType()) glVertexAttribIPointer(a.getIndex(),a.getCount(),a.getFormat(),a.getStride(),a.getPointer());
                else glVertexAttribPointer(a.getIndex(),a.getCount(),a.getFormat(),a.isNormalized(),a.getStride(),a.getPointer());
            }
        }
        @Override public void close(){if(bytes!=null)MemoryUtil.memFree(bytes);glDeleteBuffers(vbo);glDeleteBuffers(ebo);glDeleteVertexArrays(vao);}
    }

    private static boolean solid(int x,int y,int z){return x>=0&&x<4&&z>=0&&z<3&&y>=0&&y<2+x+(z&1)*2;}
    private static int value(int x,int y,int z){return 1+x+4*(z+3*y);}
    private static int[] origin(double x,double y,double z){return new int[]{(int)Math.floor(x/16)*16-64,(int)Math.floor(y/16)*16-64,(int)Math.floor(z/16)*16-64};}
    private static void fill(ShortBuffer data,int[] origin,int axis,int plane) {
        data.clear();
        for(int z=0;z<SIZE;z++)for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++) {
            int bx=x+origin[0],by=y+origin[1],bz=z+origin[2];
            data.put(axis>=0 ? (axis==0?bx:axis==1?by:bz)==plane?(short)0:Short.MIN_VALUE
                    : solid(bx,by,bz)?(short)value(bx,by,bz):Short.MIN_VALUE);
        }
        data.flip();
    }
    private static void upload(int texture,ShortBuffer data){glActiveTexture(GL_TEXTURE2);glBindTexture(GL_TEXTURE_3D,texture);glTexImage3D(GL_TEXTURE_3D,0,GL_R16I,SIZE,SIZE,SIZE,0,GL_RED_INTEGER,GL_SHORT,data);}
    private static int atlas(boolean mask){
        int t=glGenTextures();glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,t);ByteBuffer b=BufferUtils.createByteBuffer(16);
        for(int i=0;i<4;i++)b.putInt(mask&&(i==0||i==3)?0x00ffffff:0xffffffff);b.flip();
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,2,2,0,GL_RGBA,GL_UNSIGNED_BYTE,b);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);return t;
    }
    private static int texture(int internal,int format,int type){int t=glGenTextures();glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,t);glTexImage2D(GL_TEXTURE_2D,0,internal,WIDTH,HEIGHT,0,format,type,0L);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);return t;}
    private static String read(String namespace,String name)throws java.io.IOException {
        Class<?> resources=namespace.equals("sodium")
                ? me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader.class : InfraredRasterValidation.class;
        try(var in=resources.getResourceAsStream("/assets/"+namespace+"/shaders/"+name)) {
            if(in==null)throw new java.io.IOException(name);
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String expand(String text)throws java.io.IOException {StringBuilder out=new StringBuilder();for(String line:text.split("\n")){if(line.startsWith("#import <")){String id=line.substring(9,line.indexOf('>'));int colon=id.indexOf(':');out.append(expand(read(id.substring(0,colon),id.substring(colon+1))));}else out.append(line).append('\n');}return out.toString();}
    private static String withDefines(String source,String defines){int newline=source.indexOf('\n');return source.substring(0,newline+1)+defines+source.substring(newline+1);}
    private static int program(String vertex,String fragment){int p=glCreateProgram();for(int type:new int[]{GL_VERTEX_SHADER,GL_FRAGMENT_SHADER}){int s=glCreateShader(type);glShaderSource(s,type==GL_VERTEX_SHADER?vertex:fragment);glCompileShader(s);if(glGetShaderi(s,GL_COMPILE_STATUS)==0)throw new IllegalStateException(glGetShaderInfoLog(s));glAttachShader(p,s);glDeleteShader(s);}glBindAttribLocation(p,0,"a_PosId");glBindAttribLocation(p,1,"a_Color");glBindAttribLocation(p,2,"a_TexCoord");glBindAttribLocation(p,3,"a_LightCoord");glLinkProgram(p);if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(p));return p;}
    private static float radians(float degrees){return (float)java.lang.Math.toRadians(degrees);}
    private static void matrix(int p,String name,Matrix4f value){glUniformMatrix4fv(glGetUniformLocation(p,name),false,value.get(new float[16]));}
    private static void integer(int p,String name,int value){glUniform1i(glGetUniformLocation(p,name),value);}
    private static void scalar(int p,String name,float value){glUniform1f(glGetUniformLocation(p,name),value);}
}
