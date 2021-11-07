package com.teammoeg.frostedheart.research;

import java.util.ArrayList;

public class ResearchInstance {
	private Research base;
	private boolean isCompleted;
	private int points;
	private ArrayList<ClueInstance> Clues=new ArrayList<>();
	public void onClueEvent(ClueMessage cm) {

	};
}
