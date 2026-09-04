/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * IfwEngineTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
class IfwEngineTest {

    static final float SAMPLE_RATE = 44100;

    final float[] left = new float[(int) SAMPLE_RATE];
    final float[] right = new float[(int) SAMPLE_RATE];

    /** */
    static boolean soundsExist() {
        return IfwProgramTest.soundsExist();
    }

    /** */
    static Stream<Path> tones() throws IOException {
        return IfwProgramTest.tones();
    }

    /** an engine on the IFW initial program */
    static IfwEngine engine() {
        return new IfwEngine(SAMPLE_RATE);
    }

    /** one plain sine, wide open, so that a measurement means what it looks like */
    static IfwProgram sine() {
        IfwProgram program = new IfwProgram();
        program.set(IfwParameter.Osc1SawLevel, 0);
        program.set(IfwParameter.Osc1TriLevel, 10);
        program.set(IfwParameter.Osc1Waveform2, Waveform.SIN);
        program.set(IfwParameter.Osc2SawLevel, 0);
        program.set(IfwParameter.Filter1Frequency, 10);
        program.set(IfwParameter.Filter1Breath, 0);
        program.set(IfwParameter.Mod1Depth, 0);
        return program;
    }

    /** */
    float rms(int offset, int length) {
        double sum = 0;
        for (int i = 0; i < length; i++) {
            sum += left[offset + i] * left[offset + i];
        }
        return (float) Math.sqrt(sum / length);
    }

    /** how many cycles the left channel completes over a stretch of the buffer */
    int cycles(int offset, int length) {
        int count = 0;
        for (int i = offset + 1; i < offset + length; i++) {
            if (left[i - 1] < 0 && left[i] >= 0) {
                count++;
            }
        }
        return count;
    }

    /** */
    void render(IfwEngine engine, int offset, int length) {
        engine.render(left, right, offset, length);
    }

    @Test
    void aNoteSoundsAndStopsWhenItIsLetGo() {
        IfwEngine engine = engine();
        assertFalse(engine.isActive());

        engine.setBreath(1);
        engine.noteOn(60, 100);
        assertTrue(engine.isActive());
        render(engine, 0, 22050);
        assertTrue(rms(11025, 11025) > .01f, "the note should be audible");

        engine.setBreath(0);
        engine.noteOff(60);
        render(engine, 22050, 22050);
        assertTrue(rms(40000, 4000) < 1e-4f, "the tail should be gone");
        assertFalse(engine.isActive(), "the engine should have gone idle");
    }

    @Test
    void theBreathControllerDrivesTheLevel() {
        float quiet = level(.25f);
        float half = level(.5f);
        float full = level(1);
        assertEquals(0, level(0), 1e-6f, "no breath should be silent");
        assertTrue(quiet < half, quiet + " should be under " + half);
        assertTrue(half < full, half + " should be under " + full);
    }

    /** the steady level of the initial program at one breath position */
    private float level(float breath) {
        IfwEngine engine = engine();
        engine.setBreath(breath);
        engine.noteOn(60, 100);
        Arrays.fill(left, 0);
        render(engine, 0, 22050);
        return rms(11025, 11025);
    }

    @Test
    void velocityStandsInUntilABreathControllerIsHeard() {
        IfwEngine engine = engine();
        engine.noteOn(60, 127);
        render(engine, 0, 22050);
        assertTrue(rms(11025, 11025) > .01f, "a keyboard should still be audible");
    }

    @Test
    void aNoteIsAtThePitchItAsksFor() {
        IfwEngine engine = engine();
        engine.setProgram(sine());
        engine.setBreath(1);
        engine.noteOn(69, 100);
        render(engine, 0, (int) SAMPLE_RATE);
        // A4 is 440 Hz, so 220 cycles over the second half second
        assertEquals(220, cycles(22050, 22050), 2);
    }

    @Test
    void transposeMovesThePitch() {
        IfwProgram program = sine();
        program.set(IfwParameter.Transpose, -12);
        IfwEngine engine = engine();
        engine.setProgram(program);
        engine.setBreath(1);
        engine.noteOn(69, 100);
        render(engine, 0, (int) SAMPLE_RATE);
        // an octave under A4 is 220 Hz, so 110 cycles over half a second
        assertEquals(110, cycles(22050, 22050), 2);
    }

    @Test
    void aLegatoNoteGlidesToItsPitch() {
        IfwProgram program = sine();
        program.set(IfwParameter.GlideSpeed, 5);
        IfwEngine engine = engine();
        engine.setProgram(program);
        engine.setBreath(1);

        engine.noteOn(60, 100);
        render(engine, 0, 4410);
        engine.noteOn(72, 100);
        render(engine, 4410, 26460);

        // 50 ms into the glide the pitch is somewhere between C4 and C5
        int during = cycles(4410, 2205);
        // by half a second later it has arrived at C5, 523 Hz, so 26 cycles per 50 ms
        int after = cycles(28000, 2205);
        assertTrue(during > 13 && during < 26, "mid glide, but " + during + " cycles per 50 ms");
        assertEquals(26, after, 2);
    }

