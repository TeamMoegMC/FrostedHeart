package com.teammoeg.frostedheart.content.research.research.clues;

import com.teammoeg.frostedheart.content.research.research.Research;

public record ClueClosure<C extends Clue>(Research research, C clue) {

}