package com.winsonchiu.bigtwo.logic;

import android.util.JsonWriter;
import android.util.Log;

import com.winsonchiu.bigtwo.AppSettings;
import com.winsonchiu.bigtwo.Card;
import com.winsonchiu.bigtwo.MatchSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 12/28/2014.
 */
public class GameData {

    public static final int CURRENT_VERSION = 4;
    public static final String VERSION = "version";
    public static final String IS_FIRST_TURN = "isFirstTurn";
    public static final String PLAYED_CARDS = "playedCards";
    public static final String LAST_HAND = "lastHand";
    public static final String ACTIVE_PLAYERS = "activePlayers";
    public static final String STARTING_PLAYERS = "startingPlayers";
    public static final String FINISHED_PLAYERS = "finishedPlayers";
    public static final String REMOVED_PLAYERS = "removedPlayers";
    public static final String MATCH_SETTINGS = "matchSettings";
    public static final String NEXT_PLAYER = "nextPlayer";
    public static final String FINISHED = "finished";
    public static final String TURNS = "turns";
    public static final String CARDS_PREFIX = " Cards: ";
    public static final String CARD_SEPARATOR = ", ";
    private static final String TAG = GameData.class.getCanonicalName();
    HashMap<String, List<Card>> cards;
    List<String> activePlayers;
    List<String> startingPlayers;
    List<String> finishedPlayers;
    List<String> removedPlayers;
    List<Turn> turns;
    private MatchSettings settings;
    private int version;
    private boolean isFirstTurn;
    private boolean newTurn;
    private String nextPlayer;
    private boolean finished;

    public GameData() {
        settings = new MatchSettings();
        cards = new HashMap<>();
        activePlayers = new ArrayList<>();
        startingPlayers = new ArrayList<>();
        finishedPlayers = new ArrayList<>();
        removedPlayers = new ArrayList<>();
        turns = new ArrayList<>();
        version = CURRENT_VERSION;
        nextPlayer = "";
    }

    public GameData(int version, boolean isFirstTurn, MatchSettings settings,
                    HashMap<String, List<Card>> cards, List<String> activePlayers,
                    List<String> startingPlayers, List<String> finishedPlayers,
                    List<String> removedPlayers, List<Turn> turns, String nextPlayer,
                    boolean finished) {
        this.version = version;
        this.isFirstTurn = isFirstTurn;
        this.settings = settings;
        this.cards = cards;
        this.activePlayers = activePlayers;
        this.startingPlayers = startingPlayers;
        this.finishedPlayers = finishedPlayers;
        this.removedPlayers = removedPlayers;
        this.turns = turns;
        this.nextPlayer = nextPlayer;
        this.finished = finished;
    }

    public int getVersion() {
        return version;
    }

    public void playHand(String playerId, List<Card> hand) {

        isFirstTurn = false;

        if (!getNextPlayer().equals(playerId)) {
            throw new IllegalStateException("Not " + playerId + "'s turn");
        }

        if (!getActivePlayers().contains(playerId)) {
            throw new IllegalStateException("Player ID " + playerId + " not in active players");
        }

        calculateNextPlayer(playerId, hand.isEmpty());

        if (hand.isEmpty()) {
            removeActivePlayer(playerId);
        }
        else {
            turns.add(new Turn(playerId, hand, System.currentTimeMillis()));
            if (cards.containsKey(playerId)) {
                for (Card card : hand) {
                    cards.get(playerId)
                            .remove(card);
                }
            }
            if (cards.get(playerId)
                    .isEmpty()) {
                addFinishedPlayer(playerId);
            }
        }

        Log.d(TAG, "playerId: " + playerId);
        Log.d(TAG, "getNextPlayer: " + getNextPlayer());
    }

    public boolean isFirstTurn() {
        return isFirstTurn;
    }

    public void setFirstTurn(boolean isFirstTurn) {
        this.isFirstTurn = isFirstTurn;
    }

    public List<Card> getLastHand() {

        if (turns.isEmpty()) {
            return new ArrayList<>();
        }

        return turns.get(turns.size() - 1).getHand();
    }

    public List<Turn> getTurns() {
        return turns;
    }

    public Turn getLastTurn() {
        return turns.isEmpty() ? null : turns.get(turns.size() - 1);
    }

    public String getLastPlayer() {

        if (turns.isEmpty()) {
            return null;
        }

        return turns.get(turns.size() - 1).getPlayer();

    }

