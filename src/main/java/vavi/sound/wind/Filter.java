/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Filter. IFW filter 1 and filter 2.
 * <p>
 * a ladder of four one pole sections with the last one fed back to the first, each section
 * saturating what it passes on, which is the transistor ladder every subtractive synthesizer
 * has been measured against. the saturation is what gives the filter its voice: the harder
 * the resonance drives it the more it rounds off, so it thickens instead of whistling, and it
 * is why the plug-in runs the whole voice at four times the sample rate.
 * <p>
 * all four modes come off the same ladder, as taps rather than as separate filters:
 * <pre>
 * LPF24  y4
 * LPF12  y2
 * HPF12  x - 2 s1 + s2
 * BPF12  2 (s4 + s2) - 4 s3
 * </pre>
 * a cutoff is counted in semitones rather than in hertz, over a table that starts three
 * octaves under the bottom of a keyboard, so that the knob, the key tracking, the breath
 * controller and the modulation matrix can all be added up before anything is worked out.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the ladder the plug-in really has <br>
 */
public class Filter {

    /** the lowest cutoff the knob reaches, and the highest */
    public static final float MIN_CUTOFF = 0;
    public static final float MAX_CUTOFF = 172;

    /** what the resonance knob is worth before the mode's own limit is put on it */
    private static final float RESONANCE_SCALE = 6.18889f;

    /** */
    private final float sampleRate;

    /** the four sections, as they stood a sample ago and as they saturate */
    private float y1, y2, y3, y4;
    private float s1, s2, s3, s4;

    /** the last output of the ladder, which is what the resonance feeds back */
    private float feedback, last;

    /** */
    private FilterType type = FilterType.LPF12;

    /** the one pole coefficient of the current cutoff */
    private float g;

    /** what the resonance is worth here, and what the input and output are scaled by */
    private float resonance, inputGain, outputGain;

    /** */
    public Filter(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /** */
    public void reset() {
        y1 = y2 = y3 = y4 = 0;
        s1 = s2 = s3 = s4 = 0;
        feedback = last = 0;
    }

    /**
     * @param type the mode
     * @param cutoff where the cutoff stands in semitones, 0 .. 172, 24 being MIDI note 0
     * @param resonance the resonance knob, 0 .. 10
     */
    public void set(FilterType type, float cutoff, float resonance) {
        this.type = type;

        float hz = WaveBank.frequency(clamp(cutoff, MIN_CUTOFF, MAX_CUTOFF) - 24);
        // the ladder is written against how far up to nyquist the cutoff is, not against hertz
        float w = Math.min(1, Math.min(hz, sampleRate / 2 - 1) * 2 / sampleRate);
        this.g = (float) -Math.expm1(-Math.PI * w * ((1.8730 * w + .4955) * w * w - .6490 * w + .9988));
        float compensation = .9968f + 1.8409f * w - 3.9364f * w * w;

        float r = clamp(resonance / 10, 0, 1);
        float square = r * r;
        this.resonance = Math.min(RESONANCE_SCALE * square, type.resonanceMax()) * compensation;
        this.inputGain = type.isScaled() ? 2 - (1 - square) * (1 - square) : 1;
        this.outputGain = type.isScaled() ? 1 + square * (1.1f + w * type.drive()) : 1;
    }

    /** */
    public float process(float in) {
        float x = saturate(in * inputGain - feedback * resonance);

        y1 += (x - s1) * g;
        s1 = saturate(y1);
        y2 += (s1 - s2) * g;
        s2 = saturate(y2);
        y3 += (s2 - s3) * g;
        s3 = saturate(y3);
        float previous = y4;
        y4 += (s3 - s4) * g;
        s4 = saturate(y4);
        // the two samples either side of now, so that the resonance does not run away
        feedback = (previous + y4) / 2;

        float out = switch (type) {
            case LPF24 -> y4;
            case LPF12 -> y2;
            case HPF12 -> x - 2 * s1 + s2;
            case BPF12 -> 2 * (s4 + s2) - 4 * s3;
        };
        last = out * outputGain;
        return last;
    }

    /** what the ladder last put out */
    public float value() {
        return last;
    }

    /** the soft curve a ladder section passes its signal on through */
    private static float saturate(float x) {
        float v = clamp(x, -1, 1);
        return v - .25f * v * v * v;
    }

    /** */
    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * Where a cutoff knob puts the filter, in semitones.
     * <p>
     * the knob is not even: a step at the bottom is worth two semitones, a step in the middle
     * one, and a step at the top four, so that the whole of an audible range fits under a knob
     * that only turns ten times.
     *
     * @param knob 0 .. 10
     * @return -12 .. 148, before the 24 that puts MIDI note 0 at 24
     */
    public static float cutoff(float knob) {
        float x = clamp(knob, 0, 10) * 10;
        int i = Math.min(100, (int) x);
        float a = KNOB[i];
        return a + (KNOB[Math.min(100, i + 1)] - a) * (x - i);
    }

    /** where each tenth of a cutoff knob puts the filter, in semitones */
    private static final float[] KNOB = new float[101];

    static {
        int i = 0;
        for (float v = -12; v <= 34; v += 2) {
            KNOB[i++] = v;
        }
        for (float v = 36; v <= 96; v += 1) {
            KNOB[i++] = v;
        }
        for (float v = 98; v <= 108; v += 2) {
            KNOB[i++] = v;
        }
        for (float v = 112; v <= 148; v += 4) {
            KNOB[i++] = v;
        }
    }
}
