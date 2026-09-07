/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Lfo.
 * <p>
 * the speed knob is a square rather than a straight line, so that the bottom of it is a
 * vibrato and the top of it is audio: a hundredth of a hertz per step at the bottom, a
 * hundred hertz at the end.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the plug-in's own speed curve, and a positive output <br>
 */
public class Lfo {

    /** */
    private final float sampleRate;

    /** 0 .. 1 */
    private float phase;

    /** */
    private LfoWaveform waveform = LfoWaveform.TRI;

    /** cycles per second */
    private float frequency = 4;

    /** */
    private boolean keySync;

    /** 0 .. 1 */
    private float value;

    /** */
    public Lfo(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /** */
    public void set(LfoWaveform waveform, float frequency, boolean keySync) {
        this.waveform = waveform;
        this.frequency = frequency;
        this.keySync = keySync;
    }

    /** Restarts the cycle if this LFO is key synchronized. */
    public void trigger() {
        if (keySync) {
            phase = 0;
        }
    }

    /** @return 0 .. 1 */
    public float process() {
        value = waveform.value(phase);
        phase += frequency / sampleRate;
        if (phase >= 1) {
            phase -= (int) phase;
        }
        return value;
    }

    /** @return 0 .. 1 */
    public float value() {
        return value;
    }

    /** the speed knob 0 .. 10 in cycles per second, a hundredth of a hertz up to a hundred */
    public static float hertz(float knob) {
        float x = Math.max(0, Math.min(10, knob)) * 10;
        return x * x * .01f;
    }
}
