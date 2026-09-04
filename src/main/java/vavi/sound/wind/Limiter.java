/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Limiter. holds a signal inside full scale without squaring it off.
 * <p>
 * a program is free to run four oscillators of three slots each into both busses and then
 * into both amps, which is what the CLIP lamp on the IFW panel is for, and the mix of
 * sixteen parts on top of that has nowhere to go either. rather than let any of it clip,
 * everything below -1.4 dBFS passes untouched and the rest bends into a knee that
 * approaches full scale without passing it.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public final class Limiter {

    /** where the knee starts, about -1.4 dBFS, over where a tone is aimed */
    private static final float KNEE = .85f;

    /** */
    private Limiter() {
    }

    /** */
    public static float process(float value) {
        float magnitude = Math.abs(value);
        if (magnitude <= KNEE) {
            return value;
        }
        float over = (magnitude - KNEE) / (1 - KNEE);
        // over / sqrt(1 + over^2), taken the way round that does not square to infinity
        float shaped = over < 1
                ? over / (float) Math.sqrt(1 + over * over)
                : 1 / (float) Math.sqrt(1 + 1 / (over * over));
        return Math.signum(value) * (KNEE + (1 - KNEE) * shaped);
    }
}
