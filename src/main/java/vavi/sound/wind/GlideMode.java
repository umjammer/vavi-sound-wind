/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * GlideMode. whether the glide speed knob means a constant duration or a constant slope.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum GlideMode implements Labeled {

    /** every interval takes the same time */
    Time,
    /** every interval is crossed at the same semitones per second */
    Rate;

    @Override
    public String label() {
        return name();
    }

    /** */
    public static GlideMode valueOfLabel(String label) {
        return Labeled.valueOfLabel(GlideMode.class, label);
    }
}
