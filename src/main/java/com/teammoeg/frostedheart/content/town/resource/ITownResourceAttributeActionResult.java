package com.teammoeg.frostedheart.content.town.resource;

/**
 * 作为一个接口，承载了{@link TownResourceActions.ItemResourceAttributeCostActionResult}和{@link TownResourceActions.VirtualResourceAttributeActionResult}共有的一些特性，在{@link TownResourceActions.TownResourceTypeCostAction}中使用
 */
public interface ITownResourceAttributeActionResult extends IResourceActionResult{
    double getAmount();

    int getLevel();

    ITownResourceAttribute getTownResourceAttribute();

    double residualAmount();

    double totalModifiedAmount();
}
