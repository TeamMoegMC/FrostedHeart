package com.teammoeg.frostedheart.util;

import net.minecraft.util.DamageSource;

public class FHDamageSources {
    public static final DamageSource HYPOTHERMIA = (new DamageSource("hypothermia")).setDamageBypassesArmor().setDifficultyScaled();
    public static final DamageSource HYPERTHERMIA = (new DamageSource("hyperthermia")).setDamageBypassesArmor().setDifficultyScaled();
}
