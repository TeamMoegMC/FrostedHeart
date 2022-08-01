package com.teammoeg.frostedheart.research.gui.editor;

import com.google.common.collect.Iterators;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import dev.ftb.mods.ftblibrary.config.ui.ItemSearchMode;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import me.shedaniel.architectury.registry.Registries;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author LatvianModder, khjxiaogu
 */
public class SelectItemStackDialog extends EditDialog {
    public static Editor<ItemStack> EDITOR = (p, l, v, c) -> {
        new SelectItemStackDialog(p, l, v, c).open();
    };
    public static Editor<Block> EDITOR_BLOCK = (p, l, v, c) -> {
        new SelectItemStackDialog(p, l + " (Blocks only)", new ItemStack(v), e -> {
            Block b = Block.getBlockFromItem(e.getItem());
            if (b != Blocks.AIR)
                c.accept(b);
        }).open();
    };

    private static final String fromNBT(INBT nbt) {
        if (nbt == null)
            return "";
        return nbt.toString();
    }

    public static final ExecutorService ITEM_SEARCH = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "FH-ItemSearch");
        thread.setDaemon(true);
        return thread;
    });

    public static final List<ItemSearchMode> modes = new ArrayList<>();

    static {
        modes.add(ItemSearchMode.ALL_ITEMS);
        modes.add(ItemSearchMode.INVENTORY);
        modes.add(new ItemSearchMode() {

            @Override
            public Collection<ItemStack> getAllItems() {
                return ForgeRegistries.BLOCKS.getValues().stream().map(Block::asItem).filter(Objects::nonNull).map(ItemStack::new).collect(Collectors.toList());
            }

            @Override
            public IFormattableTextComponent getDisplayName() {
                return GuiUtils.str("Blocks");
            }

            @Override
            public Icon getIcon() {
                return ItemIcon.getItemIcon(Blocks.STONE.asItem());
            }
        });
    }

    private static ItemSearchMode activeMode = null;

    private class ItemStackButton extends Button {
        private final ItemStack stack;

        private ItemStackButton(Panel panel, ItemStack is) {
            super(panel, StringTextComponent.EMPTY, Icons.BARRIER);
            setSize(18, 18);
            stack = is;
            title = null;
            icon = ItemIcon.getItemIcon(is);
        }

        public boolean shouldAdd(String search, String mod) {
            if (search.isEmpty()) {
                return true;
            }

            if (!mod.isEmpty()) {
                return Registries.getId(stack.getItem(), Registry.ITEM).getNamespace().contains(mod);
            }

            return stack.getDisplayName().getString().toLowerCase().contains(search);
        }

        @Override
        public ITextComponent getTitle() {
            if (title == null) {
                title = stack.getDisplayName();
            }

            return title;
        }

        @Override
        public void addMouseOverText(TooltipList list) {
        }

        @Override
        public WidgetType getWidgetType() {
            return stack.getItem() == current.getItem() && Objects.equals(stack.getTag(), current.getTag()) ? WidgetType.MOUSE_OVER : super.getWidgetType();
        }

        @Override
        public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
            (getWidgetType() == WidgetType.MOUSE_OVER ? Color4I.LIGHT_GREEN.withAlpha(70) : Color4I.BLACK.withAlpha(50)).draw(matrixStack, x, y, w, h);
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            current = stack.copy();
        }
    }

    private class ButtonSwitchMode extends Button {
        private final Iterator<ItemSearchMode> modeIterator = Iterators.cycle(modes);

        public ButtonSwitchMode(Panel panel) {
            super(panel);
            activeMode = modeIterator.next();
        }

        @Override
        public void drawIcon(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
            activeMode.getIcon().draw(matrixStack, x, y, w, h);
        }

        @Override
        public ITextComponent getTitle() {
            return new TranslationTextComponent("ftblibrary.select_item.list_mode");
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            super.addMouseOverText(list);
            list.add(activeMode.getDisplayName().mergeStyle(TextFormatting.GRAY).appendSibling(new StringTextComponent(" [" + panelStacks.widgets.size() + "]").mergeStyle(TextFormatting.DARK_GRAY)));
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            activeMode = modeIterator.next();
            panelStacks.refreshWidgets();
        }
    }

    private abstract class ButtonStackConfig extends Button {
        public ButtonStackConfig(Panel panel, ITextComponent title, Icon icon) {
            super(panel, title, icon);
        }

        @Override
        public WidgetType getWidgetType() {
            return current.isEmpty() ? WidgetType.DISABLED : super.getWidgetType();
        }
    }

    private class ButtonEditData extends Button {
        public ButtonEditData(Panel panel) {
            super(panel, StringTextComponent.EMPTY, Icons.BUG);
        }

        @Override
        public void drawIcon(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
            matrixStack.push();
            matrixStack.translate(0, 0, 100);
            GuiHelper.drawItem(matrixStack, current, x, y, w / 16F, h / 16F, true, null);
            matrixStack.pop();
        }

        @Override
        public ITextComponent getTitle() {
            return current.getDisplayName();
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            EditPrompt.open(this, "Data", current.write(new CompoundNBT()).toString(), s -> {
                try {
                    current = ItemStack.read(JsonToNBT.getTagFromJson(s));
                } catch (CommandSyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
    }

    private class ButtonCount extends ButtonStackConfig {
        public ButtonCount(Panel panel) {
            super(panel, new TranslationTextComponent("ftblibrary.select_item.count"), ItemIcon.getItemIcon(Items.PAPER));
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            EditPrompt.open(this, "count", String.valueOf(current.getCount()), val -> {
                current.setCount(Integer.parseInt(val));

            });
        }
    }

    private class ButtonNBT extends ButtonStackConfig {
        public ButtonNBT(Panel panel) {
            super(panel, new TranslationTextComponent("ftblibrary.select_item.nbt"), ItemIcon.getItemIcon(Items.NAME_TAG));
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            EditPrompt.open(this, "nbt", fromNBT(current.getTag()), s -> {
                try {
                    current.setTag(JsonToNBT.getTagFromJson(s));
                } catch (CommandSyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
    }

    private class ButtonCaps extends ButtonStackConfig {
        public ButtonCaps(Panel panel) {
            super(panel, new TranslationTextComponent("ftblibrary.select_item.caps"), ItemIcon.getItemIcon(Items.ANVIL));
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();

            final CompoundNBT nbt = current.write(new CompoundNBT());


            EditPrompt.open(this, "caps", fromNBT(nbt.get("ForgeCaps")), s -> {
                if (s == null || s.isEmpty() || s.equals("null")) {
                    nbt.remove("ForgeCaps");
                } else {
                    try {
                        nbt.put("ForgeCaps", JsonToNBT.getTagFromJson(s));
                    } catch (CommandSyntaxException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                current = ItemStack.read(nbt);
            });
        }
    }

    public List<Widget> getItems(String search, Panel panel) {

        if (activeMode == null) {
            return Collections.emptyList();
        }

        Collection<ItemStack> items = activeMode.getAllItems();
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
            if (!stack.isEmpty()) {
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

    private final Consumer<ItemStack> callback;
    private ItemStack current;
    private final Button buttonCancel, buttonAccept;
    private final Panel panelStacks;
    private final PanelScrollBar scrollBar;
    private final TextBox searchBox;
    private final Panel tabs;
    public long update = Long.MAX_VALUE;

    public static final Editor<Collection<ItemStack>> STACK_LIST = (p, l, v, c) -> {
        new EditListDialog<>(p, l, v, new ItemStack(Items.AIR), EDITOR, SelectItemStackDialog::fromItemStack, ItemIcon::getItemIcon, c).open();
    };
    public static final Editor<Collection<Block>> BLOCK_LIST = (p, l, v, c) -> {
        new EditListDialog<>(p, l, v, Blocks.AIR, EDITOR_BLOCK, e -> e.getTranslatedName().getString(), e -> ItemIcon.getItemIcon(e.asItem()), c).open();
    };

    private static String fromItemStack(ItemStack s) {
        return s.getDisplayName().getString() + " x " + s.getCount();
    }

    public SelectItemStackDialog(Widget p, String label, ItemStack orig, Consumer<ItemStack> cb) {
        super(p);
        setSize(211, 150);
        callback = cb;
        current = orig == null ? new ItemStack(Items.AIR) : orig.copy();

        int bsize = width / 2 - 10;

        buttonCancel = new SimpleTextButton(this, new TranslationTextComponent("gui.cancel"), Icon.EMPTY) {
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

        buttonAccept = new SimpleTextButton(this, new TranslationTextComponent("gui.accept"), Icon.EMPTY) {
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
            public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
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
        searchBox.ghostText = I18n.format("gui.search_box");
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

    private void updateItemWidgets(List<Widget> items) {
        panelStacks.widgets.clear();
        panelStacks.addAll(items);
        scrollBar.setPosAndSize(panelStacks.posX + panelStacks.width + 25, panelStacks.posY - 1, 16, panelStacks.height + 2);
        scrollBar.setValue(0);
        scrollBar.setMaxValue(1 + MathHelper.ceil(panelStacks.widgets.size() / 9F) * 19);
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
    public void onClosed() {


    }


    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        theme.drawGui(matrixStack, x, y, w, h, WidgetType.NORMAL);

        long now = System.currentTimeMillis();

        if (now >= update) {
            update = Long.MAX_VALUE;
            CompletableFuture.supplyAsync(() -> this.getItems(searchBox.getText().toLowerCase(), panelStacks), ITEM_SEARCH)
                    .thenAcceptAsync(this::updateItemWidgets, Minecraft.getInstance());
        }
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void onClose() {
    }
}