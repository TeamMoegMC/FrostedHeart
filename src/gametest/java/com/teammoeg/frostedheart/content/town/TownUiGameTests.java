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

package com.teammoeg.frostedheart.content.town;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.mojang.brigadier.tree.CommandNode;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.view.HuntingBaseViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.buildings.logistics.view.TransportStationViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.view.MineBaseViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.house.view.HouseViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.house.view.HouseViewPresenter.HouseView;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.SimpleItemKey;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.VirtualItemStack;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.view.WarehouseViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.view.WarehouseViewPresenter.WarehouseView;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteBuilding;
import com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteView;
import com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteViewPresenter;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;
import com.teammoeg.frostedheart.content.town.resource.ItemStackResourceKey;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.item.townmanager.view.TownHallPresenter;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.TownUiMode;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * 城镇界面玩家视图的规则：数字规则、研究所视图不出现小数且便签按优先级排序、详细模式的权限。
 * Rules of the town screens' player view: the number rules, an institute view without decimals whose notes follow
 * their priority, and the detailed mode's permission.
 */
@GameTestHolder("frostedheart_town_ui")
@PrefixGameTestTemplate(false)
public class TownUiGameTests {
    private static final Pattern DECIMAL = Pattern.compile("\\d[.,]\\d");

