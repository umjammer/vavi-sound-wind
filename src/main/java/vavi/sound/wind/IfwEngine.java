/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Objects;

import static vavi.sound.wind.IfwParameter.Amp1Breath;
import static vavi.sound.wind.IfwParameter.Amp1BreathSubMod;
import static vavi.sound.wind.IfwParameter.Amp1Level;
import static vavi.sound.wind.IfwParameter.Amp2Breath;
import static vavi.sound.wind.IfwParameter.Amp2BreathSubMod;
import static vavi.sound.wind.IfwParameter.Amp2Level;
import static vavi.sound.wind.IfwParameter.BreathSmoothTime;
import static vavi.sound.wind.IfwParameter.EnhancerDelayL;
import static vavi.sound.wind.IfwParameter.EnhancerDelayR;
import static vavi.sound.wind.IfwParameter.EnhancerMix;
import static vavi.sound.wind.IfwParameter.EqHighGain;
import static vavi.sound.wind.IfwParameter.EqLowGain;
import static vavi.sound.wind.IfwParameter.ExciterFrequency;
import static vavi.sound.wind.IfwParameter.ExciterMix;
import static vavi.sound.wind.IfwParameter.Filter1Breath;
import static vavi.sound.wind.IfwParameter.Filter1Frequency;
import static vavi.sound.wind.IfwParameter.Filter1KeyTrack;
import static vavi.sound.wind.IfwParameter.Filter1Resonance;
import static vavi.sound.wind.IfwParameter.Filter2Breath;
import static vavi.sound.wind.IfwParameter.Filter2Frequency;
import static vavi.sound.wind.IfwParameter.Filter2KeyTrack;
import static vavi.sound.wind.IfwParameter.Filter2Resonance;
import static vavi.sound.wind.IfwParameter.Filter3Frequency;
import static vavi.sound.wind.IfwParameter.Filter3Mix;
import static vavi.sound.wind.IfwParameter.Filter3Resonance;
import static vavi.sound.wind.IfwParameter.Filter3Stage;
import static vavi.sound.wind.IfwParameter.GateCloseSmoothTime;
import static vavi.sound.wind.IfwParameter.GateOpenSmoothTime;
import static vavi.sound.wind.IfwParameter.GlideSmoothTime;
import static vavi.sound.wind.IfwParameter.GlideSpeed;
import static vavi.sound.wind.IfwParameter.MasterLevel;
import static vavi.sound.wind.IfwParameter.MasterTune;
import static vavi.sound.wind.IfwParameter.PitchBendRange;
import static vavi.sound.wind.IfwParameter.PitchBendSmoothTime;
import static vavi.sound.wind.IfwParameter.Transpose;


/**
 * IfwEngine. one monophonic IFW voice.
 * <pre>
 * OSC 1..4 -&gt; out 1 / out 2 -&gt; FILTER 1, 2, 3 -&gt; AMP 1, 2 -&gt; EQ -&gt; EXCITER -&gt; ENHANCER -&gt; MASTER
 * </pre>
 * everything down to the amplifiers runs at four times the sample rate and is then brought
 * back down through a filter of its own, which is what lets the ladder saturate the way it
 * does without whistling back a fold of itself. {@code -Dvavi.sound.wind.oversample} takes
 * that back to 2 or 1 on a machine that cannot keep up, at the cost of some of the top.
 * <p>
 * modulation is one flat scheme: a slot multiplies its two sources, scales the product by its
 * depth, and adds the result to its destination in the units of that destination's own knob,
 * so a depth of 10 sweeps a knob across its whole range. a pitch counts in semitones, and so
 * does a filter cutoff, over a range of twenty four and a hundred and fifty of them.
 * <p>
 * a controller that sends no breath would leave a normal program silent, so until the first
 * CC 2 arrives the note velocity stands in for the breath. that lets an ordinary MIDI file
 * play through the same engine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the algorithm read back out of the plug-in <br>
 * @see IfwProgram
 */
public class IfwEngine {

    /** */
    public static final float DEFAULT_SAMPLE_RATE = 44100;

    /** MIDI CC 2, what a wind controller blows into */
    public static final int BREATH_CONTROLLER = 2;

    /** what one oscillator is worth on a bus, there being four of them */
    private static final float OSC_SCALE = .25f;

