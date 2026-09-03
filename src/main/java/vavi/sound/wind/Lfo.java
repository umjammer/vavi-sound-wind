/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Lfo. one of the two IFW low frequency oscillators.
 * <p>
 * free running by default, restarted by every note when {@code Key Sync} is on, and
 * locked to the host tempo at a {@link LfoBeat} when {@code BPM Sync} is on.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
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

    /** */
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

    /** @return -1 .. 1 */
    public float process() {
        value = waveform.value(phase);
        phase += frequency / sampleRate;
        if (phase >= 1) {
            phase -= (int) phase;
        }
        return value;
    }

    /** @return -1 .. 1 */
    public float value() {
        return value;
    }

    /** the speed knob 0 .. 10 in cycles per second */
    public static float hertz(float knob) {
        return knob * 2;
    }
}
