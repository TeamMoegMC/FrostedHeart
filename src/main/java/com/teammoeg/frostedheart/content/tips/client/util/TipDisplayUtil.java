package com.teammoeg.frostedheart.content.tips.client.util;

import com.teammoeg.frostedheart.content.tips.client.RenderHUD;
import com.teammoeg.frostedheart.content.tips.client.TipElement;
import com.teammoeg.frostedheart.content.tips.client.UnlockedTipManager;
import com.teammoeg.frostedheart.content.tips.client.gui.DebugScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;

import java.util.HashMap;
import java.util.Map;

public class TipDisplayUtil {
    private static final Map<String, TipElement> CACHE = new HashMap<>();

    /**
     * 读取并添加一个{@code config\fhtips\tips}目录下的提示到渲染列表中
     * @param ID 文件名
     * @param first {@code true}时新添加的提示会被置顶
     */
    public static void displayTip(String ID, boolean first) {
        displayTip(getTipEle(ID), first);
    }

    public static void displayTip(TipElement element, boolean first) {
        if (element.ID.isEmpty()) return;
        if (element.onceOnly && UnlockedTipManager.manager.isUnlocked(element.ID)) return;

        for (TipElement ele : RenderHUD.renderQueue) {
            if (ele.ID.equals(element.ID)) {
                return;
            }
        }

        if (element.history) {
            if (element.ID.startsWith("*custom*")) {
                UnlockedTipManager.manager.unlockCustom(element);
            } else {
                UnlockedTipManager.manager.unlock(element.ID, element.hide);
            }
        }

        if (first) {
            RenderHUD.renderQueue.add(0, element);
        } else {
            RenderHUD.renderQueue.add(element);
        }
    }

    /**
     * 在渲染列表中添加一个自定义提示
     * @param title 标题
     * @param content 内容，使用 "$" 换行
     * @param visibleTime 显示时间，单位为 ms
     * @param history {@code true}保存在已解锁列表中
     */
    public static void displayCustomTip(String title, String content, int visibleTime, boolean history) {
        TipElement ele = new TipElement();
        ele.ID = "*custom*" + title;
        ele.history = history;
        ele.contents.add(new StringTextComponent(title));
        String[] contents = content.split("\\$");
        for (String s : contents) {
            ele.contents.add(new StringTextComponent(s));
        }

        if (visibleTime == -1) {
            ele.alwaysVisible = true;
        } else {
            ele.visibleTime = visibleTime;
        }

        displayTip(ele, false);
    }

    /**
     * 无视属性直接添加到渲染列表
     */
    public static void forceAdd(String ID, boolean first) {
        forceAdd(getTipEle(ID), first);
    }

    public static void forceAdd(TipElement ele, boolean first) {
        for (TipElement q : RenderHUD.renderQueue) {
            if (q.ID.equals(ele.ID)) {
                return;
            }
        }

        if (first) {
            RenderHUD.renderQueue.add(0, ele);
        } else {
            RenderHUD.renderQueue.add(ele);
        }
    }

    public static TipElement getTipEle(String ID) {
        if (CACHE.containsKey(ID)) {
            return CACHE.get(ID);
        } else {
            TipElement newElement = new TipElement(ID);
            if (newElement.history) {
                CACHE.put(ID, newElement);
            }
            return newElement;
        }
    }

    /**
     * 移除当前显示的提示
     */
    public static void removeCurrent() {
        RenderHUD.renderQueue.remove(0);
        resetTipAnimation();
        RenderHUD.currentTip = null;
    }

    /**
     * 固定列表中的某个提示，即 {@code alwaysVisible = true}
     */
    public static void pinTip(String ID) {
        for (int i = 0; i < RenderHUD.renderQueue.size(); i++) {
            TipElement ele = RenderHUD.renderQueue.get(i);
            if (ele.ID.equals(ID)) {
                try {
                    TipElement clone = (TipElement)ele.clone();
                    clone.alwaysVisible = true;
                    RenderHUD.renderQueue.set(i, clone);
                    break;
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    /**
     * 置顶列表中的某个提示
     */
    public static void moveToFirst(String ID) {
        if (RenderHUD.renderQueue.size() <= 1 || RenderHUD.renderQueue.get(0).ID.equals(ID)) {
            return;
        }
        for (int i = 0; i < RenderHUD.renderQueue.size(); i++) {
            TipElement ele = RenderHUD.renderQueue.get(i);
            if (ele.ID.equals(ID)) {
                RenderHUD.renderQueue.remove(i);
                RenderHUD.renderQueue.add(0, ele);
                RenderHUD.currentTip = null;
                resetTipAnimation();
                return;
            }
        }
    }

    public static void resetTipAnimation() {
        AnimationUtil.removeAnimation("TipFadeIn");
        AnimationUtil.removeAnimation("TipFadeOut");
        AnimationUtil.removeAnimation("TipVisibleTime");
    }

    public static void clearRenderQueue() {
        RenderHUD.renderQueue.clear();
        resetTipAnimation();
        RenderHUD.currentTip = null;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static void openDebugScreen() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().setScreen(new DebugScreen());
        }
    }
}
