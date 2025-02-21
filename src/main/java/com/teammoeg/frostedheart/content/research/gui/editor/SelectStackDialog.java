/*
 * Copyright (c) 2024 TeamMoeg
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

import com.google.common.collect.Iterators;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.TextBox;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.client.Lang;

import dev.ftb.mods.ftblibrary.ui.Panel;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author khjxiaogu
 */
public class SelectStackDialog<T> extends EditDialog {
	public interface ResourceLister<T>{
		Collection<T> getResources();
		
		MutableComponent getDisplayName();
		CIcon getIcon();
	}
	public interface ResourceMode<T>{
		CIcon getResourceIcon(T t);
		T copy(T t);
		T getDefaultValue();
		boolean isEmpty(T t);
		Component getTitle(T t);
		void appendTooltip(T t,Consumer<Component> tooltip);
		String getModid(T t);
		boolean isSame(T t1,T t2);
		CompoundTag getTag(T t);
		void setTag(T t,CompoundTag tag);
		CompoundTag save(T t);
		T load(CompoundTag tag);
		int getCount(T t);
		void setCount(T t,int num);
	}
	private static ResourceMode<ItemStack> itemMode=new ResourceMode<>() {
		@Override
		public CIcon getResourceIcon(ItemStack t) {
			return CIcons.getIcon(t);
		}

		@Override
		public ItemStack copy(ItemStack t) {
			return t.copy();
		}

		@Override
		public ItemStack getDefaultValue() {
			return ItemStack.EMPTY;
		}

		@Override
		public Component getTitle(ItemStack t) {
			return t.getHoverName();
		}

		@Override
		public void appendTooltip(ItemStack t, Consumer<Component> tooltip) {
			for(Component i:t.getTooltipLines(ClientUtils.getPlayer(), TooltipFlag.NORMAL))
				tooltip.accept(i);
		}

		@Override
		public String getModid(ItemStack t) {
			return CRegistryHelper.getRegistryName(t.getItem()).getNamespace();
		}

		@Override
		public boolean isSame(ItemStack t1, ItemStack t2) {
			return ItemStack.isSameItemSameTags(t1, t2);
		}

		@Override
		public boolean isEmpty(ItemStack t) {
			return t.isEmpty();
		}

		@Override
		public CompoundTag getTag(ItemStack t) {
			return t.getTag();
		}

		@Override
		public void setTag(ItemStack t, CompoundTag tag) {
			t.setTag(tag);
		}

		@Override
		public CompoundTag save(ItemStack t) {
			return t.serializeNBT();
		}

		@Override
		public ItemStack load(CompoundTag tag) {
			return ItemStack.of(tag);
		}

		@Override
		public int getCount(ItemStack t) {
			return t.getCount();
		}

		@Override
		public void setCount(ItemStack t, int num) {
			t.setCount(num);
		}
	};
    private static ResourceLister<ItemStack> ALL_ITEM=new ResourceLister<>() {

        @Override
        public Collection<ItemStack> getResources() {
            return CRegistryHelper.getItems().stream().filter(t -> t != null && t != Items.AIR).map(ItemStack::new).collect(Collectors.toList());
        }

        @Override
        public MutableComponent getDisplayName() {
            return Components.str("All Items");
        }

        @Override
        public CIcon getIcon() {
            return CIcons.getIcon(Items.CLOCK);
        }


    };
    private static ResourceLister<ItemStack> INVENTORY=new ResourceLister<>() {

        @Override
        public Collection<ItemStack> getResources() {
        	 return ClientUtils.getPlayer().getInventory().items.stream().filter(t -> t != null && !t.isEmpty()).map(ItemStack::copy).collect(Collectors.toList());
        }

        @Override
        public MutableComponent getDisplayName() {
            return Components.str("Inventory");
        }

        @Override
        public CIcon getIcon() {
            return CIcons.getIcon(Items.CHEST);
        }

    };
    private static ResourceLister<ItemStack> BLOCKS=new ResourceLister<>() {

            @Override
            public Collection<ItemStack> getResources() {
                return CRegistryHelper.getBlocks().stream().map(Block::asItem).filter(Objects::nonNull).filter(t -> t != Items.AIR).map(ItemStack::new).collect(Collectors.toList());
            }

            @Override
            public MutableComponent getDisplayName() {
                return Components.str("Blocks");
            }

            @Override
            public CIcon getIcon() {
                return CIcons.getIcon(Blocks.STONE.asItem());
            }
        };
    
