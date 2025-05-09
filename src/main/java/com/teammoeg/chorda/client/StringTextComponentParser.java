package com.teammoeg.chorda.client;

import com.teammoeg.chorda.lang.ComponentOptimizer;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.util.parsereader.ParseReader;
import com.teammoeg.chorda.util.parsereader.source.StringLineSource;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor;

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
	public static MutableComponent parse(String str) {
		ParseReader parser=new ParseReader(new StringLineSource(str, str));
		ComponentOptimizer builder=new ComponentOptimizer();
		while(parser.nextLine()) {
			char escapedChar=0;
			boolean escaped=false;
			boolean slashEscaped=false;
			Style style=Style.EMPTY;
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
						TextColor tc=null;
						if(colorCode.length()==6) {
							tc=TextColor.parseColor("#"+colorCode);
							style=style.withColor(tc);
						}else if(colorCode.length()>=3) {
							int r=Integer.parseInt(colorCode, 0, 1, 16);
							int g=Integer.parseInt(colorCode, 1, 2, 16);
							int b=Integer.parseInt(colorCode, 2, 3, 16);
							
							tc=TextColor.fromRgb(FastColor.ARGB32.color(0x0, r*0x11, g*0x11, b*0x11));
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
					builder.appendRawComponent(parse(Components.translatable(parser.fromStart()).getString()).withStyle(style));
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
		}
		return builder.build();
	}
	/*public static void main(String[] args) {
		System.out.println(parse("砧木&b&l&测试&r得到&#aaa测试"));
	}*/
}
