package com.teammoeg.chorda.client.cui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.teammoeg.chorda.client.ClientUtils;

public class CUIDebugHelper {
	private static boolean isDebugEnabled;
	private static List<WeakReference<UIElement>> uiObjects=new ArrayList<>();
	private CUIDebugHelper() {

	}
	public static boolean isDebugEnabled() {
		return ClientUtils.getMc().options.renderDebug&&isDebugEnabled;
		
	}
	public static void toggleDebug() {
		if(ClientUtils.getMc().options.renderDebug)
			isDebugEnabled=!isDebugEnabled;
	}
	public static void registerUIObject(UIElement elm) {
		uiObjects.add(new WeakReference<>(elm));
	}
	public static void gcDebug() {
		uiObjects.removeIf(t->t.get()==null);
	}
}
