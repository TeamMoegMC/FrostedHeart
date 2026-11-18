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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.chorda.client.cui.menu.CUISlot;
import com.teammoeg.chorda.client.cui.menu.CUISlotItemHandler;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedresearch.FRContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 实验台菜单：「材料」下一排三个材料槽，玩家物品栏沿用绘图台左侧面板的格子。
 * Experiment table menu: a row of three material slots under the materials label, and the player inventory on the
 * drawing desk panel's grid.
 */
public class ExperimentTableMenu extends CBlockEntityMenu<ExperimentTableBlockEntity> {
    /** Material slots sit in a row on the panel's paper; the screen draws their frames. */
    public static final int[][] MATERIAL_POSITIONS = {{21, 34}, {41, 34}, {61, 34}};

    public static final int[][] OUTPUT_POSITIONS = {{21, 68}, {41, 68}, {61, 68}, {81, 68}};

    @Override
    public QuickMoveStackBuilder defineQuickMoveStack() {
        return QuickMoveStackBuilder.first(0, MATERIAL_POSITIONS.length);
    }

    public ExperimentTableMenu(int id, Inventory inventory, ExperimentTableBlockEntity table) {
        super(FRContents.MenuTypes.EXPERIMENT_TABLE.get(), table, id, inventory.player, MATERIAL_POSITIONS.length + OUTPUT_POSITIONS.length);
        for (int i = 0; i < ExperimentTableBlockEntity.MATERIAL_SLOTS; i++)
            addSlot(new CUISlotItemHandler(table.getInventory(), i, MATERIAL_POSITIONS[i][0], MATERIAL_POSITIONS[i][1]));
        for (int i = 0; i < OUTPUT_POSITIONS.length; i++)
            addSlot(new CUISlotItemHandler(table.getOutputs(), i, OUTPUT_POSITIONS[i][0], OUTPUT_POSITIONS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    table.setChanged();
                }
            });
        for (int i = 0; i < 36; i++) {
            int position = i < 9 ? i + 27 : i - 9;
            addSlot(new CUISlot(inventory, i, 10 + (position % 6) * 17, 93 + (position / 6) * 17));
        }
    }
}
