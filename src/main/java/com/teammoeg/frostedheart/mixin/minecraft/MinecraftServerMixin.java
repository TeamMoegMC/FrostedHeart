package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.world.FHFeatures;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject
            (at = @At("TAIL"), cancellable = true, method = "func_240786_a_")
    private static void spacecraftGenerate(ServerWorld serverWorld, IServerWorldInfo info, boolean hasBonusChest, boolean p_240786_3_, boolean p_240786_4_, CallbackInfo ci) {
        info.setSpawnY(serverWorld.getHeight(Type.WORLD_SURFACE,info.getSpawnX(),info.getSpawnZ()));
    	FHFeatures.spacecraft_feature.generate(serverWorld, serverWorld.getChunkProvider().getChunkGenerator(), serverWorld.rand,
                new BlockPos(info.getSpawnX(), info.getSpawnY(), info.getSpawnZ()));
    }
}
