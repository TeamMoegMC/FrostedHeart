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

package com.teammoeg.frostedresearch.data;

public enum ResearchVariant {
    MAX_ENERGY("max_energy"),//max Energy increasement
    MAX_ENERGY_MULT("max_energy_multiplier"),//max Energy multiplier
    GENERATOR_EFFICIENCY("generator_effi"),//generator fuel efficiency
    GENERATOR_HEAT("generator_heat"),//generator heat efficiency
    //GENERATOR_LOCATION("generator_loc"),//generator location, to keep generators unique.
    VILLAGER_RELATION("vlg_relationship"),//relationship modifier between villagers.
    VILLAGER_FORGIVENESS("vlg_forgive"),//Forgiveness for each kill in stats
    OVERDRIVE_RECOVER("overdrive_recover"),//Overdrive recover speed
    HAS_FORECAST("has_forecast");//Weather forecast enabled

    private final String token;

    ResearchVariant(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
