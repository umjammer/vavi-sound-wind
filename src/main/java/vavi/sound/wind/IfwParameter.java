/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.util.Locale;


/**
 * IfwParameter. one slot of an IFW program.
 * <p>
 * an IFW program is a fixed table of 200 parameters, always written in this order, and a
 * parameter is addressed by its position. the trailing {@code Dummy} slots are the spare
 * room later versions grew into, which is why {@code Dummy140} sits between the LFO beats
 * and the smoothing times: it was spare too until version 1.0.39. a tone file saved by an
 * older version simply stops early, so a slot it never wrote keeps {@link #defaultValue()}.
 * <p>
 * the display names, the value ranges and the defaults are those of the IFW panel.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @see IfwProgram
 */
public enum IfwParameter {

    Osc1Sync("OSC 1 Sync", Bool.Off),
    Osc1Semi("OSC 1 Semi", -36, 36, 0f), // semitones
    Osc1Tune("OSC 1 Tune", -50, 50, 0f), // cents
    Osc1SawLevel("OSC 1 SAW Level", 0, 10, 10f),
    Osc1TriLevel("OSC 1 TRI Level", 0, 10, 0f),
    Osc1PwmLevel("OSC 1 PWM Level", 0, 10, 0f),
    Osc1PwmWidth("OSC 1 PWM Width", 0, 10, 5f), // 5 means a 50% duty pulse
    Osc1Output1("OSC 1 Output 1", Bool.On),
    Osc1Output2("OSC 1 Output 2", Bool.Off),
    Osc2Sync("OSC 2 Sync", Bool.Off),
    Osc2Semi("OSC 2 Semi", -36, 36, 0f), // semitones
    Osc2Tune("OSC 2 Tune", -50, 50, 5f), // cents
    Osc2SawLevel("OSC 2 SAW Level", 0, 10, 10f),
    Osc2TriLevel("OSC 2 TRI Level", 0, 10, 0f),
    Osc2PwmLevel("OSC 2 PWM Level", 0, 10, 0f),
    Osc2PwmWidth("OSC 2 PWM Width", 0, 10, 5f), // 5 means a 50% duty pulse
    Osc2Output1("OSC 2 Output 1", Bool.On),
    Osc2Output2("OSC 2 Output 2", Bool.Off),
    Osc3Sync("OSC 3 Sync", Bool.Off),
    Osc3Semi("OSC 3 Semi", -36, 36, 0f), // semitones
    Osc3Tune("OSC 3 Tune", -50, 50, 0f), // cents
    Osc3SawLevel("OSC 3 SAW Level", 0, 10, 0f),
    Osc3TriLevel("OSC 3 TRI Level", 0, 10, 0f),
    Osc3PwmLevel("OSC 3 PWM Level", 0, 10, 0f),
    Osc3PwmWidth("OSC 3 PWM Width", 0, 10, 5f), // 5 means a 50% duty pulse
    Osc3Output1("OSC 3 Output 1", Bool.Off),
    Osc3Output2("OSC 3 Output 2", Bool.Off),
    Osc4Sync("OSC 4 Sync", Bool.Off),
    Osc4Semi("OSC 4 Semi", -36, 36, 0f), // semitones
    Osc4Tune("OSC 4 Tune", -50, 50, 0f), // cents
    Osc4SawLevel("OSC 4 SAW Level", 0, 10, 0f),
    Osc4TriLevel("OSC 4 TRI Level", 0, 10, 0f),
    Osc4NoiseLevel("OSC 4 Noise Level", 0, 10, 0f),
    Osc4NoiseLoFi("OSC 4 Noise Lo-Fi", 0, 10, 0f), // sample & hold rate reduction
    Osc4Output1("OSC 4 Output 1", Bool.Off),
    Osc4Output2("OSC 4 Output 2", Bool.Off),
    Filter1Type("Filter 1 Type", FilterType.LPF12),
    Filter1Frequency("Filter 1 Cutoff", 0, 10, 1.7f),
    Filter1Resonance("Filter 1 Resonance", 0, 10, 2f),
    Filter1KeyTrack("Filter 1 Key Track", -10, 10, 0f),
    Filter1Breath("Filter 1 Breath", -10, 10, 7f),
    Filter2Type("Filter 2 Type", FilterType.LPF12),
    Filter2Frequency("Filter 2 Cutoff", 0, 10, 10f),
    Filter2Resonance("Filter 2 Resonance", 0, 10, 0f),
    Filter2KeyTrack("Filter 2 Key Track", -10, 10, 0f),
    Filter2Breath("Filter 2 Breath", -10, 10, 0f),
    FilterConnection("Filter Connection", vavi.sound.wind.FilterConnection.Serial),
    Filter3Enabled("Filter 3 Enabled", Bool.On),
    Filter3Stage("Filter 3 Stage", 2, 24, 22f), // all pass stage count
    Filter3Frequency("Filter 3 Frequency", 0, 10, 7f),
    Filter3Resonance("Filter 3 Resonance", -10, 10, 0f),
    Filter3Mix("Filter 3 Mix", -10, 10, 0f),
    Amp1Breath("AMP 1 Breath", 0, 10, 10f),
    Amp1Level("AMP 1 Level", 0, 10, 10f),
    Amp2Breath("AMP 2 Breath", 0, 10, 10f),
    Amp2Level("AMP 2 Level", 0, 10, 10f),
    Eg1Attack("EG 1 Attack", 0, 10, 2.6f),
    Eg1Decay("EG 1 Decay", 0, 10, 3.1f),
    Eg1Sustain("EG 1 Sustain", 0, 10, 0f),
    Eg1Release("EG 1 Release", 0, 10, 0f),
    Eg1Retrigger("EG 1 Retrigger", Bool.Off),
    Eg2Attack("EG 2 Attack", 0, 10, 0f),
    Eg2Decay("EG 2 Decay", 0, 10, 5f),
    Eg2Sustain("EG 2 Sustain", 0, 10, 10f),
    Eg2Release("EG 2 Release", 0, 10, 0f),
    Eg2Retrigger("EG 2 Retrigger", Bool.Off),
    Eg3Attack("EG 3 Attack", 0, 10, 0f),
    Eg3Decay("EG 3 Decay", 0, 10, 5f),
    Eg3Sustain("EG 3 Sustain", 0, 10, 10f),
    Eg3Release("EG 3 Release", 0, 10, 0f),
    Eg3Retrigger("EG 3 Retrigger", Bool.Off),
    Eg4Attack("EG 4 Attack", 0, 10, 0f),
    Eg4Decay("EG 4 Decay", 0, 10, 5f),
    Eg4Sustain("EG 4 Sustain", 0, 10, 10f),
    Eg4Release("EG 4 Release", 0, 10, 0f),
    Eg4Retrigger("EG 4 Retrigger", Bool.Off),
    Lfo1Waveform("LFO 1 Waveform", LfoWaveform.TRI),
    Lfo1KeySync("LFO 1 Key Sync", Bool.Off),
    Lfo1BPMSync("LFO 1 BPM Sync", Bool.Off),
    Lfo1Speed("LFO 1 Speed", 0, 10, 2f),
    Lfo2Waveform("LFO 2 Waveform", LfoWaveform.TRI),
    Lfo2KeySync("LFO 2 Key Sync", Bool.Off),
    Lfo2BpmSync("LFO 2 BPM Sync", Bool.Off),
    Lfo2Speed("LFO 2 Speed", 0, 10, 2f),
    Mod1Source1("MOD 1 Source 1", ModSource.Eg1),
    Mod1Source2("MOD 1 Source 2", ModSource.None),
    Mod1Destination("MOD 1 Destination", ModDestination.Osc1Pitch),
    Mod1Depth("MOD 1 Depth", -10, 10, 1f),
    Mod2Source1("MOD 2 Source 1", ModSource.None),
    Mod2Source2("MOD 2 Source 2", ModSource.None),
    Mod2Destination("MOD 2 Destination", ModDestination.None),
    Mod2Depth("MOD 2 Depth", -10, 10, 0f),
    Mod3Source1("MOD 3 Source 1", ModSource.None),
    Mod3Source2("MOD 3 Source 2", ModSource.None),
    Mod3Destination("MOD 3 Destination", ModDestination.None),
    Mod3Depth("MOD 3 Depth", -10, 10, 0f),
    Mod4Source1("MOD 4 Source 1", ModSource.None),
    Mod4Source2("MOD 4 Source 2", ModSource.None),
    Mod4Destination("MOD 4 Destination", ModDestination.None),
    Mod4Depth("MOD 4 Depth", -10, 10, 0f),
    Mod5Source1("MOD 5 Source 1", ModSource.None),
    Mod5Source2("MOD 5 Source 2", ModSource.None),
    Mod5Destination("MOD 5 Destination", ModDestination.None),
    Mod5Depth("MOD 5 Depth", -10, 10, 0f),
    Mod6Source1("MOD 6 Source 1", ModSource.None),
    Mod6Source2("MOD 6 Source 2", ModSource.None),
    Mod6Destination("MOD 6 Destination", ModDestination.None),
    Mod6Depth("MOD 6 Depth", -10, 10, 0f),
    Mod7Source1("MOD 7 Source 1", ModSource.None),
    Mod7Source2("MOD 7 Source 2", ModSource.None),
    Mod7Destination("MOD 7 Destination", ModDestination.None),
    Mod7Depth("MOD 7 Depth", -10, 10, 0f),
    Mod8Source1("MOD 8 Source 1", ModSource.None),
    Mod8Source2("MOD 8 Source 2", ModSource.None),
    Mod8Destination("MOD 8 Destination", ModDestination.None),
    Mod8Depth("MOD 8 Depth", -10, 10, 0f),
    Transpose("Transpose", -24, 24, 0f), // semitones
    PitchBendRange("Pitch Bend Range", 0, 24, 2f), // semitones
    GlideMidiEnabled("Glide MIDI Enabled", Bool.On),
    GlideMode("Glide Mode", vavi.sound.wind.GlideMode.Time),
    GlideSpeed("Glide Speed", 0, 10, 0f),
    EqLowGain("EQ Low Gain", -18, 18, 0f), // dB
    EqHighGain("EQ High Gain", -18, 18, 0f), // dB
    ExciterFrequency("Exciter Frequency", 0, 10, 9f),
    ExciterMix("Exciter Mix", -10, 10, 0f),
    EnhancerDelayL("Enhancer Delay L", 0, 50, 10f), // ms
    EnhancerDelayR("Enhancer Delay R", 0, 50, 20f), // ms
    EnhancerMix("Enhancer Mix", -10, 10, 0f),
    MasterTune("Master Tune", -50, 50, 0f), // cents
    MasterLevel("Master Level", 0, 10, 10f),
    Osc1Waveform2("OSC 1 Waveform 2", Waveform.TRI),
    Osc2Waveform2("OSC 2 Waveform 2", Waveform.TRI),
    Osc3Waveform2("OSC 3 Waveform 2", Waveform.TRI),
    Osc4Waveform2("OSC 4 Waveform 2", Waveform.TRI),
    Osc1Waveform3("OSC 1 Waveform 3", PcmWaveform.None),
    Osc2Waveform3("OSC 2 Waveform 3", PcmWaveform.None),
    Osc3Waveform3("OSC 3 Waveform 3", PcmWaveform.None),
    Osc4Waveform3("OSC 4 Waveform 3", PcmWaveform.None),
    Lfo1Beat("LFO 1 Beat", LfoBeat.B1_4),
    Lfo2Beat("LFO 2 Beat", LfoBeat.B1_4),
    Dummy140("dummy 140", -1000, 1000, 0f),
    GateOpenSmoothTime("Gate Open Smooth Time", 0, 100, 2f), // ms
    GateCloseSmoothTime("Gate Close Smooth Time", 0, 100, 15f), // ms
    PitchBendSmoothTime("Pitch Bend Smooth Time", 0, 100, 5f), // ms
    BreathSmoothTime("Breath Smooth Time", 0, 100, 12f), // ms
    GlideSmoothTime("Glide Smooth Time", 0, 100, 5f), // ms
    Filter1BreathSubMod("Filter 1 Breath Sub Mod", BreathSubMod.BREATH),
    Filter2BreathSubMod("Filter 2 Breath Sub Mod", BreathSubMod.BREATH),
    Amp1BreathSubMod("AMP 1 Breath Sub Mod", BreathSubMod.BREATH),
    Amp2BreathSubMod("AMP 2 Breath Sub Mod", BreathSubMod.BREATH),
    Dummy150("dummy 150", -1000, 1000, 0f),
    Dummy151("dummy 151", -1000, 1000, 0f),
    Dummy152("dummy 152", -1000, 1000, 0f),
    Dummy153("dummy 153", -1000, 1000, 0f),
    Dummy154("dummy 154", -1000, 1000, 0f),
    Dummy155("dummy 155", -1000, 1000, 0f),
    Dummy156("dummy 156", -1000, 1000, 0f),
    Dummy157("dummy 157", -1000, 1000, 0f),
    Dummy158("dummy 158", -1000, 1000, 0f),
    Dummy159("dummy 159", -1000, 1000, 0f),
    Dummy160("dummy 160", -1000, 1000, 0f),
    Dummy161("dummy 161", -1000, 1000, 0f),
    Dummy162("dummy 162", -1000, 1000, 0f),
    Dummy163("dummy 163", -1000, 1000, 0f),
    Dummy164("dummy 164", -1000, 1000, 0f),
    Dummy165("dummy 165", -1000, 1000, 0f),
    Dummy166("dummy 166", -1000, 1000, 0f),
    Dummy167("dummy 167", -1000, 1000, 0f),
    Dummy168("dummy 168", -1000, 1000, 0f),
    Dummy169("dummy 169", -1000, 1000, 0f),
    Dummy170("dummy 170", -1000, 1000, 0f),
    Dummy171("dummy 171", -1000, 1000, 0f),
    Dummy172("dummy 172", -1000, 1000, 0f),
    Dummy173("dummy 173", -1000, 1000, 0f),
    Dummy174("dummy 174", -1000, 1000, 0f),
    Dummy175("dummy 175", -1000, 1000, 0f),
    Dummy176("dummy 176", -1000, 1000, 0f),
    Dummy177("dummy 177", -1000, 1000, 0f),
    Dummy178("dummy 178", -1000, 1000, 0f),
    Dummy179("dummy 179", -1000, 1000, 0f),
    Dummy180("dummy 180", -1000, 1000, 0f),
    Dummy181("dummy 181", -1000, 1000, 0f),
    Dummy182("dummy 182", -1000, 1000, 0f),
    Dummy183("dummy 183", -1000, 1000, 0f),
    Dummy184("dummy 184", -1000, 1000, 0f),
    Dummy185("dummy 185", -1000, 1000, 0f),
    Dummy186("dummy 186", -1000, 1000, 0f),
    Dummy187("dummy 187", -1000, 1000, 0f),
    Dummy188("dummy 188", -1000, 1000, 0f),
    Dummy189("dummy 189", -1000, 1000, 0f),
    Dummy190("dummy 190", -1000, 1000, 0f),
    Dummy191("dummy 191", -1000, 1000, 0f),
    Dummy192("dummy 192", -1000, 1000, 0f),
    Dummy193("dummy 193", -1000, 1000, 0f),
    Dummy194("dummy 194", -1000, 1000, 0f),
    Dummy195("dummy 195", -1000, 1000, 0f),
    Dummy196("dummy 196", -1000, 1000, 0f),
    Dummy197("dummy 197", -1000, 1000, 0f),
    Dummy198("dummy 198", -1000, 1000, 0f),
    Dummy199("dummy 199", -1000, 1000, 0f);

