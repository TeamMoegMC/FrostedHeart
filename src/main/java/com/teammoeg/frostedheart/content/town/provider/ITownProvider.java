package com.teammoeg.frostedheart.content.town.provider;

import com.teammoeg.frostedheart.content.town.ITown;

/**
 * provide a town of this type
 * @param <T> town type
 */
public interface ITownProvider <T extends ITown>{
    T getTown();
}
