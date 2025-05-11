package com.teammoeg.frostedheart.content.town.resource;

/**
 * 作为一个接口，承载了{@link TownResourceActions.ItemResourceKeyCostActionResult}和{@link TownResourceActions.VirtualResourceKeyActionResult}共有的一些特性，在{@link TownResourceActions.TownResourceTypeCostAction}中使用
 */
public interface ITownResourceKeyActionResult extends IResourceActionResult{
    double getAmount();

    int getLevel();

    ITownResourceKey getTownResourceKey();

    double residualAmount();

    double totalModifiedAmount();
}
