package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.*;

/** One ordering and standby rule for retained runs; no migration or mutation of merged history. */
public final class ResearchRuns {
    private ResearchRuns() {}
    public static <T> Comparator<T> order(ToLongFunction<T> time,Function<T,String> id) {
        return Comparator.comparingLong(time).thenComparing(id);
    }
    public static final Comparator<ResearchHistory> ORDER=order(ResearchHistory::time,r -> r.id().toString());
    public static List<ResearchHistory> sorted(KnowledgeState state) {
        return state.research().values().stream().sorted(ORDER).toList();
    }
    public static List<ResearchHistory> project(KnowledgeState state,ResourceLocation project) {
        return sorted(state).stream().filter(r -> r.project().equals(project)).toList();
    }
    /** External results alone cannot count as actually completed research. Invalid/forgotten results restore work. */
    public static boolean completed(KnowledgeState state,ResourceLocation project) {
        var definition=KnowledgeDefinitions.current().projects().get(project);
        return definition!=null && !definition.results().isEmpty()
            && state.research().values().stream().anyMatch(r -> r.completed() && r.project().equals(project))
            && definition.results().stream().allMatch(id -> {
                var entry=state.archive().entries().get(KnowledgeKey.result(id));
                return entry!=null && KnowledgeAvailability.available(entry.element());
            });
    }
    public static boolean standby(KnowledgeState state,ResearchHistory run) {
        return run!=null && run.open() && completed(state,run.project());
    }
    public static int number(KnowledgeState state,ResearchHistory run) {
        return project(state,run.project()).indexOf(run)+1;
    }
}
