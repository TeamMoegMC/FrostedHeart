/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.observation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationActionPacket;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationSessions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.teammoeg.frostedresearch.knowledge.client.KnowledgePresentation.t;

/**
 * The world remains live; the Chorda overlay follows only the server-locked target.
 */
public final class ObservationProgressScreen extends CUIScreenWrapper {
    public ObservationProgressScreen(BlockPos position, int entityId) {
        super(new FocusLayer(position, entityId));
    }

    @Override
    public void onClose() {
        ((FocusLayer) getPrimaryLayer()).cancel();
    }

    private static final class FocusLayer extends PrimaryLayer {
        private final BlockPos position;
        private final int entityId;
        private int elapsed;
        private Component keyHint;

        FocusLayer(BlockPos position, int entityId) {
            this.position = position;
            this.entityId = entityId;
            setRenderGradient(false);
        }

        @Override
        public boolean onInit() {
            var window = Minecraft.getInstance().getWindow();
            setSize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            keyHint = t("observation.hold", ObservationClient.OBSERVE.getTranslatedKeyMessage());
            return super.onInit();
        }

        @Override
        public void addChildUIElements() {
        }

        @Override
        public void alignWidgets() {
        }

        @Override
        public void tick() {
            elapsed++;
        }

        void cancel() {
            ObservationClient.cancelHeld();
        }

        @Override
        public void back() {
            cancel();
        }

        @Override
        public boolean onCloseQuery() {
            return false;
        }

        @Override
        public Screen getPrevScreen() {
            return null;
        }

        @Override
        public boolean onKeyPressed(int key, int scan, int modifiers) {
            if (key == GLFW.GLFW_KEY_ESCAPE || ObservationClient.OBSERVE.matches(key, scan)) return true;
            return super.onKeyPressed(key, scan, modifiers);
        }

        @Override
        public void drawBackground(GuiGraphics g, int x, int y, int w, int h, RenderingHint hint) {
            float progress = Math.min(1, (elapsed + getPartialTick()) / ObservationSessions.DURATION_TICKS);
            int shade = Math.round(progress * 170) << 24;
            int[] a = aperture();
            g.fill(0, 0, w, a[1], shade);
            g.fill(0, a[3], w, h, shade);
            g.fill(0, a[1], a[0], a[3], shade);
            g.fill(a[2], a[1], w, a[3], shade);
            int ink = 0xFFD8C69E;
            for (int side = 0; side < 2; side++) {
                int ax = side == 0 ? a[0] : a[2] - 7;
                g.fill(ax, a[1], ax + 7, a[1] + 1, ink);
                g.fill(ax, a[3] - 1, ax + 7, a[3], ink);
            }
            Component title = t("observation.focus");
            g.drawString(getFont(), title, (w - getFont().width(title)) / 2, h - 110, ink, false);
            g.fill(w / 2 - 64, h - 93, w / 2 + 64, h - 91, 0x80302A21);
            g.fill(w / 2 - 64, h - 93, w / 2 - 64 + Math.round(128 * progress), h - 91, ink);
            g.drawString(getFont(), keyHint, (w - getFont().width(keyHint)) / 2, h - 78, 0xFFBEB49D, false);
        }

        private int[] aperture() {
            var entity = entityId < 0 || Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(entityId);
            AABB bounds = entity == null ? new AABB(position) : entity.getBoundingBox();
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 origin = camera.getPosition();
            Quaternionf inverse = camera.rotation().conjugate(new Quaternionf());
            double focal = getHeight() / (2 * Math.tan(Math.toRadians(Minecraft.getInstance().options.fov().get()) / 2));
            double left = getWidth(), top = getHeight(), right = 0, bottom = 0;
            boolean visible = false;
            for (int corner = 0; corner < 8; corner++) {
                Vector3f vector = new Vector3f((float) ((corner & 1) == 0 ? bounds.minX - origin.x : bounds.maxX - origin.x),
                        (float) ((corner & 2) == 0 ? bounds.minY - origin.y : bounds.maxY - origin.y),
                        (float) ((corner & 4) == 0 ? bounds.minZ - origin.z : bounds.maxZ - origin.z));
                inverse.transform(vector);
                if (vector.z >= -0.01) continue;
                visible = true;
                double x = getWidth() / 2.0 + vector.x * focal / -vector.z;
                double y = getHeight() / 2.0 - vector.y * focal / -vector.z;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
            if (!visible) return new int[]{0, 0, 0, 0};
            return new int[]{Mth.clamp((int) left - 5, 0, getWidth()), Mth.clamp((int) top - 5, 0, getHeight()),
                    Mth.clamp((int) right + 5, 0, getWidth()), Mth.clamp((int) bottom + 5, 0, getHeight())};
        }
    }
}
