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

package com.teammoeg.frostedheart.content.research.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.research.gui.editor.BaseEditDialog;
import com.teammoeg.frostedheart.content.research.gui.editor.EditListDialog;
import com.teammoeg.frostedheart.content.research.gui.editor.EditPrompt;
import com.teammoeg.frostedheart.content.research.gui.editor.EditUtils;
import com.teammoeg.frostedheart.content.research.gui.editor.Editor;
import com.teammoeg.frostedheart.content.research.gui.editor.EditorSelector;
import com.teammoeg.frostedheart.content.research.gui.editor.IngredientEditor;
import com.teammoeg.frostedheart.content.research.gui.editor.LabeledTextBox;
import com.teammoeg.frostedheart.content.research.gui.editor.NumberBox;
import com.teammoeg.frostedheart.content.research.gui.editor.OpenEditorButton;
import com.teammoeg.frostedheart.content.research.gui.editor.SelectDialog;
import com.teammoeg.frostedheart.content.research.gui.editor.SelectItemStackDialog;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.codec.AlternativeCodecBuilder;
import com.teammoeg.frostedheart.util.io.codec.NopCodec;
import com.teammoeg.frostedheart.util.io.registry.TypedCodecRegistry;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.resources.ResourceLocation;

public class FHIcons {
    private static final TypedCodecRegistry<FHIcon> serializers = new TypedCodecRegistry<>();
	public static final Codec<FHIcon> CODEC=new AlternativeCodecBuilder<FHIcon>(FHIcon.class)
		.add(FHNopIcon.class ,FHNopIcon.CODEC)
		.add(FHItemIcon.class, FHItemIcon.ICON_CODEC)
		.add(FHItemIcon.class,FHItemIcon.CODEC)
		.add(FHAnimatedIcon.class,FHAnimatedIcon.ICON_CODEC)
		.add(serializers.codec())
		.add(FHNopIcon.CODEC)
		.build();
    private static class FHAnimatedIcon extends FHIcon {
        private static final Codec<FHAnimatedIcon> CODEC=RecordCodecBuilder.create(t->t.group(
        	Codec.list(FHIcons.CODEC).fieldOf("icons").forGetter(o->o.icons)
        	).apply(t, FHAnimatedIcon::new));
        private static final Codec<FHAnimatedIcon> ICON_CODEC=Codec.list(FHIcons.CODEC).xmap(FHAnimatedIcon::new, o->o.icons);
        List<FHIcon> icons;
        public FHAnimatedIcon() {
            icons = new ArrayList<>();
        }

        public FHAnimatedIcon(List<FHIcon> icons) {
			super();
			this.icons = new ArrayList<>(icons);
		}

		public FHAnimatedIcon(FHIcon... icons2) {
            this();
            icons.addAll(Arrays.asList(icons2));
        }

        @Override
        public void draw(GuiGraphics ms, int x, int y, int w, int h) {
            if (!icons.isEmpty()) {
                GuiHelper.setupDrawing();
                icons.get((int) ((System.currentTimeMillis() / 1000) % icons.size())).draw(ms, x, y, w, h);
            }
        }

    }

    private static class FHCombinedIcon extends FHIcon {
        private static final Codec<FHCombinedIcon> CODEC=RecordCodecBuilder.create(t->t.group(
        	FHIcons.CODEC.fieldOf("base").forGetter(o->o.large),
        	FHIcons.CODEC.fieldOf("small").forGetter(o->o.small)
        	).apply(t, FHCombinedIcon::new));
        FHIcon large;
        FHIcon small;

        public FHCombinedIcon(FHIcon base, FHIcon small) {
            this.large = base;
            this.small = small;
        }

        @Override
        public void draw(GuiGraphics ms, int x, int y, int w, int h) {
            GuiHelper.setupDrawing();
            if (large != null)
                large.draw(ms, x, y, w, h);
            ms.pose().pushPose();
            ms.pose().translate(0, 0, 110);// let's get top most
            GuiHelper.setupDrawing();
            if (small != null)
                small.draw(ms, x + w / 2, y + h / 2, w / 2, h / 2);
            ms.pose().popPose();
        }
    }

