/*
 * Copyright (c) 2022-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.research.gui.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.util.RegistryUtils;

import dev.ftb.mods.ftblibrary.config.ui.ResourceSearchMode;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.BlankPanel;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * @author LatvianModder, khjxiaogu
 */
public class SelectItemStackDialog extends EditDialog {

	private class ButtonCaps extends ButtonStackConfig {
        public ButtonCaps(Panel panel) {
            super(panel, Lang.translateKey("ftblibrary.select_item.caps"), ItemIcon.getItemIcon(Items.ANVIL));
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();

            final CompoundTag nbt = current.save(new CompoundTag());


            EditPrompt.open(this, "caps", fromNBT(nbt.get("ForgeCaps")), s -> {
                if (s == null || s.isEmpty() || s.equals("null")) {
                    nbt.remove("ForgeCaps");
                } else {
                    try {
                        nbt.put("ForgeCaps", TagParser.parseTag(s));
                    } catch (CommandSyntaxException e) {
                        FHMain.LOGGER.error("Error parsing NBT when setting ForgeCaps in SelectItemStackDialog");
                        e.printStackTrace();
                    }
                }
                current = ItemStack.of(nbt);
            });
        }
    }
    private class ButtonCount extends ButtonStackConfig {
        public ButtonCount(Panel panel) {
            super(panel, Lang.translateKey("ftblibrary.select_item.count"), ItemIcon.getItemIcon(Items.PAPER));
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            EditPrompt.open(this, "count", String.valueOf(current.getCount()), val -> current.setCount(Integer.parseInt(val)));
        }
    }

    private class ButtonEditData extends Button {
        public ButtonEditData(Panel panel) {
            super(panel, Component.empty(), Icons.BUG);
        }

        @Override
        public void drawIcon(GuiGraphics guiGraphics, Theme theme, int x, int y, int w, int h) {
            guiGraphics.renderItem(current,x, y, w, h);
            //GuiHelper.drawItem(matrixStack, current, x, y, w / 16F, h / 16F, true, null);
        }

        @Override
        public Component getTitle() {
            return current.getHoverName();
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            EditPrompt.open(this, "Data", current.save(new CompoundTag()).toString(), s -> {
                try {
                    current = ItemStack.of(TagParser.parseTag(s));
                } catch (CommandSyntaxException e) {
                    FHMain.LOGGER.error("Error parsing NBT when setting ItemStack in SelectItemStackDialog");
                    e.printStackTrace();
                }
            });
        }
    }

    private class ButtonNBT extends ButtonStackConfig {
        public ButtonNBT(Panel panel) {
            super(panel, Lang.translateKey("ftblibrary.select_item.nbt"), ItemIcon.getItemIcon(Items.NAME_TAG));
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            EditPrompt.open(this, "nbt", fromNBT(current.getTag()), s -> {
                try {
                    current.setTag(TagParser.parseTag(s));
                } catch (CommandSyntaxException e) {
                    FHMain.LOGGER.error("Error parsing NBT when setting NBT in SelectItemStackDialog");
                    e.printStackTrace();
                }
            });
        }
    }

    private abstract class ButtonStackConfig extends Button {
        public ButtonStackConfig(Panel panel, Component title, Icon icon) {
            super(panel, title, icon);
        }

        @Override
        public WidgetType getWidgetType() {
            return current.isEmpty() ? WidgetType.DISABLED : super.getWidgetType();
        }
    }

    private class ButtonSwitchMode extends Button {
        private final Iterator<ResourceSearchMode> modeIterator = Iterators.cycle(modes);

        public ButtonSwitchMode(Panel panel) {
            super(panel);
            activeMode = modeIterator.next();
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            super.addMouseOverText(list);
            list.add(activeMode.getDisplayName().withStyle(ChatFormatting.GRAY).append(Lang.str(" [" + panelStacks.getWidgets().size() + "]").withStyle(ChatFormatting.DARK_GRAY)));
        }

        @Override
        public void drawIcon(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
            activeMode.getIcon().draw(matrixStack, x, y, w, h);
        }

