/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.wind;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.spi.SoundbankReader;

import vavi.sound.wind.IfwProgram;
import vavi.sound.wind.IfwSoundbank;

import static java.lang.System.getLogger;


/**
 * WindSoundbankReader. reads an IFW tone, or a folder of them, as a {@link Soundbank}.
 * <p>
 * a tone file is the {@code .xml} IFW writes under {@code ~/Documents/IFW/Sounds}, and a
 * folder of those is a whole bank. only {@link WindSynthesizer} can play the result:
 * gervill wants sampled instruments and an IFW tone is a set of knob positions.
 * <p>
 * {@link #getDefaultSoundbank()} is that whole installed folder, which is what
 * {@link WindSynthesizer} starts with.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @see IfwSoundbank
 */
public class WindSoundbankReader extends SoundbankReader {

    private static final Logger logger = getLogger(WindSoundbankReader.class.getName());

    /** where IFW keeps its tones */
    private static final String SOUNDS = System.getProperty("vavi.sound.wind.sounds",
            System.getProperty("user.home") + "/Documents/IFW/Sounds");

    /**
     * Every tone IFW has installed, as one soundbank.
     * <p>
     * this is the whole of {@code ~/Documents/IFW/Sounds}, every tone under it however
     * deep, numbered as one running count from bank 0 program 0 up, so a program change
     * with no bank select walks the installed tones. the system property
     * {@code vavi.sound.wind.sounds} points somewhere else, and when there is nothing
     * there the bank holds the IFW initial program alone.
     * <p>
     * this is what {@link WindSynthesizer} loads as its default soundbank, and it is
     * asked for here rather than through {@link javax.sound.midi.MidiSystem} because
     * that hands a folder to every reader installed, most of which will not have it.
     */
    public static Soundbank getDefaultSoundbank() {
        Path path = Path.of(SOUNDS);
        if (Files.exists(path)) {
            try {
                return IfwSoundbank.read(path);
            } catch (IOException e) {
logger.log(Level.INFO, "no IFW tone under " + path + ": " + e.getMessage());
            }
        }
        return new IfwSoundbank();
    }

    @Override
    public Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException {
        if ("file".equals(url.getProtocol())) {
            try {
                return getSoundbank(new File(url.toURI()));
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        try (InputStream in = new BufferedInputStream(url.openStream())) {
            return getSoundbank(in);
        }
    }

    @Override
    public Soundbank getSoundbank(InputStream stream) throws InvalidMidiDataException, IOException {
        InputStream in = stream.markSupported() ? stream : new BufferedInputStream(stream);
        if (!IfwProgram.isIfw(in)) {
            return null;
        }
        return IfwSoundbank.read(in);
    }

    @Override
    public Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
        try {
            return IfwSoundbank.read(file.toPath());
        } catch (IOException e) {
            // not ours, let the next reader try
            return null;
        }
    }
}