    @GameTest(template = "phase0a_empty", timeoutTicks = 20)
    public static void formatRulesKeepWholeNumbersAndBounds(GameTestHelper helper) {
        helper.assertTrue(TownFormat.whole(1.19) == 1 && TownFormat.whole(0.999999) == 0, "capacities round down");
        helper.assertTrue(TownFormat.underOne(0.4) && !TownFormat.underOne(0) && !TownFormat.underOne(1), "under one is only (0, 1)");
        helper.assertTrue(TownFormat.efficiency(1.04) == 104 && TownFormat.efficiency(1.199) == 119, "efficiency is a floored percentage");
        helper.assertTrue(TownFormat.percent(0.9999) == 99 && TownFormat.percent(1) == 100 && TownFormat.percent(3.57, 4) == 89,
                "progress never shows 100% early");
        TownFormat.Temperature nearlyWarm = TownFormat.temperature(9.9, 10, 5);
        helper.assertTrue(nearlyWarm.shown() == 9 && nearlyWarm.warmth() == TownFormat.Warmth.COLD,
                "9.9 °C against 10 °C reads as a cold 9, got " + nearlyWarm);
        TownFormat.Temperature justWarm = TownFormat.temperature(10.45, 10.4, 5);
        helper.assertTrue(justWarm.shown() >= 11 && justWarm.warmth() == TownFormat.Warmth.COOL,
                "a passing value never rounds below its minimum, got " + justWarm);
        helper.assertTrue(TownFormat.temperature(15.2, 10, 5).warmth() == TownFormat.Warmth.WARM
                && TownFormat.temperature(12.3, 10, 5).shown() == 12, "warm rooms round to nearest");
        helper.assertTrue(TownFormat.frost(16, 10) == 0 && TownFormat.frost(12, 10) == 1 && TownFormat.frost(9, 10) == 2
                && TownFormat.frost(4, 10) == 3, "frost levels follow the margin");
        helper.assertTrue(TownFormat.chance(0.19) == TownFormat.Chance.VERY_LOW && TownFormat.chance(0.2) == TownFormat.Chance.LOW
                && TownFormat.chance(0.4) == TownFormat.Chance.MEDIUM && TownFormat.chance(0.6) == TownFormat.Chance.HIGH
                && TownFormat.chance(0.8) == TownFormat.Chance.VERY_HIGH, "chance words change at 20% steps");
        helper.assertTrue(TownFormat.chanceSteps(0) == 0 && TownFormat.chanceSteps(0.14) == 1 && TownFormat.chanceSteps(1) == 5,
                "the meter fills one step per started fifth");
        helper.assertTrue(TownFormat.compact(999).equals("999") && TownFormat.compact(1000).equals("1k")
                && TownFormat.compact(12345).equals("12k") && TownFormat.compact(1_234_567).equals("1M"), "counts abbreviate without decimals");
        helper.assertTrue(TownFormat.stars(0.37) == 2 && TownFormat.stars(0.6) == 3, "ratings round to the nearest star");
        helper.assertTrue(TownFormat.light(7.9, 8) == TownFormat.Light.DIM && TownFormat.light(8, 8) == TownFormat.Light.ENOUGH
                && TownFormat.light(12, 8) == TownFormat.Light.BRIGHT && TownFormat.light(Double.NaN, 8) == TownFormat.Light.DIM,
                "light levels");
        helper.assertTrue(TownFormat.days(28.9, 1) == 28 && TownFormat.days(5, 0) == -1, "stock days round down");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void instituteViewHasWholeNumbersAndPrioritizedNotes(GameTestHelper helper) {
        TeamDataHolder holder = TownBuildingGameTests.newTeam(helper, "institute-view");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        ResearchInstituteBlockEntity core = ResearchInstituteGameTests.furnishedInstitute(helper, holder, origin);
        ResearchInstituteBuilding institute = core.getBuilding().orElseThrow();
        core.refresh_safe(institute);
        institute.setAverageLight(15);
        institute.setTemperature(12.37);
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        Resident healthy = ResearchInstituteGameTests.researcher("Gabriel", 63.4, 1);
        Resident sick = ResearchInstituteGameTests.researcher("William", 47.8, 0);
        sick.setHealth(5.5);
        for (Resident resident : List.of(healthy, sick)) {
            data.residents.put(resident.getUUID(), resident);
            institute.addResident(resident);
        }
        TeamTown town = data.createTeamTown();
        InstituteView cool = InstituteViewPresenter.present(new InstituteViewPresenter.Inputs(institute, core.getBlockPos(),
                List.of(healthy, sick), town, null, "雪松镇", 12));
        helper.assertTrue(cool.header().status() == BoardView.Status.LIMITED, "a cool institute with a sick researcher is limited, got "
                + cool.header().status());
        helper.assertTrue(cool.header().frost() == 1, "12 °C against 10 °C frosts the corners");
        helper.assertTrue(!cool.notes().isEmpty() && cool.notes().get(0).problem().equals("cool"),
                "the most important note is the cold room, got " + problems(cool));
        helper.assertTrue(cool.notes().stream().anyMatch(note -> note.problem().startsWith("sick")),
                "the sick researcher gets a note, got " + problems(cool));
        helper.assertTrue(cool.notes().size() <= 3 && cool.notes().size() + cool.talk().size() <= 6, "at most three improvements, six notes");
        helper.assertTrue(cool.researchers().people().stream().anyMatch(p -> p.duty() == BoardView.Duty.RESTING),
                "the sick researcher is resting");
        for (String text : texts(cool))
            helper.assertTrue(!DECIMAL.matcher(text).find(), "the player view shows no decimals: " + text);

        institute.setTemperature(8.6);
        InstituteView cold = InstituteViewPresenter.present(new InstituteViewPresenter.Inputs(institute, core.getBlockPos(),
                List.of(healthy, sick), town, null, "雪松镇", 12));
        helper.assertTrue(cold.header().status() == BoardView.Status.STOPPED && cold.header().frost() == 2,
                "below its minimum the institute stops and frosts over");
        helper.assertTrue(cold.notes().get(0).problem().equals("cold"), "a freezing room comes first");
        for (String text : texts(cold))
            helper.assertTrue(!DECIMAL.matcher(text).find(), "the stopped view shows no decimals either: " + text);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void houseViewSpeaksPlainlyInEveryState(GameTestHelper helper) {
        TeamDataHolder holder = TownBuildingGameTests.newTeam(helper, "house-view");
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        HouseBuilding house = new HouseBuilding(pos);
        house.setInitialized(true);
        house.setIsStructureValid(true);
        house.setArea(64);
        house.setVolume(256);
        house.setMaxResidents(3);
        house.setDecorationRating(0.9);
        data.applyBuildingUpdate(java.util.Map.of(pos, house), java.util.Set.of());
        double minimum = FHConfig.SERVER.TOWN.HOUSING.minimumTemperatureCelsius.get();
        double maximum = FHConfig.SERVER.TOWN.HOUSING.maximumTemperatureCelsius.get();
        house.setTemperature(Math.min(maximum - 1, minimum + 18.37));

        Resident anna = new Resident("Anna", "Berg");
        Resident child = new Resident("Lily", "Berg", Resident.AGE_INFANT, 0);
        for (Resident resident : List.of(anna, child)) {
            resident.setHousePos(pos);
            resident.setHealth(81.6);
            resident.setMental(77.2);
            resident.completeNutritionMeal(new ResidentNutrition(84.5, 82.3, 79.9, 88.1));
            data.residents.put(resident.getUUID(), resident);
            house.addResident(resident);
        }
        house.updateCentralDailyReport(2, 2 * FHConfig.SERVER.TOWN.HOUSING.foodConsumptionPerResidentDay.get(),
                HouseBuilding.DailyMeal.settled(7, java.util.Map.of(new ItemStackResourceKey(Items.BREAD), 2.4,
                        new ItemStackResourceKey(Items.COOKED_COD), 0.3)));
        TeamTown town = data.createTeamTown();
        HouseView warm = present(house, pos, List.of(anna, child), town);
        helper.assertTrue(warm.header().status() == BoardView.Status.RUNNING && warm.notes().isEmpty(),
                "a warm, fed, decorated house has nothing to improve, got " + warm.header().status() + " " + problems(warm.notes()));
        helper.assertTrue(warm.residents().people().get(1).duty() == BoardView.Duty.FREE
                        && warm.residents().people().get(1).lines().stream().anyMatch(line -> flatten(line).contains("too_young")),
                "an infant is free, not off work, and the card says it is too young");
        helper.assertTrue(warm.meal().items().size() == 2 && flatten(warm.meal().items().get(1).amount()).contains("a_little"),
                "a fraction of a fish reads as a little");
        assertNoDecimals(helper, houseTexts(warm), "warm");

        house.setTemperature(minimum + 2.6);
        house.updateCentralDailyReport(2, FHConfig.SERVER.TOWN.HOUSING.foodConsumptionPerResidentDay.get() * 1.13,
                HouseBuilding.DailyMeal.settled(8, java.util.Map.of(new ItemStackResourceKey(Items.BREAD), 1.0)));
        for (Resident resident : List.of(anna, child)) resident.completeNutritionMeal(new ResidentNutrition(84.5, 82.3, 9.4, 88.1));
        HouseView cool = present(house, pos, List.of(anna, child), town);
        helper.assertTrue(cool.header().status() == BoardView.Status.LIMITED && cool.header().frost() == 1,
                "a cool, underfed house is makeshift with corner frost, got " + cool.header().status() + " frost " + cool.header().frost());
        helper.assertTrue(problems(cool.notes()).equals(List.of("cool", "hungry", "nutrition")),
                "cold first, then hunger, then the shared shortage, got " + problems(cool.notes()));
        helper.assertTrue(flatten(cool.notes().get(2).line()).contains("nutrient.protein"), "both lack protein: " + flatten(cool.notes().get(2).line()));
        assertNoDecimals(helper, houseTexts(cool), "cool");

        house.setTemperature(minimum - 3.2);
        anna.setHealth(6.5);
        HouseView cold = present(house, pos, List.of(anna, child), town);
        helper.assertTrue(cold.header().status() == BoardView.Status.STOPPED && cold.header().frost() == 2,
                "below its minimum a house is unfit and frosts over");
        helper.assertTrue(cold.notes().get(0).problem().equals("cold") && cold.residents().emptySlots() == 0,
                "a freezing house leads with the cold and offers no bed");
        helper.assertTrue(cold.residents().people().get(0).duty() == BoardView.Duty.RESTING, "the sick resident rests");
        assertNoDecimals(helper, houseTexts(cold), "cold");

        house.setIsStructureValid(false);
        HouseView empty = present(house, pos, List.of(), town);
        helper.assertTrue(empty.header().status() == BoardView.Status.STOPPED && empty.notes().get(0).problem().equals("structure"),
                "a broken empty house names the room first");
        helper.assertTrue(empty.talk().size() == 1 && flatten(empty.talk().get(0).line()).contains("talk.empty.unfit"),
                "an empty house has no small talk but a memo");
        // Its residents just left, so the last meal is still the one they ate.
        helper.assertTrue(empty.meal().satisfaction() >= 0, "a house its people just left still shows their last meal");
        BlockPos vacantPos = pos.east(6);
        HouseBuilding vacant = new HouseBuilding(vacantPos);
        vacant.setInitialized(true);
        vacant.setIsStructureValid(true);
        vacant.setMaxResidents(2);
        HouseView never = present(vacant, vacantPos, List.of(), town);
        helper.assertTrue(flatten(never.stats().get(1).sub()).contains("stat.fed.nobody") && never.meal().satisfaction() < 0,
                "a house nobody has lived in promises no meal and shows no share fed");
        assertNoDecimals(helper, houseTexts(empty), "empty");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void warehouseViewCountsWithoutDecimals(GameTestHelper helper) {
        TeamDataHolder holder = TownBuildingGameTests.newTeam(helper, "warehouse-view");
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        WarehouseBuilding warehouse = new WarehouseBuilding(pos);
        warehouse.setInitialized(true);
        warehouse.setIsStructureValid(true);
        warehouse.setArea(30);
        warehouse.setVolume(120);
        warehouse.setCapacity(2345.67);
        data.applyBuildingUpdate(java.util.Map.of(pos, warehouse), java.util.Set.of());
        TeamTown town = data.createTeamTown();
        List<VirtualItemStack> stock = List.of(
                new VirtualItemStack(SimpleItemKey.from(new net.minecraft.world.item.ItemStack(Items.COBBLESTONE)), 12_345),
                new VirtualItemStack(SimpleItemKey.from(new net.minecraft.world.item.ItemStack(Items.BREAD)), 37),
                new VirtualItemStack(SimpleItemKey.from(new net.minecraft.world.item.ItemStack(Items.OAK_LOG)), 640));
        WarehouseView hungry = WarehouseViewPresenter.present(new WarehouseViewPresenter.Inputs(warehouse, pos, town, "Cedar Hollow", 9,
                stock, 2.71, 4));
        helper.assertTrue(hungry.header().status() == BoardView.Status.LIMITED && hungry.notes().stream().anyMatch(n -> n.problem().equals("food")),
                "food for two days makes things tight and gets a note, got " + hungry.header().status() + " " + problems(hungry.notes()));
        helper.assertTrue(flatten(hungry.foodLine()).contains("food.days 2"), "2.71 days reads as 2: " + flatten(hungry.foodLine()));
        helper.assertTrue(hungry.notes().stream().anyMatch(n -> n.problem().equals("racks")), "an empty rack count gets a note");
        helper.assertTrue(hungry.topItems().size() == 3 && flatten(hungry.topItems().get(0).amount()).equals("12345"),
                "the most stocked item comes first, exact below 100k: " + flatten(hungry.topItems().get(0).amount()));
        helper.assertTrue(hungry.kinds() == 3, "three kinds of goods");
        assertNoDecimals(helper, warehouseTexts(hungry), "hungry warehouse");

        warehouse.setIsStructureValid(false);
        WarehouseView broken = WarehouseViewPresenter.present(new WarehouseViewPresenter.Inputs(warehouse, pos, town, "Cedar Hollow", 9,
                List.of(), null, 0));
        helper.assertTrue(broken.header().status() == BoardView.Status.STOPPED && broken.notes().get(0).problem().equals("structure"),
                "a broken room closes the warehouse and leads the notes");
        helper.assertTrue(flatten(broken.foodLine()).contains("food.unknown") && broken.talk().stream()
                .anyMatch(n -> flatten(n.line()).contains("talk.empty")), "unknown food and an empty store read plainly");
        assertNoDecimals(helper, warehouseTexts(broken), "broken warehouse");
        helper.succeed();
    }

    private static List<String> warehouseTexts(WarehouseView view) {
        List<Component> parts = new ArrayList<>();
        parts.add(view.header().name());
        parts.add(view.header().subtitle());
        parts.addAll(view.header().reasons());
        parts.add(view.capacity().line());
        parts.add(view.foodLine());
        for (BoardView.Stat stat : view.stats()) {
            parts.addAll(List.of(stat.title(), stat.value(), stat.sub()));
            parts.addAll(stat.tooltip());
        }
        view.conditions().forEach(chip -> parts.add(chip.text()));
        for (BoardView.Note note : view.notes()) parts.addAll(List.of(note.line(), note.action(), note.signature()));
        for (BoardView.Note note : view.talk()) parts.addAll(List.of(note.line(), note.signature()));
        view.topItems().forEach(item -> parts.add(item.amount()));
        parts.addAll(view.tips());
        List<String> out = new ArrayList<>();
        for (Component part : parts) out.add(flatten(part));
        return out;
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void workBoardsSpeakPlainly(GameTestHelper helper) {
        TeamDataHolder holder = TownBuildingGameTests.newTeam(helper, "work-boards");
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        BlockPos minePos = helper.absolutePos(new BlockPos(2, 2, 2)), huntPos = helper.absolutePos(new BlockPos(6, 2, 2)),
                stationPos = helper.absolutePos(new BlockPos(10, 2, 2));
        MineBaseBuilding mineBase = new MineBaseBuilding(minePos);
        HuntingBaseBuilding hunting = new HuntingBaseBuilding(huntPos);
        TransportStationBuilding station = new TransportStationBuilding(stationPos);
        for (var building : List.of(mineBase, hunting, station)) {
            building.setInitialized(true);
            building.setIsStructureValid(true);
            building.setMaxResidents(3);
        }
        hunting.setArea(64);
        hunting.setVolume(256);
        station.setArea(64);
        station.setVolume(256);
        double minimum = FHConfig.SERVER.TOWN.HUNTING.minimumWorkingTemperatureCelsius.get();
        hunting.setTemperature(minimum + 12.4);
        data.applyBuildingUpdate(java.util.Map.of(minePos, mineBase, huntPos, hunting, stationPos, station), java.util.Set.of());
        List<Resident> miners = new ArrayList<>(), hunters = new ArrayList<>(), carriers = new ArrayList<>();
        int index = 0;
        for (List<Resident> crew : List.of(miners, hunters, carriers)) {
            var building = List.of(mineBase, hunting, station).get(index++);
            for (String name : List.of("Ada", "Bram")) {
                Resident resident = new Resident(name, "Holt" + index);
                resident.setHealth(72.6);
                resident.setMental(66.3);
                resident.setHousePos(helper.absolutePos(new BlockPos(1, 2, 1)));
                resident.setWorkPos(building.getPos());
                data.residents.put(resident.getUUID(), resident);
                building.addResident(resident);
                crew.add(resident);
            }
        }
        TeamTown town = data.createTeamTown();

        WorkView.Board mines = MineBaseViewPresenter.present(new MineBaseViewPresenter.Inputs(mineBase, minePos, miners, town, "Cedar Hollow", 4));
        helper.assertTrue(mines.header().status() == BoardView.Status.STOPPED && mines.notes().get(0).problem().equals("no_mines"),
                "a mine base without mines stops and asks for one, got " + mines.header().status() + " " + problems(mines.notes()));
        helper.assertTrue(mines.crew().people().size() == 2 && mines.crew().people().get(0).efficiency() > 0, "miners show their efficiency");
        assertNoDecimals(helper, workTexts(mines), "mine base");

        WorkView.Board warm = HuntingBaseViewPresenter.present(new HuntingBaseViewPresenter.Inputs(hunting, huntPos, hunters, town, "Cedar Hollow", 4));
        helper.assertTrue(warm.header().status() != BoardView.Status.STOPPED && warm.header().frost() == 0,
                "a warm, staffed hunting base works, got " + warm.header().status() + " " + warm.header().reasons().stream().map(TownUiGameTests::flatten).toList());
        assertNoDecimals(helper, workTexts(warm), "warm hunting base");
        hunting.setTemperature(minimum - 3.3);
        WorkView.Board cold = HuntingBaseViewPresenter.present(new HuntingBaseViewPresenter.Inputs(hunting, huntPos, hunters, town, "Cedar Hollow", 4));
        helper.assertTrue(cold.header().status() == BoardView.Status.STOPPED && cold.header().frost() == 2 && cold.notes().get(0).problem().equals("cold"),
                "below its minimum the lodge stops, frosts and leads with the cold, got " + problems(cold.notes()));
        assertNoDecimals(helper, workTexts(cold), "cold hunting base");

        WorkView.Board transport = TransportStationViewPresenter.present(new TransportStationViewPresenter.Inputs(station, stationPos, carriers, town,
                "Cedar Hollow", 4));
        helper.assertTrue(flatten(transport.stats().get(2).value()).startsWith("[gui.frostedheart.board.transport.stat.tomorrow.value"),
                "a staffed station forecasts capacity, got " + flatten(transport.stats().get(2).value()));
        assertNoDecimals(helper, workTexts(transport), "transport station");
        helper.succeed();
    }

    private static List<String> workTexts(WorkView.Board board) {
        List<Component> parts = new ArrayList<>();
        parts.add(board.header().name());
        parts.add(board.header().subtitle());
        parts.addAll(board.header().reasons());
        for (BoardView.Stat stat : board.stats()) {
            parts.addAll(List.of(stat.title(), stat.value(), stat.sub()));
            parts.addAll(stat.tooltip());
        }
        board.conditions().forEach(chip -> parts.add(chip.text()));
        for (BoardView.Note note : board.notes()) parts.addAll(List.of(note.line(), note.action(), note.signature()));
        for (BoardView.Note note : board.talk()) parts.addAll(List.of(note.line(), note.signature()));
        parts.addAll(List.of(board.crew().headline(), board.crew().slots(), board.crew().empty()));
        for (BoardView.Person person : board.crew().people()) {
            parts.add(person.badge());
            parts.add(person.reason());
            person.bars().forEach(bar -> parts.add(bar.label()));
            parts.addAll(person.lines());
        }
        WorkView.Output output = board.output();
        parts.addAll(List.of(output.yieldTitle(), output.yieldEmpty(), output.yieldNote()));
        output.yield().forEach(item -> parts.add(item.amount()));
        for (WorkView.Forecast forecast : output.forecast()) parts.addAll(List.of(forecast.title(), forecast.value(), forecast.line()));
        parts.addAll(output.tips());
        for (WorkView.Site site : board.sites()) parts.addAll(List.of(site.name(), site.detail(), site.state()));
        List<String> out = new ArrayList<>();
        for (Component part : parts) out.add(flatten(part));
        return out;
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void townHallListsEveryBuildingWithoutDecimals(GameTestHelper helper) {
        TeamDataHolder holder = TownBuildingGameTests.newTeam(helper, "town-hall");
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        BlockPos housePos = helper.absolutePos(new BlockPos(2, 2, 2)), basePos = helper.absolutePos(new BlockPos(8, 2, 2));
        HouseBuilding house = new HouseBuilding(housePos);
        house.setInitialized(true);
        house.setIsStructureValid(true);
        house.setArea(36);
        house.setVolume(108);
        house.setMaxResidents(2);
        house.setTemperature(18.6);
        MineBaseBuilding base = new MineBaseBuilding(basePos);
        base.setInitialized(true);
        base.setIsStructureValid(false);
        base.setMaxResidents(2);
        data.applyBuildingUpdate(java.util.Map.of(housePos, house, basePos, base), java.util.Set.of());
        Resident anna = new Resident("Anna", "Berg");
        anna.setHealth(57.7);
        anna.setMental(48.2);
        anna.setHousePos(housePos);
        data.residents.put(anna.getUUID(), anna);
        house.addResident(anna);
        TeamTown town = data.createTeamTown();
        TownHallPresenter.TownHallView view = TownHallPresenter.present(new TownHallPresenter.Inputs(town, data, null, 6));
        helper.assertTrue(view.buildings().size() == 2 && view.buildings().get(0).name().getString().length() > 0,
                "both buildings are listed, homes first, got " + view.buildings().size());
        helper.assertTrue(view.buildings().get(1).status() == BoardView.Status.STOPPED,
                "the broken mine base reads as stopped, just like on its own board");
        helper.assertTrue(view.people().size() == 1 && view.people().get(0).duty() == BoardView.Duty.FREE, "a fit resident without work is free");
        List<Component> parts = new ArrayList<>(List.of(view.header().name(), view.header().subtitle()));
        parts.addAll(view.header().reasons());
        for (BoardView.Stat stat : view.stats()) parts.addAll(List.of(stat.title(), stat.value(), stat.sub()));
        view.conditions().forEach(chip -> parts.add(chip.text()));
        for (BoardView.Note note : view.talk()) parts.add(note.line());
        view.tower().forEach(line -> parts.add(line.text()));
        for (TownHallPresenter.BuildingCard card : view.buildings()) parts.addAll(List.of(card.name(), card.stamp(), card.line()));
        for (BoardView.Person person : view.people()) parts.addAll(person.lines());
        for (TownHallPresenter.Law law : view.laws().laws()) parts.add(law.name());
        parts.add(view.laws().state());
        assertNoDecimals(helper, parts.stream().map(TownUiGameTests::flatten).toList(), "town hall");

        data.residents.remove(anna.getUUID());
        house.removeResident(anna);
        TownHallPresenter.TownHallView empty = TownHallPresenter.present(new TownHallPresenter.Inputs(data.createTeamTown(), data, null, 7));
        helper.assertTrue(empty.conditions().size() >= 1 && flatten(empty.conditions().get(0).text()).contains("chip.empty"),
                "an empty town says so first instead of 'everyone housed'");
        helper.assertTrue(empty.notes().stream().anyMatch(note -> note.problem().equals("empty")) && empty.talk().isEmpty(),
                "an empty town gets one note on what to do and no small talk");
        helper.assertTrue(flatten(empty.stats().get(1).sub()).contains("stat.food.nobody")
                        && flatten(empty.stats().get(3).sub()).contains("stat.wellbeing.nobody"),
                "food and well-being say there is nobody, not a count of zero");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 20)
    public static void buildingsKeepNumbersNamesAndDirections(GameTestHelper helper) {
        java.util.Map<BlockPos, com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding> buildings = new java.util.HashMap<>();
        BlockPos a = new BlockPos(0, 64, 0), b = new BlockPos(40, 64, 0), c = new BlockPos(80, 64, 0), d = new BlockPos(120, 64, 0);
        for (BlockPos pos : List.of(a, b, c)) buildings.put(pos, new HouseBuilding(pos));
        com.teammoeg.frostedheart.content.town.building.TownBuildingNumbers.assignMissing(buildings);
        helper.assertTrue(buildings.get(a).getNumber() == 1 && buildings.get(b).getNumber() == 2 && buildings.get(c).getNumber() == 3,
                "unnumbered houses are numbered in position order");
        buildings.remove(b);
        buildings.put(d, new HouseBuilding(d));
        com.teammoeg.frostedheart.content.town.building.TownBuildingNumbers.assignMissing(buildings);
        helper.assertTrue(buildings.get(d).getNumber() == 2 && buildings.get(c).getNumber() == 3,
                "a new house takes the smallest free number and nobody is renumbered");
        HouseBuilding named = (HouseBuilding) buildings.get(c);
        named.setCustomName("Old Mill");
        helper.assertTrue(com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels.name(buildings, c, named).getString().equals("Old Mill"),
                "a custom name replaces type and number");
        var encoded = com.teammoeg.frostedheart.content.town.building.ITownBuilding.CODEC
                .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, named).getOrThrow(false, message -> {});
        var decoded = (HouseBuilding) com.teammoeg.frostedheart.content.town.building.ITownBuilding.CODEC
                .parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow(false, message -> {});
        helper.assertTrue("Old Mill".equals(decoded.getCustomName()) && decoded.getNumber() == 3, "name and number survive the codec");
        // Directions are read from the energy tower: close by is the centre, +z is south, no tower means no direction.
        BlockPos tower = new BlockPos(60, 64, 0);
        java.util.function.BiFunction<BlockPos, BlockPos, String> direction = com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels::direction;
        helper.assertTrue("east".equals(direction.apply(tower, d)) && "west".equals(direction.apply(tower, a)),
                "buildings east and west of the tower are named so");
        helper.assertTrue("center".equals(direction.apply(tower, new BlockPos(70, 64, 5))), "within 16 blocks of the tower is the centre");
        helper.assertTrue("south".equals(direction.apply(tower, new BlockPos(60, 64, 40)))
                        && "northwest".equals(direction.apply(tower, new BlockPos(30, 64, -30))),
                "+z is south and the diagonals have their own names");
        helper.assertTrue(direction.apply(null, a) == null
                        && com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels.directionName(null, a).getString().isEmpty(),
                "a town without a tower gives no direction");
        // Names lose formatting codes and stop at 16 characters; a blank name restores type and number.
        String trimmed = com.teammoeg.frostedheart.content.town.TownNamingModel.normalizeBuildingName("\u00a7cRed Barn of the Northern Hills");
        helper.assertTrue(trimmed.equals("Red Barn of the"),
                "a building name is cleaned and capped at 16 characters: " + trimmed);
        helper.assertTrue(com.teammoeg.frostedheart.content.town.TownNamingModel.normalizeBuildingName("   ").isEmpty(),
                "a blank name is empty, which restores the default");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 20)
    public static void instituteIdeasSayWhatTheyCameFrom(GameTestHelper helper) {
        var one = com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.idea(new net.minecraft.resources.ResourceLocation("frostedheart", "test_one"));
        var two = com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.idea(new net.minecraft.resources.ResourceLocation("frostedheart", "test_two"));
        var many = com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.idea(new net.minecraft.resources.ResourceLocation("frostedheart", "test_many"));
        var unknown = com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.idea(new net.minecraft.resources.ResourceLocation("frostedheart", "test_unknown"));
        var a = com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.observation(new java.util.UUID(1, 1));
        var b = com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.observation(new java.util.UUID(2, 2));
        var c = com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.observation(new java.util.UUID(3, 3));
        net.minecraft.nbt.CompoundTag snapshot = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag links = new net.minecraft.nbt.ListTag();
        links.add(knowledgeLink("single", one, a));
        links.add(knowledgeLink("pair", two, a, b));
        links.add(knowledgeLink("crowd", many, a, b, c));
        snapshot.put("links", links);
        // The town day an institute found an idea on travels with the acquisitions; others have none.
        net.minecraft.nbt.ListTag acquisitions = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.CompoundTag found = new net.minecraft.nbt.CompoundTag();
        found.put("key", com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, two)
                .getOrThrow(false, message -> {}));
        found.putString("source", "institute:link:knowledge_example:pair");
        found.putLong("town_day", 31);
        acquisitions.add(found);
        net.minecraft.nbt.CompoundTag older = new net.minecraft.nbt.CompoundTag();
        older.put("key", com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, one)
                .getOrThrow(false, message -> {}));
        older.putString("source", "institute:link:knowledge_example:single");
        acquisitions.add(older);
        snapshot.put("acquisitions", acquisitions);
        var journal = new com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData(snapshot);
        helper.assertTrue(Long.valueOf(31).equals(journal.instituteDays.get(two)) && !journal.instituteDays.containsKey(one),
                "the found day is read back for the idea that has one and missing for the one that has not");
        helper.assertTrue(com.teammoeg.frostedresearch.knowledge.KnowledgeService.instituteFoundKey(two)
                        .equals("institute_found:idea/" + two.id()),
                "the day is kept under a key naming the idea");
        String prefix = "gui.frostedheart.institute.assoc.from.";
        helper.assertTrue(translationKey(com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteViewPresenter.sources(journal, one), 1)
                .equals(prefix + "one"), "an idea from one piece of knowledge names it");
        helper.assertTrue(translationKey(com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteViewPresenter.sources(journal, two), 2)
                .equals(prefix + "two"), "an idea from two pieces names both");
        helper.assertTrue(translationKey(com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteViewPresenter.sources(journal, many), 2)
                .equals(prefix + "many"), "an idea from more names the first two and says there were more");
        helper.assertTrue(com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteViewPresenter.sources(journal, unknown) == null
                        && com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteViewPresenter.sources(null, one) == null,
                "without a record of the link nothing is claimed");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 20)
    public static void townWordsExistInBothLanguages(GameTestHelper helper) {
        // Every town event needs words for its tip and its event line; device kinds and states need theirs too.
        for (String lang : List.of("en_us", "zh_cn")) {
            com.google.gson.JsonObject table = langTable(lang);
            List<String> keys = new ArrayList<>();
            for (var type : com.teammoeg.frostedheart.content.town.observation.TownSignalEvent.Type.values()) {
                keys.add(com.teammoeg.frostedheart.content.town.observation.TownSignalTipPresentationModel.eventKey(type));
                keys.add("gui.frostedheart.town_manager.event." + type.name().toLowerCase(java.util.Locale.ROOT));
            }
            for (var kind : com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind.values())
                keys.add("gui.frostedheart.town_manager.virtual_resource.transport_endpoint_kind." + kind.name().toLowerCase(java.util.Locale.ROOT));
            for (var status : com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus.values())
                keys.add("gui.frostedheart.town_manager.virtual_resource.transport_admission." + status.name().toLowerCase(java.util.Locale.ROOT));
            keys.add(com.teammoeg.frostedheart.content.town.transport.TownTransportShortageTipPresentationModel.PLAIN_KEY);
            for (String key : keys) helper.assertTrue(table.has(key), lang + " has no words for " + key);
        }
        helper.assertTrue(com.teammoeg.frostedheart.content.town.transport.TownTransportShortageTipPresentationModel.roundedShortfall(0.2) == 1
                        && com.teammoeg.frostedheart.content.town.transport.TownTransportShortageTipPresentationModel.roundedShortfall(2.0) == 2
                        && com.teammoeg.frostedheart.content.town.transport.TownTransportShortageTipPresentationModel.roundedShortfall(2.01) == 3
                        && com.teammoeg.frostedheart.content.town.transport.TownTransportShortageTipPresentationModel.roundedShortfall(Double.NaN) == 1,
                "the shortage tip rounds the gap up and never says zero");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 40)
    public static void townHallDatesEventsInTheTownsOwnDays(GameTestHelper helper) {
        // A town founded on world day 223: its twenty settlements are world days 223 to 242 but town days 1 to 20.
        TeamDataHolder holder = TownBuildingGameTests.newTeam(helper, "town-days");
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        for (int i = 0; i < 20; i++) {
            long worldDay = 223 + i;
            List<com.teammoeg.frostedheart.content.town.observation.TownSignalEvent> events = new ArrayList<>();
            if (i == 4) events.add(new com.teammoeg.frostedheart.content.town.observation.TownSignalEvent(worldDay, 0,
                    com.teammoeg.frostedheart.content.town.observation.TownSignalEvent.Type.FOOD_SHORTAGE,
                    com.teammoeg.frostedheart.content.town.observation.TownSignalEvent.Severity.CRITICAL, 0, 1, "test"));
            // The tower went cold the afternoon before the last morning.
            if (i == 19) events.add(new com.teammoeg.frostedheart.content.town.observation.TownSignalEvent(worldDay - 1, 15,
                    com.teammoeg.frostedheart.content.town.observation.TownSignalEvent.Type.TOWER_SERVICE_LOST,
                    com.teammoeg.frostedheart.content.town.observation.TownSignalEvent.Severity.CRITICAL, 0, 2, "test"));
            data.applyHistoryUpdate(new com.teammoeg.frostedheart.content.town.TownHistoryEntry(worldDay, 3, 50, 50, 1,
                    50, 50, 50, 50, 0, 0, true, 0, events), i + 1);
        }
        TownHallPresenter.TownHallView view = TownHallPresenter.present(new TownHallPresenter.Inputs(data.createTeamTown(), data, null, data.getTownDay()));
        helper.assertTrue(data.getTownDay() == 20, "the town is twenty days old: " + data.getTownDay());
        helper.assertTrue(view.events().size() == 2, "both events are listed: " + view.events().size());
        String latest = flatten(view.events().get(0).when()), earlier = flatten(view.events().get(1).when());
        helper.assertTrue(latest.contains("events.when 19 15"), "the afternoon tower event is town day 19 at 15: " + latest);
        helper.assertTrue(earlier.contains("events.when.day 5"), "the food shortage is town day 5, not world day 227: " + earlier);
        helper.assertTrue(!latest.contains("242") && !earlier.contains("227"), "no world days leak onto the board");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 20)
    public static void countsPickTheirSingularForm(GameTestHelper helper) {
        String days = "gui.frostedheart.board.town.stat.reserve.days";
        helper.assertTrue(translationKeyOf(com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(days, 1)).equals(days + ".one"),
                "one day takes the singular form");
        helper.assertTrue(translationKeyOf(com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(days, 19)).equals(days),
                "nineteen days keep the plain form");
        helper.assertTrue(translationKeyOf(com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(days, Component.literal("1"))).equals(days + ".one"),
                "a count that arrives as text reading 1 is still one");
        String left = "gui.frostedheart.institute.order.days_left";
        helper.assertTrue(translationKeyOf(com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(left, 1, 3)).equals(left),
                "a string without a singular form is untouched, even when its first number is one");
        // Every singular form exists in both languages, so a Chinese client never falls back to English text.
        com.google.gson.JsonObject en = langTable("en_us"), zh = langTable("zh_cn");
        for (String key : en.keySet())
            if (key.endsWith(".one")) helper.assertTrue(zh.has(key), "zh_cn has " + key);
        helper.succeed();
    }

    private static String translationKeyOf(Component line) {
        return line.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }

    /** The merged language table the game loads, read from the mod's own resources. */
    private static com.google.gson.JsonObject langTable(String lang) {
        java.nio.file.Path path = net.minecraftforge.fml.ModList.get().getModFileById("frostedheart").getFile()
                .findResource("assets", "frostedheart", "lang", lang + ".json");
        try (var reader = java.nio.file.Files.newBufferedReader(path, java.nio.charset.StandardCharsets.UTF_8)) {
            return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("cannot read " + lang, failure);
        }
    }

    /** The translation key of a sources line, or "" when it is missing or has the wrong number of names. */
    private static String translationKey(@javax.annotation.Nullable Component line, int names) {
        if (line == null || !(line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents)) return "";
        return contents.getArgs().length == names ? contents.getKey() : "";
    }

    private static net.minecraft.nbt.CompoundTag knowledgeLink(String rule, com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey output,
                                                               com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey... inputs) {
        net.minecraft.nbt.CompoundTag link = new net.minecraft.nbt.CompoundTag();
        link.putString("rule", rule);
        link.put("output", com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, output)
                .getOrThrow(false, message -> {}));
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (var input : inputs) {
            net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
            entry.put("key", com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, input)
                    .getOrThrow(false, message -> {}));
            list.add(entry);
        }
        link.put("inputs", list);
        link.putBoolean("active", true);
        return link;
    }

