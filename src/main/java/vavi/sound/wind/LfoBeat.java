/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * LfoBeat. the period of an LFO while it is synchronized to the host tempo.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum LfoBeat implements Labeled {

    B4_1("4/1", 16),
    B3_1("3/1", 12),
    B2_1("2/1", 8),
    B1_1("1/1", 4),
    B1_2D("1/2.", 3),
    B1_2("1/2", 2),
    B1_4D("1/4.", 1.5f),
    B1_2T("1/2T", 4 / 3f),
    B1_4("1/4", 1),
    B1_8D("1/8.", .75f),
    B1_4T("1/4T", 2 / 3f),
    B1_8("1/8", .5f),
    B1_16D("1/16.", .375f),
    B1_8T("1/8T", 1 / 3f),
    B1_16("1/16", .25f),
    B1_32D("1/32.", .1875f),
    B1_16T("1/16T", 1 / 6f),
    B1_32("1/32", .125f),
    B1_64D("1/64.", .09375f),
    B1_32T("1/32T", 1 / 12f),
    B1_64("1/64", .0625f),
    B1_64T("1/64T", 1 / 24f),
    B1_128("1/128", .03125f);

    private final String label;
    private final float quarters;

    LfoBeat(String label, float quarters) {
        this.label = label;
        this.quarters = quarters;
    }

    @Override
    public String label() {
        return label;
    }

    /** length of one LFO cycle in quarter notes */
    public float quarters() {
        return quarters;
    }

    /**
     * @param bpm quarter notes per minute
     * @return cycles per second
     */
    public float frequency(float bpm) {
        return bpm / 60 / quarters;
    }

    /** */
    public static LfoBeat valueOfLabel(String label) {
        return Labeled.valueOfLabel(LfoBeat.class, label);
    }
}
