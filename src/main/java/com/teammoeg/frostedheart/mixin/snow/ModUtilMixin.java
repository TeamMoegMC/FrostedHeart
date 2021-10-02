package com.teammoeg.frostedheart.mixin.snow;

import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import snownee.snow.ModUtil;
import snownee.snow.SnowCommonConfig;

@Mixin(ModUtil.class)
public class ModUtilMixin {
    /**
     * @author yuesha-yc
     * @reason snow melting when chunk temp < 0
     */
    @Overwrite(remap = false)
    public static boolean shouldMelt(World world, BlockPos pos) {
        if (SnowCommonConfig.snowNeverMelt)
            return false;
        if (world.getLightFor(LightType.BLOCK, pos) > 11)
            return true;
        if (ChunkData.getTemperature(world, pos) > 0)
            return true;
        Biome biome = world.getBiome(pos);
        return ModUtil.snowMeltsInWarmBiomes(biome) && !ModUtil.isColdAt(world, biome, pos) && world.canSeeSky(pos);
    }
}