    private static HouseView present(HouseBuilding house, BlockPos pos, List<Resident> residents, TeamTown town) {
        return HouseViewPresenter.present(new HouseViewPresenter.Inputs(house, pos, residents, town, "Cedar Hollow", 12));
    }

    private static void assertNoDecimals(GameTestHelper helper, List<String> texts, String state) {
        for (String text : texts)
            helper.assertTrue(!DECIMAL.matcher(text).find(), "the " + state + " house view shows no decimals: " + text);
    }

    private static List<String> problems(List<BoardView.Note> notes) {
        return notes.stream().map(BoardView.Note::problem).toList();
    }

    /** Every piece of text the house view would show, flattened like {@link #texts}. */
    private static List<String> houseTexts(HouseView view) {
        List<Component> parts = new ArrayList<>();
        parts.add(view.header().name());
        parts.add(view.header().subtitle());
        parts.addAll(view.header().reasons());
        for (BoardView.Stat stat : view.stats()) {
            parts.addAll(List.of(stat.title(), stat.value(), stat.sub()));
            parts.addAll(stat.tooltip());
        }
        for (BoardView.Chip chip : view.conditions()) {
            parts.add(chip.text());
            parts.addAll(chip.tooltip());
        }
        for (BoardView.Note note : view.notes()) parts.addAll(List.of(note.line(), note.action(), note.signature()));
        for (BoardView.Note note : view.talk()) parts.addAll(List.of(note.line(), note.signature()));
        parts.addAll(List.of(view.residents().headline(), view.residents().slots(), view.residents().empty()));
        for (BoardView.Person person : view.residents().people()) {
            parts.add(person.badge());
            parts.add(person.reason());
            person.bars().forEach(bar -> parts.add(bar.label()));
            parts.addAll(person.lines());
        }
        parts.addAll(List.of(view.meal().title(), view.meal().summary(), view.meal().empty(), view.meal().nutritionLine()));
        view.meal().nutrition().forEach(bar -> parts.add(bar.label()));
        view.meal().items().forEach(item -> parts.add(item.amount()));
        parts.addAll(view.meal().tips());
        List<String> out = new ArrayList<>();
        for (Component part : parts) out.add(flatten(part));
        return out;
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 20)
    public static void detailedModeNeedsPermissionAndDropsWithoutIt(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        TownUiMode.store(player, true);
        helper.assertTrue(TownUiMode.isDetailed(player)
                        && player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(TownUiMode.TAG),
                "the switch lives in the persisted player data");
        helper.assertTrue(TownUiMode.clearIfUnauthorized(player) && !TownUiMode.isDetailed(player),
                "a player without permission level 2 loses the switch");
        CommandSourceStack source = player.createCommandSourceStack().withPermission(0);
        CommandNode<CommandSourceStack> detailed = TownUiMode.command().build().getChild("detailed");
        helper.assertTrue(detailed != null && !detailed.canUse(source) && detailed.canUse(source.withPermission(2)),
                "the command needs permission level 2");
        helper.succeed();
    }

