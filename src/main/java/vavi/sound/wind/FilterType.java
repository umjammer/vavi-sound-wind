/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * FilterType. the mode of IFW filter 1 and filter 2.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum FilterType implements Labeled {

    /** 24 dB/oct low pass, two 12 dB/oct sections in series */
    LPF24,
    /** 12 dB/oct low pass */
    LPF12,
    /** 12 dB/oct high pass */
    HPF12,
    /** 12 dB/oct band pass */
    BPF12;

    @Override
    public String label() {
        return name();
    }

    /** */
    public static FilterType valueOfLabel(String label) {
        return Labeled.valueOfLabel(FilterType.class, label);
    }
}
