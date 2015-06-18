package com.winsonchiu.bigtwo;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.winsonchiu.bigtwo.logic.CardEvaluator;
import com.winsonchiu.bigtwo.logic.GameData;
import com.winsonchiu.bigtwo.logic.GameUtils;
import com.winsonchiu.bigtwo.logic.Turn;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by TheKeeperOfPie on 4/4/2015.
 */
public class GameRealTime implements GameBase, RoomUpdateListener, RealTimeMessageReceivedListener,
        RoomStatusUpdateListener {

    private static final String TAG = GameRealTime.class.getCanonicalName();
    private static final int MAX_ITERATIONS = 2;
    private static final long UPDATE_INTERVAL = 2000;

    // Sender IDs
    private static final byte HOST = 0;
    private static final byte PLAYER = 1;

    // Game state
    private static final byte STARTING = 11;
    private static final byte PLAYING = 12;
    private static final byte MY_TURN = 13;
    private static final byte FINISHED = 14;

    private GoogleApiClient googleApiClient;
    private GameUtils.GameEventListener listener;
    private Invitation invitation;
    private MatchSettings matchSettings;
    private int iterations;
    private Handler handler;
    private ArrayList<String> invitedPlayers;
    private ArrayList<Participant> participants;
    private Turn lastTurn;
    private byte lastTurnNumber;
    private byte myTurnNumber;
    private byte[] message;
    private byte state;
    private Map<String, Byte> players;
    private GameData gameData;
    private HashSet<String> activatedPlayers;

    public GameRealTime(GoogleApiClient googleApiClient, GameUtils.GameEventListener listener) {
        this.googleApiClient = googleApiClient;
        this.listener = listener;
        matchSettings = MatchSettings.getDefaultSettings();
        participants = new ArrayList<>();
        handler = new Handler();
        players = new HashMap<>();
        activatedPlayers = new HashSet<>();
    }

    public void playHand() {

        if (currentRoom == null) {
            return;
        }

        if (listener.getRenderer().getReadyHand().isEmpty()) {
            skipHand();
            return;
        }

        List<Card> hand = listener.getRenderer().playHand();

        listener.setTurn(false);

        lastTurn = new Turn(playerParticipantId, hand, System.currentTimeMillis());
        myTurnNumber++;
        message = null;
        broadcast();
    }

    public void skipHand() {
        Log.i(TAG, "skipHand");

        if (listener.getRenderer().isNewTurn()) {
            listener.toast("Must play a hand");
            return;
        }

        listener.setTurn(false);

        lastTurn = new Turn(playerParticipantId, new ArrayList<Card>(), System.currentTimeMillis());
        myTurnNumber++;
        message = null;
        broadcast();

        listener.toast("Turn skipped");
    }

    private void processTurn(GameData gameData) {

        Log.i(TAG, "processTurn: " + gameData.toJsonString());

        if (gameData.getVersion() != GameData.CURRENT_VERSION) {
            listener.toast("Incompatible versions, unable to load match");
            return;
        }

        Log.d(TAG, "nextPlayer: " + gameData.getNextPlayer());
        Log.d(TAG, "playerParticipantId: " + playerParticipantId);

        listener.loadGameFragment();

        boolean newTurn = false;

        setPlayers(gameData);

        Log.d(TAG, "received playerParticipantId: " + playerParticipantId);
        Log.d(TAG, "received nextPlayer: " + gameData.getNextPlayer());

        if (gameData.isFinished()) {
            listener.setTurn(false);
            setWinners(gameData);
            isPlaying = false;
        }
        else if (gameData.getNextPlayer().equals(playerParticipantId)) {
            listener.setTurn(true);
            listener.vibrate(250);

            Turn lastTurn = gameData.getLastTurn();
            if (lastTurn != null) {
                Log.d(TAG, "lastTurn player: " + lastTurn.getPlayer());
            }

            if ((lastTurn != null && lastTurn.getPlayer().equals(playerParticipantId)) || gameData.getActivePlayers().size() == 0) {
                gameData.setActivePlayers(gameData.getRemainingActivePlayers());
                newTurn = true;
            }
        }
        else {
            listener.setTurn(false);
        }
        gameData.setNewTurn(newTurn);
        listener.getRenderer().nextTurn(gameData, playerParticipantId);
    }

    private boolean isPlaying = false;
    private String playerParticipantId;
    private Room currentRoom;
    private final static int MIN_PLAYERS = 2;

    private boolean shouldStartGame(Room room) {
        int connectedPlayers = 0;
        for (Participant p : room.getParticipants()) {
            if (p.isConnectedToRoom()) ++connectedPlayers;
        }
        return connectedPlayers >= MIN_PLAYERS;
    }

    private boolean shouldCancelGame(Room room) {
        // TODO: Your game-specific cancellation logic here. For example, you might decide to
        // cancel the game if enough people have declined the invitation or left the room.
        // You can check a participant's status with Participant.getStatus().
        // (Also, your UI should have a Cancel button that cancels the game too)

        int numRemaining = 0;
        for (Participant participant : room.getParticipants()) {
            if (participant.getStatus() == Participant.STATUS_JOINED) {
                numRemaining++;
            }
        }

        return numRemaining < 2;
    }

    private void startGame(Room room) {

        isPlaying = true;
        playerParticipantId = room.getParticipantId(Games.Players.getCurrentPlayerId(googleApiClient));


        String firstPlayer = null;
        Card card = null;
        CardEvaluator cardEvaluator = new CardEvaluator(matchSettings);

        gameData = listener.getRenderer().startGame(room.getParticipantIds(),
                matchSettings,
                room.getParticipantId(Games.Players.getCurrentPlayerId(googleApiClient)));

        for (String player : gameData.getActivePlayers()) {

            if (card == null) {
                firstPlayer = player;
                card = gameData.getHand(player)
                        .get(0);
            }
            else {
                Card nextCard = gameData.getHand(player)
                        .get(0);

                if (cardEvaluator.compareCards(nextCard, card) < 0) {
                    card = nextCard;
                    firstPlayer = player;
                }
            }

        }
        gameData.setNextPlayer(firstPlayer);

        setPlayers(gameData);
        listener.getRenderer().setPlayerHand(gameData.getHand(playerParticipantId));

        for (String playerId : gameData.getStartingPlayers()) {
            players.put(playerId, (byte) 0);
        }

//        processTurn(gameData);

        Log.d(TAG, "gameData: " + gameData.toJsonString());

        handler.post(new Runnable() {
            @Override
            public void run() {
                broadcast();
                if (isPlaying) {
                    handler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        });
    }

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // display error
            return;
        }
        listener.loadWaitingRoom(room);
        updateRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // display error
            return;
        }
        listener.loadWaitingRoom(room);
        updateRoom(room);
    }

    @Override
    public void onLeftRoom(int statusCode, String s) {
        isPlaying = false;
        currentRoom = null;
    }

    @Override
    public void onRoomConnected(int statusCode, final Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // display error
            return;
        }
        updateRoom(room);
    }

    private void sendMessageToAll(final Room room, final byte[] message) {

        iterations = 0;

//        RealTimeMultiplayer.ReliableMessageSentCallback callback = new RealTimeMultiplayer.ReliableMessageSentCallback() {
//            @Override
//            public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
//                Log.d(TAG, "Message statusCode: " + statusCode);
//                if (iterations++ < MAX_ITERATIONS && statusCode == GamesStatusCodes.STATUS_REAL_TIME_MESSAGE_SEND_FAILED) {
//                    listener.toast("Message failed");
//                    Games.RealTimeMultiplayer.sendReliableMessage(googleApiClient, this, message,
//                            room.getRoomId(), recipientParticipantId);
//                }
//            }
//        };

        for (Participant participant : room.getParticipants()) {
            sendMessage(message, participant.getParticipantId());
        }
    }

    /**
     * For HOST:
     * 0 = HOST
     * 1 = State
     * 2 = Last turn number
     *
     * For PLAYER:
     * 0 = PLAYER
     * 2 = Last turn number
     *
     * @param realTimeMessage
     */
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {

        processMessage(realTimeMessage.getMessageData());
    }

    private void processMessage(byte[] receivedMessage) {

        byte sender = receivedMessage[0];
        byte state = receivedMessage[1];
        byte turnNumber = receivedMessage[2];

        String stateString = "";
        switch (state) {

            case STARTING:
                stateString = "STARTING";
                break;
            case PLAYING:
                stateString = "PLAYING";
                break;
            case MY_TURN:
                stateString = "MY_TURN";
                break;
            case FINISHED:
                stateString = "FINISHED";
                break;

        }

        Log.d(TAG, "Message received: " + sender + stateString + " " + state);

        if (sender == HOST) {
            switch (state) {

                case STARTING:
                    isPlaying = true;
                    try {
                        gameData = GameData.fromJsonString(new String(Arrays.copyOfRange(receivedMessage, 3, receivedMessage.length)));
                        Log.d(TAG, "Received gameData: " + gameData.toJsonString());
                        setPlayers(gameData);
                        listener.getRenderer().setPlayerHand(gameData.getHand(playerParticipantId));
                        matchSettings = gameData.getSettings();
                        for (String playerId : gameData.getStartingPlayers()) {
                            players.put(playerId, (byte) 0);
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }

                    startBroadcast();
                    break;
                case PLAYING:
                    break;
                case MY_TURN:
                    if (this.state != MY_TURN) {
                        listener.setTurn(true);
                        listener.vibrate(250);
                    }
                    break;
                case FINISHED:
                    break;

            }

            this.state = state;
        }
        else if (sender == PLAYER) {
            if (state != STARTING) {
                try {
                    Turn newTurn = Turn.fromJsonObject(
                            new JSONObject(new String(Arrays.copyOfRange(receivedMessage, 3,
                                    receivedMessage.length))));

                    if (players.get(newTurn.getPlayer()) < turnNumber) {
                        listener.getRenderer()
                                .nextTurn(newTurn);
                        listener.setPlayer(newTurn);
                        players.put(newTurn.getPlayer(), turnNumber);

                        if (currentRoom.getCreatorId().equals(playerParticipantId) && newTurn.getPlayer().equals(gameData.getNextPlayer())) {
                            gameData.playHand(newTurn.getPlayer(), newTurn.getHand());
                        }
                    }

                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else {
                activatedPlayers.add(new String(Arrays.copyOfRange(receivedMessage, 3, receivedMessage.length)));
                Log.d(TAG, "activatedPlayers added: " + new String(Arrays.copyOfRange(receivedMessage, 3, receivedMessage.length)));
            }
            lastTurnNumber = turnNumber;
        }
    }

    private void startBroadcast() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                broadcast();
                if (isPlaying) {
                    handler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        });
    }

    private void broadcast() {
        // TODO: Resend previous data

        if (currentRoom == null) {
            isPlaying = false;
            return;
        }

        if (currentRoom.getCreatorId().equals(playerParticipantId)) {
            Log.d(TAG, "playerParticipantId: " + playerParticipantId);
            Log.d(TAG, "activatedPlayers: " + activatedPlayers);
            Log.d(TAG, "startingPlayers: " + gameData.getStartingPlayers());
            if (activatedPlayers.size() == gameData.getStartingPlayers().size() - 1) {
                byte[] headers = new byte[3];
                headers[0] = HOST;
                headers[1] = MY_TURN;
                headers[2] = lastTurnNumber;

                sendMessage(headers, gameData.getNextPlayer());
                Log.d(TAG, "Sent HOST " + gameData.getNextPlayer() + " " + MY_TURN);
            }
            else {
                List<String> nonActivatedPlayers = new ArrayList<>(gameData.getStartingPlayers());
                nonActivatedPlayers.removeAll(activatedPlayers);
                nonActivatedPlayers.remove(playerParticipantId);
                for (String playerId : nonActivatedPlayers) {
                    byte[] headers = new byte[3];
                    headers[0] = HOST;
                    headers[1] = STARTING;
                    headers[2] = lastTurnNumber;
                    byte[] dataArray = gameData.toJsonString()
                            .getBytes();
                    byte[] messageStarting = Arrays.copyOf(headers, headers.length + dataArray.length);
                    System.arraycopy(dataArray, 0, messageStarting, headers.length, dataArray.length);

                    sendMessage(messageStarting, playerId);
                }
                Log.d(TAG, "Sent HOST " + STARTING);
            }
        }
        else if (state == STARTING) {
            byte[] headers = new byte[3];
            headers[0] = PLAYER;
            headers[1] = STARTING;
            headers[2] = lastTurnNumber;
            byte[] dataArray = playerParticipantId.getBytes();
            byte[] messageStarting = Arrays.copyOf(headers, headers.length + dataArray.length);
            System.arraycopy(dataArray, 0, messageStarting, headers.length, dataArray.length);
            sendMessage(messageStarting, currentRoom.getCreatorId());
            Log.d(TAG, "Sent " + STARTING);
        }

        if (message == null) {
            byte[] headers = new byte[3];
            headers[0] = PLAYER;
            headers[1] = PLAYING;
            headers[2] = myTurnNumber;
            if (lastTurn == null) {
                lastTurn = new Turn(playerParticipantId, new ArrayList<Card>(), System.currentTimeMillis());
            }
            byte[] turnArray = lastTurn.toJsonString().getBytes();
            message = Arrays.copyOf(headers, headers.length + turnArray.length);
            System.arraycopy(turnArray, 0, message, headers.length, turnArray.length);
        }

        sendMessageToAll(currentRoom, message);

    }

    private void sendMessage(byte[] message, String recipient) {
        if (playerParticipantId.equals(recipient)) {
            processMessage(message);
        }
        else {
            Games.RealTimeMultiplayer.sendReliableMessage(googleApiClient, null, message,
                    currentRoom.getRoomId(), currentRoom.getCreatorId());
        }
    }

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onConnectedToRoom(Room room) {

        updateRoom(room);
    }

    @Override
    public void onDisconnectedFromRoom(Room room) {

        updateRoom(room);
    }

    private void updateRoom(Room room) {
        currentRoom = room;
        playerParticipantId = room.getParticipantId(Games.Players.getCurrentPlayerId(googleApiClient));
        participants = room.getParticipants();
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> list) {

        updateRoom(room);
    }

    @Override
    public void onPeerDeclined(Room room, List<String> list) {
        // peer declined invitation -- see if game should be canceled
        if (!isPlaying && shouldCancelGame(room)) {
            Games.RealTimeMultiplayer.leave(googleApiClient, this, room.getRoomId());
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        updateRoom(room);
    }

    @Override
    public void onPeerJoined(Room room, List<String> list) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> list) {
        // peer left -- see if game should be canceled
        if (!isPlaying && shouldCancelGame(room)) {
            Games.RealTimeMultiplayer.leave(googleApiClient, this, room.getRoomId());
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> list) {
        updateRoom(room);
        listener.loadGameFragment();
        if (isPlaying) {
            // add new player to an ongoing game
        }
        else if (playerParticipantId.equals(room.getCreatorId()) && shouldStartGame(room)) {
            startGame(room);
        }

    }

    @Override
    public void onPeersDisconnected(Room room, List<String> list) {
        updateRoom(room);
        if (isPlaying) {
            // do game-specific handling of this -- remove player's avatar
            // from the screen, etc. If not enough players are left for
            // the game to go on, end the game and leave the room.
        }
        else if (shouldCancelGame(room)) {
            // cancel the game
            Games.RealTimeMultiplayer.leave(googleApiClient, this, room.getRoomId());
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onP2PConnected(String s) {

    }

    @Override
    public void onP2PDisconnected(String s) {

    }

    private void setPlayers(GameData gameData) {
        listener.setPlayers(GameUtils.getPlayers(currentRoom), gameData);
    }

    private void setWinners(GameData gameData) {
        listener.setWinners(GameUtils.getWinners(currentRoom, gameData), gameData);
    }

    public void leave() {
        matchSettings = null;
        Games.RealTimeMultiplayer.leave(googleApiClient, this, currentRoom.getRoomId());
        listener.returnToLaunch();
    }

    public void onConnected(Bundle bundle) {

        if (invitation != null) {
            acceptInvite(invitation.getInvitationId());
            invitation = null;
        }
        else if (bundle != null) {
            Object object = bundle.getParcelable(Multiplayer.EXTRA_INVITATION);
            if (object instanceof TurnBasedMatch) {
                return;
            }

            Invitation invitation = (Invitation) object;


            if (invitation != null) {
                // accept invitation
                RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this)
                        .setMessageReceivedListener(this)
                        .setRoomStatusUpdateListener(this)
                        .setMessageReceivedListener(this);
                roomConfigBuilder.setInvitationIdToAccept(invitation.getInvitationId());
                Games.RealTimeMultiplayer.join(googleApiClient, roomConfigBuilder.build());

                // prevent screen from sleeping during handshake
//                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // go to game screen
            }
        }
    }

    public void create(ArrayList<String> invitedPlayers, MatchSettings matchSettings) {

        // create the room and specify a variant if appropriate
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this)
                .setMessageReceivedListener(this);
        roomConfigBuilder.addPlayersToInvite(invitedPlayers);
        final RoomConfig roomConfig = roomConfigBuilder.build();
        this.invitedPlayers = invitedPlayers;
        this.matchSettings = matchSettings;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (googleApiClient.blockingConnect().isSuccess()) {
                    listener.postOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.loadGameFragment();
                            Games.RealTimeMultiplayer.create(googleApiClient, roomConfig);
                        }
                    });
                }
            }
        }).start();
    }

    public void acceptInvite(String id) {
        // TODO: Move to room connection
        listener.loadGameFragment();
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this)
                .setMessageReceivedListener(this);
        roomConfigBuilder.setInvitationIdToAccept(id);
        Games.RealTimeMultiplayer.join(googleApiClient, roomConfigBuilder.build());
    }

    public void setInvitation(Invitation invitation) {
        this.invitation = invitation;
    }

    public void clearMatch() {
        if (currentRoom != null) {
            Games.RealTimeMultiplayer.leave(googleApiClient, this, currentRoom.getRoomId());
            currentRoom = null;
        }
    }
}