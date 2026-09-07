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
 * WaveBankTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-07 nsano initial version <br>
 */
class WaveBankTest {

    static final WaveBank bank = WaveBank.getInstance();

    /** the amplitude of the nth harmonic of a wave at a note */
    static double harmonic(int wave, int note, int n) {
        int size = 4096;
        double re = 0, im = 0;
        for (int i = 0; i < size; i++) {
            double v = bank.value(wave, note, (float) i / size);
            double angle = 2 * Math.PI * n * i / size;
            re += v * Math.cos(angle);
            im += v * Math.sin(angle);
        }
        return 2 * Math.hypot(re, im) / size;
    }

    @Test
    void aNoteKeepsTheHarmonicsThatFitUnderTwentyTwoKilohertz() {
        // A4 is 440 Hz, and fifty of those are 22 kHz
        assertEquals(50, bank.harmonics(69));
        // the top of a keyboard has almost nothing left to keep
        assertEquals(1, bank.harmonics(127));
        // and the bottom of the table keeps everything
        assertEquals(2696, bank.harmonics(0));
    }

    @Test
    void aNoteOverTheTableIsHeldAtItsTop() {
        assertEquals(bank.harmonics(WaveBank.NOTES - 1), bank.harmonics(WaveBank.NOTES + 100));
    }

    @Test
    void theSawtoothFallsAwayAsOneOverN() {
        double first = harmonic(0, 69, 1);
        for (int n = 2; n <= 8; n++) {
            assertEquals(first / n, harmonic(0, 69, n), first * .05,
                    "the " + n + "th harmonic of the sawtooth");
        }
    }

    @Test
    void theTriangleKeepsOnlyItsOddHarmonics() {
        double first = harmonic(1, 69, 1);
        for (int n = 3; n <= 9; n += 2) {
            assertEquals(first / (n * n), harmonic(1, 69, n), first * .05,
                    "the " + n + "th harmonic of the triangle");
        }
        for (int n = 2; n <= 8; n += 2) {
            assertTrue(harmonic(1, 69, n) < first * .02, "the triangle should have no " + n + "th");
        }
    }

    @Test
    void theSineHasNothingButItsFundamental() {
        assertEquals(1, harmonic(2, 69, 1), .01);
        for (int n = 2; n <= 8; n++) {
            assertTrue(harmonic(2, 69, n) < .01, "the sine should have no " + n + "th");
        }
    }

    @Test
    void nothingAboveTheLimitOfANoteSurvives() {
        // the note two octaves over A4 keeps a dozen harmonics, and nothing over them
        int limit = bank.harmonics(93);
        assertTrue(harmonic(0, 93, limit) > 0, "the last harmonic should be there");
        assertTrue(harmonic(0, 93, limit + 2) < .01, "and nothing over it");
    }

    @Test
    void everyWaveStaysInsideFullScale() {
        for (int wave = 0; wave < WaveBank.WAVES; wave++) {
            for (int note : new int[] {0, 40, 69, 100, 150, 191}) {
                for (int i = 0; i < 512; i++) {
                    float v = bank.value(wave, note, i / 512f);
                    // a wave is scaled by the loudest of its own tables, and taking harmonics
                    // out of an instrument can leave a higher table cresting a little over it
                    assertTrue(Math.abs(v) <= 1.06f, "wave " + wave + " note " + note + " at " + v);
                }
            }
        }
    }

    @Test
    void theClassicShapesStayInsideFullScale() {
        for (int wave = 0; wave <= 2; wave++) {
            for (int note = 0; note < WaveBank.NOTES; note += 7) {
                for (int i = 0; i < 256; i++) {
                    float v = bank.value(wave, note, i / 256f);
                    assertTrue(Math.abs(v) <= 1.001f, "wave " + wave + " note " + note + " at " + v);
                }
            }
        }
    }

    @Test
    void aClarinetHasFarMoreOddHarmonicsThanEven() {
        double third = harmonic(Waveform.Clarinet.wave(), 69, 3);
        double second = harmonic(Waveform.Clarinet.wave(), 69, 2);
        assertTrue(third > second * 4, "a clarinet's third should tower over its second");
    }

    @Test
    void aFractionOfASemitoneIsWorthWhatItShouldBe() {
        assertEquals(1, WaveBank.fine(0), 1e-6);
        assertEquals(Math.pow(2, .5 / 12), WaveBank.fine(.5f), 1e-3);
        assertEquals(Math.pow(2, 1 / 12d), WaveBank.fine(1), 1e-2);
    }

    @Test
    void aNoteIsAtTheFrequencyItShouldBe() {
        assertEquals(440, WaveBank.frequency(69), 1e-3);
        assertEquals(261.626, WaveBank.frequency(60), 1e-3);
        // where a cutoff of zero sits, three octaves under the bottom of a keyboard
        assertEquals(2.04, WaveBank.frequency(-24), .01);
    }
}
