package com.teammoeg.frostedheart.content.town.provider;

import com.teammoeg.frostedheart.content.town.Town;

/**
 * provide a town of this type
 * @param <T> town type
 */
public interface ITownProvider <T extends Town>{
    T getTown();
}
