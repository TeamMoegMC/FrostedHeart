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

package com.teammoeg.chorda.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.teammoeg.chorda.text.ComponentOptimizer;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.util.parsereader.ParseReader;
import com.teammoeg.chorda.util.parsereader.source.StringLineSource;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/**
 * 字符串文本组件解析器。
 *
 * <p>将带有传统 Minecraft 格式化代码、十六进制颜色、转义、翻译键和字体图标的字符串，
 * 解析为 {@link MutableComponent}。</p>
 *
 * <p>支持的可用格式如下：</p>
 * <ul>
 *   <li>颜色和格式代码，前缀为 {@code &} 或 {@code §}：
 *       {@code 0}=黑色、{@code 1}=深蓝、{@code 2}=深绿、{@code 3}=深青、
 *       {@code 4}=深红、{@code 5}=深紫、{@code 6}=金色、{@code 7}=灰色、
 *       {@code 8}=深灰、{@code 9}=蓝色、{@code a}=绿色、{@code b}=青色、
 *       {@code c}=红色、{@code d}=淡紫、{@code e}=黄色、{@code f}=白色、
 *       {@code k}=混淆/随机、{@code l}=粗体、{@code m}=删除线、
 *       {@code n}=下划线、{@code o}=斜体、{@code r}=重置。</li>
 *   <li>十六进制颜色：
 *       {@code &#RRGGBB} 或 {@code §#RRGGBB}，6 位十六进制颜色；
 *       {@code &#RGB} 或 {@code §#RGB}，3 位简写，每位扩展为两位，
 *       例如 {@code &#f0a} 等价于 {@code &#ff00aa}。</li>
 *   <li>转义：
 *       反斜杠 {@code \} 可转义下一个字符，使其按字面输出；
 *       连续 {@code &&} 或 {@code §§} 输出单个 {@code &} 或 {@code §}。</li>
 *   <li>翻译键：
 *       <code>&#123;翻译键&#125;</code> 会先作为翻译键取出翻译文本，
 *       再递归解析并继承当前样式。</li>
 *   <li>字体图标：
 *       <code>&#123;@font 内容 字体id 颜色&#125;</code>，
 *       例如 <code>&#123;@font F204 chorda:default -44224&#125;</code>。
 *       其中“内容”若为 4 位十六进制，会转为对应 Unicode 字符；
 *       “颜色”为十进制可带符号的颜色值或以#开头的十六进制颜色值。</li>
 * </ul>
 *
 * <p>示例：{@code "&a绿色 &l粗体 &#ff0000红色"}。</p>
 */
public class StringTextComponentParser {
	public static final Char2ObjectOpenHashMap<ChatFormatting> LEGACY_FORMAT_CODE_CACHE = new Char2ObjectOpenHashMap<>();

