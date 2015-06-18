package com.winsonchiu.bigtwo.logic;

import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.Participatable;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.winsonchiu.bigtwo.CustomRenderer;
import com.winsonchiu.bigtwo.MatchPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by TheKeeperOfPie on 4/4/2015.
 */
public class GameUtils {

    private static final String TAG = GameUtils.class.getCanonicalName();

    public static List<MatchPlayer> getPlayers(Participatable participatable) {

        List<MatchPlayer> matchPlayers = new ArrayList<>();

        for (Participant participant : participatable.getParticipants()) {
            matchPlayers.add(new MatchPlayer(participant.getParticipantId(), participant.getDisplayName(), participant.getHiResImageUri()));
        }

        return matchPlayers;
    }


    public static ArrayList<MatchPlayer> getWinners(Participatable participatable, GameData gameData) {

        HashMap<String, Participant> participantHashMap = new HashMap<>();
        for (Participant participant : participatable.getParticipants()) {
            participantHashMap.put(participant.getParticipantId(), participant);
        }

        ArrayList<MatchPlayer> winners = new ArrayList<>(gameData.getFinishedPlayers().size());

        for (String playerId : gameData.getFinishedPlayers()) {

            Participant participant = participantHashMap.get(playerId);
            winners.add(new MatchPlayer(playerId, participant.getDisplayName(),
                    participant.getHiResImageUri()));

        }

        List<String> allPlayerIds = new ArrayList<>(participantHashMap.keySet());
        allPlayerIds.removeAll(gameData.getFinishedPlayers());

        for (String playerId : allPlayerIds) {

            Participant participant = participantHashMap.get(playerId);
            winners.add(new MatchPlayer(playerId, participant.getDisplayName(),
                    participant.getHiResImageUri()));

        }

        return winners;
    }

    public interface GameEventListener {
        void toast(String s);
        void setTurn(boolean isTurn);
        CustomRenderer getRenderer();
        void loadGameFragment();
        void returnToLaunch();
        void setPlayers(List<MatchPlayer> matchPlayers, GameData gameData);
        void setWinners(ArrayList<MatchPlayer> winners, GameData gameData);
        void vibrate(int duration);
        void loadWaitingRoom(Room room);
        void postOnUiThread(Runnable runnable);
        void postDelayedOnSurfaceView(Runnable runnable, long delayMillis);
        void setPlayer(Turn newTurn);
        boolean isGameFragmentLoaded();
        void removeMyTurnMatch(TurnBasedMatch match);
    }

}