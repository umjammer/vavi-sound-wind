/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Filter. IFW filter 1 and filter 2.
 * <p>
 * a zero delay feedback state variable filter, which stays stable while the breath
 * controller sweeps the cutoff across the whole range every note. {@link FilterType#LPF24}
 * runs two of the 12 dB sections in series.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public class Filter {

    /** */
    private final float sampleRate;

    /** the integrator states of the two sections */
    private float ic1eq1, ic2eq1, ic1eq2, ic2eq2;

    /** */
    private FilterType type = FilterType.LPF12;

    /** the coefficients of the current cutoff and resonance */
    private float g, k, a1, a2;

    /** */
    public Filter(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /** */
    public void reset() {
        ic1eq1 = ic2eq1 = ic1eq2 = ic2eq2 = 0;
    }

    /**
     * @param type the mode
     * @param frequency the cutoff knob, 0 .. 10, an octave per step from 20 Hz
     * @param resonance the resonance knob, 0 .. 10
     */
    public void set(FilterType type, float frequency, float resonance) {
        this.type = type;
        float hz = Math.min(sampleRate * .45f, hertz(frequency));
        g = (float) Math.tan(Math.PI * hz / sampleRate);
        k = 2 - 2 * Math.min(.98f, resonance / 10);
        a1 = 1 / (1 + g * (g + k));
        a2 = g * a1;
    }

    /** */
    public float process(float in) {
        float out = section(in, 1);
        return type == FilterType.LPF24 ? section(out, 2) : out;
    }

    /** one 12 dB/oct section, {@code n} picks which pair of integrator states to use */
    private float section(float in, int n) {
        float ic1eq = n == 1 ? ic1eq1 : ic1eq2;
        float ic2eq = n == 1 ? ic2eq1 : ic2eq2;

        float v1 = a1 * ic1eq + a2 * (in - ic2eq);
        float v2 = ic2eq + g * v1;
        ic1eq = 2 * v1 - ic1eq;
        ic2eq = 2 * v2 - ic2eq;

        if (n == 1) {
            ic1eq1 = ic1eq;
            ic2eq1 = ic2eq;
        } else {
            ic1eq2 = ic1eq;
            ic2eq2 = ic2eq;
        }

        return switch (type) {
            case LPF12, LPF24 -> v2;
            case HPF12 -> in - k * v1 - v2;
            case BPF12 -> v1;
        };
    }

    /** a cutoff knob 0 .. 10 in Hz, one octave per step from 20 Hz */
    public static float hertz(float knob) {
        return (float) (20 * Math.pow(2, Math.max(0, Math.min(10, knob))));
    }
}
