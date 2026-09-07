/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * FilterType. the mode of IFW filter 1 and filter 2.
 * <p>
 * all four are taken off the same ladder of four sections, so the mode does not change the
 * filter, only which sections are added up and how hard the ladder is driven. the 24 dB mode
 * is the one that takes the whole ladder, and it is given the resonance and the drive to go
 * with it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the ladder's own settings for each mode <br>
 * @see Filter
 */
public enum FilterType implements Labeled {

    /** 24 dB/oct low pass, the whole ladder */
    LPF24(10f, 3.5f, .598f, true),
    /** 12 dB/oct low pass, the ladder's second section */
    LPF12(4.268017f, 1.4f, .2f, true),
    /** 12 dB/oct high pass, the sections that are left when the low pass is taken away */
    HPF12(4.268017f, 1.4f, .2f, false),
    /** 12 dB/oct band pass */
    BPF12(4.268017f, 1.4f, .2f, true);

    /** as far as the resonance is let go */
    private final float resonanceMax;
    /** how much the resonance drives the output */
    private final float drive;
    /** where this mode's own cutoff sits against the knob, in semitones */
    private final float cutoffOffset;
    /** whether the resonance also opens the input and the output up */
    private final boolean scaled;

    FilterType(float resonanceMax, float drive, float cutoffOffset, boolean scaled) {
        this.resonanceMax = resonanceMax;
        this.drive = drive;
        this.cutoffOffset = cutoffOffset;
        this.scaled = scaled;
    }

    @Override
    public String label() {
        return name();
    }

    /** */
    public float resonanceMax() {
        return resonanceMax;
    }

    /** */
    public float drive() {
        return drive;
    }

    /** */
    public float cutoffOffset() {
        return cutoffOffset;
    }

    /** */
    public boolean isScaled() {
        return scaled;
    }

    /** */
    public static FilterType valueOfLabel(String label) {
        return Labeled.valueOfLabel(FilterType.class, label);
    }
}
