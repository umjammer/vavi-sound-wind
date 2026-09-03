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
 * IFW has no bank file of its own: a tone is one {@code .xml} under
 * {@code ~/Documents/IFW/Sounds} and the folders below it are how a player keeps their
 * tones apart. so a folder becomes a bank, sorted by name, and the tones in it become
 * the programs of that bank, also sorted by name. the tones sitting loose in the top
 * folder are bank 0, which is what a program change with no bank select reaches.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @see IfwProgram
 */
public class IfwSoundbank implements Soundbank {

    private static final Logger logger = getLogger(IfwSoundbank.class.getName());

    /** how many programs a MIDI bank holds */
    private static final int BANK_SIZE = 128;

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
     * a folder is walked one level deep: its own tones become bank 0 and each subfolder
     * becomes a bank of its own.
     */
    public static IfwSoundbank read(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
                return read(in);
            }
        }

        IfwSoundbank soundbank = new IfwSoundbank(path.getFileName().toString());
        soundbank.load(path, 0);
        int bank = 1;
        for (Path folder : list(path, Files::isDirectory)) {
            soundbank.load(folder, bank++);
        }
        if (soundbank.instruments.isEmpty()) {
            throw new IOException("no IFW tone under: " + path);
        }
        return soundbank;
    }

    /** Reads the tones directly in {@code folder} into one bank. */
    private void load(Path folder, int bank) throws IOException {
        int program = 0;
        for (Path file : list(folder, p -> Files.isRegularFile(p)
                && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml"))) {
            if (program >= BANK_SIZE) {
logger.log(Level.INFO, "more than " + BANK_SIZE + " tones, the rest of the bank is dropped: " + folder);
                break;
            }
            try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
                instruments.add(new IfwInstrument(this, new Patch(bank, program++), IfwProgram.read(in)));
            } catch (IOException e) {
                // a folder of tones can hold anything, one unreadable file is not fatal
logger.log(Level.DEBUG, "not an IFW tone, skipped: " + file + ": " + e.getMessage());
            }
        }
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
