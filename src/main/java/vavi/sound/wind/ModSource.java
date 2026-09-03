/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * ModSource. what drives one of the eight IFW modulation slots.
 * <p>
 * a name ending in {@code (+)} is unipolar and runs 0 .. 1, one ending in {@code (+-)}
 * is bipolar and runs -1 .. 1. the oscillator sources run at audio rate, which is how
 * IFW patches build FM and ring modulation out of the modulation matrix.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 */
public enum ModSource implements Labeled {

    None("None"),
    Breath("Breath (+)"),
    BreathBipoler("Breath (+-)"),
    PitchBend("Pitch Bend (+-)"),
    Glide("Glide (+)"),
    Note("Note (+)"),
    NoteBipolar("Note (+-)"),
    Velocity("Velocity (+)"),
    VelocityBipoler("Velocity (+-)"),
    Gate("Gate (+)"),
    Eg1("EG 1 (+)"),
    Eg1Bipoler("EG 1 (+-)"),
    Eg2("EG 2 (+)"),
    Eg2Bipoler("EG 2 (+-)"),
    Eg3("EG 3 (+)"),
    Eg3Bipoler("EG 3 (+-)"),
    Eg4("EG 4 (+)"),
    Eg4Bipoler("EG 4 (+-)"),
    Lfo1("LFO 1 (+)"),
    Lfo1Bipoler("LFO 1 (+-)"),
    Lfo2("LFO 2 (+)"),
    Lfo2Bipoler("LFO 2 (+-)"),
    Osc1("OSC 1 (+)"),
    Osc1Bipoler("OSC 1 (+-)"),
    Osc1Saw("OSC 1 SAW (+)"),
    Osc1SawBipoler("OSC 1 SAW (+-)"),
    Osc1Tri("OSC 1 TRI (+)"),
    Osc1TriBipoler("OSC 1 TRI (+-)"),
    Osc1Pwm("OSC 1 PWM (+)"),
    Osc1PwmBipoler("OSC 1 PWM (+-)"),
    Osc2("OSC 2 (+)"),
    Osc2Bipoler("OSC 2 (+-)"),
    Osc2Saw("OSC 2 SAW (+)"),
    Osc2SawBipoler("OSC 2 SAW (+-)"),
    Osc2Tri("OSC 2 TRI (+)"),
    Osc2TriBipoler("OSC 2 TRI (+-)"),
    Osc2Pwm("OSC 2 PWM (+)"),
    Osc2PwmBipoler("OSC 2 PWM (+-)"),
    Osc3("OSC 3 (+)"),
    Osc3Bipoler("OSC 3 (+-)"),
    Osc3Saw("OSC 3 SAW (+)"),
    Osc3SawBipoler("OSC 3 SAW (+-)"),
    Osc3Tri("OSC 3 TRI (+)"),
    Osc3TriBipoler("OSC 3 TRI (+-)"),
    Osc3Pwm("OSC 3 PWM (+)"),
    Osc3PwmBipoler("OSC 3 PWM (+-)"),
    Osc4("OSC 4 (+)"),
    Osc4Bipoler("OSC 4 (+-)"),
    Osc4Saw("OSC 4 SAW (+)"),
    Osc4SawBipoler("OSC 4 SAW (+-)"),
    Osc4Tri("OSC 4 TRI (+)"),
    Osc4TriBipoler("OSC 4 TRI (+-)"),
    Osc4Noise("OSC 4 Noise (+)"),
    Osc4NoiseBipoler("OSC 4 Noise (+-)");

    private final String label;

    ModSource(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    /** whether this source swings both ways, -1 .. 1 instead of 0 .. 1 */
    public boolean isBipolar() {
        return label.endsWith("(+-)");
    }

    /** */
    public static ModSource valueOfLabel(String label) {
        return Labeled.valueOfLabel(ModSource.class, label);
    }
}
