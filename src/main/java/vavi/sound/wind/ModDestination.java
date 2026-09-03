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

    None("None"),
    OscMasterPitch("OSC Master Pitch"),
    OscMasterPwmWidth("OSC Master PWM Width"),
    Osc1Pitch("OSC 1 Pitch"),
    Osc1FM("OSC 1 FM"),
    Osc1SawLevel("OSC 1 SAW Level"),
    Osc1TriLevel("OSC 1 TRI Level"),
    Osc1PwmLevel("OSC 1 PWM Level"),
    Osc1PwmPhase("OSC 1 PWM Phase"),
    Osc2Pitch("OSC 2 Pitch"),
    Osc2FM("OSC 2 FM"),
    Osc2SawLevel("OSC 2 SAW Level"),
    Osc2TriLevel("OSC 2 TRI Level"),
    Osc2PwmLevel("OSC 2 PWM Level"),
    Osc2PwmPhase("OSC 2 PWM Phase"),
    Osc3Pitch("OSC 3 Pitch"),
    Osc3FM("OSC 3 FM"),
    Osc3SawLevel("OSC 3 SAW Level"),
    Osc3TriLevel("OSC 3 TRI Level"),
    Osc3PwmLevel("OSC 3 PWM Level"),
    Osc3PwmPhase("OSC 3 PWM Phase"),
    Osc4Pitch("OSC 4 Pitch"),
    Osc4FM("OSC 4 FM"),
    Osc4SawLevel("OSC 4 SAW Level"),
    Osc4TriLevel("OSC 4 TRI Level"),
    Osc4NoiseLevel("OSC 4 Noise Level"),
    Osc4PwmPhase("OSC 4 PWM Phase"),
    Filter1Frequency("Filter 1 Frequency"),
    Filter1Resonance("Filter 1 Resonance"),
    Filter2Frequency("Filter 2 Frequency"),
    Filter2Resonance("Filter 2 Resonance"),
    Filter3Frequency("Filter 3 Frequency"),
    Filter3Resonance("Filter 3 Resonance"),
    Filter3Mix("Filter 3 Mix"),
    AmpMasterLevel("AMP Master Level"),
    Amp1Level("AMP 1 Level"),
    Amp2Level("AMP 2 Level"),
    Eg1Rate("EG 1 Rate"),
    Eg2Rate("EG 2 Rate"),
    Eg3Rate("EG 3 Rate"),
    Eg4Rate("EG 4 Rate"),
    ExciterFrequency("Exciter Frequency"),
    ExciterMix("Exciter Mix"),
    EnhancerDelay("Enhancer Delay"),
    EnhancerMix("Enhancer Mix");

    private final String label;

    ModDestination(String label) {
        this.label = label;
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
