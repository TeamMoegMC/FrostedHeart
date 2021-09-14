package com.teammoeg.frostedheart.steamenergy;

import net.minecraft.util.Direction;

public interface IConnectable {
	void disconnectAt(Direction to);
	void connectAt(Direction to);
}
