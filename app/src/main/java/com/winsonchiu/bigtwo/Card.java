package com.winsonchiu.bigtwo;

import android.graphics.PointF;
import android.opengl.GLES20;
import android.util.Log;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by TheKeeperOfPie on 12/18/2014.
 */
public class Card implements Serializable {

    public static final String TAG = Card.class.getCanonicalName();
    public static final int DECK_SIZE = 52;
    public static final int NUM_CARDS = 13;

    public static int[] maxTextureSize = new int[1];

    private static FloatBuffer backUvBuffer;
    private short indices[];
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;
    private FloatBuffer uvBuffer;
    private static float[] matrixProjection = new float[16];
    private static float[] matrixView = new float[16];
    private float[] matrixProjectionAndView = new float[16];
    private float[] transMatrix = new float[16];
    private static float cardWidth;
    private static float cardHeight;
    private static float renderScreenWidth;
    private static float renderScreenHeight;
    private static CustomRenderer.RenderCallback renderCallback;
    private float offsetX;
    private float offsetY;
    private float angleY;
    private float angleZ;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float targetOffsetX;
    private float targetOffsetY;
    private float targetAngleY;
    private float targetAngleZ;
    private float targetScaleX = 1.0f;
    private float targetScaleY = 1.0f;
    private int value;
    private Suit suit;
    private ConcurrentLinkedQueue<AnimationPoint> animationQueue;
    private boolean isAnimating = false;
    private boolean isReady = false;
    private float colorModifier = 1.0f;

    public static void setCardStaticValues(float width, float height,
                                           CustomRenderer.RenderCallback callback) {
        cardWidth = width;
        cardHeight = height;
        renderCallback = callback;
        float[] uvs = new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
        };

