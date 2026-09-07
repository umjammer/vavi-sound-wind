/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.sound.midi.Instrument;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * IfwProgramTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
class IfwProgramTest {

    /** where IFW keeps its tones, when the plug-in is installed here */
    static final Path SOUNDS = Path.of(System.getProperty("user.home"), "Documents/IFW/Sounds");

    /** */
    static boolean soundsExist() {
        return Files.isDirectory(SOUNDS);
    }

    /** the tones IFW has installed */
    static Stream<Path> tones() throws IOException {
        try (Stream<Path> stream = Files.walk(SOUNDS)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".xml")).toList().stream();
        }
    }

    /** a tone file, the parameters given as {@code <Tag>value</Tag>} */
    static String tone(String name, String... parameters) {
        return """
                <?xml version="1.0" encoding="utf-8" standalone="yes"?>
                <IFWState>
                    <CurrentProgram>
                        <IFWProgram>
                            <Name>%s</Name>
                            <Author>nsano</Author>
                            <Comment>a comment</Comment>
                            <Parameters>%s</Parameters>
                        </IFWProgram>
                    </CurrentProgram>
                </IFWState>
                """.formatted(name, String.join("", parameters));
    }

    /** */
    static IfwProgram read(String text) throws IOException {
        return IfwProgram.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void theTableHasTheTwoHundredSlotsIfwWrites() {
        assertEquals(200, IfwParameter.values().length);
        assertSame(IfwParameter.Osc1Sync, IfwParameter.values()[0]);
        assertSame(IfwParameter.Dummy199, IfwParameter.values()[199]);
    }

    @Test
    void aNewProgramIsTheIfwInitialProgram() {
        IfwProgram program = new IfwProgram();
        assertEquals(10, program.get(IfwParameter.Osc1SawLevel));
        assertEquals(1.7f, program.get(IfwParameter.Filter1Frequency));
        assertEquals(7, program.get(IfwParameter.Filter1Breath));
        assertSame(FilterType.LPF12, program.choice(IfwParameter.Filter1Type, FilterType.class));
        assertSame(ModDestination.Osc1Pitch, program.choice(IfwParameter.Mod1Destination, ModDestination.class));
        assertTrue(program.is(IfwParameter.Osc1Output1));
        assertTrue(program.diff().isEmpty());
    }

    @Test
    void aProgramSurvivesAWriteAndARead() throws Exception {
        IfwProgram program = new IfwProgram();
        program.setName("Written By The Test");
        program.set(IfwParameter.Osc2Semi, 7);
        program.set(IfwParameter.Filter2Type, FilterType.BPF12);
        program.set(IfwParameter.Lfo1Beat, LfoBeat.B1_8T);
        program.set(IfwParameter.Amp1BreathSubMod, BreathSubMod.BRxEG3);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        program.write(out);
        IfwProgram read = IfwProgram.read(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(program, read);
        assertEquals("Written By The Test", read.getName());
        assertSame(LfoBeat.B1_8T, read.choice(IfwParameter.Lfo1Beat, LfoBeat.class));
        assertSame(BreathSubMod.BRxEG3, read.choice(IfwParameter.Amp1BreathSubMod, BreathSubMod.class));
    }

    @Test
    void aSlotAnOlderIfwNeverWroteKeepsItsDefault() throws Exception {
        IfwProgram program = read(tone("Half A Tone", "<Osc1SawLevel>3.00</Osc1SawLevel>"));
        assertEquals(3, program.get(IfwParameter.Osc1SawLevel));
        assertEquals(IfwParameter.GateOpenSmoothTime.defaultValue(), program.get(IfwParameter.GateOpenSmoothTime));
    }

    @Test
    void aParameterOrALabelOfANewerIfwIsIgnored() throws Exception {
        IfwProgram program = read(tone("From The Future",
                "<Osc9Warp>1.00</Osc9Warp>",
                "<Filter1Type>LPF96</Filter1Type>",
                "<Osc1SawLevel>4.00</Osc1SawLevel>"));
        assertSame(FilterType.LPF12, program.choice(IfwParameter.Filter1Type, FilterType.class));
        assertEquals(4, program.get(IfwParameter.Osc1SawLevel));
        assertNull(IfwParameter.valueOfId("Osc9Warp"));
    }

    @Test
    void bothSpellingsOfABreathSubModAreRead() throws Exception {
        assertSame(BreathSubMod.BRxEG1, read(tone("Old", "<Filter1BreathSubMod>BRxEG1</Filter1BreathSubMod>"))
                .choice(IfwParameter.Filter1BreathSubMod, BreathSubMod.class));
        assertSame(BreathSubMod.BRxEG1, read(tone("New", "<Filter1BreathSubMod>Breath x EG 1</Filter1BreathSubMod>"))
                .choice(IfwParameter.Filter1BreathSubMod, BreathSubMod.class));
    }

    @Test
    void aWaveformOlderThanItsLabelsIsReadAsAnOrdinal() throws Exception {
        // versions before the instrument waves wrote the slot as a plain number
        assertSame(Waveform.TRI, read(tone("Old", "<Osc1Waveform2>3.00</Osc1Waveform2>"))
                .choice(IfwParameter.Osc1Waveform2, Waveform.class));
        assertSame(Waveform.Oboe, read(tone("New", "<Osc1Waveform2>Oboe</Osc1Waveform2>"))
                .choice(IfwParameter.Osc1Waveform2, Waveform.class));
    }

    @Test
    void theNulIfwPadsAToneFileWithIsNotAParseError() throws Exception {
        assertEquals("Padded", read(tone("Padded") + "\0").getName());
    }

    @Test
    void aBareAmpersandInANameIsNotAParseError() throws Exception {
        // IFW writes the name straight out, so a tone may well be called "Unaji&Travel"
        assertEquals("Unaji&Travel", read(tone("Unaji&Travel")).getName());
        // one that is already escaped still means what it says
        assertEquals("A&B", read(tone("A&amp;B")).getName());
    }

    @Test
    void aToneWithoutTheCurrentProgramWrapperIsRead() throws Exception {
        IfwProgram program = read("""
                <?xml version="1.0" encoding="utf-8" standalone="yes"?>
                <IFWState>
                        <IFWProgram>
                            <Name>ARCADIA</Name>
                            <Dummy1>hahaha</Dummy1>
                            <Author>ring2</Author>
                            <Parameters><Osc1SawLevel>2.00</Osc1SawLevel></Parameters>
                        </IFWProgram>
                </IFWState>
                """);
        assertEquals("ARCADIA", program.getName());
        assertEquals("ring2", program.getAuthor());
        assertEquals(2, program.get(IfwParameter.Osc1SawLevel));
    }

    @Test
    void somethingThatIsNotAToneIsRejected() {
        assertThrows(IOException.class, () -> read("<html><body>no</body></html>"));
        assertThrows(IOException.class, () -> read(""));
    }

    @Test
    void aStreamIsRecognizedWithoutBeingConsumed() throws Exception {
        InputStream in = new BufferedInputStream(
                new ByteArrayInputStream(tone("Sniffed").getBytes(StandardCharsets.UTF_8)));
        assertTrue(IfwProgram.isIfw(in));
        assertEquals("Sniffed", IfwProgram.read(in).getName());
    }

    @ParameterizedTest
    @MethodSource("tones")
    @EnabledIf("soundsExist")
    void everyInstalledToneIsReadAndWrittenBack(Path path) throws Exception {
        IfwProgram program;
        try (InputStream in = Files.newInputStream(path)) {
            program = IfwProgram.read(in);
        }
        assertNotNull(program.getName());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        program.write(out);
        assertEquals(program, IfwProgram.read(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    @EnabledIf("soundsExist")
    void theInstalledTonesBecomeASoundbank() throws Exception {
        IfwSoundbank soundbank = IfwSoundbank.read(SOUNDS);
        List<Instrument> instruments = List.of(soundbank.getInstruments());
        assertTrue(instruments.size() > 1, "only " + instruments.size() + " tone(s)");
        for (Instrument instrument : instruments) {
            assertNotNull(soundbank.getInstrument(instrument.getPatch()));
            assertInstanceOf(IfwProgram.class, instrument.getData());
        }
    }
}
