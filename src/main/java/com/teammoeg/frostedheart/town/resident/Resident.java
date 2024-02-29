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

package com.teammoeg.frostedheart.town.resident;

import net.minecraft.nbt.CompoundNBT;

/**
 * A resident of the town.
 *
 * This is an abstract data type used in the town simulation.
 * For the actual entity, see {@link ResidentEntity}.
 */
public class Resident {

    private String firstName;
    private String lastName;
    // physical
    private int health;
    // psychological
    private int happiness;
    // social
    private int social;
    // economic
    private int wealth;
    // political
    private int trust;
    // cultural
    private int culture;
    // educational
    private int educationLevel;

    public Resident() {
    }

    public float getProductivityMultiplier() {
        return 1.0f;
    }

    // serialization
    public CompoundNBT serialize() {
        CompoundNBT data = new CompoundNBT();
        data.putString("firstName", firstName);
        data.putString("lastName", lastName);
        data.putInt("health", health);
        data.putInt("happiness", happiness);
        data.putInt("social", social);
        data.putInt("wealth", wealth);
        data.putInt("trust", trust);
        data.putInt("culture", culture);
        data.putInt("educationLevel", educationLevel);
        return data;
    }

    public void deserialize(CompoundNBT data) {
        firstName = data.getString("firstName");
        lastName = data.getString("lastName");
        health = data.getInt("health");
        happiness = data.getInt("happiness");
        social = data.getInt("social");
        wealth = data.getInt("wealth");
        trust = data.getInt("trust");
        culture = data.getInt("culture");
        educationLevel = data.getInt("educationLevel");
    }

}
