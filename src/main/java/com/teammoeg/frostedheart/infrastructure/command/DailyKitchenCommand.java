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

package com.teammoeg.frostedheart.infrastructure.command;

import java.util.Set;
import java.util.function.Consumer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.health.dailykitchen.WantedFoodCapability;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 每日厨房查询指令：查看当前玩家每日厨房（DailyKitchen）能力的数据。
 * <p>
 * 子命令 / Sub-commands:
 * <ul>
 *   <li>{@code foods} 吃过的所有食物（foodsEaten） / all foods the player has eaten</li>
 *   <li>{@code wanted} 今日想吃的菜（wantedFoods） / today's wanted foods</li>
 *   <li>{@code amount} 计数（eatenFoodsAmount / eatenTimes） / counters</li>
 * </ul>
 * <p>
 * Daily kitchen query command: inspects the current player's DailyKitchen capability data.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DailyKitchenCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> dailyKitchen = Commands.literal("dailykitchen")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("foods").executes(ct -> show(ct, cap ->
                        sendItemList(ct.getSource(), "吃过的食物 foodsEaten(" + cap.getFoodsEaten().size() + ")", cap.getFoodsEaten()))))
                .then(Commands.literal("wanted").executes(ct -> show(ct, cap ->
                        sendItemList(ct.getSource(), "今日想吃的菜 wantedFoods(" + cap.getWantedFoods().size() + ")", cap.getWantedFoods()))))
                .then(Commands.literal("amount").executes(ct -> show(ct, cap ->
                        ct.getSource().sendSuccess(() -> Components.str("eatenFoodsAmount=" + cap.getEatenFoodsAmount()
                                + ", eatenTimes=" + cap.getEatenTimes()), false))));
        // 注册到 frostedheart / fh / twr 三个根命令下（与 NutritionCommand 等一致）
        // Registered under all three root commands, consistent with NutritionCommand etc.
        for (String root : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(root).requires(s -> s.hasPermission(2)).then(dailyKitchen));
        }
    }

    private static int show(CommandContext<CommandSourceStack> ctx, Consumer<WantedFoodCapability> consumer) throws CommandSyntaxException {
        WantedFoodCapability cap = FHCapabilities.WANTED_FOOD.getCapability(ctx.getSource().getPlayerOrException()).orElse(null);
        if (cap == null) {
            ctx.getSource().sendFailure(Components.str("WANTED_FOOD capability 不可用（仅服务端玩家）"));
        } else {
            consumer.accept(cap);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void sendItemList(CommandSourceStack source, String title, Set<Item> items) {
        source.sendSuccess(() -> Components.str(title).withStyle(ChatFormatting.AQUA), false);
        if (items.isEmpty()) {
            source.sendSuccess(() -> Components.str("  (empty)").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        StringBuilder sb = new StringBuilder("  ");
        for (Item item : items) {
            if (sb.length() > 2) sb.append(", ");
            sb.append(CRegistryHelper.getRegistryName(item));
        }
        source.sendSuccess(() -> Components.str(sb.toString()), false);
    }
}
