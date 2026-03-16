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

package com.teammoeg.chorda.text;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Strings;

/**
 * 本地化文本组件的构建器，支持链式调用来组合翻译文本、字面文本和数字格式。
 * <p>
 * A builder for creating localised text components, supporting chained calls to compose
 * translatable text, literal text, and number formatting.
 *
 * <p>用法示例 / Usage example: {@code Lang.builder().translate("prefix", "suffix", args).component()}
 * <p>仅在链式调用末尾使用 .component() / Only use .component() at end of chain
 */
public class LangBuilder {
    String namespace;
    LinkedList<MutableComponent> components=new LinkedList<>();
    Style mainStyle=Style.EMPTY;
    NumberFormat currentNumberFormat=CFormatHelper.getNumberFormats().decimal2digit;
    /**
     * 使用指定命名空间创建LangBuilder。
     * <p>
     * Creates a LangBuilder with the specified namespace.
     *
     * @param namespace 用于翻译键前缀的命名空间 / The namespace used as prefix for translation keys
     */
    public LangBuilder(String namespace) {
        this.namespace = namespace;
    }

    /**
     * 解析参数数组中的LangBuilder实例，将其转换为组件。
     * <p>
     * Resolves LangBuilder instances in the arguments array, converting them to components.
     *
     * @param args 可能包含LangBuilder的参数数组 / The arguments array that may contain LangBuilder instances
     * @return 解析后的参数数组 / The resolved arguments array
     */
    public static Object[] resolveBuilders(Object[] args) {
        for (int i = 0; i < args.length; i++)
            if (args[i]instanceof LangBuilder cb)
                args[i] = cb.component();
        return args;
    }

    /**
     * 追加一个空格字符。
     * <p>
     * Appends a space character.
     *
     * @return 此构建器 / This builder
     */
    public LangBuilder space() {
        return text(" ");
    }

    /**
     * 追加一个换行符。
     * <p>
     * Appends a newline character.
     *
     * @return 此构建器 / This builder
     */
    public LangBuilder newLine() {
        return text("\n");
    }

    /**
     * 追加一个本地化组件，使用未经处理的原始翻译键。
     * <p>
     * Appends a localised component using a raw, unprocessed translation key.
     *
     * @param langKey 未经处理的翻译键 / The raw translation key, not processed
     * @param args 翻译参数 / The translation arguments
     * @return 此构建器 / This builder
     */
    public LangBuilder key(String langKey, Object... args) {
        return add(Components.translatable(langKey, resolveBuilders(args)));
    }

    /**
     * 追加一个以命名空间为前缀的本地化组件。如需添加独立格式的本地化组件，请使用add()和嵌套构建器。
     * <p>
     * Appends a localised component with the namespace as prefix.
     * To add an independently formatted localised component, use add() and a nested builder.
     *
     * @param suffix 命名空间的后缀 / A suffix to the namespace
     * @param args 翻译参数 / The translation arguments
     * @return 此构建器 / This builder
     */
    public LangBuilder suffix(String suffix, Object... args) {
        return add(Components.translatable( namespace + "." + suffix, resolveBuilders(args)));
    }

    /**
     * 追加一个以命名空间为后缀的本地化组件。如需添加独立格式的本地化组件，请使用add()和嵌套构建器。
     * <p>
     * Appends a localised component with the namespace as suffix.
     * To add an independently formatted localised component, use add() and a nested builder.
     *
     * @param prefix 命名空间的前缀 / A prefix to the namespace
     * @param args 翻译参数 / The translation arguments
     * @return 此构建器 / This builder
     */
    public LangBuilder prefix(String prefix, Object... args) {
        return add(Components.translatable( prefix + "." + namespace, resolveBuilders(args)));
    }


    /**
     * 追加一个带前缀和后缀的本地化组件。
     * 例如：translate("tooltip", "temp_change", args) 生成键 "tooltip.namespace.temp_change"。
     * <p>
     * Appends a localised component with a prefix and suffix.
     * Example: translate("tooltip", "temp_change", args) gives "tooltip.namespace.temp_change" as key.
     *
     * @param prefix 命名空间的前缀，通常为类别 / A prefix to the namespace, normally a category
     * @param suffix 命名空间的后缀，通常为描述 / A suffix to the namespace, normally a description
     * @param args 翻译参数 / The translation arguments
     * @return 此构建器 / This builder
     */
    public LangBuilder translate(String prefix, String suffix, Object... args) {
        return add(Components.translatable( prefix + "." + namespace + "." + suffix, resolveBuilders(args)));
    }

    /**
     * 追加一个字面文本组件。
     * <p>
     * Appends a literal text component.
     *
     * @param literalText 字面文本 / The literal text
     * @return 此构建器 / This builder
     */
    public LangBuilder text(String literalText) {
        return add(Components.literal(literalText));
    }

