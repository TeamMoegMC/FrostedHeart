package com.teammoeg.frostedheart.content.scenario.commands.client;

import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;
import com.teammoeg.frostedheart.content.tips.ServerTipSender;
import net.minecraft.server.level.ServerPlayer;

public class TipCommand {

    public void displayTip(ScenarioCommandContext runner, @Param("id")String id) {
        ServerTipSender.sendGeneral(id, (ServerPlayer) runner.context().player());
    }
}
