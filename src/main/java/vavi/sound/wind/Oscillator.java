/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Oscillator. one of the four IFW oscillators.
 * <p>
 * an oscillator is three slots mixed by their own level knobs, and the modulation matrix can
 * read each slot on its own, which is how an IFW patch builds FM and ring modulation without
 * a dedicated operator section.
 * <ol>
 * <li>a sawtooth, always wave 0 of the {@link WaveBank}</li>
 * <li>the wave {@link Waveform} names, a triangle unless the tone says otherwise, or the
 *     host's own input</li>
 * <li>a pulse, which is the sawtooth of the first slot less a copy of itself shifted by the
 *     {@code WIDTH} knob, so that it is band limited for nothing</li>
 * </ol>
 * oscillator 4 spends its third slot on noise instead, which is the one a patch reaches for
 * to put breath into a flute.
 * <p>
 * nothing here is generated a sample at a time: every slot reads the table its note has, and
 * a table holds only the harmonics that fit, so a bright patch does not alias however high it
 * is played.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the wavetables the plug-in actually reads <br>
 */
public class Oscillator {

    /** the waves every oscillator reads */
    private static final WaveBank BANK = WaveBank.getInstance();

    /** whether this is oscillator 4, whose third slot is noise */
    private final boolean noiseSlot;

    /** 0 .. 1 */
    private float phase;

    /** the last output of each slot, -1 .. 1 */
    private float saw, wave, pulse;

    /** the running sample and hold of the noise slot, and how far it has run */
    private float noise, noisePhase;

    /** the two words of the plug-in's own noise generator */
    private int random0 = 0xefcdab89, random1 = 0xefcdab89;

    /** */
    public Oscillator(boolean noiseSlot) {
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

    /** 0 .. 1 */
    public float phase() {
        return phase;
    }

    /** Puts the cycle back to where a synchronised oscillator starts it. */
    public void sync(float phase) {
        this.phase = phase;
    }

    /**
     * Steps one sample.
     *
     * @param note which band limited table to read, 0 .. 191
     * @param increment how far through the cycle one sample goes
     * @param fm phase modulation in cycles, from the modulation matrix
     * @param width the pulse duty, 0 .. 1, over which the slot falls silent
     * @param waveform what the second slot reads
     * @param loFi the sample and hold of the noise slot, 0 .. 1
     * @param external the host input the second slot mixes, already weighted
     */
    public void process(int note, float increment, float fm, float width,
                        Waveform waveform, float loFi, float external) {

        // the ten keeps the truncation on the positive side, whatever the modulation did
        float p = wrap(phase + 10 + fm);
        saw = BANK.value(0, note, p);
        wave = BANK.value(waveform.wave(), note, p) * waveform.gain() + external;

        if (noiseSlot) {
            noisePhase += 1 / (1 + 39 * clamp(loFi));
            if (noisePhase >= 1) {
                noisePhase -= (int) noisePhase;
                random0 ^= random1;
                random1 += random0;
                noise = random1 * 4.4237822e-10f;
            }
            pulse = noise;
        } else {
            // a pulse is a sawtooth less the same sawtooth a part of a cycle later
            float w = width >= 1 ? 0 : Math.max(0, width);
            pulse = saw - BANK.value(0, note, wrap(p + w));
        }

        phase += increment;
        if (phase >= 1) {
            phase -= (int) phase;
        }
    }

    /** */
    private static float wrap(float phase) {
        float p = phase - (int) phase;
        return p < 0 ? p + 1 : p;
    }

    /** */
    private static float clamp(float value) {
        return value < 0 ? 0 : Math.min(value, 1);
    }
}
