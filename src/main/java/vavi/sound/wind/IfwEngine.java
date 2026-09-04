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
 * IfwEngine. one monophonic IFW instrument.
 * <p>
 * a wind controller plays one note at a time and shapes it with breath rather than with
 * velocity, so IFW is monophonic and nearly everything worth hearing hangs off MIDI CC 2.
 * this class is one whole instrument, oscillators through master level, and the MIDI side
 * gives every channel one of its own.
 * <p>
 * signal flow, following the {@link FilterConnection} the program picks:
 * <pre>
 * OSC 1..4 -&gt; out 1 / out 2 -&gt; FILTER 1, 2, 3 -&gt; AMP 1, 2 -&gt; EQ -&gt; EXCITER -&gt; ENHANCER -&gt; MASTER
 * </pre>
 * modulation is one flat scheme: a slot multiplies its two sources, scales the product by
 * its depth, and adds the result to its destination in the units of the destination's own
 * knob, so a depth of 10 sweeps a knob across its whole range. pitch destinations count in
 * semitones instead, and an {@code EG n Rate} destination halves the envelope times for
 * every two units of depth.
 * <p>
 * a controller that sends no breath would leave a normal program silent, so until the
 * first CC 2 arrives the note velocity stands in for the breath. that lets an ordinary
 * MIDI file play through the same engine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @see IfwProgram
 */
public class IfwEngine {

    /** */
    public static final float DEFAULT_SAMPLE_RATE = 44100;

    /** MIDI CC 2, what a wind controller blows into */
    public static final int BREATH_CONTROLLER = 2;

    /** where the master limiter stops being transparent, about -3 dBFS */
    private static final float KNEE = .7f;

    /** how often the filters and the effects follow the knobs, in samples */
    private static final int CONTROL_INTERVAL = 8;

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

    /** */
    private final float sampleRate;

    /** */
    private IfwProgram program = new IfwProgram();

    /** a snapshot of {@link #program}, indexed by {@link IfwParameter#ordinal()} */
    private final float[] knobs = new float[IfwParameter.values().length];

    // everything the program chooses rather than dials, resolved once per program change

    private final boolean[] oscSync = new boolean[4], oscOut1 = new boolean[4], oscOut2 = new boolean[4];
    private final Waveform[] oscWaveform = new Waveform[4];
    private final PcmWaveform[] oscPcm = new PcmWaveform[4];
    /** the semi and tune knobs as a frequency factor */
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
    /** whether any modulation slot reaches the effects, which are otherwise set once */
    private boolean modulatesEffects;

    private final Oscillator[] oscillators = new Oscillator[4];
    private final Envelope[] envelopes = new Envelope[4];
    private final Lfo[] lfos = new Lfo[2];
    private final Filter filter1, filter2;
    private final Phaser filter3;
    private final Exciter exciter;
    private final Enhancer enhancer;
    private final ToneControl toneControl;

    private final Smoother breathSmoother, bendSmoother, glideSmoother;

    /** the gate opens and closes at its own two times, so it is smoothed by hand */
    private float gate, gateOpen, gateClose;

    /** counts down to the next control rate update */
    private int controlCountdown;

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

    /** 0 .. 1 */
    private float breathTarget;

    /** whether a breath controller has ever been heard on this engine */
    private boolean breathReceived;

    /** the player's end of the breath controller, outside the tone */
    private BreathResponse breathResponse = BreathResponse.DEFAULT;

    /** -1 .. 1 */
    private float bendTarget;

