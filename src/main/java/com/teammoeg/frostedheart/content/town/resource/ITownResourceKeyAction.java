package com.teammoeg.frostedheart.content.town.resource;

import com.teammoeg.frostedheart.content.town.resource.actionattributes.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.actionattributes.ResourceActionType;

public interface ITownResourceKeyAction extends ITownResourceAction {
    @Override
    ITownResourceKeyActionResult apply(TownResourceHolder resourceHolder);

    static ITownResourceKeyAction create(ITownResourceKey resourceKey, double amount, ResourceActionMode actionMode){
        if(resourceKey instanceof ItemResourceKey itemResourceKey){
            return new TownResourceActions.ItemResourceKeyCostAction(itemResourceKey, amount, actionMode);
        } else if(resourceKey instanceof VirtualResourceKey virtualResourceKey){
            return new TownResourceActions.VirtualResourceKeyAction(virtualResourceKey, amount, ResourceActionType.COST, actionMode);
        }
        else throw new IllegalArgumentException("resourceKey must be ItemResourceKey or VirtualResourceKey");//如果添加其它TownResourceKey的话，要加上
    }
}
