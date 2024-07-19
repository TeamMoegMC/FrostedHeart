/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorContainer;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1.T1GeneratorTileEntity;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorContainer;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2.T2GeneratorTileEntity;
import com.teammoeg.frostedheart.content.decoration.RelicChestContainer;
import com.teammoeg.frostedheart.content.decoration.RelicChestTileEntity;
import com.teammoeg.frostedheart.content.incubator.HeatIncubatorTileEntity;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Container;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Container;
import com.teammoeg.frostedheart.content.incubator.IncubatorTileEntity;
import com.teammoeg.frostedheart.content.research.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedheart.content.research.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedheart.content.steamenergy.HeatStatContainer;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaContainer;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaTileEntity;
import com.teammoeg.frostedheart.content.trade.gui.TradeContainer;

import blusunrize.immersiveengineering.common.gui.GuiHandler;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHContainer {

    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS,
            FHMain.MODID);
    public static final RegistryObject<ContainerType<TradeContainer>> TRADE_GUI = CONTAINERS.register("trade", () -> IForgeContainerType.create(TradeContainer::new));
    public static final RegistryObject<ContainerType<HeatStatContainer>> HEAT_STAT = CONTAINERS.register("heat_stat", () -> IForgeContainerType.create(HeatStatContainer::new));
    public static void registerContainers() {
        GuiHandler.register(T1GeneratorTileEntity.class, new ResourceLocation(FHMain.MODID, "generator"), T1GeneratorContainer::new);
        GuiHandler.register(T2GeneratorTileEntity.class, new ResourceLocation(FHMain.MODID, "generator_t2"), T2GeneratorContainer::new);

        GuiHandler.register(RelicChestTileEntity.class, new ResourceLocation(FHMain.MODID, "relic_chest"), RelicChestContainer::new);
        GuiHandler.register(DrawingDeskTileEntity.class, new ResourceLocation(FHMain.MODID, "draw_desk"), DrawDeskContainer::new);
        GuiHandler.register(SaunaTileEntity.class, new ResourceLocation(FHMain.MODID, "sauna_vent"), SaunaContainer::new);
        GuiHandler.register(IncubatorTileEntity.class, new ResourceLocation(FHMain.MODID, "incubator"), IncubatorT1Container::new);
        GuiHandler.register(HeatIncubatorTileEntity.class, new ResourceLocation(FHMain.MODID, "heat_incubator"), IncubatorT2Container::new);

    }
}