    /** where a cutoff of MIDI note 0 sits in the filter's own semitones */
    private static final float CUTOFF_ORIGIN = 24;

    /** what one step of a filter breath knob is worth, in semitones of cutoff */
    private static final float FILTER_BREATH = 15;

    /** what a whole filter key track knob is worth, per semitone played */
    private static final float FILTER_KEY_TRACK = 1.25f;

    /** the four oscillator slots. oscillator 4 spends its third slot on noise */
    private static final IfwParameter[][] OSC = new IfwParameter[4][];
    private static final int SEMI = 0, TUNE = 1, SAW = 2, TRI = 3, THIRD = 4, WIDTH = 5;

    private static final IfwParameter[][] EG = table(4, "Eg%dAttack", "Eg%dDecay", "Eg%dSustain", "Eg%dRelease");
    private static final int ATTACK = 0, DECAY = 1, SUSTAIN = 2, RELEASE = 3;

    private static final IfwParameter[][] LFO = table(2, "Lfo%dSpeed");
    private static final int SPEED = 0;

    private static final IfwParameter[] MOD_DEPTH = new IfwParameter[8];

    /** the modulation destinations that are addressed per oscillator or per envelope */
    private static final int[] OSC_PITCH = new int[4], OSC_FM = new int[4], OSC_PWM_PHASE = new int[4],
            OSC_SAW_LEVEL = new int[4], OSC_TRI_LEVEL = new int[4], OSC_THIRD_LEVEL = new int[4],
            EG_RATE = new int[4];

    static {
        for (int i = 0; i < 4; i++) {
            String n = String.valueOf(i + 1);
            String third = i == 3 ? "Noise" : "Pwm";
            OSC[i] = new IfwParameter[] {
                    IfwParameter.valueOf("Osc" + n + "Semi"),
                    IfwParameter.valueOf("Osc" + n + "Tune"),
                    IfwParameter.valueOf("Osc" + n + "SawLevel"),
                    IfwParameter.valueOf("Osc" + n + "TriLevel"),
                    IfwParameter.valueOf("Osc" + n + third + "Level"),
                    IfwParameter.valueOf("Osc" + n + (i == 3 ? "NoiseLoFi" : "PwmWidth"))
            };
            OSC_PITCH[i] = ModDestination.valueOf("Osc" + n + "Pitch").ordinal();
            OSC_FM[i] = ModDestination.valueOf("Osc" + n + "FM").ordinal();
            OSC_PWM_PHASE[i] = ModDestination.valueOf("Osc" + n + "PwmPhase").ordinal();
            OSC_SAW_LEVEL[i] = ModDestination.valueOf("Osc" + n + "SawLevel").ordinal();
            OSC_TRI_LEVEL[i] = ModDestination.valueOf("Osc" + n + "TriLevel").ordinal();
            OSC_THIRD_LEVEL[i] = ModDestination.valueOf("Osc" + n + third + "Level").ordinal();
            EG_RATE[i] = ModDestination.valueOf("Eg" + n + "Rate").ordinal();
        }
        for (int i = 0; i < 8; i++) {
            MOD_DEPTH[i] = IfwParameter.valueOf("Mod" + (i + 1) + "Depth");
        }
    }

