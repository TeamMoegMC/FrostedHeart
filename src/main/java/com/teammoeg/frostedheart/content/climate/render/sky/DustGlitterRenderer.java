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

import com.lowdragmc.lowdraglib.client.shader.management.Shader;
import com.lowdragmc.lowdraglib.client.shader.management.ShaderProgram;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.client.FHShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Locale;

/**
 * Draws the glints of diamond dust near the eye as one instanced draw of quads after the weather, in frames where the
 * calculated sky was drawn and there is diamond dust, for the field that DustGlitterField describes. The quads have no
 * vertex data, and diamond_dust.glsl places, moves, and lights each point's glint from its index with the sky's halo
 * profile and halo maps and the light's beam at the eye, so the moon's glints need nothing more. Each glint is a round
 * sprite with a Gaussian core of dustGlitterSize, which widens where it saturates and near the eye. They draw into the
 * main target with its depth test, so the scene hides them, add to it with a screen blend, and write no depth. They
 * fade with the rain level. The clock of their lives follows time, stands still while the game is paused, and stays at
 * 0 while the screenshot harness takes shots. Render thread only.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DustGlitterRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    private static final int KINDS = IceHaloMap.MIRROR_KINDS;
    private static final int HALO_PROFILE_UNIT = 0;
    private static final int LOW_HALO_MAP_UNIT = 1;
    private static final int HIGH_HALO_MAP_UNIT = 2;
    private static final int TEXTURE_UNITS = 3;
    /**
     * The clock advances by at most this many seconds a frame, so it does not jump after a stall.
     */
    private static final double LONGEST_FRAME_SECONDS = 0.1;
    /**
     * The window height at which dustGlitterSize holds in pixels. Sprites scale with the height.
     */
    private static final float REFERENCE_WINDOW_HEIGHT = 1080.0F;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer lobeScaleBuffer = BufferUtils.createFloatBuffer(CHANNELS * KINDS);
    private static final IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
    private static final Matrix4f viewProjection = new Matrix4f();
    private static final double[] beam = new double[CHANNELS];
    private static final double[] tiltSigmas = new double[KINDS];
    private static final double[] halfHeightsOverRadius = new double[KINDS];
    private static final int[] previousTextures = new int[TEXTURE_UNITS];
    private static final SkyDrawTimer timer = new SkyDrawTimer();
    private static final GlitterProgram program = new GlitterProgram();

    /**
     * An empty vertex array, which a core profile needs to draw without vertex data.
     */
    private static int vertexArray;
    private static double clockSeconds;
    private static long lastClockNanos;
    private static boolean failed;
    private static int drawnPoints;
    private static float drawnExtinctionShare;
    private static long secondStartNanos;
    private static long currentSecondBytes;
    private static int currentSecondFrames;
    private static long lastSecondBytes;
    private static int lastSecondFrames;

    private DustGlitterRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        drawnPoints = 0;
        ClientLevel level = Minecraft.getInstance().level;
        if (failed || level == null || !CalculatedSkyRenderer.isDrawnThisFrame()) {
            return;
        }
        long allocatedBefore = LightPillarSources.allocatedBytes();
        try {
            render(level, event);
        } catch (RuntimeException exception) {
            failed = true;
            LOGGER.error("Diamond dust glints are not drawn after an error. They are drawn again once the calculated "
                    + "sky is turned on again.", exception);
        }
        countAllocations(LightPillarSources.allocatedBytes() - allocatedBefore);
    }

    private static void render(ClientLevel level, RenderLevelStageEvent event) {
        int count = (int) Math.round(SkySetting.DUST_GLITTER_COUNT.get());
        double brightness = DustGlitterField.CALIBRATED_BRIGHTNESS * SkySetting.DUST_GLITTER_BRIGHTNESS.get();
        if (count <= 0 || !(brightness > 0.0)) {
            return;
        }
        SkyFrame frame = SkyFrame.current(level, event.getPartialTick());
        double extinctionShare = DustGlitterField.extinctionShare(frame.dustExtinction());
        double dustBottomY = LightPillarSources.dustBottomY();
        double gain = brightness * (1.0 - frame.rainLevel());
        SkyState state = CalculatedSkyRenderer.drawnState();
        if (!(extinctionShare > 0.0) || Double.isNaN(dustBottomY) || !(gain > 0.0) || state == null) {
            return;
        }
        state.views().lightTransmittance(CalculatedSkyRenderer.viewBlend(state, frame),
                CalculatedSkyRenderer.depthBlend(state, frame), CalculatedSkyRenderer.lightScale(state, frame), beam);
        if (!(Math.max(beam[0], Math.max(beam[1], beam[2])) > 0.0)) {
            return;
        }
        advanceClock();
        Vec3 camera = event.getCamera().getPosition();
        viewProjection.set(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());
        timer.begin();
        try {
            draw(state, frame, camera, count, (float) extinctionShare, (float) (dustBottomY - camera.y), gain);
        } finally {
            timer.end();
        }
        drawnPoints = count;
        drawnExtinctionShare = (float) extinctionShare;
    }

    private static void advanceClock() {
        long now = System.nanoTime();
        if (CalculatedSkyScreenshots.isRunning()) {
            clockSeconds = 0.0;
        } else if (lastClockNanos != 0L && !Minecraft.getInstance().isPaused()) {
            double seconds = (now - lastClockNanos) * 1.0e-9;
            clockSeconds += Math.min(Math.max(seconds, 0.0), LONGEST_FRAME_SECONDS);
        }
        lastClockNanos = now;
    }

    private static void countAllocations(long bytes) {
        long now = System.nanoTime();
        if (secondStartNanos == 0L || now - secondStartNanos >= NANOS_PER_SECOND) {
            lastSecondBytes = currentSecondBytes;
            lastSecondFrames = currentSecondFrames;
            currentSecondBytes = 0L;
            currentSecondFrames = 0;
            secondStartNanos = now;
        }
        currentSecondBytes += bytes;
        currentSecondFrames++;
    }

    /**
     * Draws the glints into the main target, binding its framebuffer for drawing, which in Fabulous graphics replaces
     * the weather target bound at this stage. Face culling is off, since the quads' winding is not kept. Everything it
     * changes is set back after, with the framebuffer bound before, and vanilla's next immediate draw binds its own
     * vertex array again.
     */
    private static void draw(SkyState state, SkyFrame frame, Vec3 camera, int count, float extinctionShare,
            float dustBottomHeight, double gain) {
        RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousVertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        boolean blending = GL11.glIsEnabled(GL11.GL_BLEND);
        int sourceColor = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int destinationColor = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int colorEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        int alphaEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthWrites = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean culling = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        viewportBuffer.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuffer);
        int viewportX = viewportBuffer.get(0);
        int viewportY = viewportBuffer.get(1);
        int viewportWidth = viewportBuffer.get(2);
        int viewportHeight = viewportBuffer.get(3);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        for (int unit = 0; unit < TEXTURE_UNITS; unit++) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
            previousTextures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        try {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mainTarget.frameBufferId);
            RenderSystem.viewport(0, 0, mainTarget.width, mainTarget.height);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
            RenderSystem.blendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
            RenderSystem.disableCull();
            bindTexture(HALO_PROFILE_UNIT, CalculatedSkyRenderer.haloProfileTexture());
            bindTexture(LOW_HALO_MAP_UNIT, CalculatedSkyRenderer.haloMapTexture(0));
            bindTexture(HIGH_HALO_MAP_UNIT, CalculatedSkyRenderer.haloMapTexture(1));
            program.use(FHShaders.getDiamondDustVertex(), FHShaders.getDiamondDustFragment());
            program.setFrameUniforms(state, frame, camera, extinctionShare, dustBottomHeight, gain, mainTarget.width,
                    mainTarget.height);
            if (vertexArray == 0) {
                vertexArray = GL30.glGenVertexArrays();
            }
            GL30.glBindVertexArray(vertexArray);
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 4, count);
        } finally {
            GL30.glBindVertexArray(previousVertexArray);
            BufferUploader.invalidate();
            if (culling) {
                RenderSystem.enableCull();
            }
            GL20.glUseProgram(previousProgram);
            GL20.glBlendEquationSeparate(colorEquation, alphaEquation);
            RenderSystem.blendFuncSeparate(sourceColor, destinationColor, sourceAlpha, destinationAlpha);
            if (blending) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            RenderSystem.depthMask(depthWrites);
            RenderSystem.depthFunc(depthFunction);
            if (depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            RenderSystem.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
            for (int unit = 0; unit < TEXTURE_UNITS; unit++) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
                RenderSystem.bindTexture(previousTextures[unit]);
            }
            RenderSystem.activeTexture(activeTexture);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFramebuffer);
        }
    }

    private static void bindTexture(int unit, int texture) {
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
        RenderSystem.bindTexture(texture);
    }

    /**
     * The program of the two stages of diamond_dust.glsl and the locations of the uniforms that draw sets every frame.
     * It is made again when a resource reload replaced its stages.
     */
    private static final class GlitterProgram {
        @Nullable
        private ShaderProgram program;
        @Nullable
        private Shader vertex;
        @Nullable
        private Shader fragment;
        private boolean fixedUniformsSet;
        private int kinds;
        private int viewProjectionUniform;
        private int eyeOffsetUniform;
        private int clockHighUniform;
        private int clockLowUniform;
        private int lifeRateUniform;
        private int timeScaleUniform;
        private int sizeSpreadUniform;
        private int fallSpeedUniform;
        private int extinctionShareUniform;
        private int dustBottomHeightUniform;
        private int plainShareUniform;
        private int ringPhaseUniform;
        private int halfWidthUniform;
        private int pixelToClipUniform;
        private int lightUniform;
        private int exposureUniform;
        private int lightDirectionUniform;
        private int lightSideUniform;
        private int lightElevationUniform;
        private int haloBlendUniform;
        private int lobeScalesUniform;
        private int blurVarianceUniform;

        private void use(Shader newVertex, Shader newFragment) {
            if (program == null || vertex != newVertex || fragment != newFragment) {
                if (program != null) {
                    program.delete();
                    program = null;
                }
                ShaderProgram created = new ShaderProgram();
                try {
                    created.attach(newVertex).attach(newFragment);
                } catch (RuntimeException exception) {
                    created.delete();
                    throw exception;
                }
                program = created;
                vertex = newVertex;
                fragment = newFragment;
                fixedUniformsSet = false;
            }
            program.use();
            if (!fixedUniformsSet) {
                setFixedUniforms(program.programId);
                fixedUniformsSet = true;
            }
        }

        /**
         * Sets the samplers and the uniforms that never change on the program in use, with the plate kinds of
         * crystals.csv, and looks up the locations of the uniforms that draw sets every frame.
         */
        private void setFixedUniforms(int programId) {
            kinds = DustGlitterField.plateKinds(IceCrystalData.get(), tiltSigmas, halfHeightsOverRadius);
            GL20.glUniform1i(GL20.glGetUniformLocation(programId, "haloProfile"), HALO_PROFILE_UNIT);
            GL20.glUniform1i(GL20.glGetUniformLocation(programId, "lowHaloMap"), LOW_HALO_MAP_UNIT);
            GL20.glUniform1i(GL20.glGetUniformLocation(programId, "highHaloMap"), HIGH_HALO_MAP_UNIT);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "haloLowestElevation"),
                    (float) IceHaloMap.LOWEST_ELEVATION);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "haloElevationSpan"),
                    (float) (IceHaloMap.ELEVATION_BINS * IceHaloMap.BIN_STEP));
            GL20.glUniform2f(GL20.glGetUniformLocation(programId, "tiltSigmas"),
                    kinds > 0 ? (float) tiltSigmas[0] : 0.0F, kinds > 1 ? (float) tiltSigmas[1] : 0.0F);
            GL20.glUniform3f(GL20.glGetUniformLocation(programId, "refractiveIndices"),
                    (float) IceHaloFormulas.refractiveIndex(0), (float) IceHaloFormulas.refractiveIndex(1),
                    (float) IceHaloFormulas.refractiveIndex(2));
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "rowFadeAngle"),
                    (float) DustGlitterField.ROW_FADE_RADIANS);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "scaleHeight"),
                    (float) LightPillarSelection.DUST_SCALE_HEIGHT);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "cellSize"), DustGlitterField.CELL_SIZE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "fadeStart"), DustGlitterField.FADE_START);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "fadeEnd"), DustGlitterField.FADE_END);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "falloffDistance"),
                    DustGlitterField.FALLOFF_DISTANCE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "spriteSpread"),
                    (float) (0.5 * DustGlitterField.SPRITE_RADIUS_SIGMAS * DustGlitterField.SPRITE_RADIUS_SIGMAS));
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "clockWrap"),
                    (float) DustGlitterField.CLOCK_WRAP_SECONDS);
            GL20.glUniform1i(GL20.glGetUniformLocation(programId, "leastLifeCycles"),
                    DustGlitterField.LEAST_LIFE_CYCLES);
            GL20.glUniform1i(GL20.glGetUniformLocation(programId, "lifeCycleRange"), DustGlitterField.LIFE_CYCLE_RANGE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "slowestFallShare"),
                    DustGlitterField.SLOWEST_FALL_SHARE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "fallShareRange"), DustGlitterField.FALL_SHARE_RANGE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "widestTilt"), DustGlitterField.WIDEST_TILT_RADIANS);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "litTimeBase"), DustGlitterField.LIT_TIME_BASE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "litTimeSlope"), DustGlitterField.LIT_TIME_SLOPE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "litTimeTail"), DustGlitterField.LIT_TIME_TAIL);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "litTimeTailPower"),
                    DustGlitterField.LIT_TIME_TAIL_POWER);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "litRamp"), DustGlitterField.LIT_RAMP_SECONDS);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "shownMargin"), DustGlitterField.SHOWN_MARGIN);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "tintShare"), DustGlitterField.TINT_SHARE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "nearShare"), DustGlitterField.NEAR_SHARE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "nearShareDistance"),
                    DustGlitterField.NEAR_SHARE_DISTANCE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "nearDistance"), DustGlitterField.NEAR_DISTANCE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "widestNear"), DustGlitterField.WIDEST_NEAR);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "nearestDistance"),
                    DustGlitterField.NEAREST_DISTANCE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "nearFadeEnd"), DustGlitterField.NEAR_FADE_END);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "sizeDistance"), DustGlitterField.SIZE_DISTANCE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "smallestSize"), DustGlitterField.SMALLEST_SIZE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "largestSize"), DustGlitterField.LARGEST_SIZE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "smallestRadius"),
                    DustGlitterField.SMALLEST_SPRITE_RADIUS);
            viewProjectionUniform = GL20.glGetUniformLocation(programId, "viewProjection");
            eyeOffsetUniform = GL20.glGetUniformLocation(programId, "eyeOffset");
            clockHighUniform = GL20.glGetUniformLocation(programId, "clockHigh");
            clockLowUniform = GL20.glGetUniformLocation(programId, "clockLow");
            lifeRateUniform = GL20.glGetUniformLocation(programId, "lifeRate");
            timeScaleUniform = GL20.glGetUniformLocation(programId, "timeScale");
            sizeSpreadUniform = GL20.glGetUniformLocation(programId, "sizeSpread");
            fallSpeedUniform = GL20.glGetUniformLocation(programId, "fallSpeed");
            extinctionShareUniform = GL20.glGetUniformLocation(programId, "extinctionShare");
            dustBottomHeightUniform = GL20.glGetUniformLocation(programId, "dustBottomHeight");
            plainShareUniform = GL20.glGetUniformLocation(programId, "plainShare");
            ringPhaseUniform = GL20.glGetUniformLocation(programId, "ringPhase");
            halfWidthUniform = GL20.glGetUniformLocation(programId, "halfWidth");
            pixelToClipUniform = GL20.glGetUniformLocation(programId, "pixelToClip");
            lightUniform = GL20.glGetUniformLocation(programId, "light");
            exposureUniform = GL20.glGetUniformLocation(programId, "exposure");
            lightDirectionUniform = GL20.glGetUniformLocation(programId, "lightDirection");
            lightSideUniform = GL20.glGetUniformLocation(programId, "lightSide");
            lightElevationUniform = GL20.glGetUniformLocation(programId, "lightElevation");
            haloBlendUniform = GL20.glGetUniformLocation(programId, "haloBlend");
            lobeScalesUniform = GL20.glGetUniformLocation(programId, "lobeScales");
            blurVarianceUniform = GL20.glGetUniformLocation(programId, "blurVariance");
        }

        /**
         * Sets the uniforms of this frame on the program in use. The eye reaches it as its offset in its cube and the
         * clock as two parts of its share of the wrap, computed in double precision.
         * Each kind's lobe scale is blended between the halo maps and divided by the kind's cross section for the
         * light's elevation.
         */
        private void setFrameUniforms(SkyState state, SkyFrame frame, Vec3 camera, float extinctionShare,
                float dustBottomHeight, double gain, int windowWidth, int windowHeight) {
            viewProjection.get(matrixBuffer);
            GL20.glUniformMatrix4fv(viewProjectionUniform, false, matrixBuffer);
            GL20.glUniform3f(eyeOffsetUniform, (float) DustGlitterField.cellOffset(camera.x),
                    (float) DustGlitterField.cellOffset(camera.y), (float) DustGlitterField.cellOffset(camera.z));
            double clockShare = DustGlitterField.clockShare(clockSeconds);
            GL20.glUniform1f(clockHighUniform, DustGlitterField.clockHigh(clockShare));
            GL20.glUniform1f(clockLowUniform, DustGlitterField.clockLow(clockShare));
            double litSeconds = SkySetting.DUST_GLITTER_LIT_SECONDS.get();
            GL20.glUniform1f(lifeRateUniform, DustGlitterField.lifeRate(litSeconds));
            GL20.glUniform1f(timeScaleUniform, DustGlitterField.timeScale(litSeconds));
            GL20.glUniform1f(sizeSpreadUniform, (float) SkySetting.DUST_GLITTER_SIZE_SPREAD.get());
            GL20.glUniform1f(fallSpeedUniform, (float) SkySetting.DUST_GLITTER_FALL_SPEED.get());
            GL20.glUniform1f(extinctionShareUniform, extinctionShare);
            GL20.glUniform1f(dustBottomHeightUniform, dustBottomHeight);
            GL20.glUniform1f(plainShareUniform, (float) SkySetting.DUST_GLITTER_PLAIN_SHARE.get());
            GL20.glUniform1f(ringPhaseUniform, (float) DustGlitterField.ringPhase(state.haloProfile()));
            double coreSigma = SkySetting.DUST_GLITTER_SIZE.get() / DustGlitterField.HALF_MAXIMUM_WIDTH_PER_SIGMA
                    * windowHeight / REFERENCE_WINDOW_HEIGHT;
            GL20.glUniform1f(halfWidthUniform, (float) (DustGlitterField.SPRITE_RADIUS_SIGMAS * coreSigma));
            GL20.glUniform2f(pixelToClipUniform, 2.0F / windowWidth, 2.0F / windowHeight);
            GL20.glUniform3f(lightUniform, (float) (beam[0] * gain), (float) (beam[1] * gain),
                    (float) (beam[2] * gain));
            GL20.glUniform1f(exposureUniform, (float) frame.exposure());
            SkyLight light = frame.light();
            GL20.glUniform3f(lightDirectionUniform, light.x(), light.y(), 0.0F);
            GL20.glUniform1f(lightSideUniform, light.side());
            GL20.glUniform1f(lightElevationUniform, (float) light.elevationRadians());
            float haloBlend = CalculatedSkyRenderer.haloBlend(state, frame);
            GL20.glUniform1f(haloBlendUniform, haloBlend);
            lobeScaleBuffer.clear();
            for (int kind = 0; kind < KINDS; kind++) {
                double crossSection = kind < kinds ? IceMirror.crossSection(light.elevationRadians(),
                        tiltSigmas[kind], halfHeightsOverRadius[kind]) : 0.0;
                for (int channel = 0; channel < CHANNELS; channel++) {
                    double scale = crossSection > 0.0 ? DustGlitterField.blendedLobeScale(state.lowHalo(),
                            state.highHalo(), haloBlend, kind, channel) / crossSection : 0.0;
                    lobeScaleBuffer.put((float) scale);
                }
            }
            lobeScaleBuffer.flip();
            GL20.glUniform3fv(lobeScalesUniform, lobeScaleBuffer);
            double blurSigma = state.lowHalo().blurSigma();
            GL20.glUniform1f(blurVarianceUniform, (float) (blurSigma * blurSigma));
        }
    }

    public static void clearFailure() {
        failed = false;
    }

    /**
     * A line about the points drawn in the last frame, the time their draw takes, and what it allocated, for the
     * client command. The points within reach are those inside FADE_END blocks that the dust draws at its bottom.
     */
    public static String describe() {
        if (failed) {
            return "Diamond dust glints stopped after an error, see the log";
        }
        String allocated = LightPillarSources.countsAllocations()
                ? String.format(Locale.ROOT, "%d bytes allocated over %d frames in the last second", lastSecondBytes,
                lastSecondFrames)
                : "allocations not counted by this JVM";
        return String.format(Locale.ROOT, "Diamond dust glints: %d points drawn in the last frame, of which the dust "
                        + "draws %.2f, about %.0f within reach; %s; %s", drawnPoints, drawnExtinctionShare,
                drawnPoints * drawnExtinctionShare * Math.PI / 6.0, timer.describe(), allocated);
    }
}
