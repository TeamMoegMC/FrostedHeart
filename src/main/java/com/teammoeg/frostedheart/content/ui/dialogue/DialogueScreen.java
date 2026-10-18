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

package com.teammoeg.frostedheart.content.ui.dialogue;

import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.chorda.client.cui.widgets.Button;

import java.util.Collection;
import java.util.List;

public class DialogueScreen extends PrimaryLayer {

    public DialogueScreen() {
        setRenderGradient(false);
    }

    public static void open(boolean closeable, Button... buttons) {
        open(closeable, List.of(buttons));
    }

    public static void open(boolean closeable, Collection<Button> buttons) {
        DialogueOverlay.INSTANCE.open(closeable, buttons);
        CUIScreenWrapper.open(new DialogueScreen());
    }

    @Override
    public void onClosed() {
        super.onClosed();
        DialogueOverlay.INSTANCE.close();
    }
}
