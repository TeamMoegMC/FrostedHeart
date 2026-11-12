package com.teammoeg.frostedresearch.knowledge.client;
import net.minecraft.nbt.*;
/** Server supplied replacement and prerequisite projection, including undiscovered project IDs only. */
public final class LegacyResearchClient {
    private LegacyResearchClient() {}
    public static CompoundTag replacement(String id) {
        for (Tag tag : KnowledgeClientState.snapshot().getList("legacy_replacements", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            if (entry.getString("research").equals(id)) return entry;
        }
        return new CompoundTag();
    }
    public static boolean replaced(String id) { return !replacement(id).isEmpty(); }
    public static boolean completed(String id) { return replacement(id).getBoolean("effective_completed"); }
    public static void open(String id) {
        String project = replacement(id).getString("project");
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.screen instanceof com.teammoeg.chorda.client.cui.screenadapter.CUIScreen cui
            && cui.getPrimaryLayer() instanceof com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen desk) {
            desk.showKnowledge();
            desk.getKnowledgeLayer().openLegacyProject(project);
        }
    }
}
