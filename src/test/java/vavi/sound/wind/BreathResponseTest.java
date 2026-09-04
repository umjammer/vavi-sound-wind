/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * BreathResponseTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
class BreathResponseTest {

    /** */
    static float dB(float level) {
        return (float) (20 * Math.log10(level));
    }

    @Test
    void theLinearOneLeavesTheControllerAlone() {
        for (int i = 0; i <= 127; i++) {
            float breath = i / 127f;
            assertEquals(breath, BreathResponse.LINEAR.apply(breath), 1e-6f);
        }
    }

    @Test
    void everyCurveStillRunsFromSilenceToFullScale() {
        // it is built out of static fields, so this is also what says they were ready in time
        for (float depth : new float[] {-24, -6, 0, 6, 18, 36, 96}) {
            BreathResponse response = new BreathResponse(0, 1, depth);
            assertEquals(0, response.apply(0), 1e-6f, response + " at no breath");
            assertEquals(1, response.apply(1), 1e-6f, response + " at full breath");
            assertEquals(0, BreathResponse.DEFAULT.apply(0), 1e-6f);
            assertEquals(1, BreathResponse.DEFAULT.apply(1), 1e-6f);
        }
    }

    @Test
    void theDefaultGivesTheTopOfTheTravelSomethingToDo() {
        // straight through, the whole top half of the sensor is worth 6 dB and no more
        assertEquals(-6.02f, dB(BreathResponse.LINEAR.apply(.5f)), .01f);
        assertEquals(-11.6f, dB(BreathResponse.DEFAULT.apply(.5f)), .1f);
    }

    @Test
    void theDefaultSpendsItsDecibelsEvenlyOverTheTravelItShapes() {
        // an equal step of breath should be about an equal step in dB, twice the size of
        // the one the controller gives on its own. the bottom is steeper, so that a note
        // can stop, which is why this looks at the travel a player actually holds
        float smallest = Float.MAX_VALUE, largest = 0;
        for (int i = 5; i <= 10; i++) {
            float step = dB(BreathResponse.DEFAULT.apply(i / 10f))
                    - dB(BreathResponse.DEFAULT.apply((i - 1) / 10f));
            smallest = Math.min(smallest, step);
            largest = Math.max(largest, step);
        }
        assertTrue(largest - smallest < 1.2f, "uneven, " + smallest + " .. " + largest + " dB a step");
        assertTrue(smallest > 2, "flat at the top, only " + smallest + " dB a step");
    }

    @Test
    void aPositiveDepthSpreadsAndANegativeOnePulls() {
        assertTrue(new BreathResponse(0, 1, 18).apply(.5f) < BreathResponse.LINEAR.apply(.5f));
        assertTrue(new BreathResponse(0, 1, -18).apply(.5f) > BreathResponse.LINEAR.apply(.5f));
    }

    @Test
    void everyCurveOnlyEverRises() {
        for (float depth : new float[] {-18, 0, 18, 48}) {
            BreathResponse response = new BreathResponse(0, 1, depth);
            float previous = -1;
            for (int i = 0; i <= 127; i++) {
                float level = response.apply(i / 127f);
                assertTrue(level > previous, response + " went back at " + i);
                previous = level;
            }
        }
    }

    @Test
    void theCalibrationUsesTheWholeOfWhatTheSensorReallySends() {
        // a sensor that rests at 10 and tops out at 100 of 127
        BreathResponse response = new BreathResponse(10 / 127f, 100 / 127f, 0);
        assertEquals(0, response.apply(0), 1e-6f);
        assertEquals(0, response.apply(10 / 127f), 1e-6f);
        assertEquals(.5f, response.apply(55 / 127f), 1e-3f);
        assertEquals(1, response.apply(100 / 127f), 1e-6f);
        assertEquals(1, response.apply(1), 1e-6f);
    }

    @Test
    void aCurveIsReadBackFromWhatItWrote() {
        BreathResponse response = new BreathResponse(.1f, .9f, 24);
        assertEquals(response, BreathResponse.valueOf(response.toString()));
        assertEquals(response.hashCode(), BreathResponse.valueOf(response.toString()).hashCode());
        assertNotEquals(response, BreathResponse.LINEAR);
        // a depth on its own, the rest left as it comes
        assertEquals(new BreathResponse(0, 1, 12), BreathResponse.valueOf(",,12"));
    }

    @Test
    void somethingThatIsNotACurveIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BreathResponse.valueOf("18"));
        assertThrows(IllegalArgumentException.class, () -> BreathResponse.valueOf("a,b,c"));
        assertThrows(IllegalArgumentException.class, () -> new BreathResponse(.5f, .5f, 0));
        assertThrows(IllegalArgumentException.class, () -> new BreathResponse(-.1f, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new BreathResponse(0, 1, 200));
    }
}
