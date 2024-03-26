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

package com.teammoeg.frostedheart.content.town.resident;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundNBT;

/**
 * A resident of the town.
 * <p>
 * This is an abstract data type used in the town simulation.
 * For the actual entity, see {@link ResidentEntity}.
 */
public class Resident {
	public static final Codec<Resident> CODEC=RecordCodecBuilder.create(t->t.group(
		Codec.STRING.fieldOf("firstName").forGetter(o->o.firstName),
		Codec.STRING.fieldOf("lastName").forGetter(o->o.lastName)
		).apply(t, Resident::new));
	
    private String firstName = "Steve";
    private String lastName = "Alexander";
    /** Stats range from 0 to 100 */
    // physical
    private int health = 50;
    // psychological
    private int mental = 50;
    // social
    private int social = 50;
    // economic
    private int wealth = 50;
    // political
    private int trust = 50;
    // cultural
    private int culture = 50;
    // educational
    private int educationLevel = 0;

    public Resident(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Resident() {
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Compute the productivity multiplier of the resident.
     * A linear map from [0,100] to [0.0,2.0].
     * The input is average of every stat.
     * @return the productivity multiplier
     */
    public double getProductivityMultiplier() {
        return (health + mental + social + wealth + trust + culture) / 300.0;
    }

    // serialization
    public CompoundNBT serialize() {
        CompoundNBT data = new CompoundNBT();
        data.putString("firstName", firstName);
        data.putString("lastName", lastName);
        data.putInt("health", health);
        data.putInt("happiness", mental);
        data.putInt("social", social);
        data.putInt("wealth", wealth);
        data.putInt("trust", trust);
        data.putInt("culture", culture);
        data.putInt("educationLevel", educationLevel);
        return data;
    }

    public Resident deserialize(CompoundNBT data) {
        firstName = data.getString("firstName");
        lastName = data.getString("lastName");
        health = data.getInt("health");
        mental = data.getInt("happiness");
        social = data.getInt("social");
        wealth = data.getInt("wealth");
        trust = data.getInt("trust");
        culture = data.getInt("culture");
        educationLevel = data.getInt("educationLevel");
        return null;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}
