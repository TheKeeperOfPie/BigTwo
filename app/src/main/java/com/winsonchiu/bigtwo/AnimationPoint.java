package com.winsonchiu.bigtwo;


import android.view.animation.Interpolator;

import java.io.Serializable;

/**
 * Created by TheKeeperOfPie on 12/24/2014.
 */
public class AnimationPoint implements Serializable {

    private final float targetX;

    private final float targetY;

    private final float targetAngleY;

    private final float targetAngleZ;

    private final float targetScaleX;

    private final float targetScaleY;

    private final Interpolator interpolator;

    private long startTime;

    private long targetTime;

    private long duration;

    public AnimationPoint(long duration, float x, float y, float angleY, float angleZ, float scaleX,
                          float scaleY, Interpolator interpolator) {
        this.targetX = x;
        this.targetY = y;
        this.targetAngleY = angleY;
        this.targetAngleZ = angleZ;
        this.targetScaleX = scaleX;
        this.targetScaleY = scaleY;
        this.interpolator = interpolator;
        this.duration = duration;
    }

    public void start() {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
            targetTime = startTime + duration;
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public long getTargetTime() {
        return targetTime;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetAngleY() {
        return targetAngleY;
    }

    public float getTargetAngleZ() {
        return targetAngleZ;
    }

    public float getTargetScaleX() {
        return targetScaleX;
    }

    public float getTargetScaleY() {
        return targetScaleY;
    }

    public Interpolator getInterpolator() {
        return interpolator;
    }
}