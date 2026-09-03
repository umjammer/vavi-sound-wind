/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.util.Arrays;


/**
 * Phaser. IFW filter 3.
 * <p>
 * not a cutoff filter at all but a stack of 2 to 24 all pass sections with feedback,
 * mixed back into the dry signal. a positive mix gives the peaks and a negative mix the
 * notches, and a wind patch normally leaves it at a fixed frequency to colour the tone
 * rather than sweeping it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public class Phaser {

    /** the most sections the IFW panel offers */
    private static final int MAX_STAGES = 24;

    /** */
    private final float sampleRate;

    /** one state per all pass section */
    private final float[] states = new float[MAX_STAGES];

    /** */
    private float feedbackState;

    /** */
    private int stages = 2;

    /** the all pass coefficient of the current frequency */
    private float coefficient;

    /** */
    private float feedback;

    /** -1 .. 1 */
    private float mix;

    /** */
    public Phaser(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /** */
    public void reset() {
        Arrays.fill(states, 0);
        feedbackState = 0;
    }

    /**
     * @param stages the stage knob, 2 .. 24
     * @param frequency the frequency knob, 0 .. 10
     * @param resonance the resonance knob, -10 .. 10, the feedback around the stack
     * @param mix the mix knob, -10 .. 10
     */
    public void set(float stages, float frequency, float resonance, float mix) {
        this.stages = Math.min(MAX_STAGES, Math.max(1, Math.round(stages)));
        float hz = Math.min(sampleRate * .45f, Filter.hertz(frequency));
        float t = (float) Math.tan(Math.PI * hz / sampleRate);
        this.coefficient = (t - 1) / (t + 1);
        this.feedback = Math.max(-.95f, Math.min(.95f, resonance / 10 * .95f));
        this.mix = Math.max(-1, Math.min(1, mix / 10));
    }

    /** */
    public float process(float in) {
        if (mix == 0) {
            return in;
        }
        float x = in + feedbackState * feedback;
        for (int i = 0; i < stages; i++) {
            float y = coefficient * x + states[i];
            states[i] = x - coefficient * y;
            x = y;
        }
        feedbackState = x;
        return in + mix * x;
    }
}
