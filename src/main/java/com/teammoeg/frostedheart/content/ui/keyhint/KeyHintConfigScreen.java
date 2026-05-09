package com.teammoeg.frostedheart.content.ui.keyhint;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.contentpanel.Alignment;
import com.teammoeg.chorda.client.cui.contentpanel.ContentPanel;
import com.teammoeg.chorda.client.cui.contentpanel.Line;
import com.teammoeg.chorda.client.cui.contentpanel.LineHelper;
import com.teammoeg.chorda.client.cui.contentpanel.UIEleWrapperLine;
import com.teammoeg.chorda.client.cui.editor.LabeledSelection;
import com.teammoeg.chorda.client.cui.theme.UIColors;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.popup.PopupOverlay;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.ui.archive.ArchiveTheme;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KeyHintConfigScreen extends PrimaryLayer {
    ContentPanel panel;
    TextButton save;
    TextButton close;
    List<ConfigElement> configElements = new ArrayList<>();

    public KeyHintConfigScreen() {
        super();
        setTheme(ArchiveTheme.INSTANCE);
        addUIElements();
    }

    @Override
    public void addChildUIElements() {
        panel = new ContentPanel(this) {
            @Override
            public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
                super.drawBackground(graphics, x, y, w, h+20, hint);
            }

            @Override
            public void resize() {
                int h = (int)(ClientUtils.screenHeight() * 0.8F);
                int w = (int)(h * 1.3333F); // 4:3
                setSize(w, h-20);
            }
        };
        panel.setParent(this);
        add(save = new TextButton(this, Component.translatable("gui.frostedheart.save_and_close"), CIcons.nop()) {
            @Override
            public boolean renderTitleInCenter() {
                return true;
            }

            @Override
            public void onClicked(MouseButton button) {
                if (button.is(MouseButton.LEFT)) {
                    try {
                        FHConfig.CLIENT.disabledHints.set(collect());
                        FHConfig.CLIENT.disabledHints.save();
                    } catch (Exception e) {
                        PopupOverlay.pop(e.getMessage());
                        FHMain.LOGGER.warn(e);
                    }
                    closeGui(true);
                }
            }
        });
        add(close = new TextButton(this, Component.translatable("gui.frostedheart.close"), CIcons.nop()) {
            @Override
            public boolean renderTitleInCenter() {
                return true;
            }

            @Override
            public void onClicked(MouseButton button) {
                if (button.is(MouseButton.LEFT)) {
                    closeGui(true);
                }
            }
        });
    }

    public List<String> collect() {
        return configElements.stream().filter(ele -> !ele.getValue()).map(ele -> ele.rl.toString()).collect(Collectors.toSet()).stream().toList();
    }

    @Override
    public void refresh() {
        recalcContentSize();
        List<Line<?>> lines = new ArrayList<>();
        var disabled = FHConfig.CLIENT.disabledHints.get();
        for (KeyHintOverlay.TriggerType<?> type : KeyHintOverlay.TriggerType.getAllTypes()) {
            var keys = type.getRegistered().keySet();
            if (!keys.isEmpty()) {
                // 分类
                lines.add(LineHelper.text(panel, Component.translatable("hint.frostedheart.category." + type.getLocation().getPath())).color(UIColors.UI_ALT_TEXT).alignment(Alignment.CENTER));
                // 条目
                for (ResourceLocation rl : type.getRegistered().keySet()) {
                    var ele = new ConfigElement(panel, rl, !disabled.contains(rl.toString()));
                    lines.add(ele);
                    configElements.add(ele);
                }
                lines.add(LineHelper.br(panel));
            }
        }
        panel.fillContent(lines);

        for (UIElement element : elements) {
            element.refresh();
        }
        save.setPosAndSize(0, panel.getHeight()+4, (panel.getWidth()+(panel.scrollBar.isVisible()?10:0))/2-2, 16);
        close.setPosAndSize(save.getWidth()+4, save.getY(), save.getWidth(), save.getHeight());
        alignWidgets();
    }

    static class ConfigElement extends UIEleWrapperLine {
        final ResourceLocation rl;

        public ConfigElement(UIElement parent, ResourceLocation rl, boolean enable) {
            super(parent, LabeledSelection.createBool(parent, Component.translatable("hint.frostedheart.desc." + rl.getPath()), enable));
            this.rl = rl;
        }

        @SuppressWarnings("unchecked")
        public boolean getValue() {
            return ((LabeledSelection<Boolean>)getWrappedEle()).getSelection();
        }
    }
}
