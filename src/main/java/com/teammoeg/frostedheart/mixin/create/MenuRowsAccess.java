package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.foundation.config.ui.OpenCreateMenuButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(OpenCreateMenuButton.MenuRows.class)
public interface MenuRowsAccess {
    @Accessor
    List<String> getLeftButtons();
    @Accessor
    List<String> getRightButtons();
}
