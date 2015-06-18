package com.winsonchiu.bigtwo;

import java.io.Serializable;

/**
 * Created by TheKeeperOfPie on 12/18/2014.
 */
public enum Suit implements Serializable {
    CLUB(0, R.drawable.club),
    DIAMOND(1, R.drawable.diamond),
    HEART(2, R.drawable.heart),
    SPADE(3, R.drawable.spade);

    private final int value;

    private final int drawable;

    private Suit(int value, int drawable) {
        this.value = value;
        this.drawable = drawable;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        switch (this) {
            default:
            case CLUB:
                return "C";
            case DIAMOND:
                return "D";
            case HEART:
                return "H";
            case SPADE:
                return "S";
        }
    }

    public static Suit getSuitFromCharacter(char character) {
        switch (character) {
            default:
            case 'C':
                return CLUB;
            case 'D':
                return DIAMOND;
            case 'H':
                return HEART;
            case 'S':
                return SPADE;
        }
    }

    public int getDrawable() {
        return drawable;
    }
}
