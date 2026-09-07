/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * ToneControl. the IFW EQ, two shelves either side of the instrument.
 * <p>
 * one pole each rather than the two a mixing desk would use, which is why the panel gives it
 * eighteen decibels and no width: it tilts a tone rather than carving it. the low shelf turns
 * at 250 Hz and the high one at 5 kHz, and the high one comes first.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the one pole shelves the plug-in really has <br>
 */
public class ToneControl {

    /** */
    private static final float LOW_HZ = 250;

    /** */
    private static final float HIGH_HZ = 5000;

    /** the tangent each shelf turns on, worked out once for the sample rate */
    private final double lowTan, highTan;

    /** b0, b1 and a1 of each shelf, the low one first */
    private final float[][] coefficients = {{1, 0, 0}, {1, 0, 0}};

    /** what went into the high shelf, what came out of it, and what came out of the low one */
    private float highIn, highOut, lowOut;

    /** */
    public ToneControl(float sampleRate) {
        this.lowTan = Math.tan(Math.PI * LOW_HZ / sampleRate);
        this.highTan = Math.tan((Math.PI - 2 * Math.PI * HIGH_HZ / sampleRate) / 2);
        set(0, 0);
    }

    /** */
    public void reset() {
        highIn = highOut = lowOut = 0;
    }

    /** @param low @param high dB, -18 .. 18 */
    public void set(float low, float high) {
        shelf(0, lowTan, low, true);
        shelf(1, highTan, high, false);
    }

    /** */
    public float process(float in) {
        float[] h = coefficients[1];
        float[] l = coefficients[0];
        float high = h[0] * in + h[1] * highIn + h[2] * highOut;
        float out = l[0] * high + l[1] * highOut + l[2] * lowOut;
        highIn = in;
        highOut = high;
        lowOut = out;
        return out;
    }

    /** a one pole shelf, written the way round that keeps its gain at the far end at unity */
    private void shelf(int n, double t, float dB, boolean low) {
        double a = Math.pow(10, dB * 0.05);
        double b0, b1, a1;
        if (low) {
            if (a >= 1) {
                double d = 1 / (t + 1);
                b0 = (a * t + 1) * d;
                b1 = (a * t - 1) * d;
                a1 = (1 - t) * d;
            } else {
                double k = t / a;
                double d = 1 / (k + 1);
                b0 = (t + 1) * d;
                b1 = (t - 1) * d;
                a1 = (1 - k) * d;
            }
        } else {
            double p = a >= 1 ? a * t : t;
            double q = a >= 1 ? t : t / a;
            double d = 1 / (q + 1);
            b0 = (p + 1) * d;
            b1 = (1 - p) * d;
            a1 = (q - 1) * d;
        }
        float[] c = coefficients[n];
        c[0] = (float) b0;
        c[1] = (float) b1;
        c[2] = (float) a1;
    }
}
