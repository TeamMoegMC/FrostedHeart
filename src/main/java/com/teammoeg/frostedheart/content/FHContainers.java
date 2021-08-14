package com.teammoeg.frostedheart.content;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.container.ElectrolyzerContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHContainers {
    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(
            ForgeRegistries.CONTAINERS, FHMain.MODID);
    public static final RegistryObject<ContainerType<ElectrolyzerContainer>> ELECTROLYZER_CONTAINER = CONTAINERS
            .register("electrolyzer_container", () -> new ContainerType<>(ElectrolyzerContainer::new));
}
