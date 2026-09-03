/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */


package vavi.sound.wind;

import java.util.Arrays;


/**
 * ToneControl. the two band IFW master EQ.
 * <p>
 * a low shelf at 200 Hz and a high shelf at 4 kHz, both -18 to 18 dB.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public class ToneControl {

    /** */
    private static final float LOW_HZ = 200;

    /** */
    private static final float HIGH_HZ = 4000;

    /** */
    private final float sampleRate;

    /** the two shelves, low first */
    private final float[][] coefficients = new float[2][5];

    /** */
    private final float[][] states = new float[2][4];

    /** */
    public ToneControl(float sampleRate) {
        this.sampleRate = sampleRate;
        update(0, 0);
    }

    /** */
    public void reset() {
        for (float[] state : states) {
            Arrays.fill(state, 0);
        }
    }

    /** @param low @param high dB, -18 .. 18 */
    public void set(float low, float high) {
        update(low, high);
    }

    /** */
    private void update(float low, float high) {
        shelf(0, LOW_HZ, low, true);
        shelf(1, HIGH_HZ, high, false);
    }

    /** */
    public float process(float in) {
        return biquad(1, biquad(0, in));
    }

    /** a Robert Bristow-Johnson shelving section */
    private void shelf(int n, float hz, float dB, boolean low) {
        double a = Math.pow(10, dB / 40);
        double w = 2 * Math.PI * hz / sampleRate;
        double cos = Math.cos(w);
        double alpha = Math.sin(w) / 2 * Math.sqrt((a + 1 / a) * (1 / 0.9 - 1) + 2);
        double sqrt = 2 * Math.sqrt(a) * alpha;

        double b0, b1, b2, a0, a1, a2;
        if (low) {
            b0 = a * (a + 1 - (a - 1) * cos + sqrt);
            b1 = 2 * a * (a - 1 - (a + 1) * cos);
            b2 = a * (a + 1 - (a - 1) * cos - sqrt);
            a0 = a + 1 + (a - 1) * cos + sqrt;
            a1 = -2 * (a - 1 + (a + 1) * cos);
            a2 = a + 1 + (a - 1) * cos - sqrt;
        } else {
            b0 = a * (a + 1 + (a - 1) * cos + sqrt);
            b1 = -2 * a * (a - 1 + (a + 1) * cos);
            b2 = a * (a + 1 + (a - 1) * cos - sqrt);
            a0 = a + 1 - (a - 1) * cos + sqrt;
            a1 = 2 * (a - 1 - (a + 1) * cos);
            a2 = a + 1 - (a - 1) * cos - sqrt;
        }

        float[] c = coefficients[n];
        c[0] = (float) (b0 / a0);
        c[1] = (float) (b1 / a0);
        c[2] = (float) (b2 / a0);
        c[3] = (float) (a1 / a0);
        c[4] = (float) (a2 / a0);
    }

    /** direct form 1 */
    private float biquad(int n, float in) {
        float[] c = coefficients[n];
        float[] s = states[n];
        float out = c[0] * in + c[1] * s[0] + c[2] * s[1] - c[3] * s[2] - c[4] * s[3];
        s[1] = s[0];
        s[0] = in;
        s[3] = s[2];
        s[2] = out;
        return out;
    }
}
