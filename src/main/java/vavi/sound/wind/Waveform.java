/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Waveform. what the second slot of an oscillator (the "TRI" knob) actually generates.
 * <p>
 * the {@code Ext.In} choices feed the host input into the slot instead of an oscillator,
 * which lets IFW filter an external signal by the breath controller.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum Waveform implements Labeled {

    TRI("TRI"),
    SIN("SIN"),
    ExtInLR("Ext.In L+R"),
    ExtInL("Ext.In L"),
    ExtInR("Ext.In R");

    private final String label;

    Waveform(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /** whether this waveform reads the external input instead of the oscillator */
    public boolean isExternal() {
        return ordinal() >= ExtInLR.ordinal();
    }

    /**
     * @param phase 0 .. 1
     * @return -1 .. 1, 0 for the external input choices
     */
    public float value(float phase) {
        return switch (this) {
            case TRI -> phase < .5f ? phase * 4 - 1 : 3 - phase * 4;
            case SIN -> (float) Math.sin(phase * 2 * Math.PI);
            default -> 0;
        };
    }

    /** */
    public static Waveform valueOfLabel(String label) {
        return Labeled.valueOfLabel(Waveform.class, label);
    }
}
