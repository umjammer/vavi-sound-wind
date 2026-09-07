/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.wind;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.wind.BreathResponse;
import vavi.sound.wind.IfwEngine;
import vavi.sound.wind.IfwProgram;
import vavi.sound.wind.IfwSoundbank;
import vavi.sound.wind.Limiter;

import static java.lang.System.getLogger;
import static vavi.sound.midi.wind.WindMidiDeviceProvider.version;


/**
 * WindSynthesizer. a software synthesizer for wind controllers, an IFW clone.
 * <p>
 * IFW is one monophonic instrument per plug-in instance, so this synthesizer gives every
 * one of the sixteen MIDI channels an {@link IfwEngine} of its own: sixteen parts, each
 * monophonic, each with its own tone. that is how a wind player uses it, one channel per
 * controller, and it still plays an ordinary multi timbral MIDI file.
 * <p>
 * what a wind controller sends:
 * <ul>
 * <li>CC 2 breath, which nearly every IFW tone routes into its filters and its amps</li>
 * <li>channel pressure, which some controllers send instead, taken as breath as well</li>
 * <li>CC 5 and CC 65 portamento, honoured while the tone's {@code Glide MIDI Enabled} is on</li>
 * <li>pitch bend, over the tone's own bend range rather than the usual two semitones</li>
 * </ul>
 * a controller that sends none of that still sounds: until the first breath message
 * arrives, note velocity stands in for it.
 * <p>
 * the default soundbank is whatever IFW itself has installed, every tone under
 * {@code ~/Documents/IFW/Sounds}, or the IFW initial program alone when that folder is
 * not there. it is loaded whole and numbered as one running count, so a program change on
 * its own walks the installed tones and no bank select is needed to reach the first 128 of
 * them. every channel starts on the tone at its own bank and program, which is the first
 * installed tone before any program change arrives.
 * {@link WindSoundbankReader#getDefaultSoundbank()} is where it comes from, and
 * {@link #loadAllInstruments(Soundbank)} puts another one in its place, a folder or a
 * single tone read by {@link WindSoundbankReader}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @see IfwEngine
 */
