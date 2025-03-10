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

package com.teammoeg.chorda.client.icon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.AlternativeCodecBuilder;
import com.teammoeg.chorda.io.registry.TypedCodecRegistry;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A uniform icon drawing/serializing network
 */
public class CIcons {
	public static final Map<String, CIcon> internals = new HashMap<>();
	private static final TypedCodecRegistry<CIcon> serializers = new TypedCodecRegistry<>();
	public static final Codec<CIcon> CODEC = new AlternativeCodecBuilder<CIcon>(CIcon.class)
		.addSaveOnly(NopIcon.class, NopIcon.CODEC.codec())
		.add(serializers.codec())
		.add(ItemIcon.class, ItemIcon.ICON_CODEC)
		.addSaveOnly(CIcon.class, NopIcon.CODEC.codec())
		.build();

	static {
		serializers.register(NopIcon.class, "nop", NopIcon.CODEC);
		serializers.register(ItemIcon.class, "item", ItemIcon.CODEC);
		serializers.register(CombinedIcon.class, "combined", CombinedIcon.CODEC);
		serializers.register(AnimatedIcon.class, "animated", AnimatedIcon.CODEC);
		serializers.register(IngredientIcon.class, "ingredient", IngredientIcon.CODEC);
		serializers.register(TextureIcon.class, "texture", TextureIcon.CODEC);
		serializers.register(TextureUVIcon.class, "texture_uv", TextureUVIcon.CODEC);
		serializers.register(TextIcon.class, "text", TextIcon.CODEC);
		serializers.register(FHDelegateIcon.class, "internal", FHDelegateIcon.CODEC);
	}

	public static CIcon getAnimatedIcon(CIcon... icons) {
		return new AnimatedIcon(icons);
	}

	/**
	 * Make a FHIcon delegate of the given icon, THIS IS NOT SERIALIZABLE All
	 * Serialization progress would result in getting an NOP icon.
	 */
	public static CIcon getDelegateIcon(String name) {
		return new FHDelegateIcon(name);
	}

	/**
	 * get icon switching between items
	 */
	public static CIcon getIcon(Collection<? extends ItemLike> items) {
		return new IngredientIcon(Ingredient.of(items.toArray(new ItemLike[0])));
	}

	/**
	 * get icon with a small icon on the bottom-right
	 */
	public static CIcon getIcon(CIcon base, CIcon small) {
		return new CombinedIcon(base, small);
	}

	/**
	 * get icon with item
	 */
	public static CIcon getIcon(ItemLike item) {
		return new ItemIcon(item);
	}

	/**
	 * get icon with a list of item
	 */
	public static CIcon getIcon(ItemLike[] items) {
		return new IngredientIcon(Ingredient.of(items));
	}

	/**
	 * get icon switching between valid stack of ingredient
	 */
	public static CIcon getIcon(Ingredient i) {
		return getIcon(i, 1);
	}

	/**
	 * get icon switching between valid stack of ingredient and with count display
	 */
	public static CIcon getIcon(Ingredient i, int count) {
		return new IngredientIcon(i, count);
	}

	/**
	 * get icon showing an item stack
	 */
	public static CIcon getIcon(ItemStack item) {
		return new ItemIcon(item);
	}

	/**
	 * get icon showing a list of item stack
	 */
	public static CIcon getIcon(ItemStack[] stacks) {
		CIcon[] icons = new CIcon[stacks.length];
		for (int i = 0; i < stacks.length; i++)
			icons[i] = CIcons.getIcon(stacks[i]);
		return getAnimatedIcon(icons);
	}

	/**
	 * get icon showing a texture
	 */
	public static CTextureIcon getIcon(ResourceLocation texture) {
		return new TextureIcon(texture);
	}

	/**
	 * get icon showing a part of the texture
	 */
	public static CTextureIcon getIcon(ResourceLocation texture, int x, int y, int w, int h, int tw, int th) {
		return new TextureUVIcon(texture, x, y, w, h, tw, th);
	}

	/**
	 * get icon showing a string text
	 */
	public static CIcon getIcon(String text) {
		return new TextIcon(Components.str(text));
	}

