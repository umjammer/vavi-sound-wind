/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * ModDestination. what one of the eight IFW modulation slots is wired into.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum ModDestination implements Labeled {

    None("None", 0f),
    OscMasterPitch("OSC Master Pitch", 24f),
    OscMasterPwmWidth("OSC Master PWM Width", 1f),
    Osc1Pitch("OSC 1 Pitch", 24f),
    Osc1FM("OSC 1 FM", 1f),
    Osc1SawLevel("OSC 1 SAW Level", 1f),
    Osc1TriLevel("OSC 1 TRI Level", 1f),
    Osc1PwmLevel("OSC 1 PWM Level", 1f),
    Osc1PwmPhase("OSC 1 PWM Phase", 1f),
    Osc2Pitch("OSC 2 Pitch", 24f),
    Osc2FM("OSC 2 FM", 1f),
    Osc2SawLevel("OSC 2 SAW Level", 1f),
    Osc2TriLevel("OSC 2 TRI Level", 1f),
    Osc2PwmLevel("OSC 2 PWM Level", 1f),
    Osc2PwmPhase("OSC 2 PWM Phase", 1f),
    Osc3Pitch("OSC 3 Pitch", 24f),
    Osc3FM("OSC 3 FM", 1f),
    Osc3SawLevel("OSC 3 SAW Level", 1f),
    Osc3TriLevel("OSC 3 TRI Level", 1f),
    Osc3PwmLevel("OSC 3 PWM Level", 1f),
    Osc3PwmPhase("OSC 3 PWM Phase", 1f),
    Osc4Pitch("OSC 4 Pitch", 24f),
    Osc4FM("OSC 4 FM", 1f),
    Osc4SawLevel("OSC 4 SAW Level", 1f),
    Osc4TriLevel("OSC 4 TRI Level", 1f),
    Osc4NoiseLevel("OSC 4 Noise Level", 1f),
    Osc4PwmPhase("OSC 4 PWM Phase", 1f),
    Filter1Frequency("Filter 1 Frequency", 150f),
    Filter1Resonance("Filter 1 Resonance", 1f),
    Filter2Frequency("Filter 2 Frequency", 150f),
    Filter2Resonance("Filter 2 Resonance", 1f),
    Filter3Frequency("Filter 3 Frequency", 1f),
    Filter3Resonance("Filter 3 Resonance", 1f),
    Filter3Mix("Filter 3 Mix", 1f),
    AmpMasterLevel("AMP Master Level", 1f),
    Amp1Level("AMP 1 Level", 1f),
    Amp2Level("AMP 2 Level", 1f),
    Eg1Rate("EG 1 Rate", 1f),
    Eg2Rate("EG 2 Rate", 1f),
    Eg3Rate("EG 3 Rate", 1f),
    Eg4Rate("EG 4 Rate", 1f),
    ExciterFrequency("Exciter Frequency", 1f),
    ExciterMix("Exciter Mix", 1f),
    EnhancerDelay("Enhancer Delay", .05f),
    EnhancerMix("Enhancer Mix", 1f);

    private final String label;
    private final float scale;

    ModDestination(String label, float scale) {
        this.label = label;
        this.scale = scale;
    }

    /**
     * What a whole depth is worth here, in the units of the destination's own knob.
     * <p>
     * a knob runs 0 .. 10 and a depth of 10 sweeps it, so most of them are worth one. a pitch
     * is worth twenty four semitones, a filter cutoff a hundred and fifty of them, and an
     * enhancer delay a twentieth of what its knob holds.
     */
    public float scale() {
        return scale;
    }

    @Override
    public String label() {
        return label;
    }

    /** */
    public static ModDestination valueOfLabel(String label) {
        return Labeled.valueOfLabel(ModDestination.class, label);
    }
}
