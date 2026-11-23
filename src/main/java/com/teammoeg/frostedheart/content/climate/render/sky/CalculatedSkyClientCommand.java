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

package com.teammoeg.frostedheart.content.climate.render.sky;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.StringJoiner;

import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.literal;
import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.registerCommand;

/**
 * Client command that turns the calculated sky on and off, changes its settings in the client config, and records and
 * takes screenshot sets through CalculatedSkyScreenshots.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CalculatedSkyClientCommand {
    /**
     * Whether /calculatedSky is registered. Without it the sky follows the client config alone, and the screenshot
     * harness never runs.
     */
    public static final boolean ENABLED = true;

    private CalculatedSkyClientCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!ENABLED) {
            return;
        }
        LiteralArgumentBuilder<CommandSourceStack> set = literal("set");
        for (SkySetting setting : SkySetting.values()) {
            set.then(setting(setting));
        }
        var command = literal("calculatedSky")
                .executes(CalculatedSkyClientCommand::status)
                .then(literal("on").executes(context -> setEnabled(context, true)))
                .then(literal("off").executes(context -> setEnabled(context, false)))
                .then(literal("reset").executes(CalculatedSkyClientCommand::reset))
                .then(literal("pillars").executes(CalculatedSkyClientCommand::pillars)
                        .then(literal("fill").executes(CalculatedSkyClientCommand::toggleFillView))
                        .then(literal("debug").then(Commands.argument("mode",
                                IntegerArgumentType.integer(0, LightPillarRenderer.LAST_DEBUG_MODE))
                                .executes(CalculatedSkyClientCommand::setDebugMode))))
                .then(literal("dust").executes(CalculatedSkyClientCommand::dust))
                .then(set);
        CalculatedSkyScreenshots.addCommands(command);
        registerCommand(event.getDispatcher(), command);
    }

    /**
     * The subcommand of set for one setting, which takes true or false for a switch and a number in range otherwise.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> setting(SkySetting setting) {
        if (setting.isSwitch()) {
            return literal(setting.key()).then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(context -> set(context, setting,
                            BoolArgumentType.getBool(context, "value") ? 1.0 : 0.0)));
        }
        return literal(setting.key())
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(setting.minimum(), setting.maximum()))
                        .executes(context -> set(context, setting, DoubleArgumentType.getDouble(context, "value"))));
    }

    private static int set(CommandContext<CommandSourceStack> context, SkySetting setting, double value) {
        setting.set(value);
        setting.save();
        return status(context);
    }

    private static int setEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        FHConfig.CLIENT.calculatedSky.set(enabled);
        FHConfig.CLIENT.calculatedSky.save();
        if (enabled) {
            CalculatedSkyRenderer.clearFailure();
            LightPillarSources.clearFailure();
            LightPillarRenderer.clearFailure();
            DustGlitterRenderer.clearFailure();
        }
        return status(context);
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        SkySettings.DEFAULT.applyToConfig();
        // Saving one value writes the whole client config file:
        SkySetting.EXPOSURE.save();
        return status(context);
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }
        SkySettings settings = SkySettings.fromConfig();
        double megatons = settings.nanobotOpticalDepth() * SkyParticleOptics.forDistribution(
                settings.nanobotMedianNanometers(), settings.nanobotSpread()).megatonsForUnitOpticalDepth();
        send(context, String.format(Locale.ROOT, "Calculated sky: %s", CalculatedSkyRenderer.describeState(level)));
        StringJoiner values = new StringJoiner(", ");
        for (SkySetting setting : SkySetting.values()) {
            double value = settings.get(setting);
            values.add(setting.key() + " " + (setting.isSwitch() ? Boolean.toString(value != 0.0)
                    : Double.toString(value)));
        }
        send(context, values.toString());
        send(context, String.format(Locale.ROOT, "Nanobots about %.0f Mt, texels drawn at %.3f degrees", megatons,
                settings.actualTexelDegrees()));
        send(context, IceWeatherDepth.describe(Minecraft.getInstance().getFrameTime()));
        send(context, DiamondDustExtinction.describe(Minecraft.getInstance().getFrameTime()));
        send(context, LightPillarSources.describe(level));
        send(context, String.format(Locale.ROOT,
                "Last build: tables %.1f ms, halo profile %.1f ms, halo map %.1f ms, sky view %.1f ms",
                CalculatedSkyRenderer.lastTablesMilliseconds(), CalculatedSkyRenderer.lastHaloProfileMilliseconds(),
                CalculatedSkyRenderer.lastHaloMapMilliseconds(), CalculatedSkyRenderer.lastViewMilliseconds()));
        send(context, "Sky draw: " + CalculatedSkyRenderer.describeDrawTime());
        return Command.SINGLE_SUCCESS;
    }

    private static int pillars(CommandContext<CommandSourceStack> context) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }
        send(context, LightPillarSources.describe(level));
        send(context, LightPillarWorldChanges.describe());
        send(context, LightPillarWorldChanges.describeBlocks());
        send(context, LightPillarRenderer.describe());
        for (String line : LightPillarSources.describePillars(
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())) {
            send(context, line);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int dust(CommandContext<CommandSourceStack> context) {
        send(context, DiamondDustExtinction.describe(Minecraft.getInstance().getFrameTime()));
        send(context, DustGlitterRenderer.describe());
        send(context, DustColumns.describe());
        send(context, DustLights.describe());
        return Command.SINGLE_SUCCESS;
    }

    private static int toggleFillView(CommandContext<CommandSourceStack> context) {
        send(context, LightPillarRenderer.toggleFillView()
                ? "Light pillar fill view on: green pixels show light, red ones are computed for nothing, and "
                        + "overlapping ribbons add up"
                : "Light pillar fill view off");
        return Command.SINGLE_SUCCESS;
    }

    private static int setDebugMode(CommandContext<CommandSourceStack> context) {
        int mode = IntegerArgumentType.getInteger(context, "mode");
        String leftOut = LightPillarRenderer.setDebugMode(mode);
        send(context, mode > 0 ? "Light pillar debug mode " + mode + " leaves out " + leftOut
                : "Light pillar debug mode off");
        return Command.SINGLE_SUCCESS;
    }

    private static void send(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }
}
