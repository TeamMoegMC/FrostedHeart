package com.teammoeg.chorda.text;

import java.text.DecimalFormat;
import java.time.Duration;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.biome.Biome;

public class CFormatHelper {

	public static String getBiomeName(Holder<Biome> biomeHolder) {
	    if (biomeHolder == null) {
	        return "null";
	    }
	    return biomeHolder.unwrap().map(
	            biomeResourceKey -> biomeResourceKey.location().toString(),
	            unregistered -> "[unregistered " + unregistered + "]");
	}

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

	public static Component msToTime(long milliseconds) {
	    Duration duration = Duration.ofMillis(milliseconds);
	    return secToTime(duration.getSeconds());
	}
	final static String units = "KMGTPEZYH";
	final static String fractionUnits="mμnpf";
	public static class NumberFormats{
		private NumberFormats() {}
		final DecimalFormat decimal1digit = new DecimalFormat("#.#");
	
		final DecimalFormat decimal2digit = new DecimalFormat("#.##");
	
		final DecimalFormat decimal3digit = new DecimalFormat("#.##");
		
		final DecimalFormat percentage1digit = new DecimalFormat("0.0%");
	}
	private static ThreadLocal<NumberFormats> numberFormat=ThreadLocal.withInitial(()->new NumberFormats());
	public static NumberFormats getNumberFormats() {
		return numberFormat.get();
	}
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
	public static String formatPercentage(double value) {
		return getNumberFormats().percentage1digit.format(value);
	}
	
	public static String formatNumber(double value) {
		return getNumberFormats().decimal1digit.format(value);
	}
}
