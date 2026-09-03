[![Release](https://jitpack.io/v/umjammer/vavi-sound-wind.svg)](https://jitpack.io/#umjammer/vavi-sound-wind)
[![Java CI](https://github.com/umjammer/vavi-sound-wind/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound-wind/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound-wind/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-sound-wind/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-25-b07219)

# vavi-sound-wind

a wind controller synthesizer, as a java midi spi

A clone of **IFW - Instrument for Wind Controllers**, the audio unit that plays from a
breath controller rather than from a keyboard. Everything is driven by **MIDI CC 2**:
the tones route breath into their filters and their amps, and without it they stay
silent, exactly as an EWI player expects.

* one monophonic instrument per MIDI channel, sixteen parts in all
* four oscillators, three filters, four envelopes, two LFOs, an eight slot modulation
  matrix, EQ, an exciter and a stereo enhancer
* reads the tone files IFW itself writes, `~/Documents/IFW/Sounds/**/*.xml`, as a
  `javax.sound.midi.Soundbank`

## Install

 * [maven](https://jitpack.io/#umjammer/vavi-sound-wind)

## Usage

Play it as an ordinary MIDI device. The SPI registers a synthesizer called
`Wind MIDI Synthesizer`.

```java
MidiDevice.Info info = Arrays.stream(MidiSystem.getMidiDeviceInfo())
        .filter(i -> "Wind MIDI Synthesizer".equals(i.getName()))
        .findFirst().orElseThrow();
Synthesizer synthesizer = (Synthesizer) MidiSystem.getMidiDevice(info);
synthesizer.open();

MidiChannel channel = synthesizer.getChannels()[0];
channel.programChange(0);
channel.noteOn(60, 100);
channel.controlChange(2, 127); // breath, this is what makes it sound
```

Load a folder of tones as a soundbank. A folder becomes a bank and the tones in it
become its programs, both sorted by name; the tones loose in the top folder are bank 0.

```java
Soundbank soundbank = MidiSystem.getSoundbank(new File(System.getProperty("user.home") + "/Documents/IFW/Sounds"));
synthesizer.loadAllInstruments(soundbank);
```

Without a soundbank the synthesizer looks for the tones IFW has installed, under
`~/Documents/IFW/Sounds`. Point `-Dvavi.sound.wind.sounds=...` somewhere else, or get
the IFW initial program alone when there is nothing there.

Render into your own buffers instead, one voice, no audio device:

```java
IfwEngine engine = new IfwEngine(44100);
engine.setProgram(IfwProgram.read(Files.newInputStream(Path.of("Pan Flute.xml"))));
engine.setBreath(0.8f);
engine.noteOn(72, 100);

float[] left = new float[1024], right = new float[1024];
engine.render(left, right, 0, 1024);
```

### What a controller sends

| message | what it does |
|---|---|
| CC 2 breath | the tone's filters and amps, through their `BREATH` knobs |
| channel pressure | taken as breath too, for controllers that send it that way |
| CC 5, CC 65 portamento | the glide, while the tone's `Glide MIDI Enabled` is on |
| pitch bend | over the tone's own `BEND` range, not the usual two semitones |
| CC 7, CC 11, CC 10 | channel volume, expression and pan, outside the tone |

A keyboard that sends none of that still sounds: until the first breath message arrives,
note velocity stands in for it.

## Notes

IFW ships no specification, so the parameter table, the value ranges and the choice
lists were read back out of the plug-in binary and its tone files, and the sound is
built to match the panel rather than sample-for-sample. Where nothing said what a knob
did, it was made to do the musical thing:

* a modulation slot multiplies its two sources, scales the product by its depth, and adds
  the result to its destination in the units of that destination's own knob, so a depth
  of 10 sweeps a knob across its whole range
* filter 3 is a stack of 2 to 24 all pass sections with feedback, not a cutoff filter
* the instrument waves of `Waveform 3` (`ASax`, `Clarinet`, `Oboe` ...) live in the
  plug-in binary and are not distributable, so each is rebuilt from the harmonic
  amplitudes of the instrument it is named after
* a program is free to run four oscillators into both busses and both amps, which is
  what the CLIP lamp on the IFW panel is for; here everything under -3 dBFS passes
  untouched and the rest bends into a knee that never quite reaches full scale

The tone reader copes with what IFW actually writes: a trailing NUL after the root
element, an unescaped `&` in a tone name, a missing `CurrentProgram` wrapper, and files
from older versions that stop partway through the 200 slot table.

## References

 * IFW - Instrument for Wind Controllers, version 1.0.39

## TODO

 * ~~read the tone files IFW writes~~
 * ~~the synthesis engine~~
 * ~~the midi spi~~
 * the original single cycle instrument waves, rather than harmonic approximations
 * a GUI for the panel
