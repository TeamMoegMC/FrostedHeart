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

package com.teammoeg.frostedheart.content.town.buildings.institute.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteBuilding;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteDailyModel;
import com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteView.*;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardPresenters;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 把研究所的建筑记录、研究员、城镇与知识快照换成 {@link InstituteView}。纯函数：不读客户端状态，GameTest 可以直接调用。
 * 规则见 plans/2026-11-09_21-45-44_town-ui-redesign.md 的「研究所（样板）完整设计」。
 * <p>
 * Turns the institute's building record, researchers, town and knowledge snapshot into an {@link InstituteView}.
 * Pure: it reads no client state, so GameTests call it directly.
 */
public final class InstituteViewPresenter {
    public static final String KEY = "gui.frostedheart.institute.";
    private static final int MAX_IMPROVEMENTS = 3, MAX_NOTES = 6, MAX_OVERVIEW_RESEARCH = 4;
    /** Tab of the association page, for links from the overview. */
    public static final int TAB_ASSOCIATION = 3;

    /**
     * @param building    研究所；为空时显示「数据暂不可用」 / the institute, null while its data has not arrived
     * @param researchers 分到这里的居民 / residents assigned here
     * @param town        城镇快照，用来找住处与预测人手 / town snapshot for homes and the forecast
     * @param knowledge   队伍的知识快照 / the team's knowledge snapshot
     */
    public record Inputs(@Nullable ResearchInstituteBuilding building, BlockPos pos, List<Resident> researchers,
                         @Nullable TeamTown town, @Nullable KnowledgeJournalData knowledge, String townName, long townDay) {
    }

    private InstituteViewPresenter() {
    }

