package com.teammoeg.frostedheart.content.ui.wheelmenu;

import com.simibubi.create.foundation.config.ui.BaseConfigScreen;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.contentpanel.Alignment;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.chorda.client.cui.theme.UIColors;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.ui.archive.ArchiveTheme;
import com.teammoeg.frostedheart.content.ui.keyhint.KeyHintConfigScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ConfigScreen extends PrimaryLayer {
    final ContentPanel panel;

    public ConfigScreen() {
        setTheme(ArchiveTheme.INSTANCE);
        panel = new ContentPanel(this).builder()
                .text(Component.translatable("gui.frostedheart.edit_configs_client"), t -> t.alignment(Alignment.CENTER).color(UIColors.UI_ALT_TEXT))
                .wrap(new ConfigButton(this, Component.translatable("gui.wheel_menu.editor.edit"), FlatIcon.LIST.toCIcon(), b -> WheelMenuEditors.openConfigScreen()))
                .wrap(new ConfigButton(this, Component.translatable("hint.frostedheart.edit"), FlatIcon.INFO.toCIcon(), b -> CUIScreenWrapper.open(new KeyHintConfigScreen())))
                .br()
                .text(Component.translatable("gui.frostedheart.edit_configs_through_create"), t -> t.alignment(Alignment.CENTER).color(UIColors.UI_ALT_TEXT))
                .wrap(new ConfigButton(this, Component.translatable("gui.frostedheart.edit_all_fh_configs"), CIcons.getIcon(FHItems.energy_core),b -> ClientUtils.getMc().setScreen(new BaseConfigScreen(getManager().getScreen(), FHMain.MODID))))
                .wrap(new ConfigButton(this, Component.translatable("gui.frostedheart.edit_all_chorda_configs"), CIcons.getIcon(FHItems.weatherRadar), b -> ClientUtils.getMc().setScreen(new BaseConfigScreen(getManager().getScreen(), Chorda.MODID))))
                .build();
        addUIElements();
    }

    @Override
    public void addChildUIElements() {
        panel.setParent(this);
    }

    static class ConfigButton extends TextButton {
        final Consumer<MouseButton> action;

        public ConfigButton(UIElement panel, Component t, CIcons.CIcon i, Consumer<MouseButton> action) {
            super(panel, t, i);
            this.action = action;
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
            super.render(graphics, x, y, w, h, hint);
            graphics.drawString(getFont(), ">", x+w-10, y-4+h/2,textColor.getColorARGB(this, x, y, hint), hint.theme(this).isButtonTextShadow());
        }

        @Override
        public void onClicked(MouseButton button) {
            action.accept(button);
        }
    }
}
