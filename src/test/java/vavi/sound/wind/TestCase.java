/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

import vavi.sound.midi.MidiUtil.MidiMatcher;
import vavi.sound.midi.wind.WindSoundbankReader;
import vavi.sound.midi.wind.WindSynthesizer;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static vavi.sound.midi.MidiUtil.getMidiDevice;
import static vavi.sound.midi.MidiUtil.volume;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "in.name")
    String inName;

    @Property(name = "in.vendor")
    String inVendor;

    @Property(name = "in.description")
    String inDescription;

    @Property(name = "out.name")
    String outName;

    @Property(name = "out.vendor")
    String outVendor;

    @Property(name = "out.description")
    String outDescription;

    @Property
    int program;

    @Property
    String tone;

    @Property(name = "au.effects")
    String effects = "appl:mrev?Dry/Wet Mix=20,appl:dely?Dry/Wet Mix=15;Delay Time=0.25;Feedback=20";

    @Property(name = "vavi.test.volume.midi")
    float midiVolume = 0.2f;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        outName = outName != null ? (outName.isEmpty() ? null : outName) : null;
        outVendor = outVendor != null ? (outVendor.isEmpty() ? null : outVendor) : null;
        outDescription = outDescription != null ? (outDescription.isEmpty() ? null : outDescription) : null;

        if (System.getProperty("os.name").startsWith("Mac")) {
Debug.println("on mac, use AudioUnit effects: " + effects);
            System.setProperty("javax.sound.sampled.SourceDataLine", "#Rococoa Mixer"); // audio out is AudioUnit fixed
            System.setProperty("vavi.sound.sampled.rococoa.RococoaSourceDataLine.effects", effects);
        }
