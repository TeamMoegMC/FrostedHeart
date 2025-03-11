package com.teammoeg.chorda.util;

import java.util.function.Function;
import java.util.function.Supplier;


public class CFunctionHelper {

	public CFunctionHelper() {
		
	}
	public static <A,B> Function<A,B> mapNullable(Function<A,B> func,B def){
		return a->a==null?def:func.apply(a);
	}
	public static <A,B,C> Function<A,C> mapIfMapNullable(Function<A,B> func,Function<B,C> def,Function<A,C> ifNull){
		return a->{
			B b=func.apply(a);
			if(b!=null) {
				return def.apply(b);
			}
			return ifNull.apply(a);
		};
	}
	public static <T> T makeIf(boolean available,Supplier<T> supp) {
		if(available)
			return supp.get();
		return null;
	}
}
