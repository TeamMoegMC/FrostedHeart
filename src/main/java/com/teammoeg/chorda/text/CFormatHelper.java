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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

/**
 * 文本格式化工具类，提供生物群系名称、时间、数字和单位的格式化方法。
 * <p>
 * Text formatting utility class providing formatting methods for biome names, time, numbers, and units.
 */
public class CFormatHelper {

	/**
	 * 获取生物群系的名称字符串。
	 * <p>
	 * Gets the name string of a biome holder.
	 *
	 * @param biomeHolder 生物群系持有者 / The biome holder to get the name from
	 * @return 生物群系名称，若为null则返回"null" / The biome name, or "null" if the holder is null
	 */
	public static String getBiomeName(Holder<Biome> biomeHolder) {
	    if (biomeHolder == null) {
	        return "null";
	    }
	    return biomeHolder.unwrap().map(
	            biomeResourceKey -> biomeResourceKey.location().toString(),
	            unregistered -> "[unregistered " + unregistered + "]");
	}

	/**
	 * 将秒数转换为可读的时间组件（年/天/时/分/秒）。
	 * <p>
	 * Converts seconds to a human-readable time component (years/days/hours/minutes/seconds).
	 *
	 * @param secondIn 输入的秒数 / The number of seconds to convert
	 * @return 格式化的时间组件 / The formatted time component
	 */
	public static Component secToTime(long secondIn) {
	    long years = secondIn / (365 * 24 * 60 * 60);
	    long remainingSeconds = secondIn % (365 * 24 * 60 * 60);
	    long days = remainingSeconds / (24 * 60 * 60);
	    remainingSeconds %= (24 * 60 * 60);
	    long hours = remainingSeconds / (60 * 60);
	    remainingSeconds %= (60 * 60);
	    long minutes = remainingSeconds / 60;
	    long seconds = remainingSeconds % 60;
	
	    var c = Component.empty();
	    if (years > 100) {
	        c = Component.translatable("gui.frostedheart.infinity");
	    } else {
	        if (years   != 0) c.append(Component.translatable("gui.frostedheart.year", years));
	        if (days    != 0) c.append(Component.translatable("gui.frostedheart.day", days));
	        if (hours   != 0) c.append(Component.translatable("gui.frostedheart.hour", hours));
	        if (minutes != 0) c.append(Component.translatable("gui.frostedheart.minute", minutes));
	        c.append(Component.translatable("gui.frostedheart.second", seconds));
	    }
	    return c;
	}

	/**
	 * 将毫秒数转换为可读的时间组件。
	 * <p>
	 * Converts milliseconds to a human-readable time component.
	 *
	 * @param milliseconds 输入的毫秒数 / The number of milliseconds to convert
	 * @return 格式化的时间组件 / The formatted time component
	 */
	public static Component msToTime(long milliseconds) {
	    Duration duration = Duration.ofMillis(milliseconds);
	    return secToTime(duration.getSeconds());
	}
	/** SI单位前缀字符串（千、兆、吉等） / SI unit prefix string (k, M, G, T, etc.) */
	final static String units = "kMGTPEZYRQ";//6 units should satisfy
	/** SI小数单位前缀字符串（毫、微、纳等） / SI fractional unit prefix string (m, u, n, p, etc.) */
	final static String fractionUnits="mμnpfazyrq";

	/**
	 * 线程安全的数字格式化器集合，提供不同精度的十进制和百分比格式。
	 * <p>
	 * Thread-safe collection of number formatters providing decimal and percentage formats with varying precision.
	 */
	public static class NumberFormats{
		private NumberFormats() {}
		final DecimalFormat decimal1digit = new DecimalFormat("#.#");
	
		final DecimalFormat decimal2digit = new DecimalFormat("#.##");
	
		final DecimalFormat decimal3digit = new DecimalFormat("#.##");
		
		final DecimalFormat percentage1digit = new DecimalFormat("0.0%");
	}
	private static ThreadLocal<NumberFormats> numberFormat=ThreadLocal.withInitial(()->new NumberFormats());

	/**
	 * 获取当前线程的数字格式化器集合。
	 * <p>
	 * Gets the number formatters for the current thread.
	 *
	 * @return 当前线程的NumberFormats实例 / The NumberFormats instance for the current thread
	 */
	public static NumberFormats getNumberFormats() {
		return numberFormat.get();
	}

