package com.winsonchiu.bigtwo;

import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 1/6/2015.
 */
public class MatchSettings {

    public static final String MATCH_SETTINGS = "matchSettings";
    public static final String INVITED_PLAYERS = "invitedPlayers";
    private static final String USABLE_HANDS = "usableHands";
    private static final String SUIT_ORDER = "suitOrder";
    private static final String CARD_ORDER = "cardOrder";
    private static final String NUM_DECKS = "numDecks";
    private static final String NUM_CARDS = "numCards";
    private static final String CARD_ORDER_STRAIGHT = "cardOrderStraight";
    private static final String NUM_AI = "numAi";

    private List<Hand> usableHands;
    private List<Suit> suitOrder;
    private List<Integer> cardOrder;
    private int numCards;
    private int numDecks;
    private int numAI;
    private boolean useCardOrderStraight;

    public MatchSettings() {
        usableHands = new ArrayList<>();
        suitOrder = new ArrayList<>();
        cardOrder = new ArrayList<>();
        numCards = Card.NUM_CARDS;
        numDecks = 1;
    }

    public void addUsableHand(Hand hand) {
        usableHands.add(hand);
    }

    public List<Suit> getSuitOrder() {
        return suitOrder;
    }

    public void setSuitOrder(List<Suit> suitOrder) {
        this.suitOrder = suitOrder;
    }

    public List<Integer> getCardOrder() {
        return cardOrder;
    }

    public void setCardOrder(List<Integer> cardOrder) {
        this.cardOrder = cardOrder;
    }

    public void setNumDecks(int numDecks) {
        this.numDecks = numDecks;
    }

    public int getNumDecks() {
        return numDecks;
    }

    public void setNumCards(int numCards) {
        this.numCards = numCards;
    }

    public int getNumCards() {
        return numCards;
    }

    public int getNumAI() {
        return numAI;
    }

    public void setNumAI(int numAI) {
        this.numAI = numAI;
    }

    public boolean isUseCardOrderStraight() {
        return useCardOrderStraight;
    }

    public void setUseCardOrderStraight(boolean useCardOrderStraight) {
        this.useCardOrderStraight = useCardOrderStraight;
    }

    public static MatchSettings fromJsonObject(JSONObject jsonObject) throws JSONException {

        MatchSettings matchSettings = new MatchSettings();

        JSONArray handArray = jsonObject.getJSONArray(USABLE_HANDS);
        for (int index = 0; index < handArray.length(); index++) {
            matchSettings.addUsableHand(Hand.valueOf(handArray.getString(index)));
        }

        ArrayList<Suit> suitOrder = new ArrayList<>(Suit.values().length);
        JSONArray suitArray = jsonObject.getJSONArray(SUIT_ORDER);
        for (int index = 0; index < suitArray.length(); index++) {
            suitOrder.add(Suit.valueOf(suitArray.getString(index)));
        }
        matchSettings.setSuitOrder(suitOrder);

        ArrayList<Integer> cardOrder = new ArrayList<>(13);
        JSONArray cardArray = jsonObject.getJSONArray(CARD_ORDER);
        for (int index = 0; index < cardArray.length(); index++) {
            cardOrder.add(cardArray.getInt(index));
        }
        matchSettings.setCardOrder(cardOrder);

        if (jsonObject.has(NUM_DECKS)) {
            matchSettings.setNumDecks(jsonObject.getInt(NUM_DECKS));
        }
        if (jsonObject.has(NUM_CARDS)) {
            matchSettings.setNumCards(jsonObject.getInt(NUM_CARDS));
        }

        if (jsonObject.has(CARD_ORDER_STRAIGHT)) {
            matchSettings.setUseCardOrderStraight(jsonObject.getBoolean(CARD_ORDER_STRAIGHT));
        }

        if (jsonObject.has(NUM_AI)) {
            matchSettings.setNumAI(jsonObject.getInt(NUM_AI));
        }

        return matchSettings;
    }

    public String toJsonString() {

        StringWriter stringWriter = new StringWriter();
        try {
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setIndent(AppSettings.JSON_INDENT);
            jsonWriter.beginObject();

            jsonWriter.name(USABLE_HANDS);
            jsonWriter.beginArray();
            for (Hand hand : usableHands) {
                jsonWriter.value(hand.toString());
            }
            jsonWriter.endArray();

            jsonWriter.name(SUIT_ORDER);
            jsonWriter.beginArray();
            for (Suit suit : suitOrder) {
                jsonWriter.value(suit.name());
            }
            jsonWriter.endArray();

            jsonWriter.name(CARD_ORDER);
            jsonWriter.beginArray();
            for (Integer card : cardOrder) {
                jsonWriter.value(card);
            }
            jsonWriter.endArray();

            jsonWriter.name(NUM_DECKS).value(numDecks);
            jsonWriter.name(NUM_CARDS).value(numCards);

            jsonWriter.name(CARD_ORDER_STRAIGHT).value(useCardOrderStraight);
            jsonWriter.name(NUM_AI).value(numAI);

            jsonWriter.endObject();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return stringWriter.toString();

    }

    public static MatchSettings getDefaultSettings() {

        MatchSettings matchSettings = new MatchSettings();

        for (Hand hand : Hand.values()) {
            matchSettings.addUsableHand(hand);
        }

        matchSettings.setSuitOrder(Arrays.asList(Suit.values()));

        ArrayList<Integer> cardOrder = new ArrayList<>(13);
        for (int index = 0; index < 13; index++) {
            cardOrder.add((index + 2) % 13);
        }
        matchSettings.setCardOrder(cardOrder);

        return matchSettings;
    }
}