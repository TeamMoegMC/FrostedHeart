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

package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.creativeTab.ICreativeModeTabItem;
import com.teammoeg.frostedheart.util.creativeTab.TabType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class FHTabs {
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FHMain.MODID);
    public static final RegistryObject<CreativeModeTab> main = TABS.register("frostedheart_main",
            ()->CreativeModeTab
                    .builder()
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(()->new ItemStack(FHItems.energy_core.get()))
                    .title(TranslateUtils.translate("itemGroup.frostedheart"))
                    .displayItems(FHTabs::fillFHTab)
                    .build());
    public static final TabType itemGroup = new TabType(main);

    public static void fillFHTab(CreativeModeTab.ItemDisplayParameters parms, CreativeModeTab.Output out) {
        for (final RegistryObject<Item> itemRef : FHItems.registry.getEntries()) {
            final Item item = itemRef.get();
            if (item instanceof ICreativeModeTabItem) {
                continue;
            }
            else
                out.accept(itemRef.get());
        }
    }
}