    /**
     * 追加一个带聊天格式的彩色文本组件。
     * <p>
     * Appends a colored text component with chat formatting.
     *
     * @param format 聊天格式 / The chat formatting
     * @param literalText 字面文本 / The literal text
     * @return 此构建器 / This builder
     */
    public LangBuilder text(ChatFormatting format, String literalText) {
        return add(Components.literal(literalText).withStyle(format));
    }

    /**
     * 追加一个带指定颜色的文本组件。
     * <p>
     * Appends a text component with the specified color.
     *
     * @param color 颜色值 / The color value
     * @param literalText 字面文本 / The literal text
     * @return 此构建器 / This builder
     */
    public LangBuilder text(int color, String literalText) {
        return add(Components.literal(literalText).withStyle(s -> s.withColor(color)));
    }
    /**
     * 设置数字格式化模式。使用#表示可选数字，0表示必须数字。
     * 例如 #,##0.## 表示最少0位小数，最多2位小数，每3位用分隔符。
     * <p>
     * Sets the number format pattern. Use # for optional digits, 0 for mandatory digits.
     * For example, #,##0.## means minimum 0 fraction digits, maximum 2, with separator every 3 digits.
     *
     * @param format 数字格式模式字符串 / The number format pattern string
     * @return 此构建器 / This builder
     * @see java.text.DecimalFormat
     */
    public LangBuilder setNumberFormat(String format) {
    	currentNumberFormat=new DecimalFormat(format);
    	return this;
    }
    /**
     * 使用当前语言环境的数字格式。
     * <p>
     * Uses the number format for the current locale.
     *
     * @return 此构建器 / This builder
     */
    public LangBuilder useLocalNumberFormat() {
    	currentNumberFormat=CFormatHelper.getNumberFormats().decimal2digit;
    	return this;
    }
    /**
     * 将数字格式切换为百分比格式（保留1位小数）。
     * <p>
     * Switches the number format to percentage format (1 decimal place).
     *
     * @return 此构建器 / This builder
     */
    public LangBuilder percentage() {
    	currentNumberFormat=CFormatHelper.getNumberFormats().percentage1digit;
        return this;
    }
    /**
     * 追加一个格式化的长整数文本组件。
     * <p>
     * Appends a formatted long integer text component.
     *
     * @param d 要格式化的长整数 / The long integer to format
     * @return 此构建器 / This builder
     */
    public LangBuilder number(long d) {
        return add(Components.literal(currentNumberFormat.format(d)));
    }
    /**
     * 追加一个格式化的浮点数文本组件。
     * <p>
     * Appends a formatted double text component.
     *
     * @param d 要格式化的浮点数 / The double to format
     * @return 此构建器 / This builder
     */
    public LangBuilder number(double d) {
        return add(Components.literal(currentNumberFormat.format(d)));
    }

    
    /**
     * 追加一个格式化的Number文本组件。
     * <p>
     * Appends a formatted Number text component.
     *
     * @param d 要格式化的Number / The Number to format
     * @return 此构建器 / This builder
     */
    public LangBuilder number(Number d) {
        return add(Components.literal(currentNumberFormat.format(d)));
    }

    /**
     * 追加另一个构建器的内容。
     * <p>
     * Appends the contents of another builder.
     *
     * @param otherBuilder 另一个构建器 / The other builder
     * @return 此构建器 / This builder
     */
    public LangBuilder add(LangBuilder otherBuilder) {
        return add(otherBuilder.component());
    }
    
    /**
     * 追加一个可变组件。
     * <p>
     * Appends a mutable component.
     *
     * @param customComponent 要追加的可变组件 / The mutable component to append
     * @return 此构建器 / This builder
     */
    public LangBuilder add(MutableComponent customComponent) {
    	components.add(customComponent);
        return this;
    }

    /**
     * 追加一个组件（如果是不可变的则自动复制）。
     * <p>
     * Appends a component (auto-copies if immutable).
     *
     * @param component 要追加的组件 / The component to append
     * @return 此构建器 / This builder
     */
    public LangBuilder add(Component component) {
        if (component instanceof MutableComponent mutableComponent)
            return add(mutableComponent);
        else
            return add(component.copy());
    }

    //

    /**
     * 将格式应用于所有已添加的组件。
     * <p>
     * Applies the format to all added components.
     *
     * @param format 聊天格式 / The chat formatting
     * @return 此构建器 / This builder
     */
    public LangBuilder style(ChatFormatting format) {
    	mainStyle = mainStyle.applyFormat(format);
        return this;
    }

    /**
     * 将样式应用于所有已添加的组件。
     * <p>
     * Applies the style to all added components.
     *
     * @param style 要应用的样式 / The style to apply
     * @return 此构建器 / This builder
     */
    public LangBuilder style(Style style) {
    	mainStyle = style.applyTo(mainStyle);
        return this;
    }
    /**
     * 将颜色应用于所有已添加的组件。
     * <p>
     * Applies the color to all added components.
     *
     * @param color 颜色值 / The color value
     * @return 此构建器 / This builder
     */
    public LangBuilder color(int color) {
    	mainStyle = mainStyle.withColor(color);
        return this;
    }