    public void clearHands() {
        cards = new HashMap<>();
    }

    public void setHand(String player, List<Card> hand) {
        cards.put(player, hand);
    }

    public List<Card> getHand(String player) {
        return cards.get(player);
    }

    public void setPlayedCards(ArrayList<Card> playedCards) {
        cards.put(PLAYED_CARDS, playedCards);
    }

    public ArrayList<Card> getPlayedCards(boolean newTurn) {

        if (turns.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<Card> playedCards = new ArrayList<>();

        int maxIndex = newTurn ? turns.size() : turns.size() - 1;

        for (int index = 0; index < maxIndex; index++) {

            playedCards.addAll(turns.get(index).getHand());
        }

        return playedCards;
    }

    public void setNewTurn(boolean newTurn) {
        this.newTurn = newTurn;
    }

    public boolean isNewTurn() {
        return newTurn;
    }

    public List<String> getStartingPlayers() {
        return startingPlayers;
    }

    public void setStartingPlayers(ArrayList<String> players) {
        startingPlayers = players;
    }

    public void removePlayer(String player) {
        if (getNextPlayer().equals(player)) {
            calculateNextPlayer(player, false);
        }
        removedPlayers.add(player);
        removeActivePlayer(player);
        if (cards.containsKey(player)) {
            if (cards.containsKey(PLAYED_CARDS)) {
                cards.get(PLAYED_CARDS).addAll(cards.get(player));
            }
            cards.get(player).clear();
        }
        if (getRemainingActivePlayers().size() <= 1) {
            setIsFinished(true);
        }
    }

    private void calculateNextPlayer(String playerId, boolean skip) {
        if (getActivePlayers().isEmpty() || (getActivePlayers().size() == 1 && skip)) {
            Log.d(TAG, "getRemainingActivePlayers: " + getRemainingActivePlayers());
            setNextPlayer(getRemainingActivePlayers().get(
                    (getRemainingActivePlayers().indexOf(playerId) + 1) % getRemainingActivePlayers().size()));
            setActivePlayers(new ArrayList<String>());
        }
        else {
            Log.d(TAG, "getActivePlayers: " + getActivePlayers());
            setNextPlayer(getActivePlayers().get((getActivePlayers().indexOf(playerId) + 1) % getActivePlayers().size()));
        }
    }

    public void addFinishedPlayer(String player) {
        finishedPlayers.add(player);
        removeActivePlayer(player);
        if (getRemainingActivePlayers().size() <= 1) {
            setIsFinished(true);
        }
    }

    public void setFinishedPlayers(ArrayList<String> players) {
        finishedPlayers = players;
    }

    public List<String> getFinishedPlayers() {
        return finishedPlayers;
    }

    public ArrayList<String> getRemainingPlayers() {
        ArrayList<String> remainingPlayers = new ArrayList<>(startingPlayers);
        remainingPlayers.removeAll(removedPlayers);
        return remainingPlayers;
    }

    public ArrayList<String> getRemainingActivePlayers() {
        ArrayList<String> remainingPlayers = getRemainingPlayers();
        remainingPlayers.removeAll(finishedPlayers);
        return remainingPlayers;
    }

    public void setActivePlayers(ArrayList<String> players) {
        activePlayers = players;
    }

    public boolean removeActivePlayer(String player) {
        return activePlayers.remove(player);
    }

    public List<String> getActivePlayers() {
        return activePlayers;
    }

    public MatchSettings getSettings() {
        return settings;
    }

    public void setSettings(MatchSettings settings) {
        this.settings = settings;
    }

    public void setNextPlayer(String nextPlayer) {
        this.nextPlayer = nextPlayer;
    }

    public String getNextPlayer() {
        return nextPlayer;
    }


    public void setIsFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isFinished() {
        return finished;
    }

    public static GameData fromJsonString(String jsonString)
            throws JSONException {

        JSONObject jsonObject = new JSONObject(jsonString);
        int version = jsonObject.getInt(VERSION);
        if (version != CURRENT_VERSION) {
            return null;
        }

        HashMap<String, List<Card>> hands = new HashMap<>();
        List<Turn> turns = new ArrayList<>();

        List<String> activePlayers = parsePlayers(jsonObject, ACTIVE_PLAYERS);
        List<String> startingPlayers = parsePlayers(jsonObject, STARTING_PLAYERS);
        List<String> finishedPlayers = parsePlayers(jsonObject, FINISHED_PLAYERS);
        List<String> removedPlayers = parsePlayers(jsonObject, REMOVED_PLAYERS);

        MatchSettings matchSettings = MatchSettings.fromJsonObject(
                new JSONObject(jsonObject.getString("matchSettings")));
        String nextPlayer = jsonObject.getString(NEXT_PLAYER);
        boolean isFirstTurn = jsonObject.getBoolean(IS_FIRST_TURN);
        boolean finished = jsonObject.getBoolean(FINISHED);

        hands.put(LAST_HAND, parseHand(jsonObject, LAST_HAND));
        hands.put(PLAYED_CARDS, parseHand(jsonObject, PLAYED_CARDS));

        for (String player : startingPlayers) {

            ArrayList<Card> hand = new ArrayList<>();
            JSONArray jsonArray = jsonObject.getJSONArray(player);

            if (jsonArray != null) {
                for (int index = 0; index < jsonArray.length(); index++) {
                    hand.add(Card.fromString(jsonArray.getString(index)));
                }
            }
            hands.put(player, hand);
        }

        JSONArray turnsArray = jsonObject.getJSONArray(TURNS);
        for (int index = 0; index < turnsArray.length(); index++) {
            turns.add(Turn.fromJsonObject(new JSONObject(turnsArray.getString(index))));
        }

        return new GameData(version, isFirstTurn, matchSettings, hands, activePlayers,
                startingPlayers, finishedPlayers, removedPlayers, turns, nextPlayer, finished);
    }

    public static List<Card> parseHand(JSONObject jsonObject, String key) throws JSONException {
        if (!jsonObject.has(key)) {
            return new ArrayList<>();
        }
        JSONArray jsonArray = jsonObject.getJSONArray(key);
        ArrayList<Card> hand = new ArrayList<>();

        if (jsonArray != null) {
            for (int index = 0; index < jsonArray.length(); index++) {
                hand.add(Card.fromString(jsonArray.getString(index)));
            }
        }
        return hand;
    }

    public static List<String> parsePlayers(JSONObject jsonObject, String key) throws JSONException {

        JSONArray jsonArray = jsonObject.getJSONArray(key);
        ArrayList<String> players = new ArrayList<>();
        if (jsonArray != null) {
            for (int index = 0; index < jsonArray.length(); index++) {
                players.add(jsonArray.getString(index));
            }
        }
        return players;
    }

    public String toJsonString() {
        StringWriter stringWriter = new StringWriter();
        try {
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setIndent(AppSettings.JSON_INDENT);
            jsonWriter.beginObject();

            jsonWriter.name(VERSION).value(version);
            jsonWriter.name(IS_FIRST_TURN).value(isFirstTurn);

            for (String key : cards.keySet()) {
                jsonWriter.name(key);
                jsonWriter.beginArray();
                for (Card card : cards.get(key)) {
                    jsonWriter.value(card.toString());
                }
                jsonWriter.endArray();
            }

            jsonWriter.name(ACTIVE_PLAYERS);
            jsonWriter.beginArray();
            for (String player : activePlayers) {
                jsonWriter.value(player);
            }
            jsonWriter.endArray();

            jsonWriter.name(STARTING_PLAYERS);
            jsonWriter.beginArray();
            for (String player : startingPlayers) {
                jsonWriter.value(player);
            }
            jsonWriter.endArray();

            jsonWriter.name(FINISHED_PLAYERS);
            jsonWriter.beginArray();
            for (String player : finishedPlayers) {
                jsonWriter.value(player);
            }
            jsonWriter.endArray();

            jsonWriter.name(REMOVED_PLAYERS);
            jsonWriter.beginArray();
            for (String player : removedPlayers) {
                jsonWriter.value(player);
            }
            jsonWriter.endArray();

            jsonWriter.name(TURNS);
            jsonWriter.beginArray();
            for (Turn turn : turns) {
                jsonWriter.value(turn.toJsonString());
            }
            jsonWriter.endArray();

            if (settings == null) {
                settings = MatchSettings.getDefaultSettings();
            }

            jsonWriter.name(MATCH_SETTINGS).value(settings.toJsonString());
            jsonWriter.name(NEXT_PLAYER).value(nextPlayer);
            jsonWriter.name(FINISHED).value(finished);

            jsonWriter.endObject();

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return stringWriter.toString();
    }
}