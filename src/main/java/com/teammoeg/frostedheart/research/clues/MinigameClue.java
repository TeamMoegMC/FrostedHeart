package com.teammoeg.frostedheart.research.clues;

import com.teammoeg.frostedheart.FHMain;

public class MinigameClue extends CustomClue {

	public MinigameClue(float contribution) {
		super("@clue."+FHMain.MODID+".minigame", contribution);
	}
	@Override
	public String getType() {
		return "game";
	}
}
