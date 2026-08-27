package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.content.town.resident.PersonKnowledgeIntegration;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonKnowledgeOverlayTest {
    @BeforeAll
    static void registerContentPackages() {
        PersonKnowledgeIntegration.register();
    }

    @Test
    void tenPercentBoundaryIsExactForEligibleAges() {
        assertTrue(PersonKnowledgeOverlay.initialize(true, 9)
                .has(PersonKnowledgeIntegration.PROSPECTING_EXPERIENCE));
        assertFalse(PersonKnowledgeOverlay.initialize(true, 10)
                .has(PersonKnowledgeIntegration.PROSPECTING_EXPERIENCE));
        assertTrue(PersonKnowledgeOverlay.initialize(true, 0)
                .has(PersonKnowledgeIntegration.PROSPECTING_EXPERIENCE));
        assertFalse(PersonKnowledgeOverlay.initialize(false, 0)
                .has(PersonKnowledgeIntegration.PROSPECTING_EXPERIENCE));
    }

    @Test
    void nonGeologyPackageUsesTheSameBackgroundPool() {
        PersonKnowledgeOverlay overlay = PersonKnowledgeOverlay.initialize(true, 10);
        assertTrue(overlay.has(PersonKnowledgeIntegration.COLD_WEATHER_EXPERIENCE));
        assertFalse(overlay.has(PersonKnowledgeIntegration.PROSPECTING_EXPERIENCE));

        PersonKnowledgePackageCatalog.Share share = PersonKnowledgePackageCatalog
                .shares(overlay, "person:test").get(0);
        assertEquals(PersonKnowledgeIntegration.COLD_WEATHER_EXPERIENCE, share.packageId());
        assertTrue(share.offer().isEmpty(), "conversation packages need not be tied to a research topic");
    }

    @Test
    void prospectingPackageOwnsItsSpecificOfferInsteadOfTheDialogueRpc() {
        PersonKnowledgeOverlay overlay = PersonKnowledgeOverlay.initialize(true, 0);
        PersonKnowledgePackageCatalog.Share share = PersonKnowledgePackageCatalog
                .shares(overlay, "person:abc").get(0);

        KnowledgeOffer offer = share.offer().orElseThrow();
        assertEquals(PersonKnowledgeIntegration.PROSPECTING_EXPERIENCE, offer.provider());
        assertEquals("person:abc", offer.source());
    }

    @Test
    void emptyOutcomeIsStillInitializedAndStable() {
        PersonKnowledgeOverlay overlay = PersonKnowledgeOverlay.initialize(true, 73);
        assertTrue(overlay.initialized());
        assertTrue(overlay.knowledgeIds().isEmpty());
        assertEquals(73, overlay.backgroundRoll());
        assertTrue(PersonKnowledgePackageCatalog.shares(overlay, "person:empty").isEmpty());
    }

    @Test
    void persistedOrTransferredOverlayNeverRerolls() {
        PersonKnowledgeOverlay original = PersonKnowledgeOverlay.initialize(true, 10);
        PersonKnowledgeOverlay decoded = PersonKnowledgeOverlay.CODEC.parse(JsonOps.INSTANCE,
                PersonKnowledgeOverlay.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow())
                .result().orElseThrow();

        assertEquals(original, decoded);
        assertSame(decoded, decoded.initializeIfNeeded(true, 0));

        Resident recruited = new Resident("Ada", "Snow");
        recruited.setKnowledgeOverlay(decoded);
        assertSame(decoded, recruited.getKnowledgeOverlay());
    }
}
