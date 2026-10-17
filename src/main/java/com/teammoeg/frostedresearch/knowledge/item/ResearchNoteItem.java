package com.teammoeg.frostedresearch.knowledge.item;

import java.util.List;
import javax.annotation.Nullable;

import com.teammoeg.frostedresearch.item.FRBaseItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class ResearchNoteItem extends FRBaseItem {
    public ResearchNoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return ResearchNotes.isBlank(stack) ? 64 : 1;
    }

    @Override
    public Component getName(ItemStack stack) {
        return ResearchNotes.read(stack).map(ResearchNotes::title).orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(ResearchNotes.isBlank(stack) ? "knowledge.frostedresearch.note_blank" : "knowledge.frostedresearch.note_read_at_desk")
                .withStyle(ChatFormatting.GRAY));
        ResearchNotes.read(stack).ifPresent(content -> {
            if (!content.remarks().isBlank())
                lines.add(Component.literal(content.remarks()).withStyle(ChatFormatting.GRAY));
        });
    }
}
