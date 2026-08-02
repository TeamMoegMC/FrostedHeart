package com.teammoeg.frostedheart.mixin.minecraft.accessors;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 MultiNoiseBiomeSource 私有的 parameters()（惰性解析 Either 后的参数表）。
 * 在 mixins 配置中注册本接口；ColumnCachingBiomeResolver 强转使用。
 *
 */
@Mixin(MultiNoiseBiomeSource.class)
public interface MultiNoiseBiomeSourceAccessor {

    @Invoker("parameters")
    Climate.ParameterList<Holder<Biome>> fast$parameters();
}
