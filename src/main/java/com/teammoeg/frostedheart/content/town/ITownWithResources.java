package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.content.town.resource.action.IActionExecutorHandler;

public interface ITownWithResources {
    /**
     * Get the {@link IActionExecutorHandler} of this town, which is used to execute resource modifying actions.
     *
     * @return the action executor handler
     */
    IActionExecutorHandler getActionExecutorHandler();
}
