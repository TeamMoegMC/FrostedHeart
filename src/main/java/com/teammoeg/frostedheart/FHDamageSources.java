/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;

public class FHDamageSources {
    public static final DamageSource HYPOTHERMIA = (new DamageSource("hypothermia")).setDamageBypassesArmor().setDamageIsAbsolute();
    public static final DamageSource HYPERTHERMIA = (new DamageSource("hyperthermia")).setDamageBypassesArmor().setDamageIsAbsolute();
    public static final DamageSource BLIZZARD = (new DamageSource("blizzard")).setDamageBypassesArmor().setDamageIsAbsolute();
    public static final DamageSource RAD = (new DamageSource("radiation")).setDamageBypassesArmor().setDamageIsAbsolute();
    public static final DamageSource HYPOTHERMIA_INSTANT = (new DamageSource("hypothermia_instant")).setDamageBypassesArmor();
    public static final DamageSource HYPERTHERMIA_INSTANT = (new DamageSource("hyperthermia_instant")).setDamageBypassesArmor();

    public static DamageSource hypothermiaFrom(Entity e) {
        return (new DamageSource("hypothermia") {

            @Override
            public Entity getTrueSource() {
                return e;
            }

        }).setDamageBypassesArmor();
    }

    public static DamageSource hyperthermiaFrom(Entity e) {
        return (new DamageSource("hyperthermia") {

            @Override
            public Entity getTrueSource() {
                return e;
            }

        }).setDamageBypassesArmor();
    }
}
