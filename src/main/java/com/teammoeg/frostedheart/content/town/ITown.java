/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.town;

/**
 * 融合了ITownWithResources, ITownWithBuildings, ITownWithResidents三个城镇功能的接口。
 * <br>
 * 一个具有完整功能的标准城镇应该继承这个接口。
 */
public interface ITown extends ITownWithResources, ITownWithBuildings, ITownWithResidents {

    /**
     * Debug mode.
     * Should be deleted or turned to false when released.
     */
    public static final boolean DEBUG_MODE = true;//todo: 正式发布记得删掉

}