    /** MIDI CC 65, and CC 5 as an override of the glide speed knob, negative when unset */
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
        for (int i = 0; i < 4; i++) {
            oscillators[i] = new Oscillator(sampleRate, i == 3);
            envelopes[i] = new Envelope(sampleRate);
        }
        for (int i = 0; i < 2; i++) {
            lfos[i] = new Lfo(sampleRate);
        }
        filter1 = new Filter(sampleRate);
        filter2 = new Filter(sampleRate);
        filter3 = new Phaser(sampleRate);
        exciter = new Exciter(sampleRate);
        enhancer = new Enhancer(sampleRate);
        toneControl = new ToneControl(sampleRate);
        breathSmoother = new Smoother(sampleRate);
        bendSmoother = new Smoother(sampleRate);
        glideSmoother = new Smoother(sampleRate);
        apply(program);
    }

    /** */
    public float getSampleRate() {
        return sampleRate;
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
            oscPcm[i] = program.choice(IfwParameter.valueOf("Osc" + n + "Waveform3"), PcmWaveform.class);
            oscDetune[i] = Oscillator.detune(knob(OSC[i][SEMI]), knob(OSC[i][TUNE]));
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

        modulatesEffects = false;
        for (int i = 0; i < 8; i++) {
            modulatesEffects |= switch (modDestination[i]) {
                case ExciterFrequency, ExciterMix, EnhancerDelay, EnhancerMix -> knob(MOD_DEPTH[i]) != 0;
                default -> false;
            };
        }

        breathSmoother.setTime(knob(BreathSmoothTime));
        bendSmoother.setTime(knob(PitchBendSmoothTime));
        glideSmoother.setTime(knob(GlideSmoothTime));
        gateOpen = coefficient(knob(GateOpenSmoothTime));
        gateClose = coefficient(knob(GateCloseSmoothTime));

        exciter.set(knob(ExciterFrequency), knob(ExciterMix));
        enhancer.set(knob(EnhancerDelayL), knob(EnhancerDelayR), knob(EnhancerMix));
        toneControl.set(knob(EqLowGain), knob(EqHighGain));
        controlCountdown = 0;
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
     * a note that arrives while another is down is a legato note: it glides, and it
     * restarts only the envelopes whose {@code RETRIGGER} is on.
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
        gate = 0;
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
     * How the travel of the breath controller becomes loudness, which is the player's
     * setting rather than the tone's. it reaches the amplifiers only, so that the tone
     * keeps its own filters answering breath over the whole of the travel.
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

    /** MIDI CC 5 as a glide speed knob, 0 .. 1, negative to fall back to the program */
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
        if (noteHeld || gate > 1e-4f) {
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
            left[offset + i] += limit(stereo[0]);
            right[offset + i] += limit(stereo[1]);
        }
    }

    /**
     * Holds the output inside full scale.
     * <p>
     * a program is free to run four oscillators of three slots each into both busses and
     * then into both amps, which is what the CLIP lamp on the IFW panel is for. rather
     * than let that square off, everything below -3 dBFS passes untouched and the rest
     * bends into a knee that never quite reaches one.
     */
    private static float limit(float value) {
        float magnitude = Math.abs(value);
        if (magnitude <= KNEE) {
            return value;
        }
        float over = (magnitude - KNEE) / (1 - KNEE);
        return Math.signum(value) * (KNEE + (1 - KNEE) * over / (float) Math.sqrt(1 + over * over));
    }

    /** Renders one sample of the whole instrument, before the enhancer makes it stereo. */
    private float renderMono() {
        float breath = breathSmoother.process(breathTarget);
        float bend = bendSmoother.process(bendTarget);
        float gateTarget = noteHeld ? 1 : 0;
        gate += (gateTarget - gate) * (gateTarget > gate ? gateOpen : gateClose);
        float pitch = glideSmoother.process(glide());

        for (int i = 0; i < 4; i++) {
            float rate = mod[EG_RATE[i]];
            egLevels[i] = envelopes[i].process(rate == 0 ? 1 : (float) Math.pow(2, -rate / 2));
        }
        for (Lfo lfo : lfos) {
            lfo.process();
        }
        modulate(breath, bend);

        float semitones = pitch + knob(Transpose) + knob(MasterTune) / 100
                + bend * knob(PitchBendRange) + mod[ModDestination.OscMasterPitch.ordinal()];
        float base = (float) (440 * Math.pow(2, (semitones - 69) / 12));
        float masterWidth = mod[ModDestination.OscMasterPwmWidth.ordinal()];

        float out1 = 0, out2 = 0;
        boolean masterWrapped = false;
        for (int i = 0; i < 4; i++) {
            IfwParameter[] o = OSC[i];
            float frequency = base * oscDetune[i];
            float pitchMod = mod[OSC_PITCH[i]];
            if (pitchMod != 0) {
                frequency *= (float) Math.pow(2, pitchMod / 12);
            }
            float loFi = i == 3 ? knob(o[WIDTH]) : 0;
            float width = (knob(o[WIDTH]) + masterWidth) / 10;

            boolean wrapped = oscillators[i].process(frequency, mod[OSC_FM[i]] * .5f, width,
                    mod[OSC_PWM_PHASE[i]] / 10, oscWaveform[i], oscPcm[i], loFi,
                    external(oscWaveform[i]), i > 0 && oscSync[i] && masterWrapped);
            if (i == 0) {
                masterWrapped = wrapped;
            }

            float sum = level(o[SAW], OSC_SAW_LEVEL[i]) * oscillators[i].saw()
                    + level(o[TRI], OSC_TRI_LEVEL[i]) * oscillators[i].wave()
                    + level(o[THIRD], OSC_THIRD_LEVEL[i]) * oscillators[i].pulse();
            if (oscOut1[i]) {
                out1 += sum;
            }
            if (oscOut2[i]) {
                out2 += sum;
            }
        }

        // four oscillators of three slots each, keep the busses in a sane place
        out1 *= .25f;
        out2 *= .25f;

        if (--controlCountdown <= 0) {
            controlCountdown = CONTROL_INTERVAL;
            updateFilters(pitch, breath);
        }

        float amp1In, amp2In;
        switch (connection) {
            case Serial -> {
                float x = filter2.process(filter1.process(out1));
                amp1In = filter3Enabled ? filter3.process(x) : x;
                amp2In = 0;
            }
            case Parallel1 -> {
                float x = filter1.process(out1) + filter2.process(out2);
                amp1In = filter3Enabled ? filter3.process(x) : x;
                amp2In = 0;
            }
            default -> {
                float x = filter1.process(out1);
                amp1In = filter3Enabled ? filter3.process(x) : x;
                amp2In = filter2.process(out2);
            }
        }

        // the amplifiers hear the player's curve, the filters and the matrix the controller itself
        float loudness = breathResponse.apply(breath);
        float master = mod[ModDestination.AmpMasterLevel.ordinal()];
        float amp1 = amp(Amp1Level, Amp1Breath, amp1SubMod, ModDestination.Amp1Level, master, loudness);
        float amp2 = amp(Amp2Level, Amp2Breath, amp2SubMod, ModDestination.Amp2Level, master, loudness);

        float out = (amp1In * amp1 + amp2In * amp2) * gate;
        out = exciter.process(toneControl.process(out));
        return out * knob(MasterLevel) / 10;
    }

    /** Follows the knobs, the breath and the matrix with everything that costs a transcendental. */
    private void updateFilters(float pitch, float breath) {
        float keyTrack1 = knob(Filter1KeyTrack) / 10 * (pitch - 60) / 12;
        float keyTrack2 = knob(Filter2KeyTrack) / 10 * (pitch - 60) / 12;
        filter1.set(filter1Type,
                knob(Filter1Frequency) + keyTrack1 + knob(Filter1Breath) * subMod(filter1SubMod, breath)
                        + mod[ModDestination.Filter1Frequency.ordinal()],
                knob(Filter1Resonance) + mod[ModDestination.Filter1Resonance.ordinal()]);
        filter2.set(filter2Type,
                knob(Filter2Frequency) + keyTrack2 + knob(Filter2Breath) * subMod(filter2SubMod, breath)
                        + mod[ModDestination.Filter2Frequency.ordinal()],
                knob(Filter2Resonance) + mod[ModDestination.Filter2Resonance.ordinal()]);
        if (filter3Enabled) {
            filter3.set(knob(Filter3Stage),
                    knob(Filter3Frequency) + mod[ModDestination.Filter3Frequency.ordinal()],
                    knob(Filter3Resonance) + mod[ModDestination.Filter3Resonance.ordinal()],
                    knob(Filter3Mix) + mod[ModDestination.Filter3Mix.ordinal()]);
        }
        if (modulatesEffects) {
            exciter.set(knob(ExciterFrequency) + mod[ModDestination.ExciterFrequency.ordinal()],
                    knob(ExciterMix) + mod[ModDestination.ExciterMix.ordinal()]);
            float delay = mod[ModDestination.EnhancerDelay.ordinal()] * 5;
            enhancer.set(knob(EnhancerDelayL) + delay, knob(EnhancerDelayR) + delay,
                    knob(EnhancerMix) + mod[ModDestination.EnhancerMix.ordinal()]);
        }
    }

    /** Runs the eight modulation slots and leaves the totals in {@link #mod}. */
    private void modulate(float breath, float bend) {
        Arrays.fill(mod, 0);
        for (int i = 0; i < 8; i++) {
            if (modSource1[i] == ModSource.None || modDestination[i] == ModDestination.None) {
                continue;
            }
            float depth = knob(MOD_DEPTH[i]);
            if (depth == 0) {
                continue;
            }
            mod[modDestination[i].ordinal()] +=
                    source(modSource1[i], breath, bend) * source(modSource2[i], breath, bend) * depth;
        }
    }

    /** the value of one modulation source, 1 for {@link ModSource#None} so that it can be a factor */
    private float source(ModSource source, float breath, float bend) {
        boolean bipolar = source.isBipolar();
        return switch (source) {
            case None -> 1;
            case Breath, BreathBipoler -> bipolar ? breath * 2 - 1 : breath;
            case PitchBend -> bend;
            case Glide -> Math.min(1, Math.abs(glideTarget - glideCurrent) / 12);
            case Note, NoteBipolar -> bipolar ? (note - 64) / 64f : note / 127f;
            case Velocity, VelocityBipoler -> bipolar ? velocity / 64f - 1 : velocity / 127f;
            case Gate -> noteHeld ? 1 : 0;
            case Eg1, Eg1Bipoler -> polarity(egLevels[0], bipolar);
            case Eg2, Eg2Bipoler -> polarity(egLevels[1], bipolar);
            case Eg3, Eg3Bipoler -> polarity(egLevels[2], bipolar);
            case Eg4, Eg4Bipoler -> polarity(egLevels[3], bipolar);
            case Lfo1, Lfo1Bipoler -> bipolar ? lfos[0].value() : (lfos[0].value() + 1) / 2;
            case Lfo2, Lfo2Bipoler -> bipolar ? lfos[1].value() : (lfos[1].value() + 1) / 2;
            default -> oscillatorSource(source, bipolar);
        };
    }

    /** the audio rate sources, which read one oscillator slot or the whole oscillator */
    private float oscillatorSource(ModSource source, boolean bipolar) {
        String name = source.name();
        int i = name.charAt(3) - '1';
        Oscillator oscillator = oscillators[i];
        String slot = name.substring(4).replace("Bipoler", "");
        float value = switch (slot) {
            case "Saw" -> oscillator.saw();
            case "Tri" -> oscillator.wave();
            case "Pwm", "Noise" -> oscillator.pulse();
            // the whole oscillator, as its own level knobs mix it
            default -> (knob(OSC[i][SAW]) * oscillator.saw() + knob(OSC[i][TRI]) * oscillator.wave()
                    + knob(OSC[i][THIRD]) * oscillator.pulse()) / 10;
        };
        return bipolar ? value : (value + 1) / 2;
    }

    /** */
    private static float polarity(float level, boolean bipolar) {
        return bipolar ? level * 2 - 1 : level;
    }

    /** the host input an {@code Ext.In} waveform reads */
    private float external(Waveform waveform) {
        return switch (waveform) {
            case ExtInR -> externalRight;
            case ExtInLR -> (externalLeft + externalRight) / 2;
            default -> externalLeft;
        };
    }

    /** what a BREATH knob is actually fed with, the breath alone or scaled by an envelope */
    private float subMod(BreathSubMod subMod, float breath) {
        int eg = subMod.eg();
        return eg < 0 ? breath : breath * egLevels[eg];
    }

    /** an amp, whose BREATH knob says how much of its level the breath controller owns */
    private float amp(IfwParameter levelKnob, IfwParameter breathKnob, BreathSubMod subMod,
                      ModDestination destination, float master, float breath) {
        float level = (knob(levelKnob) + mod[destination.ordinal()] + master) / 10;
        float depth = knob(breathKnob) / 10;
        return Math.max(0, level) * (1 - depth + depth * subMod(subMod, breath));
    }

    /** an oscillator slot level, 0 .. 1, after whatever the matrix adds to it */
    private float level(IfwParameter parameter, int destination) {
        return Math.max(0, Math.min(1, (knob(parameter) + mod[destination]) / 10));
    }

    /** Steps the glide one sample and returns where the pitch stands. */
    private float glide() {
        if (glideCurrent == glideTarget) {
            return glideTarget;
        }
        float distance = glideTarget - glideCurrent;
        float step = glideMode == GlideMode.Rate
                ? semitonesPerSecond() / sampleRate
                : Math.abs(distance) / Math.max(1, glideSeconds() * sampleRate);
        if (Math.abs(distance) <= step) {
            glideCurrent = glideTarget;
        } else {
            glideCurrent += Math.signum(distance) * step;
        }
        return glideCurrent;
    }

    /** whether the program and the controller together ask for a glide */
    private boolean glides() {
        return (!glideMidiEnabled || portamento) && speedKnob() > 0;
    }

    /** the glide speed knob, or MIDI CC 5 when the program lets it through */
    private float speedKnob() {
        return glideMidiEnabled && portamentoTime >= 0 ? portamentoTime * 10 : knob(GlideSpeed);
    }

    /** the speed knob 0 .. 10 as a duration, one millisecond to about three seconds */
    private float glideSeconds() {
        return (float) (0.001 * Math.pow(10, speedKnob() * 0.35));
    }

    /** the speed knob 0 .. 10 as a slope, 100 down to 1 semitone per second */
    private float semitonesPerSecond() {
        return (float) (100 * Math.pow(10, -speedKnob() * 0.2));
    }

    /** */
    private float knob(IfwParameter parameter) {
        return knobs[parameter.ordinal()];
    }

    /** a smoothing time in milliseconds as a one pole coefficient */
    private float coefficient(float milliseconds) {
        return milliseconds <= 0 ? 1 : (float) -Math.expm1(-1000 / (milliseconds * sampleRate));
    }
}
