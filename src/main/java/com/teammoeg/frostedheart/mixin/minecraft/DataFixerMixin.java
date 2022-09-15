package com.teammoeg.frostedheart.mixin.minecraft;

import com.mojang.datafixers.DataFixerBuilder;
import com.teammoeg.frostedheart.util.LazyDataFixerBuilder;
import net.minecraft.util.datafix.DataFixesManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({DataFixesManager.class})
public class DataFixerMixin {

    @Redirect(method = "createFixer", at = @At(value = "NEW", target = "com/mojang/datafixers/DataFixerBuilder"))
    private static DataFixerBuilder create$replaceBuilder(int dataVersion) {
        return new LazyDataFixerBuilder(dataVersion);
    }
}
