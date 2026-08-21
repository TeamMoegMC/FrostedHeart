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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceType;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceAttribute;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TownCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var tickManual =
                Commands.literal("tick")
                        .executes(ct -> advanceTown(ct.getSource(), 1))
                        .then(Commands.argument("repeats", IntegerArgumentType.integer(1, 90))
                                .executes(ct -> advanceTown(ct.getSource(),
                                        IntegerArgumentType.getInteger(ct, "repeats"))));

        var spawnRefugees =
                Commands.literal("spawn_refugees")
                        .executes(ct -> {
                            var player = ct.getSource().getPlayer();
                            if (player != null) {
                                var data = TeamTown.from(player).getTownData();
                                if (data.isPresent()) {
                                    data.get().debugSpawnRefugeeBatch(ct.getSource().getLevel(), CTeamDataManager.get(player));
                                    ct.getSource().sendSuccess(() -> Component.literal("Refugee batch spawned"), false);
                                    return Command.SINGLE_SUCCESS;
                                }
                            }
                            ct.getSource().sendFailure(Component.literal("Unable to get your team's data"));
                            return 0;
                        });

        LiteralArgumentBuilder<CommandSourceStack> name =
                Commands.literal("name")
                        .executes(ct -> {
                            TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                            ct.getSource().sendSuccess(()-> Components.str(town.getName()), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSourceStack> listItemStackResources =
                Commands.literal("list_items")
                        .executes(ct -> {
                            TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                            ct.getSource().sendSuccess(()-> Components.str(town.getResourceHolder().getAllItems() ), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSourceStack> listVirtualResources =
                Commands.literal("list_virtual")
                        .executes(ct -> {
                            TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                            //System.out.println(town.getResourceManager().resourceHolder.getAllVirtualResources());
                            ct.getSource().sendSuccess(()-> Components.str(town.getResourceHolder().getAllVirtualResources() ), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSourceStack> listAllResources =
                Commands.literal("list_all")
                        .executes(ct -> {
                            TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                            //System.out.println(town.getResourceManager().resourceHolder.getAllVirtualResources());
                            ct.getSource().sendSuccess(()-> Components.str(town.getResourceHolder().getAllResources() ), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSourceStack> modifyVirtualResources =
                Commands.literal("modifyVirtual")
                        .then(Commands.argument("type", StringArgumentType.string())
                                .suggests((ct, s) -> {
                                    // Get all TownResourceType enum values
                                    Arrays.stream(VirtualResourceType.values()).forEach(t -> s.suggest(t.getKey()));
                                    return s.buildFuture();
                                })
                                .then(Commands.argument("level", IntegerArgumentType.integer())
                                        .suggests((ct, s) -> {
                                            IntStream.rangeClosed(0, VirtualResourceType.from(StringArgumentType.getString(ct, "type")).maxLevel)
                                                    .forEach(s::suggest);
                                            return s.buildFuture();
                                        })
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                .executes(ct -> {
                                                    double amount = DoubleArgumentType.getDouble(ct, "amount");
                                                    String type = StringArgumentType.getString(ct, "type");
                                                    int level = IntegerArgumentType.getInteger(ct, "level");
                                                    ResourceActionType actionType;
                                                    if(amount > 0){
                                                        actionType = ResourceActionType.ADD;
                                                    } else{
                                                        actionType = ResourceActionType.COST;
                                                        amount = -amount;
                                                    }
                                                    VirtualResourceAttribute attribute = VirtualResourceAttribute.of(VirtualResourceType.from(type), level);
                                                    TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                                                    IActionExecutorHandler executor = town.getActionExecutorHandler();
                                                    TownResourceActions.VirtualResourceAttributeAction action = new TownResourceActions.VirtualResourceAttributeAction(attribute, amount, actionType, ResourceActionMode.ATTEMPT);
                                                    TownResourceActionResults.VirtualResourceAttributeActionResult result = executor.execute(action);
                                                    //TownResourceManager.SimpleResourceActionResult result = town.getResourceManager().addIfHaveCapacity(VirtualResourceType.from(type).generateAttribute(level), amount);
                                                    if(result.allModified()){
                                                        ct.getSource().sendSuccess(()-> Components.str("Resource modified."), true);
                                                    } else {
                                                        if(actionType == ResourceActionType.ADD){
                                                            ct.getSource().sendSuccess(()-> Components.str("Resource added failed: No enough capacity."), true);
                                                        } else{
                                                            ct.getSource().sendSuccess(()-> Components.str("Resource cost failed: No enough resource."), true);
                                                        }
                                                    }
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        );

        LiteralArgumentBuilder<CommandSourceStack> costResourceByType =
                Commands.literal("costByType")
                        .then(Commands.argument("type", StringArgumentType.string())
                                .suggests((ct, s) -> {
                                    // Get all TownResourceType enum values
                                    Arrays.stream(VirtualResourceType.values()).forEach(t -> s.suggest(t.getKey()));
                                    Arrays.stream(ItemResourceType.values()).forEach(t -> s.suggest(t.getKey()));
                                    return s.buildFuture();
                                })
                                .then(Commands.argument("minLevel", IntegerArgumentType.integer())
                                        .suggests((ct, s) -> {
                                            ITownResourceType type = ITownResourceType.from(StringArgumentType.getString(ct, "type"));
                                            if(type == null) return s.buildFuture();
                                            IntStream.rangeClosed(0, type.getMaxLevel())
                                                    .forEach(s::suggest);
                                            return s.buildFuture();
                                        })
                                        .then(Commands.argument("maxLevel", IntegerArgumentType.integer())
                                                .suggests((ct, s) -> {
                                                    ITownResourceType type = ITownResourceType.from(StringArgumentType.getString(ct, "type"));
                                                    if(type == null) return s.buildFuture();
                                                    int minLevel = IntegerArgumentType.getInteger(ct, "minLevel");
                                                    IntStream.rangeClosed(minLevel, type.getMaxLevel())
                                                            .forEach(s::suggest);
                                                    return s.buildFuture();
                                                })
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                        .then(Commands.literal("ascending").executes(ct -> {
                                                            double amount = DoubleArgumentType.getDouble(ct, "amount");
                                                            String typeString = StringArgumentType.getString(ct, "type");
                                                            int minLevel = IntegerArgumentType.getInteger(ct, "minLevel");
                                                            int maxLevel = IntegerArgumentType.getInteger(ct, "maxLevel");
                                                            ITownResourceType type = ITownResourceType.from(typeString);
                                                            if(type == null){
                                                                ct.getSource().sendFailure(Components.str("Invalid type"));
                                                                return Command.SINGLE_SUCCESS;
                                                            }
                                                            if(amount < 0){
                                                                ct.getSource().sendFailure(Components.str("Invalid amount: Amount must be positive."));
                                                                return Command.SINGLE_SUCCESS;
                                                            }
                                                            TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                                                            IActionExecutorHandler executor = town.getActionExecutorHandler();
                                                            TownResourceActions.TownResourceTypeCostAction action = new TownResourceActions.TownResourceTypeCostAction(type, amount, minLevel, maxLevel, ResourceActionMode.ATTEMPT, ResourceActionOrder.ASCENDING);
                                                            TownResourceActionResults.TownResourceTypeCostActionResult result = executor.execute(action);
                                                            if(result.allCosted()){
                                                                ct.getSource().sendSuccess(()-> Components.str("Resource costed (ascending)."), true);
                                                            } else ct.getSource().sendSuccess(()-> Components.str("Resource cost failed: No enough resource."), true);
                                                            return Command.SINGLE_SUCCESS;
                                                        }))
                                                        .then(Commands.literal("descending").executes(ct -> {
                                                            double amount = DoubleArgumentType.getDouble(ct, "amount");
                                                            String typeString = StringArgumentType.getString(ct, "type");
                                                            int minLevel = IntegerArgumentType.getInteger(ct, "minLevel");
                                                            int maxLevel = IntegerArgumentType.getInteger(ct, "maxLevel");
                                                            ITownResourceType type = ITownResourceType.from(typeString);
                                                            if(type == null){
                                                                ct.getSource().sendFailure(Components.str("Invalid type"));
                                                                return Command.SINGLE_SUCCESS;
                                                            }
                                                            if(amount < 0){
                                                                ct.getSource().sendFailure(Components.str("Invalid amount: Amount must be positive."));
                                                                return Command.SINGLE_SUCCESS;
                                                            }
                                                            TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                                                            IActionExecutorHandler executor = town.getActionExecutorHandler();
                                                            TownResourceActions.TownResourceTypeCostAction action = new TownResourceActions.TownResourceTypeCostAction(type, amount, minLevel, maxLevel, ResourceActionMode.ATTEMPT, ResourceActionOrder.DESCENDING);
                                                            TownResourceActionResults.TownResourceTypeCostActionResult result = executor.execute(action);
                                                            if(result.allCosted()){
                                                                ct.getSource().sendSuccess(()-> Components.str("Resource costed (descending)."), true);
                                                            } else ct.getSource().sendSuccess(()-> Components.str("Resource cost failed: No enough resource."), true);
                                                            return Command.SINGLE_SUCCESS;
                                                        }))
                                                )
                                        )
                                )
                        );

        LiteralArgumentBuilder<CommandSourceStack> costResource =
                Commands.literal("cost")
                        .then(Commands.argument("type", StringArgumentType.string())
                                .suggests((ct, s) -> {
                                    // Get all TownResourceType enum values
                                    Arrays.stream(VirtualResourceType.values()).forEach(t -> s.suggest(t.getKey()));
                                    Arrays.stream(ItemResourceType.values()).forEach(t -> s.suggest(t.getKey()));
                                    return s.buildFuture();
                                })
                                .then(Commands.argument("level", IntegerArgumentType.integer())
                                        .suggests((ct, s) -> {
                                            ITownResourceType type = ITownResourceType.from(StringArgumentType.getString(ct, "type"));
                                            if(type == null) return s.buildFuture();
                                            IntStream.rangeClosed(0, type.getMaxLevel())
                                                    .forEach(s::suggest);
                                            return s.buildFuture();
                                        })
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                .executes(ct -> {
                                                    double amount = DoubleArgumentType.getDouble(ct, "amount");
                                                    String typeString = StringArgumentType.getString(ct, "type");
                                                    int level = IntegerArgumentType.getInteger(ct, "level");
                                                    ITownResourceType type = ITownResourceType.from(typeString);
                                                    if(type == null){
                                                        ct.getSource().sendFailure(Components.str("Invalid type"));
                                                        return Command.SINGLE_SUCCESS;
                                                    }
                                                    if(amount < 0){
                                                        ct.getSource().sendFailure(Components.str("Invalid amount: Amount must be positive."));
                                                        return Command.SINGLE_SUCCESS;
                                                    }
                                                    TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                                                    IActionExecutorHandler executor = town.getActionExecutorHandler();
                                                    ITownResourceAction<? extends ITownResourceAttributeActionResult<?>> action = TownResourceActions.createAttributeCostAction(type.generateAttribute(level), amount, ResourceActionMode.ATTEMPT);
                                                    ITownResourceAttributeActionResult<?> result = executor.executeFuzzy(action);
                                                    //TownResourceManager.SimpleResourceActionResult result = null;
                                                    //result = town.getResourceManager().costIfHaveEnough(type.generateAttribute(level), amount);
                                                    if(result.allModified()){
                                                        ct.getSource().sendSuccess(()-> Components.str("Resource costed."), true);
                                                    } else ct.getSource().sendSuccess(()-> Components.str("Resource cost failed: No enough resource."), true);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        );

        LiteralArgumentBuilder<CommandSourceStack> addItemOnHand =
                Commands.literal("addItemOnHand")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                .executes(ct -> {
                                    double amount = DoubleArgumentType.getDouble(ct, "amount");
                                    TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                                    ItemStack itemStack = ct.getSource().getPlayerOrException().getMainHandItem();
                                    ct.getSource().sendSuccess(()-> Components.str("Adding ItemStack: " + itemStack), true);
                                    TownResourceActions.ItemResourceAction action = new TownResourceActions.ItemResourceAction(itemStack, ResourceActionType.ADD, amount, ResourceActionMode.ATTEMPT);
                                    TownResourceActionResults.ItemResourceActionResult result = town.getActionExecutorHandler().execute(action);
                                    //TownResourceManager.SimpleResourceActionResult result = town.getResourceManager().addIfHaveCapacity(itemStack, amount);
                                    if(result.allModified()){
                                        ct.getSource().sendSuccess(()-> Components.str("Resource added"), true);
                                        return Command.SINGLE_SUCCESS;
                                    } else ct.getSource().sendSuccess(()-> Components.str("Resource added failed: No enough capacity."), true);
                                    return Command.SINGLE_SUCCESS;
                                })

                        );

        LiteralArgumentBuilder<CommandSourceStack> listResidents =
                Commands.literal("list").executes(ct -> {
                    TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                            int size = town.getResidents().values().size();
                            ct.getSource().sendSuccess(()-> Components.str("Total residents: " + size), true);
                            ct.getSource().sendSuccess(()-> Components.str(town.getResidents().values()), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSourceStack> addResident = residentAddCommand();

        LiteralArgumentBuilder<CommandSourceStack> listBlocks =
                Commands.literal("list").executes(ct -> {
                    TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                    ct.getSource().sendSuccess(()-> Components.str("Total blocks: " + town.getTownBuildings().size()), true);
                    town.getTownBuildings().forEach((k, v) -> {
                        String buildingName = v.getClass().getSimpleName();
                        ct.getSource().sendSuccess(()-> Components.str(buildingName).append(Components.str(" at " + k)), true);
                    });
                    return Command.SINGLE_SUCCESS;
                });

        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string)
                    .requires(s -> s.hasPermission(2))
                    .then(Commands.literal("town")
                            .then(tickManual)
                            .then(spawnRefugees)
                            .then(name)
                            .then(Commands.literal("resources")
                                    .then(listItemStackResources)
                                .then(listVirtualResources)
                                .then(modifyVirtualResources)
                                    .then(addItemOnHand)
                                .then(costResource)
                                    .then(costResourceByType)
                                    .then(listAllResources)
                            )
                            .then(Commands.literal("residents")
                                    .then(listResidents)
                                    .then(addResident)
                            )
                            .then(Commands.literal("blocks")
                                    .then(listBlocks)
                            )
                    )
            );
        }

        // alias without modid
        dispatcher.register(Commands.literal("town")
                .requires(s -> s.hasPermission(2))
                .then(tickManual)
                .then(spawnRefugees)
                .then(name)
                .then(Commands.literal("resources")
                        .then(listItemStackResources)
                        .then(listVirtualResources)
                        .then(modifyVirtualResources)
                        .then(addItemOnHand)
                        .then(costResource)
                        .then(costResourceByType)
                        .then(listAllResources)
                )
                .then(Commands.literal("residents")
                        .then(listResidents)
                        .then(addResident)
                )
                .then(Commands.literal("blocks")
                        .then(listBlocks)
                )
        );
    }

    static LiteralArgumentBuilder<CommandSourceStack> residentAddCommand() {
        return Commands.literal("add")
                .executes(ct -> addRandomResidents(ct, 1, null, null, null))
                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .executes(ct -> addRandomResidents(
                                ct, IntegerArgumentType.getInteger(ct, "count"),
                                null, null, null))
                        .then(Commands.argument("age", StringArgumentType.word())
                                .suggests((ct, builder) -> SharedSuggestionProvider.suggest(
                                        new String[]{"infant", "child", "adult", "elder"}, builder))
                                .executes(ct -> addRandomResidents(
                                        ct, IntegerArgumentType.getInteger(ct, "count"),
                                        StringArgumentType.getString(ct, "age"), null, null))
                                .then(Commands.argument("first_name", StringArgumentType.string())
                                        .executes(ct -> addRandomResidents(
                                                ct, IntegerArgumentType.getInteger(ct, "count"),
                                                StringArgumentType.getString(ct, "age"),
                                                StringArgumentType.getString(ct, "first_name"), null))
                                        .then(Commands.argument("last_name", StringArgumentType.string())
                                                .executes(ct -> addRandomResidents(
                                                        ct, IntegerArgumentType.getInteger(ct, "count"),
                                                        StringArgumentType.getString(ct, "age"),
                                                        StringArgumentType.getString(ct, "first_name"),
                                                        StringArgumentType.getString(ct, "last_name")))))));
    }

    private static int addRandomResidents(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,
            int count,
            String ageName,
            String firstName,
            String lastName
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        TeamTown town = TeamTown.from(context.getSource().getPlayerOrException());
        Integer age = ageName == null ? null : parseResidentAge(ageName);
        if (age != null && age < 0) {
            context.getSource().sendFailure(Component.literal(
                    "Unknown age; expected infant, child, adult, or elder"));
            return 0;
        }

        int added = addUntilRejected(count,
                ordinal -> createRandomResident(firstName, lastName, age, ordinal),
                town::addResident);
        if (added == count) {
            context.getSource().sendSuccess(
                    () -> Components.str("Residents added: " + added), true);
            return added;
        }
        String partialResult = "Residents added: " + added + " of " + count
                + "; no available housing for the remaining " + (count - added);
        if (added == 0) {
            context.getSource().sendFailure(Component.literal(partialResult));
            return 0;
        }
        context.getSource().sendSuccess(() -> Components.str(partialResult), true);
        return added;
    }

    private static Resident createRandomResident(
            String firstName,
            String lastName,
            Integer age,
            int ordinal
    ) {
        ResidentName name = resolveResidentName(
                firstName, lastName, ordinal,
                TownCommand::randomFirstName, TownCommand::randomLastName);
        if (age == null) {
            return Resident.createRandomRecruit(name.firstName(), name.lastName());
        }
        return Resident.createRandomRecruit(name.firstName(), name.lastName(), age);
    }

    static ResidentName resolveResidentName(
            String firstName,
            String lastName,
            int ordinal,
            Supplier<String> randomFirstName,
            Supplier<String> randomLastName
    ) {
        Objects.requireNonNull(randomFirstName);
        Objects.requireNonNull(randomLastName);
        boolean fixedFirstName = firstName != null;
        boolean fixedLastName = lastName != null;
        String resolvedFirstName = fixedFirstName ? firstName : randomFirstName.get();
        if (fixedFirstName && fixedLastName) {
            resolvedFirstName += " " + ordinal;
        }
        return new ResidentName(
                resolvedFirstName,
                fixedLastName ? lastName : randomLastName.get());
    }

    static <T> int addUntilRejected(
            int count,
            IntFunction<T> residentFactory,
            Predicate<T> addResident
    ) {
        if (count < 1) throw new IllegalArgumentException("Resident count must be positive");
        Objects.requireNonNull(residentFactory);
        Objects.requireNonNull(addResident);
        int added = 0;
        for (int ordinal = 1; ordinal <= count; ordinal++) {
            if (!addResident.test(residentFactory.apply(ordinal))) break;
            added++;
        }
        return added;
    }

    private static String randomFirstName() {
        return WanderingRefugee.FIRST_NAMES[
                CMath.RANDOM.nextInt(WanderingRefugee.FIRST_NAMES.length)];
    }

    private static String randomLastName() {
        return WanderingRefugee.LAST_NAMES[
                CMath.RANDOM.nextInt(WanderingRefugee.LAST_NAMES.length)];
    }

    record ResidentName(String firstName, String lastName) {
    }

    static int parseResidentAge(String ageName) {
        return switch (ageName.toLowerCase(java.util.Locale.ROOT)) {
            case "infant" -> Resident.AGE_INFANT;
            case "child" -> Resident.AGE_CHILD;
            case "adult" -> Resident.AGE_ADULT;
            case "elder" -> Resident.AGE_ELDER;
            default -> -1;
        };
    }

    private static int advanceTown(CommandSourceStack source, int repeats) {
        var player = source.getPlayer();
        if (player != null) {
            var data = TeamTown.from(player).getTownData();
            if (data.isPresent()) {
                var teamData = CTeamDataManager.get(player);
                for (int index = 0; index < repeats; index++) {
                    data.get().tickMorning(source.getLevel(), teamData);
                }
                source.sendSuccess(() -> Component.literal(
                        "Advanced the town by " + repeats + (repeats == 1 ? " settlement day" : " settlement days")),
                        false);
                return Command.SINGLE_SUCCESS;
            }
        }
        source.sendFailure(Component.literal("Unable to get your team's data"));
        return 0;
    }
}
