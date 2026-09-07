/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


/**
 * WaveBank. the single cycle waves the oscillators read, one set per note.
 * <p>
 * IFW is not a phase distortion synthesizer: every slot of every oscillator reads a table of
 * 8192 samples, and the table is built for the note it is played at, holding only the
 * harmonics that fit under half the sample rate. that is where the plug-in's brightness at
 * the bottom of the keyboard and its purity at the top both come from, and it is why it
 * writes a thirty megabyte {@code wavecache.dat} the first time it runs.
 * <p>
 * a note keeps the harmonics that fit under 22050 Hz, which is the plug-in's own figure and
 * not the sample rate's, so a table is the same whatever the audio device is doing:
 * <pre>
 * harmonics(note) = 22050 / 440 * 2^((69 - note) / 12), at least one
 * </pre>
 * consecutive notes that come out at the same count share a table, which is what leaves 104
 * of them rather than 192.
 * <ul>
 * <li>wave 0, the sawtooth of the first slot: {@code sum (-1)^(n+1) sin(2 pi n t) / n}</li>
 * <li>wave 1, the triangle: {@code sum (-1)^((n-1)/2) sin(2 pi n t) / n^2} over odd n</li>
 * <li>wave 2, the sine, which needs no band limiting and so has one table</li>
 * <li>waves 3 to 9, the instruments, each a table of harmonic amplitudes and phases</li>
 * </ul>
 * the instrument tables live inside the plug-in and are not distributable, so they are
 * rebuilt here from the harmonic amplitudes of the instrument each one is named after. where
 * IFW is installed its own wave cache is read instead and the waves are then exactly the
 * ones it plays, which is what {@link #isCached()} reports.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-07 nsano initial version <br>
 * @see Waveform
 */
public final class WaveBank {

    private static final Logger logger = System.getLogger(WaveBank.class.getName());

    /** samples in one cycle */
    public static final int SIZE = 8192;

    /** the highest note IFW builds a table for, three octaves over the top of a keyboard */
    public static final int NOTES = 192;

    /** how many waves there are: a sawtooth, a triangle, a sine and seven instruments */
    public static final int WAVES = 10;

    /** the frequency the harmonics of a table have to fit under */
    private static final float NYQUIST = 22050;

    /** where IFW keeps the waves it has already built */
    private static final String CACHE = System.getProperty("vavi.sound.wind.wavecache",
            System.getProperty("user.home") + "/Library/Application Support/IFW/wavecache.dat");

    /**
     * The harmonic amplitudes each instrument wave is rebuilt from, read off the panel's own
     * waves. the first eleven are the ones that carry the instrument; over them the wave
     * falls away as an ordinary reed or brass instrument does.
     */
    private static final float[][] INSTRUMENTS = {
            // ASax, a formant on the fourth and a long even tail
            {1, .850f, .593f, 1.017f, .416f, .329f, .232f, .221f, .330f, .373f, .345f},
            // Clarinet, the odd harmonics and almost nothing between them
            {1, .020f, .399f, .056f, .524f, .036f, .304f, .068f, .126f, .038f, .034f},
            // D50Saw, a sawtooth with the top taken off it
            {1, .417f, .268f, .196f, .153f, .124f, .102f, .086f, .074f, .063f, .054f},
            // Harmonica, a reed with a dip under its formant
            {1, .203f, .160f, .294f, .566f, .117f, .404f, .074f, .175f, .087f, .071f},
            // Oboe, a weak fundamental under a formant that runs to the sixth
            {1, 1.440f, 1.066f, 1.534f, 1.707f, 1.922f, .687f, .100f, .057f, .145f, .266f},
            // Tb, a trombone, weak at the bottom and even all the way up
            {1, 1.730f, 1.988f, 3.333f, 1.819f, 1.600f, 2.169f, 1.506f, 1.263f, 1.094f, 1.051f},
            // Tp, a trumpet, its formant on the fifth
            {1, 1.317f, 1.690f, 2.389f, 3.025f, 2.201f, 1.835f, 1.479f, 1.250f, 1.173f, 1.012f},
    };

    /** how far past the amplitudes above an instrument wave is carried */
    private static final int INSTRUMENT_HARMONICS = 64;

    /** the one bank every engine shares, since the waves are the same for all of them */
    private static final WaveBank INSTANCE = new WaveBank();

    /** */
    public static WaveBank getInstance() {
        return INSTANCE;
    }

    /** how many harmonics each note keeps */
    private final int[] harmonics = new int[NOTES];

    /** which table each note reads, so that notes of the same count share one */
    private final int[] table = new int[NOTES];

    /** how many tables a wave needs */
    private final int tables;

