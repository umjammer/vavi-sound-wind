/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.util.Arrays;


/**
 * Labeled. an enum constant that is written into an IFW tone file by a display label.
 * <p>
 * the labels are not java identifiers ("Parallel 1", "1/4.", "Ext.In L+R" ...), and IFW
 * renamed some of them between versions, so a constant carries its primary label plus
 * every legacy spelling as an alias.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public interface Labeled {

    /** the label written into a tone file */
    String label();

    /** labels of older IFW versions that mean the same constant */
    default String[] aliases() {
        return new String[0];
    }

    /** whether this constant is written as the given label */
    default boolean is(String label) {
        return label().equalsIgnoreCase(label) || Arrays.stream(aliases()).anyMatch(a -> a.equalsIgnoreCase(label));
    }

    /**
     * Finds the constant written as {@code label}.
     * <p>
     * a label IFW did not know yet is stored as a plain number, so a numeric label is
     * accepted as an ordinal as well.
     *
     * @throws IllegalArgumentException no constant is written as the label
     */
    static <E extends Enum<E> & Labeled> E valueOfLabel(Class<E> type, String label) {
        String l = label.trim();
        for (E e : type.getEnumConstants()) {
            if (e.is(l)) {
                return e;
            }
        }
        try {
            int ordinal = (int) Float.parseFloat(l);
            E[] constants = type.getEnumConstants();
            if (ordinal >= 0 && ordinal < constants.length) {
                return constants[ordinal];
            }
        } catch (NumberFormatException e) {
            // not an ordinal either, fall through
        }
        throw new IllegalArgumentException("no " + type.getSimpleName() + " labeled: " + label);
    }
}
