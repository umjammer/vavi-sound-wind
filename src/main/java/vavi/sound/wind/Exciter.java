/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Exciter. the IFW psychoacoustic exciter.
 * <p>
 * takes what is above the frequency knob, bends it through a soft asymmetric curve to
 * make harmonics that were not there, and mixes those back in. a subtractive wind patch
 * loses its top end as soon as the breath drops, and this is what puts the air back.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public class Exciter {

    /** */
    private final float sampleRate;

    /** the one pole state of the split */
    private float state;

    /** */
    private float coefficient;

    /** -1 .. 1 */
    private float mix;

    /** */
    public Exciter(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /** */
    public void reset() {
        state = 0;
    }

    /**
     * @param frequency the frequency knob, 0 .. 10, 500 Hz to 16 kHz
     * @param mix the mix knob, -10 .. 10
     */
    public void set(float frequency, float mix) {
        float hz = Math.min(sampleRate * .45f, (float) (500 * Math.pow(2, frequency / 2)));
        coefficient = (float) -Math.expm1(-2 * Math.PI * hz / sampleRate);
        this.mix = Math.max(-1, Math.min(1, mix / 10));
    }

    /** */
    public float process(float in) {
        if (mix == 0) {
            return in;
        }
        state += (in - state) * coefficient;
        float high = in - state;
        // an asymmetric curve, so that it makes even harmonics as well as odd ones
        float harmonics = high * Math.abs(high) + high * high * .5f;
        return in + mix * harmonics * 2;
    }
}
