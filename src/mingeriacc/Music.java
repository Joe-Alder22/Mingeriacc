package mingeriacc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Music sequencer. Songs are written in the code as chord progressions,
 * melodies and patterns; accompaniment (arpeggios, pads, bass) follows the
 * chords. Songs cross-fade into each other, and small random variations keep
 * repeats from sounding identical.
 */
final class Music {
    static final int NONE = 0, DAY = 1, NIGHT = 2, CAVE = 3, TITLE = 4, JUNGLE = 5, CORRUPTION = 6, UNDERWORLD = 7,
            SNOW = 8, BOSS = 9, BLOOD_MOON = 10;
    static final int SONGS = 11;

    // track kinds
    private static final int MELODY = 0, ARP = 1, PAD = 2, BASS = 3, DRUM = 4, SPARKLE = 5;

    private static final class Note {
        final int start, len, midi;
        Note(int start, int len, int midi) { this.start = start; this.len = len; this.midi = midi; }
    }

    private static final class Chord {
        int root, bass = -1;
        int[] iv;
        String name;
    }

    private static final class Track {
        int kind, inst;
        double gain = 0.5, pan, rev = 0.3, echo;
        int octave = 60;
        String pattern = "";
        String mask = "";          // per bar: X = play, . = rest, ? = maybe (decided per loop)
        List<Note> notes = new ArrayList<>();
        double chance = 0.2;       // sparkles
        int stepDiv = 2;           // pattern steps per beat
    }

    private static final class Song {
        int bpm;
        Chord[] chords;
        List<Track> tracks = new ArrayList<>();
        int echoTime = Synth.RATE / 3;
        double reverb = 0.8;
        double volume = 1;         // evens out the loudness of the songs
    }

    private final Song[] songs = new Song[SONGS];
    private final Synth.Reverb reverb = new Synth.Reverb();
    private final Synth.Echo echo = new Synth.Echo();
    private SongPlayer cur, old;
    private int wantId = NONE;
    private final Random rnd = new Random();

    Music() {
        songs[DAY] = day();
        songs[NIGHT] = night();
        songs[CAVE] = cave();
        songs[TITLE] = title();
        songs[JUNGLE] = jungle();
        songs[CORRUPTION] = corruption();
        songs[UNDERWORLD] = underworld();
        songs[SNOW] = snow();
        songs[BOSS] = boss();
        songs[BLOOD_MOON] = bloodMoon();
    }

