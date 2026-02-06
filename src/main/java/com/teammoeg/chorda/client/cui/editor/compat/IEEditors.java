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

package com.teammoeg.chorda.client.cui.editor.compat;

import com.teammoeg.chorda.client.cui.editor.Editor;
import com.teammoeg.chorda.client.cui.editor.SelectDialog;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;

public class IEEditors {

	public static final Editor<IMultiblock> EDITOR_MULTIBLOCK = (p, l, v, c) -> new SelectDialog<>(p, l, v, c, MultiblockHandler::getMultiblocks,
		SelectDialog.wrap(IMultiblock::getUniqueName)).open();

	private IEEditors() {
	}

}