    public static final ExecutorService ITEM_SEARCH = Executors.newSingleThreadExecutor(CUtils.makeThreadFactory("Chorda-ItemSearch", true));
    public static Editor<ItemStack> EDITOR = (p, l, v, c) -> new SelectStackDialog<ItemStack>(p, l, v, c,itemMode,ALL_ITEM,INVENTORY,BLOCKS).open();
    public static final Editor<Collection<ItemStack>> STACK_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, new ItemStack(Items.AIR), EDITOR, SelectStackDialog::fromItemStack, CIcons::getIcon, c).open();
    public static Editor<Block> EDITOR_BLOCK = (p, l, v, c) -> new SelectStackDialog<ItemStack>(p, Components.empty().append(l).append(" (Blocks only)"), new ItemStack(v), e -> {
        Block b = Block.byItem(e.getItem());
        if (b != Blocks.AIR)
            c.accept(b);
    },itemMode,INVENTORY,BLOCKS).open();
    public static final Editor<Collection<Block>> BLOCK_LIST = (p, l, v, c) -> new EditListDialog<>(p, l, v, Blocks.AIR, EDITOR_BLOCK, e -> e.getName().getString(), e -> CIcons.getIcon(e.asItem()), c).open();
    private ResourceLister<T> activeMode = null;
    public final List<ResourceLister<T>> modes = new ArrayList<>();
    public ResourceMode<T> type;
    private final Consumer<T> callback;
    private final TextButton buttonCancel, buttonAccept;
    private final Layer panelStacks;
    private final LayerScrollBar scrollBar;
    private final TextBox searchBox;
    private final Layer tabs;
    public long update = Long.MAX_VALUE;
    private T current;

    @SafeVarargs
	public SelectStackDialog(UIWidget p, Component label, T orig, Consumer<T> cb,ResourceMode<T> mode,ResourceLister<T>...listers) {
        super(p);
        setSize(222, 150);
        callback = cb;
        this.type=mode;
        current = orig == null ? type.getDefaultValue() : type.copy(orig);
        
        this.modes.addAll(Arrays.asList(listers));
        int bsize = width / 2 - 30;

        buttonCancel = new TextButton(this, Lang.translateKey("gui.cancel"), CIcons.nop()) {
            @Override
            public void onClicked(MouseButton button) {
                CInputHelper.playClickSound();
                close();
            }

            @Override
            public boolean renderTitleInCenter() {
                return true;
            }
        };

        buttonCancel.setPosAndSize(27, height - 24, bsize, 16);

        buttonAccept = new TextButton(this, Lang.translateKey("gui.accept"), CIcons.nop()) {
            @Override
            public void onClicked(MouseButton button) {
            	CInputHelper.playClickSound();
                callback.accept(current);
                close();
            }

            @Override
            public boolean renderTitleInCenter() {
                return true;
            }
        };

        buttonAccept.setPosAndSize(width - bsize -29, height - 24, bsize, 16);

        panelStacks = new Layer(this) {
            @Override
            public void addUIElements() {
                update = System.currentTimeMillis() + 100L;
            }

            @Override
            public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
            	CGuiHelper.drawLayerBackground(matrixStack, x, y, w, h);
            }

			@Override
			public void alignWidgets() {
				
			}
        };

        panelStacks.setPosAndSize(28, 24, 9 * 19 + 1, 5 * 19 + 1);

        scrollBar = new LayerScrollBar(this, panelStacks);
        scrollBar.setIgnoreMouseOver(true);
        scrollBar.setScrollStep(20);

        searchBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                panelStacks.refresh();
            }
        };

        searchBox.setPosAndSize(27, 4, width - 32, 16);
        searchBox.ghostText = I18n.get("gui.search_box");
        searchBox.setFocused(true);

        tabs = new Layer(this) {
            @Override
            public void addUIElements() {
                add(new ButtonSwitchMode(tabs));
                add(new ButtonEditData(tabs));

                add(new ButtonCount(tabs));


                add(new ButtonNBT(tabs));
                add(new ButtonCaps(tabs));
            }

            @Override
            public void alignWidgets() {
                for (UIWidget widget : super.elements) {
                    widget.setSize(20, 20);
                }
                setWidth(20);
                setHeight(align(false));
            }

        };

        tabs.setPosAndSize(5, 8, 20, 100);

        updateItemWidgets(Collections.emptyList());
    }

    private static String fromItemStack(ItemStack s) {
        return s.getHoverName().getString() + " x " + s.getCount();
    }

    private static String fromNBT(Tag nbt) {
        if (nbt == null)
            return "";
        return nbt.toString();
    }

    @Override
    public void addUIElements() {
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
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
        CGuiHelper.drawUIBackground(matrixStack, x, y, w, h);

        long now = System.currentTimeMillis();

        if (now >= update) {
            update = Long.MAX_VALUE;
            CompletableFuture.supplyAsync(() -> this.getItems(searchBox.getText().toLowerCase(), panelStacks), ITEM_SEARCH)
                    .thenAcceptAsync(this::updateItemWidgets, Minecraft.getInstance());
        }
    }

    public List<UIWidget> getItems(String search, Layer panel) {

        if (activeMode == null) {
            return Collections.emptyList();
        }

        Collection<T> items = activeMode.getResources();
        List<UIWidget> widgets = new ArrayList<>(search.isEmpty() ? items.size() + 1 : 64);

        String mod = "";
        if (search.startsWith("@")) {
            mod = search.substring(1);
        }

        ItemStackButton button = new ItemStackButton(panel, type.getDefaultValue());

        if (button.shouldAdd(search, mod)) {
            widgets.add(new ItemStackButton(panel, type.getDefaultValue()));
        }

        for (T stack : items) {
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

    private void updateItemWidgets(List<UIWidget> items) {
        panelStacks.getElements().clear();
        for(UIWidget elm:items)
        	panelStacks.add((UIWidget)elm);
        scrollBar.setPosAndSize(panelStacks.getX() + panelStacks.getWidth() +3, panelStacks.getY() - 1, 10, panelStacks.getHeight() + 2);
        scrollBar.setValue(0);
        
        //scrollBar.setMaxValue(1 + Mth.ceil(panelStacks.getWidgets().size() / 9F) * 19);
    }

    private class ButtonCaps extends ButtonStackConfig {
        public ButtonCaps(UIWidget panel) {
            super(panel, Components.str("Caps"), CIcons.getIcon(Items.ANVIL));
        }

        @Override
        public void onClicked(MouseButton button) {
        	CInputHelper.playClickSound();

            final CompoundTag nbt = type.save(current);


            EditPrompt.open(this, Components.str("capability"), fromNBT(nbt.get("ForgeCaps")), s -> {
            	
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
                current = type.load(nbt);
            });
        }
    }

    private class ButtonCount extends ButtonStackConfig {
        public ButtonCount(UIWidget panel) {
            super(panel, Components.str("Count"), CIcons.getIcon(Items.PAPER));
        }

        @Override
        public void onClicked(MouseButton button) {
        	CInputHelper.playClickSound();
            EditPrompt.open(this, Components.str("Count"), String.valueOf(type.getCount(current)), val -> type.setCount(current,Integer.parseInt(val)));
        }
    }

    private class ButtonEditData extends Button {
        public ButtonEditData(UIWidget panel) {
            super(panel, Component.empty(), CIcons.getIcon(Items.BARRIER));
        }

        @Override
        public void drawIcon(GuiGraphics guiGraphics, int x, int y, int w, int h) {
            type.getResourceIcon(current).draw(guiGraphics, x, y, w, h);
            //CGuis.drawItem(matrixStack, current, x, y, w / 16F, h / 16F, true, null);
        }

        @Override
		public void getTooltip(Consumer<Component> tooltip) {
			type.appendTooltip(current, tooltip);
		}

		@Override
        public Component getTitle() {
            return type.getTitle(current);
        }

        @Override
        public void onClicked(MouseButton button) {
        	CInputHelper.playClickSound();
            EditPrompt.open(this, Components.str("Data"), type.save(current).toString(), s -> {
                try {
                    current = type.load(TagParser.parseTag(s));
                } catch (CommandSyntaxException e) {
                    FHMain.LOGGER.error("Error parsing NBT when setting ItemStack in SelectItemStackDialog");
                    e.printStackTrace();
                }
            });
        }
    }

    private class ButtonNBT extends ButtonStackConfig {
        public ButtonNBT(UIWidget panel) {
            super(panel, Components.str("nbt"), CIcons.getIcon(Items.NAME_TAG));
        }

        @Override
        public void onClicked(MouseButton button) {
        	CInputHelper.playClickSound();
            EditPrompt.open(this, Components.str("NBTag"), fromNBT(type.getTag(current)), s -> {
                try {
                    type.setTag(current,TagParser.parseTag(s));
                } catch (CommandSyntaxException e) {
                    FHMain.LOGGER.error("Error parsing NBT when setting NBT in SelectItemStackDialog");
                    e.printStackTrace();
                }
            });
        }
    }

    private abstract class ButtonStackConfig extends Button {
        public ButtonStackConfig(UIWidget panel, Component title, CIcon icon) {
            super(panel, title, icon);
        }

        @Override
        public boolean isEnabled() {
            return type.isEmpty(current) ? false:super.isEnabled();
        }
    }

    private class ButtonSwitchMode extends Button {
        private final Iterator<ResourceLister<T>> modeIterator = Iterators.cycle(modes);

        public ButtonSwitchMode(UIWidget panel) {
            super(panel);
            activeMode = modeIterator.next();
        }

        @Override
        public void getTooltip(Consumer<Component> list) {
            super.getTooltip(list);
            list.accept(activeMode.getDisplayName().withStyle(ChatFormatting.GRAY).append(Components.str(" [" + panelStacks.getElements().size() + "]").withStyle(ChatFormatting.DARK_GRAY)));
        }

        @Override
        public void drawIcon(GuiGraphics matrixStack, int x, int y, int w, int h) {
            activeMode.getIcon().draw(matrixStack, x, y, w, h);
        }

        @Override
        public Component getTitle() {
            return Components.str("mode");
        }

        @Override
        public void onClicked(MouseButton button) {
            CInputHelper.playClickSound();
            activeMode = modeIterator.next();
            panelStacks.refresh();
        }
    }

    private class ItemStackButton extends Button {
        private final T stack;
        private ItemStackButton(UIWidget panel, T is) {
            super(panel, Component.empty(), CIcons.getIcon(Items.BARRIER));
            setSize(18, 18);
            stack = is;
            title = null;
            icon = type.getResourceIcon(stack);
        }

        @Override
        public void getTooltip(Consumer<Component> list) {
        	type.appendTooltip(stack, list);
        }

        @Override
        public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
        	matrixStack.fill(x, y, x+w, y+h, (this.isMouseOver()||type.isSame(current, stack))?0x46FFFFFF:0x22000000);

        }

        @Override
        public Component getTitle() {
            if (title == null) {
                title = type.getTitle(stack);
            }

            return title;
        }

        @Override
        public void onClicked(MouseButton button) {
            CInputHelper.playClickSound();
            current = type.copy(stack);
        }

        public boolean shouldAdd(String search, String mod) {
            if (search.isEmpty()) {
                return true;
            }

            if (!mod.isEmpty()) {
                return type.getModid(stack).contains(mod);
            }

            return type.getTitle(stack).getString().toLowerCase().contains(search.toLowerCase());
        }
    }
}