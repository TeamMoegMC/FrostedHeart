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
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Locale;

/**
 * Draws the light pillars of the lights that LightPillarSources chose, after the weather, in frames where the
 * calculated sky was drawn and there is diamond dust. Each pillar has one ribbon per plate kind on a static vertex
 * buffer, and light_pillars.glsl spreads the ribbons over the pillar on the screen and computes the light on them.
 * The fragment stage reads the depth of the main target and counts only the crystals in front of the scene. Each light
 * carries its horizon, ceiling, and top toward the eye from LightPillarHorizons, and crystals whose light those blocks
 * stop are left out. Crystals near the eye fade out by SkySetting.PILLAR_NEAR_FADE. Each pixel-sized cell of the sky
 * draws the count of crystals that glitter in it from the mean count, which the plates' size sets, and the counts
 * change over the twinkle clock. Pillars add to the frame with a screen blend. They
 * fade out with the brightness of the fog color, so by day, and with the rain level, so in snow. Render thread only.
 * <p>
 * While SkySetting.PILLAR_TEXEL_DEGREES is above 0, pillars are drawn in the square cells of LightPillarCells instead
 * of per pixel, and the cells are the sky cells that glitter. A first pass draws the ribbons into the cell atlas,
 * which computes each cell once at its center without the scene, and the ribbons on the screen then read their
 * cells and keep the share of their light from the crystals in front of the scene.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LightPillarRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SLOTS = LightPillarSelection.SLOT_COUNT;
    /**
     * The plate kinds of crystals.csv, plates and wobbling plates, each with its own ribbon.
     */
    private static final int KINDS = 2;
    /**
     * Segments along each ribbon, as SEGMENTS in light_pillars.glsl.
     */
    private static final int SEGMENTS = 16;
    /**
     * The scene depth goes to texture unit 0 while the pillars draw, and the cell atlas to unit 1.
     */
    private static final int SCENE_DEPTH_UNIT = 0;
    private static final int CELL_ATLAS_UNIT = 1;
    /**
     * Blocks the eye moves per twinkle period, since the crystals that line up change as the eye moves.
     */
    private static final double TWINKLE_DISTANCE = 0.25;
    /**
     * The twinkle clock wraps here, which keeps it precise in single precision.
     */
    private static final double TWINKLE_CLOCK_WRAP = 65536.0;
    /**
     * What the fragment stage leaves out in each debug mode, as debugMode in light_pillars.glsl.
     */
    private static final String[] DEBUG_MODES = {"off", "the integral, replaced by a constant, and with it the glitter",
            "the glitter, with the ribbon widths kept", "the scene depth, so pillars show through the scene",
            "everything but the flat ribbons"};
    public static final int LAST_DEBUG_MODE = DEBUG_MODES.length - 1;
    private static final int DEBUG_NO_SCENE_DEPTH = 3;
    private static final int DEBUG_RIBBONS_ONLY = 4;
    private static final FloatBuffer lightBuffer = BufferUtils.createFloatBuffer(4 * SLOTS);
    private static final FloatBuffer overhangBuffer = BufferUtils.createFloatBuffer(2 * SLOTS);
    private static final FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(4 * SLOTS);
    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer clearedCell = BufferUtils.createFloatBuffer(4);
    private static final IntBuffer viewportBuffer = BufferUtils.createIntBuffer(16);
    private static final Matrix4f viewProjection = new Matrix4f();
    private static final Matrix4f inverseViewProjection = new Matrix4f();
    private static final Vector4f eyeColumn = new Vector4f();
    private static final Vector3f eyeOffset = new Vector3f();
    private static final SkyDrawTimer timer = new SkyDrawTimer();
    private static final PillarProgram screenPass = new PillarProgram();
    private static final PillarProgram atlasPass = new PillarProgram();

    @Nullable
    private static VertexBuffer ribbons;
    /**
     * Whether the ribbons show where the fragment stage runs instead of the pillars, for the client command.
     */
    private static boolean fillView;
    private static int debugMode;
    private static double twinkleClock;
    private static long lastTwinkleNanos;
    private static double lastTwinkleX;
    private static double lastTwinkleY;
    private static double lastTwinkleZ;
    /**
     * A framebuffer that the pillars draw through, holding the main target's color only while they draw. Without the
     * main target's depth attached, the fragment stage can read that depth.
     */
    private static int colorFramebuffer;
    /**
     * The cell atlas, a half float texture with the value of one cell in each texel, its framebuffer, and the rows of
     * each ribbon and the side of a cell in radians that it was made for. The texture is 0 while pillars draw per pixel.
     */
    private static int atlasTexture;
    private static int atlasFramebuffer;
    private static int atlasRows;
    private static double atlasCellAngle;
    private static boolean failed;
    private static int drawnPillars;

    private LightPillarRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        drawnPillars = 0;
        if (atlasTexture != 0 && !(SkySetting.PILLAR_TEXEL_DEGREES.get() > 0.0)) {
            deleteAtlas();
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (failed || level == null || !CalculatedSkyRenderer.isDrawnThisFrame()) {
            return;
        }
        try {
            render(level, event);
        } catch (RuntimeException exception) {
            failed = true;
            LOGGER.error("Light pillars are not drawn after an error. They are drawn again once the calculated sky is "
                    + "turned on again.", exception);
        }
    }

    private static void render(ClientLevel level, RenderLevelStageEvent event) {
        float partialTick = event.getPartialTick();
        SkyFrame frame = SkyFrame.current(level, partialTick);
        double extinctionPerBlock = frame.dustExtinction() / 1000.0;
        double dustBottomY = LightPillarSources.dustBottomY();
        float opacity = daylightOpacity() * (1.0F - frame.rainLevel());
        if (!(extinctionPerBlock > 0.0) || Double.isNaN(dustBottomY) || !(opacity > 0.0F)) {
            return;
        }
        Matrix4f projection = event.getProjectionMatrix();
        viewProjection.set(projection).mul(event.getPoseStack().last().pose());
        findEyeOffset();
        viewProjection.translate(eyeOffset);
        inverseViewProjection.set(viewProjection).invert();
        Vec3 camera = event.getCamera().getPosition();
        double eyeX = camera.x + eyeOffset.x;
        double eyeY = camera.y + eyeOffset.y;
        double eyeZ = camera.z + eyeOffset.z;
        int pillars = fillPillars(eyeX, eyeY, eyeZ, partialTick, opacity);
        if (pillars == 0) {
            return;
        }
        advanceTwinkle(eyeX, eyeY, eyeZ);
        timer.begin();
        try {
            draw(projection, extinctionPerBlock, dustBottomY - eyeY);
        } finally {
            timer.end();
        }
        drawnPillars = pillars;
    }

    /**
     * Sets the eye offset to the position of the eye relative to the camera, which view bobbing moves in the
     * projection matrix. The eye is the point that the view projection maps to a clip position with only a depth.
     */
    private static void findEyeOffset() {
        inverseViewProjection.set(viewProjection).invert().getColumn(2, eyeColumn);
        float x = eyeColumn.x / eyeColumn.w;
        float y = eyeColumn.y / eyeColumn.w;
        float z = eyeColumn.z / eyeColumn.w;
        if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) {
            eyeOffset.set(x, y, z);
        } else {
            eyeOffset.zero();
        }
    }

    /**
     * Advances the twinkle clock by the time since the last drawn frame over the twinkle period and by the distance
     * the eye moved over TWINKLE_DISTANCE, by at most one period a frame. It stands still while the game is paused and
     * stays at 0 while the screenshot harness takes shots.
     */
    private static void advanceTwinkle(double eyeX, double eyeY, double eyeZ) {
        long now = System.nanoTime();
        if (CalculatedSkyScreenshots.isRunning()) {
            twinkleClock = 0.0;
        } else if (lastTwinkleNanos != 0L && !Minecraft.getInstance().isPaused()) {
            double seconds = (now - lastTwinkleNanos) * 1.0e-9;
            double moved = Math.sqrt((eyeX - lastTwinkleX) * (eyeX - lastTwinkleX)
                    + (eyeY - lastTwinkleY) * (eyeY - lastTwinkleY) + (eyeZ - lastTwinkleZ) * (eyeZ - lastTwinkleZ));
            double periods = seconds / SkySetting.PILLAR_TWINKLE_SECONDS.get() + moved / TWINKLE_DISTANCE;
            twinkleClock = (twinkleClock + Math.min(Math.max(periods, 0.0), 1.0)) % TWINKLE_CLOCK_WRAP;
        }
        lastTwinkleNanos = now;
        lastTwinkleX = eyeX;
        lastTwinkleY = eyeY;
        lastTwinkleZ = eyeZ;
    }

    /**
     * Fades pillars out as the fog color brightens, as 1 - Y to the fourth power with Y its luminance as displayed.
     */
    private static float daylightOpacity() {
        float[] fogColor = RenderSystem.getShaderFogColor();
        float luminance = 0.2126F * fogColor[0] + 0.7152F * fogColor[1] + 0.0722F * fogColor[2];
        float darkness = 1.0F - Math.max(0.0F, Math.min(1.0F, luminance));
        float squared = darkness * darkness;
        return squared * squared;
    }

    /**
     * Writes the light positions relative to the eye with their horizons, ceilings, and tops toward the eye, and the
     * colors of every slot into the uniform buffers, with the colors of slots that are faded out at 0, and returns how
     * many slots show.
     */
    private static int fillPillars(double eyeX, double eyeY, double eyeZ, float partialTick, float opacity) {
        float brightness = (float) (LightPillarOptics.BRIGHTNESS * SkySetting.PILLAR_BRIGHTNESS.get());
        if (!(brightness > 0.0F)) {
            return 0;
        }
        LightPillarSelection selection = LightPillarSources.selection();
        LightPillarHorizons horizons = LightPillarSources.horizons();
        lightBuffer.clear();
        overhangBuffer.clear();
        colorBuffer.clear();
        int pillars = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            float fade = selection.fade(slot, partialTick);
            if (fade > 0.0F) {
                lightBuffer.put((float) (selection.x(slot) - eyeX)).put((float) (selection.y(slot) - eyeY))
                        .put((float) (selection.z(slot) - eyeZ)).put(horizons.horizon(slot, eyeX, eyeZ));
                overhangBuffer.put(horizons.ceiling(slot, eyeX, eyeZ)).put(horizons.top(slot, eyeX, eyeZ));
                colorBuffer.put(selection.red(slot) * brightness).put(selection.green(slot) * brightness)
                        .put(selection.blue(slot) * brightness).put(fade * opacity);
                pillars++;
            } else {
                lightBuffer.put(0.0F).put(0.0F).put(0.0F).put(0.0F);
                overhangBuffer.put(0.0F).put(0.0F);
                colorBuffer.put(0.0F).put(0.0F).put(0.0F).put(0.0F);
            }
        }
        lightBuffer.flip();
        overhangBuffer.flip();
        colorBuffer.flip();
        return pillars;
    }

    /**
     * Draws every ribbon into the main target's color through the color framebuffer, with the main target's depth on
     * the scene depth unit for the fragment stage. In Fabulous graphics this stage runs with the weather target bound,
     * and either way the framebuffer bound before is bound again after. No depth test runs, since the fragment stage
     * hides the pillars behind the scene itself. With cells, the ribbons first fill the cell atlas, which goes to the
     * cell atlas unit, and margins, blur, and glitter follow the side of a cell instead of a pixel where it is larger.
     */
    private static void draw(Matrix4f projection, double extinctionPerBlock, double dustBottomHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        VertexBuffer ribbonBuffer = ribbons();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        double pixelAngle = 2.0 / (Math.abs(projection.m11()) * Math.max(1, mainTarget.height));
        double cellAngle = Math.toRadians(SkySetting.PILLAR_TEXEL_DEGREES.get());
        boolean cells = cellAngle > 0.0;
        float sampleAngle = (float) Math.max(cellAngle, pixelAngle);
        float crystalCountScale = (float) LightPillarOptics.crystalCountScale(cells ? cellAngle : pixelAngle,
                SkySetting.PILLAR_CRYSTAL_SIZE.get());

        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean blending = GL11.glIsEnabled(GL11.GL_BLEND);
        int sourceColor = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int destinationColor = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int colorEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        int alphaEquation = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean culling = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        viewportBuffer.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuffer);
        int viewportX = viewportBuffer.get(0);
        int viewportY = viewportBuffer.get(1);
        int viewportWidth = viewportBuffer.get(2);
        int viewportHeight = viewportBuffer.get(3);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + CELL_ATLAS_UNIT);
        int previousAtlasTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0 + SCENE_DEPTH_UNIT);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            ribbonBuffer.bind();
            if (cells) {
                prepareAtlas(cellAngle);
                if (debugMode != DEBUG_RIBBONS_ONLY) {
                    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, atlasFramebuffer);
                    RenderSystem.viewport(0, 0, LightPillarCells.ATLAS_COLUMNS,
                            LightPillarCells.atlasHeight(atlasRows));
                    RenderSystem.disableScissor();
                    GL30.glClearBufferfv(GL11.GL_COLOR, 0, clearedCell);
                    RenderSystem.disableBlend();
                    atlasPass.use(FHShaders.getLightPillarCellsVertex(), FHShaders.getLightPillarCellsFragment());
                    atlasPass.setFrameUniforms(extinctionPerBlock, dustBottomHeight, sampleAngle, crystalCountScale,
                            (float) cellAngle, mainTarget);
                    ribbonBuffer.draw();
                    if (scissor) {
                        GlStateManager._enableScissorTest();
                    }
                }
            }
            bindColorFramebuffer(mainTarget);
            RenderSystem.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + CELL_ATLAS_UNIT);
            RenderSystem.bindTexture(atlasTexture);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + SCENE_DEPTH_UNIT);
            boolean readsSceneDepth = debugMode != DEBUG_NO_SCENE_DEPTH && debugMode != DEBUG_RIBBONS_ONLY;
            RenderSystem.bindTexture(readsSceneDepth ? mainTarget.getDepthTextureId() : 0);
            screenPass.use(FHShaders.getLightPillarsVertex(), FHShaders.getLightPillarsFragment());
            screenPass.setFrameUniforms(extinctionPerBlock, dustBottomHeight, sampleAngle, crystalCountScale,
                    cells ? (float) cellAngle : 0.0F, mainTarget);
            RenderSystem.enableBlend();
            RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
            RenderSystem.blendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
            ribbonBuffer.draw();
        } finally {
            VertexBuffer.unbind();
            GL20.glUseProgram(previousProgram);
            GL20.glBlendEquationSeparate(colorEquation, alphaEquation);
            RenderSystem.blendFuncSeparate(sourceColor, destinationColor, sourceAlpha, destinationAlpha);
            if (blending) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            if (depthTest) {
                RenderSystem.enableDepthTest();
            }
            if (culling) {
                RenderSystem.enableCull();
            }
            if (scissor) {
                GlStateManager._enableScissorTest();
            }
            RenderSystem.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + CELL_ATLAS_UNIT);
            RenderSystem.bindTexture(previousAtlasTexture);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + SCENE_DEPTH_UNIT);
            RenderSystem.bindTexture(previousTexture);
            RenderSystem.activeTexture(activeTexture);
            releaseColorFramebuffer(previousFramebuffer);
        }
    }

    /**
     * Makes the cell atlas and its framebuffer for the side of a cell in radians, and again when the side changed, and
     * leaves the framebuffer bound for drawing.
     */
    private static void prepareAtlas(double cellAngle) {
        if (atlasTexture != 0 && atlasCellAngle == cellAngle) {
            return;
        }
        int rows = LightPillarCells.atlasRows(cellAngle, widestPlateTiltSigma());
        atlasCellAngle = 0.0;
        if (atlasTexture == 0) {
            atlasTexture = GL11.glGenTextures();
        }
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlasTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R16F, LightPillarCells.ATLAS_COLUMNS,
                    LightPillarCells.atlasHeight(rows), 0, GL11.GL_RED, GL11.GL_FLOAT, 0L);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
        if (atlasFramebuffer == 0) {
            atlasFramebuffer = GL30.glGenFramebuffers();
        }
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, atlasFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                atlasTexture, 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("The light pillar cell atlas framebuffer is incomplete with status "
                    + status);
        }
        atlasRows = rows;
        atlasCellAngle = cellAngle;
    }

    /**
     * Deletes the cell atlas and its framebuffer once pillars draw per pixel, before anything binds them in a frame.
     */
    private static void deleteAtlas() {
        if (atlasFramebuffer != 0) {
            GL30.glDeleteFramebuffers(atlasFramebuffer);
            atlasFramebuffer = 0;
        }
        if (atlasTexture != 0) {
            GL11.glDeleteTextures(atlasTexture);
            atlasTexture = 0;
        }
        atlasRows = 0;
        atlasCellAngle = 0.0;
    }

    /**
     * The widest tilt spread in radians among the plate kinds that setFixedUniforms passes on.
     */
    private static double widestPlateTiltSigma() {
        double widest = 0.0;
        int kind = 0;
        for (IceCrystalData.Crystal crystal : IceCrystalData.get().crystals()) {
            if (crystal.orientation() == IceCrystalData.Orientation.PLATE && kind < KINDS) {
                widest = Math.max(widest, Math.toRadians(crystal.tiltDegrees()));
                kind++;
            }
        }
        return widest;
    }

    /**
     * Binds the color framebuffer for drawing with the main target's color attached, making it first if needed.
     */
    private static void bindColorFramebuffer(RenderTarget mainTarget) {
        if (colorFramebuffer == 0) {
            colorFramebuffer = GL30.glGenFramebuffers();
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, colorFramebuffer);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        } else {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, colorFramebuffer);
        }
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                mainTarget.getColorTextureId(), 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("The light pillar framebuffer is incomplete with status " + status);
        }
    }

    /**
     * Takes the main target's color off the color framebuffer, so it holds no texture that a resize deletes, and binds
     * the framebuffer for drawing again.
     */
    private static void releaseColorFramebuffer(int framebuffer) {
        if (colorFramebuffer != 0) {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, colorFramebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, 0, 0);
        }
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
    }

    /**
     * A program of two stages of light_pillars.glsl and the locations of the uniforms that draw sets every frame. It is
     * made again when a resource reload replaced its stages.
     */
    private static final class PillarProgram {
        @Nullable
        private ShaderProgram program;
        @Nullable
        private Shader vertex;
        @Nullable
        private Shader fragment;
        private boolean fixedUniformsSet;
        private int viewProjectionUniform;
        private int lightsUniform;
        private int overhangsUniform;
        private int colorsUniform;
        private int nearFadeUniform;
        private int extinctionUniform;
        private int dustBottomHeightUniform;
        private int lateralBlurUniform;
        private int marginAngleUniform;
        private int inverseViewProjectionUniform;
        private int screenSizeUniform;
        private int crystalCountScaleUniform;
        private int twinkleClockUniform;
        private int fillViewUniform;
        private int debugModeUniform;
        private int cellAngleUniform;
        private int atlasRowsUniform;

        /**
         * Uses the program of the two stages, making it first if needed. The ribbon buffer feeds its Position attribute
         * at location 0.
         */
        private void use(Shader newVertex, Shader newFragment) {
            if (program == null || vertex != newVertex || fragment != newFragment) {
                if (program != null) {
                    program.delete();
                    program = null;
                }
                ShaderProgram created = new ShaderProgram();
                try {
                    created.attach(newVertex).attach(newFragment);
                    GL20.glBindAttribLocation(created.programId, 0, "Position");
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
         * Sets the uniforms that never change on the program in use, with the plate kinds of crystals.csv, and looks
         * up the locations of the uniforms that draw sets every frame.
         */
        private void setFixedUniforms(int programId) {
            float[] tiltSigmas = new float[KINDS];
            float[] shares = new float[KINDS];
            IceCrystalData crystals = IceCrystalData.get();
            int kind = 0;
            for (IceCrystalData.Crystal crystal : crystals.crystals()) {
                if (crystal.orientation() == IceCrystalData.Orientation.PLATE && kind < KINDS) {
                    tiltSigmas[kind] = (float) Math.toRadians(crystal.tiltDegrees());
                    shares[kind] = (float) crystals.shareOf(crystal);
                    kind++;
                }
            }
            GL20.glUniform2f(GL20.glGetUniformLocation(programId, "tiltSigmas"), tiltSigmas[0], tiltSigmas[1]);
            GL20.glUniform2f(GL20.glGetUniformLocation(programId, "shares"), shares[0], shares[1]);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "refractiveIndex"),
                    (float) IceHaloFormulas.refractiveIndex(1));
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "scaleHeight"),
                    (float) LightPillarSelection.DUST_SCALE_HEIGHT);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "visibleValue"),
                    (float) LightPillarOptics.VISIBLE_VALUE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "glintValue"),
                    (float) LightPillarOptics.GLINT_VALUE);
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "highestElevation"),
                    (float) LightPillarOptics.HIGHEST_RIBBON_ELEVATION);
            GL20.glUniform1i(GL20.glGetUniformLocation(programId, "sceneDepth"), SCENE_DEPTH_UNIT);
            GL20.glUniform1i(GL20.glGetUniformLocation(programId, "cellAtlas"), CELL_ATLAS_UNIT);
            viewProjectionUniform = GL20.glGetUniformLocation(programId, "viewProjection");
            lightsUniform = GL20.glGetUniformLocation(programId, "lights");
            overhangsUniform = GL20.glGetUniformLocation(programId, "overhangs");
            colorsUniform = GL20.glGetUniformLocation(programId, "colors");
            nearFadeUniform = GL20.glGetUniformLocation(programId, "nearFade");
            extinctionUniform = GL20.glGetUniformLocation(programId, "extinction");
            dustBottomHeightUniform = GL20.glGetUniformLocation(programId, "dustBottomHeight");
            lateralBlurUniform = GL20.glGetUniformLocation(programId, "lateralBlur");
            marginAngleUniform = GL20.glGetUniformLocation(programId, "marginAngle");
            inverseViewProjectionUniform = GL20.glGetUniformLocation(programId, "inverseViewProjection");
            screenSizeUniform = GL20.glGetUniformLocation(programId, "screenSize");
            crystalCountScaleUniform = GL20.glGetUniformLocation(programId, "crystalCountScale");
            twinkleClockUniform = GL20.glGetUniformLocation(programId, "twinkleClock");
            fillViewUniform = GL20.glGetUniformLocation(programId, "fillView");
            debugModeUniform = GL20.glGetUniformLocation(programId, "debugMode");
            cellAngleUniform = GL20.glGetUniformLocation(programId, "cellAngle");
            atlasRowsUniform = GL20.glGetUniformLocation(programId, "atlasRows");
        }

        /**
         * Sets the uniforms of this frame on the program in use. The sample angle sets the margins and the blur, the
         * crystal count scale is per pixel or per cell, and the cell angle is 0 while pillars draw per pixel.
         */
        private void setFrameUniforms(double extinctionPerBlock, double dustBottomHeight, float sampleAngle,
                float crystalCountScale, float cellAngle, RenderTarget mainTarget) {
            viewProjection.get(matrixBuffer);
            GL20.glUniformMatrix4fv(viewProjectionUniform, false, matrixBuffer);
            GL20.glUniform4fv(lightsUniform, lightBuffer);
            GL20.glUniform2fv(overhangsUniform, overhangBuffer);
            GL20.glUniform4fv(colorsUniform, colorBuffer);
            GL20.glUniform1f(nearFadeUniform, (float) SkySetting.PILLAR_NEAR_FADE.get());
            GL20.glUniform1f(extinctionUniform, (float) extinctionPerBlock);
            GL20.glUniform1f(dustBottomHeightUniform, (float) dustBottomHeight);
            GL20.glUniform1f(lateralBlurUniform, (float) LightPillarOptics.lateralBlur(sampleAngle));
            GL20.glUniform1f(marginAngleUniform, sampleAngle);
            inverseViewProjection.get(matrixBuffer);
            GL20.glUniformMatrix4fv(inverseViewProjectionUniform, false, matrixBuffer);
            GL20.glUniform2f(screenSizeUniform, mainTarget.width, mainTarget.height);
            GL20.glUniform1f(crystalCountScaleUniform, crystalCountScale);
            GL20.glUniform1f(twinkleClockUniform, (float) twinkleClock);
            GL20.glUniform1f(fillViewUniform, fillView ? 1.0F : 0.0F);
            GL20.glUniform1i(debugModeUniform, debugMode);
            GL20.glUniform1f(cellAngleUniform, cellAngle);
            GL20.glUniform1i(atlasRowsUniform, atlasRows);
        }
    }

    /**
     * The ribbons of every slot and plate kind as quads, each vertex holding its ribbon index, its row from the bottom,
     * and its side, -1 or 1, doubled for the two vertices at the top of each quad. The vertex shader places them.
     */
    private static VertexBuffer ribbons() {
        if (ribbons == null) {
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            for (int ribbon = 0; ribbon < SLOTS * KINDS; ribbon++) {
                for (int segment = 0; segment < SEGMENTS; segment++) {
                    builder.vertex(ribbon, segment, -1.0).endVertex();
                    builder.vertex(ribbon, segment, 1.0).endVertex();
                    builder.vertex(ribbon, segment + 1, 2.0).endVertex();
                    builder.vertex(ribbon, segment + 1, -2.0).endVertex();
                }
            }
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            try {
                buffer.bind();
                buffer.upload(builder.end());
            } catch (RuntimeException exception) {
                buffer.close();
                throw exception;
            } finally {
                VertexBuffer.unbind();
            }
            ribbons = buffer;
        }
        return ribbons;
    }

    public static void clearFailure() {
        failed = false;
    }

    /**
     * Switches the fill view on or off and returns whether it is on. While it is on, every pixel that a ribbon covers
     * shows green where its pillar adds to the display and red where the fragment stage runs for nothing, or with cells
     * blue where its cell lies beyond the atlas and is computed at the pixel, and overlapping ribbons add up.
     */
    public static boolean toggleFillView() {
        fillView = !fillView;
        return fillView;
    }

    /**
     * Sets the debug mode, from 0 to LAST_DEBUG_MODE, and returns what the fragment stage leaves out in it, for
     * measuring the cost of each part. 1 shades every pixel whose scene lies beyond the light alike instead of computing
     * the integral and the glitter, 2 leaves out the glitter but keeps the ribbons as wide, 3 neither binds nor reads
     * the scene depth, and 4 shades every pixel the ribbons cover alike. With cells, 1 writes 0.1 as every cell's value
     * instead of computing it, which the pixels then show in the light's color times their scene share, 2 leaves the
     * glitter out of the cell values, and 4 also leaves out the atlas pass.
     */
    public static String setDebugMode(int mode) {
        debugMode = Math.max(0, Math.min(LAST_DEBUG_MODE, mode));
        return DEBUG_MODES[debugMode];
    }

    /**
     * A line about the slots drawn in the last frame and the time their draw takes, for the client command. A slot
     * counts when its fade is above 0, whether or not its pillar is in view.
     */
    public static String describe() {
        if (failed) {
            return "Light pillar drawing stopped after an error, see the log";
        }
        String cells = atlasTexture != 0 ? String.format(Locale.ROOT, "; cells of %.3f degrees, atlas %d x %d",
                Math.toDegrees(atlasCellAngle), LightPillarCells.ATLAS_COLUMNS, LightPillarCells.atlasHeight(atlasRows))
                : "; drawn per pixel";
        return String.format(Locale.ROOT, "Light pillar slots drawn in the last frame: %d; %s%s%s%s", drawnPillars,
                timer.describe(), cells, fillView ? "; fill view on" : "",
                debugMode > 0 ? "; debug mode " + debugMode + " leaves out " + DEBUG_MODES[debugMode] : "");
    }
}