    /** [wave][table], filled in the first time a wave is asked for */
    private final float[][][] waves = new float[WAVES][][];

    /** the wave cache IFW has written, when there is one */
    private final ByteBuffer cache;

    /** where in {@link #cache} each table starts, in floats */
    private final int[] cacheOffsets;

    /** */
    private WaveBank() {
        int count = 0;
        int last = -1;
        for (int note = 0; note < NOTES; note++) {
            int n = (int) (NYQUIST / 440 / Math.pow(2, (note - 69) / 12d));
            harmonics[note] = Math.max(1, n);
            if (harmonics[note] != last) {
                last = harmonics[note];
                count++;
            }
            table[note] = count - 1;
        }
        this.tables = count;

        ByteBuffer buffer = null;
        int[] offsets = null;
        try {
            Path path = Path.of(CACHE);
            if (Files.isReadable(path)) {
                try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
                    ByteBuffer header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
                    channel.read(header);
                    header.flip();
                    if (header.getInt() == 0x61637677 && header.getInt(12) == WAVES && header.getInt(16) == NOTES) {
                        ByteBuffer index = ByteBuffer.allocate(WAVES * NOTES * 4).order(ByteOrder.LITTLE_ENDIAN);
                        channel.read(index, 24);
                        index.flip();
                        offsets = new int[WAVES * NOTES];
                        for (int i = 0; i < offsets.length; i++) {
                            offsets[i] = index.getInt();
                        }
                        long start = 24L + offsets.length * 4L;
                        buffer = channel.map(FileChannel.MapMode.READ_ONLY, start, channel.size() - start)
                                .order(ByteOrder.LITTLE_ENDIAN);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            logger.log(Level.DEBUG, "no usable wave cache: " + e);
            buffer = null;
            offsets = null;
        }
        this.cache = buffer;
        this.cacheOffsets = offsets;
        if (buffer != null) {
logger.log(Level.DEBUG, "reading the waves IFW itself built: " + CACHE);
        }
    }

    /** whether the waves are the ones IFW has built rather than the ones rebuilt here */
    public boolean isCached() {
        return cache != null;
    }

    /** how many harmonics a note keeps */
    public int harmonics(int note) {
        return harmonics[clamp(note)];
    }

    /**
     * Reads one sample of a wave.
     *
     * @param wave 0 the sawtooth, 1 the triangle, 2 the sine, 3 .. 9 the instruments
     * @param note which band limited table to read, 0 .. 191
     * @param phase 0 .. 1
     * @return -1 .. 1
     */
    public float value(int wave, int note, float phase) {
        float[] t = table(wave, clamp(note));
        float x = phase * SIZE;
        int i = (int) x & SIZE - 1;
        int j = i + 1 & SIZE - 1;
        float f = x - (float) Math.floor(x);
        return t[i] + (t[j] - t[i]) * f;
    }

    /** */
    private static int clamp(int note) {
        return note < 0 ? 0 : Math.min(note, NOTES - 1);
    }

    /** the table a note reads, built the first time its wave is asked for */
    private float[] table(int wave, int note) {
        float[][] w = waves[wave];
        if (w == null) {
            synchronized (this) {
                w = waves[wave];
                if (w == null) {
                    w = cache != null ? read(wave) : build(wave);
                    waves[wave] = w;
                }
            }
        }
        // the sine needs no band limiting, so it has one table for every note
        return w[wave == 2 ? 0 : table[note]];
    }

    /** the tables IFW has already built for one wave */
    private float[][] read(int wave) {
        int count = wave == 2 ? 1 : tables;
        float[][] result = new float[count][];
        for (int note = NOTES - 1, seen = count; note >= 0 && seen > 0; note--) {
            int i = wave == 2 ? 0 : table[note];
            if (result[i] == null) {
                float[] t = new float[SIZE];
                int offset = cacheOffsets[wave * NOTES + note];
                for (int k = 0; k < SIZE; k++) {
                    t[k] = cache.getFloat((offset + k) * 4);
                }
                result[i] = t;
                seen--;
            }
        }
        return result;
    }

    /** every table of one wave, built as the plug-in builds them */
    private float[][] build(int wave) {
        int count = wave == 2 ? 1 : tables;
        float[][] result = new float[count][];
        float peak = 0;
        for (int note = NOTES - 1, seen = count; note >= 0 && seen > 0; note--) {
            int i = wave == 2 ? 0 : table[note];
            if (result[i] == null) {
                float[] t = cycle(wave, wave == 2 ? 1 : harmonics[note]);
                result[i] = t;
                for (float v : t) {
                    peak = Math.max(peak, Math.abs(v));
                }
                seen--;
            }
        }
        // IFW scales a whole wave by its loudest table, which leaves the others a shade under
        if (peak > 0) {
            for (float[] t : result) {
                for (int k = 0; k < SIZE; k++) {
                    t[k] /= peak;
                }
            }
        }
        return result;
    }

    /** one cycle of a wave, with everything over {@code harmonics} left out */
    private static float[] cycle(int wave, int harmonics) {
        float[] sine = new float[SIZE];
        float[] cosine = new float[SIZE];
        switch (wave) {
            case 0 -> {
                for (int n = 1; n <= harmonics; n++) {
                    sine[n] = (n % 2 == 1 ? 1f : -1f) / n;
                }
            }
            case 1 -> {
                for (int n = 1; n <= harmonics; n += 2) {
                    sine[n] = ((n - 1) / 2 % 2 == 0 ? 1f : -1f) / (n * (float) n);
                }
            }
            case 2 -> sine[1] = 1;
            default -> {
                float[] amplitudes = INSTRUMENTS[wave - 3];
                for (int n = 1; n <= Math.min(harmonics, INSTRUMENT_HARMONICS); n++) {
                    float a = n <= amplitudes.length
                            ? amplitudes[n - 1]
                            // past what the instrument names, an ordinary reed roll off
                            : amplitudes[amplitudes.length - 1] * amplitudes.length / (float) n;
                    // a wave of an instrument is not a pulse: its harmonics do not line up
                    double phase = n * (n + 1) * Math.PI / 7;
                    sine[n] = (float) (a * Math.cos(phase));
                    cosine[n] = (float) (a * Math.sin(phase));
                }
            }
        }
        return inverse(sine, cosine);
    }

    /**
     * Turns harmonic amplitudes into one cycle.
     * <p>
     * adding up the harmonics one at a time is what the plug-in does, and it is why the
     * plug-in writes a cache rather than doing it twice; a transform does the same sum in a
     * thousandth of the time.
     *
     * @param sine the amplitude of {@code sin(2 pi n t)} for each n
     * @param cosine the amplitude of {@code cos(2 pi n t)} for each n
     */
    private static float[] inverse(float[] sine, float[] cosine) {
        double[] re = new double[SIZE];
        double[] im = new double[SIZE];
        for (int n = 1; n < SIZE / 2; n++) {
            if (sine[n] == 0 && cosine[n] == 0) {
                continue;
            }
            re[n] = cosine[n] / 2d;
            im[n] = -sine[n] / 2d;
            re[SIZE - n] = cosine[n] / 2d;
            im[SIZE - n] = sine[n] / 2d;
        }
        fft(re, im);
        float[] result = new float[SIZE];
        for (int i = 0; i < SIZE; i++) {
            result[i] = (float) re[i];
        }
        return result;
    }

    /** an in place radix 2 transform, the sign of the exponent being the inverse one */
    private static void fft(double[] re, double[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) {
                j ^= bit;
            }
            j ^= bit;
            if (i < j) {
                double t = re[i];
                re[i] = re[j];
                re[j] = t;
                t = im[i];
                im[i] = im[j];
                im[j] = t;
            }
        }
        for (int length = 2; length <= n; length <<= 1) {
            double angle = 2 * Math.PI / length;
            double wr = Math.cos(angle);
            double wi = Math.sin(angle);
            for (int i = 0; i < n; i += length) {
                double cr = 1;
                double ci = 0;
                for (int j = 0; j < length / 2; j++) {
                    int a = i + j;
                    int b = a + length / 2;
                    double xr = re[b] * cr - im[b] * ci;
                    double xi = re[b] * ci + im[b] * cr;
                    re[b] = re[a] - xr;
                    im[b] = im[a] - xi;
                    re[a] += xr;
                    im[a] += xi;
                    double nr = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = nr;
                }
            }
        }
    }

    /** the frequency of a note, which is also what a cutoff knob is read against */
    public static float frequency(float note) {
        return (float) (440 * Math.pow(2, (note - 69) / 12d));
    }

    /**
     * What a part of a semitone multiplies a frequency by.
     * <p>
     * a semitone is cut into a hundred and twenty eight, which is what the plug-in does rather
     * than raise two to a power for every oscillator of every sample.
     *
     * @param fraction 0 .. 1
     */
    public static float fine(float fraction) {
        int i = (int) (fraction * FINE.length);
        return FINE[i < 0 ? 0 : Math.min(i, FINE.length - 1)];
    }

    /** */
    private static final float[] FINE = new float[128];

    static {
        for (int i = 0; i < FINE.length; i++) {
            FINE[i] = (float) Math.pow(2, i / (12d * FINE.length));
        }
    }
}
