/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * BreathResponse. what the travel of the breath controller does to the loudness.
 * <p>
 * an IFW tone file holds no breath curve. a tone shapes breath only with its BREATH
 * knobs, its breath sub mod and the modulation matrix, so the curve is the tone
 * designer's business and every tone answers the controller differently. what is missing
 * is the player's own end of it, the part a wind controller keeps in its setup rather
 * than in the patch, and that is what this is.
 * <p>
 * three things, in the order they happen:
 * <ol>
 * <li>{@link #low()} and {@link #high()} say where the sensor really rests and where it
 *     really tops out, so that the whole of the player's travel is used
 * <li>{@link #depth()} says how many decibels the travel is spread over, and bends it so
 *     that they are spent evenly rather than all at once
 * </ol>
 * the bend is the point. a level that follows breath straight has all of its decibels
 * crammed into the bottom of the travel, half breath being only 6 dB down and the top
 * half of the sensor doing almost nothing, which is what makes a wind synthesizer feel
 * like a switch rather than an instrument. spending them evenly instead is the rule a
 * mixing desk fader and MIDI volume already follow, and the one the ear reads as even,
 * since loudness goes with the logarithm of the level rather than with the level. at the
 * default depth full breath answers about two and a half times as steeply as it does
 * straight through, and half breath sits 12 dB down instead of 6.
 * <p>
 * the last of the travel is not quite even: the curve is pulled down so that no breath is
 * real silence rather than {@code depth} dB of it, which steepens the bottom of the
 * travel, and that is what lets a note stop.
 * <p>
 * a positive depth spreads the level out and is what a shallow tone wants; a negative one
 * pulls it together, for a tone whose own filters already answer breath so steeply that
 * the note will not speak until the player is halfway in. zero leaves the controller
 * exactly as IFW has it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @see IfwEngine#setBreathResponse(BreathResponse)
 */
public class BreathResponse {

    /** a decibel as a natural logarithm, so that the curve costs one exp and no pow */
    private static final double LN10_20 = Math.log(10) / 20;

    /** breath straight through, exactly what IFW itself does */
    public static final BreathResponse LINEAR = new BreathResponse(0, 1, 0);

    /**
     * the whole of the sensor, spread over 18 dB.
     * <p>
     * measured over the fifty tones IFW installs, this is what moves the middle one from
     * answering half breath 10 dB under its loudest to answering it 16 dB under, and its
     * whole travel from 32 dB to 41 dB, which is about what an acoustic wind instrument
     * gives between its softest and its loudest note. more than this and the tones that
     * already sweep their filters hard stop speaking at the bottom of the travel.
     */
    public static final BreathResponse DEFAULT = new BreathResponse(0, 1, 18);

    /** */
    private final float low, high, depth;

    /** {@code 10 ^ (-depth / 20)}, where the curve lands at no breath */
    private final double floor;

    /** */
    private final double span;

    /**
     * @param low   where the sensor rests, 0 .. 1
     * @param high  where the sensor tops out, over {@code low} and up to 1
     * @param depth how many decibels the travel is spread over. positive spreads the
     *              level out, negative pulls it together, 0 leaves it alone
     */
    public BreathResponse(float low, float high, float depth) {
        if (!(low >= 0) || !(high <= 1) || !(low < high)) {
            throw new IllegalArgumentException("0 <= low < high <= 1, but " + low + " .. " + high);
        }
        if (!(Math.abs(depth) <= 96)) {
            throw new IllegalArgumentException("depth within +-96 dB, but " + depth);
        }
        this.low = low;
        this.high = high;
        this.depth = depth;
        this.floor = Math.exp(-depth * LN10_20);
        this.span = 1 - floor;
    }

    /** */
    public float low() {
        return low;
    }

    /** */
    public float high() {
        return high;
    }

    /** */
    public float depth() {
        return depth;
    }

    /**
     * Turns what the controller sent into what the amplifiers should do with it.
     *
     * @param breath the controller, 0 .. 1
     * @return the level it asks for, 0 .. 1
     */
    public float apply(float breath) {
        float x = (breath - low) / (high - low);
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        return depth == 0 ? x : (float) ((Math.exp(depth * (x - 1) * LN10_20) - floor) / span);
    }

    /**
     * Reads back what {@link #toString()} wrote, {@code low,high,depth}.
     * <p>
     * any of the three may be left out, {@code ,,18} being a depth alone.
     */
    public static BreathResponse valueOf(String text) {
        String[] parts = text.split(",", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("low,high,depth, but " + text);
        }
        return new BreathResponse(
                field(parts[0], LINEAR.low), field(parts[1], LINEAR.high), field(parts[2], LINEAR.depth));
    }

    /** */
    private static float field(String text, float fallback) {
        return text.isBlank() ? fallback : Float.parseFloat(text.trim());
    }

    @Override
    public String toString() {
        return low + "," + high + "," + depth;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BreathResponse that
                && low == that.low && high == that.high && depth == that.depth;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(low) * 31 * 31 + Float.hashCode(high) * 31 + Float.hashCode(depth);
    }
}
