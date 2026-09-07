/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.util.Arrays;


/**
 * Exciter. what IFW puts between its EQ and its enhancer.
 * <p>
 * it makes no harmonics of its own. it holds the signal back by up to twenty one samples, and
 * then adds the difference between what is arriving now and what arrived then, together with
 * that same difference one delay ago. that is a comb, and moving the delay moves where it
 * bites, so the frequency knob picks the band it sharpens rather than a corner it works over.
 * a negative mix rounds the same band off instead.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the comb the plug-in really has <br>
 */
public class Exciter {

    /** the longest the delay reaches, rounded up to a power of two */
    private static final int SIZE = 64;

    /** what arrived, and the difference each sample made */
    private final float[] history = new float[SIZE];
    private final float[] difference = new float[SIZE];

    /** */
    private int cursor;

    /** in samples, 1 to 21 */
    private float delay = 1;

    /** -1 .. 1 */
    private float mix;

    /** */
    public void reset() {
        Arrays.fill(history, 0);
        Arrays.fill(difference, 0);
        cursor = 0;
    }

    /**
     * @param frequency the frequency knob, 0 .. 10, the whole of it being the shortest delay
     * @param mix the mix knob, -10 .. 10
     */
    public void set(float frequency, float mix) {
        this.delay = 1 + 20 * (1 - clamp(frequency / 10, 0, 1));
        this.mix = clamp(mix / 10, -1, 1);
    }

    /** */
    public float process(float in) {
        if (mix == 0) {
            history[cursor] = in;
            difference[cursor] = 0;
            cursor = cursor + 1 & SIZE - 1;
            return in;
        }

        float position = cursor - delay;
        while (position < 0) {
            position += SIZE;
        }
        int i = (int) position & SIZE - 1;
        int j = i + 1 & SIZE - 1;
        float f = position - (int) position;

        float delayed = history[i] + (history[j] - history[i]) * f;
        float before = difference[i] + (difference[j] - difference[i]) * f;
        float now = in - delayed;

        history[cursor] = in;
        difference[cursor] = now;
        cursor = cursor + 1 & SIZE - 1;

        return delayed + 2 * mix * (before - now);
    }

    /** */
    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}
