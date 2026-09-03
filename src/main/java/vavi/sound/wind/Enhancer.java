/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.util.Arrays;


/**
 * Enhancer. the IFW stereo enhancer.
 * <p>
 * a monophonic wind synthesizer is a point in the middle of the image. delaying a copy
 * by a few milliseconds differently per side spreads that point out, by the precedence
 * effect, without detuning anything. a negative mix flips the copy and widens further.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public class Enhancer {

    /** the longest delay the IFW panel offers, in milliseconds */
    private static final float MAX_DELAY = 50;

    /** */
    private final float sampleRate;

    /** */
    private final float[] line;

    /** */
    private int cursor;

    /** in samples */
    private float delayL, delayR;

    /** -1 .. 1 */
    private float mix;

    /** */
    public Enhancer(float sampleRate) {
        this.sampleRate = sampleRate;
        this.line = new float[(int) Math.ceil(MAX_DELAY / 1000 * sampleRate) + 2];
    }

    /** */
    public void reset() {
        Arrays.fill(line, 0);
        cursor = 0;
    }

    /**
     * @param delayL the delay L knob, 0 .. 50 ms
     * @param delayR the delay R knob, 0 .. 50 ms
     * @param mix the mix knob, -10 .. 10
     */
    public void set(float delayL, float delayR, float mix) {
        this.delayL = clamp(delayL);
        this.delayR = clamp(delayR);
        this.mix = Math.max(-1, Math.min(1, mix / 10));
    }

    /** */
    private float clamp(float milliseconds) {
        return Math.max(0, Math.min(MAX_DELAY, milliseconds)) / 1000 * sampleRate;
    }

    /**
     * Steps one sample.
     *
     * @param out the left and the right sample, written back
     */
    public void process(float in, float[] out) {
        line[cursor] = in;
        out[0] = in + mix * read(delayL);
        out[1] = in + mix * read(delayR);
        cursor = cursor + 1 == line.length ? 0 : cursor + 1;
    }

    /** linearly interpolated, so that a moving delay does not click */
    private float read(float delay) {
        float position = cursor - delay;
        while (position < 0) {
            position += line.length;
        }
        int i = (int) position;
        float f = position - i;
        int j = i + 1 == line.length ? 0 : i + 1;
        return line[i] + (line[j] - line[i]) * f;
    }
}
