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
 * it is not a cutoff filter at all: it is a stack of twenty four all pass sections with the
 * stack fed back into itself, and the {@code STAGE} knob says how far down the stack the
 * output is taken from. mixing that against the dry signal is what puts the notches in, and
 * two more sections move every notch, which is why a tone that turns it up sounds hollow
 * rather than filtered.
 * <p>
 * the mix is a crossfade rather than an addition, so a mix of 10 is the stack alone and a mix
 * of -10 is the stack alone with its sign turned over.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the stack the plug-in really runs <br>
 */
public class Phaser {

    /** the most sections the IFW panel offers */
    private static final int MAX_STAGES = 24;

    /**
     * what each section has passed on: {@code states[i]} is what came out of the first
     * {@code i} of them, so the stage knob picks one of these to listen to
     */
    private final float[] states = new float[MAX_STAGES + 1];

    /** where the output is taken from, 2 .. 24 */
    private int stages = 2;

    /** the all pass coefficient of the current frequency */
    private float coefficient = 1;

    /** -1 .. 1 */
    private float resonance;

    /** -1 .. 1 */
    private float mix;

    /** whether the tone turned it on at all */
    private boolean enabled = true;

    /** */
    public void reset() {
        Arrays.fill(states, 0);
    }

    /**
     * @param enabled whether the tone has it switched on
     * @param stages the stage knob, 2 .. 24
     * @param frequency the frequency knob, 0 .. 10
     * @param resonance the resonance knob, -10 .. 10, the feedback around the stack
     * @param mix the mix knob, -10 .. 10
     */
    public void set(boolean enabled, float stages, float frequency, float resonance, float mix) {
        this.enabled = enabled;
        this.stages = Math.min(MAX_STAGES, Math.max(1, Math.round(stages)));
        float f = clamp(frequency / 10, 0, 1);
        this.coefficient = 1 - f * f * f * f;
        this.resonance = clamp(resonance / 10, -1, 1);
        this.mix = clamp(mix / 10, -1, 1);
    }

    /** */
    public float process(float in) {
        // the stack is read as it stood a sample ago, then stepped
        float wet = states[stages];
        float out = clamp((1 - Math.abs(mix)) * in + mix * wet, -1, 1);

        float x = in + resonance * wet;
        float previous = states[0];
        for (int i = 0; i < MAX_STAGES; i++) {
            float y = coefficient * (states[i + 1] - x) + previous;
            states[i] = x;
            previous = states[i + 1];
            x = y;
        }
        states[MAX_STAGES] = x;

        return enabled ? out : in;
    }

    /** */
    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}
