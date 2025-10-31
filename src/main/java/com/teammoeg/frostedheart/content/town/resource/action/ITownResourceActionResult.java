package com.teammoeg.frostedheart.content.town.resource.action;

import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;

public interface ITownResourceActionResult<T extends ITownResourceAction> {
    T getAction();
}