Debug.println("volume: " + midiVolume);
    }

    /** opens the midi keyboard which is specified by local.properties */
    MidiDevice openInputDevice() throws Exception {
        Info info = getMidiDevice(new MidiMatcher(inName, inVendor, inDescription, null), true);
        MidiDevice device = MidiSystem.getMidiDevice(info);
System.err.println("---- IN: " + info + " (" + device.getClass().getName() + ")" + " ----");
System.err.println("name      : " + info.getName());
System.err.println("vendor    : " + info.getVendor());
System.err.println("descriptor: " + info.getDescription());
System.err.println("version   : " + info.getVersion());
        device.open();
        return device;
    }

    @Test
    @DisplayName("specified in to specified out")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test2() throws Exception {

        MidiDevice device = openInputDevice();

        Info outInfo = getMidiDevice(new MidiMatcher(outName, outVendor, outDescription, null), false);
        MidiDevice outDevice =  MidiSystem.getMidiDevice(outInfo);
        assertInstanceOf(WindSynthesizer.class, outDevice);
System.err.println("---- OUT: " + outInfo +" (" + outDevice.getClass().getName() + ")" + " ----");
System.err.println("name      : " + outInfo.getName());
System.err.println("vendor    : " + outInfo.getVendor());
System.err.println("descriptor: " + outInfo.getDescription());
System.err.println("version   : " + outInfo.getVersion());
        outDevice.open();
        Receiver receiver = outDevice.getReceiver();
        // the master volume sysex is 14 bits over silence .. unity, and MidiUtil does not
        // clamp, so anything over 1 wraps round and comes out quieter than 1 would
        volume(receiver, Math.min(1, midiVolume));

        Synthesizer synthesizer = (Synthesizer) outDevice;
        if (tone != null) {
            // a tone, or a folder of them, in place of everything IFW has installed
Debug.println("tone: " + tone);
            Soundbank soundbank = new WindSoundbankReader().getSoundbank(Path.of(tone).toFile());
            assertNotNull(soundbank, "not an IFW tone: " + tone);
            synthesizer.loadAllInstruments(soundbank);
        }
        // the channel is on the first tone of the bank already, this is to pick another
Debug.println("program: " + program);
        MidiChannel channel = synthesizer.getChannels()[0];
        channel.programChange(program);

        // Now, display strings from synthInfos list in GUI.

        Transmitter transmitter = device.getTransmitter();
//        transmitter.setReceiver(new SimpleReceiver(receiver));
        transmitter.setReceiver(receiver);

        CountDownLatch cdl = new CountDownLatch(1);
Debug.println("waiting...");
        cdl.await();
Debug.println("done");

        device.close();
    }

    /** print received message */
    static class SimpleReceiver implements Receiver {
        Receiver receiver;
        SimpleReceiver(Receiver receiver) {
            this.receiver = receiver;
        }
        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (message instanceof ShortMessage shortMessage) {
                int channel = shortMessage.getChannel();
                int command = shortMessage.getCommand();
                int data1 = shortMessage.getData1();
                int data2 = shortMessage.getData2();
Debug.printf("short: command: %02x, channel: %d, data1: %d, data2: %d", command, channel, data1, data2);
                switch (command) {
                    case ShortMessage.NOTE_OFF:
                        break;
                    case ShortMessage.NOTE_ON:
                        break;
                    case ShortMessage.POLY_PRESSURE:
                        break;
                    case ShortMessage.CONTROL_CHANGE:
                        break;
                    case ShortMessage.PROGRAM_CHANGE:
                        break;
                    case ShortMessage.CHANNEL_PRESSURE:
                        break;
                    case ShortMessage.PITCH_BEND:
                        break;
                }
            } else if (message instanceof SysexMessage sysexMessage) {
                byte[] data = sysexMessage.getData();
Debug.println("sysex: %02X\n%s".formatted(sysexMessage.getStatus(), StringUtil.getDump(data, 32)));
            } else if (message instanceof MetaMessage metaMessage) {
Debug.println("meta: %02x".formatted(metaMessage.getType()));
            } else {
                assert false;
            }

            receiver.send(message, timeStamp);
        }

        @Override
        public void close() {
        }
    }

    /** */
    static String getInOut(MidiDevice device) {
        if (device.getMaxTransmitters() == 0 && device.getMaxReceivers() == 0)
            return "UNKNOWN(t:" + device.getMaxTransmitters() + ", r:" + device.getMaxReceivers() + ")";
        else if (device.getMaxTransmitters() == 0)
            return "INPUT";
        else if (device.getMaxReceivers() == 0)
            return "OUTPUT";
        else
            return "INOUT(t:" + device.getMaxTransmitters() + ", r:" + device.getMaxReceivers() + ")";
    }

    /**
     * @see "https://bonar.hatenablog.com/entry/20090322/1237711377"
     */
    @Test
    @DisplayName("show info")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test0() throws Exception {
        // MIDI
        Synthesizer synthesizer;
        Sequencer sequencer;
        MidiChannel[] channels;

        // Obtain information about all the installed synthesizers.
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (int i = 0; i < infos.length; i++) {
            MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
System.err.println("---- [" + i + "] " + infos[i] +" (" + device.getClass().getName() + ")" + " " + getInOut(device) + " ----");
System.err.println("name      : " + infos[i].getName());
System.err.println("vendor    : " + infos[i].getVendor());
System.err.println("descriptor: " + infos[i].getDescription());
System.err.println("version   : " + infos[i].getVersion());
        }

        // Now, display strings from synthInfos list in GUI.

System.err.println("----");
        sequencer = MidiSystem.getSequencer();
System.err.println("default sequencer: " + sequencer.getDeviceInfo());
System.err.println("default sequencer: " + sequencer);
        sequencer.open();

System.err.println("---- t0");
        synthesizer = MidiSystem.getSynthesizer();
System.err.println("default synthesizer: " + synthesizer.getDeviceInfo());
System.err.println("default synthesizer: " + synthesizer);
        channels = synthesizer.getChannels();
System.err.println("channels: " + channels.length);
System.err.println("sound bank: " + synthesizer.getDefaultSoundbank());
System.err.println("instruments: "+ synthesizer.getLoadedInstruments().length);

        Receiver receiver = MidiSystem.getReceiver();
System.err.println("default receiver: " + receiver);

        Transmitter transmitter = MidiSystem.getTransmitter();
System.err.println("default transmitter: " + transmitter);

        sequencer.close();
    }
}