public class WindSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(WindSynthesizer.class.getName());

    /** the device information */
    protected static final MidiDevice.Info info =
            new MidiDevice.Info("Wind MIDI Synthesizer",
                    "vavi",
                    "Software synthesizer for wind controllers, an IFW clone",
                    "Version " + version) {};

    /** one monophonic instrument per channel, as IFW is one instance per part */
    private static final int MAX_CHANNEL = 16;

    /** */
    private static final float SAMPLE_RATE = 44100;

    /** samples per rendering cycle */
    private static final int BLOCK_SIZE = Integer.getInteger("vavi.sound.wind.block", 256);

    /**
     * how many blocks the audio device is allowed to sit on, which is the latency.
     * <p>
     * a wind controller is played against the sound, so this has to stay small: left to
     * itself the mixer hands out half a second, and half a second of breath arriving late
     * is the difference between an instrument and a tape recorder. four blocks is 23 ms
     * at the default block size, against the 1.4 ms it costs to fill one with all sixteen
     * parts sounding. raise it if the machine cannot keep up and the sound breaks up.
     */
    private static final int BLOCKS_BUFFERED = Integer.getInteger("vavi.sound.wind.buffer", 4);

    /** the player's end of the breath controller, {@code low,high,depth} */
    private static final BreathResponse BREATH_RESPONSE = breathResponse();

    /** */
    private static BreathResponse breathResponse() {
        String property = System.getProperty("vavi.sound.wind.breath");
        try {
            return property == null ? BreathResponse.DEFAULT : BreathResponse.valueOf(property);
        } catch (IllegalArgumentException e) {
logger.log(Level.WARNING, "vavi.sound.wind.breath: " + e.getMessage());
            return BreathResponse.DEFAULT;
        }
    }

    /** 16 bit, stereo, signed, little endian */
    private final AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);

    /** */
    private final WindMidiChannel[] channels = new WindMidiChannel[MAX_CHANNEL];

    /** */
    private Soundbank soundbank;

    /** the tones the channels may switch between, {@link #soundbank} unless one was loaded */
    private final List<Instrument> loaded = new ArrayList<>();

    /** */
    private final List<Receiver> receivers = new ArrayList<>();

    /** */
    private long timestamp;

    /** */
    private volatile boolean isOpen;

    /** */
    private SourceDataLine line;

    /** the universal realtime master volume, 0 .. 1 */
    private volatile float masterGain = volume();

    /** where the master volume stands before any sysex has arrived */
    private static float volume() {
        String property = System.getProperty("vavi.sound.wind.volume");
        try {
            return property == null ? 1 : Math.clamp(Float.parseFloat(property), 0, 1);
        } catch (NumberFormatException e) {
logger.log(Level.WARNING, "vavi.sound.wind.volume: " + property);
            return 1;
        }
    }

    /** the rendering thread */
    private ExecutorService executor;

    /** */
    public WindSynthesizer() {
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new WindMidiChannel(i);
        }
        breathResponse(BREATH_RESPONSE);
        this.soundbank = WindSoundbankReader.getDefaultSoundbank();
        Collections.addAll(loaded, soundbank.getInstruments());
        select();
    }

    /**
     * Puts the tone at its own bank and program into every channel.
     * <p>
     * a channel plays whatever the loaded instruments say its bank and program mean, and
     * that is decided here rather than at the program change alone: a channel that has had
     * no program change is on bank 0 program 0, which is the first tone of the installed
     * folder, and a soundbank arriving later moves every channel to its tone of the same
     * patch. without it a freshly opened synthesizer would sound the IFW initial program,
     * whatever the soundbank holds.
     */
    private void select() {
        for (WindMidiChannel channel : channels) {
            channel.select();
        }
    }

    /** the tone a channel is playing, which is the tone at its bank and program */
    public IfwProgram getTone(int channel) {
        return channels[channel].engine.getProgram();
    }

    /**
     * How the travel of the breath controller becomes loudness, on every channel.
     * <p>
     * this is the player's setting rather than the tone's, so it is set here and not in
     * the soundbank. it starts at {@link BreathResponse#DEFAULT}, or at whatever
     * {@code -Dvavi.sound.wind.breath=low,high,depth} asks for.
     *
     * @see BreathResponse
     */
    public void setBreathResponse(BreathResponse breathResponse) {
        breathResponse(breathResponse);
    }

    /** */
    private void breathResponse(BreathResponse breathResponse) {
        for (WindMidiChannel channel : channels) {
            channel.engine.setBreathResponse(breathResponse);
        }
    }

    /** what {@link #setBreathResponse(BreathResponse)} last set */
    public BreathResponse getBreathResponse() {
        return channels[0].engine.getBreathResponse();
    }

    /**
     * The volume of the whole mix, 1 being the tones as they come.
     * <p>
     * there is no setter to go with this. the master volume is the universal realtime
     * device control the MIDI specification already carries, so it is set by sending that
     * sysex to {@link #getReceiver()} and by nothing else. the mix of sixteen parts under
     * it runs into a soft knee at -1.4 dBFS rather than squaring off.
     * <p>
     * {@code -Dvavi.sound.wind.volume} says where it stands before the first one arrives,
     * since an SPI synthesizer is usually opened and played by a sequencer that will never
     * send one.
     */
    public float getMasterVolume() {
        return masterGain;
    }

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (isOpen()) {
logger.log(Level.WARNING, "already open: " + hashCode());
            return;
        }

        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(lineInfo);
            line.addLineListener(event -> logger.log(Level.DEBUG, "Line: " + event.getType()));
            line.open(audioFormat, BLOCK_SIZE * BLOCKS_BUFFERED * audioFormat.getFrameSize());
            line.start();
