package com.teammoeg.frostedresearch.knowledge.item;

import com.teammoeg.frostedresearch.knowledge.experiment.TrialKilnSamples;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class TrialKilnSampleItem extends Item {
    public TrialKilnSampleItem(Properties properties) { super(properties); }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        super.appendHoverText(stack, level, lines, flags);
        lines.add(Component.translatable("tooltip.frostedresearch.trial_kiln." + TrialKilnSamples.variant(stack)).withStyle(ChatFormatting.GRAY));
    }
}
