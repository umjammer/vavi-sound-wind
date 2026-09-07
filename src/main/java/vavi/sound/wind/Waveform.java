/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;


/**
 * Waveform. what the second slot of an oscillator (the "TRI" knob) generates.
 * <p>
 * the first three choices feed the host input into the slot instead of an oscillator, which
 * lets IFW filter an external signal by the breath controller. the rest name a single cycle
 * wave the plug-in builds once and keeps in its wave cache: two of them are the classic
 * shapes, the other seven are instruments.
 * <p>
 * the order is the plug-in's own, so that a tone file which writes the choice as a plain
 * number still reads back as the wave its author picked.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @version 0.01 2026-09-07 nsano the twelve choices the plug-in really offers <br>
 * @see WaveBank
 */
public enum Waveform implements Labeled {

    ExtInLR("Ext.In L+R", 1, 1),
    ExtInL("Ext.In L", 2, 0),
    ExtInR("Ext.In R", 0, 2),
    TRI("TRI"),
    SIN("SIN"),
    ASax("ASax"),
    Clarinet("Clarinet"),
    D50Saw("D50Saw"),
    Harmonica("Harmonica"),
    Oboe("Oboe"),
    Tb("Tb"),
    Tp("Tp");

    private final String label;
    private final float externalLeft;
    private final float externalRight;

    Waveform(String label) {
        this(label, 0, 0);
    }

    Waveform(String label, float externalLeft, float externalRight) {
        this.label = label;
        this.externalLeft = externalLeft;
        this.externalRight = externalRight;
    }

    @Override
    public String label() {
        return label;
    }

    /** whether this waveform reads the external input instead of a wave */
    public boolean isExternal() {
        return ordinal() < TRI.ordinal();
    }

    /** how much of the host's left channel this waveform lets through */
    public float externalLeft() {
        return externalLeft;
    }

    /** how much of the host's right channel this waveform lets through */
    public float externalRight() {
        return externalRight;
    }

    /**
     * Which wave of {@link WaveBank} this slot reads.
     * <p>
     * the sawtooth is wave 0 and belongs to the first slot, so the second slot starts at 1.
     * an {@code Ext.In} choice still names wave 1, since the plug-in leaves the oscillator
     * running and only silences it.
     */
    public int wave() {
        return isExternal() ? 1 : ordinal() - 2;
    }

    /** how much of the wave the slot mixes, as against the host input */
    public float gain() {
        return isExternal() ? 0 : 1;
    }

    /** */
    public static Waveform valueOfLabel(String label) {
        return Labeled.valueOfLabel(Waveform.class, label);
    }
}
