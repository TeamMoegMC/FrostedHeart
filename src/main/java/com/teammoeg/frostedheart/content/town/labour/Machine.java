package com.teammoeg.frostedheart.content.town.labour;

import net.minecraft.core.GlobalPos;

/**
 * 机器实例接口。管理器只负责推送等级，具体效果由机器自行处理。
 */
public interface Machine {
    GlobalPos getMachineLocation();
    void applyLevel(int level);
    MachineType getType();
    boolean isLoaded();
}