        @Override
        public Component getTitle() {
            return Lang.translateKey("ftblibrary.select_item.list_mode");
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            activeMode = modeIterator.next();
            panelStacks.refreshWidgets();
        }
    }

    private class ItemStackButton extends Button {
        private final ItemStack stack;

        private ItemStackButton(Panel panel, ItemStack is) {
            super(panel, Component.empty(), Icons.BARRIER);
            setSize(18, 18);
            stack = is;
            title = null;
            icon = ItemIcon.getItemIcon(is);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
        }

        @Override
        public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
            (getWidgetType() == WidgetType.MOUSE_OVER ? Color4I.LIGHT_GREEN.withAlpha(70) : Color4I.BLACK.withAlpha(50)).draw(matrixStack, x, y, w, h);
        }

        @Override
        public Component getTitle() {
            if (title == null) {
                title = stack.getHoverName();
            }

            return title;
        }

        @Override
        public WidgetType getWidgetType() {
            return stack.getItem() == current.getItem() && Objects.equals(stack.getTag(), current.getTag()) ? WidgetType.MOUSE_OVER : super.getWidgetType();
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            current = stack.copy();
        }

        public boolean shouldAdd(String search, String mod) {
            if (search.isEmpty()) {
                return true;
            }

            if (!mod.isEmpty()) {
                return RegistryUtils.getRegistryName(stack.getItem()).getNamespace().contains(mod);
            }

            return stack.getHoverName().getString().toLowerCase().contains(search);
        }
    }

    public static Editor<ItemStack> EDITOR = (p, l, v, c) -> new SelectItemStackDialog(p, l, v, c).open();

    public static Editor<Block> EDITOR_BLOCK = (p, l, v, c) -> new SelectItemStackDialog(p, l + " (Blocks only)", new ItemStack(v), e -> {
        Block b = Block.byItem(e.getItem());
        if (b != Blocks.AIR)
            c.accept(b);
    }).open();

    public static final ExecutorService ITEM_SEARCH = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "FH-ItemSearch");
        thread.setDaemon(true);
        return thread;
    });

    public final List<ResourceSearchMode> modes = new ArrayList<>();

    {
        modes.add(ResourceSearchMode.ALL_ITEMS);
        modes.add(ResourceSearchMode.INVENTORY);
        modes.add(new ResourceSearchMode() {

            @Override
            public Collection<ItemStack> getAllResources() {
                return RegistryUtils.getBlocks().stream().map(Block::asItem).filter(Objects::nonNull).map(ItemStack::new).collect(Collectors.toList());
            }

            @Override
            public MutableComponent getDisplayName() {
                return Lang.str("Blocks");
            }

            @Override
            public Icon getIcon() {
                return ItemIcon.getItemIcon(Blocks.STONE.asItem());
            }
        });
    }

    private static ResourceSearchMode activeMode = null;

    public static final Editor<Collection<ItemStack>> STACK_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, new ItemStack(Items.AIR), EDITOR, SelectItemStackDialog::fromItemStack, ItemIcon::getItemIcon, c).open();

    public static final Editor<Collection<Block>> BLOCK_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, Blocks.AIR, EDITOR_BLOCK, e -> e.getName().getString(), e -> ItemIcon.getItemIcon(e.asItem()), c).open();

    private final Consumer<ItemStack> callback;
    private ItemStack current;
    private final Button buttonCancel, buttonAccept;
    private final Panel panelStacks;
    private final PanelScrollBar scrollBar;
    private final TextBox searchBox;
    private final Panel tabs;
    public long update = Long.MAX_VALUE;

    private static String fromItemStack(ItemStack s) {
        return s.getHoverName().getString() + " x " + s.getCount();
    }
    private static String fromNBT(Tag nbt) {
        if (nbt == null)
            return "";
        return nbt.toString();
    }

    public SelectItemStackDialog(Widget p, String label, ItemStack orig, Consumer<ItemStack> cb) {
        super(p);
        setSize(211, 150);
        callback = cb;
        current = orig == null ? new ItemStack(Items.AIR) : orig.copy();

        int bsize = width / 2 - 10;

        buttonCancel = new SimpleTextButton(this, Lang.translateKey("gui.cancel"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                close();
            }

            @Override
            public boolean renderTitleInCenter() {
                return true;
            }
        };

        buttonCancel.setPosAndSize(27, height - 24, bsize, 16);

        buttonAccept = new SimpleTextButton(this, Lang.translateKey("gui.accept"), Icon.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                callback.accept(current);
                close();
            }

            @Override
            public boolean renderTitleInCenter() {
                return true;
            }
        };

        buttonAccept.setPosAndSize(width - bsize + 11, height - 24, bsize, 16);

        panelStacks = new BlankPanel(this) {
            @Override
            public void addWidgets() {
                update = System.currentTimeMillis() + 100L;
            }

            @Override
            public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
                theme.drawPanelBackground(matrixStack, x, y, w, h);
            }
        };

        panelStacks.setPosAndSize(28, 24, 9 * 19 + 1, 5 * 19 + 1);

        scrollBar = new PanelScrollBar(this, panelStacks);
        scrollBar.setCanAlwaysScroll(true);
        scrollBar.setScrollStep(20);

        searchBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                panelStacks.refreshWidgets();
            }
        };

        searchBox.setPosAndSize(27, 7, width - 16, 12);
        searchBox.ghostText = I18n.get("gui.search_box");
        searchBox.setFocused(true);

        tabs = new Panel(this) {
            @Override
            public void addWidgets() {
                add(new ButtonSwitchMode(this));
                add(new ButtonEditData(this));

                add(new ButtonCount(this));


                add(new ButtonNBT(this));
                add(new ButtonCaps(this));
            }

            @Override
            public void alignWidgets() {
                for (Widget widget : widgets) {
                    widget.setSize(20, 20);
                }

                setHeight(align(WidgetLayout.VERTICAL));
            }
        };

        tabs.setPosAndSize(0, 8, 20, 0);

        updateItemWidgets(Collections.emptyList());
    }

    @Override
    public void addWidgets() {
        add(tabs);
        add(panelStacks);
        add(scrollBar);
        add(searchBox);
        add(buttonCancel);
        add(buttonAccept);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        theme.drawGui(matrixStack, x, y, w, h, WidgetType.NORMAL);

        long now = System.currentTimeMillis();

        if (now >= update) {
            update = Long.MAX_VALUE;
            CompletableFuture.supplyAsync(() -> this.getItems(searchBox.getText().toLowerCase(), panelStacks), ITEM_SEARCH)
                    .thenAcceptAsync(this::updateItemWidgets, Minecraft.getInstance());
        }
    }

    public List<Widget> getItems(String search, Panel panel) {

        if (activeMode == null) {
            return Collections.emptyList();
        }

        Collection<ItemStack> items = activeMode.getAllResources();
        List<Widget> widgets = new ArrayList<>(search.isEmpty() ? items.size() + 1 : 64);

        String mod = "";

        if (search.startsWith("@")) {
            mod = search.substring(1);
        }

        ItemStackButton button = new ItemStackButton(panel, ItemStack.EMPTY);

        if (button.shouldAdd(search, mod)) {
            widgets.add(new ItemStackButton(panel, ItemStack.EMPTY));
        }

        for (ItemStack stack : items) {
            if (true) {
                button = new ItemStackButton(panel, stack);

                if (button.shouldAdd(search, mod)) {
                    widgets.add(button);
                    int j = widgets.size() - 1;
                    button.setPos(1 + (j % 9) * 19, 1 + (j / 9) * 19);
                }
            }
        }

        return widgets;
    }


    @Override
    public void onClose() {
    }

    private void updateItemWidgets(List<Widget> items) {
        panelStacks.getWidgets().clear();
        panelStacks.addAll(items);
        scrollBar.setPosAndSize(panelStacks.posX + panelStacks.width + 25, panelStacks.posY - 1, 16, panelStacks.height + 2);
        scrollBar.setValue(0);
        //scrollBar.setMaxValue(1 + Mth.ceil(panelStacks.getWidgets().size() / 9F) * 19);
    }
}