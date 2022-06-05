package com.teammoeg.frostedheart.research.gui.drawdesk.game;

import java.util.function.Consumer;

public enum BroadStyles {
	RECT0(cards->{
		for(int i=2;i<=3;i++)
			for(int j=2;j<=3;j++)
				cards[i][j].unplacable=true;
		for(int i=5;i<=6;i++)
			for(int j=2;j<=3;j++)
				cards[i][j].unplacable=true;
		for(int i=2;i<=3;i++)
			for(int j=5;j<=6;j++)
				cards[i][j].unplacable=true;
		for(int i=5;i<=6;i++)
			for(int j=5;j<=6;j++)
				cards[i][j].unplacable=true;
	},5),
	RECT1(cards->{
		for(int i=0;i<=1;i++){
			cards[0][i].unplacable=true;
			cards[1][i].unplacable=true;
			cards[9-1][i].unplacable=true;
			cards[9-2][i].unplacable=true;
		}
		for(int i=9-2;i<=9-1;i++){
			cards[0][i].unplacable=true;
			cards[1][i].unplacable=true;
			cards[9-1][i].unplacable=true;
			cards[9-2][i].unplacable=true;
		}
		for(int i=4-1;i<=4+1;i++)
			for(int j=4-1;j<=4+1;j++)
				if(i!=4||j!=4){
					cards[i][j].unplacable=true;
				}
	},5),
	RECT2(cards->{
		for(int i=4-1;i<=4+1;i++)
			for(int j=4-1;j<=4+1;j++)
				if(i!=4||j!=4){
					cards[i][j].unplacable=true;
				}
		
		for(int i=4-2;i<=4+2;i++){
			if(i!=4){
				cards[i][1].unplacable=true;
				cards[i][9-2].unplacable=true;
				cards[1][i].unplacable=true;
				cards[9-2][i].unplacable=true;
			}
		}
	},5),
	RECT3(cards->{
		for(int i=4-1;i<=4+1;i++){
			if(i!=4){
				cards[i][2].unplacable=true;
				cards[i][9-3].unplacable=true;
				cards[2][i].unplacable=true;
				cards[9-3][i].unplacable=true;
				cards[i][1].unplacable=true;
				cards[i][9-2].unplacable=true;
				cards[1][i].unplacable=true;
				cards[9-2][i].unplacable=true;
			}
		}
		cards[1][1].unplacable=true;
		cards[1][9-2].unplacable=true;
		cards[9-2][1].unplacable=true;
		cards[9-2][9-2].unplacable=true;
	},5);
	final Consumer<Card[][]> deployer;
	
	int tier;
	
	private BroadStyles(Consumer<Card[][]> deployer, int tier) {
		this.deployer = deployer;
		this.tier = tier;
	}
	public void deploy(Card[][] cs) {
		for(int i=0;i<9;i++)
			for(int j=0;j<9;j++)
				cs[i][j].unplacable=false;
		deployer.accept(cs);
	}
}
