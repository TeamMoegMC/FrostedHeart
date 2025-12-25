package com.teammoeg.frostedheart.content.robotics.logistics.grid;

import net.minecraftforge.common.util.LazyOptional;

public record GridAndAmount(LazyOptional<IGridElement> grid,int amount) {

}
