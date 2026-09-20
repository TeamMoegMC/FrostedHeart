package com.teammoeg.frostedheart.content.robotics;

import com.teammoeg.frostedheart.content.robotics.MachineType.BaseMachineType;

public class MachineTypes {
	public static final MachineType LOGISTIC=MachineType.register(new BaseMachineType(0,5,10,15,20,25,30,35,40,45,50), "logistic");
	public static void init() {
		
	}
}