    /** the parameters named {@code formats} for slot 1 .. n */
    private static IfwParameter[][] table(int n, String... formats) {
        IfwParameter[][] result = new IfwParameter[n][formats.length];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < formats.length; j++) {
                result[i][j] = IfwParameter.valueOf(formats[j].formatted(i + 1));
            }
        }
        return result;
    }

    /** how many times over the sample rate everything up to the amplifiers runs */
    private static int oversample(float sampleRate) {
        String property = System.getProperty("vavi.sound.wind.oversample");
        if (property != null) {
            try {
                return Math.max(1, Math.min(8, Integer.parseInt(property)));
            } catch (NumberFormatException e) {
                // the plug-in's own rule, below
            }
        }
        return sampleRate < 50000 ? 4 : sampleRate < 100000 ? 2 : 1;
    }

    /** */
    private final float sampleRate;

    /** */
    private final int oversample;

    /** */
    private IfwProgram program = new IfwProgram();

    /** a snapshot of {@link #program}, indexed by {@link IfwParameter#ordinal()} */
    private final float[] knobs = new float[IfwParameter.values().length];

    // everything the program chooses rather than dials, resolved once per program change

    private final boolean[] oscSync = new boolean[4], oscOut1 = new boolean[4], oscOut2 = new boolean[4];
    /** whether anything at all reads an oscillator, so that a silent one costs nothing */
    private final boolean[] oscUsed = new boolean[4];
    private final Waveform[] oscWaveform = new Waveform[4];
    /** the semi and tune knobs in semitones */
    private final float[] oscDetune = new float[4];
    private final boolean[] egRetrigger = new boolean[4];
    private FilterType filter1Type = FilterType.LPF12, filter2Type = FilterType.LPF12;
    private FilterConnection connection = FilterConnection.Serial;
    private boolean filter3Enabled;
    private BreathSubMod filter1SubMod, filter2SubMod, amp1SubMod, amp2SubMod;
    private final ModSource[] modSource1 = new ModSource[8], modSource2 = new ModSource[8];
    private final ModDestination[] modDestination = new ModDestination[8];
    private GlideMode glideMode = GlideMode.Time;
    private boolean glideMidiEnabled;

    private final Oscillator[] oscillators = new Oscillator[4];
    private final Envelope[] envelopes = new Envelope[4];
    private final Lfo[] lfos = new Lfo[2];
    private final Filter filter1, filter2;
    private final Phaser filter3;
    private final Exciter exciter;
    private final Enhancer enhancer;
    private final ToneControl toneControl;
    private final Decimator decimator1, decimator2;

    private final Smoother breathSmoother, bendSmoother, glideSmoother, gateSmoother;

    /** the levels of the four envelopes this sample */
    private final float[] egLevels = new float[4];

    /** what the modulation matrix adds to each destination this sample */
    private final float[] mod = new float[ModDestination.values().length];

    /** the notes that are down, most recent last */
    private final Deque<Integer> heldNotes = new ArrayDeque<>();

    private int note = 60;
    private int velocity = 100;
    private boolean noteHeld;

    /** the pitch the glide is heading for and where it stands, in MIDI note numbers */
    private float glideTarget = 60, glideCurrent = 60;

    /** how far the glide had to go when it started, which is what fixes how long it takes */
    private float glideDistance;

    /** 0 .. 1 */
    private float breathTarget;

    /** whether a breath controller has ever been heard on this engine */
    private boolean breathReceived;

    /** the player's end of the breath controller, outside the tone */
    private BreathResponse breathResponse = BreathResponse.DEFAULT;

    /** -1 .. 1 */
    private float bendTarget;

    /** MIDI CC 65, and CC 5, which is both a glide speed and a modulation source */
    private boolean portamento = true;
    private float portamentoTime = -1;

    /** the host tempo the BPM synchronized LFOs follow */
    private float tempo = 120;

    /** the host input the {@code Ext.In} waveforms read */
    private float externalLeft, externalRight;

    /** scratch for {@link Enhancer#process} */
    private final float[] stereo = new float[2];

    /** */
    public IfwEngine() {
        this(DEFAULT_SAMPLE_RATE);
    }

    /** */
    public IfwEngine(float sampleRate) {
        this.sampleRate = sampleRate;
        this.oversample = oversample(sampleRate);
        float inner = sampleRate * oversample;
        for (int i = 0; i < 4; i++) {
            oscillators[i] = new Oscillator(i == 3);
            envelopes[i] = new Envelope(sampleRate);
        }
        for (int i = 0; i < 2; i++) {
            lfos[i] = new Lfo(sampleRate);
        }
        filter1 = new Filter(inner);
        filter2 = new Filter(inner);
        filter3 = new Phaser();
        exciter = new Exciter();
        enhancer = new Enhancer(sampleRate);
        toneControl = new ToneControl(sampleRate);
        decimator1 = new Decimator(inner);
        decimator2 = new Decimator(inner);
        breathSmoother = new Smoother(sampleRate);
        bendSmoother = new Smoother(sampleRate);
        glideSmoother = new Smoother(sampleRate);
        gateSmoother = new Smoother(sampleRate);
        apply(program);
    }

    /** */
    public float getSampleRate() {
        return sampleRate;
    }

    /** how many times over the sample rate the oscillators and the filters run */
    public int getOversample() {
        return oversample;
    }

    /** */
    public IfwProgram getProgram() {
        return program;
    }

    /** Loads a tone. a sounding note keeps playing, as it does when IFW switches program. */
    public void setProgram(IfwProgram program) {
        apply(program);
    }

    /** */
    private void apply(IfwProgram program) {
        this.program = program;
        for (IfwParameter p : IfwParameter.values()) {
            knobs[p.ordinal()] = program.get(p);
        }

        for (int i = 0; i < 4; i++) {
            String n = String.valueOf(i + 1);
            oscSync[i] = program.is(IfwParameter.valueOf("Osc" + n + "Sync"));
            oscOut1[i] = program.is(IfwParameter.valueOf("Osc" + n + "Output1"));
            oscOut2[i] = program.is(IfwParameter.valueOf("Osc" + n + "Output2"));
            oscWaveform[i] = program.choice(IfwParameter.valueOf("Osc" + n + "Waveform2"), Waveform.class);
            oscDetune[i] = knob(OSC[i][SEMI]) + knob(OSC[i][TUNE]) / 100;
            egRetrigger[i] = program.is(IfwParameter.valueOf("Eg" + n + "Retrigger"));
            envelopes[i].set(knob(EG[i][ATTACK]), knob(EG[i][DECAY]), knob(EG[i][SUSTAIN]), knob(EG[i][RELEASE]));
        }
        for (int i = 0; i < 2; i++) {
            String n = String.valueOf(i + 1);
            LfoWaveform waveform = program.choice(IfwParameter.valueOf("Lfo" + n + "Waveform"), LfoWaveform.class);
            boolean bpmSync = program.is(i == 0 ? IfwParameter.Lfo1BPMSync : IfwParameter.Lfo2BpmSync);
            float hz = bpmSync
                    ? program.choice(IfwParameter.valueOf("Lfo" + n + "Beat"), LfoBeat.class).frequency(tempo)
                    : Lfo.hertz(knob(LFO[i][SPEED]));
            lfos[i].set(waveform, hz, program.is(IfwParameter.valueOf("Lfo" + n + "KeySync")));
        }
        for (int i = 0; i < 8; i++) {
            String n = String.valueOf(i + 1);
            modSource1[i] = program.choice(IfwParameter.valueOf("Mod" + n + "Source1"), ModSource.class);
            modSource2[i] = program.choice(IfwParameter.valueOf("Mod" + n + "Source2"), ModSource.class);
            modDestination[i] = program.choice(IfwParameter.valueOf("Mod" + n + "Destination"), ModDestination.class);
        }

        for (int i = 0; i < 4; i++) {
            oscUsed[i] = oscOut1[i] || oscOut2[i] || i == 0 && anySync();
            for (int slot = 0; slot < 8 && !oscUsed[i]; slot++) {
                oscUsed[i] |= reads(modSource1[slot], i) || reads(modSource2[slot], i);
            }
        }

        filter1Type = program.choice(IfwParameter.Filter1Type, FilterType.class);
        filter2Type = program.choice(IfwParameter.Filter2Type, FilterType.class);
        connection = program.choice(IfwParameter.FilterConnection, FilterConnection.class);
        filter3Enabled = program.is(IfwParameter.Filter3Enabled);
        filter1SubMod = program.choice(IfwParameter.Filter1BreathSubMod, BreathSubMod.class);
        filter2SubMod = program.choice(IfwParameter.Filter2BreathSubMod, BreathSubMod.class);
        amp1SubMod = program.choice(Amp1BreathSubMod, BreathSubMod.class);
        amp2SubMod = program.choice(Amp2BreathSubMod, BreathSubMod.class);
        glideMode = program.choice(IfwParameter.GlideMode, GlideMode.class);
        glideMidiEnabled = program.is(IfwParameter.GlideMidiEnabled);

        breathSmoother.setTime(knob(BreathSmoothTime));
        bendSmoother.setTime(knob(PitchBendSmoothTime));
        glideSmoother.setTime(knob(GlideSmoothTime));

        toneControl.set(knob(EqLowGain), knob(EqHighGain));
    }

    /** whether any oscillator is synchronised, which is what keeps oscillator 1 running */
    private boolean anySync() {
        return oscSync[1] || oscSync[2] || oscSync[3];
    }

    /** whether a modulation source reads one of the oscillators */
    private static boolean reads(ModSource source, int oscillator) {
        return source.isAudioRate() && source.name().charAt(3) - '1' == oscillator;
    }

    /** the host tempo the BPM synchronized LFOs follow */
    public void setTempo(float bpm) {
        this.tempo = bpm;
        apply(program);
    }

    /** the host input the {@code Ext.In} waveforms read, one sample */
    public void setExternalInput(float left, float right) {
        this.externalLeft = left;
        this.externalRight = right;
    }

    /**
     * Starts a note.
     * <p>
     * a note that arrives while another is down is a legato note: it glides, and it restarts
     * only the envelopes whose {@code RETRIGGER} is on.
     */
    public void noteOn(int note, int velocity) {
        if (velocity == 0) {
            noteOff(note);
            return;
        }
        boolean legato = !heldNotes.isEmpty();
        heldNotes.remove(note);
        heldNotes.addLast(note);
        start(note, velocity, legato);
    }

    /** */
    public void noteOff(int note) {
        if (!heldNotes.remove(note)) {
            return;
        }
        if (heldNotes.isEmpty()) {
            noteHeld = false;
            for (Envelope envelope : envelopes) {
                envelope.gateOff();
            }
        } else {
            // fall back to the note that is still down, as a monophonic synthesizer does
            start(heldNotes.peekLast(), velocity, true);
        }
    }

    /** */
    public void allNotesOff() {
        heldNotes.clear();
        noteHeld = false;
        for (Envelope envelope : envelopes) {
            envelope.gateOff();
        }
    }

    /** Stops at once, without a release. */
    public void allSoundOff() {
        heldNotes.clear();
        noteHeld = false;
        gateSmoother.reset(0);
        for (Envelope envelope : envelopes) {
            envelope.reset();
        }
        for (Oscillator oscillator : oscillators) {
            oscillator.reset();
        }
        filter1.reset();
        filter2.reset();
        filter3.reset();
        exciter.reset();
        enhancer.reset();
        toneControl.reset();
        decimator1.reset();
        decimator2.reset();
    }

    /** */
    private void start(int note, int velocity, boolean legato) {
        this.note = note;
        this.velocity = velocity;
        this.noteHeld = true;
        this.glideTarget = note;

        if (!legato || !glides()) {
            glideCurrent = note;
            glideSmoother.reset(note);
        }
        glideDistance = Math.abs(glideTarget - glideCurrent);
        if (!breathReceived) {
            // no wind controller in front of us, let velocity stand in for the breath
            breathTarget = velocity / 127f;
        }
        for (int i = 0; i < 4; i++) {
            if (!legato || egRetrigger[i]) {
                envelopes[i].gateOn(egRetrigger[i]);
            }
        }
        if (!legato) {
            for (Lfo lfo : lfos) {
                lfo.trigger();
            }
        }
    }

    /** the breath controller, 0 .. 1 */
    public void setBreath(float breath) {
        this.breathTarget = Math.max(0, Math.min(1, breath));
        this.breathReceived = true;
    }

    /** */
    public float getBreath() {
        return breathTarget;
    }

    /**
     * How the travel of the breath controller becomes loudness, which is the player's setting
     * rather than the tone's. it reaches the amplifiers only, so that the tone keeps its own
     * filters answering breath over the whole of the travel.
     *
     * @see BreathResponse
     */
    public void setBreathResponse(BreathResponse breathResponse) {
        this.breathResponse = Objects.requireNonNull(breathResponse);
    }

    /** */
    public BreathResponse getBreathResponse() {
        return breathResponse;
    }

    /** the pitch bend wheel, -1 .. 1 */
    public void setPitchBend(float bend) {
        this.bendTarget = Math.max(-1, Math.min(1, bend));
    }

    /** MIDI CC 65, honoured only while {@code Glide MIDI Enabled} is on */
    public void setPortamento(boolean on) {
        this.portamento = on;
    }

    /** MIDI CC 5, a glide speed of its own and the {@code Glide} modulation source */
    public void setPortamentoTime(float time) {
        this.portamentoTime = time;
    }

    /** */
    public boolean isNoteHeld() {
        return noteHeld;
    }

    /** the note that is sounding */
    public int getNote() {
        return note;
    }

    /** whether the engine still has anything to render */
    public boolean isActive() {
        if (noteHeld || gateSmoother.value() > 1e-4f) {
            return true;
        }
        for (Envelope envelope : envelopes) {
            if (!envelope.isIdle()) {
                return true;
            }
        }
        return false;
    }

    /** Adds one block of stereo audio to the buffers. */
    public void render(float[] left, float[] right, int offset, int length) {
        for (int i = 0; i < length; i++) {
            enhancer.process(renderMono(), stereo);
            left[offset + i] += stereo[0];
            right[offset + i] += stereo[1];
        }
    }

    /** Renders one sample of the whole instrument, before the enhancer makes it stereo. */
    private float renderMono() {
        float breath = breathSmoother.process(breathTarget);
        float bend = bendSmoother.process(bendTarget);
        // the gate has one time for opening and another for closing, so it is set as it turns
        gateSmoother.setTime(knob(noteHeld ? GateOpenSmoothTime : GateCloseSmoothTime));
        float gate = gateSmoother.process(noteHeld ? 1 : 0);
        float pitch = glideSmoother.process(glide());

        for (int i = 0; i < 4; i++) {
            egLevels[i] = envelopes[i].process(mod[EG_RATE[i]]);
        }
        for (Lfo lfo : lfos) {
            lfo.process();
        }
        modulate(breath, bend, gate);

        float note = pitch + knob(Transpose) + knob(MasterTune) / 100
                + bend * knob(PitchBendRange) + mod[ModDestination.OscMasterPitch.ordinal()];
        float masterWidth = mod[ModDestination.OscMasterPwmWidth.ordinal()];

        updateFilters(pitch, breath, gate);

        float amp1In = 0, amp2In = 0;
        for (int step = 0; step < oversample; step++) {
            float out1 = 0, out2 = 0;
            for (int i = 0; i < 4; i++) {
                if (!oscUsed[i]) {
                    continue;
                }
                IfwParameter[] o = OSC[i];
                float n = clamp(note + oscDetune[i] + mod[OSC_PITCH[i]], 0, WaveBank.NOTES - 1);
                int index = (int) n;
                float increment = WaveBank.frequency(index) * WaveBank.fine(n - index) / (sampleRate * oversample);

                float loFi = i == 3 ? knob(o[WIDTH]) / 10 : 0;
                float width = (knob(o[WIDTH]) + masterWidth * 10 + mod[OSC_PWM_PHASE[i]] * 10) / 10;
                Oscillator oscillator = oscillators[i];
                if (i > 0 && oscSync[i] && oscillators[0].phase() < increment) {
                    oscillator.sync(0);
                }
                oscillator.process(index, increment, mod[OSC_FM[i]], width, oscWaveform[i], loFi,
                        external(oscWaveform[i]));

                float sum = level(o[SAW], OSC_SAW_LEVEL[i]) * oscillator.saw()
                        + level(o[TRI], OSC_TRI_LEVEL[i]) * oscillator.wave()
                        + level(o[THIRD], OSC_THIRD_LEVEL[i]) * oscillator.pulse();
                sum *= OSC_SCALE;
                if (oscOut1[i]) {
                    out1 += sum;
                }
                if (oscOut2[i]) {
                    out2 += sum;
                }
            }

            float first, second;
            switch (connection) {
                case Serial -> {
                    first = filter2.process(filter1.process(out1));
                    second = 0;
                }
                case Parallel1 -> {
                    first = filter1.process(out1) + filter2.process(out2);
                    second = 0;
                }
                default -> {
                    first = filter1.process(out1);
                    second = filter2.process(out2);
                }
            }
            amp1In = decimator1.process(first);
            amp2In = decimator2.process(second);
        }

        amp1In = filter3.process(amp1In);

        // the amplifiers hear the player's curve, the filters and the matrix the controller itself
        float loudness = breathResponse.apply(breath);
        float master = mod[ModDestination.AmpMasterLevel.ordinal()];
        float amp1 = amp(Amp1Level, Amp1Breath, amp1SubMod, ModDestination.Amp1Level, master, loudness, gate);
        float amp2 = amp(Amp2Level, Amp2Breath, amp2SubMod, ModDestination.Amp2Level, master, loudness, gate);

        float out = exciter.process(toneControl.process(amp1In * amp1 + amp2In * amp2));
        float level = knob(MasterLevel) / 10;
        return out * level * level;
    }

    /** Follows the knobs, the breath and the matrix with everything that costs a transcendental. */
    private void updateFilters(float pitch, float breath, float gate) {
        filter1.set(filter1Type, cutoff(Filter1Frequency, Filter1KeyTrack, Filter1Breath, filter1SubMod,
                        filter1Type, ModDestination.Filter1Frequency, pitch, breath, gate),
                knob(Filter1Resonance) + mod[ModDestination.Filter1Resonance.ordinal()]);
        filter2.set(filter2Type, cutoff(Filter2Frequency, Filter2KeyTrack, Filter2Breath, filter2SubMod,
                        filter2Type, ModDestination.Filter2Frequency, pitch, breath, gate),
                knob(Filter2Resonance) + mod[ModDestination.Filter2Resonance.ordinal()]);
        filter3.set(filter3Enabled, knob(Filter3Stage),
                knob(Filter3Frequency) + mod[ModDestination.Filter3Frequency.ordinal()],
                knob(Filter3Resonance) + mod[ModDestination.Filter3Resonance.ordinal()],
                knob(Filter3Mix) + mod[ModDestination.Filter3Mix.ordinal()]);
        exciter.set(knob(ExciterFrequency) + mod[ModDestination.ExciterFrequency.ordinal()],
                knob(ExciterMix) + mod[ModDestination.ExciterMix.ordinal()]);
        float delay = mod[ModDestination.EnhancerDelay.ordinal()];
        enhancer.set(knob(EnhancerDelayL) + delay, knob(EnhancerDelayR) + delay,
                knob(EnhancerMix) + mod[ModDestination.EnhancerMix.ordinal()]);
    }

    /**
     * Where one of the two filters stands, in semitones.
     * <p>
     * the knob, the mode's own corner, the key it is played at, the breath controller and the
     * matrix are all semitones, so they are added up before anything is worked out in hertz.
     */
    private float cutoff(IfwParameter knob, IfwParameter keyTrack, IfwParameter breathKnob,
                         BreathSubMod subMod, FilterType type, ModDestination destination,
                         float pitch, float breath, float gate) {
        return Filter.cutoff(knob(knob)) + CUTOFF_ORIGIN + type.cutoffOffset()
                + knob(keyTrack) / 10 * FILTER_KEY_TRACK * (pitch - 60)
                + breath * subMod(subMod, gate) * knob(breathKnob) * FILTER_BREATH
                + mod[destination.ordinal()];
    }

    /** Runs the eight modulation slots and leaves the totals in {@link #mod}. */
    private void modulate(float breath, float bend, float gate) {
        Arrays.fill(mod, 0);
        for (int i = 0; i < 8; i++) {
            ModDestination destination = modDestination[i];
            if (modSource1[i] == ModSource.None || destination == ModDestination.None) {
                continue;
            }
            float depth = knob(MOD_DEPTH[i]) / 10;
            if (depth == 0) {
                continue;
            }
            float a = source(modSource1[i], breath, bend, gate);
            float b = modSource2[i] == ModSource.None ? 1 : source(modSource2[i], breath, bend, gate);
            mod[destination.ordinal()] += a * b * destination.scale() * depth;
        }
    }

    /** the value of one modulation source, after its own scale and offset */
    private float source(ModSource source, float breath, float bend, float gate) {
        float raw = switch (source) {
            case None -> 0;
            case Breath, BreathBipoler -> breath;
            case PitchBend -> bend;
            case Glide -> Math.max(0, portamentoTime);
            case Note, NoteBipolar -> note;
            case Velocity, VelocityBipoler -> velocity / 127f;
            case Gate -> gate;
            case Eg1, Eg1Bipoler -> egLevels[0];
            case Eg2, Eg2Bipoler -> egLevels[1];
            case Eg3, Eg3Bipoler -> egLevels[2];
            case Eg4, Eg4Bipoler -> egLevels[3];
            case Lfo1, Lfo1Bipoler -> lfos[0].value();
            case Lfo2, Lfo2Bipoler -> lfos[1].value();
            default -> oscillatorSource(source);
        };
        return raw * source.scale() + source.offset();
    }

    /** the audio rate sources, which read one oscillator slot or the whole oscillator */
    private float oscillatorSource(ModSource source) {
        String name = source.name();
        int i = name.charAt(3) - '1';
        Oscillator oscillator = oscillators[i];
        String slot = name.substring(4).replace("Bipoler", "");
        return switch (slot) {
            case "Saw" -> oscillator.saw();
            case "Tri" -> oscillator.wave();
            case "Pwm", "Noise" -> oscillator.pulse();
            // the whole oscillator, as its own level knobs mix it
            default -> (knob(OSC[i][SAW]) * oscillator.saw() + knob(OSC[i][TRI]) * oscillator.wave()
                    + knob(OSC[i][THIRD]) * oscillator.pulse()) / 10;
        };
    }

    /** the host input an {@code Ext.In} waveform reads */
    private float external(Waveform waveform) {
        return waveform.isExternal()
                ? externalLeft * waveform.externalLeft() + externalRight * waveform.externalRight()
                : 0;
    }

    /**
     * What a BREATH knob is actually fed with.
     * <p>
     * the breath controller itself is already a factor of everything a BREATH knob reaches, so
     * this is the other one: the gate for {@code BREATH}, an envelope for {@code BRxEGn}, which
     * is how IFW gives a wind patch an attack without spending a modulation slot.
     */
    private float subMod(BreathSubMod subMod, float gate) {
        int eg = subMod.eg();
        return eg < 0 ? gate : egLevels[eg];
    }

    /** an amp, whose BREATH knob says how much of its level the breath controller owns */
    private float amp(IfwParameter levelKnob, IfwParameter breathKnob, BreathSubMod subMod,
                      ModDestination destination, float master, float breath, float gate) {
        float depth = knob(breathKnob) / 10;
        float value = breath * subMod(subMod, gate) * depth + mod[destination.ordinal()] + master;
        return clamp(value * knob(levelKnob) / 10, 0, 1);
    }

    /** an oscillator slot level, 0 .. 1, after whatever the matrix adds to it */
    private float level(IfwParameter parameter, int destination) {
        return clamp(knob(parameter) / 10 + mod[destination], 0, 1);
    }

    /** Steps the glide one sample and returns where the pitch stands. */
    private float glide() {
        if (glideCurrent == glideTarget) {
            return glideTarget;
        }
        float distance = glideTarget - glideCurrent;
        // the distance is the one the glide set out to cover, so that it holds a steady speed
        float step = rate() * (glideMode == GlideMode.Rate ? 12 : glideDistance);
        if (Math.abs(distance) <= step) {
            glideCurrent = glideTarget;
        } else {
            glideCurrent += Math.signum(distance) * step;
        }
        return glideCurrent;
    }

    /**
     * How much of a glide one sample covers.
     * <p>
     * the knob is a square the wrong way round, so that the whole of the useful range sits at
     * the bottom of it: turned up it takes longer, and turned all the way down it is off.
     */
    private float rate() {
        float x = speedKnob() / 10;
        return x <= 0 ? 1 : (float) (2 / 3d / (x * x * sampleRate));
    }

    /** whether the program and the controller together ask for a glide */
    private boolean glides() {
        return (!glideMidiEnabled || portamento) && speedKnob() > 0;
    }

    /** the glide speed knob, which MIDI CC 5 can only make slower */
    private float speedKnob() {
        return glideMidiEnabled && portamentoTime >= 0
                ? Math.max(portamentoTime * 10, knob(GlideSpeed))
                : knob(GlideSpeed);
    }

    /** */
    private float knob(IfwParameter parameter) {
        return knobs[parameter.ordinal()];
    }

    /** */
    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * What brings the oversampled voice back down to the sample rate.
     * <p>
     * the plug-in runs its oscillators and its ladder four times over and then takes them
     * through one 20 kHz section on the way out, which is enough because the wavetables were
     * band limited before any of it started.
     */
    private static final class Decimator {

        private final float b0, b1, b2, a1, a2;
        private float x1, x2, y1, y2;

        Decimator(float sampleRate) {
            double w = 2 * Math.PI * 20000 / sampleRate;
            double s = Math.sin(w);
            double c = Math.cos(w);
            double k = (.5 - .25 * s) / (1 + .5 * s);
            double d = c * (k + .5);
            double e = k + .5 - d;
            this.b0 = (float) (e / 2);
            this.b1 = (float) e;
            this.b2 = (float) (e / 2);
            this.a1 = (float) (-2 * d);
            this.a2 = (float) (2 * k);
        }

        void reset() {
            x1 = x2 = y1 = y2 = 0;
        }

        float process(float in) {
            float out = b0 * in + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = in;
            y2 = y1;
            y1 = out;
            return out;
        }
    }
}
