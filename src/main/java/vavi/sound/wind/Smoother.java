/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Smoother. what stands between a controller and the thing it moves.
 * <p>
 * a straight line rather than a curve: IFW gives every one of these a time in milliseconds
 * and then covers exactly that much ground in exactly that long, so a smooth time is a
 * duration and not a half life. that is why the gate has one time for opening and another for
 * closing, and why they are quoted in milliseconds on the panel.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano a ramp, which is what the plug-in runs <br>
 */
public class Smoother {

    /** */
    private final float sampleRate;

    /** how long a whole move takes, in seconds */
    private float time;

    /** */
    private float value;

    /** where the ramp is heading, and how far it goes each sample */
    private float target;
    private float step;
    private int remaining;

    /** */
    public Smoother(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * @param milliseconds how long the whole of a move takes, 0 to follow at once
     */
    public void setTime(float milliseconds) {
        this.time = Math.max(0, milliseconds) / 1000;
    }

    /** Steps one sample towards the target. */
    public float process(float target) {
        if (target != this.target) {
            this.target = target;
            int samples = (int) (time * sampleRate);
            if (samples < 1) {
                value = target;
                remaining = 0;
            } else {
                step = (target - value) / samples;
                remaining = samples;
            }
        }
        if (remaining > 0) {
            value += step;
            if (--remaining == 0) {
                value = target;
            }
        }
        return value;
    }

    /** */
    public float value() {
        return value;
    }

    /** Jumps to a value without smoothing. */
    public void reset(float value) {
        this.value = value;
        this.target = value;
        this.remaining = 0;
    }
}