	/**
	 * 将长整数转换为带SI单位前缀的可读字符串（超过2000时升级单位）。
	 * <p>
	 * Converts a long number to a human-readable string with SI unit prefixes (upscales when exceeding 2000).
	 *
	 * @param num 要格式化的数字 / The number to format
	 * @return 带单位前缀的可读字符串 / A readable string with unit prefix
	 */
	public static String toReadableUnit(long num) {
		int unit = -1;
		double lnum = num;
		while (lnum > 1999) {//upscale unit when number reaches 2000
			unit++;
			lnum /= 1000;
		}
		if (unit < 0)
			return String.valueOf(num);
		return toDynamicDigits(lnum) + units.charAt(unit);
	}
	/**
	 * 将大数字字符串转换为带SI单位前缀的可读字符串。
	 * 支持小数和非常大的数字，通过解析字符串中的有效数字来确定合适的单位。
	 * <p>
	 * Converts a big number string to a human-readable string with SI unit prefixes.
	 * Supports decimals and very large numbers by parsing significant digits to determine the appropriate unit.
	 *
	 * @param bigNumber 要格式化的数字字符串 / The number string to format
	 * @return 带单位前缀的可读字符串 / A readable string with unit prefix
	 */
	public static String toBigReadableUnit(String bigNumber) {
		int index=0,exponent=0,unit=0,fractionDigits=3;
		char[] digits=new char[3];
		boolean isFraction=false;
		for(;index<bigNumber.length();index++) {
			char c=bigNumber.charAt(index);
			if(c>='0'&&c<='9') {
				if(digits[0]==0) {//at start
					if(isFraction)
						exponent--;
					if(c=='0')
						continue;
					digits[0]=c;
				}else if(digits[1]==0) {
					digits[1]=c;
				}else if(digits[2]==0) {
					digits[2]=c;
					if(isFraction)
						break;
				}
				if(!isFraction)
					exponent++;
			}else if(c=='.')
				isFraction=true;
		}
		if(exponent>0)
			unit=(int) Math.floor((exponent-1)/3f);
		else
			unit=(int) Math.floor((exponent)/3f);
		int dot=exponent%3;
		if(dot<0)
			dot=(dot+4);
		
		for(int i=2;i>=0;i--) {
			if(digits[i]==0) {
				digits[i]='0';
			}
			if(digits[i]=='0'&&i>=dot) {
				fractionDigits=i;
			}
		}
		if(fractionDigits==0)
			return "0";
		StringBuilder sb=new StringBuilder();
		if(digits[0]!=0)
			for(int i=0;i<fractionDigits;i++) {
				sb.append(digits[i]);
				if(i==(dot-1)&&i!=fractionDigits-1)
					sb.append(".");
			}
		if(unit<0)
			sb.append(fractionUnits.charAt(-unit-1));
		else if(unit>0)
			sb.append(units.charAt(unit-1));
		return sb.toString();
	}
	public static void main(String[] args) {
		for(int i=7;i>=0;i--) {
			String num="9".repeat(i);
		System.out.println(i+"="+toBigReadableUnit(num));
		}
		for(int i=0;i<6;i++) {
			String num="0."+("0".repeat(i))+"9";
		System.out.println("-"+i+"="+toBigReadableUnit(num));
		}
	}
	/**
	 * 将长整数转换为适用于物品堆叠显示的可读单位字符串（超过1000时升级单位）。
	 * <p>
	 * Converts a long number to a readable unit string suitable for item stack display (upscales when exceeding 1000).
	 *
	 * @param num 要格式化的数字 / The number to format
	 * @return 带单位前缀的可读字符串 / A readable string with unit prefix
	 */
	public static String toReadableItemStackUnit(long num) {
		int unit = -1;
		double lnum = num;
		while (lnum > 999) {//upscale unit when number reaches 1000
			unit++;
			lnum /= 1000;
		}
		if (unit < 0)
			return String.valueOf(num);
		return getNumberFormats().decimal1digit.format(lnum) + units.charAt(unit);
	}
	/**
	 * 将浮点数转换为带SI单位前缀的可读字符串，同时支持大于1和小于1的数值。
	 * <p>
	 * Converts a double to a human-readable string with SI unit prefixes, supporting both values greater than and less than 1.
	 *
	 * @param num 要格式化的数字 / The number to format
	 * @return 带单位前缀的可读字符串 / A readable string with unit prefix
	 */
	public static String toReadableUnit(double num) {
		int unit = -1;
		double lnum = num;
		while (lnum > 1999) {//upscale unit when number reaches 2000
			unit++;
			lnum /= 1000;
		}
		while(lnum<1) {
			unit--;
			lnum*=1000;
		}
		String number=toDynamicDigits(lnum);
		if(unit<-1) {
			return number+fractionUnits.charAt(-unit-2);
		}else if(unit==-1) {
			return number;
		}else{
			return number+units.charAt(unit);
		}
	}
	/**
	 * 根据数值大小动态选择小数位数进行格式化。
	 * 大于等于1000时无小数位，大于等于100时1位小数，否则2位小数。
	 * <p>
	 * Dynamically selects decimal precision based on the value's magnitude.
	 * No decimal for values >= 1000, 1 decimal for >= 100, otherwise 2 decimals.
	 *
	 * @param lnum 要格式化的数字 / The number to format
	 * @return 格式化后的字符串 / The formatted string
	 */
	public static String toDynamicDigits(double lnum) {
		NumberFormats numberFormat=getNumberFormats();
		if (lnum >= 1000) {//4 digits, no decimal digits
			return String.valueOf((long) lnum);
		} else if (lnum >= 100) {//3 digits, 1 decimal digits
			return numberFormat.decimal1digit.format(lnum);
		} else if (lnum >= 10) {//2 digit, 2 decimal digits
			return numberFormat.decimal2digit.format(lnum);
		} else {//1 digit, still 2 decimal digits
			return numberFormat.decimal2digit.format(lnum);
		}
	}
	/**
	 * 将浮点数格式化为百分比字符串（保留1位小数）。
	 * <p>
	 * Formats a double value as a percentage string with 1 decimal place.
	 *
	 * @param value 要格式化的值（0.0-1.0） / The value to format (0.0-1.0)
	 * @return 百分比字符串 / The percentage string
	 */
	public static String formatPercentage(double value) {
		return getNumberFormats().percentage1digit.format(value);
	}
	
	/**
	 * 将浮点数格式化为保留1位小数的字符串。
	 * <p>
	 * Formats a double value as a string with 1 decimal place.
	 *
	 * @param value 要格式化的值 / The value to format
	 * @return 格式化后的字符串 / The formatted string
	 */
	public static String formatNumber(double value) {
		return getNumberFormats().decimal1digit.format(value);
	}
}
