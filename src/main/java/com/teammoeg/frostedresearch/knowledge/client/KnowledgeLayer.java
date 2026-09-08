/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.*;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.drawdesk.*;
import com.teammoeg.frostedresearch.knowledge.client.ui.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

import static com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation.t;
import static com.teammoeg.frostedresearch.knowledge.client.ui.KnowledgeUiTheme.*;

/**
 * 独立知识札记 CUI；界面状态不决定知识判定。 / Independent knowledge journal surface.
 */
public final class KnowledgeLayer extends UILayer {
    private static final com.teammoeg.chorda.client.icon.CIcons.CTextureIcon DESK_INVENTORY = DrawDeskIcons.ALL.withUV(0, 0, 140, 203, 512, 512);

    private enum Page {INBOX, ARCHIVE, RELATIONS, HINTS}

    private enum Mode {READ, CANDIDATES, THINK, DELETE, TOOLS}

    private final DrawDeskContainer menu;
    private final Runnable back, surfaceChanged;
    private final JournalList list;
    private final JournalText reading;
    private final KnowledgeGraphViewport overview;
    private final KnowledgeNodeSheet nodeSheet;
    private KnowledgeGraphModel graphModel;
    private boolean graphDirty = true;
    private final TextBox search, noteTitleBox, noteRemarksBox;
    private KnowledgeJournalData data;
    private Page page = Page.INBOX;
    private Mode mode = Mode.READ;
    private KnowledgeKey.Kind kind = KnowledgeKey.Kind.OBSERVATION;
    private KnowledgeKey focused;
    private int hintIndex = -1, candidateIndex;
    private boolean organize, details, history, filterOpen;
    private String query = "", statusFilter = "", subtype = "", partner = "", draftTitle = "", draftRemarks = "";
    private final LinkedHashSet<KnowledgeKey> selected = new LinkedHashSet<>();
    private final Set<String> expanded = new HashSet<>();
    private final List<CompoundTag> candidates = new ArrayList<>();
    private List<KnowledgeJournalData.Entry> filtered = List.of();
    private Component notice = Component.empty();
    private List<Component> notices = List.of();
    private KnowledgeLayout layout;
    private Language language;
    private long snapshotSerial = -1;
    private JournalButton learn, remove, combine, retrieve;
    private final JournalText toolSelection;
    private ItemStack inspectedStack = ItemStack.EMPTY;
    private KnowledgeKey inspectedKey;
    private java.util.UUID retrievalRequest;
    private boolean retrievePending;
    private Component toolFeedback = Component.empty();
    private boolean inspectedPresent;
    private int noticeTicks;
    private int lastBatchCount = -1;
    private boolean rowsDirty = true;

