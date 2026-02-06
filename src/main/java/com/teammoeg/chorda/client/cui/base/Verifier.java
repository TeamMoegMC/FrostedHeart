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

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.teammoeg.chorda.text.Components;

import net.minecraft.network.chat.Component;

public interface Verifier<T> {
	public static record VerifyResult(boolean isError,@Nullable Component hint){
		public static final Verifier.VerifyResult SUCCESS=new Verifier.VerifyResult(false,null);
		public static Verifier.VerifyResult error(Component message) {
			return new Verifier.VerifyResult(true,message);
		}

	}
	public static <T> Verifier<T> nonNull(){
		return successOrTranslatable(Objects::nonNull,"gui.chorda.editor.must_be_nonnull");
	}
	VerifyResult test(T val);
	public static<T> Verifier<T> successOrComponent(Predicate<T> test,Supplier<Component> comp){
		return o->test.test(o)?VerifyResult.SUCCESS:new VerifyResult(true,comp.get());
	}
	public static<T> Verifier<T> successOrTranslatable(Predicate<T> test,String comp){
		return successOrComponent(test,()->Components.translatable(comp));
	}
	public default Verifier<T> and(Verifier<T> other){
		return o->{
			VerifyResult thisResult=this.test(o);
			VerifyResult thatResult=other.test(o);
			boolean resultIsError=thisResult.isError()||thatResult.isError();
			if(thisResult.hint()!=null&&thatResult.hint()!=null)
				return new VerifyResult(resultIsError,Components.str("").append(thisResult.hint()).append(thatResult.hint()));
			return new VerifyResult(resultIsError,thisResult.hint()==null?thatResult.hint():thisResult.hint());
		};
	}
}
