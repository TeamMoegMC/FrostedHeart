package com.teammoeg.frostedheart.content.tips.client.gui.widget;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.base.client.gui.widget.ActionStateIconButton;
import com.teammoeg.frostedheart.base.client.gui.widget.ColorEditbox;
import com.teammoeg.frostedheart.base.client.gui.widget.IconButton;
import com.teammoeg.frostedheart.base.client.gui.widget.IconCheckbox;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipRenderer;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.client.FHColorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TipEditsList extends ContainerObjectSelectionList<TipEditsList.EditEntry> {
    private final Font font;
    private String cachedId;

    public TipEditsList(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
        super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
        this.font = pMinecraft.font;
        setRenderHeader(true, 10);

        addEntry(new StringEntry("id", Component.translatable("gui.frostedheart.tip_editor.title")));
        addEntry(new StringEntry("category", Component.translatable("gui.frostedheart.tip_editor.category")));
        addEntry(new StringEntry("nextTip", Component.translatable("gui.frostedheart.tip_editor.next_tip")));
        addEntry(new StringEntry("image", Component.translatable("gui.frostedheart.tip_editor.image")));
        addEntry(new MultiComponentEntry("contents", Component.translatable("gui.frostedheart.tip_editor.contents")));
        addEntry(new ColorEntry("fontColor", Component.translatable("gui.frostedheart.tip_editor.font_color"), FHColorHelper.CYAN));
        addEntry(new ColorEntry("backgroundColor", Component.translatable("gui.frostedheart.tip_editor.background_color"),FHColorHelper.BLACK));
        addEntry(new IntegerEntry("displayTime", Component.translatable("gui.frostedheart.tip_editor.display_time")));
        addEntry(new BooleanEntry("onceOnly", Component.translatable("gui.frostedheart.tip_editor.once_only")));
        addEntry(new BooleanEntry("hide", Component.translatable("gui.frostedheart.tip_editor.hide")));
        addEntry(new BooleanEntry("pin", Component.translatable("gui.frostedheart.tip_editor.pin")));
    }

    public void updatePreview() {
        TipRenderer.TIP_QUEUE.clear();
        TipRenderer.forceClose();
        Tip tip = Tip.builder("").fromJson(getJson()).alwaysVisible(true).build();
        tip.forceDisplay();
        this.cachedId = tip.getId();
    }

    public JsonObject getJson() {
        JsonObject json = new JsonObject();
        children().forEach(e -> json.add(e.property, e.getValue()));
        return json;
    }

    public class ColorEntry extends IntegerEntry {
        public ColorEntry(String property, Component message, int defValue) {
            super(property, message);
            this.input = new ColorEditbox(font, 0, 0, 64, 12, message, true ,defValue);
            this.input.setResponder(s -> {
                try {
                    input.setTextColor(FHColorHelper.WHITE);
                    Integer.parseUnsignedInt(s, 16);
                } catch (NumberFormatException e) {
                    input.setTextColor(FHColorHelper.RED);
                }
                updatePreview();
            });
        }

        @Override
        public JsonElement getValue() {
            return new JsonPrimitive(input.getValue());
        }
    }

    public class IntegerEntry extends StringEntry {
        public IntegerEntry(String property, Component message) {
            super(property, message);
            this.input.setResponder(s -> {
                try {
                    input.setTextColor(FHColorHelper.WHITE);
                    Integer.parseUnsignedInt(s, 16);
                } catch (NumberFormatException e) {
                    input.setTextColor(FHColorHelper.RED);
                }
                updatePreview();
            });
        }

        @Override
        public JsonElement getValue() {
            int value;
            try {
                value = Integer.parseInt(input.getValue());
            } catch (NumberFormatException e) {
                value = FHColorHelper.CYAN;
            }
            return new JsonPrimitive(value);
        }
    }

    public class MultiComponentEntry extends StringEntry {
        protected final IconButton addButton;
        protected final IconButton deleteButton;
        protected final IconButton translationButton;
        protected final List<String> contents = new ArrayList<>();

        public MultiComponentEntry(String property, Component message) {
            super(property, message);
            this.addButton = new IconButton(0, 0, IconButton.Icon.CHECK, FHColorHelper.CYAN, Component.translatable("gui.frostedheart.tip_editor.add_line"), b -> {
                if (input.getValue().isBlank()) return;

                contents.add(this.input.getValue());
                input.setValue("");
                updatePreview();
            });

            this.deleteButton = new IconButton(0, 0, IconButton.Icon.TRASH_CAN, FHColorHelper.CYAN, Component.translatable("gui.frostedheart.tip_editor.delete_last_line"), b -> {
                if (!contents.isEmpty()) {
                    contents.remove(contents.size() - 1);
                    updatePreview();
                }
            });

            this.translationButton = new ActionStateIconButton(0, 0, IconButton.Icon.LIST, FHColorHelper.CYAN, Component.translatable("gui.frostedheart.tip_editor.convert_and_copy"), Component.translatable("gui.frostedheart.copied"), b -> {
                if (!contents.isEmpty()) {
                    String prefix = "tips.frostedheart." + cachedId;
                    StringBuilder copy = new StringBuilder();
                    List<String> converted = new ArrayList<>();

                    converted.add(prefix + ".title");
                    copy.append(prefix).append(".title: \"").append(contents.get(0)).append("\",\n");
                    for (int i = 1; i < contents.size(); i++) {
                        //tips.frostedheart.example.desc1
                        String s = prefix + ".desc" + i;
                        converted.add(s);
                        copy.append(s).append(": \"").append(contents.get(i)).append("\",\n");
                    }

                    contents.clear();
                    contents.addAll(converted);
                    ClientUtils.mc().keyboardHandler.setClipboard(copy.substring(0, copy.length()-2)); // 删除最后一行的逗号和换行
                    updatePreview();
                }
            });
        }

        @Override
        public void render(@NotNull GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean p_93531_, float pPartialTick) {
            super.render(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, p_93531_, pPartialTick);
            addButton.setPosition(pLeft + 146, pTop + (pHeight/2) - 10);
            addButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            deleteButton.setPosition(pLeft + 132, pTop + (pHeight/2) - 10);
            deleteButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            translationButton.setPosition(pLeft + 118, pTop + (pHeight/2) - 10);
            translationButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        @Override
        public JsonElement getValue() {
            JsonArray contents = new JsonArray();
            this.contents.forEach(contents::add);
            return contents;
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(addButton, deleteButton, translationButton, input);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return ImmutableList.of(addButton, deleteButton, translationButton, input);
        }
    }

    public class StringEntry extends EditEntry {
        protected EditBox input;

        public StringEntry(String property, Component message) {
            super(property, message);
            this.input = new EditBox(font, 0, 0, 64, 12, message);
            this.input.setResponder(b -> updatePreview());
        }

        @Override
        public void render(@NotNull GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean p_93531_, float pPartialTick) {
            pGuiGraphics.drawString(font, message, pLeft, pTop, FHColorHelper.WHITE);
            input.setPosition(pLeft + 160, pTop);
            input.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        @Override
        public JsonElement getValue() {
            return new JsonPrimitive(input.getValue());
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(input);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return Collections.singletonList(input);
        }
    }

    public class BooleanEntry extends EditEntry {
        protected final IconCheckbox checkbox;

        public BooleanEntry(String property, Component message) {
            super(property, message);
            this.checkbox = new IconCheckbox(0, 0, 2, message, false) {
                @Override
                public void onPress() {
                    super.onPress();
                    updatePreview();
                }
            };
        }

        @Override
        public void render(@NotNull GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean p_93531_, float pPartialTick) {
            pGuiGraphics.drawString(font, message, pLeft, pTop, FHColorHelper.WHITE);
            checkbox.setX(pLeft + 160 + 64/2 - checkbox.getWidth()/2);
            checkbox.setY(pTop - 12/2);
            checkbox.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        @Override
        public JsonElement getValue() {
            return new JsonPrimitive(checkbox.selected());
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(checkbox);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return Collections.singletonList(checkbox);
        }
    }

    public abstract static class EditEntry extends ContainerObjectSelectionList.Entry<EditEntry> {
        public final String property;
        public final Component message;

        protected EditEntry(String property, Component message) {
            this.property = property;
            this.message = message;
        }

        public abstract JsonElement getValue();
    }
}