    public KnowledgeLayer(UIElement parent, DrawDeskContainer menu, Runnable back, Runnable surfaceChanged) {
        super(parent);
        this.menu = menu;
        this.back = back;
        this.surfaceChanged = surfaceChanged;
        setTheme(INSTANCE);
        setScissorEnabled(false);
        list = new JournalList(this, this::rowClicked);
        reading = new JournalText(this);
        toolSelection = new JournalText(this);
        overview = new KnowledgeGraphViewport(this, this::openGraphNode);
        nodeSheet = new KnowledgeNodeSheet(this, this::closeGraphSheet, this::openGraphNode);
        search = new TextBox(this) {
            @Override
            public void onTextChanged() {
                query = getText();
                if (page == Page.RELATIONS) overview.find(query);
                else {
                    list.top();
                    rebuildRows();
                }
            }
        };
        noteTitleBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                draftTitle = getText();
            }
        };
        noteRemarksBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                draftRemarks = getText();
            }
        };
        noteTitleBox.setMaxLength(128);
        noteRemarksBox.setMaxLength(2048);
        search.setMaxLength(80);
        search.ghostText = t("search").getString();
        data = new KnowledgeJournalData(KnowledgeClientState.snapshot());
        language = Language.getInstance();
        setSize(456, 246);
    }

    public boolean toolsOpen() {
        return mode == Mode.TOOLS;
    }

    public void resize(int width, int height) {
        if (width != getWidth() || height != getHeight() || layout == null) {
            setSize(width, height);
            rebuild();
        }
    }

    public void opened() {
        refreshSnapshot();
        send(Action.REFRESH, List.of(), "", "", "");
    }

    public void refreshSnapshot() {
        if (snapshotSerial == KnowledgeClientState.serial() && language == Language.getInstance()) return;
        snapshotSerial = KnowledgeClientState.serial();
        language = Language.getInstance();
        data = new KnowledgeJournalData(KnowledgeClientState.snapshot());
        graphDirty = true;
        refreshInspectedPresence();
        Set<KnowledgeKey> retained = new HashSet<>();
        (page == Page.INBOX ? data.inbox : data.archive).forEach(e -> retained.add(e.key()));
        selected.removeIf(key -> !retained.contains(key) || page == Page.ARCHIVE && !data.entry(key).active());
        if (focused != null && data.entry(focused) == null) focused = null;
        rowsDirty = true;
        rebuild();
    }

    private void rebuild() {
        layout = KnowledgeLayout.of(getWidth(), getHeight(), page == Page.ARCHIVE && !selected.isEmpty() && mode != Mode.TOOLS);
        refresh();
    }

    @Override
    public void addUIElements() {
        learn = remove = combine = null;
        if (mode == Mode.TOOLS) {
            buildTools();
            return;
        }
        buildHeader();
        if (page == Page.RELATIONS) {
            buildGraphPage();
            return;
        }
        int x = 14, w = layout.left();
        if (page == Page.INBOX || page == Page.ARCHIVE) {
            int tabWidth = w / 3;
            for (var value : KnowledgeKey.Kind.values()) {
                button(x + value.ordinal() * tabWidth, layout.header() + 6, tabWidth - 1, 17, t("kind." + value.name().toLowerCase(Locale.ROOT)), true, () -> {
                    rowsDirty = true;
                    kind = value;
                    focused = null;
                    subtype = "";
                    filterOpen = false;
                    list.top();
                    mode = Mode.READ;
                    rebuild();
                }).selected(kind == value);
            }
            search.setPosAndSize(x, layout.header() + 27, w - 23, 16);
            search.ghostText = t("search").getString();
            add(search);
            button(x + w - 20, layout.header() + 27, 20, 16, t("filter.short"), true, () -> {
                filterOpen = !filterOpen;
                rebuild();
            }).tooltip(t("filter.title"));
            if (filterOpen) buildFilters();
        } else {
            search.setPosAndSize(x, layout.header() + 8, w, 17);
            add(search);
            if (page == Page.RELATIONS)
                button(x, layout.header() + 28, w, 16, t(history ? "history.on" : "history.off"), true, () -> {
                    history = !history;
                    rebuildRows();
                }).selected(history);
        }
        list.setPosAndSize(x, layout.listTop() + (filterOpen ? 38 : 0), w, Math.max(25, layout.bottom() - layout.listTop() - (filterOpen ? 38 : 0)));
        list.selection(page == Page.INBOX && organize, value -> value instanceof KnowledgeKey key && selected.contains(key));
        list.setEnabled(mode != Mode.DELETE);
        add(list);
        buildReading();
        if (page == Page.ARCHIVE && !selected.isEmpty()) buildTray();
        if (rowsDirty) rebuildRows();
        else updateListFocus();
    }

    private void buildHeader() {
        int tabX = getWidth() < 420 ? 14 : 105, tabY = getWidth() < 420 ? 30 : 12;
        for (Page value : Page.values())
            button(tabX + value.ordinal() * 43, tabY, 42, 18, t("page." + value.name().toLowerCase(Locale.ROOT)), true, () -> switchPage(value)).selected(page == value);
        button(getWidth() - 122, 12, 57, 18, t("tools"), true, this::openTools);
        button(getWidth() - 63, 12, 48, 18, t("back"), true, back);
    }

    private void buildFilters() {
        List<String> statuses = page == Page.INBOX ? List.of("", "LEARNABLE", "NO_USE", "NOT_UNDERSTOOD", "NO_NEW_DIFFERENCE", "DEFINITION_UNAVAILABLE") : List.of("", "ACTIVE", "DORMANT");
        button(14, layout.header() + 47, layout.left(), 16, statusFilter.isEmpty() ? t("filter.all") : statusFilter.equals("DORMANT") ? t("filter.dormant") : KnowledgePresentation.status(statusFilter), true, () -> {
            rowsDirty = true;
            statusFilter = statuses.get((statuses.indexOf(statusFilter) + 1) % statuses.size());
            list.top();
            rebuild();
        });
        button(14, layout.header() + 64, layout.left(), 16, subtype.isEmpty() ? t("subtype.all") : KnowledgePresentation.subtype(subtype), true, () -> {
            List<String> types = new ArrayList<>(List.of(""));
            source().stream().filter(e -> e.key().kind() == kind).map(KnowledgeJournalData.Entry::subtype).filter(s -> !s.isBlank()).distinct().sorted().forEach(types::add);
            rowsDirty = true;
            subtype = types.get((types.indexOf(subtype) + 1) % types.size());
            list.top();
            rebuild();
        });
    }

    private JournalButton button(int x, int y, int width, int height, Component label, boolean quiet, Runnable action) {
        JournalButton b = new JournalButton(this, label, quiet, action);
        b.setPosAndSize(x, y, Math.max(1, width), height);
        add(b);
        return b;
    }

    private void buildReading() {
        int x = layout.rightX(), w = layout.rightWidth(), top = layout.header() + 12, bottom = layout.bottom();
        var entry = data.entry(focused);
        reading.setPosAndSize(x, top + 35, w, Math.max(25, bottom - top - 69));
        add(reading);
        reading.setVisible(true);
        reading.color(INK);
        if (mode == Mode.DELETE) {
            reading.text(List.of(t("delete.question", selected.size()), t("delete.explain")));
            button(x, bottom - 21, w / 2 - 3, 19, t("delete.confirm"), false, () -> {
                sendSelected(Action.DELETE);
                organize = false;
                mode = Mode.READ;
                rebuild();
            });
            button(x + w / 2 + 2, bottom - 21, w / 2 - 2, 19, t("cancel"), true, () -> {
                mode = Mode.READ;
                rebuild();
            });
            return;
        }
        if (mode == Mode.CANDIDATES) {
            reading.text(candidates.isEmpty() ? List.of(t("link.empty")) : List.of(KnowledgePresentation.text(candidates.get(candidateIndex).getString("title")), KnowledgePresentation.text(candidates.get(candidateIndex).getString("body"))));
            if (candidates.size() > 1) {
                button(x, top + 14, 22, 16, Component.literal("‹"), true, () -> {
                    candidateIndex = Math.floorMod(candidateIndex - 1, candidates.size());
                    rebuild();
                });
                button(x + 26, top + 14, 22, 16, Component.literal("›"), true, () -> {
                    candidateIndex = (candidateIndex + 1) % candidates.size();
                    rebuild();
                });
                button(x + 52, top + 14, 44, 16, Component.literal((candidateIndex + 1) + " / " + candidates.size()), true, () -> {
                }).setEnabled(false);
            }
            button(x, bottom - 21, w - 48, 19, t("link.accept"), false, () -> send(Action.LINK, List.copyOf(selected), candidates.get(candidateIndex).getString("rule"), "", "")).setEnabled(!candidates.isEmpty());
            button(x + w - 44, bottom - 21, 44, 19, t("cancel"), true, () -> {
                mode = Mode.READ;
                rebuild();
            });
            return;
        }
        if (mode == Mode.THINK && entry != null) {
            reading.text(List.of(t("think.text")));
            button(x, top + 66, w, 20, t("think.sleep"), false, () -> {
                send(Action.DREAM_TOPIC, List.of(focused), "", "", "");
                mode = Mode.READ;
                rebuild();
            });
            button(x, top + 91, w, 20, partner.isBlank() ? t("think.partner") : Component.literal(partner), true, this::cyclePartner);
            button(x, bottom - 21, w - 48, 19, t("think.discuss"), false, () -> send(Action.DISCUSS, List.of(focused), partner, "", "")).setEnabled(!partner.isBlank());
            button(x + w - 44, bottom - 21, 44, 19, t("cancel"), true, () -> {
                mode = Mode.READ;
                rebuild();
            });
            return;
        }
        if (page == Page.HINTS) {
            reading.text(hintIndex >= 0 && hintIndex < data.hints.size() ? List.of(data.hints.get(hintIndex), t("hint.reflect")) : List.of(t("empty.hints")));
            return;
        }
        reading.setVisible(true);
        List<Component> paragraphs = new ArrayList<>();
        if (entry == null) paragraphs.add(t(page == Page.INBOX ? "empty.inbox" : "empty.archive"));
        else {
            if (!entry.body().getString().isBlank()) paragraphs.add(entry.body());
            paragraphs.addAll(entry.summary());
            if (details) {
                paragraphs.addAll(entry.details());
                paragraphs.add(KnowledgePresentation.source(entry.source()));
            }
            if (page == Page.INBOX && !entry.status().equals("LEARNABLE") || page == Page.ARCHIVE && !entry.active())
                paragraphs.add(KnowledgePresentation.status(entry.status()));
        }
        reading.text(paragraphs);
        if (entry != null) button(x, top + 18, w, 14, t(details ? "details.hide" : "details.show"), true, () -> {
            details = !details;
            rebuild();
        });
        if (page == Page.INBOX && organize) {
            learn = button(x, bottom - 45, w / 2 - 3, 19, t("learn.selection", selected.size()), false, () -> sendSelected(Action.LEARN));
            remove = button(x + w / 2 + 2, bottom - 45, w / 2 - 2, 19, t("remove.selection"), true, () -> {
                mode = Mode.DELETE;
                rebuild();
            });
            button(x, bottom - 22, w / 2 - 3, 17, t("select.all"), true, () -> {
                filtered.forEach(e -> selected.add(e.key()));
                rebuild();
            });
            button(x + w / 2 + 2, bottom - 22, w / 2 - 2, 17, t("done"), true, () -> {
                organize = false;
                selected.clear();
                rebuild();
            });
            reading.setHeight(Math.max(20, reading.getHeight() - 23));
        } else {
            if (page == Page.INBOX) {
                learn = button(x, bottom - 23, w / 2 - 3, 20, t("learn"), false, () -> {
                    selected.clear();
                    selected.add(focused);
                    sendSelected(Action.LEARN);
                });
                button(x + w / 2 + 2, bottom - 23, w / 2 - 2, 20, t("organize"), true, () -> {
                    organize = true;
                    selected.clear();
                    rebuild();
                });
            } else {
                combine = button(x, bottom - 23, w / 2 - 3, 20, t(selected.contains(focused) ? "tray.remove" : "tray.add"), false, () -> toggleTray(focused));
                button(x + w / 2 + 2, bottom - 23, w / 2 - 2, 20, t("think"), true, () -> {
                    mode = Mode.THINK;
                    rebuild();
                }).setEnabled(entry != null && entry.active());
            }
            if (entry != null) button(x, bottom - 43, w, 16, t("copy"), true, this::openTools)
                    .tooltip(entry.element().canExportNote() ? t("copy") : t("tools.no_item_notes")).setEnabled(entry.element().canExportNote());
            reading.setHeight(Math.max(20, reading.getHeight() - 21));
        }
        if (learn != null)
            learn.setEnabled(!KnowledgeWorkbenchBatch.running() && (organize ? !selected.isEmpty() : entry != null && entry.status().equals("LEARNABLE")));
        if (remove != null) remove.setEnabled(!selected.isEmpty() && !KnowledgeWorkbenchBatch.running());
        if (combine != null)
            combine.setEnabled(entry != null && entry.active() && (selected.size() < 5 || selected.contains(focused)));
    }

    private void buildTray() {
        int y = getHeight() - 55, x = 14, actionWidth = 69, cardWidth = Math.max(30, (getWidth() - 38 - actionWidth) / 5);
        int index = 0;
        for (KnowledgeKey key : selected) {
            button(x + index * cardWidth, y, cardWidth - 3, 28, data.title(key), false, () -> toggleTray(key)).tooltip(t("tray.remove_record", data.title(key)));
            index++;
        }
        button(getWidth() - actionWidth - 15, y, actionWidth, 28, t("link.try"), false, () -> send(Action.PREVIEW_LINK, List.copyOf(selected), "", "", "")).tooltip(t(selected.size() < 2 ? "link.need_more" : "link.explain")).setEnabled(selected.size() >= 2);
    }

    private void buildGraphPage() {
        int width = getWidth(), top = layout.header() + 8;
        search.setPosAndSize(14, top, Math.max(65, width - 223), 17);
        add(search);
        button(width - 204, top, 94, 17, t(history ? "history.on" : "history.off"), true, () -> {
            history = !history;
            graphDirty = true;
            rebuild();
        }).selected(history);
        button(width - 106, top, 44, 17, t("graph.fit"), true, overview::fit);
        button(width - 58, top, 20, 17, Component.literal("−"), true, () -> overview.zoom(1 / 1.25));
        button(width - 34, top, 20, 17, Component.literal("+"), true, () -> overview.zoom(1.25));
        overview.setPosAndSize(14, layout.header() + 31, width - 28, getHeight() - layout.header() - 54);
        add(overview);
        if (graphDirty) {
            graphModel = new KnowledgeGraphModel(data, history);
            overview.model(graphModel);
            graphDirty = false;
        }
        overview.find(query);
        int sheetWidth = Math.min(326, width - 34), sheetHeight = Math.min(250, getHeight() - 24);
        nodeSheet.setPosAndSize((width - sheetWidth) / 2, (getHeight() - sheetHeight) / 2, sheetWidth, sheetHeight);
        if (nodeSheet.isVisible()) {
            var node = graphModel.byId.get(nodeSheet.nodeId());
            if (node == null) closeGraphSheet();
            else nodeSheet.show(data, node);
        }
        add(nodeSheet);
    }

    private void openGraphNode(KnowledgeGraphModel.Node node) {
        getLayerHolder().focusOn(null);
        focused = node.key();
        if (focused != null) kind = focused.kind();
        nodeSheet.show(data, node);
        overview.setEnabled(false);
    }

    private void openGraphNode(String id) {
        if (graphModel != null && graphModel.byId.containsKey(id)) openGraphNode(graphModel.byId.get(id));
    }

    private void closeGraphSheet() {
        nodeSheet.setVisible(false);
        nodeSheet.setEnabled(false);
        overview.setEnabled(true);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (nodeSheet.isVisible()) {
            if (nodeSheet.isMouseOver()) nodeSheet.onMousePressed(button);
            else closeGraphSheet();
            return true;
        }
        return super.onMousePressed(button);
    }

    @Override
    public boolean onMouseScrolled(double delta) {
        if (nodeSheet.isVisible()) {
            nodeSheet.onMouseScrolled(delta);
            return true;
        }
        return super.onMouseScrolled(delta);
    }

    private void buildTools() {
        int x = 149, w = getWidth() - 165;
        button(getWidth() - 62, 13, 47, 18, t("return"), true, () -> {
            getLayerHolder().focusOn(null);
            mode = Mode.READ;
            surfaceChanged.run();
            rebuild();
        });
        reading.setVisible(true);
        reading.setPosAndSize(x, 40, w, 34);
        reading.text(List.of(toolFeedback.getString().isBlank() ? t("tools.read_help") : toolFeedback));
        reading.color(toolFeedback.getString().isBlank() ? INK : ACCENT);
        add(reading);
        retrieve = button(x, 77, w, 20, t("tools.read"), false, () -> send(Action.RETRIEVE, List.of(), "", "", "")).tooltip(t("tools.read_help"));
        var entry = data.entry(focused);
        toolSelection.setPosAndSize(47, 38, 76, 37);
        toolSelection.text(List.of(entry == null ? t("tools.no_open_note") : entry.title()));
        add(toolSelection);
        updateRetrieveButton();
        noteTitleBox.ghostText = t("note.title").getString();
        noteTitleBox.setText(draftTitle, false);
        noteTitleBox.setPosAndSize(x, 117, w, 18);
        add(noteTitleBox);
        noteRemarksBox.ghostText = t("note.remarks").getString();
        noteRemarksBox.setText(draftRemarks, false);
        noteRemarksBox.setPosAndSize(x, 141, w, 18);
        add(noteRemarksBox);
        button(x, 167, w, 21, t("tools.write"), false, () -> send(Action.EXPORT, List.of(focused), "", draftTitle, draftRemarks)).setEnabled(entry != null && entry.element().canExportNote());
    }

    private void cyclePartner() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        List<String> partners = mc.level.players().stream().filter(p -> p != mc.player && p.distanceToSqr(mc.player) <= 64).map(p -> p.getGameProfile().getName()).sorted().toList();
        partner = partners.isEmpty() ? "" : partners.get((partners.indexOf(partner) + 1) % partners.size());
        rebuild();
    }

    private List<KnowledgeJournalData.Entry> source() {
        return page == Page.INBOX ? data.inbox : data.archive;
    }

    private void rebuildRows() {
        if (mode == Mode.TOOLS) return;
        List<JournalList.Row> rows = new ArrayList<>();
        String needle = query.toLowerCase(Locale.ROOT);
        if (page == Page.INBOX || page == Page.ARCHIVE) {
            filtered = source().stream().filter(e -> e.key().kind() == kind).filter(e -> subtype.isEmpty() || subtype.equals(e.subtype()))
                    .filter(e -> statusFilter.isEmpty() || statusFilter.equals("DORMANT") ? statusFilter.isEmpty() || !e.active() : statusFilter.equals(e.status()))
                    .filter(e -> e.search().contains(needle)).toList();
            Map<String, List<KnowledgeJournalData.Entry>> groups = new LinkedHashMap<>();
            for (var entry : filtered)
                groups.computeIfAbsent(entry.key().kind() == KnowledgeKey.Kind.OBSERVATION ? entry.group() : entry.key().toString(), k -> new ArrayList<>()).add(entry);
            for (var group : groups.entrySet()) {
                var values = group.getValue();
                var first = values.get(0);
                if (values.size() > 1)
                    rows.add(new JournalList.Row(group.getKey(), first.title(), t(expanded.contains(group.getKey()) ? "group.close" : "group.open", values.size()), first.icon(), true));
                if (values.size() == 1 || expanded.contains(group.getKey())) for (var entry : values)
                    rows.add(new JournalList.Row(entry.key(), entry.title(), page == Page.INBOX ? KnowledgePresentation.status(entry.status()) : entry.summary().isEmpty() ? KnowledgePresentation.subtype(entry.subtype()) : entry.summary().get(0), entry.icon(), false, values.size() > 1 ? 1 : 0));
            }
        } else {
            for (int i = 0; i < data.hints.size(); i++)
                if (data.hints.get(i).getString().toLowerCase(Locale.ROOT).contains(needle))
                    rows.add(new JournalList.Row(i, data.hints.get(i), t("hint.saved"), new ItemStack(Items.FEATHER), false));
        }
        list.rows(rows);
        rowsDirty = false;
        updateListFocus();
    }

    private void updateListFocus() {
        list.focus(page == Page.HINTS ? hintIndex : focused);
    }

    private void rowClicked(JournalList.Row row, boolean checkbox) {
        if (row.group()) {
            String group = (String) row.value();
            if (!expanded.add(group)) expanded.remove(group);
            rebuildRows();
            return;
        }
        if (row.value() instanceof KnowledgeKey key) {
            if (checkbox) {
                if (!selected.add(key)) selected.remove(key);
            } else {
                focused = key;
                details = false;
                mode = Mode.READ;
            }
        } else if (row.value() instanceof Integer i) hintIndex = i;
        rebuild();
    }

    private void switchPage(Page next) {
        closeGraphSheet();
        notice = Component.empty();
        rowsDirty = true;
        page = next;
        mode = Mode.READ;
        organize = false;
        selected.clear();
        focused = null;
        hintIndex = -1;
        statusFilter = "";
        subtype = "";
        filterOpen = false;
        list.top();
        rebuild();
    }

    private void toggleTray(KnowledgeKey key) {
        if (key == null) return;
        if (!selected.remove(key) && selected.size() < 5) selected.add(key);
        mode = Mode.READ;
        rebuild();
    }

    private void openTools() {
        mode = Mode.TOOLS;
        surfaceChanged.run();
        rebuild();
    }

    public boolean goBack() {
        if (nodeSheet.isVisible()) {
            closeGraphSheet();
            return true;
        }
        if (mode != Mode.READ) {
            getLayerHolder().focusOn(null);
            mode = Mode.READ;
            surfaceChanged.run();
            rebuild();
            return true;
        }
        if (organize) {
            organize = false;
            selected.clear();
            rebuild();
            return true;
        }
        if (!selected.isEmpty()) {
            selected.clear();
            rebuild();
            return true;
        }
        return false;
    }

    private void sendSelected(Action action) {
        if (!selected.isEmpty())
            KnowledgeWorkbenchBatch.start(menu.getBlock().getBlockPos(), action, List.copyOf(selected));
    }

    private void send(Action action, List<KnowledgeKey> keys, String rule, String title, String remarks) {
        var packet = new KnowledgeWorkbenchActionPacket(menu.getBlock().getBlockPos(), action, keys, rule, title, remarks);
        if (action == Action.RETRIEVE) {
            retrievalRequest = packet.requestId();
            retrievePending = true;
            inspectedStack = menu.getBlock().getInventory().getStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT).copy();
            updateRetrieveButton();
        }
        FRNetwork.INSTANCE.sendToServer(packet);
    }

    private void updateRetrieveButton() {
        if (retrieve == null || !toolsOpen() || menu == null) return;
        ItemStack input = menu.getBlock().getInventory().getStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT);
        boolean note = com.teammoeg.frostedresearch.knowledge.item.ResearchNotes.isNote(input);
        boolean blank = note && com.teammoeg.frostedresearch.knowledge.item.ResearchNotes.isBlank(input);
        boolean present = note && inspectedKey != null ? data.entry(inspectedKey) != null : inspectedPresent;
        boolean same = !inspectedStack.isEmpty() && ItemStack.isSameItemSameTags(input, inspectedStack);
        retrieve.label(t(retrievePending ? "tools.reading" : same && present ? "tools.recorded" : note ? "tools.read_note" : "tools.read"));
        retrieve.tooltip(t(blank ? "tools.note_not_observable" : "tools.read_help"));
        retrieve.setEnabled(!retrievePending && !input.isEmpty() && !blank && !(same && present));
    }

    private void refreshInspectedPresence() {
        inspectedPresent = inspectedKey != null && data.inbox.stream().anyMatch(e -> e.key().equals(inspectedKey));
    }

    public void batchProgress() {
        notice = t("batch.progress", KnowledgeWorkbenchBatch.completed(), KnowledgeWorkbenchBatch.total());
        noticeTicks = 100;
    }

    public void receiveReply(CompoundTag payload) {
        boolean retrieval = payload.getString("action").equals("RETRIEVE");
        if (retrieval && payload.hasUUID("request") && payload.getUUID("request").equals(retrievalRequest)) {
            retrievePending = false;
            inspectedKey = KnowledgeJournalData.key(payload.get("retrieved_key"));
            refreshInspectedPresence();
            if (inspectedKey != null && data.entry(inspectedKey) != null) {
                focused = inspectedKey;
                kind = inspectedKey.kind();
                rowsDirty = true;
                page = data.inbox.stream().anyMatch(e -> e.key().equals(inspectedKey)) ? Page.INBOX : Page.ARCHIVE;
            }
        }
        List<Component> feedback = new ArrayList<>();
        ListTag results = payload.getList("results", Tag.TAG_COMPOUND);
        if (results.size() > 1) {
            int learned = 0;
            for (Tag raw : results) if (((CompoundTag) raw).getString("status").equals("SUCCESS")) learned++;
            feedback.add(t("batch.complete", learned, results.size() - learned));
        } else
            for (Tag raw : results) feedback.add(KnowledgePresentation.status(((CompoundTag) raw).getString("status")));
        if (payload.contains("message")) {
            Component message = payload.getString("message").equals("retrieve.unavailable") && !feedback.isEmpty() ? feedback.get(0) : message(payload.getString("message"));
            if (retrieval) {
                feedback.add(0, message);
                toolFeedback = message;
            } else feedback.add(message);
        }
        if (payload.contains("candidates")) {
            candidates.clear();
            for (Tag raw : payload.getList("candidates", Tag.TAG_COMPOUND)) candidates.add((CompoundTag) raw);
            candidateIndex = 0;
            mode = Mode.CANDIDATES;
            if (candidates.isEmpty()) feedback.add(t("link.empty"));
        }
        if (payload.getBoolean("linked")) {
            mode = Mode.READ;
            selected.clear();
            feedback.add(t("link.discovered"));
        }
        if (payload.getBoolean("exported")) {
            draftTitle = "";
            draftRemarks = "";
        }
        notices = List.copyOf(feedback);
        notice = feedback.isEmpty() ? Component.empty() : feedback.get(0);
        noticeTicks = 100;
        rebuild();
    }

    private static Component message(String token) {
        return t(switch (token) {
            case "note.not_observable" -> "tools.note_not_observable";
            case "retrieve.received" -> "tools.received";
            case "retrieve.already_recorded" -> "tools.recorded_message";
            case "note.unreadable" -> "tools.unreadable";
            case "note.exported" -> "tools.written";
            case "retrieve.empty" -> "tools.empty";
            case "note.output_full" -> "tools.output_full";
            case "note.blank_required" -> "tools.blank_required";
            case "note.not_exportable" -> "tools.no_item_notes";
            case "discussion.invited" -> "think.invited";
            case "discussion.unavailable" -> "think.unavailable";
            case "desk.unavailable" -> "tools.closed";
            default -> "status.unchanged";
        });
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void tick() {
        if (noticeTicks > 0 && --noticeTicks == 0) notice = Component.empty();
        if (language != Language.getInstance()) refreshSnapshot();
        if (toolsOpen()) updateRetrieveButton();
        int count = KnowledgeWorkbenchBatch.running() ? KnowledgeWorkbenchBatch.completed() : -1;
        if (count != lastBatchCount) {
            lastBatchCount = count;
            if (count >= 0) batchProgress();
            if (learn != null)
                learn.setEnabled(count < 0 && (organize ? !selected.isEmpty() : data.entry(focused) != null && data.entry(focused).status().equals("LEARNABLE")));
            if (remove != null) remove.setEnabled(count < 0 && !selected.isEmpty());
        }
        super.tick();
    }

    @Override
    public void drawBackground(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
        drawPaper(g, x, y, w, h);
        if (mode == Mode.TOOLS) {
            DESK_INVENTORY.draw(g, x, y, 140, 203);
            g.drawString(getFont(), t("tools.current_note"), x + 21, y + 20, MUTED, false);
            var opened = data.entry(focused);
            if (opened != null) g.renderItem(opened.icon(), x + 24, y + 39);
            else TechIcons.Question.draw(g, x + 24, y + 39, 16, 16);
            g.drawString(getFont(), t("tools.title"), x + 149, y + 18, INK, false);
            Component copying = data.entry(focused) == null ? t("tools.choose_note") : t("tools.copying", data.entry(focused).title());
            g.drawString(getFont(), Language.getInstance().getVisualOrder(getFont().substrByWidth(copying, w - 166)), x + 149, y + 103, MUTED, false);
            return;
        }
        g.drawString(getFont(), t("title"), x + 18, y + 17, INK, false);
        rule(g, x + 14, y + layout.header() + 1, w - 28);
        if (page == Page.RELATIONS) return;
        g.fill(x + layout.rightX() - 9, y + layout.header() + 8, x + layout.rightX() - 8, y + layout.bottom(), PAPER_DARK);
        Component title = mode == Mode.CANDIDATES ? t("link.possibilities") : mode == Mode.DELETE ? t("delete.title") : mode == Mode.THINK ? t("think")
                : page == Page.HINTS ? t("page.hints") : data.entry(focused) == null ? t("reading.title") : data.entry(focused).title();
        g.drawString(getFont(), Language.getInstance().getVisualOrder(getFont().substrByWidth(title, layout.rightWidth())), x + layout.rightX(), y + layout.header() + 12, INK, false);
    }

    @Override
    public void afterDrawElements(GuiGraphics g, int px, int py, int cx, int cy, int w, int h) {
        if (!notice.getString().isBlank())
            g.drawString(getFont(), Language.getInstance().getVisualOrder(getFont().substrByWidth(notice, w - 34)), px + 17, py + h - 15, ACCENT, false);
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (nodeSheet.isVisible()) {
            nodeSheet.getTooltip(tooltip);
            return;
        }
        if (getMouseY() > getHeight() - 22 && !notices.isEmpty()) notices.forEach(tooltip::accept);
        else super.getTooltip(tooltip);
    }
}
