/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.wind;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import vavi.sound.wind.IfwEngine;
import vavi.sound.wind.IfwParameter;
import vavi.sound.wind.IfwProgram;
import vavi.sound.wind.IfwSoundbank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * WindSynthesizerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
class WindSynthesizerTest {

    /** the device the provider offers, or null when the service is not registered */
    static MidiDevice.Info info() throws Exception {
        return Arrays.stream(MidiSystem.getMidiDeviceInfo())
                .filter(i -> "Wind MIDI Synthesizer".equals(i.getName()))
                .findFirst().orElse(null);
    }

    @Test
    void midiSystemFindsTheSynthesizer() throws Exception {
        MidiDevice.Info info = info();
        assertNotNull(info, "the SPI service should be registered");
        assertInstanceOf(WindSynthesizer.class, MidiSystem.getMidiDevice(info));
    }

    @Test
    void theSynthesizerHasOneMonophonicPartPerChannel() {
        Synthesizer synthesizer = new WindSynthesizer();
        assertEquals(16, synthesizer.getChannels().length);
        assertEquals(16, synthesizer.getMaxPolyphony());
        for (MidiChannel channel : synthesizer.getChannels()) {
            assertTrue(channel.getMono());
        }
    }

    @Test
    void aToneCanBeLoadedAndSwitchedTo() {
        Synthesizer synthesizer = new WindSynthesizer();
        IfwProgram program = new IfwProgram();
        program.setName("Loaded By The Test");
        program.set(IfwParameter.Osc1Semi, 12);

        Soundbank soundbank = new IfwSoundbank();
        assertTrue(synthesizer.isSoundbankSupported(soundbank));
        assertTrue(synthesizer.loadInstrument(
                new IfwSoundbank.IfwInstrument(soundbank, new Patch(0, 5), program)));

        synthesizer.getChannels()[0].programChange(5);
        assertEquals(5, synthesizer.getChannels()[0].getProgram());
    }

    @Test
    void theSoundbankReaderReadsAToneFile(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("A Tone.xml");
        IfwProgram program = new IfwProgram();
        program.setName("A Tone");
        try (OutputStream out = Files.newOutputStream(file)) {
            program.write(out);
        }

        Soundbank soundbank = MidiSystem.getSoundbank(file.toFile());
        assertInstanceOf(IfwSoundbank.class, soundbank);
        Instrument[] instruments = soundbank.getInstruments();
        assertEquals(1, instruments.length);
        assertEquals("A Tone", instruments[0].getName());
    }

    @Test
    void theSoundbankReaderLeavesSomethingElseAlone(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("not-a-tone.xml");
        Files.writeString(file, "<html><body>no</body></html>");
        assertNull(new WindSoundbankReader().getSoundbank(file.toFile()));
    }

    @Test
    void aFolderOfTonesBecomesBanks(@TempDir Path folder) throws Exception {
        Files.createDirectory(folder.resolve("bank one"));
        write(folder.resolve("top.xml"), "Top");
        write(folder.resolve("bank one/first.xml"), "First");
        write(folder.resolve("bank one/second.xml"), "Second");

        IfwSoundbank soundbank = IfwSoundbank.read(folder);
        assertEquals(3, soundbank.getInstruments().length);
        assertEquals("Top", soundbank.getInstrument(new Patch(0, 0)).getName());
        assertEquals("First", soundbank.getInstrument(new Patch(1, 0)).getName());
        assertEquals("Second", soundbank.getInstrument(new Patch(1, 1)).getName());
    }

    /** */
    private static void write(Path file, String name) throws Exception {
        IfwProgram program = new IfwProgram();
        program.setName(name);
        try (OutputStream out = Files.newOutputStream(file)) {
            program.write(out);
        }
    }

    /** the universal realtime device control, {@code F0 7F <device> 04 01 <lsb> <msb> F7} */
    static SysexMessage masterVolume(float volume) throws Exception {
        int value = Math.round(16383 * volume);
        byte[] message = {(byte) 0xf0, 0x7f, 0x7f, 0x04, 0x01,
                (byte) (value & 0x7f), (byte) (value >> 7 & 0x7f), (byte) 0xf7};
        return new SysexMessage(message, message.length);
    }

    @Test
    void theMasterVolumeIsSetByTheUniversalRealtimeSysex() throws Exception {
        WindSynthesizer synthesizer = new WindSynthesizer();
        Receiver receiver = synthesizer.getReceiver();
        assertEquals(1, synthesizer.getMasterVolume(), "it should start wide open");

        for (float volume : new float[] {.5f, 0, .25f, 1}) {
            receiver.send(masterVolume(volume), -1);
            assertEquals(volume, synthesizer.getMasterVolume(), 1e-3f);
        }
    }

    @Test
    void aSequencerShapeOfTheSameSysexAlsoWorks() throws Exception {
        // what a standard MIDI file hands over: the body alone, no F0 and no F7
        WindSynthesizer synthesizer = new WindSynthesizer();
        int value = 16383 / 4;
        byte[] body = {0x7f, 0x7f, 0x04, 0x01, (byte) (value & 0x7f), (byte) (value >> 7 & 0x7f)};
        SysexMessage message = new SysexMessage();
        message.setMessage(0xf0, body, body.length);

        synthesizer.getReceiver().send(message, -1);
        assertEquals(.25f, synthesizer.getMasterVolume(), 1e-3f);
    }

    @Test
    void somethingThatIsNotAMasterVolumeLeavesItAlone() throws Exception {
        WindSynthesizer synthesizer = new WindSynthesizer();
        Receiver receiver = synthesizer.getReceiver();
        receiver.send(masterVolume(.5f), -1);

        // GM system on, which shares the universal non realtime shape but is not this
        byte[] other = {(byte) 0xf0, 0x7e, 0x7f, 0x09, 0x01, (byte) 0xf7};
        receiver.send(new SysexMessage(other, other.length), -1);
        assertEquals(.5f, synthesizer.getMasterVolume(), 1e-3f);
    }

    /**
     * Plays a phrase the way a wind controller would, with breath rather than velocity.
     * <p>
     * this one opens the audio device, so it only runs when it is asked for.
     */
    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void playsAPhrase() throws Exception {
        MidiDevice device = MidiSystem.getMidiDevice(info());
        device.open();
        try (Receiver receiver = device.getReceiver()) {
            int[] notes = {60, 62, 64, 65, 67, 69, 71, 72};
            for (int note : notes) {
                receiver.send(new ShortMessage(ShortMessage.NOTE_ON, 0, note, 100), -1);
                for (int i = 0; i <= 100; i++) {
                    int breath = (int) (127 * Math.sin(Math.PI * i / 100));
                    receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0,
                            IfwEngine.BREATH_CONTROLLER, breath), -1);
                    Thread.sleep(3);
                }
                receiver.send(new ShortMessage(ShortMessage.NOTE_OFF, 0, note, 0), -1);
            }
            Thread.sleep(500);
        } finally {
            device.close();
        }
    }
}