	static {
		LEGACY_FORMAT_CODE_CACHE.put('0', ChatFormatting.BLACK);
		LEGACY_FORMAT_CODE_CACHE.put('1', ChatFormatting.DARK_BLUE);
		LEGACY_FORMAT_CODE_CACHE.put('2', ChatFormatting.DARK_GREEN);
		LEGACY_FORMAT_CODE_CACHE.put('3', ChatFormatting.DARK_AQUA);
		LEGACY_FORMAT_CODE_CACHE.put('4', ChatFormatting.DARK_RED);
		LEGACY_FORMAT_CODE_CACHE.put('5', ChatFormatting.DARK_PURPLE);
		LEGACY_FORMAT_CODE_CACHE.put('6', ChatFormatting.GOLD);
		LEGACY_FORMAT_CODE_CACHE.put('7', ChatFormatting.GRAY);
		LEGACY_FORMAT_CODE_CACHE.put('8', ChatFormatting.DARK_GRAY);
		LEGACY_FORMAT_CODE_CACHE.put('9', ChatFormatting.BLUE);
		LEGACY_FORMAT_CODE_CACHE.put('a', ChatFormatting.GREEN);
		LEGACY_FORMAT_CODE_CACHE.put('b', ChatFormatting.AQUA);
		LEGACY_FORMAT_CODE_CACHE.put('c', ChatFormatting.RED);
		LEGACY_FORMAT_CODE_CACHE.put('d', ChatFormatting.LIGHT_PURPLE);
		LEGACY_FORMAT_CODE_CACHE.put('e', ChatFormatting.YELLOW);
		LEGACY_FORMAT_CODE_CACHE.put('f', ChatFormatting.WHITE);
		LEGACY_FORMAT_CODE_CACHE.put('k', ChatFormatting.OBFUSCATED);
		LEGACY_FORMAT_CODE_CACHE.put('l', ChatFormatting.BOLD);
		LEGACY_FORMAT_CODE_CACHE.put('m', ChatFormatting.STRIKETHROUGH);
		LEGACY_FORMAT_CODE_CACHE.put('n', ChatFormatting.UNDERLINE);
		LEGACY_FORMAT_CODE_CACHE.put('o', ChatFormatting.ITALIC);
		LEGACY_FORMAT_CODE_CACHE.put('r', ChatFormatting.RESET);
	}
	private StringTextComponentParser() {
		
	}
	/**
     * 解析十六进制颜色代码为 {@link TextColor}。
     *
     * <p>支持两种长度：</p>
     * <ul>
     *   <li>长度为 6：按 {@code RRGGBB} 解析，等价于 {@code #RRGGBB}。</li>
     *   <li>长度不小于 3：取前 3 个字符分别作为 R、G、B 的简写，
     *       每位乘以 {@code 0x11} 扩展为两位，例如 {@code f0a} → {@code #ff00aa}。</li>
     * </ul>
     *
     * @param colorCode 颜色代码字符序列，不包含前缀 {@code #}
     * @return 解析出的 {@link TextColor}；若长度不足 3 则返回 {@code null}
     */
	public static TextColor parseColor(CharSequence colorCode) {
		TextColor tc=null;
		if(colorCode.length()==6) {
			tc=TextColor.parseColor("#"+colorCode);
		}else if(colorCode.length()>=3) {
			int r=Integer.parseInt(colorCode, 0, 1, 16);
			int g=Integer.parseInt(colorCode, 1, 2, 16);
			int b=Integer.parseInt(colorCode, 2, 3, 16);
			
			tc=TextColor.fromRgb(FastColor.ARGB32.color(0x0, r*0x11, g*0x11, b*0x11));
		}
		return tc;
	}
	/**
     * 解析单行文本，将结果追加到 {@code builder}。
     *
     * <p>该方法处理转义、传统格式代码、十六进制颜色、翻译键和 {@code @font} 字体图标。
     * 初始样式为 {@code defaultStyle}，解析过程中样式会随格式代码动态变化。</p>
     *
     * @param parser       当前行的解析读取器
     * @param builder      组件构建器，用于收集解析结果
     * @param defaultStyle 本行初始默认样式
     * @return 文本结束后的样式
     */
	public static Style parseLine(ParseReader parser, ComponentOptimizer builder, Style defaultStyle) {
		char escapedChar=0;
		boolean escaped=false;
		boolean slashEscaped=false;
		Style style=defaultStyle;
		while(parser.has()) {
			char ch=parser.eat();
			if(slashEscaped) {
				slashEscaped=false;
				builder.appendChar(ch, style);
				continue;
			}
			if(escaped) {
				escaped=false;
				if(ch=='&'||ch=='\u00a7') {
					builder.appendChar(ch, style);
					continue;
				}else if(ch=='#') {
					StringBuilder colorCode=new StringBuilder();
					parser.saveIndex();
					for(int i=0;i<6;i++) {
						if(!parser.has())
							break;
						if(i==3)
							parser.saveIndex();
						char cch=parser.eat();
						
						if((cch>='0'&&cch<='9')||(cch>='a'&&cch<='f')||(cch>='A'&&cch<='F'))
							colorCode.append(cch);
						else
							break;
					}
					TextColor tc=parseColor(colorCode);
					if(tc!=null) {
						if(colorCode.length()!=6)
							parser.restoreIndex();
						style=style.withColor(tc);
					}else {
						builder.appendChar(escapedChar, style);
						builder.appendChar(ch, style);
						parser.restoreIndex();
					}
					continue;
				}else {
					ChatFormatting format=LEGACY_FORMAT_CODE_CACHE.get(ch);
					if(format!=null) {
						style=style.applyFormat(format);
						continue;
					}
				}
				builder.appendChar(escapedChar, style);
			}
			switch(ch) {
			case '\\':
				slashEscaped=true;break;
			case '&':
			case '\u00a7':
				escaped=true;
				escapedChar=ch;break;
			case '{':
				parser.saveIndex();
				while(parser.has()&&parser.read()!='}') {
					parser.eat();
				}
				String content = parser.fromStart();
				if (content.startsWith("@font")) { // example: {@font F204 chorda:default -44224}
					var contents = content.split(" ");
					Style font = Style.EMPTY.withFont(new ResourceLocation(contents[2])); // font
					String t = contents[1]; // content
					if (contents[1].length() == 4)
						try {
                        	t = String.valueOf((char)Integer.parseInt(contents[1], 16));
                    	} catch (NumberFormatException ignore) {}
					var s = style;
					if (contents.length >= 4) { // color
						if(contents[3].startsWith("#")) {
							s = s.withColor(parseColor(contents[3].substring(1)));
						}else
							s = s.withColor(Integer.parseInt(contents[3]));
					}
					var icon = Components.literal(t)
							.withStyle(s)
							.withStyle(font);
					builder.appendRawComponent(icon);
				} else {
					
					builder.appendComponent(parse(Components.translatable(content).withStyle(style)));
				}
				if(parser.has())
					parser.eat('}');
				break;
				default:
					builder.appendChar(ch, style);break;
			}
		}
		if(escaped) {
			builder.appendChar(escapedChar, style);
		}
		return style;
	}
	/**
     * 将已有 {@link Component} 解析为可变文本组件。
     *
     * <p>遍历组件的所有子组件，对每个子组件的纯文本内容按行调用
     * {@link #parseLine(ParseReader, ComponentOptimizer, Style)}，
     * 并继承子组件自身的样式。</p>
     *
     * @param comp 待解析的组件
     * @return 解析后的可变文本组件
     */
	public static MutableComponent parse(Component comp) {
		ComponentOptimizer co=new ComponentOptimizer(comp);
		ComponentOptimizer out=new ComponentOptimizer();
		for(Component c:co.components()) {
			parseLine(new ParseReader(new StringLineSource(c.getString())),out,c.getStyle());
		}
		return out.build();
	}
	/**
     * 使用 {@link Style#EMPTY} 作为默认样式解析字符串。
     *
     * @param str 待解析的字符串
     * @return 解析后的可变文本组件
     */
	public static MutableComponent parse(String str) {
		return parse(str,Style.EMPTY);
	}

