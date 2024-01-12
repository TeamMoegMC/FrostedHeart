package com.teammoeg.frostedheart.research.data;

public enum ResearchVariant {
    MAX_ENERGY("max_energy"),//max Energy increasement
    MAX_ENERGY_MULT("max_energy_multiplier"),//max Energy multiplier
    GENERATOR_EFFICIENCY("generator_effi"),//generator fuel efficiency
    GENERATOR_HEAT("generator_heat"),//generator heat efficiency
    GENERATOR_LOCATION("generator_loc"),//generator location, to keep generators unique.
    VILLAGER_RELATION("vlg_relationship"),//relationship modifier between villagers.
    VILLAGER_FORGIVENESS("vlg_forgive")//Forgiveness for each kill in stats
    ;

    private ResearchVariant(String token) {
        this.token = token;
    }

    private final String token;

    public String getToken() {
        return token;
    }
}
