package com.teammoeg.frostedheart.research.gui.drawdesk.game;

public class CardPos{
	final int x;
	final int y;
	private int hash=0;
	private static CardPos[][] cache=new CardPos[11][11];
	static {
		for(int i=0;i<11;i++)
			for(int j=0;j<11;j++)
				cache[i][j]=new CardPos(i-1,j-1);
		
	}
	public static CardPos valueOf(int x,int y) {
		int i=x+1;
		int j=y+1;
		if(i<cache.length&&i>=0&&j<cache[x].length&&j>=0)
			return cache[i][j];
		return new CardPos(x,y);
	}
	private CardPos(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	@Override
	public int hashCode() {
		if(hash==0) {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			hash=result;
		}
		return hash;
		
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CardPos other = (CardPos) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "CardPos [x=" + x + ", y=" + y + ", hash=" + hash + "]";
	}
	
}