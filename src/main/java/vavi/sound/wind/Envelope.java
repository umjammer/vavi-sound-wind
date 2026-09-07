/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Envelope. one of the four IFW envelope generators.
 * <p>
 * a segment does not run on a coefficient: it counts a phase down from one to zero at the
 * rate its knob asks for and reads the level off that, so a knob is a time and a segment
 * always takes exactly as long as it says. the attack is a straight line and everything under
 * it is bent, three quarters of the way towards a fourth power, which is what makes a decay
 * fall away quickly and then hang.
 * <p>
 * the times are the plug-in's own, and they are not the same at both ends: an attack runs
 * from under three milliseconds to nine seconds, a decay or a release from nine milliseconds
 * to seven.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the segments the plug-in really runs <br>
 */
public class Envelope {

    /** */
    public enum Stage {
        Attack, Decay, Sustain, Release, Idle
    }

    /** how much of a fourth power each segment is bent by */
    private static final float[] CURVE = {0, .75f, .75f, .75f, .75f};

    /** whether a segment counts down at all, the sustain and the idle staying where they are */
    private static final float[] RUNS = {1, 1, 0, 1, 0};

    /** */
    private final float sampleRate;

    /** knobs 0 .. 1 */
    private float attack, decay, sustain, release;

    /** */
    private Stage stage = Stage.Idle;

    /** where the segment stands, 1 at its start and 0 at its end */
    private float phase;

    /** where the level stood when the segment was entered */
    private float from;

    /** 0 .. 1 */
    private float level;

    /** */
    public Envelope(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    /** @param attack .. @param release knob values, 0 .. 10 */
    public void set(float attack, float decay, float sustain, float release) {
        this.attack = attack / 10;
        this.decay = decay / 10;
        this.sustain = sustain / 10;
        this.release = release / 10;
    }

    /**
     * @param retrigger whether to start the attack from zero rather than from where the
     *                  envelope stands
     */
    public void gateOn(boolean retrigger) {
        from = retrigger ? 0 : level;
        stage = Stage.Attack;
        phase = 1;
    }

    /** */
    public void gateOff() {
        if (stage != Stage.Idle) {
            from = level;
            stage = Stage.Release;
            phase = 1;
        }
    }

    /** */
    public void reset() {
        stage = Stage.Idle;
        level = from = phase = 0;
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
     * @param rate what the modulation matrix adds to every time knob, -1 .. 1, a whole knob
     *             at either end
     * @return 0 .. 1
     */
    public float process(float rate) {
        int i = stage.ordinal();
        float to = switch (stage) {
            case Attack -> 1;
            case Decay, Sustain -> sustain;
            case Release, Idle -> 0;
        };
        float shaped = phase + CURVE[i] * (phase * phase * phase * phase - phase);
        level = to + (from - to) * shaped;

        phase -= RUNS[i] / (seconds(i, rate) * sampleRate);
        if (phase <= 0) {
            phase = 1;
            from = to;
            stage = Stage.values()[Math.min(Stage.Idle.ordinal(), i + 1)];
        }
        return level;
    }

    /** how long the segment of a stage takes */
    private float seconds(int stage, float rate) {
        float knob = switch (stage) {
            case 0 -> attack;
            case 1 -> decay;
            case 3 -> release;
            default -> 0;
        };
        float x = Math.max(0, Math.min(10, (knob - rate) * 10));
        return stage == 0 ? attackSeconds(x) : decaySeconds(x);
    }

    /** an attack knob 0 .. 10 in seconds, under three milliseconds up to nine seconds */
    public static float attackSeconds(float knob) {
        return (float) Math.pow(10, knob * 0.3524176374259158 - 2.551016901713846);
    }

    /** a decay or release knob 0 .. 10 in seconds, nine milliseconds up to seven */
    public static float decaySeconds(float knob) {
        return (float) Math.pow(10, knob * 0.29125878566535722 - 2.0556068658995925);
    }
}
