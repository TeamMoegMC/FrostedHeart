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