    private static class FHDelegateIcon extends FHIcon {
        private static final Codec<FHDelegateIcon> CODEC=RecordCodecBuilder.create(t->t.group(
        	Codec.STRING.fieldOf("name").forGetter(o->o.name)
        	).apply(t, FHDelegateIcon::new));
        String name;

        public FHDelegateIcon(String name) {
            super();
            this.name = name;
        }

        @Override
        public void draw(GuiGraphics ms, int x, int y, int w, int h) {
            GuiHelper.setupDrawing();
            TechIcons.internals.get(name).draw(ms, x, y, w, h);
        }


    }

    public static abstract class FHIcon extends Icon implements Cloneable {
        public FHIcon() {
            super();
        }

        @Override
        public FHIcon clone() {
            try {
                return (FHIcon) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    private static class FHIngredientIcon extends FHAnimatedIcon {
        private static final Codec<FHIngredientIcon> CODEC=RecordCodecBuilder.create(t->t.group(
        	CodecUtil.INGREDIENT_CODEC.fieldOf("ingredient").forGetter(o->o.igd)
        	).apply(t, FHIngredientIcon::new));
        Ingredient igd;

        public FHIngredientIcon(Ingredient i) {
            igd = i;
            for (ItemStack stack : igd.getItems())
                icons.add(new FHItemIcon(stack));
        }

        public FHIngredientIcon(JsonElement elm) {
            this(Ingredient.fromJson(elm.getAsJsonObject().get("ingredient")));
        }
    }

    private static class FHItemIcon extends FHIcon {
        private static final Codec<FHItemIcon> CODEC=RecordCodecBuilder.create(t->t.group(
        	CodecUtil.ITEMSTACK_CODEC.fieldOf("item").forGetter(o->o.stack)
        	).apply(t, FHItemIcon::new));
        private static final Codec<FHItemIcon> ICON_CODEC=
        	CodecUtil.ITEMSTACK_CODEC.xmap(FHItemIcon::new, o->o.stack);
        ItemStack stack;

        public FHItemIcon(ItemLike item2) {
            this(new ItemStack(item2));
        }

        public FHItemIcon(ItemStack stack) {
            this.stack = stack;
        }
        
        @Override
        public void draw(GuiGraphics matrixStack, int x, int y, int w, int h) {
        	//ItemRenderer itemRenderer=ClientUtils.mc().getItemRenderer();
        	/*
            itemRenderer.zLevel = 200.0F;
            net.minecraft.client.gui.FontRenderer font = stack.getItem().getFontRenderer(stack);
            if (font == null) font = ClientUtils.mc().fontRenderer;
            itemRenderer.renderItemAndEffectIntoGUI(stack, x, y);
            itemRenderer.renderItemOverlayIntoGUI(font, stack, x, y, null);
            itemRenderer.zLevel = 0.0F;*/
        	matrixStack.pose().pushPose();
        	matrixStack.pose().translate(0,0, 199);
        	//TODO repair draw item
        	//GuiHelper.drawItem(matrixStack, stack, x, y, w/16f, h/16f, true, null);
            matrixStack.pose().popPose();
            /*ClientUtils.mc().getItemRenderer().renderItem(stack, TransformType.GUI,LightTexture., y, matrixStack, null);
            if (stack != null && stack.getCount() > 1) {
                matrixStack.push();
                matrixStack.translate(x + w - 8, y + h - 7, 199);
                matrixStack.push();
                matrixStack.scale(w / 16f, h / 16f, 0);
                ClientUtils.mc().fontRenderer.drawStringWithShadow(matrixStack, String.valueOf(stack.getCount()), 0, 0,
                        0xffffffff);
                matrixStack.pop();
                matrixStack.pop();
            }*/
        }
    }

    private static class FHNopIcon extends FHIcon {
       
        public static final FHNopIcon INSTANCE = new FHNopIcon();
        private static final Codec<FHNopIcon> CODEC=new NopCodec<>(INSTANCE);


        private FHNopIcon() {
        }

        @Override
        public void draw(GuiGraphics ms, int x, int y, int w, int h) {
        }


    }

    private static class FHTextIcon extends FHIcon {
        private static final Codec<FHTextIcon> CODEC=RecordCodecBuilder.create(t->t.group(
        	Codec.STRING.fieldOf("text").forGetter(o->o.text)
        	).apply(t, FHTextIcon::new));
        String text;

        public FHTextIcon(String text) {
            super();
            this.text = text;
        }

        @Override
        public void draw(GuiGraphics ms, int x, int y, int w, int h) {

            ms.pose().pushPose();
            ms.pose().translate(x, y, 0);
            ms.pose().scale(w / 16f, h / 16f, 0);

            ms.pose().pushPose();

            ms.pose().scale(2.286f, 2.286f, 0);// scale font height 7 to height 16
            ms.drawString(ClientUtils.mc().font,text, 0, 0, 0xFFFFFFFF);
            ms.pose().popPose();
            ms.pose().popPose();
            GuiHelper.setupDrawing();
        }

    }

    private static class FHTextureIcon extends FHIcon {
        private static final Codec<FHTextureIcon> CODEC=RecordCodecBuilder.create(t->t.group(
        	ResourceLocation.CODEC.fieldOf("location").forGetter(o->o.rl)
        	).apply(t, FHTextureIcon::new));
        Icon nested;
        ResourceLocation rl;

        public FHTextureIcon(ResourceLocation rl) {
            this.rl = rl;
            nested = ImageIcon.getIcon(rl);
        }

        @Override
        public void draw(GuiGraphics ms, int x, int y, int w, int h) {
            GuiHelper.setupDrawing();
            nested.draw(ms, x, y, w, h);
        }

    }

    private static class FHTextureUVIcon extends FHIcon {
        private static final Codec<FHTextureUVIcon> CODEC=RecordCodecBuilder.create(t->t.group(
        	ResourceLocation.CODEC.fieldOf("location").forGetter(o->o.rl),
        	Codec.INT.fieldOf("x") .forGetter(o->o.x),
        	Codec.INT.fieldOf("y") .forGetter(o->o.y),
        	Codec.INT.fieldOf("w") .forGetter(o->o.w),
        	Codec.INT.fieldOf("h") .forGetter(o->o.h),
        	Codec.INT.fieldOf("tw").forGetter(o->o.tw),
        	Codec.INT.fieldOf("th").forGetter(o->o.th)
        	).apply(t, FHTextureUVIcon::new));
        Icon nested;
        ResourceLocation rl;
        int x, y, w, h, tw, th;

        public FHTextureUVIcon() {
        }

        public FHTextureUVIcon(ResourceLocation rl, int x, int y, int w, int h, int tw, int th) {
            super();
            this.rl = rl;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.tw = tw;
            this.th = th;
            init();
        }

        @Override
        public void draw(GuiGraphics ms, int x, int y, int w, int h) {
            GuiHelper.setupDrawing();
            if (nested != null)
                nested.draw(ms, x, y, w, h);
        }

        public void init() {
            nested = ImageIcon.getIcon(rl).withUV(x, y, w, h, tw, th);
        }

    }

    public static abstract class IconEditor<T extends FHIcon> extends BaseEditDialog {

        private static class Combined extends IconEditor<FHCombinedIcon> {
            String label;
            Consumer<FHCombinedIcon> i;

            public Combined(Widget panel, String label, FHCombinedIcon v, Consumer<FHCombinedIcon> i) {
                super(panel, v == null ? new FHCombinedIcon(null, null) : v);
                this.label = label;
                this.i = i;
            }

            @Override
            public void addWidgets() {
                add(EditUtils.getTitle(this, label));
                add(new OpenEditorButton<>(this, "Edit base icon", EDITOR, v.large, e -> v.large = e));
                add(new OpenEditorButton<>(this, "Edit corner icon", EDITOR, v.small, e -> v.small = e));
            }

            @Override
            public void onClose() {
                i.accept(v);

            }

        }

        private static class UV extends IconEditor<FHTextureUVIcon> {
            String label;
            Consumer<FHTextureUVIcon> i;
            LabeledTextBox rl;
            NumberBox x;
            NumberBox y;
            NumberBox w;
            NumberBox h;
            NumberBox tw;
            NumberBox th;

            public UV(Widget panel, String label, FHTextureUVIcon v, Consumer<FHTextureUVIcon> i) {
                super(panel, v == null ? new FHTextureUVIcon() : v);
                this.label = label;
                this.i = i;
                v = this.v;
                rl = new LabeledTextBox(this, "Texture", this.v.rl == null ? "" : this.v.rl.toString());
                x = new NumberBox(this, "X", (v.x));
                y = new NumberBox(this, "Y", (v.y));
                w = new NumberBox(this, "Width", (v.w));
                h = new NumberBox(this, "Height", (v.h));
                tw = new NumberBox(this, "Texture Width", (v.tw));
                th = new NumberBox(this, "Texture Height", (v.th));
            }

            @Override
            public void addWidgets() {
                add(EditUtils.getTitle(this, label));
                add(rl);
                add(x);
                add(y);
                add(w);
                add(h);
                add(tw);
                add(th);
                add(new SimpleTextButton(this, TranslateUtils.str("Commit"), Icon.empty()) {
                    @Override
                    public void onClicked(MouseButton arg0) {
                        v.rl = new ResourceLocation(rl.getText());
                        v.x = (int) x.getNum();
                        v.y = (int) y.getNum();
                        v.w = (int) w.getNum();
                        v.h = (int) h.getNum();
                        v.tw = (int) tw.getNum();
                        v.th = (int) th.getNum();
                        v.init();

                    }

                });
            }

            @Override
            public void onClose() {

                v.rl = new ResourceLocation(rl.getText());
                v.x = (int) x.getNum();
                v.y = (int) y.getNum();
                v.w = (int) w.getNum();
                v.h = (int) h.getNum();
                v.tw = (int) tw.getNum();
                v.th = (int) th.getNum();
                v.init();
                i.accept(v);

            }

        }
        public static final Editor<FHIcon> EDITOR = (p, l, v, c) -> {
            if (v == null || v instanceof FHNopIcon) {
                new EditorSelector<>(p, l, c).addEditor("Empty", IconEditor.NOP_EDITOR)
                        .addEditor("ItemStack", IconEditor.ITEM_EDITOR).addEditor("Texture", IconEditor.TEXTURE_EDITOR)
                        .addEditor("Texture with UV", IconEditor.UV_EDITOR).addEditor("Text", IconEditor.TEXT_EDITOR)
                        .addEditor("Ingredient", IconEditor.INGREDIENT_EDITOR)
                        .addEditor("IngredientWithSize", IconEditor.INGREDIENT_SIZE_EDITOR)
                        .addEditor("Internal", IconEditor.INTERNAL_EDITOR)
                        .addEditor("Combined", IconEditor.COMBINED_EDITOR)
                        .addEditor("Animated", IconEditor.ANIMATED_EDITOR).open();
            } else
                new EditorSelector<>(p, l, (o, t) -> true, v, c).addEditor("Edit", IconEditor.CHANGE_EDITOR)
                        .addEditor("New", IconEditor.NOP_CHANGE_EDITOR).open();
        };
        public static final Editor<FHIcon> CHANGE_EDITOR = (p, l, v, c) -> {
            if (v instanceof FHItemIcon) {
                IconEditor.ITEM_EDITOR.open(p, l, (FHItemIcon) v, c::accept);
            } else if (v instanceof FHCombinedIcon) {
                IconEditor.COMBINED_EDITOR.open(p, l, (FHCombinedIcon) v, c::accept);
            } else if (v instanceof FHIngredientIcon) {
                IconEditor.INGREDIENT_EDITOR.open(p, l, (FHIngredientIcon) v, c::accept);
            } else if (v instanceof FHAnimatedIcon) {
                IconEditor.ANIMATED_EDITOR.open(p, l, (FHAnimatedIcon) v, c::accept);
            } else if (v instanceof FHTextureIcon) {
                IconEditor.TEXTURE_EDITOR.open(p, l, (FHTextureIcon) v, c::accept);
            } else if (v instanceof FHTextureUVIcon) {
                IconEditor.UV_EDITOR.open(p, l, (FHTextureUVIcon) v, e -> c.accept(v));
            } else if (v instanceof FHTextIcon) {
                IconEditor.TEXT_EDITOR.open(p, l, (FHTextIcon) v, c::accept);
            } else if (v instanceof FHDelegateIcon) {
                IconEditor.INTERNAL_EDITOR.open(p, l, (FHDelegateIcon) v, c::accept);
            } else
                IconEditor.NOP_CHANGE_EDITOR.open(p, l, v, c);
        };
        public static final Editor<FHItemIcon> ITEM_EDITOR = (p, l, v, c) -> SelectItemStackDialog.EDITOR.open(p, l, v == null ? null : v.stack, e -> c.accept(new FHItemIcon(e)));
        public static final Editor<FHTextureIcon> TEXTURE_EDITOR = (p, l, v, c) -> EditPrompt.TEXT_EDITOR.open(p, l, v == null ? "" : (v.rl == null ? "" : v.rl.toString()),
                e -> c.accept(new FHTextureIcon(new ResourceLocation(e))));
        public static final Editor<FHIngredientIcon> INGREDIENT_EDITOR = (p, l, v, c) -> IngredientEditor.EDITOR_INGREDIENT_EXTERN.open(p, l, v == null ? null : v.igd,
                e -> c.accept(new FHIngredientIcon(e)));

        public static final Editor<FHIcon> INGREDIENT_SIZE_EDITOR = (p, l, v, c) -> IngredientEditor.EDITOR.open(p, l, null, e -> c.accept(FHIcons.getIcon(e)));
        public static final Editor<FHTextIcon> TEXT_EDITOR = (p, l, v, c) -> EditPrompt.TEXT_EDITOR.open(p, l, v == null ? null : v.text, e -> c.accept(new FHTextIcon(e)));
        public static final Editor<FHIcon> NOP_EDITOR = (p, l, v, c) -> {
            c.accept(FHNopIcon.INSTANCE);
            p.getGui().refreshWidgets();
        };
        public static final Editor<FHIcon> NOP_CHANGE_EDITOR = (p, l, v, c) -> EDITOR.open(p, l, null, c);
        public static final Editor<FHAnimatedIcon> ANIMATED_EDITOR = (p, l, v, c) -> new EditListDialog<>(p, l, v == null ? null : v.icons, null, EDITOR, e -> e.getClass().getSimpleName(),
                e -> e, e -> c.accept(new FHAnimatedIcon(e.toArray(new FHIcon[0])))).open();
        public static final Editor<FHCombinedIcon> COMBINED_EDITOR = (p, l, v, c) -> new Combined(p, l, v, c).open();

        public static final Editor<FHDelegateIcon> INTERNAL_EDITOR = (p, l, v, c) -> new SelectDialog<>(p, l, v == null ? null : v.name, o -> c.accept(new FHDelegateIcon(o)), TechIcons.internals::keySet, TranslateUtils::str, e -> new String[]{e}, TechIcons.internals::get).open();

        public static final Editor<FHTextureUVIcon> UV_EDITOR = (p, l, v, c) -> new UV(p, l, v, c).open();

        T v;

        public IconEditor(Widget panel, T v) {
            super(panel);
            this.v = v;
        }

        @Override
        public void draw(GuiGraphics arg0, Theme arg1, int arg2, int arg3, int arg4, int arg5) {
            super.draw(arg0, arg1, arg2, arg3, arg4, arg5);
            v.draw(arg0, arg2 + 300, arg3 + 20, 32, 32);
        }

    }



    static {
        serializers.register(FHNopIcon.class, "nop", FHNopIcon.CODEC);
        serializers.register(FHItemIcon.class, "item", FHItemIcon.CODEC);
        serializers.register(FHCombinedIcon.class, "combined", FHCombinedIcon.CODEC);
        serializers.register(FHAnimatedIcon.class, "animated", FHAnimatedIcon.CODEC);
        serializers.register(FHIngredientIcon.class, "ingredient", FHIngredientIcon.CODEC);
        serializers.register(FHTextureIcon.class, "texture", FHTextureIcon.CODEC);
        serializers.register(FHTextureUVIcon.class, "texture_uv", FHTextureUVIcon.CODEC);
        serializers.register(FHTextIcon.class, "text", FHTextIcon.CODEC);
        serializers.register(FHDelegateIcon.class, "internal", FHDelegateIcon.CODEC);
    }

    public static FHIcon getAnimatedIcon(FHIcon... icons) {
        return new FHAnimatedIcon(icons);
    }

    /**
     * Make a FHIcon delegate of the given icon, THIS IS NOT SERIALIZABLE
     * All Serialization progress would result in getting an NOP icon.
     */
    public static FHIcon getDelegateIcon(String name) {
        return new FHDelegateIcon(name);
    }

    public static FHIcon getIcon(Collection<ItemLike> items) {
        return new FHIngredientIcon(Ingredient.of(items.toArray(new ItemLike[0])));
    }

    public static FHIcon getIcon(FHIcon base, FHIcon small) {
        return new FHCombinedIcon(base, small);
    }

    public static FHIcon getIcon(ItemLike item) {
        return new FHItemIcon(item);
    }

    public static FHIcon getIcon(ItemLike[] items) {
        return new FHIngredientIcon(Ingredient.of(items));
    }

    public static FHIcon getIcon(Ingredient i) {
        return new FHIngredientIcon(i);
    }

    public static FHIcon getIcon(IngredientWithSize i) {
        if (i.getCount() == 1)
            return getIcon(i.getBaseIngredient());
        if (i.getCount() < 10)
            return getIcon(getIcon(i.getBaseIngredient()), getIcon(" " + i.getCount()));
        return getIcon(getIcon(i.getBaseIngredient()), getIcon(String.valueOf(i.getCount())));
    }

    public static FHIcon getIcon(ItemStack item) {
        return new FHItemIcon(item);
    }

    public static FHIcon getIcon(ItemStack[] stacks) {
        FHIcon[] icons = new FHIcon[stacks.length];
        for (int i = 0; i < stacks.length; i++)
            icons[i] = FHIcons.getIcon(stacks[i]);
        return getAnimatedIcon(icons);
    }


    public static FHIcon getIcon(ResourceLocation texture) {
        return new FHTextureIcon(texture);
    }

    public static FHIcon getIcon(ResourceLocation texture, int x, int y, int w, int h, int tw, int th) {
        return new FHTextureUVIcon(texture, x, y, w, h, tw, th);
    }

    public static FHIcon getIcon(String text) {
        return new FHTextIcon(text);
    }

    /**
     * This does not preserve nbt on save
     */
    public static FHIcon getStackIcons(Collection<ItemStack> rewards) {
        return new FHIngredientIcon(Ingredient.of(rewards.stream()));
    }

    public static FHIcon nop() {
        return FHNopIcon.INSTANCE;
    }
}