    /**
     * 将格式应用于最后一个组件。
     * <p>
     * Applies the format to the last added component.
     *
     * @param format 聊天格式 / The chat formatting
     * @return 此构建器 / This builder
     */
    public LangBuilder withStyle(ChatFormatting format) {
    	components.addLast(components.pollLast().withStyle(format));
        return this;
    }

    /**
     * 将样式应用于最后一个组件。
     * <p>
     * Applies the style to the last added component.
     *
     * @param style 要应用的样式 / The style to apply
     * @return 此构建器 / This builder
     */
    public LangBuilder withStyle(Style style) {
    	components.addLast(components.pollLast().withStyle(style));
        return this;
    }
    /**
     * 将颜色应用于最后一个组件。
     * <p>
     * Applies the color to the last added component.
     *
     * @param color 颜色值 / The color value
     * @return 此构建器 / This builder
     */
    public LangBuilder withColor(int color) {
    	components.addLast(components.pollLast().withStyle(s -> s.withColor(color)));
        return this;
    }
    //
    /**
     * 构建原始文本组件。
     * <p>
     * Builds the raw text component.
     *
     * @return 构建完成的可变组件 / The built mutable component
     */
    public MutableComponent component() {
    	if(components.isEmpty())
    		return Components.empty();
    	if(components.size()==1)
    		return components.getFirst().withStyle(s->s.applyTo(mainStyle));
    	MutableComponent parent=Components.empty().withStyle(mainStyle);
    	components.forEach(parent::append);
        return parent;
    }
    /**
     * 构建优化后的组件，合并相同内容的组件并翻译文本，仅限客户端使用。
     * <p>
     * Builds an optimized component, merging components with same content and translating texts. Client-side only.
     *
     * @return 优化后的可变组件 / The optimized mutable component
     */
    @OnlyIn(Dist.CLIENT)
    public MutableComponent optimizedComponent() {
    	if(components.isEmpty())
    		return Components.empty();
    	if(components.size()==1)
    		return components.getFirst().withStyle(s->s.applyTo(mainStyle));
    	MutableComponent parent=Components.empty().withStyle(mainStyle);
    	components.forEach(parent::append);
    	return ComponentOptimizer.optimize(parent);
    }

    /**
     * 将构建的组件转换为纯字符串。
     * <p>
     * Converts the built component to a plain string.
     *
     * @return 组件的字符串表示 / The string representation of the component
     */
    public String string() {
        return component().getString();
    }

    /**
     * 将构建的组件序列化为JSON字符串。
     * <p>
     * Serializes the built component to a JSON string.
     *
     * @return 组件的JSON表示 / The JSON representation of the component
     */
    public String json() {
        return Component.Serializer.toJson(component());
    }

    /**
     * 将构建的组件作为状态栏消息发送给玩家。
     * <p>
     * Sends the built component as a status bar message to the player.
     *
     * @param player 目标玩家 / The target player
     */
    public void sendStatus(Player player) {
        player.displayClientMessage(component(), true);
    }

    /**
     * 将构建的组件作为聊天消息发送给玩家。
     * <p>
     * Sends the built component as a chat message to the player.
     *
     * @param player 目标玩家 / The target player
     */
    public void sendChat(Player player) {
        player.displayClientMessage(component(), false);
    }

    /**
     * 将构建的组件添加到工具提示列表中。
     * <p>
     * Adds the built component to a tooltip list.
     *
     * @param tooltip 工具提示列表 / The tooltip list
     */
    public void addTo(List<? super MutableComponent> tooltip) {
        tooltip.add(component());
    }

    /**
     * 将构建的组件以护目镜样式（带缩进）添加到工具提示列表。
     * <p>
     * Adds the built component to a tooltip list in goggles style (with indentation).
     *
     * @param tooltip 工具提示列表 / The tooltip list
     */
    public void forGoggles(List<? super MutableComponent> tooltip) {
        forGoggles(tooltip, 0);
    }

    /**
     * 将构建的组件以护目镜样式（带指定缩进级别）添加到工具提示列表。
     * <p>
     * Adds the built component to a tooltip list in goggles style with the specified indentation level.
     *
     * @param tooltip 工具提示列表 / The tooltip list
     * @param indents 额外缩进级别 / The additional indentation level
     */
    public void forGoggles(List<? super MutableComponent> tooltip, int indents) {
        tooltip.add(new LangBuilder(this.namespace)
                .text(Strings.repeat(" ", getIndents(Minecraft.getInstance().font, 4 + indents)))
                .add(this)
                .component());
    }

    public static final float DEFAULT_SPACE_WIDTH = 4.0F; // space width in vanilla's default font
    static int getIndents(Font font, int defaultIndents) {
        int spaceWidth = font.width(" ");
        if (DEFAULT_SPACE_WIDTH == spaceWidth) {
            return defaultIndents;
        }
        return Mth.ceil(DEFAULT_SPACE_WIDTH * defaultIndents / spaceWidth);
    }

}
