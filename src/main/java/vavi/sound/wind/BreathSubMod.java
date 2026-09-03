/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * BreathSubMod. what the BREATH knob of a filter or an amp is fed with.
 * <p>
 * either the breath controller alone, or the breath controller scaled by one of the
 * envelopes, which is how IFW gives a wind patch an attack without spending a
 * modulation slot.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum BreathSubMod implements Labeled {

    BREATH("BREATH", "Breath"),
    BRxEG1("BRxEG1", "Breath x EG 1"),
    BRxEG2("BRxEG2", "Breath x EG 2"),
    BRxEG3("BRxEG3", "Breath x EG 3"),
    BRxEG4("BRxEG4", "Breath x EG 4");

    private final String label;
    private final String alias;

    BreathSubMod(String label, String alias) {
        this.label = label;
        this.alias = alias;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String[] aliases() {
        return new String[] {alias};
    }

    /** index of the envelope to scale with, or -1 for the breath controller alone */
    public int eg() {
        return ordinal() - 1;
    }

    /** */
    public static BreathSubMod valueOfLabel(String label) {
        return Labeled.valueOfLabel(BreathSubMod.class, label);
    }
}