    /** how the parameter is labelled on the IFW panel */
    private final String displayName;
    /** the lowest value of the knob, 0 for a choice */
    private final float min;
    /** the highest value of the knob, the last ordinal for a choice */
    private final float max;
    /** the value of the IFW initial program */
    private final float defaultValue;
    /** the enum this parameter chooses from, or null when it is a knob */
    private final Class<? extends Labeled> choiceType;

    /** a knob */
    IfwParameter(String displayName, float min, float max, float defaultValue) {
        this.displayName = displayName;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.choiceType = null;
    }

    /** a switch or a selector */
    <E extends Enum<E> & Labeled> IfwParameter(String displayName, E defaultValue) {
        this.displayName = displayName;
        this.min = 0;
        this.max = defaultValue.getDeclaringClass().getEnumConstants().length - 1;
        this.defaultValue = defaultValue.ordinal();
        this.choiceType = defaultValue.getDeclaringClass();
    }

    /** the name IFW writes this parameter under */
    public String id() {
        return name();
    }

    /** */
    public String displayName() {
        return displayName;
    }

    /** */
    public float min() {
        return min;
    }

    /** */
    public float max() {
        return max;
    }

    /** */
    public float defaultValue() {
        return defaultValue;
    }

    /** the enum this parameter chooses from, or null when it is a knob */
    public Class<? extends Labeled> choiceType() {
        return choiceType;
    }

