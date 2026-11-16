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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.bootstrap.client.FHShaders;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.concurrent.CompletionException;

/**
 * Draws the calculated sky and its sun over the vanilla sky dome, before vanilla draws the moon and stars, which
 * LevelRendererMixin dims by the factors of NightSkyDimming set here, and gives the fog color and the daylight of the
 * same sky. SkyBuilder builds what it is drawn from and SkyTextures holds it on
 * the GPU. The two of each around the light's elevation and the ice depth of IceWeatherDepth are blended by their exact
 * values. The light is the sun by day and the moon at night, as MoonLight chooses it.
 */
@OnlyIn(Dist.CLIENT)
public final class CalculatedSkyRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHANNELS = SkyAtmosphere.CHANNEL_COUNT;
    private static final int VIEW_SLOTS = SkyViewSet.VIEW_COUNT;
    private static final int HALO_SLOTS = SkyState.HALO_COUNT;
    /**
     * GlStateManager caches the bindings of 12 texture units, so the sky must not use more.
     */
    private static final int TEXTURE_UNITS = 12;
    private static final int SKY_VIEW_UNIT = 0;
    private static final int ICE_VIEW_UNIT = SKY_VIEW_UNIT + VIEW_SLOTS;
    private static final int HALO_MAP_UNIT = ICE_VIEW_UNIT + VIEW_SLOTS;
    private static final int HALO_PROFILE_UNIT = HALO_MAP_UNIT + HALO_SLOTS;
    private static final int ROW_TABLE_UNIT = HALO_PROFILE_UNIT + 1;
    private static final boolean OCULUS_LOADED = ModList.get().isLoaded("oculus");
    private static final SkyBuilder builder = new SkyBuilder();
    private static final SkyTextures textures = new SkyTextures();
    private static final SkyGlPass pass = new SkyGlPass(TEXTURE_UNITS);
    private static final SkyDrawTimer timer = new SkyDrawTimer();
    private static final Matrix4f inverseViewProjection = new Matrix4f();
    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private static final float[] reusedFogColor = new float[CHANNELS];
    private static final double[] reusedLightScale = new double[CHANNELS];
    private static final int[] drawnTextures = new int[TEXTURE_UNITS];
    private static final float[] moonColor = new float[CHANNELS];

    @Nullable
    private static ShaderProgram program;
    @Nullable
    private static Shader programShader;
    /**
     * The program whose samplers and constant uniforms are set and whose uniform locations are below.
     */
    @Nullable
    private static ShaderProgram uniformsProgram;
    private static int viewBlendUniform;
    private static int depthBlendUniform;
    private static int inverseViewProjectionUniform;
    private static int sunDirectionUniform;
    private static int sunSideUniform;
    private static int lightScaleUniform;
    private static int sunScaleUniform;
    private static int exposureUniform;
    private static int texelFaceSizeUniform;
    private static int haloBlendUniform;
    private static int fogColorUniform;
    private static int rainLevelUniform;
    private static int thunderLevelUniform;
    private static int nightFloorUniform;
    private static float starScale = 1.0F;
    private static boolean failed;
    private static boolean sunDrawn;
    private static long drawnFrameNumber = -1;

    private CalculatedSkyRenderer() {
    }

    public static boolean isDrawing(ClientLevel level) {
        return isAllowed(level) && builder.state() != null;
    }

    /**
     * Whether the calculated sky may be drawn in the level, which needs it enabled, the overworld, no shader pack, and
     * no earlier error.
     */
    public static boolean isAllowed(ClientLevel level) {
        return FHConfig.CLIENT.calculatedSky.get() && !failed && level.dimension() == Level.OVERWORLD
                && !shaderPackInUse();
    }

    /**
     * Whether the sky is drawn from tables, halo maps, and views built for the current config, ice depth, light, and
     * light elevation, with no build pending.
     */
    public static boolean isSettled(ClientLevel level, float partialTick) {
        SkyState current = builder.state();
        if (current == null || builder.isBuilding() || !isAllowed(level)) {
            return false;
        }
        SkyFrame frame = SkyFrame.current(level, partialTick);
        return current.settings().matchesConfig() && current.isFor(frame.light().kind(), frame.haloIndex(),
                frame.depthIndex(), frame.viewIndex());
    }

    private static boolean shaderPackInUse() {
        return OCULUS_LOADED && OculusShaderPack.isInUse();
    }

    /**
     * Whether the calculated sky was drawn in the current call of LevelRenderer.renderSky, so vanilla's sun is hidden.
     * By day it draws its own sun, and at night vanilla's sun is below the horizon.
     */
    public static boolean isSunDrawn() {
        return sunDrawn;
    }

    /**
     * Whether the calculated sky was drawn in the current frame. It is not when vanilla skips its sky, for example in
     * a fluid, in fog, or with blindness.
     */
    public static boolean isDrawnThisFrame() {
        return drawnFrameNumber == SkyFrame.frameNumber();
    }

    public static void render(ClientLevel level, PoseStack poseStack, Matrix4f projection, float partialTick,
            Camera camera) {
        sunDrawn = false;
        if (!isAllowed(level)) {
            return;
        }
        try {
            SkyFrame frame = SkyFrame.current(level, partialTick);
            updateTables(frame);
            SkyState current = builder.state();
            if (failed || current == null || camera.getFluidInCamera() != FogType.NONE) {
                return;
            }
            timer.begin();
            try {
                draw(current, frame, poseStack, projection);
            } finally {
                timer.end();
            }
            updateNightSky(current, frame);
            sunDrawn = true;
            drawnFrameNumber = SkyFrame.frameNumber();
        } catch (RuntimeException exception) {
            fail("drawing", exception);
        }
    }

    /**
     * The factor on vanilla's star brightness in the current frame, as NightSkyDimming.starScale gives it. Only valid
     * while isDrawnThisFrame is true.
     */
    public static float starScale() {
        return starScale;
    }

    /**
     * The factor on a channel of vanilla's moon sprite in the current frame, as NightSkyDimming.moonColor gives it.
     * Only valid while isDrawnThisFrame is true.
     */
    public static float moonColor(int channel) {
        return moonColor[channel];
    }

    /**
     * Sets the star scale and the moon color for the frame from the drawn state. The moon stands opposite the sun.
     */
    private static void updateNightSky(SkyState current, SkyFrame frame) {
        double depthWeight = depthBlend(current, frame);
        double zenithDisplay = current.views().zenithDisplayLuminance(viewBlend(current, frame), depthWeight,
                frame.exposure(), lightScale(current, frame), current.haloProfile());
        starScale = (float) NightSkyDimming.starScale(frame.iceDepth(), zenithDisplay,
                SkySetting.STAR_TWILIGHT_DISPLAY.get());
        SkyLight light = frame.light();
        double moonElevation = light.kind() == SkyLight.Kind.MOON ? light.elevationRadians()
                : -light.elevationRadians();
        NightSkyDimming.moonColor(current.lowDepthTables(), current.highDepthTables(), depthWeight, moonElevation,
                frame.exposure(), moonColor);
    }

    /**
     * The fog colour while the calculated sky is drawn: the sky at the horizon in the direction the camera looks.
     * Returns null when the calculated sky is not drawn. The returned array is reused by the next call.
     */
    @Nullable
    public static float[] fogColor(ClientLevel level, Camera camera, float partialTick) {
        SkyState current = builder.state();
        if (current == null || !isAllowed(level)) {
            return null;
        }
        SkyFrame frame = SkyFrame.current(level, partialTick);
        Vector3f look = camera.getLookVector();
        double horizontalFraction = Math.min(1.0, Math.sqrt(look.x() * look.x() + look.z() * look.z()));
        double azimuth = Math.atan2(look.z(), look.x() * frame.light().side());
        current.views().horizonDisplayColor(viewBlend(current, frame), depthBlend(current, frame), azimuth,
                horizontalFraction, frame.exposure(), lightScale(current, frame), frame.nightFloor(),
                frame.rainLevel(), frame.thunderLevel(), reusedFogColor);
        return reusedFogColor;
    }

    /**
     * Writes the sky light on the terrain while the calculated sky is drawn into the result, as
     * SkyViewSet.daylightColor gives it for vanilla's sky darkness. Returns false and leaves the result unchanged when
     * the sky is not drawn.
     */
    public static boolean daylightColor(ClientLevel level, float partialTick, float skyDarken, float[] result) {
        SkyState current = builder.state();
        if (current == null || !isAllowed(level)) {
            return false;
        }
        SkyFrame frame = SkyFrame.current(level, partialTick);
        current.views().daylightColor(viewBlend(current, frame), depthBlend(current, frame), frame.exposure(),
                daylightScale(current, frame), frame.rainLevel(), skyDarken, result);
        return true;
    }

    /**
     * The state the sky draws from, or null before the first build. A build that finishes in render replaces it.
     */
    @Nullable
    static SkyState drawnState() {
        return builder.state();
    }

    /**
     * The texture of the halo profile of the drawn state, with the tumbling crystals in its first row.
     */
    static int haloProfileTexture() {
        return textures.haloProfileTexture();
    }

    /**
     * The texture of the low (0) or the high (1) halo map of the drawn state.
     */
    static int haloMapTexture(int slot) {
        return textures.haloTexture(slot);
    }

    /**
     * The factor on each channel of the state's radiance for the frame's light. The returned array is reused by the
     * next call.
     */
    static double[] lightScale(SkyState current, SkyFrame frame) {
        SkyLight light = frame.light();
        for (int channel = 0; channel < CHANNELS; channel++) {
            reusedLightScale[channel] = light.radianceScale(channel, current.lightKind());
        }
        return reusedLightScale;
    }

    /**
     * The factor on each channel of the state's ground radiance for the terrain light, as SkyLight.terrainScale gives
     * it. The returned array is reused by the next call.
     */
    private static double[] daylightScale(SkyState current, SkyFrame frame) {
        SkyLight light = frame.light();
        for (int channel = 0; channel < CHANNELS; channel++) {
            reusedLightScale[channel] = light.terrainScale(channel, current.lightKind());
        }
        return reusedLightScale;
    }

    /**
     * How far the light is from the low elevation toward the high one, clamped to the range from 0 to 1.
     */
    private static float blendWeight(double lightElevation, double lowElevation, double highElevation) {
        return (float) Math.max(0.0, Math.min(1.0, (lightElevation - lowElevation) / (highElevation - lowElevation)));
    }

    static float viewBlend(SkyState current, SkyFrame frame) {
        return blendWeight(frame.light().elevationRadians(), current.views().lowSunElevationRadians(),
                current.views().highSunElevationRadians());
    }

    static float haloBlend(SkyState current, SkyFrame frame) {
        return blendWeight(frame.light().elevationRadians(), current.lowHalo().sunElevationRadians(),
                current.highHalo().sunElevationRadians());
    }

    static double depthBlend(SkyState current, SkyFrame frame) {
        return IceDepthGrid.blendWeight(frame.iceDepth(), current.depthIndex());
    }

    /**
     * Uploads and installs a build that finished, then asks for a build when the config or the frame's grid indices
     * no longer match the state and no build runs. The config is copied only when it changed.
     */
    private static void updateTables(SkyFrame frame) {
        SkyState finished;
        try {
            finished = builder.takeFinished();
        } catch (CompletionException exception) {
            fail("building tables", exception.getCause());
            return;
        }
        if (finished != null) {
            textures.upload(finished);
            builder.install(finished);
        }
        if (builder.isBuilding()) {
            return;
        }
        SkyState current = builder.state();
        SkySettings settings = current != null && current.settings().matchesConfig() ? current.settings()
                : SkySettings.fromConfig();
        builder.request(settings, frame.light().kind(), frame.haloIndex(), frame.depthIndex(), frame.viewIndex());
    }

    private static void draw(SkyState current, SkyFrame frame, PoseStack poseStack, Matrix4f projection) {
        ShaderProgram shaderProgram = program();
        inverseViewProjection.set(projection).mul(poseStack.last().pose()).invert();
        float[] fogColor = RenderSystem.getShaderFogColor();
        for (int slot = 0; slot < VIEW_SLOTS; slot++) {
            drawnTextures[SKY_VIEW_UNIT + slot] = textures.viewSkyTexture(slot);
            drawnTextures[ICE_VIEW_UNIT + slot] = textures.viewIceTexture(slot);
        }
        for (int slot = 0; slot < HALO_SLOTS; slot++) {
            drawnTextures[HALO_MAP_UNIT + slot] = textures.haloTexture(slot);
        }
        drawnTextures[HALO_PROFILE_UNIT] = textures.haloProfileTexture();
        drawnTextures[ROW_TABLE_UNIT] = textures.rowTableTexture();

        pass.begin();
        try {
            for (int unit = 0; unit < TEXTURE_UNITS; unit++) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit);
                RenderSystem.bindTexture(drawnTextures[unit]);
            }
            shaderProgram.use();
            if (uniformsProgram != shaderProgram) {
                setFixedUniforms(shaderProgram.programId);
                uniformsProgram = shaderProgram;
            }
            GL20.glUniform1f(viewBlendUniform, viewBlend(current, frame));
            GL20.glUniform1f(depthBlendUniform, (float) depthBlend(current, frame));
            inverseViewProjection.get(matrixBuffer);
            GL20.glUniformMatrix4fv(inverseViewProjectionUniform, false, matrixBuffer);
            SkyLight light = frame.light();
            GL20.glUniform3f(sunDirectionUniform, light.x(), light.y(), 0.0F);
            GL20.glUniform1f(sunSideUniform, light.side());
            double[] lightScale = lightScale(current, frame);
            GL20.glUniform3f(lightScaleUniform, (float) lightScale[0], (float) lightScale[1], (float) lightScale[2]);
            GL20.glUniform1f(sunScaleUniform, light.kind() == SkyLight.Kind.SUN ? 1.0F : 0.0F);
            GL20.glUniform1f(exposureUniform, (float) frame.exposure());
            GL20.glUniform1f(texelFaceSizeUniform, current.settings().texelFaceSize());
            GL20.glUniform1f(haloBlendUniform, haloBlend(current, frame));
            GL20.glUniform3f(fogColorUniform, fogColor[0], fogColor[1], fogColor[2]);
            GL20.glUniform1f(rainLevelUniform, frame.rainLevel());
            GL20.glUniform1f(thunderLevelUniform, frame.thunderLevel());
            double[] nightFloor = frame.nightFloor();
            GL20.glUniform3f(nightFloorUniform, (float) nightFloor[0], (float) nightFloor[1], (float) nightFloor[2]);
            SkyGlPass.drawFullScreenQuad();
        } finally {
            pass.end();
        }
    }

    /**
     * Sets the samplers and the uniforms that never change on the program in use, and looks up the locations of the
     * uniforms that draw sets every frame.
     */
    private static void setFixedUniforms(int programId) {
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "lowDepthLowSunSky"), SKY_VIEW_UNIT);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "lowDepthHighSunSky"), SKY_VIEW_UNIT + 1);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "highDepthLowSunSky"), SKY_VIEW_UNIT + 2);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "highDepthHighSunSky"), SKY_VIEW_UNIT + 3);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "lowDepthLowSunIce"), ICE_VIEW_UNIT);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "lowDepthHighSunIce"), ICE_VIEW_UNIT + 1);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "highDepthLowSunIce"), ICE_VIEW_UNIT + 2);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "highDepthHighSunIce"), ICE_VIEW_UNIT + 3);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "lowHaloMap"), HALO_MAP_UNIT);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "highHaloMap"), HALO_MAP_UNIT + 1);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "haloProfile"), HALO_PROFILE_UNIT);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "rowTables"), ROW_TABLE_UNIT);
        GL20.glUniform1f(GL20.glGetUniformLocation(programId, "sunHalfTangent"), (float) SkyTables.SUN_HALF_TANGENT);
        GL20.glUniform1f(GL20.glGetUniformLocation(programId, "horizonDip"), (float) SkyAtmosphere.HORIZON_DIP_RADIANS);
        GL20.glUniform1f(GL20.glGetUniformLocation(programId, "haloLowestElevation"),
                (float) IceHaloMap.LOWEST_ELEVATION);
        GL20.glUniform1f(GL20.glGetUniformLocation(programId, "haloElevationSpan"),
                (float) (IceHaloMap.ELEVATION_BINS * IceHaloMap.BIN_STEP));
        viewBlendUniform = GL20.glGetUniformLocation(programId, "viewBlend");
        depthBlendUniform = GL20.glGetUniformLocation(programId, "depthBlend");
        inverseViewProjectionUniform = GL20.glGetUniformLocation(programId, "inverseViewProjection");
        sunDirectionUniform = GL20.glGetUniformLocation(programId, "sunDirection");
        sunSideUniform = GL20.glGetUniformLocation(programId, "sunSide");
        lightScaleUniform = GL20.glGetUniformLocation(programId, "lightScale");
        sunScaleUniform = GL20.glGetUniformLocation(programId, "sunScale");
        exposureUniform = GL20.glGetUniformLocation(programId, "exposure");
        texelFaceSizeUniform = GL20.glGetUniformLocation(programId, "texelFaceSize");
        haloBlendUniform = GL20.glGetUniformLocation(programId, "haloBlend");
        fogColorUniform = GL20.glGetUniformLocation(programId, "fogColor");
        rainLevelUniform = GL20.glGetUniformLocation(programId, "rainLevel");
        thunderLevelUniform = GL20.glGetUniformLocation(programId, "thunderLevel");
        nightFloorUniform = GL20.glGetUniformLocation(programId, "nightFloor");
    }

    private static ShaderProgram program() {
        Shader shader = FHShaders.getCalculatedSky();
        if (program == null || programShader != shader) {
            if (program != null) {
                program.delete();
            }
            program = new ShaderProgram().attach(FHShaders.IMAGE_V).attach(shader);
            programShader = shader;
        }
        return program;
    }

    private static void fail(String stage, Throwable cause) {
        failed = true;
        LOGGER.error("The calculated sky stopped after an error while {}. The vanilla sky is drawn until it is enabled again.",
                stage, cause);
    }

    public static void clearFailure() {
        failed = false;
    }

    public static String describeState(ClientLevel level) {
        if (!FHConfig.CLIENT.calculatedSky.get()) {
            return "off";
        }
        if (failed) {
            return "stopped after an error, see the log";
        }
        if (shaderPackInUse()) {
            return "not drawn while a shader pack is in use";
        }
        if (level.dimension() != Level.OVERWORLD) {
            return "only drawn in the overworld";
        }
        if (builder.state() == null) {
            return "building tables";
        }
        return "on";
    }

    public static double lastTablesMilliseconds() {
        return builder.lastTablesMilliseconds();
    }

    public static double lastViewMilliseconds() {
        return builder.lastViewMilliseconds();
    }

    public static double lastHaloProfileMilliseconds() {
        return builder.lastHaloProfileMilliseconds();
    }

    public static double lastHaloMapMilliseconds() {
        return builder.lastHaloMapMilliseconds();
    }

    /**
     * The mean CPU and GPU times of the sky's draw, without the uploads of finished builds.
     */
    public static String describeDrawTime() {
        return timer.describe();
    }

    private static final class OculusShaderPack {
        private static boolean isInUse() {
            return IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
