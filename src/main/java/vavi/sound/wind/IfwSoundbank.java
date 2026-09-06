/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;

import static java.lang.System.getLogger;


/**
 * IfwSoundbank. a folder of IFW tones as a {@link Soundbank}.
 * <p>
 * IFW has no bank file of its own, and no numbering either: a tone is one {@code .xml}
 * under {@code ~/Documents/IFW/Sounds} and the folders below it are how a player keeps
 * their tones apart. so the numbering is one running count over the lot, in the order a
 * player reads them: the tones sitting loose in a folder first, sorted by name, then the
 * folders in it, sorted by name, each one read the same way. the count is program 0 of
 * bank 0 upwards, running on into the bank above every {@value #BANK_SIZE} tones, so a
 * program change with no bank select reaches the first {@value #BANK_SIZE} of them.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @see IfwProgram
 */
public class IfwSoundbank implements Soundbank {

    private static final Logger logger = getLogger(IfwSoundbank.class.getName());

    /** how many programs a MIDI bank holds */
    private static final int BANK_SIZE = 128;

    /** how many banks a bank select reaches, the 14 bits of CC 0 and CC 32 */
    private static final int BANK_COUNT = 16384;

    /** */
    private final String name;

    /** */
    private final List<Instrument> instruments = new ArrayList<>();

    /** an empty bank holding the IFW initial program alone */
    public IfwSoundbank() {
        this("IFW");
        instruments.add(new IfwInstrument(this, new Patch(0, 0), new IfwProgram()));
    }

    /** */
    private IfwSoundbank(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getVendor() {
        return "vavi";
    }

    @Override
    public String getDescription() {
        return "IFW tones, " + instruments.size() + " program(s)";
    }

    @Override
    public SoundbankResource[] getResources() {
        return new SoundbankResource[0];
    }

    @Override
    public Instrument[] getInstruments() {
        return instruments.toArray(Instrument[]::new);
    }

    @Override
    public Instrument getInstrument(Patch patch) {
        for (Instrument instrument : instruments) {
            if (instrument.getPatch().getBank() == patch.getBank()
                    && instrument.getPatch().getProgram() == patch.getProgram()) {
                return instrument;
            }
        }
        return null;
    }

    /** Reads a single tone as a bank of one program. */
    public static IfwSoundbank read(InputStream in) throws IOException {
        IfwProgram program = IfwProgram.read(in);
        IfwSoundbank soundbank = new IfwSoundbank(program.getName());
        soundbank.instruments.add(new IfwInstrument(soundbank, new Patch(0, 0), program));
        return soundbank;
    }

    /**
     * Reads a tone, or a folder of them.
     * <p>
     * a folder is walked all the way down, and every tone under it is numbered, so nothing
     * is left out however deep a player has filed it away.
     */
    public static IfwSoundbank read(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
                return read(in);
            }
        }

        IfwSoundbank soundbank = new IfwSoundbank(path.getFileName().toString());
        soundbank.load(path);
        if (soundbank.instruments.isEmpty()) {
            throw new IOException("no IFW tone under: " + path);
        }
        return soundbank;
    }

    /** Reads the tones in {@code folder}, and then those of the folders in it. */
    private void load(Path folder) throws IOException {
        for (Path file : list(folder, p -> Files.isRegularFile(p)
                && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml"))) {
            if (instruments.size() >= BANK_COUNT * BANK_SIZE) {
logger.log(Level.INFO, "the numbering is full, the rest is dropped: " + folder);
                return;
            }
            try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
                // the count so far is the number, so a file that turns out not to be a tone takes none
                instruments.add(new IfwInstrument(this, patch(instruments.size()), IfwProgram.read(in)));
            } catch (IOException e) {
                // a folder of tones can hold anything, one unreadable file is not fatal
logger.log(Level.DEBUG, "not an IFW tone, skipped: " + file + ": " + e.getMessage());
            }
        }
        for (Path sub : list(folder, Files::isDirectory)) {
            load(sub);
        }
    }

    /** the patch of the {@code index}th tone, the count running on into the bank above at {@value #BANK_SIZE} */
    private static Patch patch(int index) {
        return new Patch(index / BANK_SIZE, index % BANK_SIZE);
    }

    /** */
    private static List<Path> list(Path folder, Predicate<Path> filter) throws IOException {
        try (Stream<Path> stream = Files.list(folder)) {
            return stream.filter(filter)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
    }

    /**
     * IfwInstrument. one IFW tone.
     */
    public static class IfwInstrument extends Instrument {

        /** */
        private final IfwProgram program;

        /** */
        public IfwInstrument(Soundbank soundbank, Patch patch, IfwProgram program) {
            super(soundbank, patch, program.getName(), IfwProgram.class);
            this.program = program;
        }

        @Override
        public IfwProgram getData() {
            return program;
        }
    }
}
