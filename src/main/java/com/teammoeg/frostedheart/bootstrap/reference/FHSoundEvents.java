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

package com.teammoeg.frostedheart.bootstrap.reference;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHSoundEvents {
    public FHSoundEvents() {
    }
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FHMain.MODID);
    public static final RegistryObject<SoundEvent> MC_BELL = makeReference("mc_bell");
    public static final RegistryObject<SoundEvent> MC_ROLL = makeReference("mc_roll");
    public static final RegistryObject<SoundEvent> ICE_CRACKING = makeReference("ice_cracking");
    public static final RegistryObject<SoundEvent> WIND = makeReference("wind");
    public static final RegistryObject<SoundEvent> TFOA = makeReference("the_fall_of_arcana");
    public static RegistryObject<SoundEvent> makeReference(String name){
    	return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(FHMain.rl(name)));
    }
}
