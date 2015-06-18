package com.winsonchiu.bigtwo.logic;

import android.util.Log;

import com.winsonchiu.bigtwo.Card;
import com.winsonchiu.bigtwo.Hand;
import com.winsonchiu.bigtwo.MatchSettings;
import com.winsonchiu.bigtwo.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 4/6/2015.
 */
public class CardEvaluator {

    private static final String TAG = CardEvaluator.class.getCanonicalName();
    private MatchSettings matchSettings;

    public CardEvaluator(MatchSettings matchSettings) {
        this.matchSettings = matchSettings;
    }

    public boolean isHandValid(List<Card> hand, List<Card> lastHand) {

        Log.i(TAG, "hand: " + hand);
        Log.i(TAG, "lastHand: " + lastHand);

        Hand handType = getHandType(hand);
        Hand lastHandType = getHandType(lastHand);

        if (handType == Hand.BOMB) {
            return lastHandType != Hand.BOMB || compareCards(hand.get(hand.size() - 1), lastHand.get(lastHand.size() - 1)) > 0;
        }

        if (hand.isEmpty() || handType == Hand.INVALID) {
            return false;
        }

        if (lastHand.isEmpty()) {
            return true;
        }

        if (handType != lastHandType || hand.size() != lastHand.size()) {
            return false;
        }

        Log.i(TAG, "Hand: " + handType.name());

        Card handCompare = hand.get(hand.size() - 1);
        Card lastHandCompare = lastHand.get(lastHand.size() - 1);

        if (handType == Hand.FULL_HOUSE) {
            if (getHandType(new ArrayList<>(hand.subList(0, 3))) == Hand.THREE_OF_A_KIND) {
                handCompare = hand.get(2);
            }

            if (getHandType(new ArrayList<>(lastHand.subList(0, 3))) == Hand.THREE_OF_A_KIND) {
                lastHandCompare = lastHand.get(2);
            }
        }

        return compareCards(handCompare, lastHandCompare) > 0;

    }

    public int compareCards(Card first, Card second) {
        List<Integer> cardOrder = matchSettings.getCardOrder();

        int adjustedValueThis = cardOrder.indexOf(first.getValue());
        int adjustedValueOther = cardOrder.indexOf(second.getValue());

        return adjustedValueThis - adjustedValueOther == 0 ? compareSuits(first.getSuit(),
                second.getSuit())
                : adjustedValueThis - adjustedValueOther;

    }

    public int compareSuits(Suit first, Suit second) {

        List<Suit> suits = matchSettings.getSuitOrder();

        int firstIndex = suits.indexOf(first);
        int secondIndex = suits.indexOf(second);

        return firstIndex - secondIndex;

    }

    public Hand getHandType(List<Card> hand) {

        switch (hand.size()) {

            case 0:
                return Hand.INVALID;
            case 1:
                return Hand.SINGLE;
            case 2:
                if (hand.get(0).getValue() == hand.get(1).getValue()) {
                    return Hand.PAIR;
                }
                break;
            case 3:
                if (hand.get(0).getValue() == hand.get(1).getValue()
                        && hand.get(0).getValue() == hand.get(2).getValue()) {
                    return Hand.THREE_OF_A_KIND;
                }
                break;
            case 4:
                if (hand.get(0).getValue() == hand.get(1).getValue()
                        && hand.get(0).getValue() == hand.get(2).getValue()
                        && hand.get(0).getValue() == hand.get(3).getValue()) {
                    return Hand.BOMB;
                }
                break;
            case 5:
                boolean isSame = true;
                Card checkCard = hand.get(0);
                for (int index = 1; index < hand.size(); index++) {
                    Card card = hand.get(index);
                    if (card.getValue() != checkCard.getValue()) {
                        isSame = false;
                        break;
                    }
                }

                if (isSame) {
                    return Hand.INVALID;
                }

                if ((getHandType(new ArrayList<>(hand.subList(0, 2))) == Hand.PAIR
                        && getHandType(new ArrayList<>(hand.subList(2, 5))) == Hand.THREE_OF_A_KIND) ||
                        getHandType(new ArrayList<>(hand.subList(0, 3))) == Hand.THREE_OF_A_KIND
                                && getHandType(new ArrayList<>(hand.subList(3, 5))) == Hand.PAIR) {
                    return Hand.FULL_HOUSE;
                }
            default:
                break;

        }

        if (matchSettings.isUseCardOrderStraight()) {
            return getConsecutiveCardOrder(hand);
        }

        return getConsecutiveAceToAce(hand);

    }

