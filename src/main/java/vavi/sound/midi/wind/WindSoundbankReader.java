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
import java.net.URISyntaxException;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.spi.SoundbankReader;

import vavi.sound.wind.IfwProgram;
import vavi.sound.wind.IfwSoundbank;


/**
 * WindSoundbankReader. reads an IFW tone, or a folder of them, as a {@link Soundbank}.
 * <p>
 * a tone file is the {@code .xml} IFW writes under {@code ~/Documents/IFW/Sounds}, and a
 * folder of those is a whole bank. only {@link WindSynthesizer} can play the result:
 * gervill wants sampled instruments and an IFW tone is a set of knob positions.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @see IfwSoundbank
 */
public class WindSoundbankReader extends SoundbankReader {

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
