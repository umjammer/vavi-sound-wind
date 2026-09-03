/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * FilterConnection. how the oscillator busses, the three filters and the two amps are wired.
 * <pre>
 * Serial     : Out 1 -&gt; Filter 1 -&gt; Filter 2 -&gt; Filter 3 -&gt; Amp 1
 * Parallel 1 : Out 1 -&gt; Filter 1 -+
 *              Out 2 -&gt; Filter 2 -+-&gt; Filter 3 -&gt; Amp 1
 * Parallel 2 : Out 1 -&gt; Filter 1 ---&gt; Filter 3 -&gt; Amp 1
 *              Out 2 -&gt; Filter 2 -----------------&gt; Amp 2
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum FilterConnection implements Labeled {

    Serial("Serial"),
    Parallel1("Parallel 1"),
    Parallel2("Parallel 2");

    private final String label;

    FilterConnection(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /** */
    public static FilterConnection valueOfLabel(String label) {
        return Labeled.valueOfLabel(FilterConnection.class, label);
    }
}
