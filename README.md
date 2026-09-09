[![Release](https://jitpack.io/v/umjammer/vavi-sound-wind.svg)](https://jitpack.io/#umjammer/vavi-sound-wind)
[![Java CI](https://github.com/umjammer/vavi-sound-wind/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound-wind/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound-wind/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-sound-wind/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-25-b07219)

# vavi-sound-wind

<img alt="logo" src="src/test/resources/duke_ewi.png" width="160" />

a wind controller synthesizer, as a java midi spi

A clone of **IFW - Instrument for Wind Controllers**, the audio unit that plays from a
breath controller rather than from a keyboard. Everything is driven by **MIDI CC 2**:
the tones route breath into their filters and their amps, and without it they stay
silent, exactly as an EWI player expects.

* one monophonic instrument per MIDI channel, sixteen parts in all
* four wavetable oscillators, a saturating transistor ladder, a 24 stage phaser, four
  envelopes, two LFOs, an eight slot modulation matrix, EQ, an exciter and a stereo
  enhancer, all of it run at four times the sample rate
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

Load a tone, or a folder of them, as a soundbank. Every tone under the folder is numbered
as one running count, in the order a player reads them: a folder's own tones first, sorted
by name, then the folders in it, sorted by name, each read the same way. The count starts
at bank 0 program 0 and runs on into the bank above every 128 tones, so a program change
on its own walks the tones and no bank select is needed below 128. Loading a soundbank
puts every channel on the tone at its own bank and program, so it sounds straight away.

```java
Soundbank soundbank = MidiSystem.getSoundbank(new File("Pan Flute.xml"));
synthesizer.loadAllInstruments(soundbank);
```

Left alone the synthesizer already holds every tone IFW has installed, the whole of
`~/Documents/IFW/Sounds` read by `WindSoundbankReader.getDefaultSoundbank()`, and every
channel starts on the first of them. A program change is all it takes to walk them.
Point `-Dvavi.sound.wind.sounds=...` somewhere else, or get the IFW initial program alone
when there is nothing there.

```java
Soundbank installed = WindSoundbankReader.getDefaultSoundbank();
```

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

### Playing feel

An IFW tone file holds no breath curve. A tone shapes breath only with its `BREATH` knobs,
its breath sub mod and the modulation matrix, so the curve is the tone designer's business,
and the fifty tones IFW installs answer the controller very differently. What is missing is
the player's own end of it, the part a wind controller keeps in its setup rather than in the
patch, so it lives outside the tone here:

```java
synthesizer.setBreathResponse(new BreathResponse(0, 1, 18)); // low, high, depth in dB
```

`low` and `high` say where the sensor really rests and really tops out, so that the whole of
the travel is used. `depth` says how many decibels that travel is spread over. It matters
because a level that follows breath straight, which is what IFW does, has all of its decibels
crammed into the bottom: half breath is 6 dB down and the top half of the sensor does almost
nothing, which is what makes a wind synthesizer feel like a switch. Spending them evenly is
the rule a mixing desk fader and MIDI volume already follow, and the one the ear reads as
even, since loudness goes with the logarithm of the level.

The default is 18 dB, which over the installed tones moves the middle one from answering half
breath 10 dB under its loudest to answering it 16 dB under, and its whole travel from 32 dB
to 41 dB, about what an acoustic wind instrument gives from its softest note to its loudest.
`BreathResponse.LINEAR`, or `-Dvavi.sound.wind.breath=0,1,0`, puts back exactly what IFW does.

The curve reaches the amps only, so the tone's own filters keep answering the controller
itself and stay bright across the whole of the travel.

The other half of the feel is latency, which is why the synthesizer asks the audio device for
a small buffer rather than taking the half second it offers: four blocks of 256 frames, 23 ms.
`-Dvavi.sound.wind.block` and `-Dvavi.sound.wind.buffer` move it, up if the machine cannot
keep up and the sound breaks up, though one block costs 1.4 ms to fill with all sixteen parts
sounding.

The master volume is the universal realtime device control the MIDI specification already
carries, `F0 7F <device> 04 01 <lsb> <msb> F7`, and there is no method beside it:

```java
MidiUtil.volume(synthesizer.getReceiver(), .5f); // 14 bits over silence .. unity
```

The mix of sixteen parts runs into a soft knee at -1.4 dBFS rather than squaring off, and
the knee belongs to the mix rather than to a tone: a tone arrives at exactly the level IFW
gives it, which for one oscillator is a quarter of full scale.
`-Dvavi.sound.wind.volume` says where the master stands before the first sysex arrives,
since a sequencer will usually never send one.

Spreading breath out deliberately leaves everything under full breath quieter than IFW has
it, and a player who never reaches the top of the sensor loses that much again. Calibrating
`high` to where the sensor really tops out is what gets that back, and a smaller `depth`
trades some of the feel for the rest of it.

## Notes

IFW ships no specification, so the algorithm was read back out of the plug-in binary
itself: the parameter table, the choice lists, the modulation scales and the DSP are the
ones the 1.0.39 build runs, not a reconstruction from the panel.

* **the oscillators are wavetables, not generators.** every slot reads one cycle of 8192
  samples, and a table is built per note holding only the harmonics that fit under
  22050 Hz, `22050 / 440 * 2^((69 - note) / 12)` of them. notes that come out at the same
  count share a table, which leaves 104 of them rather than 192. that is what the thirty
  megabyte `wavecache.dat` in `~/Library/Application Support/IFW` holds, and where IFW is
  installed it is read straight off it, so the waves are the ones the plug-in plays.
  `-Dvavi.sound.wind.wavecache` points that elsewhere, and without it the sawtooth, the
  triangle and the sine are rebuilt exactly and the seven instruments approximately
* the `SAW` slot is that sawtooth, the `PWM` slot is the same sawtooth less a copy of
  itself shifted by the `WIDTH` knob, and the `TRI` slot is whatever `Waveform 2` names.
  **`Waveform 2` has twelve choices**, not five: the three `Ext.In` inputs, `TRI`, `SIN`
  and then `ASax`, `Clarinet`, `D50Saw`, `Harmonica`, `Oboe`, `Tb` and `Tp`.
  `Waveform 3` is read, clamped to a switch, and then never used at all
* **filters 1 and 2 are one transistor ladder** of four one pole sections, each saturating
  what it passes on, with the last fed back to the first. the four modes are taps off it
  rather than separate filters, and only the 24 dB one gets the whole ladder's resonance
  and drive. a cutoff counts in semitones over a table that starts at 2.04 Hz, which is
  how the knob, the key tracking, the breath and the matrix are added before any of it is
  worked out in hertz
* **everything down to the amplifiers runs at four times the sample rate** and comes back
  down through a 20 kHz section of its own, which is what lets the ladder saturate without
  folding. `-Dvavi.sound.wind.oversample` takes that to 2 or 1 on a machine that cannot
  keep up; one voice costs 0.31 ms per 256 frames at the full rate
* an envelope segment counts a phase from one down to zero at the rate its knob asks for,
  so a knob is a duration: an attack runs from under three milliseconds to nine seconds, a
  decay or a release from nine milliseconds to seven. the attack is a straight line and
  everything under it is bent three quarters of the way towards a fourth power
* a modulation slot multiplies its two sources, scales the product by its depth and adds
  the result to its destination, a depth of 10 being a whole knob, 24 semitones of pitch,
  150 semitones of cutoff or a twentieth of an enhancer delay
* filter 3 is a stack of 24 all pass sections with feedback and the `STAGE` knob choosing
  where the output is taken from, crossfaded against the dry signal rather than added
* an amplifier is `breath x its BREATH knob x its LEVEL knob`, held inside unity, so a
  `BREATH` of 0 is silent rather than wide open. `BREATH` itself means the note gate, and
  `BRxEGn` an envelope
* **each oscillator is worth a quarter of a bus**, whether or not the other three are
  used, so a plain one oscillator tone peaks a quarter of the way up and four in step fill
  the bus exactly. a tone that runs all four into both busses and both amplifiers goes
  past full scale, which is what the CLIP lamp on the IFW panel is for; there is no
  limiter inside a tone, and the one at -1.4 dBFS belongs to the synthesizer that mixes
  the sixteen parts

The tone reader copes with what IFW actually writes: a trailing NUL after the root
element, an unescaped `&` in a tone name, a missing `CurrentProgram` wrapper, and files
from older versions that stop partway through the 200 slot table.

## References

 * IFW - Instrument for Wind Controllers, version 1.0.39

### Lesson

 - when idea uses graalvm, junit test runner becomes different from when openjdk.
   so volume mixer controller like `BackGroundMusic.app` setting for `JUnitStarter` doesn't work. 

## TODO

 * ~~read the tone files IFW writes~~
 * ~~the synthesis engine~~
 * ~~the midi spi~~
 * ~~the breath curve and the latency, so that it plays like an instrument~~
 * ~~the true algorithm, read back out of the plug-in binary~~
 * ~~the original single cycle instrument waves, where IFW itself is installed~~
 * the seven instrument waves on a machine that has no IFW on it, rather than
   approximations of them
 * a GUI for the panel
 * ~~connect midi output to au~~ ... [vavi-sound-sandbox](https://github.com/umjammer/vavi-sound-sandbox)

---

<sub>image designed by @umjammer, drawn by nano banana</sub>
