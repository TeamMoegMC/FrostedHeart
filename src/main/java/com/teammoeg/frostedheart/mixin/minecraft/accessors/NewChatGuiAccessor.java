package com.teammoeg.frostedheart.mixin.minecraft.accessors;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;

@Mixin(ChatComponent.class)
public interface NewChatGuiAccessor {
    @Accessor
    List<GuiMessage.Line> getTrimmedMessages();
}
