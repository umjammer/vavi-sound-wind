/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.util.Arrays;


/**
 * Enhancer. what makes the one voice of IFW into two channels.
 * <p>
 * a delay of its own for each side, crossfaded against the dry signal rather than added to
 * it, so that a mix of 10 is the delays alone. two delays a few milliseconds apart is what
 * puts an instrument in a room without a reverb.
 * <p>
 * a direct current blocker follows it, which is what keeps a tone that leans on one side of
 * zero from pushing the whole mix off centre.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano a crossfade rather than an addition <br>
 */
public class Enhancer {

    /** as far as the delay is let go, whatever the knob says */
    private static final float MAX_DELAY = .1f;

    /** */
    private final float sampleRate;

    /** */
    private final float[] line;

    /** */
    private final int mask;

    /** */
    private int cursor;

    /** in samples */
    private float delayL, delayR;

    /** -1 .. 1 */
    private float mix;

    /** the two channels of the direct current blocker */
    private final float[] blockerIn = new float[2], blockerOut = new float[2];

    /** */
    private final float blocker;

    /** */
    public Enhancer(float sampleRate) {
        this.sampleRate = sampleRate;
        int size = Integer.highestOneBit((int) Math.ceil(MAX_DELAY * sampleRate) * 2 - 1) * 2;
        this.line = new float[size];
        this.mask = size - 1;
        this.blocker = 1 - 16 / sampleRate;
    }

    /** */
    public void reset() {
        Arrays.fill(line, 0);
        Arrays.fill(blockerIn, 0);
        Arrays.fill(blockerOut, 0);
        cursor = 0;
    }

    /**
     * @param delayL the delay L knob, 0 .. 50 ms
     * @param delayR the delay R knob, 0 .. 50 ms
     * @param mix the mix knob, -10 .. 10
     */
    public void set(float delayL, float delayR, float mix) {
        this.delayL = samples(delayL);
        this.delayR = samples(delayR);
        this.mix = clamp(mix / 10, -1, 1);
    }

    /** */
    private float samples(float milliseconds) {
        return clamp(milliseconds / 1000, 0, MAX_DELAY) * sampleRate;
    }

    /**
     * Steps one sample.
     *
     * @param out the left and the right sample, written back
     */
    public void process(float in, float[] out) {
        line[cursor] = in;
        cursor = cursor + 1 & mask;

        float dry = 1 - Math.abs(mix);
        out[0] = block(0, dry * in + mix * read(delayL));
        out[1] = block(1, dry * in + mix * read(delayR));
    }

    /** linearly interpolated, so that a moving delay does not click */
    private float read(float delay) {
        float position = cursor - 1 - delay;
        while (position < 0) {
            position += line.length;
        }
        int i = (int) position & mask;
        int j = i + 1 & mask;
        float f = position - (int) position;
        return line[i] + (line[j] - line[i]) * f;
    }

    /** */
    private float block(int channel, float in) {
        float out = in - blockerIn[channel] + blocker * blockerOut[channel];
        blockerIn[channel] = in;
        blockerOut[channel] = out;
        return out;
    }

    /** */
    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
}
