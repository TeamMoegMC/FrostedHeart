package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.util.IReorderingProcessor;
@Mixin(NewChatGui.class)
public interface NewChatGuiAccessor {
	@Accessor
	List<ChatLine<IReorderingProcessor>> getDrawnChatLines(); 
}
