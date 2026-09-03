/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Envelope. one of the four IFW attack decay sustain release generators.
 * <p>
 * the attack is linear and the decay and the release are exponential, which is what a
 * knob range of 0 .. 10 covering one millisecond to ten seconds needs to stay usable at
 * both ends. {@code Retrigger} decides what a second note during a legato phrase does:
 * off leaves the envelope running, on starts it from zero again.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public class Envelope {

    /** */
    public enum Stage {
        Idle, Attack, Decay, Sustain, Release
    }

    /** below this the release is over */
    private static final float EPSILON = 1e-5f;

    /** */
    private final float sampleRate;

    /** knob 0 .. 10 */
    private float attack, decay, sustain, release;

    /** */
    private Stage stage = Stage.Idle;

    /** 0 .. 1 */
    private float level;

    /** */
    public Envelope(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /** @param attack .. @param release knob values, 0 .. 10 */
    public void set(float attack, float decay, float sustain, float release) {
        this.attack = attack;
        this.decay = decay;
        this.sustain = sustain / 10;
        this.release = release;
    }

    /**
     * @param retrigger whether to restart from zero rather than from where the envelope stands
     */
    public void gateOn(boolean retrigger) {
        if (retrigger) {
            level = 0;
        }
        stage = Stage.Attack;
    }

    /** */
    public void gateOff() {
        if (stage != Stage.Idle) {
            stage = Stage.Release;
        }
    }

    /** */
    public void reset() {
        stage = Stage.Idle;
        level = 0;
    }

    /** */
    public Stage stage() {
        return stage;
    }

    /** */
    public float level() {
        return level;
    }

    /** */
    public boolean isIdle() {
        return stage == Stage.Idle;
    }

    /**
     * Steps one sample.
     *
     * @param rate a factor on every segment time, 1 to run at the knob settings
     * @return 0 .. 1
     */
    public float process(float rate) {
        switch (stage) {
            case Attack -> {
                level += 1 / Math.max(1, seconds(attack) * rate * sampleRate);
                if (level >= 1) {
                    level = 1;
                    stage = Stage.Decay;
                }
            }
            case Decay -> {
                level += (sustain - level) * coefficient(decay, rate);
                if (Math.abs(level - sustain) < EPSILON) {
                    level = sustain;
                    stage = Stage.Sustain;
                }
            }
            case Sustain -> level = sustain;
            case Release -> {
                level -= level * coefficient(release, rate);
                if (level < EPSILON) {
                    level = 0;
                    stage = Stage.Idle;
                }
            }
            case Idle -> level = 0;
        }
        return level;
    }

    /** knob 0 .. 10 to one millisecond .. ten seconds */
    static float seconds(float knob) {
        return (float) (0.001 * Math.pow(10, knob * 0.4));
    }

    /** the per sample factor of an exponential segment that reaches -60 dB in its time */
    private float coefficient(float knob, float rate) {
        float samples = seconds(knob) * rate * sampleRate;
        return samples < 1 ? 1 : (float) -Math.expm1(-6.9 / samples);
    }
}
