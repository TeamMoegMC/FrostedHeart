package com.teammoeg.frostedheart.content.tips;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.mojang.logging.LogUtils;
import com.teammoeg.frostedheart.util.lang.Lang;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理 tip 的展示和状态
 * <p>
 * 注意：仅客户端
 */
@OnlyIn(Dist.CLIENT)
public class TipManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final File CONFIG_PATH = new File(FMLPaths.CONFIGDIR.get().toFile(), "fhtips");
    public static final File TIP_PATH = new File(CONFIG_PATH, "tips");
    public static final File TIP_STATE_FILE = new File(CONFIG_PATH, "tip_states.json");
    public static final TipManager INSTANCE = new TipManager();

    private final Map<String, Tip> loadedTips = new HashMap<>();
    private final DisplayManager display;
    private final StateManager state;

    private TipManager() {
        this.display = new DisplayManager(this);
        this.state = new StateManager(this);
        loadFromFile();
    }

    /**
     * 管理 tip 的展示
     */
    public DisplayManager display() {
        return display;
    }

    /**
     * 管理 tip 的状态
     * <p>
     * 注意： tip 状态仅在客户端储存
     */
    public StateManager state() {
        return state;
    }

    /**
     * 获取对应的 tip 实例
     */
    public Tip getTip(String id) {
        Tip tip = loadedTips.get(id);
        if (tip != null) {
            return tip;
        }
        return Tip.builder(id).error(Tip.ErrorType.NOT_EXISTS, Lang.str(TIP_PATH.toString() + "\\" + id + ".json")).build();
    }

    /**
     * 返回对应的 tip 实例是否存在
     */
    public boolean hasTip(String id) {
        return loadedTips.containsKey(id);
    }

    /**
     * 加载所有 tip 文件
     */
    public void loadFromFile() {
        TIP_PATH.mkdirs();
        display.clearRenderQueue();
        loadedTips.clear();

        // 加载所有 tip 文件
        List<File> files = new ArrayList<>();
        File[] general = TIP_PATH.listFiles();
        if (general != null) files.addAll(List.of(general));
        if (!files.isEmpty()) {
            for (File tipFile : files) {
                Tip tip = Tip.fromJsonFile(tipFile);
                loadedTips.put(tip.getId(), tip);
            }
            state.loadFromFile();
        }
        LOGGER.debug("Loaded {} tips", files.size());
    }

    private void displayException(Tip.ErrorType type, Exception e) {
        Tip exception = Tip.builder("exception").error(type, e).build();
        display.force(exception);
    }

    public static class DisplayManager {
        private final TipManager manager;

        private DisplayManager(TipManager manager) {
            this.manager = manager;
        }

        /**
         * 添加对应 id 的 tip 到渲染队列中
         */
        public void general(String id) {
            general(manager.getTip(id));
        }

        /**
         * 添加此 tip 到渲染队列中
         */
        public void general(Tip tip) {
            if (tip == null) return;
            if (tip.isOnceOnly() && manager.state.isUnlocked(tip)) return;

            // 渲染队列已有此 tip 时返回
            for (Tip queue : TipRenderer.renderQueue) {
                if (queue.getId().equals(tip.getId())) return;
            }

            // 更改非临时 tip 的状态
            if (!tip.isTemporary()) {
//                if (tip.id.startsWith("*custom*")) {
//                    TipStateManager.manager.unlockCustom(tip);
//                }
                manager.state.setLockState(tip, true);
            }

            if (tip.isPin()) {
                TipRenderer.removeCurrent();
                TipRenderer.renderQueue.add(0, tip);
            } else {
                TipRenderer.renderQueue.add(tip);
            }
        }

        /**
         * 无视 tip 的状态，在渲染队列中强制添加此 tip
         */
        public void force(String id) {
            force(manager.getTip(id));
        }

        /**
         * 无视 tip 的状态，在渲染队列中强制添加此 tip
         */
        public void force(Tip tip) {
            if (tip.isPin()) {
                TipRenderer.removeCurrent();
                TipRenderer.renderQueue.add(0, tip);
            } else {
                TipRenderer.renderQueue.add(tip);
            }
        }

        /**
         * 使 tip 永久显示，即 {@code alwaysVisible = true}
         */
        public void alwaysVisible(Tip tip) {
            var list = TipRenderer.renderQueue;
            if (list.size() <= 1 || list.get(0) == tip) return;
            for (int i = 0; i < list.size(); i++) {
                Tip t = list.get(i);
                if (t == tip) {
                    Tip clone = Tip.builder("").copy(t).alwaysVisible(true).build();
                    TipRenderer.renderQueue.set(i, clone);
                    return;
                }
            }
        }

        /**
         * 置顶 tip
         */
        public void pin(String id) {
            var list = TipRenderer.renderQueue;
            if (list.size() <= 1 || list.get(0).getId().equals(id)) return;
            Iterator<Tip> iterator = list.iterator();
            while (iterator.hasNext()) {
                Tip tip = iterator.next();
                if (tip.getId().equals(id)) {
                    iterator.remove();
                    list.add(0, tip);
                    return;
                }
            }
        }

        /**
         * 移除当前显示的 tip
         */
        public void removeCurrent() {
            TipRenderer.removeCurrent();
        }

        /**
         * 清除 tip 队列
         */
        public void clearRenderQueue() {
            TipRenderer.renderQueue.clear();
            TipRenderer.removeCurrent();
        }
    }

    public static class StateManager {
        private static final Type STATE_TYPE = new TypeToken<Set<State>>(){}.getType();
        private final TipManager manager;
        private final Map<Tip, State> tipStates = new HashMap<>();

        private StateManager(TipManager manager) {
            this.manager = manager;
        }

        /**
         * 加载 {@code tip_states.json} 文件
         */
        protected void loadFromFile() {
            tipStates.clear();
            // 为所有已加载的tip创建空的TipState
            manager.loadedTips.forEach((id, tip) -> tipStates.put(tip, new State(tip)));
            try (FileReader reader = new FileReader(TIP_STATE_FILE)) {
                Set<State> stateList = GSON.fromJson(reader, STATE_TYPE);
                if (stateList == null) {
                    // 文件存在但是无法正确读取
                    if (TIP_STATE_FILE.exists()) {
                        String message = "The file '" + TIP_STATE_FILE + "' already exists but cannot be read correctly, it may be corrupted";
                        manager.displayException(Tip.ErrorType.OTHER, new Exception(message));
                        LOGGER.warn(message);
                    }
                    return;
                }
                // 将对应的空TipState替换为文件中储存的TipState
                tipStates.putAll(stateList.stream()
                        .collect(Collectors.toMap(state -> manager.getTip(state.id), s -> s)));
            } catch (IOException e) {
                LOGGER.error("Unable to load file: '{}'", TIP_STATE_FILE, e);
                manager.displayException(Tip.ErrorType.LOAD, e);
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
                LOGGER.error("Unable to save file: '{}'", TIP_STATE_FILE, e);
                manager.displayException(Tip.ErrorType.SAVE, e);
            }
        }

        public List<Tip> getAllUnlockedTips() {
            return tipStates.values().stream().filter(s -> s.unlocked).map(State::getTip).toList();
        }

        /**
         * 设置 tip 的查看状态
         */
        public void setViewState(Tip tip, boolean view) {
            getState(tip).ifPresent(state -> {
                state.viewed = view;
                saveToFile();
            });
        }

        /**
         * 设置 tip 的解锁状态
         */
        public void setLockState(Tip tip, boolean unlock) {
            getState(tip).ifPresent(state -> {
                state.unlocked = unlock;
                saveToFile();
            });
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
            if (tip != null && manager.loadedTips.containsValue(tip) && !tip.isTemporary()) {
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
                this.id = tip.getId();
                this.tip = tip;
                this.unlocked = false;
                this.viewed = false;
            }
        }
    }
}