logger.log(Level.DEBUG, "latency: " + line.getBufferSize() / audioFormat.getFrameSize() * 1000 / SAMPLE_RATE + " ms");
        } catch (LineUnavailableException e) {
            throw (MidiUnavailableException) new MidiUnavailableException().initCause(e);
        }

        timestamp = 0;
        isOpen = true;

        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "Wind Renderer");
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.setDaemon(true);
            return thread;
        });
        executor.submit(this::play);
    }

    /** Mixes the sixteen parts into the line. */
    private void play() {
        float[] left = new float[BLOCK_SIZE];
        float[] right = new float[BLOCK_SIZE];
        byte[] buffer = new byte[BLOCK_SIZE * 4];

        while (isOpen) {
            try {
                Arrays.fill(left, 0);
                Arrays.fill(right, 0);
                for (WindMidiChannel channel : channels) {
                    channel.render(left, right);
                }
                float gain = masterGain;
                for (int i = 0; i < BLOCK_SIZE; i++) {
                    // sixteen parts and the master volume on top, so the mix needs a knee too
                    write(buffer, i * 4, Limiter.process(left[i] * gain));
                    write(buffer, i * 4 + 2, Limiter.process(right[i] * gain));
                }
                line.write(buffer, 0, buffer.length);
            } catch (Exception e) {
logger.log(Level.INFO, e.getMessage(), e);
            }
        }
    }

    /** one clipped 16 bit little endian sample */
    private static void write(byte[] buffer, int offset, float value) {
        int sample = Math.round(Math.clamp(value, -1, 1) * Short.MAX_VALUE);
        buffer[offset] = (byte) sample;
        buffer[offset + 1] = (byte) (sample >> 8);
    }

    @Override
    public void close() {
        isOpen = false;
        for (Receiver receiver : new ArrayList<>(receivers)) {
            receiver.close();
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        if (line != null) {
            line.drain();
            line.close();
            line = null;
        }
        for (WindMidiChannel channel : channels) {
            channel.engine.allSoundOff();
        }
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public long getMicrosecondPosition() {
        return timestamp;
    }

    @Override
    public int getMaxReceivers() {
        return -1;
    }

    @Override
    public int getMaxTransmitters() {
        return 0;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new WindReceiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        return receivers;
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        throw new MidiUnavailableException("No transmitter available");
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return Collections.emptyList();
    }

    @Override
    public int getMaxPolyphony() {
        // a wind instrument plays one note, and there is one of them per channel
        return MAX_CHANNEL;
    }

    @Override
    public long getLatency() {
        return (long) (BLOCK_SIZE / SAMPLE_RATE * 1_000_000);
    }

    @Override
    public MidiChannel[] getChannels() {
        return channels;
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        List<VoiceStatus> statuses = new ArrayList<>();
        for (WindMidiChannel channel : channels) {
            VoiceStatus status = channel.voiceStatus();
            if (status != null) {
                statuses.add(status);
            }
        }
        return statuses.toArray(VoiceStatus[]::new);
    }

    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        return soundbank instanceof IfwSoundbank;
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        if (!(instrument.getData() instanceof IfwProgram)) {
            throw new IllegalArgumentException("not an IFW tone: " + instrument);
        }
        loaded.removeIf(i -> samePatch(i.getPatch(), instrument.getPatch()));
        loaded.add(instrument);
        select();
        return true;
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
        loaded.remove(instrument);
        select();
    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        if (!loaded.remove(from)) {
            return false;
        }
        loaded.add(to);
        select();
        return true;
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        return soundbank;
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return soundbank.getInstruments();
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return loaded.toArray(Instrument[]::new);
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        if (!isSoundbankSupported(soundbank)) {
            throw new IllegalArgumentException("not an IFW soundbank: " + soundbank);
        }
        this.soundbank = soundbank;
        loaded.clear();
        Collections.addAll(loaded, soundbank.getInstruments());
        select();
        return true;
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        loaded.removeIf(i -> i.getSoundbank() == soundbank);
        select();
    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        boolean all = true;
        for (Patch patch : patchList) {
            Instrument instrument = soundbank.getInstrument(patch);
            if (instrument == null) {
                all = false;
            } else {
                loadInstrument(instrument);
            }
        }
        return all;
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        for (Patch patch : patchList) {
            loaded.removeIf(i -> i.getSoundbank() == soundbank && samePatch(i.getPatch(), patch));
        }
        select();
    }

    /** */
    private static boolean samePatch(Patch a, Patch b) {
        return a.getBank() == b.getBank() && a.getProgram() == b.getProgram();
    }

    /**
     * The tone at a patch, the nearest one in the bank when it is empty.
     * <p>
     * the bank is the 14 bits of CC 0 and CC 32 together, the 0 .. 16383 that
     * {@link MidiChannel#programChange(int, int)} and {@link Patch#getBank()} carry.
     * {@link IfwSoundbank} numbers its tones from bank 0 up, so the bank select LSB is
     * what reaches the ones past the first 128 and a program change alone is enough below
     * that.
     */
    private IfwProgram find(int bank, int program) {
        Instrument fallback = null;
        for (Instrument instrument : loaded) {
            Patch patch = instrument.getPatch();
            if (patch.getBank() == bank && patch.getProgram() == program) {
                return (IfwProgram) instrument.getData();
            }
            if (fallback == null && patch.getBank() == bank) {
                fallback = instrument;
            }
        }
        if (fallback == null && !loaded.isEmpty()) {
            fallback = loaded.getFirst();
        }
        return fallback == null ? null : (IfwProgram) fallback.getData();
    }

    /**
     * WindMidiChannel. one monophonic IFW instrument on one MIDI channel.
     */
    private class WindMidiChannel implements MidiChannel {

        /** */
        private final int channel;

        /** */
        private final IfwEngine engine = new IfwEngine(SAMPLE_RATE);

        /** this part on its own, before the channel volume and the pan place it */
        private float[] partLeft = new float[0], partRight = new float[0];

        private int program;
        private int bank;
        private boolean mute;
        private boolean solo;

        private final int[] polyPressure = new int[128];
        private int pressure;
        private int pitchBend = 8192;
        private final int[] control = new int[128];

        /** */
        WindMidiChannel(int channel) {
            this.channel = channel;
            control[7] = 100; // channel volume
            control[11] = 127; // expression
            control[10] = 64; // pan
        }

        /** Adds this part to the block, at its channel volume and its pan. */
        void render(float[] left, float[] right) {
            if (mute || anySolo() && !solo || !engine.isActive()) {
                return;
            }
            if (partLeft.length != left.length) {
                partLeft = new float[left.length];
                partRight = new float[left.length];
            }
            Arrays.fill(partLeft, 0);
            Arrays.fill(partRight, 0);
            engine.render(partLeft, partRight, 0, left.length);

            // the channel volume and the expression sit outside the tone's own master level
            float gain = control[7] / 127f * (control[11] / 127f);
            // constant power pan, unity in the middle
            double angle = control[10] / 127.0 * Math.PI / 2;
            float gainLeft = gain * (float) (Math.cos(angle) * Math.sqrt(2));
            float gainRight = gain * (float) (Math.sin(angle) * Math.sqrt(2));
            for (int i = 0; i < left.length; i++) {
                left[i] += partLeft[i] * gainLeft;
                right[i] += partRight[i] * gainRight;
            }
        }

        /** */
        private boolean anySolo() {
            for (WindMidiChannel c : channels) {
                if (c.solo) {
                    return true;
                }
            }
            return false;
        }

        /** */
        VoiceStatus voiceStatus() {
            if (!engine.isActive()) {
                return null;
            }
            VoiceStatus status = new VoiceStatus();
            status.active = true;
            status.channel = channel;
            status.bank = bank;
            status.program = program;
            status.note = engine.getNote();
            status.volume = Math.round(engine.getBreath() * 127);
            return status;
        }

        @Override
        public void noteOn(int noteNumber, int velocity) {
            if (mute) {
                return;
            }
            engine.noteOn(noteNumber, velocity);
        }

        @Override
        public void noteOff(int noteNumber, int velocity) {
            engine.noteOff(noteNumber);
        }

        @Override
        public void noteOff(int noteNumber) {
            noteOff(noteNumber, 0);
        }

        @Override
        public void setPolyPressure(int noteNumber, int pressure) {
            polyPressure[noteNumber] = pressure;
        }

        @Override
        public int getPolyPressure(int noteNumber) {
            return polyPressure[noteNumber];
        }

        @Override
        public void setChannelPressure(int pressure) {
            this.pressure = pressure;
            // some wind controllers blow into aftertouch rather than into CC 2
            engine.setBreath(pressure / 127f);
        }

        @Override
        public int getChannelPressure() {
            return pressure;
        }

        @Override
        public void controlChange(int controller, int value) {
            control[controller] = value;
            switch (controller) {
                case 0 -> bank = (value << 7) + (bank & 0x7f);
                case 32 -> bank = (bank & 0x3f80) + value;
                case IfwEngine.BREATH_CONTROLLER -> engine.setBreath(value / 127f);
                case 5 -> engine.setPortamentoTime(value / 127f);
                case 65 -> engine.setPortamento(value >= 64);
                case 120 -> engine.allSoundOff();
                case 121 -> resetAllControllers();
                case 123, 124, 125, 126, 127 -> engine.allNotesOff();
                default ->
logger.log(Level.TRACE, "control change unhandled[%d]: (%02x): %d".formatted(channel, controller, value));
            }
        }

        @Override
        public int getController(int controller) {
            return control[controller];
        }

        @Override
        public void programChange(int program) {
            this.program = program & 0x7f;
            select();
        }

        /** Puts the tone at this channel's bank and program into the engine. */
        void select() {
            IfwProgram tone = find(bank, program);
            if (tone == null) {
logger.log(Level.DEBUG, "select[%d]: %d, no tone".formatted(channel, program));
                return;
            }
            engine.setProgram(new IfwProgram(tone));
logger.log(Level.DEBUG, "select[%d]: %d: %s".formatted(channel, program, tone.getName()));
        }

        @Override
        public void programChange(int bank, int program) {
            controlChange(0, bank >> 7 & 0x7f);
            controlChange(32, bank & 0x7f);
            programChange(program);
        }

        @Override
        public int getProgram() {
            return program;
        }

        @Override
        public void setPitchBend(int bend) {
            pitchBend = bend;
            engine.setPitchBend((bend - 8192) / 8192f);
        }

        @Override
        public int getPitchBend() {
            return pitchBend;
        }

        @Override
        public void resetAllControllers() {
            control[7] = 100;
            control[11] = 127;
            control[10] = 64;
            pitchBend = 8192;
            engine.setPitchBend(0);
            engine.setPortamentoTime(-1);
            engine.setPortamento(true);
        }

        @Override
        public void allNotesOff() {
            engine.allNotesOff();
        }

        @Override
        public void allSoundOff() {
            engine.allSoundOff();
        }

        @Override
        public boolean localControl(boolean on) {
            control[122] = on ? 127 : 0;
            return on;
        }

        @Override
        public void setMono(boolean on) {
            // an IFW part is monophonic whatever mode it is asked for
        }

        @Override
        public boolean getMono() {
            return true;
        }

        @Override
        public void setOmni(boolean on) {
            // one part per channel, omni would merge them
        }

        @Override
        public boolean getOmni() {
            return false;
        }

        @Override
        public void setMute(boolean mute) {
            this.mute = mute;
            if (mute) {
                engine.allSoundOff();
            }
        }

        @Override
        public boolean getMute() {
            return mute;
        }

        @Override
        public void setSolo(boolean soloState) {
            this.solo = soloState;
        }

        @Override
        public boolean getSolo() {
            return solo;
        }
    }

    /**
     * WindReceiver.
     */
    private class WindReceiver implements MidiDeviceReceiver {

        @SuppressWarnings("hiding")
        private boolean isOpen;

        /** */
        WindReceiver() {
            receivers.add(this);
            isOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!isOpen) {
                throw new IllegalStateException("receiver is not open");
            }

            timestamp = timeStamp;
            switch (message) {
                case ShortMessage shortMessage -> {
                    MidiChannel channel = channels[shortMessage.getChannel()];
                    int data1 = shortMessage.getData1();
                    int data2 = shortMessage.getData2();
                    switch (shortMessage.getCommand()) {
                        case ShortMessage.NOTE_OFF -> channel.noteOff(data1, data2);
                        case ShortMessage.NOTE_ON -> channel.noteOn(data1, data2);
                        case ShortMessage.POLY_PRESSURE -> channel.setPolyPressure(data1, data2);
                        case ShortMessage.CONTROL_CHANGE -> channel.controlChange(data1, data2);
                        case ShortMessage.PROGRAM_CHANGE -> channel.programChange(data1);
                        case ShortMessage.CHANNEL_PRESSURE -> channel.setChannelPressure(data1);
                        case ShortMessage.PITCH_BEND -> channel.setPitchBend(data1 | data2 << 7);
                        default ->
logger.log(Level.DEBUG, "unhandled short: %02X".formatted(shortMessage.getCommand()));
                    }
                }
                case SysexMessage sysexMessage -> {
                    byte[] data = sysexMessage.getData();
                    // universal realtime, device control, master volume
                    if (data.length >= 6 && data[0] == 0x7f && data[2] == 0x04 && data[3] == 0x01) {
                        // F0 7F <device> 04 01 <lsb> <msb> F7, 14 bits over silence .. unity
                        masterGain = ((data[4] & 0x7f) | (data[5] & 0x7f) << 7) / 16383f;
logger.log(Level.DEBUG, "sysex volume: gain: %4.2f".formatted(masterGain));
                    } else {
logger.log(Level.DEBUG, "sysex unhandled: %02x".formatted(data.length == 0 ? 0 : data[0]));
                    }
                }
                case null, default ->
logger.log(Level.DEBUG, message == null ? "null" : message.getClass().getName());
            }
        }

        @Override
        public void close() {
            receivers.remove(this);
            isOpen = false;
        }

        @Override
        public MidiDevice getMidiDevice() {
            return WindSynthesizer.this;
        }
    }
}
