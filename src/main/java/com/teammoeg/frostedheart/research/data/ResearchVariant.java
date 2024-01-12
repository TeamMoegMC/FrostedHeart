/*
 * Copyright (c) 2024 TeamMoeg
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
