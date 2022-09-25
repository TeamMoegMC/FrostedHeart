package com.teammoeg.frostedheart.mixin.ftb;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.gui.quests.QuestScreen;
@Mixin(QuestScreen.class)
public abstract class QuestScreenMixin extends BaseScreen {
	@Shadow(remap=false)
	public ClientQuestFile file;
	public QuestScreenMixin() {
	}

	@Override
	public boolean doesGuiPauseGame() {
		return file.pauseGame;
	}

}
