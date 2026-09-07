/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * LfoWaveform.
 * <p>
 * every one of them runs between 1 and 0 rather than either side of zero: an LFO of IFW is a
 * positive quantity, and it is the {@code (+-)} modulation sources that spread it over both
 * sides of its destination.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the plug-in's own order, and its own shapes <br>
 */
public enum LfoWaveform implements Labeled {

    SAW,
    TRI,
    SQU;

    @Override
    public String label() {
        return name();
    }

    /**
     * @param phase 0 .. 1
     * @return 0 .. 1
     */
    public float value(float phase) {
        return switch (this) {
            case SAW -> 1 - phase;
            case TRI -> Math.abs(phase * 2 - 1);
            case SQU -> phase * 2 < 1 ? 1 : 0;
        };
    }

    /** */
    public static LfoWaveform valueOfLabel(String label) {
        return Labeled.valueOfLabel(LfoWaveform.class, label);
    }
}
