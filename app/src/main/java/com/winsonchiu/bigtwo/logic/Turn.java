package com.winsonchiu.bigtwo.logic;

import android.util.JsonWriter;

import com.winsonchiu.bigtwo.AppSettings;
import com.winsonchiu.bigtwo.Card;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 2/10/2015.
 */
public class Turn {

    private static final String PLAYER = "player";
    private static final String HAND = "hand";
    private static final String TIME = "time";

    private String player;
    private List<Card> hand;
    private long time;

    public Turn(String player, List<Card> hand, long time) {
        this.player = player;
        this.hand = hand;
        this.time = time;
    }

    public String getPlayer() {
        return player;
    }

    public List<Card> getHand() {
        return hand;
    }

    public long getTime() {
        return time;
    }

    public String toJsonString() {

        StringWriter stringWriter = new StringWriter();
        try {
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setIndent(AppSettings.JSON_INDENT);
            jsonWriter.beginObject();

            jsonWriter.name(PLAYER).value(player);
            jsonWriter.name(TIME).value(time);

            jsonWriter.name(HAND);
            jsonWriter.beginArray();
            for (Card card : hand) {
                jsonWriter.value(card.toString());
            }
            jsonWriter.endArray();

            jsonWriter.endObject();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return stringWriter.toString();
    }

    public static Turn fromJsonObject(JSONObject jsonObject) throws JSONException {

        String player = "";
        ArrayList<Card> hand = new ArrayList<>();
        long time = -1;

        if (jsonObject.has(PLAYER)) {
            player = jsonObject.getString(PLAYER);
        }

        if (jsonObject.has(TIME)) {
            time = jsonObject.getLong(TIME);
        }

        if (jsonObject.has(HAND)) {
            JSONArray handArray = jsonObject.getJSONArray(HAND);
            for (int index = 0; index < handArray.length(); index++) {
                hand.add(Card.fromString(handArray.getString(index)));
            }
        }

        return new Turn(player, hand, time);

    }

    @Override
    public String toString() {
        return "Turn{" +
                "player='" + player + '\'' +
                ", hand=" + hand +
                ", time=" + time +
                '}';
    }
}