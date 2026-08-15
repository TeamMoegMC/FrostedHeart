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

package com.teammoeg.frostedheart.content.town.tabs;

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.VirtualItemGridElement;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseMenu;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseScreen;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseSortMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public class TownResourceTab extends AbstractTownTab<WarehouseMenu> {

    public TownResourceTab(AbstractTownWorkerBlockScreen<WarehouseMenu> screen) {
        super(screen);
    }

    @Override
    public void build(UILayer layer) {
        WarehouseMenu menu = getMenu();
        VirtualItemGridElement grid = new VirtualItemGridElement(layer, 7, 18,
                menu::getResources
        );
        layer.add(grid);

        //物品名搜索框：输入实时过滤；右键清空；Esc/回车取消焦点
        //搜索词持久化在 menu 中，Tab 切换重建时恢复，避免刷新丢失
        TextBox searchBox = new TextBox(layer) {
            @Override
            public void onTextChanged() {
                menu.setSearchText(getText());
            }
        };
        searchBox.ghostText = Component.translatable("gui.frostedheart.warehouse.search_hint").getString();
        searchBox.setMaxLength(64);
        searchBox.setText(menu.getSearchText(), false);
        searchBox.setPosAndSize(7, 1, 104, 14);
        layer.add(searchBox);

        //排序切换按钮：数量↓ → 数量↑ → 名称↑ → 名称↓ 循环
        TextButton sortButton = new TextButton(layer, menu.getSortMode().label(), CIcons.nop()) {
            @Override
            public void onClicked(MouseButton button) {
                WarehouseSortMode next = menu.getSortMode().next();
                menu.setSortMode(next);
                setTitle(next.label());
            }

            @Override
            public void getTooltip(TooltipBuilder tooltip) {
                tooltip.accept(Component.translatable("gui.frostedheart.warehouse.sort.tooltip"));
            }
        };
        //宽度由标题自适应，仅需设置位置
        sortButton.setPos(115, 0);
        layer.add(sortButton);
    }

    @Override
    public CIcons.CIcon getIcon() {
        return WarehouseScreen.INACTIVE_TAB;
    }

    @Override
    public CIcons.CIcon getActiveIcon() {
        return WarehouseScreen.ACTIVE_TAB;
    }

    @Override
    public CIcons.CIcon getContentIcon() {
        return CIcons.getIcon(Items.CHEST);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.frostedheart.warehouse.inventory");
    }
}
