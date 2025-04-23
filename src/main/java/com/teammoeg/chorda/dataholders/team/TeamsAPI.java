package com.teammoeg.chorda.dataholders.team;

public class TeamsAPI {
	private static TeamsAPIProvider provider=new SinglePlayerTeamAPIProvider();
	private TeamsAPI() {
	}
	public static TeamsAPIProvider getAPI() {
		return provider;
	}
	public static void register(TeamsAPIProvider prov) {
		provider=prov;
	}
}
