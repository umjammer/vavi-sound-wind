/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Oscillator. one of the four IFW oscillators.
 * <p>
 * an oscillator is three slots mixed by their own level knobs, and the modulation matrix
 * can read each slot on its own, which is how an IFW patch builds FM and ring modulation
 * without a dedicated operator section.
 * <ol>
 * <li>a sawtooth</li>
 * <li>the wave {@link Waveform} selects, a triangle unless the patch says otherwise</li>
 * <li>a pulse whose width the {@code WIDTH} knob sets, or the single cycle instrument
 *     wave {@link PcmWaveform} selects</li>
 * </ol>
 * oscillator 4 replaces the third slot with noise, so it is the one a patch reaches for
 * to put breath noise into a flute.
 * <p>
 * the sawtooth and the pulse are band limited by polyBLEP, which keeps the aliasing of a
 * bright patch under control without oversampling.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public class Oscillator {

    /** */
    private final float sampleRate;

    /** whether this is oscillator 4, whose third slot is noise */
    private final boolean noiseSlot;

    /** 0 .. 1 */
    private float phase;

    /** the last output of each slot, -1 .. 1 */
    private float saw, wave, pulse;

    /** the running sample and hold of the noise slot */
    private float noise;

    /** how far the lo-fi sample and hold of the noise slot has run */
    private float noisePhase;

    /** */
    private long random = 0x5deece66dL;

    /** */
    public Oscillator(float sampleRate, boolean noiseSlot) {
        this.sampleRate = sampleRate;
        this.noiseSlot = noiseSlot;
    }

    /** */
    public void reset() {
        phase = 0;
        saw = wave = pulse = 0;
    }

    /** the sawtooth slot, -1 .. 1 */
    public float saw() {
        return saw;
    }

    /** the {@link Waveform} slot, -1 .. 1 */
    public float wave() {
        return wave;
    }

    /** the pulse or noise slot, -1 .. 1 */
    public float pulse() {
        return pulse;
    }

    /** */
    public float phase() {
        return phase;
    }

    /**
     * Steps one sample.
     *
     * @param frequency Hz
     * @param fm phase modulation in cycles, from the modulation matrix
     * @param width the pulse duty, 0 .. 1
     * @param pulsePhase an offset of the pulse slot only, in cycles
     * @param waveform what the second slot generates
     * @param pcm an instrument wave for the third slot, {@link PcmWaveform#None} for the pulse
     * @param loFi the noise sample and hold knob of oscillator 4, 0 .. 10
     * @param external the host input, for the {@code Ext.In} waveforms
     * @param sync whether to restart the cycle, the master oscillator has just wrapped
     * @return whether this oscillator wrapped, so that it can drive a synchronized one
     */
    public boolean process(float frequency, float fm, float width, float pulsePhase,
                           Waveform waveform, PcmWaveform pcm, float loFi, float external, boolean sync) {

        float increment = Math.min(.5f, Math.abs(frequency) / sampleRate);
        if (sync) {
            phase = 0;
        }

        float p = wrap(phase + fm);
        saw = 2 * p - 1 - polyBlep(p, increment);

        wave = waveform.isExternal() ? external : waveform.value(p);

        if (noiseSlot) {
            // 10 means a sample and hold at about 200 Hz, 0 leaves the noise at full rate
            float rate = loFi <= 0 ? 1 : (float) Math.pow(10, -loFi * 0.24);
            noisePhase += rate;
            if (noisePhase >= 1) {
                noisePhase -= (int) noisePhase;
                random = random * 0x5deece66dL + 0xb;
                noise = (random >> 24 & 0xffff) / 32768f - 1;
            }
            pulse = noise;
        } else if (pcm != PcmWaveform.None) {
            pulse = pcm.value(wrap(p + pulsePhase));
        } else {
            float w = Math.min(.99f, Math.max(.01f, width));
            float t = wrap(p + pulsePhase);
            pulse = (t < w ? 1 : -1) + polyBlep(t, increment) - polyBlep(wrap(t - w), increment);
        }

        phase += increment;
        boolean wrapped = phase >= 1;
        if (wrapped) {
            phase -= (int) phase;
        }
        return wrapped;
    }

    /** */
    private static float wrap(float phase) {
        float p = phase - (int) phase;
        return p < 0 ? p + 1 : p;
    }

    /**
     * The correction that rounds off the step of a naive sawtooth or pulse, so that the
     * harmonics above nyquist fold back quietly instead of as a whistle.
     *
     * @param t where in the cycle we are, 0 .. 1
     * @param dt one sample as a part of the cycle
     */
    private static float polyBlep(float t, float dt) {
        if (t < dt) {
            float x = t / dt;
            return x + x - x * x - 1;
        }
        if (t > 1 - dt) {
            float x = (t - 1) / dt;
            return x * x + x + x + 1;
        }
        return 0;
    }

    /** the semi and tune knobs of an oscillator as a frequency factor */
    public static float detune(float semi, float cents) {
        return (float) Math.pow(2, (semi + cents / 100) / 12);
    }
}