    static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(KEY + key, args);
    }

    static Component b(String key, Object... args) {
        return BoardPresenters.b(key, args);
    }

    static Component education(int level) {
        return BoardPresenters.education(level);
    }

    public static InstituteView present(Inputs in) {
        ResearchInstituteBuilding building = in.building();
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        Map<BlockPos, AbstractTownBuilding> buildings = in.town() == null ? Map.of() : in.town().getTownBuildings();

        List<Double> forecast = building == null || in.town() == null ? List.of() : building.forecastLabor(in.town());
        double hands = forecast.stream().mapToDouble(Double::doubleValue).sum();
        List<Person> people = new ArrayList<>();
        for (Resident resident : in.researchers()) people.add(researcher(building, resident, buildings));
        KnowledgeJournalData knowledge = in.knowledge();
        KnowledgeJournalData.LaborDay day = knowledge == null ? null : knowledge.laborDay;
        int minEducation = config.associationMinimumEducation.get();
        int bestEducation = in.researchers().stream().filter(r -> canWork(building, r))
                .mapToInt(Resident::getEducationLevel).max().orElse(-1);

        Header header = header(in, building, buildings, hands);
        Research research = research(knowledge, day, hands, bestEducation);
        Association association = association(day, people, knowledge, minEducation, bestEducation);
        Overview overview = overview(building, people, hands, knowledge, research, association);
        List<Note> notes = notes(in, building, people, buildings, knowledge, research, minEducation, bestEducation);
        List<Note> talk = talk(building, people, research, day, Math.max(0, MAX_NOTES - notes.size()));
        Researchers team = new Researchers(researchersHeadline(forecast, hands), people,
                building == null ? Component.empty() : slotsLine(building, people.size()),
                building == null ? 0 : Math.max(0, building.getMaxResidents() - people.size()));
        return new InstituteView(header, overview, notes, talk, team, research, association);
    }

    // ---------------------------------------------------------------- header and status

    private static Header header(Inputs in, ResearchInstituteBuilding building, Map<BlockPos, AbstractTownBuilding> buildings, double hands) {
        if (building == null)
            return new Header(t("name.unknown"), Component.empty(), Status.STOPPED, List.of(b("reason.missing")), 0);
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        List<Component> stopped = stopReasons(building);
        if (stopped.isEmpty() && TownFormat.whole(hands) == 0 && !TownFormat.underOne(hands))
            stopped = List.of(b("reason.no_workers"));
        List<Component> limited = new ArrayList<>();
        if (stopped.isEmpty()) {
            int onDuty = (int) in.researchers().stream().filter(r -> canWork(building, r)).count();
            if (in.researchers().size() < building.getMaxResidents()) limited.add(b("reason.empty_slots", building.getMaxResidents() - in.researchers().size()));
            if (onDuty < in.researchers().size()) limited.add(b("reason.off_duty", in.researchers().size() - onDuty));
            if (TownFormat.frost(building.getEffectiveTemperature(), config.minimumWorkingTemperatureCelsius.get()) == 1)
                limited.add(b("reason.cool"));
            if (building.getAverageLight() < config.minimumAverageBlockLight.get() + 2) limited.add(b("reason.dim"));
            if (building.getDesks() > building.getMaxResidents()) limited.add(t("reason.crowded"));
        }
        int frost = TownFormat.frost(building.getEffectiveTemperature(), config.minimumWorkingTemperatureCelsius.get());
        return BoardPresenters.header(building, in.town(), in.pos(), in.townName(), in.townDay(), stopped, limited, frost);
    }

    /** Why the building cannot work at all, in the order players should fix them. */
    static List<Component> stopReasons(ResearchInstituteBuilding building) {
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        List<Component> reasons = BoardPresenters.roomReasons(building, building.getArea(), config.minimumFloorAreaBlocks.get(),
                building.getVolume(), config.minimumInteriorVolumeBlocks.get());
        if (!building.isTemperatureValid()) reasons.add(b("reason.cold", minTemperature()));
        if (!building.isLightValid()) reasons.add(b("reason.dark"));
        return reasons;
    }

    private static int minTemperature() {
        return (int) Math.ceil(FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.minimumWorkingTemperatureCelsius.get());
    }

    // ---------------------------------------------------------------- overview

    private static Overview overview(ResearchInstituteBuilding building, List<Person> people, double hands,
                                     KnowledgeJournalData knowledge, Research research, Association association) {
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        int slots = building == null ? 0 : building.getMaxResidents();
        Stat researchers = BoardPresenters.workersStat(t("stat.researchers"), people, slots);
        Stat handsStat = new Stat(t("stat.hands"), TownFormat.hands(hands), t("stat.hands.sub"), Tone.MUTED, List.of(t("stat.hands.tooltip")), 2);

        KnowledgeJournalData.Capacity capacity = knowledge == null ? KnowledgeJournalData.Capacity.UNKNOWN : knowledge.capacity;
        int shelves = building == null ? 0 : building.getBookshelves();
        List<Component> capacityTooltip = List.of(
                t("stat.capacity.tooltip", shelves,
                        ResearchInstituteDailyModel.capacity(shelves, config.archiveObservationsPerBookshelf.get()),
                        ResearchInstituteDailyModel.capacity(shelves, config.inboxObservationsPerBookshelf.get())),
                t("stat.capacity.paused"));
        Capacity capacityView = new Capacity(capacity.archiveUsed(), capacity.archiveLimit(), capacity.inboxUsed(),
                capacity.inboxLimit(), shelves, capacityTooltip);

        Stat associationStat = new Stat(t("stat.association"), t("stat.association." + association.outcome().name().toLowerCase(Locale.ROOT)),
                t("stat.association.more"), Tone.MUTED, List.of(), TAB_ASSOCIATION);

        List<Chip> chips = new ArrayList<>();
        if (building != null) {
            chips.add(BoardPresenters.spaceChip(building.isSpaceValid(), building.getMaxResidents()));
            chips.add(BoardPresenters.temperatureChip(building.getEffectiveTemperature(), config.minimumWorkingTemperatureCelsius.get(),
                    Double.POSITIVE_INFINITY));
            chips.add(BoardPresenters.lightChip(building.getAverageLight(), config.minimumAverageBlockLight.get()));
            chips.add(BoardPresenters.decorationChip(building.getDecorationRating()));
        }

        List<ProgressLine> lines = new ArrayList<>();
        for (WorkOrder order : research.orders()) {
            if (lines.size() >= MAX_OVERVIEW_RESEARCH) break;
            if (order.stamp() == OrderStamp.DONE) continue;
            boolean stopped = order.stamp() == OrderStamp.STOP;
            lines.add(new ProgressLine(order.title(), order.percent(), stopped ? Tone.BAD : order.percent() >= 50 ? Tone.GOOD : Tone.WARN, stopped));
        }
        return new Overview(researchers, handsStat, capacityView, associationStat, chips, lines);
    }

    // ---------------------------------------------------------------- researchers

    /**
     * 这个想法是由哪些知识想到的：找出产出它的联想链接，列出它的来源。没有记录时为空。
     * Which knowledge an idea came from: the association link that produced it, by its inputs; null without a record.
     */
    @Nullable
    public static Component sources(@Nullable KnowledgeJournalData knowledge, @Nullable com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey subject) {
        if (knowledge == null || subject == null) return null;
        for (KnowledgeJournalData.Relation relation : knowledge.relations) {
            if (relation.research() || !relation.outputs().contains(subject) || relation.inputs().isEmpty()) continue;
            List<Component> titles = relation.inputs().stream().map(knowledge::title).toList();
            return switch (titles.size()) {
                case 1 -> t("assoc.from.one", titles.get(0));
                case 2 -> t("assoc.from.two", titles.get(0), titles.get(1));
                default -> t("assoc.from.many", titles.get(0), titles.get(1));
            };
        }
        return null;
    }

    static boolean canWork(@Nullable ResearchInstituteBuilding building, Resident resident) {
        return building == null ? BoardPresenters.fitToWork(resident) : building.canResidentWork(resident);
    }

    private static Person researcher(@Nullable ResearchInstituteBuilding building, Resident resident, Map<BlockPos, AbstractTownBuilding> buildings) {
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        Component capable = resident.getEducationLevel() <= 0 ? t("person.capable.basic") : t("person.capable", education(resident.getEducationLevel()));
        // On duty only while the institute works; canWork alone still decides who counts for the schooling notes.
        return BoardPresenters.person(resident, canWork(building, resident) && (building == null || building.isBuildingWorkable()),
                building == null ? 0 : building.getResearcherLabor(resident),
                ResearchInstituteBuilding.class, t("person.skill"), "intelligence", config.physicalActivity.get(), config.learningActivity.get(),
                List.of(capable), buildings);
    }

    private static Component researchersHeadline(List<Double> forecast, double hands) {
        MutableComponent tiers = Component.empty();
        boolean first = true;
        for (int level = forecast.size() - 1; level >= 0; level--) {
            double value = forecast.get(level);
            if (!(value > 1e-6)) continue;
            if (!first) tiers.append(b("separator"));
            tiers.append(t("researchers.tier", education(level), TownFormat.hands(value)));
            first = false;
        }
        return first ? t("researchers.headline.none", TownFormat.hands(hands)) : t("researchers.headline", TownFormat.hands(hands), tiers);
    }

    private static Component slotsLine(ResearchInstituteBuilding building, int assigned) {
        return assigned < building.getMaxResidents() ? b("workers.empty_slot") : t("researchers.full", building.getDesks());
    }

    // ---------------------------------------------------------------- collaborative research

    private static Research research(@Nullable KnowledgeJournalData knowledge, @Nullable KnowledgeJournalData.LaborDay day, double hands,
                                     int bestEducation) {
        List<WorkOrder> orders = new ArrayList<>();
        if (knowledge != null) {
            for (KnowledgeJournalData.Run run : knowledge.runs) {
                if (!run.open()) continue;
                for (KnowledgeJournalData.Requirement requirement : run.requirements()) {
                    if (requirement.labor() == null || requirement.evidence()) continue;
                    orders.add(order(run, requirement, day, bestEducation));
                }
            }
        }
        Component idle;
        boolean idleLinks = true;
        if (day == null) {
            idle = Component.empty();
            idleLinks = false;
        } else if (TownFormat.underOne(day.idleTotal())) idle = t("research.idle.less");
        else if (TownFormat.whole(day.idleTotal()) > 0) idle = t("research.idle", TownFormat.whole(day.idleTotal()));
        else {
            idle = t("research.idle.none");
            idleLinks = false;
        }
        return new Research(t("research.headline", TownFormat.hands(hands)), List.of(t("research.headline.tooltip")), orders, idle, idleLinks,
                t("research.empty"));
    }

    private static WorkOrder order(KnowledgeJournalData.Run run, KnowledgeJournalData.Requirement requirement,
                                   @Nullable KnowledgeJournalData.LaborDay day, int bestEducation) {
        KnowledgeJournalData.Labor labor = requirement.labor();
        Component requirementTitle = requirement.title().getString().isBlank()
                ? t(requirement.experiment() ? "order.experiment" : "order.calculation") : requirement.title();
        Component title = t("order.title", run.title(), requirementTitle);
        double share = Math.max(1e-6, labor.share());
        boolean complete = labor.required() && labor.remaining() <= 1e-6;
        KnowledgeJournalData.Allocation allocation = day == null ? null : day.allocation(run.id(), requirement.id());
        boolean noOne = !complete && bestEducation < labor.education();

        int percent;
        Component progress;
        if (labor.required()) {
            percent = TownFormat.percent(Math.min(labor.done(), share), share);
            double rate = allocation != null && allocation.given() > 1e-6 ? allocation.given() : labor.workers();
            int daysLeft = (int) Math.max(1, Math.ceil(labor.remaining() / Math.max(1e-6, rate) - 1e-9));
            progress = complete ? t("order.complete") : noOne ? t("order.stopped", percent) : t("order.days_left", percent, daysLeft);
        } else {
            double current = labor.remaining() <= 1e-6 ? share : labor.done() - labor.shares() * share;
            percent = TownFormat.percent(Math.max(0, current), share);
            progress = labor.shares() > 0 ? t("order.shared", labor.shares(), percent) : t("order.share", percent);
        }
        List<Component> details = new ArrayList<>();
        details.add(t("order.need", labor.workers(), labor.days()));
        if (!labor.items().isEmpty()) {
            MutableComponent items = Component.empty();
            for (int i = 0; i < labor.items().size(); i++) {
                if (i > 0) items.append(t("order.item_separator"));
                items.append(labor.items().get(i));
            }
            details.add(t("order.daily", items));
        }
        if (labor.education() > 0) details.add(t("order.education", education(labor.education())));

        Component today = Component.empty();
        Component problem = Component.empty();
        OrderStamp stamp = complete ? OrderStamp.DONE : OrderStamp.NONE;
        if (!complete && allocation != null) {
            if (allocation.given() > 1e-6) {
                int whole = (int) Math.floor(allocation.given() / share + 1e-6);
                today = !labor.required() && whole >= 1 ? t("order.today.shares", whole) : t("order.today", TownFormat.percent(allocation.given(), share));
            }
            if (allocation.given() <= 1e-6) {
                if (allocation.itemsShort()) {
                    problem = t("order.no_items");
                    stamp = OrderStamp.SHORT;
                } else if (noOne) {
                    problem = t("order.no_one", education(labor.education()));
                    stamp = OrderStamp.STOP;
                } else problem = t("order.no_turn");
            } else if (allocation.given() < allocation.wanted() - 1e-6) {
                if (allocation.itemsShort()) {
                    problem = t("order.short_items");
                    stamp = OrderStamp.SHORT;
                } else problem = t("order.short_hands");
            }
        } else if (noOne) {
            problem = t("order.no_one", education(labor.education()));
            stamp = OrderStamp.STOP;
        }
        return new WorkOrder(run.id(), run.project(), title, requirement.experiment(), labor.required(), percent, progress,
                details, today, problem, stamp, labor.education());
    }

    // ---------------------------------------------------------------- association

    private static Association association(@Nullable KnowledgeJournalData.LaborDay day, List<Person> people,
                                           @Nullable KnowledgeJournalData knowledge, int minEducation, int bestEducation) {
        Component headline = minEducation <= 0 ? t("assoc.headline.any") : t("assoc.headline", education(minEducation));
        Outcome outcome;
        Component title = Component.empty(), detail = Component.empty(), action = Component.empty();
        double chance = 0;
        if (day == null) {
            outcome = Outcome.NEVER;
            title = t("assoc.never");
        } else {
            outcome = switch (day.outcome()) {
                case "idea" -> Outcome.IDEA;
                case "hint" -> Outcome.HINT;
                case "quiet" -> Outcome.QUIET;
                case "nothing" -> Outcome.NOTHING;
                default -> Outcome.NONE;
            };
            chance = outcome == Outcome.NONE ? 0 : day.chance();
            switch (outcome) {
                case IDEA -> {
                    title = day.subjectTitle();
                    Component from = sources(knowledge, day.subject());
                    detail = from == null ? t("assoc.idea") : from;
                    action = t("assoc.idea.action");
                }
                case HINT -> {
                    title = t("assoc.hint", day.subjectTitle());
                    action = t("assoc.hint.action");
                }
                case QUIET -> title = t("assoc.quiet");
                case NOTHING -> {
                    title = t("assoc.nothing");
                    action = t("assoc.nothing.action");
                }
                default -> {
                    title = t("assoc.none");
                    if (bestEducation < 0) {
                        // Nobody works here at all; education is not the reason.
                        detail = t("assoc.none.nobody");
                        action = t("assoc.none.nobody.action");
                    } else if (bestEducation < minEducation) {
                        detail = t("assoc.none.education", education(minEducation));
                        action = t("assoc.none.action", education(minEducation));
                    } else detail = t("assoc.none.busy");
                }
            }
        }
        boolean thought = outcome != Outcome.NONE && outcome != Outcome.NEVER;
        List<Person> thinkers = thought ? people.stream().filter(p -> p.duty() == Duty.ON_DUTY).toList() : List.of();
        Component idle = thought ? t("assoc.idle", TownFormat.hands(day.associationLabor())) : Component.empty();
        List<InstituteView.PastIdea> history = pastIdeas(knowledge);
        List<Component> tips = new ArrayList<>();
        if (outcome != Outcome.IDEA) {
            if (bestEducation >= 0 && bestEducation < minEducation || bestEducation < 0 && minEducation > 0)
                tips.add(t("assoc.tip.education", education(minEducation)));
            tips.add(t("assoc.tip.busy"));
            tips.add(t("assoc.tip.knowledge"));
        }
        // With nothing left to connect, the chance is not "high"; it says so.
        boolean exhausted = outcome == Outcome.NOTHING;
        return new Association(headline, outcome, title, detail, action, thinkers, idle, exhausted ? 0 : TownFormat.chanceSteps(chance),
                exhausted ? t("assoc.chance.exhausted") : TownFormat.chance(chance).text(), List.of(t("assoc.chance.tooltip")),
                t("assoc.history", history.size()), history, tips);
    }

    /**
     * 研究所想到过的想法，最新的在上；没记下日子的旧想法排在后面，保持札记里的顺序。
     * The ideas the institute found, newest first; older ones without a recorded day follow in the journal's order.
     */
    public static List<InstituteView.PastIdea> pastIdeas(@Nullable KnowledgeJournalData knowledge) {
        List<InstituteView.PastIdea> ideas = new ArrayList<>();
        if (knowledge == null) return ideas;
        List<KnowledgeJournalData.Entry> found = new ArrayList<>();
        for (KnowledgeJournalData.Entry entry : knowledge.archive)
            if (entry.source() != null && entry.source().startsWith("institute:")) found.add(entry);
        found.sort(java.util.Comparator.comparingLong((KnowledgeJournalData.Entry entry) -> -knowledge.instituteDays.getOrDefault(entry.key(), Long.MIN_VALUE + 1)));
        // Days are recorded from the first dated idea on, so an undated one was found before the earliest of them.
        long earliest = found.stream().map(entry -> knowledge.instituteDays.get(entry.key())).filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue).min().orElse(Long.MIN_VALUE);
        for (KnowledgeJournalData.Entry entry : found) {
            Long day = knowledge.instituteDays.get(entry.key());
            Component when = day != null ? t("assoc.history.day", day)
                    : earliest != Long.MIN_VALUE ? t("assoc.history.before", earliest) : Component.empty();
            Component from = sources(knowledge, entry.key());
            ideas.add(new InstituteView.PastIdea(entry.key(), entry.title(), when,
                    from == null ? Component.empty() : from, entry.body() == null ? Component.empty() : entry.body()));
        }
        return ideas;
    }

    // ---------------------------------------------------------------- notes

    private static List<Note> notes(Inputs in, @Nullable ResearchInstituteBuilding building, List<Person> people,
                                    Map<BlockPos, AbstractTownBuilding> buildings, @Nullable KnowledgeJournalData knowledge,
                                    Research research, int minEducation, int bestEducation) {
        List<Note> notes = new ArrayList<>();
        if (building == null) return notes;
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        Component signer = BoardPresenters.signer(people, null);
        Note cold = BoardPresenters.coldNote(building.getEffectiveTemperature(), config.minimumWorkingTemperatureCelsius.get(), signer);
        if (cold != null) notes.add(cold);
        if (!building.isLightValid()) notes.add(BoardPresenters.improve("dark", b("note.dark"), b("note.dark.action"), signer));
        List<Component> room = BoardPresenters.roomReasons(building, building.getArea(), config.minimumFloorAreaBlocks.get(),
                building.getVolume(), config.minimumInteriorVolumeBlocks.get());
        if (!room.isEmpty())
            notes.add(BoardPresenters.improve("structure", room.get(0),
                    BoardPresenters.roomAction(building, building.getArea(), config.minimumFloorAreaBlocks.get(), config.minimumInteriorVolumeBlocks.get()), signer));
        for (int i = 0; i < people.size() && i < in.researchers().size(); i++) {
            if (people.get(i).duty() == Duty.ON_DUTY || BoardPresenters.fitToWork(in.researchers().get(i))) continue;
            notes.add(BoardPresenters.unfitNote(in.researchers().get(i), people.get(i), people, buildings));
            break;
        }
        if (knowledge != null) {
            KnowledgeJournalData.Capacity capacity = knowledge.capacity;
            boolean inboxFull = capacity.inboxLimit() > 0 && capacity.inboxUsed() >= 0.9 * capacity.inboxLimit();
            boolean archiveFull = capacity.archiveLimit() > 0 && capacity.archiveUsed() >= 0.9 * capacity.archiveLimit();
            if (inboxFull || archiveFull)
                notes.add(BoardPresenters.improve("full", t(inboxFull ? "note.full.inbox" : "note.full.archive"),
                        t("note.full.action", config.inboxObservationsPerBookshelf.get(), config.archiveObservationsPerBookshelf.get()), signer));
        }
        WorkOrder blocked = research.orders().stream().filter(o -> o.stamp() == OrderStamp.STOP).findFirst().orElse(null);
        if (blocked != null || (bestEducation >= 0 && bestEducation < minEducation))
            // The note names the schooling the stopped task itself asks for.
            notes.add(BoardPresenters.improve("education", blocked != null
                            ? t("note.education", blocked.title(), education(blocked.education()))
                            : t("note.education.association", education(minEducation)),
                    t("note.education.action", education(blocked != null ? blocked.education() : minEducation)), signer));
        int perResearcher = (int) Math.ceil(config.floorBlocksPerResearcher.get());
        if (building.getDesks() > building.getMaxResidents())
            notes.add(BoardPresenters.improve("crowded", t("note.crowded"), t("note.crowded.action", perResearcher), signer));
        else if (building.isSpaceValid() && building.getArea() / Math.max(1.0, config.floorBlocksPerResearcher.get()) >= building.getDesks() + 1)
            notes.add(BoardPresenters.improve("desk", t("note.desk"), t("note.desk.action"), signer));
        int stars = TownFormat.stars(building.getDecorationRating());
        if (stars < 3) {
            double bonus = config.decorationBonusAtFullRating.get();
            double gain = ResearchInstituteDailyModel.decorationMultiplier(0.6, bonus)
                    / ResearchInstituteDailyModel.decorationMultiplier(building.getDecorationRating(), bonus) - 1;
            notes.add(BoardPresenters.improve("decoration", t("note.decoration"), t("note.decoration.action", TownFormat.percent(gain)), signer));
        }
        return BoardPresenters.firstNotes(notes, MAX_IMPROVEMENTS);
    }

    private static List<Note> talk(@Nullable ResearchInstituteBuilding building, List<Person> people, Research research,
                                   @Nullable KnowledgeJournalData.LaborDay day, int room) {
        List<Note> talk = new ArrayList<>();
        if (room <= 0) return talk;
        Component signer = BoardPresenters.signer(people, null);
        research.orders().stream().filter(o -> o.stamp() != OrderStamp.DONE && o.percent() >= 80).findFirst()
                .ifPresent(o -> talk.add(BoardPresenters.chat(t("talk.almost_done", o.title()), signer)));
        if (day != null && "idea".equals(day.outcome())) talk.add(BoardPresenters.chat(t("talk.idea", day.subjectTitle()), signer));
        if (building != null && TownFormat.frost(building.getEffectiveTemperature(),
                FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.minimumWorkingTemperatureCelsius.get()) == 0)
            talk.add(BoardPresenters.chat(t("talk.warm"), signer));
        if (building != null && building.getBookshelves() >= 8) talk.add(BoardPresenters.chat(t("talk.shelves"), signer));
        for (int i = 1; i <= 4; i++) talk.add(BoardPresenters.chat(t("talk.daily." + i), signer));
        return talk.size() > room ? List.copyOf(talk.subList(0, room)) : talk;
    }
}
