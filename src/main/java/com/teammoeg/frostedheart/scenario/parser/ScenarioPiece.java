package com.teammoeg.frostedheart.scenario.parser;

import java.util.List;

public class ScenarioPiece {
    public final String fileName;
    public final List<Node> pieces;

    public ScenarioPiece(String fileName, List<Node> pieces) {
        super();
        this.fileName = fileName;
        this.pieces = pieces;
    }
}
