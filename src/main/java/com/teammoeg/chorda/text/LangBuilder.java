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
 * A builder for creating localised components
 *
 * How to use?
 *
 * Example: Lang.builder().translate("prefix", "suffix", args).component()
 *
 * Only use .component() at end of chain
 */
public class LangBuilder {
    String namespace;
    LinkedList<MutableComponent> components=new LinkedList<>();
    Style mainStyle=Style.EMPTY;
    NumberFormat currentNumberFormat=CFormatHelper.getNumberFormats().decimal2digit;
    public LangBuilder(String namespace) {
        this.namespace = namespace;
    }

    public static Object[] resolveBuilders(Object[] args) {
        for (int i = 0; i < args.length; i++)
            if (args[i]instanceof LangBuilder cb)
                args[i] = cb.component();
        return args;
    }

    public LangBuilder space() {
        return text(" ");
    }

    public LangBuilder newLine() {
        return text("\n");
    }

    /**
     * Appends a localised component.
     *
     * @param langKey simply key not processed
     * @param args
     * @return
     */
    public LangBuilder key(String langKey, Object... args) {
        return add(Components.translatable(langKey, resolveBuilders(args)));
    }

    /**
     * Appends a localised component<br>
     * To add an independently formatted localised component, use add() and a nested
     * builder
     *
     * @param suffix a suffix to the namespace
     * @param args
     * @return
     */
    public LangBuilder suffix(String suffix, Object... args) {
        return add(Components.translatable( namespace + "." + suffix, resolveBuilders(args)));
    }

    /**
     * Appends a localised component<br>
     * To add an independently formatted localised component, use add() and a nested
     * builder
     *
     * @param prefix a prefix to the namespace
     * @param args
     * @return
     */
    public LangBuilder prefix(String prefix, Object... args) {
        return add(Components.translatable( prefix + "." + namespace, resolveBuilders(args)));
    }


    /**
     * Appends a localised component with a prefix and suffix<br>
     *
     * Example usage: translate("tooltip", "temp_change", args)
     * Gives "tooltip.namespace.temp_change" as key.
     *
     * @param prefix a prefix to the namespace, normally a category
     * @param suffix a suffix to the namespace, normally a description
     * @param args the arguments
     * @return this builder
     */
    public LangBuilder translate(String prefix, String suffix, Object... args) {
        return add(Components.translatable( prefix + "." + namespace + "." + suffix, resolveBuilders(args)));
    }

    /**
     * Appends a text component
     *
     * @param literalText
     * @return
     */
    public LangBuilder text(String literalText) {
        return add(Components.literal(literalText));
    }

    /**
     * Appends a colored text component
     *
     * @param format
     * @param literalText
     * @return
     */
    public LangBuilder text(ChatFormatting format, String literalText) {
        return add(Components.literal(literalText).withStyle(format));
    }

    /**
     * Appends a colored text component
     *
     * @param color
     * @param literalText
     * @return
     */
    public LangBuilder text(int color, String literalText) {
        return add(Components.literal(literalText).withStyle(s -> s.withColor(color)));
    }
    /**
     * set number format for number
     * use # to represent optional digits, 0 to represent mandatory digits
     * such as #,##0.## would cause a minimum fraction digit of 0 and maximum fraction digit of 2 and a seperator by 3, more digits would have more seperators
     * @see java.text.DecimalFormat
     * 
     * */
    public LangBuilder setNumberFormat(String format) {
    	currentNumberFormat=new DecimalFormat(format);
    	return this;
    }
    /**
     * use number format for current locale
     * */
    public LangBuilder useLocalNumberFormat() {
    	currentNumberFormat=CFormatHelper.getNumberFormats().decimal2digit;
    	return this;
    }
    public LangBuilder percentage() {
    	currentNumberFormat=CFormatHelper.getNumberFormats().percentage1digit;
        return this;
    }
    public LangBuilder number(long d) {
        return add(Components.literal(currentNumberFormat.format(d)));
    }
    public LangBuilder number(double d) {
        return add(Components.literal(currentNumberFormat.format(d)));
    }

    
    public LangBuilder number(Number d) {
        return add(Components.literal(currentNumberFormat.format(d)));
    }

    /**
     * Appends the contents of another builder
     *
     * @param otherBuilder
     * @return
     */
    public LangBuilder add(LangBuilder otherBuilder) {
        return add(otherBuilder.component());
    }
    
    /**
     * Appends a component
     *
     * @param customComponent
     * @return
     */
    public LangBuilder add(MutableComponent customComponent) {
    	components.add(customComponent);
        return this;
    }

    /**
     * Appends a component
     *
     * @param component the component to append
     * @return this builder
     */
    public LangBuilder add(Component component) {
        if (component instanceof MutableComponent mutableComponent)
            return add(mutableComponent);
        else
            return add(component.copy());
    }

    //

    /**
     * Applies the format to all added components
     *
     * @param format
     * @return
     */
    public LangBuilder style(ChatFormatting format) {
    	mainStyle = mainStyle.applyFormat(format);
        return this;
    }

    /**
     * Applies the format to all added components
     *
     * @param style
     * @return
     */
    public LangBuilder style(Style style) {
    	mainStyle = style.applyTo(mainStyle);
        return this;
    }
    /**
     * Applies the color to all added components
     *
     * @param color
     * @return
     */
    public LangBuilder color(int color) {
    	mainStyle = mainStyle.withColor(color);
        return this;
    }

    /**
     * Applies the format to the last component
     *
     * @param format
     * @return
     */
    public LangBuilder withStyle(ChatFormatting format) {
    	components.addLast(components.pollLast().withStyle(format));
        return this;
    }

    /**
     * Applies the style to the last component
     *
     * @param style
     * @return
     */
    public LangBuilder withStyle(Style style) {
    	components.addLast(components.pollLast().withStyle(style));
        return this;
    }
    /**
     * Applies the color to the last component
     *
     * @param color
     * @return
     */
    public LangBuilder withColor(int color) {
    	components.addLast(components.pollLast().withStyle(s -> s.withColor(color)));
        return this;
    }
    //
    /**
     * Build raw component
     * */
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
     * Build optimized component, merging component of same content and translate texts, client only
     * */
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

    public String string() {
        return component().getString();
    }

    public String json() {
        return Component.Serializer.toJson(component());
    }

    public void sendStatus(Player player) {
        player.displayClientMessage(component(), true);
    }

    public void sendChat(Player player) {
        player.displayClientMessage(component(), false);
    }

    public void addTo(List<? super MutableComponent> tooltip) {
        tooltip.add(component());
    }

    public void forGoggles(List<? super MutableComponent> tooltip) {
        forGoggles(tooltip, 0);
    }

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
