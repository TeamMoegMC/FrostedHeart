package com.teammoeg.chorda;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CShaders {

    @Nullable
    private static ShaderInstance roundRect;

    @Nullable
    private static ShaderInstance ring;

    @Nullable
    private static ShaderInstance round;

    public static ShaderInstance getRoundRectShader() {
        return Objects.requireNonNull(roundRect, "Attempted to call getRoundRectShader before shaders have finished loading.");
    }

    public static ShaderInstance getRingShader() {
        return Objects.requireNonNull(ring, "Attempted to call getRingShader before shaders have finished loading.");
    }

    public static ShaderInstance getRoundShader() {
        return Objects.requireNonNull(round, "Attempted to call getRoundShader before shaders have finished loading.");
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        ResourceProvider provider = event.getResourceProvider();
        event.registerShader(
                new ShaderInstance(
                        provider,
                        Chorda.rl("ring"),
                        DefaultVertexFormat.POSITION_TEX_COLOR
                ),
                (shader) -> ring = shader
        );
        event.registerShader(
                new ShaderInstance(
                        provider,
                        Chorda.rl("round_rect"),
                        DefaultVertexFormat.POSITION_TEX_COLOR
                ),
                (shader) -> roundRect = shader
        );
        event.registerShader(
                new ShaderInstance(
                        provider,
                        Chorda.rl("round"),
                        DefaultVertexFormat.POSITION_TEX
                ),
                (shader) -> round = shader
        );
    }
}
