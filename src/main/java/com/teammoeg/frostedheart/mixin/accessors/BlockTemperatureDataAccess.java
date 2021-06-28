package com.teammoeg.frostedheart.mixin.accessors;

import com.stereowalker.survive.util.data.BlockTemperatureData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockTemperatureData.class)
public interface BlockTemperatureDataAccess {
    @Accessor("temperatureModifier")
    void setTemperatureModifier(float newTemperatureModifier);

    @Accessor("range")
    void setRange(int newRange);

    @Accessor("usesLitOrActiveProperty")
    void setUsesLitOrActiveProperty(boolean newUsesLitOrActiveProperty);

    @Accessor("usesLevelProperty")
    void setUsesLevelProperty(boolean newUsesLevelProperty);
}
