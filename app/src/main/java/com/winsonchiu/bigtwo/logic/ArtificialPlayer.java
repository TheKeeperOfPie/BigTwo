package com.winsonchiu.bigtwo.logic;

import android.util.Log;

import com.winsonchiu.bigtwo.Card;
import com.winsonchiu.bigtwo.Hand;
import com.winsonchiu.bigtwo.MatchSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by TheKeeperOfPie on 4/7/2015.
 */
public class ArtificialPlayer {

    private static final String TAG = ArtificialPlayer.class.getCanonicalName();
    private final CardEvaluator cardEvaluator;
    private List<Card> hand;
    private HashMap<Integer, List<Card>> cardMap;

    public ArtificialPlayer(List<Card> hand, MatchSettings matchSettings) {
        cardEvaluator = new CardEvaluator(matchSettings);
        setHand(hand);
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
        Collections.sort(hand, new Comparator<Card>() {
            @Override
            public int compare(Card lhs, Card rhs) {
                return cardEvaluator.compareCards(lhs, rhs);
            }
        });
        this.cardMap = new HashMap<>(Card.NUM_CARDS);

        for (Card card : hand) {
            List<Card> cardList = cardMap.containsKey(card.getValue()) ? cardMap.get(card.getValue()) : new ArrayList<Card>();
            cardList.add(card);
            cardMap.put(card.getValue(), cardList);
        }

        for (int index = 0; index < Card.NUM_CARDS; index++) {
            if (!cardMap.containsKey(index)) {
                cardMap.put(index, new ArrayList<Card>());
            }
        }
    }

    public List<Card> takeTurn(List<Card> lastHand) {

        Random random = new Random();
        ArrayList<List<Card>> possibleHands;
        ArrayList<List<Card>> playableHands = new ArrayList<>();
        ArrayList<List<Card>> exactSizeHands = new ArrayList<>();
        ArrayList<Card> handToPlay = new ArrayList<>();
        Hand lastHandType = cardEvaluator.getHandType(lastHand);
        Log.d(TAG, "lastHandType: " + lastHandType);

        switch (lastHandType) {

            case INVALID:
                // TODO: Find and return lowest hand
                handToPlay.add(hand.get(0));
                return handToPlay;
            case SINGLE:
                Card compareCard = lastHand.get(lastHand.size() - 1);
                for (Card card : hand) {
                    if (cardEvaluator.compareCards(card, compareCard) > 0 && cardMap.get(card.getValue()).size() == 1) {
                        handToPlay.add(card);
                        return handToPlay;
                    }
                }
                if (random.nextFloat() > 0.75f) {
                    ArrayList<Card> randomizedHand = new ArrayList<>(hand);
                    Collections.shuffle(randomizedHand);
                    for (Card card : randomizedHand) {
                        if (cardEvaluator.compareCards(card, compareCard) > 0) {
                            handToPlay.add(card);
                            return handToPlay;
                        }
                    }
                }
                break;
            case PAIR:
                possibleHands = getPossibleMultiples(2);
                playableHands.clear();
                for (List<Card> hand : possibleHands) {
                    if (cardEvaluator.compareCards(hand.get(hand.size() - 1), lastHand.get(1)) > 0) {
                        if (hand.size() == lastHand.size()) {
                            return hand;
                        }
                        playableHands.add(hand);
                    }
                }
                if (!playableHands.isEmpty()) {
                    // TODO: Check how optimal a hand is
                    // TODO: Allow breaking up of higher hands
                    return playableHands.get(0).subList(0, 2);
                }
                break;
            case CONSECUTIVE_PAIR:
                possibleHands = getViableConsecutiveHands(2, lastHand.size());
                for (List<Card> possibleHand : possibleHands) {
                    List<Card> subListHand = possibleHand.subList(0, lastHand.size());
                    if (cardEvaluator.isHandValid(subListHand, lastHand)) {
                        return subListHand;
                    }
                }
                break;
            case THREE_OF_A_KIND:
                possibleHands = getPossibleMultiples(3);
                playableHands.clear();
                for (List<Card> hand : possibleHands) {
                    if (cardEvaluator.compareCards(hand.get(hand.size() - 1), lastHand.get(2)) > 0) {
                        if (hand.size() == lastHand.size()) {
                            return hand;
                        }
                        playableHands.add(hand);
                    }
                }
                if (!playableHands.isEmpty()) {
                    // TODO: Check how optimal a hand is
                    // TODO: Allow breaking up of higher hands
                    return playableHands.get(0).subList(0, 3);
                }
                break;
            case CONSECUTIVE_THREE_OF_A_KIND:
                possibleHands = getViableConsecutiveHands(2, lastHand.size());
                for (List<Card> possibleHand : possibleHands) {
                    List<Card> subListHand = possibleHand.subList(0, lastHand.size());
                    if (cardEvaluator.isHandValid(subListHand, lastHand)) {
                        return subListHand;
                    }
                }
                break;
            case FULL_HOUSE:
                ArrayList<List<Card>> possiblePairs = getPossibleMultiples(2);
                ArrayList<List<Card>> possibleTriples = getPossibleMultiples(3);
                if (!possiblePairs.isEmpty() && !possibleTriples.isEmpty()) {

                    for (List<Card> triple : possibleTriples) {
                        ArrayList<Card> fullHouseHand = new ArrayList<>(triple);
                        fullHouseHand.addAll(possiblePairs.get(0));
                        if (cardEvaluator.isHandValid(fullHouseHand, lastHand)) {
                            return fullHouseHand;
                        }
                    }

                }
                break;
            case STRAIGHT:
                possibleHands = getViableConsecutiveHands(1, lastHand.size());
                for (List<Card> possibleHand : possibleHands) {
                    if (cardEvaluator.isHandValid(possibleHand, lastHand)) {
                        return possibleHand;
                    }
                }
                break;
            case BOMB:
                possibleHands = getPossibleMultiples(4);
                for (List<Card> possibleHand : possibleHands) {
                    if (cardEvaluator.compareCards(possibleHand.get(2), lastHand.get(2)) > 0 && possibleHand.size() == 4) {
                        return possibleHand;
                    }
                }
                break;
        }

        return null;
    }

    public ArrayList<List<Card>> getViableConsecutiveHands(int numInOneSet, int handSize) {

        ArrayList<List<Card>> possibleMultiples = getPossibleMultiples(numInOneSet);
        if (possibleMultiples.size() <= 1) {
            return new ArrayList<>();
        }
        ArrayList<List<Card>> viableHands = new ArrayList<>();
        int currentValue = possibleMultiples.get(0).get(0).getValue();
        int tempLength = 1;
        for (int index = 1; index < possibleMultiples.size(); index++) {
            int nextValue = possibleMultiples.get(0).get(0).getValue();
            if (nextValue - currentValue == 1) {
                tempLength++;
            }
            else {
                tempLength = 1;
            }
            if (tempLength * numInOneSet >= handSize) {
                ArrayList<Card> viableHand = new ArrayList<>();
                for (int position = 0; position < handSize / numInOneSet; position++) {
                    viableHand.addAll(possibleMultiples.get(index - position));
                }
                viableHands.add(viableHand);
            }
            currentValue = nextValue;
        }

        return viableHands;
    }

    public ArrayList<List<Card>> getPossibleMultiples(int handSize) {

        ArrayList<List<Card>> possibleMultiples = new ArrayList<>();
        for (int value : cardMap.keySet()) {
            if (cardMap.get(value).size() >= handSize) {
                possibleMultiples.add(cardMap.get(value));
            }
        }

        return possibleMultiples;
    }

}