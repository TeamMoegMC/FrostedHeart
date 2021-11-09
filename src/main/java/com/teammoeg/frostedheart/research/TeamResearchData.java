package com.teammoeg.frostedheart.research;

import java.util.ArrayList;

public class TeamResearchData {
	ArrayList<Boolean> clueComplete=new ArrayList<>();
	ArrayList<ResearchData> rdata=new ArrayList<>();
	public void triggerClue(int id) {
		ensureClue(id);
		clueComplete.set(id-1,true);
	}
	public void triggerClue(AbstractClue clue) {
		triggerClue(clue.getRId());
	}
	public void triggerClue(String lid) {
		triggerClue(FHResearch.clues.getByName(lid));
	}
	public void ensureClue(int len) {
		clueComplete.ensureCapacity(len);
		while(clueComplete.size()<len)
			clueComplete.add(false);
	}
	public boolean isClueTriggered(int id){
		if(clueComplete.size()<=id) {
			Boolean b=clueComplete.get(id-1);
			if(b!=null&&b==true)
				return true;
		}
		return false;
	}
}
