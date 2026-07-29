package com.teammoeg.frostedheart.mixin.minecraft.misc;

import com.teammoeg.frostedheart.util.ColumnCachingBiomeResolver;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessBiomeMixin {

    @Unique
    private Climate.Sampler frostedheart$capturedSampler;

    @Inject(method = "fillBiomesFromNoise", at = @At("HEAD"))
    private void frostedheart$captureSampler(BiomeResolver resolver, Climate.Sampler sampler, CallbackInfo ci) {
        this.frostedheart$capturedSampler = sampler;
    }

    @ModifyArg(
            method = "fillBiomesFromNoise",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;fillBiomesFromNoise(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/world/level/biome/Climate$Sampler;III)V"
            ),
            index = 0,
            remap = false
    )
    private BiomeResolver frostedheart$wrapResolver(BiomeResolver original) {
        if (original instanceof MultiNoiseBiomeSource multi) {
            ChunkAccess self = (ChunkAccess) (Object) this;
            // 注意：此处不再置空 capturedSampler
            return new ColumnCachingBiomeResolver(multi, this.frostedheart$capturedSampler, self);
        }
        return original;
    }

    @Inject(method = "fillBiomesFromNoise", at = @At("TAIL"))
    private void frostedheart$clearSampler(BiomeResolver resolver, Climate.Sampler sampler, CallbackInfo ci) {
        this.frostedheart$capturedSampler = null;
    }
}