package com.teammoeg.frostedresearch.knowledge.game;

/** The server checks an editable layout before allowing it to be filed. */
public interface DeskDrawing {
    String initialLayout();
    boolean valid(String layout);
    boolean solved(String layout);
}
