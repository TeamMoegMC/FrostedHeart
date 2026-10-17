package com.teammoeg.frostedresearch.knowledge.item;

import java.util.Optional;

import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import com.teammoeg.frostedresearch.knowledge.state.KnowledgeEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 研究笔记是知识的抄本，标题与备注不参与身份。 Immutable content with carrier-only annotations.
 */
public final class ResearchNotes {
    public static final int TITLE_LIMIT = 128;
    public static final int REMARKS_LIMIT = 2048;
    public static final String CONTENT_TAG = "KnowledgeNote";

    private ResearchNotes() {
    }

    public record Content(KnowledgeElement element, String title, String remarks,
                          Optional<AcquisitionSource> originalSource) {
    }

    public static boolean isNote(ItemStack stack) {
        return stack.getItem() instanceof ResearchNoteItem;
    }

    public static boolean isBlank(ItemStack stack) {
        return isNote(stack) && (!stack.hasTag() || !stack.getTag().contains(CONTENT_TAG));
    }

    public static Optional<Content> read(ItemStack stack) {
        if (!isNote(stack) || !stack.hasTag() || !stack.getTag().contains(CONTENT_TAG, Tag.TAG_COMPOUND))
            return Optional.empty();
        return decodeContent(stack.getTag().getCompound(CONTENT_TAG));
    }

    static Optional<Content> decodeContent(CompoundTag content) {
        if (!content.contains("element", Tag.TAG_COMPOUND)) return Optional.empty();
        return KnowledgeElement.CODEC.parse(NbtOps.INSTANCE, content.get("element")).result()
                .filter(ResearchNotes::canExport)
                .map(element -> new Content(element, content.getString("title"), content.getString("remarks"),
                        content.contains("original_source") ? AcquisitionSource.CODEC.parse(NbtOps.INSTANCE, content.get("original_source")).result() : Optional.empty()));
    }

    public static boolean canExport(KnowledgeElement element) {
        return element.observation().map(observation -> observation.type() != Observation.Type.ITEM).orElse(true);
    }

    /**
     * Pure creation; callers commit blank-note consumption together with output placement.
     */
    public static ItemStack create(KnowledgeElement element, String title, String remarks) {
        return create(element, Optional.empty(), title, remarks);
    }

    public static ItemStack create(KnowledgeEntry entry, String title, String remarks) {
        return create(entry.element(), Optional.of(entry.originalSource()), title, remarks);
    }

    public static ItemStack create(KnowledgeElement element, Optional<AcquisitionSource> originalSource, String title, String remarks) {
        Optional<CompoundTag> content = encodeContent(element, originalSource, title, remarks);
        if (content.isEmpty()) return ItemStack.EMPTY;
        ItemStack output = new ItemStack(FRContents.Items.RESEARCH_NOTE.get());
        output.getOrCreateTag().put(CONTENT_TAG, content.get());
        return output;
    }

    static Optional<CompoundTag> encodeContent(KnowledgeElement element, Optional<AcquisitionSource> originalSource, String title, String remarks) {
        if (!canExport(element)) return Optional.empty();
        Optional<Tag> encoded = KnowledgeElement.CODEC.encodeStart(NbtOps.INSTANCE, element).result();
        if (encoded.isEmpty()) return Optional.empty();
        CompoundTag content = new CompoundTag();
        content.put("element", encoded.get());
        originalSource.flatMap(source -> AcquisitionSource.CODEC.encodeStart(NbtOps.INSTANCE, source).result())
                .ifPresent(source -> content.put("original_source", source));
        content.putString("title", truncate(title.strip(), TITLE_LIMIT));
        content.putString("remarks", truncate(remarks, REMARKS_LIMIT));
        return Optional.of(content);
    }

    public static Component title(Content content) {
        if (!content.title().isBlank()) return Component.literal(content.title());
        KnowledgeElement element = content.element();
        if (!element.title().isBlank() && !element.title().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            return Component.translatableWithFallback(element.title(), element.title());
        Component subject = element.observation().map(observation -> switch (observation.type()) {
            case BLOCK ->
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(observation.object()).map(block -> (Component) block.getName()).orElse(Component.translatable("gui.frostedresearch.journal.unknown.object"));
            case ITEM ->
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(observation.object()).map(item -> item.getDescription()).orElse(Component.translatable("gui.frostedresearch.journal.unknown.object"));
            case ENTITY ->
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(observation.object()).map(entity -> entity.getDescription()).orElse(Component.translatable("gui.frostedresearch.journal.unknown.creature"));
        }).orElse(Component.translatable("gui.frostedresearch.journal.untitled." + element.key().kind().name().toLowerCase(java.util.Locale.ROOT)));
        return Component.translatable("knowledge.frostedresearch.note_title." + element.key().kind().name().toLowerCase(java.util.Locale.ROOT), subject);
    }

    private static String truncate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit);
    }
}
