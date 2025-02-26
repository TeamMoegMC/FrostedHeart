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
