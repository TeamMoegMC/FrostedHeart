package com.teammoeg.chorda.client.icon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.widget.IconButton;
import net.minecraftforge.common.util.Size2i;

public enum FlatIcon {
    MOUSE_LEFT   (0 , 0 ),
    MOUSE_RIGHT  (10, 0 ),
    MOUSE_MIDDLE (20, 0 ),
    SIGHT        (30, 0 ),
    QUESTION_MARK(0 , 10),
    LOCK         (10, 10),
    CONTINUE     (20, 10),
    FORBID       (30, 10),
    RIGHT        (40, 10),
    DOWN         (50, 10),
    LEFT         (60, 10),
    TOP          (70, 10),
    TRADE        (0 , 20),
    GIVE         (10, 20),
    GAIN         (20, 20),
    LEAVE        (30, 20),
    JUMP         (40, 20),
    BOX          (0 , 30),
    BOX_ON       (10, 30),
    CROSS        (20, 30),
    HISTORY      (30, 30),
    LIST         (40, 30),
    TRASH_CAN    (50, 30),
    CHECK        (60, 30),
    FOLDER       (70, 30),
    LEFT_SLIDE   (0 , 40),
    RIGHT_SLIDE  (0 , 50),
    WRENCH       (0 , 70);

    public final int x;
    public final int y;
    public final Size2i size;

    FlatIcon(int x, int y, Size2i size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }

    FlatIcon(int x, int y) {
        this.x = x;
        this.y = y;
        this.size = new Size2i(10, 10);
    }

    public void render(PoseStack pose, int x, int y, int color) {
        CGuiHelper.bindTexture(IconButton.ICON_LOCATION);
        CGuiHelper.blitColored(pose, x, y, this.size.width, this.size.height, this.x, this.y, this.size.width, this.size.height, IconButton.TEXTURE_WIDTH, IconButton.TEXTURE_HEIGHT, color);
    }

    public static void render(FlatIcon icon, PoseStack pose, int x, int y, int color) {
        icon.render(pose, x, y, color);
    }

    private CIcons.CIcon cache;
    public CIcons.CIcon toCIcon() {
        if (cache == null)
            cache = CIcons.getIcon(IconButton.ICON_LOCATION, x, y, size.width, size.height, IconButton.TEXTURE_WIDTH, IconButton.TEXTURE_HEIGHT);
        return cache;
    }
}