    @Test
    void aPatchThatWouldClipIsHeldInsideFullScale() {
        IfwProgram program = new IfwProgram();
        // every oscillator, every slot, into both busses and so into both amps
        for (int i = 1; i <= 4; i++) {
            program.set(IfwParameter.valueOf("Osc" + i + "SawLevel"), 10);
            program.set(IfwParameter.valueOf("Osc" + i + "TriLevel"), 10);
            program.set(IfwParameter.valueOf("Osc" + i + "Output1"), Bool.On);
            program.set(IfwParameter.valueOf("Osc" + i + "Output2"), Bool.On);
        }
        program.set(IfwParameter.FilterConnection, FilterConnection.Parallel2);
        program.set(IfwParameter.Filter1Frequency, 10);
        program.set(IfwParameter.Filter2Frequency, 10);

        IfwEngine engine = engine();
        engine.setProgram(program);
        engine.setBreath(1);
        engine.noteOn(60, 127);
        render(engine, 0, 22050);

        float peak = 0;
        for (int i = 0; i < 22050; i++) {
            peak = Math.max(peak, Math.abs(left[i]));
        }
        assertTrue(peak > .5f, "it should still be loud, but peaks at " + peak);
        assertTrue(peak <= 1, "it should not leave full scale, but peaks at " + peak);
    }

    @Test
    void anExternalInputWaveformReadsTheHost() {
        IfwProgram program = sine();
        program.set(IfwParameter.Osc1Waveform2, Waveform.ExtInL);

        IfwEngine engine = engine();
        engine.setProgram(program);
        engine.setBreath(1);
        engine.noteOn(60, 100);
        for (int i = 0; i < 22050; i++) {
            engine.setExternalInput((float) Math.sin(2 * Math.PI * 440 * i / SAMPLE_RATE), 0);
            render(engine, i, 1);
        }
        assertTrue(rms(11025, 11025) > .01f, "the host input should come through");
    }

    @Test
    void aProgramWithAnInstrumentWaveSounds() {
        IfwProgram program = sine();
        program.set(IfwParameter.Osc1TriLevel, 0);
        program.set(IfwParameter.Osc1PwmLevel, 10);
        program.set(IfwParameter.Osc1Waveform3, PcmWaveform.Clarinet);

        IfwEngine engine = engine();
        engine.setProgram(program);
        engine.setBreath(1);
        engine.noteOn(60, 100);
        render(engine, 0, 22050);
        assertTrue(rms(11025, 11025) > .01f, "the instrument wave should be audible");
    }

    @Test
    void theBreathResponseSpreadsTheLoudnessOverTheTravel() {
        IfwProgram program = sine();  // its filter is wide open, so this is the amp alone
        assertEquals(BreathResponse.DEFAULT, engine().getBreathResponse());

        float straight = dB(program, BreathResponse.LINEAR, .5f);
        float shaped = dB(program, BreathResponse.DEFAULT, .5f);
        // straight through, half breath is the 6 dB that makes the top of the sensor dull
        assertEquals(-6.02f, straight, .3f);
        assertEquals(-11.6f, shaped, .5f);
    }

    @Test
    void theBreathResponseLeavesTheTonesOwnFiltersAlone() {
        // the amp is taken out of it, so what is left answers breath only through filter 1
        IfwProgram program = sine();
        program.set(IfwParameter.Amp1Breath, 0);
        program.set(IfwParameter.Amp2Breath, 0);
        program.set(IfwParameter.Filter1Frequency, 0);
        program.set(IfwParameter.Filter1Breath, 7);

        assertEquals(dB(program, BreathResponse.LINEAR, .5f), dB(program, BreathResponse.DEFAULT, .5f), 1e-3f);
        assertEquals(dB(program, BreathResponse.LINEAR, .2f), dB(program, BreathResponse.DEFAULT, .2f), 1e-3f);
    }

    /** the steady level at one breath position, in dB under the same tone at full breath */
    private float dB(IfwProgram program, BreathResponse response, float breath) {
        return 20 * (float) Math.log10(level(program, response, breath) / level(program, response, 1));
    }

    /** */
    private float level(IfwProgram program, BreathResponse response, float breath) {
        IfwEngine engine = engine();
        engine.setProgram(program);
        engine.setBreathResponse(response);
        engine.setBreath(breath);
        engine.noteOn(69, 100);
        Arrays.fill(left, 0);
        render(engine, 0, 22050);
        return rms(11025, 11025);
    }

    @ParameterizedTest
    @MethodSource("tones")
    @EnabledIf("soundsExist")
    void everyInstalledToneRendersCleanly(Path path) throws Exception {
        IfwProgram program;
        try (InputStream in = Files.newInputStream(path)) {
            program = IfwProgram.read(in);
        }
        IfwEngine engine = engine();
        engine.setProgram(program);
        engine.setBreath(.8f);
        engine.noteOn(72, 100);
        Arrays.fill(left, 0);
        Arrays.fill(right, 0);
        render(engine, 0, 22050);
        engine.setBreath(0);
        engine.noteOff(72);
        render(engine, 22050, 22050);

        for (int i = 0; i < left.length; i++) {
            assertTrue(Float.isFinite(left[i]) && Float.isFinite(right[i]),
                    program.getName() + " went off the rails at sample " + i);
            assertTrue(Math.abs(left[i]) <= 1 && Math.abs(right[i]) <= 1,
                    program.getName() + " left full scale at sample " + i);
        }

        // the longest release the installed tones ask for is a bit over a second
        for (int second = 0; second < 4 && engine.isActive(); second++) {
            render(engine, 0, left.length);
        }
        assertFalse(engine.isActive(), program.getName() + " never let go of the note");
    }
}