    private static List<String> problems(InstituteView view) {
        return problems(view.notes());
    }

    /** Every piece of text the player view would show, translation keys with their arguments flattened. */
    private static List<String> texts(InstituteView view) {
        List<Component> parts = new ArrayList<>();
        parts.add(view.header().name());
        parts.add(view.header().subtitle());
        parts.addAll(view.header().reasons());
        for (BoardView.Stat stat : List.of(view.overview().researchers(), view.overview().hands(), view.overview().association())) {
            parts.add(stat.title());
            parts.add(stat.value());
            parts.add(stat.sub());
            parts.addAll(stat.tooltip());
        }
        parts.addAll(view.overview().capacity().tooltip());
        view.overview().conditions().forEach(chip -> parts.add(chip.text()));
        view.overview().research().forEach(line -> parts.add(line.title()));
        for (BoardView.Note note : view.notes()) parts.addAll(List.of(note.line(), note.action(), note.signature()));
        for (BoardView.Note note : view.talk()) parts.addAll(List.of(note.line(), note.signature()));
        parts.add(view.researchers().headline());
        parts.add(view.researchers().slots());
        for (BoardView.Person person : view.researchers().people()) {
            parts.add(person.badge());
            parts.add(person.reason());
            person.bars().forEach(bar -> parts.add(bar.label()));
            parts.addAll(person.lines());
        }
        parts.add(view.research().headline());
        parts.add(view.research().idle());
        parts.add(view.research().empty());
        InstituteView.Association association = view.association();
        parts.addAll(List.of(association.headline(), association.title(), association.detail(), association.action(),
                association.idle(), association.chance(), association.historyTitle()));
        List<String> out = new ArrayList<>();
        for (Component part : parts) out.add(flatten(part));
        return out;
    }

    private static String flatten(Component component) {
        StringBuilder out = new StringBuilder();
        walk(component, out);
        return out.toString();
    }

    private static void walk(Component component, StringBuilder out) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            out.append('[').append(translatable.getKey());
            for (Object arg : translatable.getArgs()) {
                out.append(' ');
                if (arg instanceof Component nested) walk(nested, out);
                else out.append(arg);
            }
            out.append(']');
        } else if (component.getContents() instanceof LiteralContents literal) out.append(literal.text());
        for (Component sibling : component.getSiblings()) walk(sibling, out);
    }
}
