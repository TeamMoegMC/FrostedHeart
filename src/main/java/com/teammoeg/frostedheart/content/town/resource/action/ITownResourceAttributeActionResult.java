package com.teammoeg.frostedheart.content.town.resource.action;

import com.teammoeg.frostedheart.content.town.resource.ITownResourceAttribute;

/**
 * 作为一个接口，承载了{@link TownResourceActionResults.ItemResourceAttributeCostActionResult}和{@link TownResourceActionResults.VirtualResourceAttributeActionResult}共有的一些特性，在{@link TownResourceActions.TownResourceTypeCostAction}中使用
 */
public interface ITownResourceAttributeActionResult<T extends ITownResourceAttributeAction> extends ITownResourceActionResult<T> {
    boolean allModified();

    double getAmount();

    int getLevel();

    ITownResourceAttribute getTownResourceAttribute();

    double residualAmount();

    double totalModifiedAmount();
}