        ByteBuffer uvByteBuffer = ByteBuffer.allocateDirect(uvs.length * 4);
        uvByteBuffer.order(ByteOrder.nativeOrder());
        backUvBuffer = uvByteBuffer.asFloatBuffer();
        backUvBuffer.put(uvs);
        backUvBuffer.position(0);
    }

    public Card(Suit suit, int value) {
        this.value = value;
        this.suit = suit;
        animationQueue = new ConcurrentLinkedQueue<>();
        float[] uvs = new float[]{
                1.0f / 13f * value, 1.0f / 4f * suit.getValue(),
                1.0f / 13f * value, 1.0f / 4f * (suit.getValue() + 1),
                1.0f / 13f * (value + 1), 1.0f / 4f * (suit.getValue() + 1),
                1.0f / 13f * (value + 1), 1.0f / 4f * suit.getValue()
        };

        ByteBuffer uvByteBuffer = ByteBuffer.allocateDirect(uvs.length * 4);
        uvByteBuffer.order(ByteOrder.nativeOrder());
        uvBuffer = uvByteBuffer.asFloatBuffer();
        uvBuffer.put(uvs);
        uvBuffer.position(0);

        float[] vertices = new float[]{
                0.0f, cardHeight, -cardWidth,
                0.0f, 0.0f, -cardWidth,
                cardWidth, 0.0f, -cardWidth,
                cardWidth, cardHeight, -cardWidth
        };

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

    public int getValue() {
        return value;
    }

    public int getAdjustedValue() {
        return (value - 2) % 13;
    }

    public Suit getSuit() {
        return suit;
    }

    public float getToggleHeight() {

        isReady = !isReady;
        return getIntendedY();

    }

    public float getIntendedY() {
        return isReady ? cardHeight * 0.75f : -cardHeight * 0.25f;
    }

    public boolean isReady() {
        return isReady;
    }

    public static void setDimensions(float width, float height) {

        renderScreenWidth = width;
        renderScreenHeight = height;

        android.opengl.Matrix.orthoM(matrixProjection,
                0,
                0,
                renderScreenWidth,
                0,
                renderScreenHeight,
                0,
                cardWidth * 2);

        android.opengl.Matrix.setLookAtM(matrixView, 0, 0f, 0f, 2f, 0f, 0f, 1f, 0.0f, 1.0f, 0.0f);

    }

    public void setOffsets(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        targetOffsetX = offsetX;
        targetOffsetY = offsetY;
    }

    public void setColorModifier(float colorModifier) {
        this.colorModifier = colorModifier;
        Log.d(TAG, "set colorModifier: " + this.colorModifier);
    }

    public float getAngleY() {
        return targetAngleY;
    }

    public float getAngleZ() {
        return targetAngleY;
    }

    public void addAnimation(AnimationPoint point) {
        animationQueue.offer(point);
        animationQueue.peek().start();
        isAnimating = true;
    }

    public boolean contains(float x, float y) {

        PointF checkPoint = new PointF(x, y);

        PointF topLeft = new PointF(
                (float) (offsetX - Math.cos(Math.toRadians(90f - angleZ)) * cardHeight),
                (float) (offsetY + Math.sin(Math.toRadians(90f - angleZ)) * cardHeight));
        PointF bottomRight = new PointF(
                (float) (offsetX + Math.cos(Math.toRadians(angleZ)) * cardWidth),
                (float) (offsetY + Math.sin(Math.toRadians(angleZ)) * cardWidth));
        PointF topRight = new PointF(
                (float) (bottomRight.x - Math.sin(Math.toRadians(90f - angleZ) * cardHeight)),
                (float) (bottomRight.y + Math.cos(Math.toRadians(angleZ)) * cardHeight));
        PointF bottomLeft = new PointF(offsetX, offsetY);

        float area1 = getArea(topLeft, topRight, checkPoint);
        float area2 = getArea(topRight, bottomRight, checkPoint);
        float area3 = getArea(bottomRight, bottomLeft, checkPoint);
        float area4 = getArea(bottomLeft, topLeft, checkPoint);

//        Log.i(TAG, "topLeft: (" + topLeft.x + ", " + topLeft.y + ")");
//        Log.i(TAG, "bottomRight: (" + bottomRight.x + ", " + bottomRight.y + ")");
//        Log.i(TAG, "topRight: (" + topRight.x + ", " + topRight.y + ")");
//        Log.i(TAG, "bottomLeft: (" + bottomLeft.x + ", " + bottomLeft.y + ")");
//
//        Log.i(TAG, "area1: " + area1);
//        Log.i(TAG, "area2: " + area2);
//        Log.i(TAG, "area3: " + area3);
//        Log.i(TAG, "area4: " + area4);

        return !((area1 + area2 + area3 + area4) > cardHeight * cardWidth + 10f);

    }

    private float getArea(PointF a, PointF b, PointF c) {

        return Math.abs(((a.x * (b.y - c.y)) + (b.x * (c.y - a.y)) + (c.x * (a.y - b.y))) / 2f);

    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void render(int[] textureNames) {

        if (isAnimating) {
            animate();
        }
        else {
            targetOffsetX = offsetX;
            targetOffsetY = offsetY;
            targetAngleY = angleY;
            targetAngleZ = angleZ;
            targetScaleX = scaleX;
            targetScaleY = scaleY;
        }

        android.opengl.Matrix.setIdentityM(transMatrix, 0);
        android.opengl.Matrix.translateM(transMatrix,
                0,
                targetOffsetX + cardWidth / 2f,
                targetOffsetY + cardHeight / 2f,
                -cardWidth);
        android.opengl.Matrix.rotateM(transMatrix, 0, targetAngleY, 0.0f, 1.0f, 0.0f);
        android.opengl.Matrix.rotateM(transMatrix, 0, targetAngleZ, 0.0f, 0.0f, 1.0f);
        android.opengl.Matrix.scaleM(transMatrix, 0, targetScaleX, targetScaleY, 1.0f);
        android.opengl.Matrix.translateM(transMatrix,
                0,
                -(targetOffsetX + cardWidth / 2f),
                -(targetOffsetY + cardHeight / 2f),
                cardWidth);
        android.opengl.Matrix.translateM(transMatrix, 0, targetOffsetX, targetOffsetY, 0);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(RenderValues.positionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer);

        GLES20.glUniform1f(RenderValues.alphaHandle, 1.0f);

        if (((int) ((targetAngleY + 90) / 180)) % 2 == 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
            GLES20.glVertexAttribPointer(RenderValues.texCoordLoc, 2, GLES20.GL_FLOAT,
                    false,
                    0, uvBuffer);
        }
        else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[1]);
            GLES20.glVertexAttribPointer(RenderValues.texCoordLoc, 2, GLES20.GL_FLOAT,
                    false,
                    0, backUvBuffer);
        }

//        android.opengl.Matrix.multiplyMM(matrixProjectionAndView,
//                0,
//                matrixView,
//                0,
//                transMatrix,
//                0);
//        android.opengl.Matrix.multiplyMM(matrixProjectionAndView,
//                0,
//                matrixProjection,
//                0,
//                transMatrix,
//                0);

        android.opengl.Matrix.multiplyMM(matrixProjectionAndView,
                0,
                matrixProjection,
                0,
                transMatrix,
                0);

        android.opengl.Matrix.multiplyMM(matrixProjectionAndView,
                0,
                matrixProjectionAndView,
                0,
                matrixView,
                0);

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(RenderValues.matrixHandle, 1, false, matrixProjectionAndView, 0);

        // Set the sampler texture unit to 0, where we have saved the texture.
        GLES20.glUniform1i(RenderValues.samplerLoc, 0);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        if (isAnimating && renderCallback != null) {
            renderCallback.requestRender();
        }

    }

    private void animate() {

        if (animationQueue.isEmpty()) {
            isAnimating = false;
            return;
        }

        AnimationPoint point = animationQueue.peek();

        long currentTime = System.currentTimeMillis();
        if (currentTime > point.getTargetTime()) {
            animationQueue.poll();
            offsetX = point.getTargetX();
            offsetY = point.getTargetY();
            angleY = point.getTargetAngleY();
            angleZ = point.getTargetAngleZ();
            scaleX = point.getTargetScaleX();
            scaleY = point.getTargetScaleY();

            if (animationQueue.isEmpty()) {
                isAnimating = false;
            }
            else {
                animationQueue.peek().start();
            }
        }

        float interpolation = point.getInterpolator().getInterpolation(
                1.0f - ((point.getTargetTime() - currentTime) / (float) (point.getTargetTime()
                                                                         - point.getStartTime())));

        targetOffsetX = offsetX + (point.getTargetX() - offsetX) * interpolation;
        targetOffsetY = offsetY + (point.getTargetY() - offsetY) * interpolation;
        targetAngleY = angleY + (point.getTargetAngleY() - angleY) * interpolation;
        targetAngleZ = angleZ + (point.getTargetAngleZ() - angleZ) * interpolation;
        targetScaleX = scaleX + (point.getTargetScaleX() - scaleX) * interpolation;
        targetScaleY = scaleY + (point.getTargetScaleY() - scaleY) * interpolation;

    }

    public static Card fromString(String string) {
        return new Card(Suit.getSuitFromCharacter(string.charAt(0)),
                Integer.parseInt(string.substring(1)));
    }

    @Override
    public String toString() {
        return suit.toString() + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Card)) {
            return false;
        }

        Card card = (Card) o;

        return value == card.value && suit == card.suit;

    }

    @Override
    public int hashCode() {
        int result = value;
        result = 31 * result + suit.hashCode();
        return result;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public String getReadableString() {

        return suit.toString() + getReadableValue(value);

    }

    public static String getReadableValue(int value) {

        switch (value) {
            case 0:
                return "A";
            case 10:
                return "J";
            case 11:
                return "Q";
            case 12:
                return "K";
            default:
                return String.valueOf(value + 1);
        }
    }

}
