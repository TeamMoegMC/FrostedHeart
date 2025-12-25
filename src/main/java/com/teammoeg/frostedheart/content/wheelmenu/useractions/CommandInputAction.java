package com.teammoeg.frostedheart.content.wheelmenu.useractions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.wheelmenu.Action;
import com.teammoeg.frostedheart.content.wheelmenu.Selection;

import net.minecraft.SharedConstants;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public record CommandInputAction(String command) implements Action{
	public static final MapCodec<CommandInputAction> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		Codec.STRING.fieldOf("command").forGetter(CommandInputAction::command)
		).apply(t,CommandInputAction::new));
	public String getCommand() {
		return command;
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute(Selection selection) {
		 String s1 = SharedConstants.filterText(command);
         if (s1.startsWith("/")) {
            if (!ClientUtils.getLocalPlayer().connection.sendUnsignedCommand(s1.substring(1))) {
               FHMain.LOGGER.error("Not allowed to run unsigned command '{}' from wheelmenu action", s1);
            }
         } else {
        	 FHMain.LOGGER.error("Failed to run command without '/' prefix from wheelmenu action: '{}'", (Object)s1);
         }
	}

}