	/**
     * 将带格式代码的字符串解析为文本组件。
     *
     * <p>支持的格式详见类注释。解析按行进行，每行使用 {@code defaultStyle} 作为初始样式。
     * 若 {@code defaultStyle} 为 {@code null}，则使用 {@link Style#EMPTY}。</p>
     *
     * @param str          待解析的字符串
     * @param defaultStyle 每行初始默认样式，可为 {@code null}
     * @return 解析后的可变文本组件
     */
	public static MutableComponent parse(String str,@Nullable Style defaultStyle) {
		if(defaultStyle==null)
			defaultStyle=Style.EMPTY;
		ParseReader parser=new ParseReader(new StringLineSource(str, str));
		ComponentOptimizer builder=new ComponentOptimizer();
		while(parser.nextLine()) {
			parseLine(parser,builder,defaultStyle);
		}
		return builder.build();
	}
	/**
     * 将带格式代码的字符串逐行解析为多个可变文本组件。
     *
     * <p>每一行独立生成一个 {@link MutableComponent}，每行均使用
     * {@code defaultStyle} 作为初始样式。若 {@code defaultStyle} 为 {@code null}，
     * 则使用 {@link Style#EMPTY}。</p>
     *
     * @param str          待解析的字符串
     * @param defaultStyle 每行初始默认样式，可为 {@code null}
     * @return 每行对应的可变文本组件列表
     */
    public static List<MutableComponent> parseMultiline(String str, Style defaultStyle) {
    	return parseMultiline(str,defaultStyle,false);
    }
	/**
     * 将带格式代码的字符串逐行解析为多个可变文本组件。
     *
     * <p>每一行独立生成一个 {@link MutableComponent}。若 {@code defaultStyle} 为 {@code null}，
     * 则使用 {@link Style#EMPTY}。</p>
     *
     * @param str                  待解析的字符串
     * @param defaultStyle         默认样式，可为 {@code null}
     * @param inheritPreviousStyle 是否让后续行继承上一行解析结束时的样式。
     *                             为 {@code true} 时，从第二行开始以上一行结束时的样式作为初始样式；
     *                             为 {@code false} 时，每一行都使用 {@code defaultStyle} 作为初始样式。
     * @return 每行对应的可变文本组件列表
     */
    public static List<MutableComponent> parseMultiline(String str, Style defaultStyle, boolean inheritPreviousStyle) {
		if(defaultStyle==null)
			defaultStyle=Style.EMPTY;
		ParseReader parser=new ParseReader(new StringLineSource(str, str));
		List<MutableComponent> list=new ArrayList<>();
		while(parser.nextLine()) {
			ComponentOptimizer builder=new ComponentOptimizer();
			Style endStyle=parseLine(parser,builder,defaultStyle);
			if(inheritPreviousStyle)
				defaultStyle=endStyle;
			list.add(builder.build());
		}
		return list;
	}
	/*public static void main(String[] args) {
		System.out.println(parse("砧木&b&l&测试&r得到&#aaa测试"));
	}*/
}
