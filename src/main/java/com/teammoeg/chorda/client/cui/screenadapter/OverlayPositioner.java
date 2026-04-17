package com.teammoeg.chorda.client.cui.screenadapter;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.math.Point;

public final class OverlayPositioner {

    // 默认边距常量
    private static final int DEFAULT_MARGIN_LEFT = 8;
    private static final int DEFAULT_MARGIN_TOP = 12;
    private static final int DEFAULT_MARGIN_RIGHT = 8;
    private static final int DEFAULT_MARGIN_BOTTOM = 6;
    private static final int EXTRA_PADDING_X = 12;  // 用于边界避让时的横向偏移
    private static final int EXTRA_PADDING_Y = 3;   // 用于边界避让时的纵向偏移

    // 垂直居中时的向上偏移比例 (2.5%)
    private static final float VERTICAL_CENTER_OFFSET_FACTOR = 0.025F;

    // ========== 核心位置调整方法 ==========

    /**
     * 根据屏幕边界修正位置，避免 UI 元素超出可视区域。
     *
     * @param element 需要显示的 UI 元素
     * @param desired  期望的左上角坐标
     * @return 修正后的安全坐标
     */
    public static Point position(UIElement element, Point desired) {
        int screenWidth = ClientUtils.screenWidth();
        int screenHeight = ClientUtils.screenHeight();

        int width = element.getWidth();
        int height = element.getHeight();
        int x = desired.getX();
        int y = desired.getY();

        // 右边界修正
        if (x + width > screenWidth) {
            x = Math.max(x - EXTRA_PADDING_X - width, DEFAULT_MARGIN_LEFT);
        }

        // 下边界修正
        int totalHeight = height + EXTRA_PADDING_Y;
        if (y + totalHeight > screenHeight) {
            y = screenHeight - totalHeight - DEFAULT_MARGIN_BOTTOM;
        }

        return new Point(x, y);
    }

    // ========== 统一锚点定义 ==========

    /**
     * 标准 9 方位锚点，提供基于屏幕边界的坐标计算。
     */
    public enum Anchor {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_CENTER,
        MIDDLE_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT;

        /**
         * 计算给定元素在此锚点下的起始坐标。
         */
        public Point getStartPos(UIElement element) {
            ScreenContext ctx = ScreenContext.current();
            int width = element.getWidth();
            int height = element.getHeight();

            int x = switch (this) {
                case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> DEFAULT_MARGIN_LEFT;
                case TOP_CENTER, MIDDLE_CENTER, BOTTOM_CENTER -> ctx.centerX - width / 2;
                case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> ctx.screenWidth - width - DEFAULT_MARGIN_RIGHT;
            };

            int y = switch (this) {
                case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> DEFAULT_MARGIN_TOP;
                case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT -> ctx.centerYOffset - height / 2;
                case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> ctx.screenHeight - height - DEFAULT_MARGIN_BOTTOM;
            };

            return new Point(x, y);
        }
    }

    // ========== 屏幕上下文（缓存计算值） ==========

    private record ScreenContext(int screenWidth, int screenHeight, int centerX, int centerYOffset) {
        static ScreenContext current() {
            int sw = ClientUtils.screenWidth();
            int sh = ClientUtils.screenHeight();
            int centerX = sw / 2;
            int centerYOffset = sh / 2 - (int) (sh * VERTICAL_CENTER_OFFSET_FACTOR);
            return new ScreenContext(sw, sh, centerX, centerYOffset);
        }
    }

    // ========== 不同场景的预设枚举（仅保留选择逻辑） ==========

    /** 全部 9 个位置 */
    public enum All {
        TOP_LEFT, TOP_MIDDLE, TOP_RIGHT,
        MIDDLE_LEFT, MIDDLE, MIDDLE_RIGHT,
        BOTTOM_LEFT, BOTTOM_MIDDLE, BOTTOM_RIGHT;

        public Point startPos(UIElement ele) {
            return toAnchor().getStartPos(ele);
        }

