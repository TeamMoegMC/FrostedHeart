package com.teammoeg.frostedheart.research.gui.drawdesk.game;

public class CardStat {
    public final CardType type;
    public final int card;
    public int num;
    public int tot;

    public CardStat(CardType type, int card) {
        super();
        this.type = type;
        this.card = card;
        this.num = 0;
        this.tot = 0;
    }

    public boolean isGood() {
        return type.isGood(num);
    }

    public int pack() {
        return card + (type.ordinal() << 16);
    }

    @Override
    public String toString() {
        return "CardStat [type=" + type + ", card=" + card + ", num=" + num + "]";
    }
}
