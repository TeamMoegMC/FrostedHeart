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

package com.teammoeg.frostedheart.content.tips;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.teammoeg.frostedheart.content.tips.client.gui.TipOverlay;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import static com.teammoeg.frostedheart.content.tips.Tip.LOGGER;

/**
 * 管理 tip 的展示和状态
 * <p>
 * 注意：仅客户端
 */
@OnlyIn(Dist.CLIENT)
public class TipManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    public static final File CONFIG_PATH = new File(FMLPaths.CONFIGDIR.get().toFile(), "fhtips");
    public static final File TIP_PATH = new File(CONFIG_PATH, "tips");
    public static final File TIP_STATE_FILE = new File(CONFIG_PATH, "tip_states.json");

    public static final TipManager INSTANCE = new TipManager();
    private final Map<String, Tip> loadedTips = new HashMap<>();
    private final DisplayManager display;
    private final StateManager state;

    private TipManager() {
        this.display = new DisplayManager();
        this.state = new StateManager();
        loadFromFile();
    }

    /**
     * 管理 tip 的展示
     */
    public static DisplayManager display() {
        return INSTANCE.display;
    }

    /**
     * 管理 tip 的状态
     * <p>
     * 注意： tip 状态仅在客户端储存
     */
    public static StateManager state() {
        return INSTANCE.state;
    }

    /**
     * 获取对应的 tip 实例
     */
    public Tip getTip(String id) {
        Tip tip = loadedTips.get(id);
        if (tip != null) {
            return tip;
        }
        return TipHelper.Error.OTHER.create("tips.frostedheart.error.load.tip_not_exists", "ID: " + id).build();
    }

    /**
     * 对应的 tip 是否存在
     */
    public boolean hasTip(String id) {
        return id != null && loadedTips.containsKey(id);
    }

    public Set<String> getAllIds() {
        return loadedTips.keySet();
    }

    public Collection<Tip> getAllTips() {
        return loadedTips.values();
    }

    public List<Tip> getSortedTips() {
        return loadedTips.values().stream().sorted(Comparator.comparing(Tip::id)).toList();
    }

    public List<Tip> getUnlockedTips() {
        return state.getUnlockedTips();
    }

    /**
     * 加载所有 tip 文件
     */
    public void loadFromFile() {
        TIP_PATH.mkdirs();
        loadedTips.clear();

        // 加载所有 tip 文件
        List<File> files = new ArrayList<>();
        File[] general = TIP_PATH.listFiles();
        if (general != null) files.addAll(List.of(general));
        if (!files.isEmpty()) {
            int sum = 0;
            for (File tipFile : files) {
                Tip tip = TipHelper.load(tipFile);
                if (loadedTips.containsKey(tip.id())) {
                    // 重复的 id
                    LOGGER.warn("Duplicated tip '{}'", tipFile);
                } else {
                    loadedTips.put(tip.id(), tip);
                    sum++;
                }
            }
            LOGGER.info("Loaded {} tip(s)", sum);
        }
        state.loadFromFile();
    }

    public class DisplayManager {
        private DisplayManager() {}

        /**
         * 添加对应 id 的 tip 到渲染队列中
         */
        public void general(String id) {
            general(getTip(id));
        }

        /**
         * 添加此 tip 到渲染队列中
         */
        public void general(Tip tip) {
            if (tip == null) return;
            if (tip.display().onceOnly() && state.isUnlocked(tip)) return;

            // 渲染队列已有此 tip 时返回
            for (Tip queue : TipOverlay.getQUEUE()) {
                if (queue.id().equals(tip.id())) return;
            }

            // 更改非临时 tip 的状态
            if (!tip.temporary()) {
                state.unlock(tip, true);
            }

            state.unlock(tip.unlocks().stream().map(TipManager.this::getTip).toList(), true);

            if (tip.display().pin() && !TipOverlay.getQUEUE().isEmpty()) {
                Tip last = TipOverlay.getQUEUE().get(0);
                TipOverlay.removeCurrent();
                TipOverlay.getQUEUE().add(0, last);
                TipOverlay.getQUEUE().add(0, tip);
            } else {
                TipOverlay.getQUEUE().add(tip);
            }

            // 添加下一个 tip
            if (TipHelper.hasNext(tip)) {
                general(tip.nextTip());
            } else if (!tip.nextTip().isBlank()) {
                LOGGER.warn("Tip '{}' declared next tip '{}', but it doesn't exist", tip.id(), tip.nextTip());
            }
        }

        /**
         * 无视 tip 的状态，在渲染队列中强制添加此 tip
         */
        public void force(String id) {
            force(getTip(id));
        }

        /**
         * 无视 tip 的状态，在渲染队列中强制添加此 tip
         */
        public void force(Tip tip) {
            if (!tip.temporary()) {
                state.unlock(tip, true);
            }

            if (tip.display().pin() && !TipOverlay.getQUEUE().isEmpty()) {
                Tip last = TipOverlay.getQUEUE().get(0);
                TipOverlay.removeCurrent();
                TipOverlay.getQUEUE().add(0, last);
                TipOverlay.getQUEUE().add(0, tip);
            } else {
                TipOverlay.getQUEUE().add(tip);
            }
        }

        /**
         * 使 tip 永久显示，即 {@code alwaysVisible = true}
         */
        public void alwaysVisible(Tip tip) {
            if (TipOverlay.getQUEUE().isEmpty()) return;

            var list = TipOverlay.getQUEUE();
            if (list.size() == 1 || list.get(0) == tip) return;
            for (int i = 0; i < list.size(); i++) {
                Tip t = list.get(i);
                if (t == tip) {
                    Tip clone = t.copy().alwaysVisible(true).build();
                    TipOverlay.getQUEUE().set(i, clone);
                    return;
                }
            }
        }

        /**
         * 置顶 tip
         */
        public void pin(Tip tip) {
            var list = TipOverlay.getQUEUE();
            if (list.size() <= 1 || list.get(0) == tip) return;

            for (Tip t : list) {
                if (t == tip) {
                    TipOverlay.getQUEUE().remove(t);
                    force(tip);
                    return;
                }
            }
        }

        /**
         * 移除当前显示的 tip
         */
        public void removeCurrent() {
            TipOverlay.removeCurrent();
        }

        /**
         * 清除 tip 队列
         */
        public void clearRenderQueue() {
            TipOverlay.getQUEUE().clear();
            TipOverlay.removeCurrent();
        }
    }

    public class StateManager {
        private static final Type STATE_TYPE = new TypeToken<Set<State>>(){}.getType();
        private final Map<Tip, State> tipStates = new HashMap<>();

        private StateManager() {}

        /**
         * 加载 {@code tip_states.json} 文件
         */
        protected void loadFromFile() {
            tipStates.clear();
            // 为所有已加载的 tip 创建空的 TipState
            loadedTips.forEach((id, tip) -> tipStates.put(tip, new State(tip)));

            if (!TIP_STATE_FILE.exists()) {
                try (FileWriter writer = new FileWriter(TIP_STATE_FILE)) {
                    writer.write("[]");
                } catch (IOException e) {
                    String msg = "Unable to create file: '%s'".formatted(TIP_STATE_FILE);
                    LOGGER.error(msg , e);
                    TipHelper.display(TipHelper.Error.SAVE.create().build());
                }
                return;
            }

            try (FileReader reader = new FileReader(TIP_STATE_FILE)) {
                Set<State> stateList = GSON.fromJson(reader, STATE_TYPE);
                if (stateList == null) {
                    // 文件存在但是无法正确读取
                    if (TIP_STATE_FILE.exists()) {
                        String message = "'%s' exists but cannot be read correctly, it may corrupted".formatted(TIP_STATE_FILE);
                        LOGGER.warn(message);
                        TipHelper.Error.OTHER.create(message);
                    }
                    return;
                }
                // 将对应的空 TipState 替换为文件中储存的 TipState
                Map<Tip, State> toAdd = new HashMap<>();
                for (Iterator<State> iterator = stateList.iterator(); iterator.hasNext(); ) {
                    State fromFile = iterator.next();
                    if (!hasTip(fromFile.getId())) {
                        iterator.remove();
                        continue;
                    }
                    Tip tip = getTip(fromFile.getId());
                    toAdd.put(tip, State.copyState(tip, fromFile));
                }
                tipStates.putAll(toAdd);
            } catch (IOException e) {
                String message = "Unable to load file: '%s'".formatted(TIP_STATE_FILE);
                LOGGER.error(message, e);
                TipHelper.Error.OTHER.create(message);
            }
        }

        /**
         * 保存 {@code tip_states.json} 文件
         */
        public void saveToFile() {
            try (FileWriter writer = new FileWriter(TIP_STATE_FILE)) {
                String json = GSON.toJson(tipStates.values().stream().filter(s -> s.unlocked || s.viewed).toList());
                writer.write(json);
            } catch (IOException e) {
                String msg = "Unable to save file: '%s'".formatted(TIP_STATE_FILE);
                LOGGER.error(msg, e);
                TipHelper.Error.OTHER.create(msg);
            }
        }

        public List<Tip> getUnlockedTips() {
            return tipStates.values().stream().filter(State::isUnlocked).map(State::getTip).toList();
        }

        public List<Tip> getChildren(Tip tip) {
            return tip.children().stream().filter(s -> isUnlocked(s) && hasTip(s)).map(TipManager.this::getTip).toList();
        }

        public void view(Tip tip) {
            view(tip, true);
        }

        /**
         * 设置 tip 的查看状态
         */
        public void view(Tip tip, boolean view) {
            getState(tip).ifPresent(state -> {
                if (state.viewed != view) {
                    state.viewed = view;
                    saveToFile();
                }
            });
        }

        public void view(Collection<Tip> tips, boolean view) {
            tips.forEach(tip -> getState(tip).ifPresent(state -> state.viewed = view));
            saveToFile();
        }

        public void viewAll() {
            tipStates.forEach((tip, state) -> state.setViewed(true));
            saveToFile();
        }

        public void unlock(Tip tip) {
            unlock(tip, true);
        }

        /**
         * 设置 tip 的解锁状态
         */
        public void unlock(Tip tip, boolean unlock) {
            getState(tip).ifPresent(state -> {
                if (state.unlocked != unlock) {
                    state.unlocked = unlock;
                    saveToFile();
                }
            });
        }

        public void unlock(Collection<Tip> tips, boolean unlock) {
            tips.forEach(tip -> getState(tip).ifPresent(state -> state.unlocked = unlock));
            saveToFile();
        }

        public void reset(Tip tip) {
            getState(tip).ifPresent(state -> {
                state.viewed = false;
                state.unlocked = false;
            });
            saveToFile();
        }

        public void unlockAll() {
            tipStates.forEach((tip, state) -> state.setUnlocked(true));
            saveToFile();
        }

        /**
         * tip 是否已解锁
         */
        public boolean isUnlocked(String tip) {
            var state = getState(getTip(tip));
            return state.isPresent() && state.get().unlocked;
        }

        /**
         * tip 是否已在条目列表中被玩家查看
         */
        public boolean isViewed(String tip) {
            var state = getState(getTip(tip));
            return state.isPresent() && state.get().viewed;
        }

        /**
         * tip 是否已解锁
         */
        public boolean isUnlocked(Tip tip) {
            var state = getState(tip);
            return state.isPresent() && state.get().unlocked;
        }

        /**
         * tip 是否已在条目列表中被玩家查看
         */
        public boolean isViewed(Tip tip) {
            var state = getState(tip);
            return state.isPresent() && state.get().viewed;
        }

        /**
         * 获取提示的状态
         */
        private Optional<State> getState(Tip tip) {
            State state = null;
            if (tip != null && loadedTips.containsValue(tip) && !tip.temporary()) {
                if (tipStates.get(tip) != null) {
                    state = tipStates.get(tip);
                } else {
                    state = new State(tip);
                    this.tipStates.put(tip, state);
                    saveToFile();
                }
            }
            return Optional.ofNullable(state);
        }

        /**
         * 重置所有 tip 的状态
         */
        public void resetAll() {
            tipStates.clear();
            saveToFile();
            loadFromFile();
        }

        @Getter
        @Setter
        public static class State {
            private final Tip tip;
            @Expose
            private final String id;
            @Expose
            private boolean unlocked;
            @Expose
            private boolean viewed;

            protected State(Tip tip) {
                this.id = tip.id();
                this.tip = tip;
                this.unlocked = false;
                this.viewed = false;
            }

            protected static State copyState(Tip tip, State original) {
                State newState = new State(tip);
                newState.unlocked = original.unlocked;
                newState.viewed = original.viewed;
                return newState;
            }
        }
    }
}
