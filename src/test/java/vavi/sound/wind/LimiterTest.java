/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * LimiterTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
class LimiterTest {

    @Test
    void anythingUnderTheKneePassesUntouched() {
        for (float value = -.7f; value <= .7f; value += .01f) {
            assertEquals(value, Limiter.process(value), 1e-6f);
        }
    }

    @Test
    void nothingEverLeavesFullScale() {
        for (float value : new float[] {1, 2, 4, 16, 1000, Float.MAX_VALUE}) {
            assertTrue(Limiter.process(value) <= 1, value + " came out at " + Limiter.process(value));
            assertTrue(Limiter.process(-value) >= -1, -value + " came out at " + Limiter.process(-value));
        }
        // it approaches full scale rather than arriving at it, until float runs out of room
        assertTrue(Limiter.process(4) < 1);
        assertTrue(Limiter.process(4) > .9f);
    }

    @Test
    void itOnlyEverRisesAndKeepsItsSign() {
        float previous = -1;
        for (float value = -8; value <= 8; value += .01f) {
            float limited = Limiter.process(value);
            assertTrue(limited > previous, "went back at " + value);
            assertEquals(Math.signum(value), Math.signum(limited), 1e-6f, "flipped at " + value);
            previous = limited;
        }
    }
}
