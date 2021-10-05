package com.teammoeg.frostedheart.util;

import net.minecraftforge.fml.ModList;

import java.util.Optional;
import java.util.function.Supplier;

public enum Mods {
    DIET,
    CREATE,
    IMMERSIVEENGINEERING,
    RANKINE,
    PRIMALWINTER,
    THEONEPROBE,
    STEAMPOWERED,
    FROSTEDHEART,
    CURIOS,
    JEI;

    /**
     * @return a boolean of whether the mod is loaded or not based on mod id
     */
    public boolean isLoaded() {
        return ModList.get().isLoaded(asId());
    }

    /**
     * @return the mod id
     */
    public String asId() {
        return Lang.asId(name());
    }

    /**
     * Simple hook to run code if a mod is installed
     * @param toRun will be run only if the mod is loaded
     * @return Optional.empty() if the mod is not loaded, otherwise an Optional of the return value of the given supplier
     */
    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        if (isLoaded())
            return Optional.of(toRun.get().get());
        return Optional.empty();
    }
}
