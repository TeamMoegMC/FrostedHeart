/*
 * Copyright (c) 2026 TeamMoeg
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
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.AlternativeCodecBuilder;
import com.teammoeg.chorda.io.registry.TypedCodecRegistry;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedresearch.gui.TechIcons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 统一的图标绘制与序列化网络。提供各种类型图标的创建工厂方法和编解码器注册。
 * <p>
 * A uniform icon drawing and serialization network. Provides factory methods for creating various icon types and codec registration.
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

	/**
	 * 创建一个在多个图标间循环切换的动画图标。
	 * <p>
	 * Creates an animated icon that cycles through multiple icons.
	 *
	 * @param icons 要循环显示的图标数组 / the array of icons to cycle through
	 * @return 动画图标实例 / the animated icon instance
	 */
	public static CIcon getAnimatedIcon(CIcon... icons) {
		return new AnimatedIcon(icons);
	}

	/**
	 * 创建给定名称的委托图标，此图标不可序列化，序列化将得到空图标。
	 * <p>
	 * Creates a delegate icon by name. THIS IS NOT SERIALIZABLE; all serialization will result in a NOP icon.
	 *
	 * @param name 内部注册的图标名称 / the internally registered icon name
	 * @return 委托图标实例 / the delegate icon instance
	 */
	public static CIcon getDelegateIcon(String name) {
		return new FHDelegateIcon(name);
	}

	/**
	 * 获取在多个物品间循环切换的图标。
	 * <p>
	 * Gets an icon that switches between the given items.
	 *
	 * @param items 物品集合 / the collection of items
	 * @return 循环切换的图标 / the cycling icon
	 */
	public static CIcon getIcon(Collection<? extends ItemLike> items) {
		return new IngredientIcon(Ingredient.of(items.toArray(new ItemLike[0])));
	}

	/**
	 * 获取一个组合图标，右下角叠加一个小图标。
	 * <p>
	 * Gets an icon with a small icon overlaid on the bottom-right corner.
	 *
	 * @param base 基础图标 / the base icon
	 * @param small 右下角的小图标 / the small icon on the bottom-right
	 * @return 组合图标实例 / the combined icon instance
	 */
	public static CIcon getIcon(CIcon base, CIcon small) {
		return new CombinedIcon(base, small);
	}

	/**
	 * 获取显示单个物品的图标。
	 * <p>
	 * Gets an icon displaying a single item.
	 *
	 * @param item 要显示的物品 / the item to display
	 * @return 物品图标 / the item icon
	 */
	public static CIcon getIcon(ItemLike item) {
		return new ItemIcon(item);
	}

	/**
	 * 获取在物品列表间循环切换的图标。
	 * <p>
	 * Gets an icon that switches between an array of items.
	 *
	 * @param items 物品数组 / the array of items
	 * @return 循环切换的图标 / the cycling icon
	 */
	public static CIcon getIcon(ItemLike[] items) {
		return new IngredientIcon(Ingredient.of(items));
	}

	/**
	 * 获取在合成材料的有效物品栈间循环切换的图标。
	 * <p>
	 * Gets an icon that switches between valid stacks of an ingredient.
	 *
	 * @param i 合成材料 / the ingredient
	 * @return 循环切换的图标 / the cycling icon
	 */
	public static CIcon getIcon(Ingredient i) {
		return getIcon(i, 1);
	}

	/**
	 * 获取在合成材料的有效物品栈间循环切换并显示数量的图标。
	 * <p>
	 * Gets an icon that switches between valid stacks of an ingredient with count display.
	 *
	 * @param i 合成材料 / the ingredient
	 * @param count 显示的数量 / the count to display
	 * @return 带数量显示的循环切换图标 / the cycling icon with count display
	 */
	public static CIcon getIcon(Ingredient i, int count) {
		return new IngredientIcon(i, count);
	}

	/**
	 * 获取显示物品栈的图标。
	 * <p>
	 * Gets an icon showing an item stack.
	 *
	 * @param item 要显示的物品栈 / the item stack to display
	 * @return 物品栈图标 / the item stack icon
	 */
	public static CIcon getIcon(ItemStack item) {
		return new ItemIcon(item);
	}

	/**
	 * 获取在物品栈列表间循环切换的动画图标。
	 * <p>
	 * Gets an animated icon showing a list of item stacks.
	 *
	 * @param stacks 物品栈数组 / the array of item stacks
	 * @return 动画图标 / the animated icon
	 */
	public static CIcon getIcon(ItemStack[] stacks) {
		CIcon[] icons = new CIcon[stacks.length];
		for (int i = 0; i < stacks.length; i++)
			icons[i] = CIcons.getIcon(stacks[i]);
		return getAnimatedIcon(icons);
	}

	/**
	 * 获取显示纹理的图标。
	 * <p>
	 * Gets an icon showing a texture.
	 *
	 * @param texture 纹理资源位置 / the texture resource location
	 * @return 纹理图标 / the texture icon
	 */
	public static CTextureIcon getIcon(ResourceLocation texture) {
		return new TextureIcon(texture);
	}

	/**
	 * 获取显示纹理局部区域的图标。
	 * <p>
	 * Gets an icon showing a part of a texture with UV coordinates.
	 *
	 * @param texture 纹理资源位置 / the texture resource location
	 * @param x 纹理U偏移 / the texture U offset
	 * @param y 纹理V偏移 / the texture V offset
	 * @param w 采样宽度 / the sample width
	 * @param h 采样高度 / the sample height
	 * @param tw 纹理总宽度 / the total texture width
	 * @param th 纹理总高度 / the total texture height
	 * @return 带UV的纹理图标 / the texture icon with UV
	 */
	public static CTextureIcon getIcon(ResourceLocation texture, int x, int y, int w, int h, int tw, int th) {
		return new TextureUVIcon(texture, x, y, w, h, tw, th);
	}

	/**
	 * 获取显示文本字符串的图标。
	 * <p>
	 * Gets an icon showing a string text.
	 *
	 * @param text 要显示的文本 / the text to display
	 * @return 文本图标 / the text icon
	 */
	public static CIcon getIcon(String text) {
		return new TextIcon(Components.str(text));
	}

	/**
	 * 获取在物品栈集合间循环切换的图标。
	 * <p>
	 * Gets an icon that switches between the given item stacks.
	 *
	 * @param rewards 物品栈集合 / the collection of item stacks
	 * @return 循环切换的图标 / the cycling icon
	 */
	public static CIcon getStackIcons(Collection<ItemStack> rewards) {
		return new IngredientIcon(Ingredient.of(rewards.stream()));
	}

	/**
	 * 获取不显示任何内容的空图标。
	 * <p>
	 * Gets an icon that shows nothing.
	 *
	 * @return 空图标实例 / the empty icon instance
	 */
	public static CIcon nop() {
		return NopIcon.INSTANCE;
	}
	/**
	 * 获取空图标，与 {@link #nop()} 相同。
	 * <p>
	 * Gets an empty icon, same as {@link #nop()}.
	 *
	 * @return 空图标实例 / the empty icon instance
	 */
	public static CIcon empty() {
		return NopIcon.INSTANCE;
	}

	/**
	 * 获取显示文本组件的图标。
	 * <p>
	 * Gets an icon showing a text component.
	 *
	 * @param text 要显示的文本组件 / the text component to display
	 * @return 文本图标 / the text icon
	 */
	public static CIcon getIcon(Component text) {
		return new TextIcon(text);
	}

	/**
	 * 根据时间在多个图标间循环切换的动画图标。
	 * <p>
	 * An animated icon that cycles through multiple icons based on time.
	 */
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
		@Override
		public boolean isEmpty() {
			return icons.isEmpty()&&icons.stream().allMatch(CIcon::isEmpty);
		}
	}

	/**
	 * 组合图标，将一个大图标和一个小图标叠加在一起显示，小图标位于右下角。
	 * <p>
	 * A combined icon that overlays a large icon with a small icon at the bottom-right corner.
	 */
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
			ms.pose().translate(0, 0, 300);// let's get top most
			if (small != null)
				small.draw(ms, x + w / 2, y + h / 2, w / 2, h / 2);
			ms.pose().popPose();
		}
		@Override
		public boolean isEmpty() {
			return large.isEmpty()&&small.isEmpty();
		}
	}

	/**
	 * 委托图标，通过名称引用内部注册的图标。未找到时显示问号图标。
	 * <p>
	 * A delegate icon that references internally registered icons by name. Shows a question mark if not found.
	 */
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
			CIcon icon = internals.get(name);
			if (icon != null) {
				icon.draw(ms, x, y, w, h);
			} else {
				FHMain.LOGGER.warn("No icon found for name " + name, ", using default.");
				TechIcons.Question.draw(ms, x, y, w, h);
			}
		}

		@Override
		public boolean isEmpty() {
			return internals.get(name)==null;
		}
	}

	/**
	 * 图标的抽象基类。所有图标类型都必须实现绘制和空检查方法。
	 * <p>
	 * Abstract base class for all icons. All icon types must implement draw and isEmpty methods.
	 */
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
		public abstract boolean isEmpty();
	}

	/**
	 * 纹理图标的抽象基类。提供UV裁切、九宫格拉伸和子区域提取功能。
	 * <p>
	 * Abstract base class for texture icons. Provides UV slicing, nine-slice scaling, and sub-region extraction.
	 */
	public static abstract class CTextureIcon extends CIcon {
		public abstract CTextureIcon withUV(int x, int y, int w, int h, int tw, int th);

		public abstract CTextureIcon toNineSlice(int c);

		public abstract CTextureIcon asPart(int x, int y, int w, int h);

		@Override
		public boolean isEmpty() {
			return false;
		}
		
	}

	/**
	 * 合成材料图标，在合成材料的所有有效物品栈间循环显示。
	 * <p>
	 * An ingredient icon that cycles through all valid item stacks of an ingredient.
	 */
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
		@Override
		public boolean isEmpty() {
			return igd.isEmpty();
		}
	}

	/**
	 * 物品图标，在GUI中渲染一个物品栈。
	 * <p>
	 * An item icon that renders an item stack in the GUI.
	 */
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
			 * String.valueOf(stack.getCount()), 0, 0, getTheme().getButtonTextColor()); matrixStack.pop();
			 * matrixStack.pop(); }
			 */
		}
		@Override
		public boolean isEmpty() {
			return stack.isEmpty();
		}
	}

	/**
	 * 空图标，不绘制任何内容。作为单例使用。
	 * <p>
	 * A no-operation icon that draws nothing. Used as a singleton.
	 */
	static class NopIcon extends CIcon {

		public static final NopIcon INSTANCE = new NopIcon();
		private static final MapCodec<NopIcon> CODEC = MapCodec.unit(INSTANCE);

		private NopIcon() {
		}

		@Override
		public void draw(GuiGraphics ms, int x, int y, int w, int h) {
		}
		@Override
		public boolean isEmpty() {
			return true;
		}
	}

	/**
	 * 文本图标，在GUI中渲染文本组件。
	 * <p>
	 * A text icon that renders a text component in the GUI.
	 */
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
			ms.drawString(ClientUtils.getMc().font, text, 0, 0, 0xFFFFFFFF,false);
			ms.pose().popPose();
			ms.pose().popPose();
			CGuiHelper.resetGuiDrawing();
		}
		@Override
		public boolean isEmpty() {
			return false;
		}
	}

	/**
	 * 纹理图标，绘制完整的纹理图片。
	 * <p>
	 * A texture icon that draws a complete texture image.
	 */
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

	/**
	 * 带UV坐标的纹理图标，可以绘制纹理的指定区域。
	 * <p>
	 * A texture icon with UV coordinates that can draw a specified region of a texture.
	 */
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
			super(Chorda.rl("texture/gui"));
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

	/**
	 * 九宫格图标，支持按九宫格方式拉伸纹理，常用于可变大小的UI背景。
	 * <p>
	 * A nine-slice icon that stretches a texture using nine-slice scaling, commonly used for resizable UI backgrounds.
	 */
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
