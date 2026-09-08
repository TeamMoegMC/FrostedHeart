/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client;

import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** 玩家展示语义，与原始记录和匹配字段分离。 / Player presentation, independent from record identity. */
public final class KnowledgePresentation {
    private KnowledgePresentation() {}
    public static Component t(String key,Object... args) { return Component.translatable("gui.frostedresearch.journal."+key,args); }
    public static Component text(String value) {
        if(value==null || value.isBlank()) return Component.empty();
        if(Language.getInstance().has(value)) return Component.translatable(value);
        if(technical(value)) return t("unreadable");
        return Component.literal(value);
    }
    private static final java.util.regex.Pattern TECHNICAL=java.util.regex.Pattern.compile(
        "(?:[a-z0-9_.-]+:[a-z0-9_./-]+|[0-9a-fA-F]{8}-[0-9a-fA-F-]{27}|(?:gui|knowledge|item|block|entity|biome)\\.[a-z0-9_.-]+)");
    public static boolean technical(String value) { return TECHNICAL.matcher(value).matches(); }
    public static Component title(KnowledgeElement element) {
        if(element.observation().isPresent()) return object(element.observation().get());
        return element.title().isBlank() || technical(element.title()) && !Language.getInstance().has(element.title())
            ? t("untitled."+element.key().kind().name().toLowerCase(Locale.ROOT)) : text(element.title());
    }
    public static Component body(KnowledgeElement element) { return text(element.body()); }
    public static Component kind(String kind) { return t("kind."+kind.toLowerCase(Locale.ROOT)); }
    public static Component subtype(String type) {
        String key="gui.frostedresearch.journal.subtype."+type.toLowerCase(Locale.ROOT);
        return Language.getInstance().has(key) ? Component.translatable(key) : t("all");
    }
    public static Component status(String status) {
        return t("status."+switch(status.toUpperCase(Locale.ROOT)) {
            case "SUCCESS" -> "success";
            case "ACTIVE" -> "active";
            case "LEARNABLE" -> "learnable";
            case "ALREADY_OWNED", "ALREADY_RECEIVED" -> "known";
            case "NO_USE" -> "no_use";
            case "NO_NEW_DIFFERENCE" -> "similar";
            case "NOT_UNDERSTOOD" -> "understanding";
            case "DEFINITION_UNAVAILABLE" -> "unavailable";
            case "CAPACITY" -> "full";
            case "DAILY_LIMIT" -> "rest";
            case "NO_MATCH", "NO_HINT" -> "no_match";
            case "NOT_FOUND" -> "missing";
            default -> "unchanged";
        });
    }
    public static Component object(Observation observation) {
        ResourceLocation id=observation.object();
        return switch(observation.type()) {
            case BLOCK -> BuiltInRegistries.BLOCK.getOptional(id).map(b->(Component)b.getName()).orElse(t("unknown.object"));
            case ITEM -> BuiltInRegistries.ITEM.getOptional(id).map(i->i.getDescription()).orElse(t("unknown.object"));
            case ENTITY -> BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(e->e.getDescription()).orElse(t("unknown.creature"));
        };
    }
    public static ItemStack icon(KnowledgeElement element) {
        if(element.observation().isEmpty()) return new ItemStack(element.key().kind()==KnowledgeKey.Kind.IDEA ? Items.FEATHER : Items.PAPER);
        Observation o=element.observation().get();
        Item item=switch(o.type()) {
            case BLOCK -> BuiltInRegistries.BLOCK.getOptional(o.object()).map(b->b.asItem()).orElse(Items.AIR);
            case ITEM -> BuiltInRegistries.ITEM.getOptional(o.object()).orElse(Items.AIR);
            case ENTITY -> BuiltInRegistries.ENTITY_TYPE.getOptional(o.object()).map(SpawnEggItem::byId).orElse(null);
        };
        return new ItemStack(item==null || item==Items.AIR ? Items.SPYGLASS : item);
    }
    public static Component source(AcquisitionSource source) { return source(source.mechanism()); }
    public static Component source(String source) {
        String kind=source.startsWith("link:") ? "link" : source.startsWith("research:") ? "research" : switch(source) {
            case "research_note" -> "note";
            case "player_observation", "item_retrieval" -> "self";
            case "initial" -> "initial";
            case "npc_observation", "npc" -> "traveler";
            case "town_observation", "town" -> "town";
            case "world_generated_observation", "world_generated" -> "found";
            default -> "unknown";
        };
        return t("source."+kind);
    }
    public static Component observer(Optional<UUID> observer) {
        var minecraft=Minecraft.getInstance();
        if(observer.isPresent()) {
            if(minecraft!=null && minecraft.player!=null && observer.get().equals(minecraft.player.getUUID())) return t("observer.self");
            if(minecraft!=null && minecraft.getConnection()!=null) {
                var info=minecraft.getConnection().getPlayerInfo(observer.get());
                if(info!=null) return Component.literal(info.getProfile().getName());
            }
        }
        return t("observer.unknown");
    }
    public static Component place(String value,boolean dimension) {
        ResourceLocation id=ResourceLocation.tryParse(value);
        if(id==null) return t("place.unknown");
        String key=(dimension ? "dimension." : "biome.")+id.getNamespace()+"."+id.getPath().replace('/','.');
        String journalKey="gui.frostedresearch.journal."+key;
        if(Language.getInstance().has(journalKey)) return Component.translatable(journalKey);
        return Language.getInstance().has(key) ? Component.translatable(key) : t(dimension ? "dimension.unknown" : "place.unknown");
    }
}
