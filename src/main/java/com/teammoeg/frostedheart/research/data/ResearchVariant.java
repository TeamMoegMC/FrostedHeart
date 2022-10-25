package com.teammoeg.frostedheart.research.data;

public enum ResearchVariant {
	MAX_ENERGY("max_energy"),//max Energy increasement
	MAX_ENERGY_MULT("max_energy_multiplier"),//max Energy multiplier
	GENERATOR_LOCATION("generator_loc");//generator location, to keep generators unique.
	private ResearchVariant(String token) {
		this.token = token;
	}
	private final String token;
	public String getToken() {
		return token;
	}
}
