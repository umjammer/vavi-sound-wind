/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Bool. an IFW on/off switch.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum Bool implements Labeled {

    Off,
    On;

    @Override
    public String label() {
        return name();
    }

    /** */
    public boolean isOn() {
        return this == On;
    }

    /** */
    public static Bool valueOfLabel(String label) {
        return Labeled.valueOfLabel(Bool.class, label);
    }
}
