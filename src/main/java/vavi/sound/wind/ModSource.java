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

    None("None", 1f, 0f),
    Breath("Breath (+)", 1f, 0f),
    BreathBipoler("Breath (+-)", 2f, -1f),
    PitchBend("Pitch Bend (+-)", 1f, 0f),
    Glide("Glide (+)", 1f, 0f),
    Note("Note (+)", 0.0078125f, 0f),
    NoteBipolar("Note (+-)", 0.015625f, -1f),
    Velocity("Velocity (+)", 1f, 0f),
    VelocityBipoler("Velocity (+-)", 2f, -1f),
    Gate("Gate (+)", 1f, 0f),
    Eg1("EG 1 (+)", 1f, 0f),
    Eg1Bipoler("EG 1 (+-)", 2f, -1f),
    Eg2("EG 2 (+)", 1f, 0f),
    Eg2Bipoler("EG 2 (+-)", 2f, -1f),
    Eg3("EG 3 (+)", 1f, 0f),
    Eg3Bipoler("EG 3 (+-)", 2f, -1f),
    Eg4("EG 4 (+)", 1f, 0f),
    Eg4Bipoler("EG 4 (+-)", 2f, -1f),
    Lfo1("LFO 1 (+)", 1f, 0f),
    Lfo1Bipoler("LFO 1 (+-)", 2f, -1f),
    Lfo2("LFO 2 (+)", 1f, 0f),
    Lfo2Bipoler("LFO 2 (+-)", 2f, -1f),
    Osc1("OSC 1 (+)", 0.5f, 0.5f),
    Osc1Bipoler("OSC 1 (+-)", 1f, 0f),
    Osc1Saw("OSC 1 SAW (+)", 0.5f, 0.5f),
    Osc1SawBipoler("OSC 1 SAW (+-)", 1f, 0f),
    Osc1Tri("OSC 1 TRI (+)", 0.5f, 0.5f),
    Osc1TriBipoler("OSC 1 TRI (+-)", 1f, 0f),
    Osc1Pwm("OSC 1 PWM (+)", 0.5f, 0.5f),
    Osc1PwmBipoler("OSC 1 PWM (+-)", 1f, 0f),
    Osc2("OSC 2 (+)", 0.5f, 0.5f),
    Osc2Bipoler("OSC 2 (+-)", 1f, 0f),
    Osc2Saw("OSC 2 SAW (+)", 0.5f, 0.5f),
    Osc2SawBipoler("OSC 2 SAW (+-)", 1f, 0f),
    Osc2Tri("OSC 2 TRI (+)", 0.5f, 0.5f),
    Osc2TriBipoler("OSC 2 TRI (+-)", 1f, 0f),
    Osc2Pwm("OSC 2 PWM (+)", 0.5f, 0.5f),
    Osc2PwmBipoler("OSC 2 PWM (+-)", 1f, 0f),
    Osc3("OSC 3 (+)", 0.5f, 0.5f),
    Osc3Bipoler("OSC 3 (+-)", 1f, 0f),
    Osc3Saw("OSC 3 SAW (+)", 0.5f, 0.5f),
    Osc3SawBipoler("OSC 3 SAW (+-)", 1f, 0f),
    Osc3Tri("OSC 3 TRI (+)", 0.5f, 0.5f),
    Osc3TriBipoler("OSC 3 TRI (+-)", 1f, 0f),
    Osc3Pwm("OSC 3 PWM (+)", 0.5f, 0.5f),
    Osc3PwmBipoler("OSC 3 PWM (+-)", 1f, 0f),
    Osc4("OSC 4 (+)", 0.5f, 0.5f),
    Osc4Bipoler("OSC 4 (+-)", 1f, 0f),
    Osc4Saw("OSC 4 SAW (+)", 0.5f, 0.5f),
    Osc4SawBipoler("OSC 4 SAW (+-)", 1f, 0f),
    Osc4Tri("OSC 4 TRI (+)", 0.5f, 0.5f),
    Osc4TriBipoler("OSC 4 TRI (+-)", 1f, 0f),
    Osc4Noise("OSC 4 Noise (+)", 0.5f, 0.5f),
    Osc4NoiseBipoler("OSC 4 Noise (+-)", 1f, 0f);

    private final String label;
    private final float scale;
    private final float offset;

    ModSource(String label, float scale, float offset) {
        this.label = label;
        this.scale = scale;
        this.offset = offset;
    }

    /** what the raw source is multiplied by before it is used */
    public float scale() {
        return scale;
    }

    /** what is added to it after that, which is what turns a positive source over */
    public float offset() {
        return offset;
    }

    /** how far the source has to be read: 0 for a knob, 1 for something read every sample */
    public boolean isAudioRate() {
        return ordinal() >= Osc1.ordinal();
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
