package com.teammoeg.frostedheart.content.robotics;

import com.teammoeg.chorda.io.registry.TypeRegistry;

public interface MachineType {
	public static record BaseMachineType(LevelCostTable cost) implements MachineType{

	    public BaseMachineType(int... costs) {
	    	this(new LevelCostTable(costs));
	    }
		@Override
		public int getCost(int level) {
			return cost().cost(level);
		}

		@Override
		public int maxLevelForValue(int level) {
			return cost().maxLevelForValue(level);
		}

		@Override
		public int maxLevel() {
			return cost().maxLevel();
		}}
	public static TypeRegistry<MachineType> registry=new TypeRegistry<>();
    int getCost(int level);
    int maxLevelForValue(int level);
    int maxLevel();
    public static <A extends MachineType> A register(A cls, String type) {
    	registry.register(cls, type);
    	return cls;
    }
}
