package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.config.ui.OpenCreateMenuButton;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(OpenCreateMenuButton.OpenConfigButtonHandler.class)
public class MixinOpenConfigButtonHandler {
    /**
     * @author yuesha-yc
     * @reason remove from main menu
     */
    @SubscribeEvent
    @Overwrite(remap = false)
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent event) {
        Screen gui = event.getGui();
        OpenCreateMenuButton.MenuRows menu = null;
        int rowIdx = 0;
        int offsetX = 0;
        if (gui instanceof IngameMenuScreen) {
            menu = OpenCreateMenuButton.MenuRows.INGAME_MENU;
            rowIdx = (Integer)AllConfigs.CLIENT.ingameMenuConfigButtonRow.get();
            offsetX = (Integer)AllConfigs.CLIENT.ingameMenuConfigButtonOffsetX.get();
        }

        if (rowIdx != 0 && menu != null) {
            boolean onLeft = offsetX < 0;
            MenuRowsAccess menuRowsAccess = (MenuRowsAccess) menu;
            String target = (String)(onLeft ? menuRowsAccess.getLeftButtons() : menuRowsAccess.getRightButtons()).get(rowIdx - 1);
            int finalOffsetX = offsetX;
            event.getWidgetList().stream().filter((w) -> {
                return w.getMessage().getString().equals(target);
            }).findFirst().ifPresent((w) -> {
                event.addWidget(new OpenCreateMenuButton(w.x + finalOffsetX + (onLeft ? -20 : w.getWidth()), w.y));
            });
        }

    }
}