    public Hand getConsecutiveAceToAce(List<Card> hand) {

        boolean containsKing = false;

        for (Card card : hand) {
            if (card.getValue() == 12) {
                containsKing = true;
            }
        }

        Comparator<Card> comparator;

        if (containsKing) {
            comparator = new Comparator<Card>() {
                @Override
                public int compare(Card lhs, Card rhs) {

                    int lhsValue = lhs.getValue() == 0 ? 13 : lhs.getValue();
                    int rhsValue = rhs.getValue() == 0 ? 13 : rhs.getValue();

                    return lhsValue - rhsValue == 0 ? compareSuits(lhs.getSuit(), rhs.getSuit())
                            : lhsValue - rhsValue;
                }
            };
        }
        else {
            comparator = new Comparator<Card>() {
                @Override
                public int compare(Card lhs, Card rhs) {
                    return lhs.getValue() - rhs.getValue() == 0 ? compareSuits(lhs.getSuit(),
                            rhs.getSuit())
                            : lhs.getValue() - rhs.getValue();
                }
            };
        }

        Collections.sort(hand, comparator);

        if (hand.size() >= 4 && hand.size() % 2 == 0) {

            boolean isConsecutivePair = true;

            int prevValue = hand.get(0).getValue() - 1;

            for (int index = 0; index < hand.size(); index += 2) {

                int currentValue = hand.get(index).getValue();
                int difference = currentValue - prevValue < 0 ? currentValue - prevValue + 13
                        : currentValue - prevValue;
                if (index > 0 && difference != 1) {
                    isConsecutivePair = false;
                    break;
                }

                prevValue = currentValue;

                if (getHandType(new ArrayList<>(hand.subList(index, index + 2))) != Hand.PAIR) {
                    isConsecutivePair = false;
                    break;
                }

            }

            if (isConsecutivePair) {
                return Hand.CONSECUTIVE_PAIR;
            }
        }

        if (hand.size() >= 6 && hand.size() % 3 == 0) {

            boolean isConsecutiveThree = true;

            int prevValue = hand.get(0).getValue() - 1;

            for (int index = 0; index < hand.size(); index += 3) {

                int currentValue = hand.get(index).getValue();
                int difference = currentValue - prevValue < 0 ? currentValue - prevValue + 13
                        : currentValue - prevValue;
                if (index > 0 && difference != 1) {
                    isConsecutiveThree = false;
                    break;
                }

                prevValue = currentValue;

                if (getHandType(new ArrayList<>(hand.subList(index, index + 3)))
                        != Hand.THREE_OF_A_KIND) {
                    isConsecutiveThree = false;
                    break;
                }

            }

            if (isConsecutiveThree) {
                return Hand.CONSECUTIVE_THREE_OF_A_KIND;
            }
        }

        if (hand.size() >= 5) {
            int numberOfAces = 0;
            Card ace = null;

            for (Card card : hand) {
                if (card.getValue() == 0) {
                    numberOfAces++;
                    ace = card;
                }
            }

            if (numberOfAces == 2) {

                Collections.sort(hand, new Comparator<Card>() {
                    @Override
                    public int compare(Card lhs, Card rhs) {
                        return lhs.getValue() - rhs.getValue() == 0 ? compareSuits(lhs.getSuit(),
                                rhs.getSuit())
                                : lhs.getValue() - rhs.getValue();
                    }
                });

                hand.remove(ace);
                hand.add(ace);
            }

            boolean isStraight = true;

            int prevValue = hand.get(0).getValue();

            for (int index = 1; index < hand.size(); index++) {

                int currentValue = hand.get(index).getValue();
                int difference = currentValue - prevValue < 0 ? currentValue - prevValue + 13
                        : currentValue - prevValue;
                if (difference != 1) {
                    isStraight = false;
                    break;
                }

                prevValue = currentValue;

            }

            if (isStraight) {
                return Hand.STRAIGHT;
            }
        }

        return Hand.INVALID;
    }

    public Hand getConsecutiveCardOrder(List<Card> hand) {

        Collections.sort(hand, new Comparator<Card>() {
            @Override
            public int compare(Card lhs, Card rhs) {
                return compareCards(lhs, rhs);
            }
        });

        if (hand.size() >= 4 && hand.size() % 2 == 0) {

            boolean isConsecutivePair = true;

            int prevValue = hand.get(0).getValue() - 1;

            for (int index = 0; index < hand.size(); index += 2) {

                int currentValue = hand.get(index).getValue();
                int difference = currentValue - prevValue < 0 ? currentValue - prevValue + 13
                        : currentValue - prevValue;
                if (index > 0 && difference != 1) {
                    isConsecutivePair = false;
                    break;
                }

                prevValue = currentValue;

                if (getHandType(new ArrayList<>(hand.subList(index, index + 2))) != Hand.PAIR) {
                    isConsecutivePair = false;
                    break;
                }

            }

            if (isConsecutivePair) {
                return Hand.CONSECUTIVE_PAIR;
            }
        }

        if (hand.size() >= 6 && hand.size() % 3 == 0) {

            boolean isConsecutiveThree = true;

            int prevValue = hand.get(0).getValue() - 1;

            for (int index = 0; index < hand.size(); index += 3) {

                int currentValue = hand.get(index).getValue();
                int difference = currentValue - prevValue < 0 ? currentValue - prevValue + 13
                        : currentValue - prevValue;
                if (index > 0 && difference != 1) {
                    isConsecutiveThree = false;
                    break;
                }

                prevValue = currentValue;

                Log.d(TAG, "Triple: " + new ArrayList<>(hand.subList(index, index + 3)).toString());

                if (getHandType(new ArrayList<>(hand.subList(index, index + 3)))
                        != Hand.THREE_OF_A_KIND) {
                    isConsecutiveThree = false;
                    break;
                }

            }

            if (isConsecutiveThree) {
                return Hand.CONSECUTIVE_THREE_OF_A_KIND;
            }
        }

        if (hand.size() > Card.NUM_CARDS) {
            return Hand.INVALID;
        }

        if (hand.size() >= 5) {
            boolean isStraight = true;

            int prevValue = hand.get(0).getValue();

            for (int index = 1; index < hand.size(); index++) {

                int currentValue = hand.get(index).getValue();
                int difference = currentValue - prevValue < 0 ? currentValue - prevValue + 13
                        : currentValue - prevValue;
                if (difference != 1) {
                    isStraight = false;
                    break;
                }

                prevValue = currentValue;

            }

            if (isStraight) {
                return Hand.STRAIGHT;
            }
        }

        return Hand.INVALID;
    }

}
