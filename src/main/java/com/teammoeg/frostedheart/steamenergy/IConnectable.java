package com.teammoeg.frostedheart.steamenergy;

import net.minecraft.util.Direction;

public interface IConnectable {
    boolean disconnectAt(Direction to);

    boolean connectAt(Direction to);
}
