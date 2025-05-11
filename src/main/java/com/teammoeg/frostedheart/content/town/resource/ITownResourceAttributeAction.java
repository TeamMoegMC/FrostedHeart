package com.teammoeg.frostedheart.content.town.resource;

import com.teammoeg.frostedheart.content.town.resource.actionattributes.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.actionattributes.ResourceActionType;

public interface ITownResourceAttributeAction extends ITownResourceAction {
    @Override
    ITownResourceAttributeActionResult apply(TownResourceHolder resourceHolder);

    static ITownResourceAttributeAction create(ITownResourceAttribute resourceAttribute, double amount, ResourceActionMode actionMode){
        if(resourceAttribute instanceof ItemResourceAttribute itemResourceKey){
            return new TownResourceActions.ItemResourceAttributeCostAction(itemResourceKey, amount, actionMode);
        } else if(resourceAttribute instanceof VirtualResourceAttribute virtualResourceKey){
            return new TownResourceActions.VirtualResourceAttributeAction(virtualResourceKey, amount, ResourceActionType.COST, actionMode);
        }
        else throw new IllegalArgumentException("resourceAttribute must be ItemResourceAttribute or VirtualResourceAttribute");//如果添加其它TownResourceAttribute的话，要在这里加上
    }
}