        private Anchor toAnchor() {
            return switch (this) {
                case TOP_LEFT -> Anchor.TOP_LEFT;
                case TOP_MIDDLE -> Anchor.TOP_CENTER;
                case TOP_RIGHT -> Anchor.TOP_RIGHT;
                case MIDDLE_LEFT -> Anchor.MIDDLE_LEFT;
                case MIDDLE -> Anchor.MIDDLE_CENTER;
                case MIDDLE_RIGHT -> Anchor.MIDDLE_RIGHT;
                case BOTTOM_LEFT -> Anchor.BOTTOM_LEFT;
                case BOTTOM_MIDDLE -> Anchor.BOTTOM_CENTER;
                case BOTTOM_RIGHT -> Anchor.BOTTOM_RIGHT;
            };
        }
    }

    /** 边框位置（缺少 TOP_MIDDLE） */
    public enum Border {
        TOP_LEFT, TOP_RIGHT,
        MIDDLE_LEFT, MIDDLE, MIDDLE_RIGHT,
        BOTTOM_LEFT, BOTTOM_MIDDLE, BOTTOM_RIGHT;

        public Point startPos(UIElement ele) {
            return toAnchor().getStartPos(ele);
        }

        private Anchor toAnchor() {
            return switch (this) {
                case TOP_LEFT -> Anchor.TOP_LEFT;
                case TOP_RIGHT -> Anchor.TOP_RIGHT;
                case MIDDLE_LEFT -> Anchor.MIDDLE_LEFT;
                case MIDDLE -> Anchor.MIDDLE_CENTER;
                case MIDDLE_RIGHT -> Anchor.MIDDLE_RIGHT;
                case BOTTOM_LEFT -> Anchor.BOTTOM_LEFT;
                case BOTTOM_MIDDLE -> Anchor.BOTTOM_CENTER;
                case BOTTOM_RIGHT -> Anchor.BOTTOM_RIGHT;
            };
        }
    }

    /** 四个角落 */
    public enum Corner {
        MIDDLE_LEFT, MIDDLE_RIGHT,
        BOTTOM_LEFT, BOTTOM_RIGHT;

        public Point startPos(UIElement ele) {
            return toAnchor().getStartPos(ele);
        }

        private Anchor toAnchor() {
            return switch (this) {
                case MIDDLE_LEFT -> Anchor.MIDDLE_LEFT;
                case MIDDLE_RIGHT -> Anchor.MIDDLE_RIGHT;
                case BOTTOM_LEFT -> Anchor.BOTTOM_LEFT;
                case BOTTOM_RIGHT -> Anchor.BOTTOM_RIGHT;
            };
        }
    }

    /** 上下左右四个方向 */
    public enum TopBottomLeftRight {
        TOP, BOTTOM, LEFT, RIGHT;

        public Point startPos(UIElement ele) {
            return toAnchor().getStartPos(ele);
        }

        private Anchor toAnchor() {
            return switch (this) {
                case TOP -> Anchor.TOP_CENTER;
                case BOTTOM -> Anchor.BOTTOM_CENTER;
                case LEFT -> Anchor.MIDDLE_LEFT;
                case RIGHT -> Anchor.MIDDLE_RIGHT;
            };
        }
    }

    /** 仅左或右 */
    public enum LeftAndRight {
        LEFT, RIGHT;

        public Point startPos(UIElement ele) {
            return toAnchor().getStartPos(ele);
        }

        private Anchor toAnchor() {
            return this == LEFT ? Anchor.MIDDLE_LEFT : Anchor.MIDDLE_RIGHT;
        }
    }

    /** 仅上或下 */
    public enum TopAndBottom {
        TOP, BOTTOM;

        public Point startPos(UIElement ele) {
            return toAnchor().getStartPos(ele);
        }

        private Anchor toAnchor() {
            return this == TOP ? Anchor.TOP_CENTER : Anchor.BOTTOM_CENTER;
        }
    }
}