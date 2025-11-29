package com.teammoeg.chorda.client.cui.editor;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;

public interface Verifier<T> {
	public static record VerifyResult(boolean isError,@Nullable Component hint){

	}
	public static final VerifyResult SUCCESS=new VerifyResult(false,null);
	public static <T> Verifier<T> nonNull(){
		return successOrTranslatable(Objects::nonNull,"gui.chorda.editor.must_be_nonnull");
	}
	VerifyResult test(T val);
	public static<T> Verifier<T> successOrComponent(Predicate<T> test,Supplier<Component> comp){
		return o->test.test(o)?SUCCESS:new VerifyResult(true,comp.get());
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