    /** Snow: a music box, harp and flute over soft pads. */
    private static Song snow() {
        Song s = new Song();
        s.bpm = 72;
        s.chords = chords("Amaj7 Amaj7 F#m7 E "
                + "A E/G# F#m7 Dmaj7 A/C# Bm7 Esus4 E "
                + "Dmaj7 E C#m7 F#m7 Bm7 E Amaj7 F#m7 "
                + "Dmaj7 E Asus2 A");
        Track box = track(s, ARP, Synth.BELL, 0.16, 0.25, 0.6, all(24));
        box.octave = 64;
        box.pattern = "4 2 3 1 4 2 3 5";
        Track harp = track(s, ARP, Synth.HARP, 0.24, -0.25, 0.5, "...." + "XXXXXXXX" + "XXXXXXXX" + "XXXX");
        harp.octave = 45;
        harp.pattern = "0 2 4 2 3 2 4 2";
        Track pad = track(s, PAD, Synth.PAD, 0.14, -0.1, 0.6, all(24));
        pad.octave = 55;
        Track bass = track(s, BASS, Synth.BASS, 0.26, 0, 0.2, "...." + "XXXXXXXX" + "XXXXXXXX" + "XXX.");
        bass.octave = 33;
        bass.pattern = "R.......R.......";
        Track flute = track(s, MELODY, Synth.FLUTE, 0.27, 0.15, 0.5, "...." + "XXXXXXXX" + "........" + "....");
        melody(flute, 4, "E5:6 C#5:2 A4:8 | B4:4 E5:4 G#5:8 | A5:6 F#5:2 C#5:8 | D5:4 F#5:4 A5:8 | "
                + "E5:6 A5:2 C#6:8 | B5:4 A5:4 F#5:8 | E5:4 F#5:4 A5:4 B5:4 | G#5:12 r:4");
        Track piano = track(s, MELODY, Synth.EPIANO, 0.3, -0.1, 0.55, "............" + "XXXXXXXX" + "....");
        melody(piano, 12, "F#5:4 A5:4 C#6:8 | B5:4 G#5:4 E5:8 | E5:4 G#5:4 B5:6 C#6:2 | A5:6 F#5:2 C#5:8 | "
                + "D5:4 F#5:4 B5:8 | G#5:6 B5:2 E6:8 | C#6:4 E6:4 G#5:8 | A5:12 r:4");
        Track bells = track(s, MELODY, Synth.BELL, 0.2, 0.2, 0.7, all(24));
        melody(bells, 20, "A6:8 F#6:8 | G#6:8 E6:8 | E6:16 | C#6:12 r:4");
        Track strings = track(s, PAD, Synth.STRINGS, 0.1, 0.1, 0.7, "............" + "XXXXXXXX" + "XXXX");
        strings.octave = 64;
        Track shaker = track(s, DRUM, 0, 0.14, 0.3, 0.4, "...." + "....XXXX" + "XXXXXXXX" + "....");
        shaker.pattern = "....h.......h...";
        Track spark = track(s, SPARKLE, Synth.BELL, 0.1, 0, 0.85, all(24));
        spark.octave = 88;
        spark.chance = 0.12;
        spark.echo = 0.4;
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.75);
        s.reverb = 0.9;
        s.volume = 0.75;
        return s;
    }

    /** Boss fights: a driving bass and string ostinato, drums and a choir. */
    private static Song boss() {
        Song s = new Song();
        s.bpm = 128;
        s.chords = chords("Dm Dm Bb C Dm Dm Gm A Dm Bb F C Gm Bb A A");
        Track bass = track(s, BASS, Synth.PLUCK_BASS, 0.42, 0, 0.15, all(16));
        bass.octave = 26;
        bass.pattern = "R.R.R.R.R.R.R.RO";
        Track ost = track(s, ARP, Synth.PLUCK, 0.22, 0.25, 0.2, all(16));
        ost.octave = 50;
        ost.stepDiv = 4;
        ost.pattern = "0 1 2 1";
        Track choir = track(s, PAD, Synth.CHOIR, 0.2, -0.15, 0.55, all(16));
        choir.octave = 50;
        Track strings = track(s, MELODY, Synth.STRINGS, 0.34, -0.1, 0.4, "...." + "XXXXXXXX" + "XXXX");
        melody(strings, 4, "D5:4 F5:4 A5:6 G5:2 | F5:4 E5:4 D5:8 | G5:4 Bb5:4 D6:6 C6:2 | C#6:8 A5:8 | "
                + "A5:4 D6:4 F6:6 E6:2 | D6:4 Bb5:4 F5:8 | A5:4 C6:4 F6:8 | E6:6 D6:2 C6:8 | "
                + "Bb5:4 D6:4 G6:8 | F6:6 D6:2 Bb5:8 | A5:4 C#6:4 E6:8 | A5:16");
        Track bells = track(s, MELODY, Synth.BELL, 0.22, 0.2, 0.5, all(16));
        melody(bells, 0, "D6:8 A5:8 | D6:8 F6:8 | Bb5:8 D6:8 | C6:16");
        Track tom = track(s, DRUM, 0, 0.42, -0.25, 0.3, all(16));
        tom.pattern = "t.t.t..tt.t.t..t";
        Track boom = track(s, DRUM, 0, 0.5, 0, 0.4, all(16));
        boom.pattern = "b.......b...b...";
        Track rim = track(s, DRUM, 0, 0.2, 0.3, 0.25, "...." + "XXXXXXXX" + "XXXX");
        rim.pattern = "....r.......r..r";
        Track shaker = track(s, DRUM, 0, 0.14, 0.35, 0.2, all(16));
        shaker.pattern = "h.h.H.h.h.h.H.h.";
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.5);
        s.reverb = 0.75;
        s.volume = 0.78;
        return s;
    }

    /** The blood moon: a slow heartbeat under a dark choir. */
    private static Song bloodMoon() {
        Song s = new Song();
        s.bpm = 58;
        s.chords = chords("Em Em F Em Em C B B Em Em F Em Am C B B");
        Track choir = track(s, PAD, Synth.CHOIR, 0.22, 0, 0.75, all(16));
        choir.octave = 50;
        Track low = track(s, PAD, Synth.STRINGS, 0.13, -0.2, 0.6, "..XX" + "XXXXXXXX" + "XXXX");
        low.octave = 40;
        Track bass = track(s, BASS, Synth.BASS, 0.3, 0, 0.3, all(16));
        bass.octave = 28;
        bass.pattern = "R.......R.......";
        Track heart = track(s, DRUM, 0, 0.5, 0, 0.3, "..XX" + "XXXXXXXX" + "XXXX");
        heart.pattern = "k..k............";
        Track piano = track(s, MELODY, Synth.EPIANO, 0.3, 0.15, 0.7, "...." + "XXXXXXXX" + "XXXX");
        piano.echo = 0.4;
        melody(piano, 4, "G5:6 E5:2 B4:8 | C5:4 E5:4 G5:8 | F#5:8 D#5:8 | B4:16 | "
                + "E5:4 G5:4 B5:8 | A5:6 G5:2 E5:8 | F5:8 C5:8 | E5:16 | "
                + "A5:6 C6:2 E6:8 | G5:4 E5:4 C5:8 | D#5:6 F#5:2 B5:8 | B4:16");
        Track tom = track(s, DRUM, 0, 0.3, -0.3, 0.5, "........" + "XXXXXXXX");
        tom.pattern = "t...............";
        Track spark = track(s, SPARKLE, Synth.BELL, 0.1, 0, 0.9, all(16));
        spark.octave = 84;
        spark.chance = 0.08;
        spark.echo = 0.5;
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.75);
        s.reverb = 0.92;
        s.volume = 0.75;
        return s;
    }

    private static Song jungle() {
        Song s = new Song();
        s.bpm = 100;
        s.chords = chords("Am Am G G "
                + "Am F C G Am F G Am "
                + "F G Em Am Dm Am F G "
                + "Am G F G");
        Track marimba = track(s, ARP, Synth.MARIMBA, 0.34, 0.2, 0.3, all(24));
        marimba.octave = 55;
        marimba.pattern = "0 2 3 2 4 3 2 1";
        Track pad = track(s, PAD, Synth.PAD, 0.14, -0.1, 0.55, "...." + "XXXXXXXX" + "XXXXXXXX" + "XXXX");
        pad.octave = 55;
        Track bass = track(s, BASS, Synth.PLUCK_BASS, 0.42, 0, 0.15, all(24));
        bass.octave = 33;
        bass.pattern = "R..R..F.R...F.O.";
        Track flute = track(s, MELODY, Synth.FLUTE, 0.28, -0.2, 0.45, "...." + "XXXXXXXX" + "........" + "....");
        melody(flute, 4, "E5:4 A5:4 G5:4 E5:4 | A5:6 G5:2 E5:8 | G5:4 E5:4 C5:8 | D5:6 E5:2 G5:8 | "
                + "A5:4 C6:4 A5:4 G5:4 | A5:6 G5:2 E5:4 C5:4 | D5:4 G5:4 B5:8 | A5:12 r:4");
        Track piano = track(s, MELODY, Synth.EPIANO, 0.3, 0.15, 0.45, "............" + "XXXXXXXX" + "....");
        melody(piano, 12, "C6:4 A5:4 F5:8 | D6:4 B5:4 G5:8 | E6:6 D6:2 B5:8 | C6:4 A5:4 E5:8 | "
                + "F5:4 A5:4 D6:8 | E6:6 C6:2 A5:8 | C6:4 A5:4 G5:4 F5:4 | G5:8 B5:4 D6:4");
        Track tom = track(s, DRUM, 0, 0.36, -0.3, 0.3, "..XX" + "XXXXXXXX" + "XXXXXXXX" + "XXXX");
        tom.pattern = "t..t..t.t...t.t.";
        Track shaker = track(s, DRUM, 0, 0.22, 0.35, 0.25, "...." + "XXXXXXXX" + "XXXXXXXX" + "XX..");
        shaker.pattern = "h.h.H.h.h.h.H.h.";
        Track kick = track(s, DRUM, 0, 0.45, 0, 0.1, "...." + "....XXXX" + "XXXXXXXX" + "....");
        kick.pattern = "k.......k..k....";
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.75);
        return s;
    }

    private static Song corruption() {
        Song s = new Song();
        s.bpm = 66;
        s.chords = chords("Em Em F F Em Em Dm Dm Em F Em Dm C Bdim Em Em");
        Track choir = track(s, PAD, Synth.CHOIR, 0.2, 0, 0.75, all(16));
        choir.octave = 52;
        Track strings = track(s, PAD, Synth.STRINGS, 0.1, -0.2, 0.6, "...." + "XXXXXXXX" + "XXXX");
        strings.octave = 43;
        Track bass = track(s, BASS, Synth.PLUCK_BASS, 0.4, 0, 0.35, all(16));
        bass.octave = 28;
        bass.pattern = "R.......R.......";
        Track boom = track(s, DRUM, 0, 0.5, 0, 0.5, all(16));
        boom.pattern = "b...............";
        Track piano = track(s, MELODY, Synth.EPIANO, 0.3, 0.2, 0.7, "...." + "XXXXXXXX" + "????");
        piano.echo = 0.35;
        melody(piano, 4, "B4:12 r:4 | C5:12 r:4 | B4:4 G4:4 E4:8 | D5:12 r:4 | E5:6 F5:2 E5:8 | B4:16 | "
                + "C5:4 B4:4 A4:8 | B4:16");
        melody(piano, 12, "G4:8 E5:8 | F5:12 r:4 | E5:4 B4:4 G4:8 | E4:16");
        Track spark = track(s, SPARKLE, Synth.BELL, 0.12, 0, 0.85, all(16));
        spark.octave = 76;
        spark.chance = 0.1;
        spark.echo = 0.5;
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.75);
        s.reverb = 0.9;
        return s;
    }

    private static Song underworld() {
        Song s = new Song();
        s.bpm = 84;
        s.chords = chords("Dm Dm Bb A Dm Dm Gm A Dm Bb Gm A Bb C A A");
        Track bass = track(s, BASS, Synth.PLUCK_BASS, 0.34, 0, 0.2, all(16));
        bass.octave = 26;
        bass.pattern = "R.R.R.R.R.R.R.R.";
        Track choir = track(s, PAD, Synth.CHOIR, 0.18, -0.15, 0.6, all(16));
        choir.octave = 50;
        Track strings = track(s, MELODY, Synth.STRINGS, 0.3, 0.15, 0.5, "...." + "XXXXXXXX" + "XXXX");
        melody(strings, 4, "D5:8 F5:4 E5:4 | D5:6 A4:2 A4:8 | Bb4:8 D5:4 F5:4 | E5:12 C#5:4 | "
                + "D5:4 F5:4 A5:8 | G5:6 F5:2 E5:8 | D5:4 Bb4:4 G4:8 | A4:16");
        melody(strings, 12, "F5:8 D5:8 | G5:8 E5:8 | A5:12 G5:4 | E5:16");
        Track tom = track(s, DRUM, 0, 0.4, -0.25, 0.35, "..XX" + "XXXXXXXX" + "XXXX");
        tom.pattern = "t...t.t.t...t.t.";
        Track boom = track(s, DRUM, 0, 0.42, 0, 0.45, all(16));
        boom.pattern = "b.......b.......";
        Track rim = track(s, DRUM, 0, 0.18, 0.3, 0.3, "...." + "....XXXX" + "XXXX");
        rim.pattern = "....r.......r..r";
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.5);
        s.reverb = 0.85;
        return s;
    }

    // ---- notation --------------------------------------------------------

    private static int pitchClass(String s, int[] pos) {
        int pc;
        switch (Character.toUpperCase(s.charAt(pos[0]))) {
            case 'C': pc = 0; break;
            case 'D': pc = 2; break;
            case 'E': pc = 4; break;
            case 'F': pc = 5; break;
            case 'G': pc = 7; break;
            case 'A': pc = 9; break;
            default: pc = 11;
        }
        pos[0]++;
        if (pos[0] < s.length() && s.charAt(pos[0]) == '#') { pc++; pos[0]++; }
        else if (pos[0] < s.length() && s.charAt(pos[0]) == 'b') { pc--; pos[0]++; }
        return Math.floorMod(pc, 12);
    }

    private static int midi(String s) {
        int[] pos = {0};
        int pc = pitchClass(s, pos);
        int oct = s.charAt(pos[0]) - '0';
        // B# and Cb cross octaves
        return 12 * (oct + 1) + pc;
    }

    private static Chord chord(String s) {
        Chord c = new Chord();
        c.name = s;
        String main = s, bass = null;
        int slash = s.indexOf('/');
        if (slash > 0) {
            main = s.substring(0, slash);
            bass = s.substring(slash + 1);
        }
        int[] pos = {0};
        c.root = pitchClass(main, pos);
        String q = main.substring(pos[0]);
        switch (q) {
            case "m": c.iv = new int[]{0, 3, 7}; break;
            case "7": c.iv = new int[]{0, 4, 7, 10}; break;
            case "maj7": c.iv = new int[]{0, 4, 7, 11}; break;
            case "m7": c.iv = new int[]{0, 3, 7, 10}; break;
            case "m9": c.iv = new int[]{0, 3, 7, 10, 14}; break;
            case "add9": c.iv = new int[]{0, 4, 7, 14}; break;
            case "sus2": c.iv = new int[]{0, 2, 7}; break;
            case "sus4": c.iv = new int[]{0, 5, 7}; break;
            case "6": c.iv = new int[]{0, 4, 7, 9}; break;
            case "dim": c.iv = new int[]{0, 3, 6}; break;
            default: c.iv = new int[]{0, 4, 7};
        }
        if (bass != null) c.bass = pitchClass(bass, new int[]{0});
        return c;
    }

    private static Chord[] chords(String s) {
        String[] parts = s.trim().split("\\s+");
        Chord[] out = new Chord[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = chord(parts[i]);
        return out;
    }

    /** Adds a melody starting at a bar; notes like "E5:4" (length in 16th notes), "r:4" = rest. */
    private static void melody(Track t, int startBar, String notes) {
        int step = startBar * 16;
        for (String tok : notes.replace("|", " ").trim().split("\\s+")) {
            String[] p = tok.split(":");
            int len = Integer.parseInt(p[1]);
            if (!p[0].equals("r")) t.notes.add(new Note(step, len, midi(p[0])));
            step += len;
        }
    }

    private static Track track(Song s, int kind, int inst, double gain, double pan, double rev, String mask) {
        Track t = new Track();
        t.kind = kind;
        t.inst = inst;
        t.gain = gain;
        t.pan = pan;
        t.rev = rev;
        t.mask = mask;
        s.tracks.add(t);
        return t;
    }

    private static String all(int bars) {
        return "X".repeat(bars);
    }

    // ---- songs --------------------------------------------------------------

    private static Song day() {
        Song s = new Song();
        s.bpm = 94;
        s.chords = chords("G Cmaj7 G Dsus4 "
                + "G D/F# Em7 Cmaj7 G/B Am7 Dsus4 D "
                + "G D/F# Em7 Cmaj7 G/B Am7 Dsus4 D "
                + "Em C G D Em C Am7 D "
                + "Cmaj7 G/B Am7 Dsus4");
        //                         intro   A        A'       B        bridge
        Track guitar = track(s, ARP, Synth.PLUCK, 0.42, -0.25, 0.25, all(32));
        guitar.octave = 43;
        guitar.pattern = "0 2 1 3 2 4 3 2";
        Track pad = track(s, PAD, Synth.PAD, 0.16, 0.1, 0.5, "XXXX" + "XXXXXXXX" + "XXXXXXXX" + "XXXXXXXX" + "XXXX");
        pad.octave = 55;
        Track bass = track(s, BASS, Synth.BASS, 0.34, 0, 0.1, "...." + "XXXXXXXX" + "XXXXXXXX" + "XXXXXXXX" + "XXX.");
        bass.octave = 31;
        bass.pattern = "R.......F.....R.";
        Track flute = track(s, MELODY, Synth.FLUTE, 0.3, 0.15, 0.4, "...." + "XXXXXXXX" + "XXXXXXXX" + "........" + "....");
        melody(flute, 4, "D5:4 G5:4 B5:6 A5:2 | A5:6 G5:2 F#5:4 D5:4 | E5:4 G5:4 B5:4 D6:4 | C6:6 B5:2 G5:8 | "
                + "B5:4 A5:4 G5:4 B5:4 | C6:6 B5:2 A5:4 E5:4 | G5:4 A5:4 D5:4 G5:4 | F#5:12 r:4");
        melody(flute, 12, "B5:4 D6:4 B5:4 G5:4 | A5:4 F#5:4 D5:8 | G5:6 A5:2 B5:4 G5:4 | E5:4 G5:4 C6:6 B5:2 | "
                + "D6:6 B5:2 G5:8 | A5:4 C6:4 E6:4 C6:4 | D6:4 A5:4 G5:4 A5:4 | F#5:4 A5:4 D6:8");
        Track piano = track(s, MELODY, Synth.EPIANO, 0.34, -0.1, 0.45, all(32));
        melody(piano, 20, "B4:4 E5:4 G5:6 F#5:2 | E5:6 D5:2 C5:4 G4:4 | B4:4 D5:4 G5:8 | F#5:6 E5:2 D5:4 A4:4 | "
                + "G5:4 B5:4 E6:6 D6:2 | C6:6 B5:2 G5:4 E5:4 | A5:4 G5:4 E5:4 C5:4 | D5:8 r:4 A4:4");
        Track bells = track(s, MELODY, Synth.BELL, 0.2, 0.3, 0.6, all(32));
        melody(bells, 28, "G6:8 E6:8 | D6:16 | C6:8 E6:8 | D6:12 r:4");
        melody(bells, 0, "r:16 | G6:8 E6:8 | r:16 | A6:8 F#6:8");
        Track kick = track(s, DRUM, 0, 0.5, 0, 0.1, "...." + "........" + "XXXXXXXX" + "XXXXXXX." + "....");
        kick.pattern = "k.......k.......";
        Track shaker = track(s, DRUM, 0, 0.3, 0.35, 0.2, "...." + "....XXXX" + "XXXXXXXX" + "XXXXXXXX" + "XX..");
        shaker.pattern = "..h...h...h...h.";
        Track brush = track(s, DRUM, 0, 0.22, -0.2, 0.3, "...." + "........" + "XXXXXXXX" + "XXXXXXXX" + "....");
        brush.pattern = "....s.......s...";
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.75);
        return s;
    }

    private static Song night() {
        Song s = new Song();
        s.bpm = 70;
        s.chords = chords("Em7 Cmaj7 Em7 Cmaj7 "
                + "Em7 Cmaj7 G D Am7 Em7 Cmaj7 D "
                + "Cmaj7 D Bm7 Em7 Am7 Bm7 Cmaj7 D "
                + "Em7 Cmaj7 Am7 Bm7");
        Track harp = track(s, ARP, Synth.HARP, 0.3, 0.3, 0.55, all(24));
        harp.octave = 40;
        harp.pattern = "0 2 4 3 5 3 4 2";
        Track pad = track(s, PAD, Synth.PAD, 0.2, -0.1, 0.65, all(24));
        pad.octave = 52;
        Track bass = track(s, BASS, Synth.BASS, 0.26, 0, 0.2, "...." + "XXXXXXXX" + "XXXXXXXX" + "XXXX");
        bass.octave = 28;
        bass.pattern = "R...............";
        Track piano = track(s, MELODY, Synth.EPIANO, 0.36, -0.15, 0.55, "...." + "XXXXXXXX" + "????????" + "....");
        melody(piano, 4, "G5:6 F#5:2 E5:8 | G5:4 B5:4 E5:8 | D5:6 E5:2 G5:4 B4:4 | A4:12 r:4 | "
                + "C5:4 E5:4 A5:6 G5:2 | G5:6 F#5:2 E5:4 B4:4 | E5:4 G5:4 B5:8 | A5:6 F#5:2 D5:8");
        Track bells = track(s, MELODY, Synth.BELL, 0.24, 0.25, 0.7, "............" + "XXXXXXXX" + "....");
        melody(bells, 12, "E6:8 D6:4 B5:4 | A5:8 F#5:8 | D6:4 B5:4 F#5:8 | G5:6 A5:2 B5:8 | "
                + "C6:6 B5:2 A5:4 E5:4 | D6:4 B5:4 A5:4 F#5:4 | G5:4 E5:4 C5:4 E5:4 | F#5:8 A5:8");
        Track strings = track(s, PAD, Synth.STRINGS, 0.12, 0.0, 0.7, "............" + "XXXXXXXX" + "XXXX");
        strings.octave = 64;
        Track spark = track(s, SPARKLE, Synth.BELL, 0.12, 0, 0.8, "XXXX" + "........" + "........" + "XXXX");
        spark.octave = 84;
        spark.chance = 0.18;
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.75);
        s.reverb = 0.88;
        return s;
    }

    private static Song cave() {
        Song s = new Song();
        s.bpm = 78;
        s.chords = chords("Dm Dm Bbmaj7 C "
                + "Dm Bbmaj7 F C Dm Gm7 Bbmaj7 Asus4 "
                + "Gm7 Dm Bbmaj7 F Gm7 Dm C Asus4 "
                + "Dm Bbmaj7 Gm7 Asus4");
        Track pluck = track(s, ARP, Synth.PLUCK, 0.3, 0.2, 0.35, all(24));
        pluck.octave = 50;
        pluck.pattern = "0 2 1 3 0 2 4 3";
        pluck.echo = 0.35;
        Track drone = track(s, PAD, Synth.PAD, 0.2, -0.15, 0.7, all(24));
        drone.octave = 45;
        Track bass = track(s, BASS, Synth.PLUCK_BASS, 0.4, 0, 0.3, "..XX" + "XXXXXXXX" + "XXXXXXXX" + "XXXX");
        bass.octave = 26;
        bass.pattern = "R.........R.....";
        Track flute = track(s, MELODY, Synth.FLUTE, 0.26, -0.2, 0.55, "...." + "XXXXXXXX" + "........" + "....");
        flute.echo = 0.15;
        melody(flute, 4, "A4:6 D5:2 F5:8 | D5:6 F5:2 A5:8 | A5:6 G5:2 F5:8 | E5:6 D5:2 C5:8 | "
                + "D5:4 F5:4 A5:6 G5:2 | F5:6 D5:2 Bb4:8 | A4:6 Bb4:2 D5:8 | D5:4 E5:4 A4:8");
        Track piano = track(s, MELODY, Synth.EPIANO, 0.3, 0.1, 0.6, "............" + "XXXXXXXX" + "....");
        piano.echo = 0.25;
        melody(piano, 12, "Bb5:8 A5:4 G5:4 | F5:8 A5:8 | D6:6 C6:2 A5:8 | C6:8 A5:4 F5:4 | "
                + "G5:6 A5:2 Bb5:8 | A5:6 F5:2 D5:8 | E5:4 G5:4 C6:8 | A5:12 r:4");
        Track spark = track(s, SPARKLE, Synth.BELL, 0.13, 0, 0.8, all(24));
        spark.octave = 81;
        spark.chance = 0.12;
        spark.echo = 0.4;
        Track tom = track(s, DRUM, 0, 0.3, -0.25, 0.4, "...." + "....XXXX" + "XXXXXXXX" + "....");
        tom.pattern = "t.........t..t..";
        Track shaker = track(s, DRUM, 0, 0.18, 0.3, 0.35, "...." + "........" + "XXXXXXXX" + "....");
        shaker.pattern = "h...h.h.h...h.h.";
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.75);
        s.reverb = 0.88;
        return s;
    }

    private static Song title() {
        Song s = new Song();
        s.bpm = 80;
        s.chords = chords("C G/B Am7 Fmaj7 C/E Dm7 Fmaj7 G "
                + "Am7 Em7 Fmaj7 C/E Dm7 G Fmaj7 Gsus4");
        Track piano = track(s, MELODY, Synth.EPIANO, 0.38, -0.1, 0.5, all(16));
        melody(piano, 0, "E5:4 G5:4 C6:6 B5:2 | B5:4 D6:4 G5:8 | A5:4 C6:4 E6:6 D6:2 | A5:6 G5:2 F5:4 E5:4 | "
                + "G5:4 E5:4 C5:8 | D5:4 F5:4 A5:4 C6:4 | A5:6 G5:2 F5:8 | B4:4 D5:4 G5:8");
        Track flute = track(s, MELODY, Synth.FLUTE, 0.27, 0.15, 0.5, all(16));
        melody(flute, 8, "C6:6 B5:2 A5:8 | G5:6 A5:2 B5:8 | A5:4 C6:4 E6:8 | D6:4 C6:4 G5:8 | "
                + "F5:4 A5:4 C6:6 A5:2 | B5:6 A5:2 G5:8 | A5:6 G5:2 E5:4 C5:4 | D5:12 r:4");
        Track harp = track(s, ARP, Synth.HARP, 0.26, 0.3, 0.45, all(16));
        harp.octave = 43;
        harp.pattern = "0 2 4 2 3 2 4 2";
        Track strings = track(s, PAD, Synth.STRINGS, 0.14, -0.2, 0.6, all(16));
        strings.octave = 55;
        Track bass = track(s, BASS, Synth.BASS, 0.3, 0, 0.15, all(16));
        bass.octave = 31;
        bass.pattern = "R.......R.......";
        s.echoTime = (int) (Synth.RATE * 60.0 / s.bpm * 0.75);
        return s;
    }

    // ---- playback ----------------------------------------------------------

    /** A song being played (two can sound at once while cross-fading). */
    private final class SongPlayer {
        final Song song;
        final int id;
        final Synth.Voice[] voices = new Synth.Voice[48];
        double gain, target = 1, fadeSpeed;
        long sample;
        int step = -1;
        final boolean[] on;
        int[] padVoicing = new int[0];

        SongPlayer(Song song, int id) {
            this.song = song;
            this.id = id;
            for (int i = 0; i < voices.length; i++) voices[i] = new Synth.Voice();
            on = new boolean[song.tracks.size() * song.chords.length];
            decideLoop();
        }

        int stepSamples() {
            return (int) (Synth.RATE * 60.0 / song.bpm / 4);
        }

        private void decideLoop() {
            for (int t = 0; t < song.tracks.size(); t++) {
                Track tr = song.tracks.get(t);
                for (int b = 0; b < song.chords.length; b++) {
                    char c = b < tr.mask.length() ? tr.mask.charAt(b) : 'X';
                    boolean play = c == 'X' || (c == '?' && rnd.nextBoolean());
                    on[t * song.chords.length + b] = play;
                }
            }
        }

        private Synth.Voice free() {
            Synth.Voice best = null;
            long oldest = -1;
            for (Synth.Voice v : voices) {
                if (!v.active) return v;
                if (v.age > oldest) {
                    oldest = v.age;
                    best = v;
                }
            }
            return best;
        }

        private void play(Track tr, int inst, int midi, double vel, int lenSteps, double panOffset) {
            Synth.Voice v = free();
            long len = (long) lenSteps * stepSamples() - stepSamples() / 6;
            long delay = tr.kind == MELODY || tr.kind == PAD ? rnd.nextInt(120) : rnd.nextInt(300);
            double f = inst >= Synth.KICK && inst <= Synth.BOOM && inst != Synth.TOM ? 1 : Synth.midiFreq(midi);
            v.start(inst, f, vel * tr.gain * song.volume, Math.max(-1, Math.min(1, tr.pan + panOffset)), Math.max(1, len), delay,
                    rnd.nextInt());
            v.rev = tr.rev;
            v.echo = tr.echo;
        }

        private int[] arpTones(Chord c, int base) {
            int root = base + c.root;
            while (root > base + 11) root -= 12;
            int bass = c.bass >= 0 ? base + c.bass : root;
            while (bass > root) bass -= 12;
            if (bass < base - 5) bass += 12;
            int[] iv = c.iv;
            int third = iv.length > 1 ? iv[1] : 4;
            int fifth = iv.length > 2 ? iv[2] : 7;
            int top = iv.length > 3 ? iv[3] : 12;
            return new int[]{bass, root + fifth, root + 12, root + 12 + third, root + 12 + fifth, root + 12 + top,
                    root + 24, root + 24 + third};
        }

        private int[] voicing(Chord c, int base) {
            int[] out = new int[c.iv.length];
            for (int i = 0; i < c.iv.length; i++) {
                int n = base + Math.floorMod(c.root + c.iv[i] - base, 12);
                // keep close to the previous chord
                if (padVoicing.length > 0) {
                    int best = n, bd = 99;
                    for (int o = -12; o <= 12; o += 12) {
                        for (int p : padVoicing) {
                            int d = Math.abs(n + o - p);
                            if (d < bd && n + o >= base - 3 && n + o <= base + 17) {
                                bd = d;
                                best = n + o;
                            }
                        }
                    }
                    n = best;
                }
                out[i] = n;
            }
            return out;
        }

        void onStep(int stepInSong) {
            int bars = song.chords.length;
            int bar = stepInSong / 16, inBar = stepInSong % 16;
            if (stepInSong == 0 && sample > 0) decideLoop();
            Chord ch = song.chords[bar];
            for (int t = 0; t < song.tracks.size(); t++) {
                Track tr = song.tracks.get(t);
                if (!on[t * bars + bar]) continue;
                switch (tr.kind) {
                    case MELODY:
                        for (Note n : tr.notes)
                            if (n.start == stepInSong) play(tr, tr.inst, n.midi, 0.9 + rnd.nextDouble() * 0.1, n.len, 0);
                        break;
                    case ARP: {
                        int per = 16 / (tr.stepDiv * 4);
                        if (inBar % per != 0) break;
                        String[] pat = tr.pattern.split(" ");
                        String tok = pat[(inBar / per) % pat.length];
                        if (tok.equals(".")) break;
                        int[] tones = arpTones(ch, tr.octave);
                        int idx = Math.min(tones.length - 1, Integer.parseInt(tok));
                        double vel = (inBar % 8 == 0 ? 0.95 : 0.7) * (0.85 + rnd.nextDouble() * 0.25);
                        play(tr, tr.inst, tones[idx], vel, per * 3, (rnd.nextDouble() - 0.5) * 0.2);
                        break;
                    }
                    case PAD: {
                        if (inBar != 0) break;
                        if (bar > 0 && song.chords[bar - 1].name.equals(ch.name) && on[t * bars + bar - 1]) break;
                        int same = 1;
                        while (bar + same < bars && song.chords[bar + same].name.equals(ch.name)
                                && on[t * bars + bar + same]) same++;
                        int[] vo = voicing(ch, tr.octave);
                        if (tr.inst == Synth.PAD || tr.inst == Synth.CHOIR) padVoicing = vo;
                        for (int k = 0; k < vo.length; k++)
                            play(tr, tr.inst, vo[k], 0.8, same * 16, (k - vo.length / 2.0) * 0.25);
                        break;
                    }
                    case BASS: {
                        char c = tr.pattern.charAt(inBar % tr.pattern.length());
                        if (c == '.') break;
                        int root = ch.bass >= 0 ? ch.bass : ch.root;
                        int n = tr.octave + Math.floorMod(root - tr.octave, 12);
                        if (c == 'F') n = tr.octave + Math.floorMod(ch.root + 7 - tr.octave, 12);
                        else if (c == 'O') n += 12;
                        int len = 1;
                        while (inBar + len < 16 && tr.pattern.charAt((inBar + len) % tr.pattern.length()) == '.') len++;
                        play(tr, tr.inst, n, 0.9, len, 0);
                        break;
                    }
                    case DRUM: {
                        char c = tr.pattern.charAt(inBar % tr.pattern.length());
                        int inst;
                        switch (Character.toLowerCase(c)) {
                            case 'k': inst = Synth.KICK; break;
                            case 'h': inst = Synth.SHAKER; break;
                            case 's': inst = Synth.BRUSH; break;
                            case 't': inst = Synth.TOM; break;
                            case 'r': inst = Synth.RIM; break;
                            case 'b': inst = Synth.BOOM; break;
                            default: inst = -1;
                        }
                        if (inst < 0) break;
                        double vel = (Character.isUpperCase(c) ? 1.0 : 0.75) * (0.85 + rnd.nextDouble() * 0.3);
                        play(tr, inst, inst == Synth.TOM ? 43 + (inBar % 3) * 3 : 60, vel, 2, 0);
                        break;
                    }
                    default: { // sparkles
                        if (inBar % 2 != 0 || rnd.nextDouble() > tr.chance) break;
                        int pc = ch.root + ch.iv[rnd.nextInt(ch.iv.length)];
                        int n = tr.octave + Math.floorMod(pc - tr.octave, 12);
                        play(tr, tr.inst, n, 0.5 + rnd.nextDouble() * 0.4, 4, (rnd.nextDouble() - 0.5) * 1.2);
                    }
                }
            }
        }

        /** Renders into the dry, reverb and echo buses. Returns false when faded out. */
        boolean render(float[] dl, float[] dr, float[] rv, float[] ec, int n) {
            int ss = stepSamples();
            int total = song.chords.length * 16;
            for (int i = 0; i < n; i++) {
                int st = (int) (sample / ss);
                if (st != step) {
                    step = st;
                    onStep(st % total);
                }
                sample++;
                if (gain != target) {
                    gain += Math.signum(target - gain) * fadeSpeed;
                    if (Math.abs(gain - target) < fadeSpeed) gain = target;
                }
                float l = 0, r = 0, rev = 0, echoS = 0;
                for (Synth.Voice v : voices) {
                    if (!v.active) continue;
                    float x = (float) v.next();
                    float lg = (float) Math.min(1, 1 - v.pan), rg = (float) Math.min(1, 1 + v.pan);
                    l += x * lg;
                    r += x * rg;
                    rev += x * (float) v.rev;
                    echoS += x * (float) v.echo;
                }
                float g = (float) gain;
                dl[i] += l * g;
                dr[i] += r * g;
                rv[i] += rev * g;
                ec[i] += echoS * g;
            }
            return !(target == 0 && gain <= 0);
        }
    }

    // ---- public -------------------------------------------------------------

    void request(int id) {
        wantId = id;
    }

    int current() {
        return cur == null ? NONE : cur.id;
    }

    private float[] dl = new float[0], dr = new float[0], rv = new float[0], ec = new float[0];

    /** Adds n stereo samples (-1..1) to the buffers. */
    void render(float[] outL, float[] outR, int n, double volume) {
        if (cur == null ? wantId != NONE : cur.id != wantId) {
            if (cur != null) {
                old = cur;
                old.target = 0;
                old.fadeSpeed = 1.0 / (Synth.RATE * 2.5);
            }
            cur = wantId == NONE ? null : new SongPlayer(songs[wantId], wantId);
            if (cur != null) {
                cur.gain = 0;
                cur.target = 1;
                cur.fadeSpeed = 1.0 / (Synth.RATE * (old == null ? 1.0 : 3.0));
                echo.time = cur.song.echoTime;
                reverb.feedback = (float) (0.7 + cur.song.reverb * 0.28);
            }
        }
        if (dl.length < n) {
            dl = new float[n];
            dr = new float[n];
            rv = new float[n];
            ec = new float[n];
        }
        java.util.Arrays.fill(dl, 0, n, 0);
        java.util.Arrays.fill(dr, 0, n, 0);
        java.util.Arrays.fill(rv, 0, n, 0);
        java.util.Arrays.fill(ec, 0, n, 0);
        if (old != null && !old.render(dl, dr, rv, ec, n)) old = null;
        if (cur != null) cur.render(dl, dr, rv, ec, n);
        float vol = (float) volume;
        for (int i = 0; i < n; i++) {
            echo.process(ec[i], ec[i]);
            reverb.process(rv[i] + echo.outL * 0.5f, rv[i] + echo.outR * 0.5f);
            outL[i] += (dl[i] + echo.outL * 0.8f + reverb.outL * 2.6f) * vol * 0.55f;
            outR[i] += (dr[i] + echo.outR * 0.8f + reverb.outR * 2.6f) * vol * 0.55f;
        }
    }
}
