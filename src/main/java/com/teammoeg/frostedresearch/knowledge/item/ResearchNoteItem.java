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
