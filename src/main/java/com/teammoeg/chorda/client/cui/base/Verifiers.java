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

package com.teammoeg.chorda.client.cui.base;

import com.simibubi.create.foundation.utility.Components;
import com.teammoeg.chorda.client.cui.base.Verifier.VerifyResult;

public class Verifiers {
	public static final Verifier<String> COMMAND=Verifier.successOrTranslatable(s->s.startsWith("/"), "gui.chorda.editor.command_must_starts_with_slash");
	public static final Verifier<String> NUMBER_STR=Verifier.successOrTranslatable(s->{try{
		Double.parseDouble(s);return true;
	}catch(Throwable t) {return false;}}, "gui.chorda.editor.wrong_number");
	public static final Verifier<String> INT_STR=Verifier.successOrTranslatable(s->{try{
		Integer.parseInt(s);return true;
	}catch(Throwable t) {return false;}}, "gui.chorda.editor.wrong_number");
	
	public static final Verifier<String> LONG_STR=Verifier.successOrTranslatable(s->{try{
		Long.parseLong(s);return true;
	}catch(Throwable t) {return false;}}, "gui.chorda.editor.wrong_number");
	private Verifiers() {
	}
	public static Verifier<String> intRange(final int min,final int max){
		
		return s->{
			try{
				int val=Integer.parseInt(s);
				if(val>=min&&val<=max) {
					
					return VerifyResult.SUCCESS;
				}else {
					String minStr,maxStr;
					if(min==Integer.MIN_VALUE) {
						minStr="";
					}else
						minStr=String.valueOf(min);
					if(max==Integer.MAX_VALUE) {
						maxStr="";
					}else
						maxStr=String.valueOf(max);
					return VerifyResult.error(Components.translatable("gui.chorda.editor.wrong_value_range",minStr,maxStr));
				}
			}catch(Throwable t) {
				return VerifyResult.error(Components.translatable("gui.chorda.editor.wrong_number"));
			}
		};
	}
	public static Verifier<String> longRange(final long min,final long max){
		
		return s->{
			try{
				long val=Long.parseLong(s);
				if(val>=min&&val<=max) {
					
					return VerifyResult.SUCCESS;
				}else {
					String minStr,maxStr;
					if(min==Long.MIN_VALUE) {
						minStr="";
					}else
						minStr=String.valueOf(min);
					if(max==Long.MAX_VALUE) {
						maxStr="";
					}else
						maxStr=String.valueOf(max);
					return VerifyResult.error(Components.translatable("gui.chorda.editor.wrong_value_range",minStr,maxStr));
				}
			}catch(Throwable t) {
				return VerifyResult.error(Components.translatable("gui.chorda.editor.wrong_number"));
			}
		};
	}
	public static Verifier<String> doubleRange(final double min,final double max){
		
		return s->{
			try{
				double val=Double.parseDouble(s);
				if(val>=min&&val<=max) {
					
					return VerifyResult.SUCCESS;
				}else {
					String minStr,maxStr;
					if(min==Double.MIN_VALUE) {
						minStr="";
					}else
						minStr=String.valueOf(min);
					if(max==Double.MAX_VALUE) {
						maxStr="";
					}else
						maxStr=String.valueOf(max);
					return VerifyResult.error(Components.translatable("gui.chorda.editor.wrong_value_range",minStr,maxStr));
				}
			}catch(Throwable t) {
				return VerifyResult.error(Components.translatable("gui.chorda.editor.wrong_number"));
			}
		};
	}
}
