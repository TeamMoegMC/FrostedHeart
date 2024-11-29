package com.teammoeg.frostedheart.content.scenario;

import java.util.Map;

public class ScenarioUtils {
	public static String getOfAlias(Map<String,String> params,String...keys) {
		for(String s:keys) {
			String r=params.get(s);
			if(r!=null)
				return r;
		}
		return null;
	}
}
