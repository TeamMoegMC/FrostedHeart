package com.teammoeg.chorda.util;

import java.util.function.Function;

public class CFunctionHelper {

	public CFunctionHelper() {
		
	}
	public static <A,B> Function<A,B> mapNullable(Function<A,B> func,B def){
		return a->a==null?def:func.apply(a);
	}

}