    /** */
    public boolean isChoice() {
        return choiceType != null;
    }

    /** whether the knob only makes sense at whole steps */
    public boolean isInteger() {
        return !isChoice() && (name().endsWith("Semi") || name().equals("Transpose") || name().equals("Filter3Stage"));
    }

    /** */
    public float clamp(float value) {
        return Math.min(max, Math.max(min, value));
    }

    /**
     * Reads the text a tone file holds for this parameter.
     *
     * @return a knob value, or the ordinal of the chosen constant
     * @throws IllegalArgumentException the text is neither a number nor a label this parameter knows
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public float parse(String text) {
        if (choiceType != null) {
            return ((Enum<?>) Labeled.valueOfLabel((Class) choiceType, text)).ordinal();
        }
        return clamp(Float.parseFloat(text.trim()));
    }

    /** Writes the text a tone file holds for this parameter. */
    public String format(float value) {
        if (choiceType != null) {
            Labeled[] constants = choiceType.getEnumConstants();
            return constants[Math.min(constants.length - 1, Math.max(0, Math.round(value)))].label();
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** the parameter written under the given name, or null when the name is unknown */
    public static IfwParameter valueOfId(String id) {
        for (IfwParameter p : values()) {
            if (p.name().equals(id)) {
                return p;
            }
        }
        return null;
    }
}
