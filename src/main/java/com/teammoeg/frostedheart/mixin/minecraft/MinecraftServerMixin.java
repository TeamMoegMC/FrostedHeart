package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.world.FHFeatures;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
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
    	int y=256;
    	for(int x=info.getSpawnX()-1;x<info.getSpawnX()+1;x++)
    		for(int z=info.getSpawnZ()-1;z<info.getSpawnZ()+1;z++)
    			y=Math.min(serverWorld.getHeight(Type.MOTION_BLOCKING_NO_LEAVES,x,z),y);
    	info.setSpawnY(y-1);
    	serverWorld.setSpawnLocation(new BlockPos(info.getSpawnX(),info.getSpawnY(),info.getSpawnZ()),info.getSpawnAngle());
    	FHFeatures.spacecraft_feature.generate(serverWorld, serverWorld.getChunkProvider().getChunkGenerator(), serverWorld.rand,
                new BlockPos(info.getSpawnX(), info.getSpawnY(), info.getSpawnZ()));
    }
}
