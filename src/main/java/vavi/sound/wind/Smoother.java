/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Smoother. a one pole low pass that turns a stepping controller into a continuous one.
 * <p>
 * a wind controller sends breath, bend and glide as 7 bit steps at a few hundred hertz.
 * IFW gives each of them its own smoothing time so that the steps do not become a
 * zipper noise, and so that a patch can decide how sluggish it wants to feel.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public class Smoother {

    /** */
    private final float sampleRate;

    /** */
    private float coefficient = 1;

    /** */
    private float value;

    /** */
    public Smoother(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * @param milliseconds time to cover 63% of a step, 0 to follow immediately
     */
    public void setTime(float milliseconds) {
        coefficient = milliseconds <= 0 ? 1 : (float) -Math.expm1(-1000 / (milliseconds * sampleRate));
    }

    /** Steps one sample towards the target. */
    public float process(float target) {
        value += (target - value) * coefficient;
        return value;
    }

    /** */
    public float value() {
        return value;
    }

    /** Jumps to a value without smoothing. */
    public void reset(float value) {
        this.value = value;
    }
}
