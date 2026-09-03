/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * LfoWaveform.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum LfoWaveform implements Labeled {

    TRI,
    SAW,
    SQU;

    @Override
    public String label() {
        return name();
    }

    /**
     * @param phase 0 .. 1
     * @return -1 .. 1
     */
    public float value(float phase) {
        return switch (this) {
            case TRI -> phase < .5f ? phase * 4 - 1 : 3 - phase * 4;
            case SAW -> 1 - phase * 2;
            case SQU -> phase < .5f ? 1 : -1;
        };
    }

    /** */
    public static LfoWaveform valueOfLabel(String label) {
        return Labeled.valueOfLabel(LfoWaveform.class, label);
    }
}