	/**
	 * get icon switching between stacks
	 */
	public static CIcon getStackIcons(Collection<ItemStack> rewards) {
		return new IngredientIcon(Ingredient.of(rewards.stream()));
	}

	/**
	 * get icon showing nothing
	 */
	public static CIcon nop() {
		return NopIcon.INSTANCE;
	}

	/**
	 * get icon showing a text component
	 */
	public static CIcon getIcon(Component text) {
		return new TextIcon(text);
	}

	static class AnimatedIcon extends CIcon {
		private static final MapCodec<AnimatedIcon> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
			Codec.list(CIcons.CODEC).fieldOf("icons").forGetter(o -> o.icons)).apply(t, AnimatedIcon::new));
		private static final Codec<AnimatedIcon> ICON_CODEC = Codec.list(CIcons.CODEC).xmap(AnimatedIcon::new, o -> o.icons);
		List<CIcon> icons;

		public AnimatedIcon() {
			icons = new ArrayList<>();
		}

		public AnimatedIcon(List<CIcon> icons) {
			super();
			this.icons = new ArrayList<>(icons);
		}

		public AnimatedIcon(CIcon... icons2) {
			this();
			icons.addAll(Arrays.asList(icons2));
		}

		@Override
		public void draw(GuiGraphics ms, int x, int y, int w, int h) {
			if (!icons.isEmpty()) {
				CMath.selectElementByTime(icons).draw(ms, x, y, w, h);
			}
		}

	}

	static class CombinedIcon extends CIcon {
		private static final MapCodec<CombinedIcon> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
			CIcons.CODEC.optionalFieldOf("base", NopIcon.INSTANCE).forGetter(o -> o.large),
			CIcons.CODEC.optionalFieldOf("small", NopIcon.INSTANCE).forGetter(o -> o.small)).apply(t, CombinedIcon::new));
		CIcon large;
		CIcon small;

		public CombinedIcon(CIcon base, CIcon small) {
			this.large = base;
			this.small = small;
		}

		@Override
		public void draw(GuiGraphics ms, int x, int y, int w, int h) {
			if (large != null)
				large.draw(ms, x, y, w, h);
			ms.pose().pushPose();
			ms.pose().translate(0, 0, 110);// let's get top most
			if (small != null)
				small.draw(ms, x + w / 2, y + h / 2, w / 2, h / 2);
			ms.pose().popPose();
		}
	}

	static class FHDelegateIcon extends CIcon {
		private static final MapCodec<FHDelegateIcon> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
			Codec.STRING.fieldOf("name").forGetter(o -> o.name)).apply(t, FHDelegateIcon::new));
		String name;

		public FHDelegateIcon(String name) {
			super();
			this.name = name;
		}

		@Override
		public void draw(GuiGraphics ms, int x, int y, int w, int h) {
			CGuiHelper.resetGuiDrawing();
			internals.get(name).draw(ms, x, y, w, h);
		}

	}

	public static abstract class CIcon implements Cloneable {

		public CIcon() {
			super();
		}

		@Override
		public CIcon clone() {
			try {
				return (CIcon) super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return CIcons.nop();
		}

		public abstract void draw(GuiGraphics ms, int x, int y, int w, int h);
	}

	public static abstract class CTextureIcon extends CIcon {
		public abstract CTextureIcon withUV(int x, int y, int w, int h, int tw, int th);

		public abstract CTextureIcon toNineSlice(int c);

		public abstract CTextureIcon asPart(int x, int y, int w, int h);
	}

	static class IngredientIcon extends AnimatedIcon {
		private static final MapCodec<IngredientIcon> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
			CodecUtil.INGREDIENT_CODEC.fieldOf("ingredient").forGetter(o -> o.igd),
			Codec.INT.optionalFieldOf("count", 1).forGetter(o -> o.count)).apply(t, IngredientIcon::new));
		Ingredient igd;
		int count;

		public IngredientIcon(Ingredient i) {
			igd = i;
			for (ItemStack stack : igd.getItems())
				icons.add(new ItemIcon(stack));
		}

		public IngredientIcon(Ingredient i, int count) {
			igd = i;
			this.count = count;
			for (ItemStack stack : igd.getItems()) {
				if (count != 1)
					stack = stack.copyWithCount(count);
				icons.add(new ItemIcon(stack));
			}

		}

		public IngredientIcon(JsonElement elm) {
			this(Ingredient.fromJson(elm.getAsJsonObject().get("ingredient")));
		}
	}

	public static class ItemIcon extends CIcon {
		private static final MapCodec<ItemIcon> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
			ItemStack.CODEC.fieldOf("item").forGetter(o -> o.stack)).apply(t, ItemIcon::new));
		private static final Codec<ItemIcon> ICON_CODEC = ForgeRegistries.ITEMS.getCodec().xmap(ItemIcon::new, t -> t.stack.getItem());
		public final ItemStack stack;

		public ItemIcon(ItemLike item2) {
			this(new ItemStack(item2));
		}

		public ItemIcon(ItemStack stack) {
			this.stack = stack;
		}

		@Override
		public void draw(GuiGraphics matrixStack, int x, int y, int w, int h) {
			// ItemRenderer itemRenderer=ClientUtils.mc().getItemRenderer();
			/*
			 * itemRenderer.zLevel = 200.0F; net.minecraft.client.gui.FontRenderer font =
			 * stack.getItem().getFontRenderer(stack); if (font == null) font =
			 * ClientUtils.mc().fontRenderer; itemRenderer.renderItemAndEffectIntoGUI(stack,
			 * x, y); itemRenderer.renderItemOverlayIntoGUI(font, stack, x, y, null);
			 * itemRenderer.zLevel = 0.0F;
			 */
			CGuiHelper.drawItem(matrixStack, stack, x, y, 50, w / 16f, h / 16f, true, null);
			/*
			 * ClientUtils.mc().getItemRenderer().renderItem(stack,
			 * TransformType.GUI,LightTexture., y, matrixStack, null); if (stack != null &&
			 * stack.getCount() > 1) { matrixStack.push(); matrixStack.translate(x + w - 8,
			 * y + h - 7, 199); matrixStack.push(); matrixStack.scale(w / 16f, h / 16f, 0);
			 * ClientUtils.mc().fontRenderer.drawStringWithShadow(matrixStack,
			 * String.valueOf(stack.getCount()), 0, 0, 0xffffffff); matrixStack.pop();
			 * matrixStack.pop(); }
			 */
		}
	}

	static class NopIcon extends CIcon {

		public static final NopIcon INSTANCE = new NopIcon();
		private static final MapCodec<NopIcon> CODEC = MapCodec.unit(INSTANCE);

		private NopIcon() {
		}

		@Override
		public void draw(GuiGraphics ms, int x, int y, int w, int h) {
		}

	}

	static class TextIcon extends CIcon {
		private static final MapCodec<TextIcon> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
			CodecUtil.COMPONENT_CODEC.fieldOf("text").forGetter(o -> o.text)).apply(t, TextIcon::new));
		Component text;

		public TextIcon(Component text) {
			super();
			this.text = text;
		}

		@Override
		public void draw(GuiGraphics ms, int x, int y, int w, int h) {

			ms.pose().pushPose();
			ms.pose().translate(x, y, 0);
			ms.pose().scale(w / 16f, h / 16f, 0);

			ms.pose().pushPose();

			ms.pose().scale(2f, 2f, 0);// scale font height 8 to height 16
			ms.drawString(ClientUtils.mc().font, text, 0, 0, 0xFFFFFFFF,false);
			ms.pose().popPose();
			ms.pose().popPose();
			CGuiHelper.resetGuiDrawing();
		}

	}

	static class TextureIcon extends CTextureIcon {
		private static final MapCodec<TextureIcon> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
			ResourceLocation.CODEC.fieldOf("location").forGetter(o -> o.rl)).apply(t, TextureIcon::new));
		ResourceLocation rl;

		public TextureIcon(ResourceLocation rl) {
			this.rl = rl;
		}

		@Override
		public void draw(GuiGraphics ms, int x, int y, int w, int h) {
			CGuiHelper.resetGuiDrawing();
			ms.blit(rl, x, y, 0, 0, w, h, w, h);
		}

		public CTextureIcon withUV(int x, int y, int w, int h, int tw, int th) {
			return new TextureUVIcon(rl, x, y, w, h, tw, th);
		}

		@Override
		public CTextureIcon toNineSlice(int c) {
			return new NineSliceIcon(rl, 0, 0, 256, 256, c, 256, 256);
		}

		@Override
		public CTextureIcon asPart(int x, int y, int w, int h) {
			return new TextureUVIcon(rl, x, y, w, h, 256, 256);
		}

	}

	static class TextureUVIcon extends TextureIcon {
		private static final MapCodec<TextureUVIcon> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
			ResourceLocation.CODEC.fieldOf("location").forGetter(o -> o.rl),
			Codec.INT.fieldOf("x").forGetter(o -> o.x),
			Codec.INT.fieldOf("y").forGetter(o -> o.y),
			Codec.INT.fieldOf("w").forGetter(o -> o.w),
			Codec.INT.fieldOf("h").forGetter(o -> o.h),
			Codec.INT.fieldOf("tw").forGetter(o -> o.tw),
			Codec.INT.fieldOf("th").forGetter(o -> o.th)).apply(t, TextureUVIcon::new));
		ResourceLocation rl;
		protected int x, y, w, h, tw = 256, th = 256;

		public TextureUVIcon() {
			super(FHMain.rl("texture/gui"));
		}

		public TextureUVIcon(ResourceLocation rl, int x, int y, int w, int h, int tw, int th) {
			super(rl);
			this.rl = rl;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.tw = tw;
			this.th = th;
		}

		@Override
		public void draw(GuiGraphics ms, int x, int y, int w, int h) {
			CGuiHelper.resetGuiDrawing();
			ms.blit(rl, x, y, w, h,  this.x, this.y,this.w, this.h, this.tw, this.th);
		}

		@Override
		public CTextureIcon toNineSlice(int c) {
			return new NineSliceIcon(rl, x, y, w, h, c, tw, th);
		}

		@Override
		public CTextureIcon asPart(int x, int y, int w, int h) {
			return new TextureUVIcon(rl, this.x + x, this.y + y, Math.min(w, this.w - x), Math.min(h, this.h - y), tw, th);
		}
	}

	static class NineSliceIcon extends TextureUVIcon {
		private static final MapCodec<NineSliceIcon> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
			ResourceLocation.CODEC.fieldOf("location").forGetter(o -> o.rl),
			Codec.INT.fieldOf("x").forGetter(o -> o.x),
			Codec.INT.fieldOf("y").forGetter(o -> o.y),
			Codec.INT.fieldOf("w").forGetter(o -> o.w),
			Codec.INT.fieldOf("h").forGetter(o -> o.h),
			Codec.INT.fieldOf("c").forGetter(o -> o.corner),
			Codec.INT.fieldOf("tw").forGetter(o -> o.tw),
			Codec.INT.fieldOf("th").forGetter(o -> o.th)).apply(t, NineSliceIcon::new));
		ResourceLocation rl;
		int corner;

		public NineSliceIcon(ResourceLocation rl, int x, int y, int w, int h, int corner, int tw, int th) {
			super(rl, x, y, w, h, tw, th);
			this.rl = rl;
			this.corner = corner;

		}

		@Override
		public void draw(GuiGraphics ms, int x, int y, int w, int h) {
			CGuiHelper.resetGuiDrawing();
			CGuiHelper.blitNineSliced(ms, rl, x, y, w, h, corner, this.w, this.h, this.x, this.y, this.tw, this.th);
		}

		public CTextureIcon withUV(int x, int y, int w, int h, int tw, int th) {
			return new NineSliceIcon(rl, x, y, w, h, corner, tw, th);
		}

		public CTextureIcon asPart(int x, int y, int w, int h) {
			return new NineSliceIcon(rl, this.x + x, this.y + y, Math.min(w, this.w), Math.min(h, this.h), corner, tw, th);
		}
	}

}
