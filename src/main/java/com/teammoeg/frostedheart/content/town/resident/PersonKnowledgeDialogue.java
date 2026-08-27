/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.resident;

import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.knowledge.PersonKnowledgeOverlay;
import com.teammoeg.frostedresearch.knowledge.PersonKnowledgePackageCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Shared server-side presentation adapter for refugee and resident knowledge. */
public final class PersonKnowledgeDialogue {
    private static final String NOTHING_TO_SHARE =
            "message.frostedheart.person_knowledge.nothing_to_share";

    private PersonKnowledgeDialogue() {
    }

    /**
     * Shares the first available package in stable catalogue order. The
     * package decides whether this also creates a team knowledge offer.
     */
    public static void shareFirst(ServerPlayer player, PersonKnowledgeOverlay overlay, String source) {
        PersonKnowledgePackageCatalog.shares(overlay, source).stream().findFirst()
                .ifPresentOrElse(share -> {
                    share.offer().ifPresent(offer -> TeamResearchService.acceptKnowledgeOffer(player, offer));
                    player.displayClientMessage(Component.translatable(share.replyTranslationKey()), false);
                }, () -> player.displayClientMessage(Component.translatable(NOTHING_TO_SHARE), false));
    }
}
