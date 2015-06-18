package com.winsonchiu.bigtwo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import com.winsonchiu.bigtwo.logic.CardEvaluator;
import com.winsonchiu.bigtwo.logic.GameData;
import com.winsonchiu.bigtwo.logic.Turn;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CustomRenderer implements Renderer {

    private static final String TAG = CustomRenderer.class.getCanonicalName();
    private static final long SPREAD_ANIMATION_TIME = 350;
    private static final long READY_ANIMATION_TIME = 200;
    private static final long HIDE_ANIMATION_TIME = 200;
    private static final long SHOW_PLAYED_CARDS_ANIMATION_TIME = 350;
    private static final float COLOR_MODIFIER = 0.4f;
    private static final float CARD_WIDTH_MODIFIER = 0.2f;
    private static float cardWidth;
    private static float cardHeight;
    private final GestureDetector gestureDetector;
    private int cardsInRow = 1;
    private float screenWidth;
    private float screenHeight;
    private Context context;
    private int[] textureNames;
    private List<Card> playerHand;
    private List<Card> lastHand;
    private List<Card> playedCards;
    private GameData latestGameData;
    private float playerIconHeight;
    private float pileOffsetX;
    private float pileOffsetY;
    private RenderCallback callback;
    private boolean isHidden;
    private int[] backgroundNames;
    private boolean backgroundLoaded;
    private static float[] matrixProjection;
    private static float[] matrixView;
    private float[] matrixProjectionAndView;
    private FloatBuffer uvBuffer;
    private FloatBuffer vertexBuffer;
    private short[] indices;
    private ShortBuffer drawListBuffer;
    private boolean showPlayedCards;
    private boolean newTurn;
    private boolean changeCard = true;
    private boolean changeBackground = true;
    private int marginDp;
    private boolean newScroll;

    public CustomRenderer(Activity activity, final RenderCallback callback) {
        this.context = activity;
        marginDp = activity.getResources()
                .getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        cardWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 140f * (2.5f / 3.5f),
                context.getResources()
                        .getDisplayMetrics());
        cardHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 140f,
                context.getResources()
                        .getDisplayMetrics());
        Card.setCardStaticValues(cardWidth, cardHeight, callback);
        this.callback = callback;
        playerHand = new ArrayList<>();
        lastHand = new ArrayList<>();
        playedCards = new ArrayList<>();
        latestGameData = new GameData();
        textureNames = new int[2];
        backgroundNames = new int[1];
        matrixProjection = new float[16];
        matrixView = new float[16];
        matrixProjectionAndView = new float[16];
        gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                newScroll = true;
                return super.onDown(e);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return onTap(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {

                if (!isEventOnCard(e)) {
                    callback.fetchImage();
                    return true;
                }

                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1,
                                    MotionEvent e2,
                                    float distanceX,
                                    float distanceY) {

                if (e1.getY() > screenHeight * 3 / 4 && e2.getY() > screenHeight * 3 / 4) {
                    translateHandX(-distanceX);
                    return true;
                }
                if (newScroll) {
                    newScroll = !onSwipeGesture(e1, e2);
                    return !newScroll;
                }

                return false;
            }
        });
    }

    private boolean onSwipeGesture(MotionEvent e1, MotionEvent e2) {

        Log.d(TAG, " onSwipeGesture: " + e1.getX() + " : " + e2.getX());

        if (e1.getY() - e2.getY() > screenHeight / 4) {
            if (isHidden) {
                toggleHand();
            }
            else if (callback.isTurn()) {
                callback.onPlayHand();
            }
        }
        else if (e1.getY() < screenHeight * 9 / 10 && e2.getY() > screenHeight * 19 / 20) {
            toggleHand();
        }
        else {
            return false;
        }

        return true;

    }

    @Override
    public void onDrawFrame(GL10 gl) {

        if (changeCard) {
            onChangeCard();
            changeCard = false;
        }

        if (changeBackground) {
            onChangeBackground();
            changeBackground = false;
        }

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glEnableVertexAttribArray(RenderValues.positionHandle);
        GLES20.glEnableVertexAttribArray(RenderValues.texCoordLoc);
        GLES20.glUniform1f(RenderValues.colorModifierHandle, 1.0f);

        if (backgroundLoaded) {
            renderBackground();
        }

        render();

        GLES20.glDisableVertexAttribArray(RenderValues.positionHandle);
        GLES20.glDisableVertexAttribArray(RenderValues.texCoordLoc);
    }

    public GameData startGame(ArrayList<String> participantIds, MatchSettings matchSettings, String playerId) {

        playedCards = new ArrayList<>();
        lastHand = new ArrayList<>();
        playerHand = new ArrayList<>();

        latestGameData = new GameData();
        latestGameData.setFirstTurn(true);
        latestGameData.setStartingPlayers(new ArrayList<>(participantIds));
        latestGameData.setActivePlayers(new ArrayList<>(participantIds));
        latestGameData.setSettings(matchSettings);

        dealHands();

        playerHand = latestGameData.getHand(playerId);
        arrangeHand();

        callback.requestRender();

        return latestGameData;
    }

    private void dealHands() {

        ArrayList<String> players = latestGameData.getRemainingPlayers();

        ArrayList<Card> deck = getDeck();
        Collections.shuffle(deck);

        MatchSettings matchSettings = latestGameData.getSettings();
        final List<Integer> cardOrder = matchSettings.getCardOrder();
        final List<Suit> suits = matchSettings.getSuitOrder();

        int numCards = matchSettings.getNumCards();

        latestGameData.clearHands();

        int index;
        for (index = 0; index < players.size(); index++) {

            ArrayList<Card> cards = new ArrayList<>(
                    deck.subList(index * numCards, index * numCards + numCards));
            Collections.sort(cards, new Comparator<Card>() {
                @Override
                public int compare(Card lhs, Card rhs) {

                    int lhsIndex = cardOrder.indexOf(lhs.getValue());
                    int rhsIndex = cardOrder.indexOf(rhs.getValue());

                    if (lhsIndex - rhsIndex == 0) {
                        int firstIndex = suits.indexOf(lhs.getSuit());
                        int secondIndex = suits.indexOf(rhs.getSuit());
                        return firstIndex - secondIndex;
                    }

                    return lhsIndex - rhsIndex;
                }
            });

            latestGameData.setHand(players.get(index), cards);
        }

        if (players.size() == 2) {
            latestGameData.setPlayedCards(new ArrayList<Card>());
        }
        else {
            latestGameData
                    .setPlayedCards(new ArrayList<>(deck.subList(index * numCards, deck.size())));
        }

    }

    private ArrayList<Card> getDeck() {

        ArrayList<Card> cards = new ArrayList<>();

        for (int index = 0; index < latestGameData.getSettings().getNumDecks(); index++) {
            for (Suit suit : Suit.values()) {
                for (int value = 0; value < 13; value++) {
                    cards.add(new Card(suit, value));
                }
            }
        }

        return cards;

    }

    public void setPlayerHand(List<Card> cards) {
        playerHand = cards;
        arrangeHand();
        callback.requestRender();
    }

    public void nextTurn(Turn turn) {
        ArrayList<Card> newPlayedCards = new ArrayList<>(playedCards);
        newPlayedCards.addAll(lastHand);
        lastHand = turn.getHand();
        arrangePlayedCards();
        arrangeLastHand();
        callback.requestRender();
    }

    public void nextTurn(GameData data, String playerId) {

        this.newTurn = data.isNewTurn();
        latestGameData = data;

        if (!data.getLastHand().equals(lastHand) || newTurn) {
            ArrayList<Card> newPlayedCards = new ArrayList<>(playedCards);
            newPlayedCards.addAll(lastHand);
            playedCards = newPlayedCards;
            lastHand = newTurn ? new ArrayList<Card>() : data.getLastHand();
            for (Card card : lastHand) {
                card.setOffsets((screenWidth + cardWidth) / 2, screenHeight);
            }
        }

        if (playedCards.isEmpty()) {
            playedCards = latestGameData.getPlayedCards(newTurn);
            for (Card card : playedCards) {
                card.setOffsets(pileOffsetX, pileOffsetY);
            }
        }

        List<Card> newHand = data.getHand(playerId);

        if (playerHand.isEmpty() || !newHand.containsAll(new ArrayList<>(playerHand))) {
            playerHand = newHand;
            arrangeHand();
            isHidden = false;
        }
        arrangePlayedCards();
        arrangeLastHand();

        callback.requestRender();
    }

    public ArrayList<Card> getReadyHand() {

        ArrayList<Card> hand = new ArrayList<>();
        for (Card card : playerHand) {
            if (card.isReady()) {
                hand.add(card);
            }
        }
        return hand;
    }

    public List<Card> playHand() {
        ArrayList<Card> hand = getReadyHand();
        final CardEvaluator cardEvaluator = new CardEvaluator(latestGameData.getSettings());

        Collections.sort(hand, new Comparator<Card>() {
            @Override
            public int compare(Card lhs, Card rhs) {
                // Creater comparator inside CardEvaluator
                return cardEvaluator.compareCards(lhs, rhs);
            }
        });

        if (!cardEvaluator.isHandValid(hand, lastHand)) {
            callback.toast("Hand is not valid");
            return null;
        }

        ArrayList<Card> newCards = new ArrayList<>(playerHand);
        ArrayList<Integer> removeIndexes = new ArrayList<>();
        for (int index = playerHand.size() - 1; index >= 0; index--) {
            if (playerHand.get(index).isReady()) {
                removeIndexes.add(index);
            }
        }
        for (int index : removeIndexes) {
            newCards.remove(index);
        }

        ArrayList<Card> newPlayedCards = new ArrayList<>(playedCards);
        newPlayedCards.addAll(lastHand);
        playedCards = newPlayedCards;
        playerHand = newCards;
        lastHand = hand;

        arrangePlayedCards();
        arrangeLastHand();
        arrangeHand();

        callback.requestRender();
        return hand;
    }

    public GameData playHand(String playerId) {

        ArrayList<Card> hand = getReadyHand();
        final CardEvaluator cardEvaluator = new CardEvaluator(latestGameData.getSettings());

        Collections.sort(hand, new Comparator<Card>() {
            @Override
            public int compare(Card lhs, Card rhs) {
                // Creater comparator inside CardEvaluator
                return cardEvaluator.compareCards(lhs, rhs);
            }
        });

        if (latestGameData.isFirstTurn() && !playerHand.isEmpty()) {

            Card lowestCard = playerHand.get(0);

            for (Card card : playerHand) {
                if (cardEvaluator.compareCards(card, lowestCard) < 0) {
                    lowestCard = card;
                }
            }

            if (!hand.contains(lowestCard)) {
                callback.toast("Must play lowest card on first turn");
                return null;
            }
        }

        if (!cardEvaluator.isHandValid(hand, lastHand)) {
            callback.toast("Hand is not valid");
            return null;
        }

        latestGameData.setFirstTurn(false);

        ArrayList<Card> newCards = new ArrayList<>(playerHand);
        ArrayList<Integer> removeIndexes = new ArrayList<>();
        for (int index = playerHand.size() - 1; index >= 0; index--) {
            if (playerHand.get(index).isReady()) {
                removeIndexes.add(index);
            }
        }
        for (int index : removeIndexes) {
            newCards.remove(index);
        }

        latestGameData.playHand(playerId, hand);

        ArrayList<Card> newPlayedCards = new ArrayList<>(playedCards);
        newPlayedCards.addAll(lastHand);
        playedCards = newPlayedCards;
        playerHand = newCards;
        lastHand = hand;

        arrangePlayedCards();
        arrangeLastHand();
        arrangeHand();

        callback.requestRender();

        return latestGameData;
    }

    public GameData getLatestGameData() {
        return latestGameData;
    }

    private void arrangePlayedCards() {

        if (showPlayedCards) {
            if (playedCards.isEmpty()) {
                onTap(null);
                return;
            }

            int numRows = (playedCards.size() - 1) / cardsInRow + 1;
            float distanceX = cardWidth;
            float distanceY = cardHeight;

            float maximumWidth = screenWidth > screenHeight ? screenWidth * 0.75f : screenWidth;
            float maximumHeight = screenHeight - playerIconHeight;

            if (cardsInRow * cardWidth > maximumWidth) {
                if (cardsInRow * CARD_WIDTH_MODIFIER * cardWidth <= maximumWidth) {
                    distanceX = CARD_WIDTH_MODIFIER * cardWidth;
                }
                else {
                    distanceX = maximumWidth / cardsInRow;
                }
            }

            if (numRows * cardHeight > maximumHeight) {
                distanceY = (maximumHeight - cardHeight) / numRows;
            }

            float offsetX = (maximumWidth - cardWidth - distanceX * (cardsInRow - 1)) / 2;
            float offsetY = (maximumHeight - cardHeight - distanceY * (numRows - 1)) / 2;

            Log.d(TAG, "numRows: " + numRows);
            Log.d(TAG, "distanceX: " + distanceX);
            Log.d(TAG, "distanceY: " + distanceY);
            Log.i(TAG, "offsetX: " + offsetX);
            Log.i(TAG, "offsetY: " + offsetY);

            for (int index = 0; index < playedCards.size(); index++) {

                int positionX = index % cardsInRow;
                int positionY = numRows - 1 - (index / cardsInRow);

                Card card = playedCards.get(index);
                card.addAnimation(new AnimationPoint(SHOW_PLAYED_CARDS_ANIMATION_TIME,
                        offsetX + distanceX * positionX,
                        offsetY + distanceY * positionY, 0.0f, 0.0f,
                        1.0f, 1.0f, new DecelerateInterpolator()));

                if (positionX == 0) {
                    Log.i(TAG,
                            "Index: " + index +
                            " X: " + (distanceX * positionX) +
                            " Y: " + (distanceY * positionY));
                }
            }
        }
        else {
            for (Card card : playedCards) {
                card.addAnimation(
                        new AnimationPoint(SPREAD_ANIMATION_TIME, pileOffsetX, pileOffsetY, 180.0f,
                                0.0f, 0.6f, 0.6f, new DecelerateInterpolator()));
            }
        }

    }

    private void arrangeHand() {

        float maximumWidth = screenWidth > screenHeight ? screenWidth * 0.75f : screenWidth;
        float distance;
        float offsetX;

        int numCards = playerHand.size();

        if (numCards * cardWidth < maximumWidth) {
            distance = cardWidth;
            offsetX = (maximumWidth - numCards * cardWidth) / 2;
        }
        else if (CARD_WIDTH_MODIFIER * cardWidth * (numCards - 1) + cardWidth < maximumWidth) {
            distance = (maximumWidth - marginDp * 2 - cardWidth) / (numCards - 1);
            offsetX = marginDp;
        }
        else {
            distance = CARD_WIDTH_MODIFIER * cardWidth;
            offsetX = playerHand.isEmpty() ? marginDp : playerHand.get(0).getOffsetX();
        }

        for (int index = 0; index < playerHand.size(); index++) {
            Card card = playerHand.get(index);
            card.addAnimation(new AnimationPoint(SPREAD_ANIMATION_TIME, offsetX + distance * index,
                    card.getIntendedY(), 0.0f, 0.0f, 1.0f, 1.0f,
                    new DecelerateInterpolator()));
        }

    }

    private void arrangeLastHand() {

        float maximumWidth = screenWidth > screenHeight ? screenWidth * 0.75f : screenWidth;
        float distance;
        float offsetX = marginDp;

        int numCards = lastHand.size();

        if (numCards * cardWidth < maximumWidth) {
            distance = cardWidth;
            offsetX = (maximumWidth - numCards * cardWidth) / 2;
        }
        else if (CARD_WIDTH_MODIFIER * cardWidth * (numCards - 1) + cardWidth < maximumWidth) {
            distance = (maximumWidth - marginDp * 2 - cardWidth) / (numCards - 1);
        }
        else {
            distance = (maximumWidth - cardWidth) / lastHand.size();
            offsetX = (maximumWidth - cardWidth - distance * (numCards - 1)) / 2;
        }

        float offsetY = screenWidth > screenHeight ?
                (screenHeight + cardHeight * 0.75f - playerIconHeight) / 2 - cardWidth / 2
                : (cardHeight + pileOffsetY - (cardHeight * 0.8f)) / 2;

        for (int index = 0; index < lastHand.size(); index++) {
            Card card = lastHand.get(index);
            card.addAnimation(
                    new AnimationPoint(SPREAD_ANIMATION_TIME, offsetX + distance * index, offsetY,
                            0.0f, 0.0f, 0.8f, 0.8f, new DecelerateInterpolator()));
        }

    }

    public void translateHandX(float translateX) {
        if (playerHand.isEmpty()) {
            return;
        }

        if (translateX > 0) {
            if (playerHand.get(0).getOffsetX() > screenWidth / 2) {
                return;
            }
        }
        else if (translateX < 0) {
            if (playerHand.get(playerHand.size() - 1).getOffsetX() < screenWidth / 2) {
                return;
            }
        }

        for (Card card : playerHand) {
            card.setOffsets(card.getOffsetX() + translateX, card.getOffsetY());
        }
        callback.requestRender();
    }

    private void renderBackground() {

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(RenderValues.positionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundNames[0]);
        GLES20.glVertexAttribPointer(RenderValues.texCoordLoc, 2, GLES20.GL_FLOAT,
                false,
                0, uvBuffer);

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(RenderValues.matrixHandle, 1, false, matrixProjectionAndView, 0);

        // Set the sampler texture unit to 0, where we have saved the texture.
        GLES20.glUniform1i(RenderValues.samplerLoc, 0);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        callback.requestRender();
    }

    private void render() {

        for (Card card : lastHand) {
            card.render(textureNames);
        }

        GLES20.glUniform1f(RenderValues.colorModifierHandle,
                callback.isTurn() ? 1.0f : COLOR_MODIFIER);

        for (Card card : playerHand) {
            card.render(textureNames);
        }

        GLES20.glUniform1f(RenderValues.colorModifierHandle, 1.0f);

        for (Card card : playedCards) {
            card.render(textureNames);
        }

    }

    public boolean isEventOnCard(MotionEvent event) {

        /*
            Converts Android touch point, origin from top left, to OpenGL touch point, origin
            at bottom left
         */

        float pointX = event.getX();
        float pointY = screenHeight - event.getY();

        for (Card card : playerHand) {
            if (card.contains(pointX, pointY)) {
                return true;
            }
        }

        for (Card card : lastHand) {
            if (card.contains(pointX, pointY)) {
                return true;
            }
        }

        for (Card card : playedCards) {
            if (card.contains(pointX, pointY)) {
                return true;
            }
        }

        return false;

    }

    public boolean onTap(MotionEvent event) {

        if (latestGameData == null) {
            return false;
        }

        if (showPlayedCards) {
            showPlayedCards = false;
            DecelerateInterpolator interpolator = new DecelerateInterpolator();
            for (Card card : lastHand) {
                card.addAnimation(new AnimationPoint(SPREAD_ANIMATION_TIME, card.getOffsetX(), card.getOffsetY(), 0.0f, 0.0f, card.getScaleX(), card.getScaleY(), interpolator));
            }
            arrangePlayedCards();
            if (!isHidden) {
                isHidden = true;
                toggleHand();
            }
            callback.requestRender();
            return true;
        }

        /*
            Converts Android touch point, origin from top left, to OpenGL touch point, origin
            at bottom left
         */
        float pointX = event.getX();
        float pointY = screenHeight - event.getY();

        if (!playedCards.isEmpty() && playedCards.get(0).contains(pointX, pointY)) {

            ArrayList<Card> cards = latestGameData.getPlayedCards(newTurn);
            for (Card card : cards) {
                card.setOffsets(pileOffsetX, pileOffsetY);
            }

            playedCards = cards;

            showPlayedCards = true;
            DecelerateInterpolator interpolator = new DecelerateInterpolator();
            for (Card card : lastHand) {
                card.addAnimation(new AnimationPoint(SPREAD_ANIMATION_TIME, card.getOffsetX(), card.getOffsetY(), 180.0f, 0.0f, card.getScaleX(), card.getScaleY(), interpolator));
            }

            arrangePlayedCards();
            if (!isHidden) {
                for (Card card : playerHand) {
                    card.addAnimation(new AnimationPoint(SPREAD_ANIMATION_TIME, card.getOffsetX(),
                            -cardHeight, card.getAngleY(),
                            card.getAngleZ(), 1.0f, 1.0f,
                            interpolator));
                }
            }
            callback.requestRender();
            return true;
        }

        int moveIndex = -1;

        for (int index = 0; index < playerHand.size(); index++) {

            Card card = playerHand.get(index);
            Card nextCard = playerHand.get((index + 1) % playerHand.size());

            if (card.contains(pointX, pointY) && !nextCard.contains(pointX, pointY)
                || playerHand.size() == 1) {
                moveIndex = index;
            }
        }
        if (moveIndex >= 0) {
            Card card = playerHand.get(moveIndex);

            card.addAnimation(new AnimationPoint(READY_ANIMATION_TIME, card.getOffsetX(),
                    card.getToggleHeight(), 0.0f, 0.0f, 1.0f, 1.0f,
                    new DecelerateInterpolator()));
        }

        callback.requestRender();

        return moveIndex >= 0;

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        // We need to know the current width and height.
        screenWidth = width;
        screenHeight = height;

        Card.setDimensions(width, height);

        float areaWidth = screenWidth > screenHeight ? screenWidth * 0.25f : screenWidth;

        playerIconHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64,
                context.getResources().getDisplayMetrics());
        pileOffsetY = screenWidth > screenHeight ? screenHeight / 2
                : screenHeight - cardHeight - playerIconHeight;
        pileOffsetX = (areaWidth - cardWidth) / 2;

        if (screenWidth > screenHeight) {
            pileOffsetX += screenWidth * 0.75f;
        }

        cardsInRow = (int) ((areaWidth - cardWidth - marginDp * 2) / (cardWidth * 0.2f));
        if (cardsInRow < 1) {
            cardsInRow = 1;
        }


        // Redo the Viewport, making it fullscreen.
        GLES20.glViewport(0, 0, (int) screenWidth, (int) screenHeight);

        for (Card card : playedCards) {
            card.setOffsets(pileOffsetX, pileOffsetY);
        }

        arrangePlayedCards();
        arrangeHand();
        arrangeLastHand();

        android.opengl.Matrix.orthoM(matrixProjection,
                0,
                0,
                screenWidth,
                0,
                screenHeight,
                0,
                cardWidth * 2);

        android.opengl.Matrix.setLookAtM(matrixView, 0, 0f, 0f, 2f, 0f, 0f, 1f, 0.0f, 1.0f, 0.0f);

        android.opengl.Matrix.multiplyMM(matrixProjectionAndView,
                0,
                matrixProjection,
                0,
                matrixView,
                0);

        float[] uvs = new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };

        ByteBuffer uvByteBuffer = ByteBuffer.allocateDirect(uvs.length * 4);
        uvByteBuffer.order(ByteOrder.nativeOrder());
        uvBuffer = uvByteBuffer.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        float[] vertices = new float[]{
                0.0f, screenHeight, -cardWidth,
                0.0f, 0.0f, -cardWidth,
                screenWidth, 0.0f, -cardWidth,
                screenWidth, screenHeight, -cardWidth
        };

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        indices = new short[]{0, 1, 2, 0, 2, 3};

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);


    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        loadTextures();

        GLES20.glClearColor(0.188f, 0.188f, 0.188f, 1f);

        int vertexShader = RenderValues
                .loadShader(GLES20.GL_VERTEX_SHADER, RenderValues.vertexShaderImage);
        int fragmentShader = RenderValues
                .loadShader(GLES20.GL_FRAGMENT_SHADER, RenderValues.fragmentShaderImage);

        RenderValues.program = GLES20.glCreateProgram();
        GLES20.glAttachShader(RenderValues.program, vertexShader);
        GLES20.glAttachShader(RenderValues.program, fragmentShader);
        GLES20.glLinkProgram(RenderValues.program);

        // Set our shader program
        GLES20.glUseProgram(RenderValues.program);
        RenderValues.setupRenderValues();

    }

    public void loadTextures() {

        Log.i(TAG, "Loading textures...");

        GLES20.glGenTextures(2, textureNames, 0);

        int[] maxTextureSize = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);

        BitmapFactory.Options cardOptions = new BitmapFactory.Options();
        cardOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), R.drawable.cards, cardOptions);
        cardOptions.inJustDecodeBounds = false;

        int longestLength = cardOptions.outWidth > cardOptions.outHeight ? cardOptions.outWidth
                : cardOptions.outHeight;
        int sampleSize = 1;

        while (longestLength / sampleSize > maxTextureSize[0]) {
            sampleSize *= 2;
        }

        Bitmap cardTexture = BitmapFactory
                .decodeResource(context.getResources(), R.drawable.cards, cardOptions);

        Log.d(TAG, "cardTexture width: " + cardTexture.getWidth());
        Log.d(TAG, "cardTexture height: " + cardTexture.getHeight());

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, cardTexture, 0);

        cardTexture.recycle();
    }

    public void release() {
        GLES20.glDeleteTextures(textureNames.length, textureNames, 0);
        if (backgroundLoaded) {
            GLES20.glDeleteTextures(backgroundNames.length, backgroundNames, 0);
        }
    }

    public void toggleHand() {

        DecelerateInterpolator interpolator = new DecelerateInterpolator();

        if (isHidden) {
            for (Card card : playerHand) {
                card.addAnimation(new AnimationPoint(HIDE_ANIMATION_TIME, card.getOffsetX(),
                        card.getIntendedY(), card.getAngleY(),
                        card.getAngleZ(), 1.0f, 1.0f, interpolator));
            }
        }
        else {
            for (Card card : playerHand) {
                card.addAnimation(
                        new AnimationPoint(HIDE_ANIMATION_TIME, card.getOffsetX(), -cardHeight,
                                card.getAngleY(), card.getAngleZ(), 1.0f, 1.0f,
                                interpolator));
            }
        }

        isHidden = !isHidden;

        callback.requestRender();

    }

    public void setChangeBackground(boolean changeBackground) {
        this.changeBackground = changeBackground;
    }

    public void setChangeCard(boolean changeCard) {
        this.changeCard = changeCard;
    }

    public void onChangeBackground() {

        final File image = AppSettings.getBackgroundImage();

        if (!AppSettings.useBackgroundImage() || !image.exists()) {
            if (backgroundLoaded) {
                GLES20.glDeleteTextures(1, backgroundNames, 0);
            }
            backgroundLoaded = false;
            return;
        }

        loadBackground(image);

    }

    public void onChangeCard() {

        final File image = AppSettings.getCardImage();

        if (!AppSettings.useCardImage() || !image.exists()) {
            loadDefaultCardBack();
            return;
        }

        loadCard(image);

    }

    public void loadDefaultCardBack() {

        BitmapFactory.Options backOptions = new BitmapFactory.Options();
        backOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), R.drawable.back, backOptions);
        backOptions.inJustDecodeBounds = false;

        int[] maxTextureSize = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);

        int longestLength = backOptions.outWidth > backOptions.outHeight ? backOptions.outWidth
                : backOptions.outHeight;
        int sampleSize = 1;

        while (longestLength / sampleSize > cardHeight) {
            sampleSize *= 2;
        }

        Bitmap backTexture = ThumbnailUtils.extractThumbnail(BitmapFactory
                .decodeResource(context.getResources(), R.drawable.back, backOptions), (int) cardWidth, (int) cardHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

        GLES20.glDeleteTextures(1, textureNames, 1);
        GLES20.glGenTextures(1, textureNames, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[1]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, backTexture, 0);

        backTexture.recycle();
        callback.requestRender();
    }

    public void loadCard(File imageFile) {

        if (!imageFile.exists()) {
            callback.toast("Error loading card image");
            return;
        }

        try {
            BitmapFactory.Options backOptions = new BitmapFactory.Options();
            backOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), backOptions);
            backOptions.inJustDecodeBounds = false;

            int[] maxTextureSize = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);

            int longestLength = backOptions.outWidth > backOptions.outHeight ? backOptions.outWidth
                    : backOptions.outHeight;
            int sampleSize = 1;

            while (longestLength / sampleSize > cardHeight) {
                sampleSize *= 2;
            }

            Bitmap image = ThumbnailUtils.extractThumbnail(
                    BitmapFactory.decodeFile(imageFile.getAbsolutePath(), backOptions),
                    (int) cardWidth, (int) cardHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

            GLES20.glDeleteTextures(1, textureNames, 1);
            GLES20.glGenTextures(1, textureNames, 1);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[1]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0);

            image.recycle();

            Log.i(TAG, "Card image loaded: " + imageFile.getAbsolutePath());

            callback.requestRender();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            callback.toast("Error loading card image");
        }
    }

    public void loadBackground(File imageFile) {

        if (!imageFile.exists()) {
            callback.toast("Error loading background");
            return;
        }

        try {
            Bitmap backgroundImage = ThumbnailUtils
                    .extractThumbnail(BitmapFactory.decodeFile(imageFile.getAbsolutePath()),
                            (int) screenWidth, (int) screenHeight,
                            ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

            if (backgroundLoaded) {
                GLES20.glDeleteTextures(1, backgroundNames, 0);
            }
            GLES20.glGenTextures(1, backgroundNames, 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundNames[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, backgroundImage, 0);

            backgroundImage.recycle();

            backgroundLoaded = true;

            Log.i(TAG, "Background loaded");

            callback.requestRender();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            callback.toast("Error loading background");
        }
    }

    public boolean isNewTurn() {
        return newTurn;
    }

    public void onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
    }

    public void clear() {
        latestGameData = null;
        playedCards = new ArrayList<>();
        lastHand = new ArrayList<>();
        playerHand = new ArrayList<>();
        callback.requestRender();
    }

    public interface RenderCallback {

        void requestRender();
        void toast(String text);
        boolean isTurn();
        void onPlayHand();
        void fetchImage();
    }

}