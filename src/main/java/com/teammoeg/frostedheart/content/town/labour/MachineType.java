package com.teammoeg.frostedheart.content.town.labour;

import com.teammoeg.chorda.io.registry.TypeRegistry;

public interface MachineType {
	public static TypeRegistry<MachineType> registry=new TypeRegistry<>();
    LevelCostTable getCost();
    public static <A extends MachineType> void register(MachineType cls, String type) {
    	registry.register(cls, type);
    }
}
