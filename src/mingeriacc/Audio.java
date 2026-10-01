package mingeriacc;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;

/**
 * Sound engine: all sound effects are synthesized at startup and mixed in
 * stereo with the music on a thread of their own. Without an audio device
 * the game simply runs silently.
 */
final class Audio implements Runnable {
    static final int RATE = Synth.RATE;
    static final int DIG_SOFT = 0, DIG_HARD = 1, BREAK = 2, PLACE = 3, PICKUP = 4, JUMP = 5, LAND = 6,
            TICK = 7, CRAFT = 8, SWING = 9, TREE = 10, CHOP = 11, OPEN = 12, GRASS = 13, MENU = 14,
            HURT = 15, DEATH = 16, SQUISH = 17, SLIME_DIE = 18, BONE = 19, HIT = 20, KILL = 21, ARROW_HIT = 22,
            BOW = 23, COIN = 24, HEAL = 25, CRYSTAL = 26, ZOMBIE = 27, GLASS = 28, METAL = 29, POT = 30,
            HISS = 31, SPLASH = 32, DOOR = 33, CHEST = 34, SPELL = 35, ROAR = 36, THUNDER = 37, STAR = 38,
            ZAP = 39, FREEZE = 40, MIRROR = 41, SQUEAK = 42, BUZZ = 43, GROWL = 44, SQUELCH = 45, OOF = 46,
            RATTLE = 47, BLUB = 48, SQUAWK = 49;
    private static final int SFX_COUNT = 50;

    private static final class Voice {
        float[] data;
        double pos, rate;
        float gl, gr;
    }

    private final float[][] sfx = new float[SFX_COUNT][];
    private final Voice[] voices = new Voice[32];
    private final Music music = new Music();
    private final Random rnd = new Random(5);
    private SourceDataLine line;
    private volatile boolean running;
    boolean ok;

    volatile double sfxVolume = 0.8, musicVolume = 0.6;
    volatile boolean musicMuted;

    Audio() {
        for (int i = 0; i < voices.length; i++) voices[i] = new Voice();
        buildSfx();
    }

    void start() {
        try {
            AudioFormat fmt = new AudioFormat(RATE, 16, 2, true, false);
            line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, 4 * 3072);
            line.start();
            ok = true;
            running = true;
            Thread t = new Thread(this, "Mingeriacc-audio");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
        } catch (Exception | Error e) {
            ok = false;
            System.err.println("Could not open an audio device, continuing without sound: " + e);
        }
    }

    void stop() {
        running = false;
    }

    void play(int id, double vol, double pitch) {
        play(id, vol, pitch, 0);
    }

    /** Plays a sound; pan -1 (left) .. 1 (right). */
    void play(int id, double vol, double pitch, double pan) {
        if (!ok || sfxVolume <= 0) return;
        synchronized (voices) {
            Voice best = null;
            for (Voice v : voices) {
                if (v.data == null) { best = v; break; }
                if (best == null || v.pos > best.pos) best = v;
            }
            best.data = sfx[id];
            best.pos = 0;
            best.rate = pitch;
            best.gl = (float) (vol * Math.min(1, 1 - pan * 0.8));
            best.gr = (float) (vol * Math.min(1, 1 + pan * 0.8));
        }
    }

    void setMusic(int id) {
        music.request(id);
    }

    @Override
    public void run() {
        int n = 512;
        float[] l = new float[n], r = new float[n];
        byte[] out = new byte[n * 4];
        while (running) {
            java.util.Arrays.fill(l, 0f);
            java.util.Arrays.fill(r, 0f);
            music.render(l, r, n, musicMuted ? 0 : musicVolume);
            mixSfx(l, r, n);
            for (int i = 0; i < n; i++) {
                int sl = clip(l[i]), sr = clip(r[i]);
                out[i * 4] = (byte) sl;
                out[i * 4 + 1] = (byte) (sl >> 8);
                out[i * 4 + 2] = (byte) sr;
                out[i * 4 + 3] = (byte) (sr >> 8);
            }
            line.write(out, 0, out.length);
        }
        line.drain();
        line.close();
    }

    private void mixSfx(float[] l, float[] r, int n) {
        float sv = (float) sfxVolume;
        synchronized (voices) {
            for (Voice v : voices) {
                if (v.data == null) continue;
                float[] d = v.data;
                for (int i = 0; i < n; i++) {
                    int p = (int) v.pos;
                    if (p >= d.length - 1) { v.data = null; break; }
                    float f = (float) (v.pos - p);
                    float x = (d[p] * (1 - f) + d[p + 1] * f) * sv;
                    l[i] += x * v.gl;
                    r[i] += x * v.gr;
                    v.pos += v.rate;
                }
            }
        }
    }

    /** Soft limiter to 16 bits. */
    private static int clip(float x) {
        if (x > 1.5f) x = 1.5f;
        else if (x < -1.5f) x = -1.5f;
        float y = x - x * x * x * 0.148f; // gentle saturation, reaches 1.0 at 1.5
        return (int) (y * 30000);
    }

    // ---- synthesis ----------------------------------------------------------

    private float[] buf(double sec) {
        return new float[Math.max(1, (int) (sec * RATE))];
    }

    private float[] noise(double sec) {
        float[] o = buf(sec);
        for (int i = 0; i < o.length; i++) o[i] = rnd.nextFloat() * 2 - 1;
        return o;
    }

    /** One-pole low-pass with a cutoff in Hz. */
    private float[] lowpass(float[] s, double hz) {
        double a = 1 - Math.exp(-2 * Math.PI * hz / RATE);
        double y = 0;
        for (int i = 0; i < s.length; i++) {
            y += (s[i] - y) * a;
            s[i] = (float) y;
        }
        return s;
    }

    private float[] highpass(float[] s, double hz) {
        double a = 1 - Math.exp(-2 * Math.PI * hz / RATE);
        double y = 0;
        for (int i = 0; i < s.length; i++) {
            y += (s[i] - y) * a;
            s[i] = (float) (s[i] - y);
        }
        return s;
    }

    /** Resonant band-pass (biquad). */
    private float[] bandpass(float[] s, double hz, double q) {
        double w = 2 * Math.PI * hz / RATE, alpha = Math.sin(w) / (2 * q);
        double b0 = alpha, b2 = -alpha, a0 = 1 + alpha, a1 = -2 * Math.cos(w), a2 = 1 - alpha;
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < s.length; i++) {
            double x = s[i];
            double y = (b0 * x + b2 * x2 - a1 * y1 - a2 * y2) / a0;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            s[i] = (float) y;
        }
        return s;
    }

    /** Sine with a pitch slide f0 -> f1 (exponential). */
    private float[] sine(double sec, double f0, double f1) {
        float[] o = buf(sec);
        double ph = 0;
        for (int i = 0; i < o.length; i++) {
            double t = i / (double) o.length;
            ph += f0 * Math.pow(f1 / f0, t) / RATE;
            o[i] = (float) Math.sin(2 * Math.PI * ph);
        }
        return o;
    }

    /** FM tone: carrier f, modulator ratio, index decaying with idxDecay seconds. */
    private float[] fm(double sec, double f, double ratio, double index, double idxDecay) {
        float[] o = buf(sec);
        for (int i = 0; i < o.length; i++) {
            double t = i / (double) RATE;
            double mi = index * Math.exp(-t / idxDecay);
            o[i] = (float) Math.sin(2 * Math.PI * f * t + mi * Math.sin(2 * Math.PI * f * ratio * t));
        }
        return o;
    }

    /** A buzzing voice-like tone through two formant filters (for grunts and groans). */
    private float[] voice(double sec, double f0, double f1, double form1, double form2, double wobble) {
        float[] o = buf(sec);
        double ph = 0;
        for (int i = 0; i < o.length; i++) {
            double t = i / (double) o.length;
            double f = f0 * Math.pow(f1 / f0, t) * (1 + wobble * Math.sin(i * 2 * Math.PI * 5.0 / RATE));
            ph = (ph + f / RATE) % 1.0;
            o[i] = (float) (ph * 2 - 1);
        }
        float[] a = bandpass(o.clone(), form1, 4), b = bandpass(o, form2, 5);
        for (int i = 0; i < a.length; i++) a[i] = a[i] + b[i] * 0.6f;
        return a;
    }

    /** Envelope: linear attack, then exponential decay with a time constant (seconds). */
    private float[] env(float[] s, double attack, double decay, double gain) {
        int an = Math.max(1, (int) (attack * RATE));
        for (int i = 0; i < s.length; i++) {
            double a = Math.min(1.0, i / (double) an);
            double tail = Math.min(1, (s.length - i) / (RATE * 0.005));
            s[i] *= (float) (a * Math.exp(-i / (decay * RATE)) * tail * gain);
        }
        return s;
    }

    /** Random crackle: amplitude jumps for a crumbly texture. */
    private float[] crackle(float[] s, double grain) {
        int g = Math.max(1, (int) (grain * RATE));
        float amp = 1;
        for (int i = 0; i < s.length; i++) {
            if (i % g == 0) amp = 0.3f + rnd.nextFloat() * 0.9f;
            s[i] *= amp;
        }
        return s;
    }

    private float[] mix(float[]... layers) {
        int n = 0;
        for (float[] l : layers) n = Math.max(n, l.length);
        float[] o = new float[n];
        for (float[] l : layers) for (int i = 0; i < l.length; i++) o[i] += l[i];
        return o;
    }

    private float[] seq(double gap, float[]... parts) {
        int g = (int) (gap * RATE);
        int n = 0;
        for (float[] p : parts) n += p.length + g;
        float[] o = new float[n];
        int k = 0;
        for (float[] p : parts) {
            for (int i = 0; i < p.length; i++) o[k + i] += p[i];
            k += g > 0 ? g : p.length;
        }
        return o;
    }

    private void buildSfx() {
        sfx[DIG_SOFT] = mix(env(lowpass(noise(0.09), 900), 0.002, 0.025, 1.3),
                env(sine(0.07, 110, 55), 0.002, 0.03, 0.5));
        sfx[DIG_HARD] = mix(env(bandpass(noise(0.05), 3400, 3), 0.0005, 0.008, 2.2),
                env(sine(0.09, 2350, 2250), 0.0005, 0.02, 0.18), env(sine(0.09, 3320, 3300), 0.0005, 0.012, 0.1),
                env(lowpass(noise(0.06), 600), 0.001, 0.02, 0.7));
        sfx[BREAK] = mix(env(crackle(lowpass(noise(0.22), 1800), 0.012), 0.002, 0.06, 1.2),
                env(sine(0.12, 120, 50), 0.001, 0.04, 0.6));
        sfx[PLACE] = mix(env(sine(0.08, 150, 70), 0.001, 0.03, 0.8), env(lowpass(noise(0.04), 1400), 0.001, 0.01, 0.6));
        sfx[PICKUP] = seq(0.035, env(sine(0.06, 700, 1000), 0.002, 0.02, 0.45), env(sine(0.07, 1050, 1300), 0.002, 0.025, 0.35));
        sfx[JUMP] = env(bandpass(noise(0.12), 700, 1), 0.02, 0.04, 0.3);
        sfx[LAND] = mix(env(lowpass(noise(0.1), 500), 0.001, 0.03, 1.0), env(sine(0.1, 90, 45), 0.001, 0.04, 0.7));
        sfx[TICK] = env(sine(0.02, 1900, 1700), 0.0005, 0.005, 0.3);
        sfx[CRAFT] = seq(0.07, env(fm(0.5, 1046, 3.5, 0.8, 0.1), 0.002, 0.18, 0.3),
                env(fm(0.6, 1568, 3.5, 0.8, 0.1), 0.002, 0.25, 0.3));
        float[] sw = noise(0.16);
        double y = 0;
        for (int i = 0; i < sw.length; i++) {
            double t = i / (double) sw.length;
            double hz = 300 + 1500 * Math.sin(Math.PI * t);
            y += (sw[i] - y) * (1 - Math.exp(-2 * Math.PI * hz / RATE));
            sw[i] = (float) (y * Math.sin(Math.PI * t));
        }
        sfx[SWING] = env(sw, 0.001, 1.0, 1.6);
        float[] creak = env(voice(0.45, 70, 55, 400, 900, 0.08), 0.05, 0.3, 0.35);
        float[] crash = env(crackle(lowpass(noise(0.6), 1200), 0.02), 0.02, 0.2, 1.0);
        float[] thud = env(sine(0.3, 80, 35), 0.001, 0.1, 1.0);
        sfx[TREE] = mix(creak, seq(0.35, new float[1], crash), seq(0.5, new float[1], thud));
        sfx[CHOP] = mix(env(bandpass(noise(0.08), 520, 6), 0.001, 0.025, 3.0), env(sine(0.1, 230, 180), 0.001, 0.03, 0.6));
        sfx[OPEN] = mix(env(bandpass(noise(0.07), 1600, 1.5), 0.01, 0.02, 0.6), env(sine(0.05, 600, 900), 0.002, 0.015, 0.15));
        sfx[GRASS] = env(crackle(highpass(noise(0.08), 2500), 0.004), 0.004, 0.025, 0.8);
        sfx[MENU] = mix(env(sine(0.04, 1250, 1250), 0.001, 0.01, 0.25), env(sine(0.04, 1870, 1870), 0.001, 0.008, 0.12));
        sfx[HURT] = mix(env(voice(0.2, 190, 130, 650, 1050, 0.02), 0.005, 0.07, 0.8),
                env(sine(0.1, 110, 60), 0.001, 0.04, 0.6));
        sfx[DEATH] = mix(env(voice(0.6, 170, 80, 600, 950, 0.03), 0.01, 0.2, 0.8),
                env(lowpass(noise(0.5), 600), 0.01, 0.15, 0.5), env(sine(0.4, 90, 40), 0.001, 0.15, 0.7));
        float[] sq = buf(0.16);
        double ph = 0;
        for (int i = 0; i < sq.length; i++) {
            double t = i / (double) RATE;
            ph += (320 * Math.exp(-t * 9) + 90 + 60 * Math.sin(t * 2 * Math.PI * 38)) / RATE;
            sq[i] = (float) Math.sin(2 * Math.PI * ph);
        }
        sfx[SQUISH] = mix(env(sq, 0.002, 0.05, 0.6), env(lowpass(noise(0.12), 800), 0.002, 0.04, 0.8));
        float[] sd = buf(0.3);
        ph = 0;
        for (int i = 0; i < sd.length; i++) {
            double t = i / (double) RATE;
            ph += (260 * Math.exp(-t * 6) + 70 + 50 * Math.sin(t * 2 * Math.PI * 30)) / RATE;
            sd[i] = (float) Math.sin(2 * Math.PI * ph);
        }
        sfx[SLIME_DIE] = mix(env(sd, 0.002, 0.1, 0.6), env(crackle(lowpass(noise(0.25), 1000), 0.01), 0.002, 0.08, 1.0));
        sfx[BONE] = seq(0.03, env(bandpass(noise(0.04), 1300, 8), 0.0005, 0.012, 4.0),
                env(bandpass(noise(0.05), 900, 8), 0.0005, 0.015, 3.5));
        sfx[HIT] = mix(env(sine(0.1, 170, 80), 0.001, 0.035, 0.8), env(lowpass(noise(0.07), 1200), 0.001, 0.02, 0.9));
        sfx[KILL] = mix(env(sine(0.2, 150, 50), 0.001, 0.07, 0.8), env(crackle(lowpass(noise(0.2), 900), 0.015), 0.002, 0.06, 1.0));
        sfx[ARROW_HIT] = mix(env(bandpass(noise(0.05), 800, 4), 0.0005, 0.012, 2.5), env(sine(0.06, 300, 250), 0.001, 0.02, 0.3));
        float[] twang = buf(0.25);
        {
            int per = RATE / 190;
            float[] ks = new float[per];
            for (int i = 0; i < per; i++) ks[i] = rnd.nextFloat() * 2 - 1;
            for (int i = 0; i < twang.length; i++) {
                int k = i % per, k2 = (i + 1) % per;
                float v = ks[k];
                ks[k] = (ks[k] + ks[k2]) * 0.497f;
                twang[i] = v;
            }
        }
        sfx[BOW] = mix(env(twang, 0.001, 0.07, 0.6), env(bandpass(noise(0.1), 1200, 1.2), 0.005, 0.03, 0.5));
        sfx[COIN] = mix(env(fm(0.3, 2100, 3.5, 0.9, 0.05), 0.001, 0.09, 0.3), env(fm(0.3, 3150, 2.0, 0.5, 0.05), 0.001, 0.07, 0.15));
        float[] gulp = seq(0.07, env(sine(0.05, 220, 330), 0.005, 0.02, 0.5), env(sine(0.05, 200, 300), 0.005, 0.02, 0.5));
        float[] shimmer = seq(0.06, env(fm(0.5, 1319, 3.5, 0.6, 0.1), 0.002, 0.15, 0.18),
                env(fm(0.5, 1568, 3.5, 0.6, 0.1), 0.002, 0.15, 0.18), env(fm(0.6, 2093, 3.5, 0.6, 0.1), 0.002, 0.2, 0.18));
        sfx[HEAL] = mix(gulp, seq(0.12, new float[1], shimmer));
        sfx[CRYSTAL] = seq(0.09, env(fm(0.9, 1047, 3.5, 1.0, 0.2), 0.002, 0.35, 0.28),
                env(fm(0.9, 1319, 3.5, 1.0, 0.2), 0.002, 0.35, 0.28), env(fm(0.9, 1568, 3.5, 1.0, 0.2), 0.002, 0.35, 0.28),
                env(fm(1.2, 2093, 3.5, 1.0, 0.2), 0.002, 0.5, 0.3));
        sfx[ZOMBIE] = env(voice(0.8, 110, 85, 500, 820, 0.06), 0.12, 0.35, 0.55);
        float[] glass = new float[1];
        for (int k = 0; k < 4; k++) {
            double f = 2400 + rnd.nextDouble() * 2600;
            glass = mix(glass, seq(0.012 * k, new float[1], env(sine(0.2, f, f * 0.98), 0.0005, 0.05, 0.12)));
        }
        sfx[GLASS] = mix(glass, env(highpass(noise(0.1), 3500), 0.0005, 0.02, 0.6));
        sfx[METAL] = mix(env(fm(0.6, 980, 2.76, 2.0, 0.08), 0.0005, 0.18, 0.35), env(bandpass(noise(0.05), 3000, 3), 0.0005, 0.01, 1.2));
        sfx[POT] = mix(env(crackle(bandpass(noise(0.18), 2000, 1.5), 0.008), 0.001, 0.05, 2.0),
                env(sine(0.12, 180, 90), 0.001, 0.04, 0.5));
        sfx[HISS] = env(crackle(highpass(noise(0.6), 3000), 0.003), 0.02, 0.2, 0.8);
        float[] sp = noise(0.35);
        double yy = 0;
        for (int i = 0; i < sp.length; i++) {
            double t = i / (double) sp.length;
            double hz = 2500 - 1800 * t;
            yy += (sp[i] - yy) * (1 - Math.exp(-2 * Math.PI * hz / RATE));
            sp[i] = (float) yy;
        }
        sfx[SPLASH] = mix(env(crackle(sp, 0.006), 0.005, 0.1, 1.6), env(sine(0.08, 300, 150), 0.001, 0.03, 0.3));
        sfx[DOOR] = mix(env(voice(0.18, 260, 180, 700, 1500, 0.1), 0.01, 0.06, 0.8),
                seq(0.14, new float[1], env(bandpass(noise(0.08), 400, 3), 0.001, 0.02, 5.0)));
        sfx[CHEST] = mix(env(voice(0.15, 320, 220, 900, 1800, 0.08), 0.01, 0.05, 0.7),
                env(bandpass(noise(0.06), 600, 4), 0.001, 0.02, 4.0));
        sfx[SPELL] = mix(env(fm(0.4, 660, 1.5, 3.0, 0.15), 0.01, 0.12, 0.3), env(bandpass(noise(0.3), 1500, 2), 0.02, 0.1, 0.6));
        // version 0.4
        sfx[ROAR] = mix(env(voice(1.4, 125, 52, 360, 720, 0.14), 0.08, 0.55, 0.9),
                env(crackle(lowpass(noise(1.3), 320), 0.02), 0.12, 0.45, 0.9), env(sine(1.1, 72, 38), 0.05, 0.45, 0.6));
        sfx[THUNDER] = mix(env(highpass(noise(0.12), 2200), 0.0005, 0.025, 0.9),
                env(crackle(lowpass(noise(1.5), 420), 0.03), 0.01, 0.45, 1.0), env(sine(0.9, 62, 30), 0.004, 0.3, 0.45));
        sfx[STAR] = seq(0.05, env(fm(0.5, 2637, 3.5, 0.6, 0.1), 0.001, 0.15, 0.18),
                env(fm(0.5, 3136, 3.5, 0.6, 0.1), 0.001, 0.15, 0.18), env(fm(0.7, 3951, 3.5, 0.6, 0.1), 0.001, 0.25, 0.18));
        sfx[ZAP] = mix(env(sine(0.18, 480, 1500), 0.003, 0.06, 0.3), env(fm(0.2, 900, 2.0, 2.0, 0.05), 0.002, 0.05, 0.15),
                env(bandpass(noise(0.2), 2600, 1.5), 0.01, 0.06, 0.6));
        float[] ice = new float[1];
        for (int k = 0; k < 6; k++) {
            double f = 3000 + rnd.nextDouble() * 3500;
            ice = mix(ice, seq(0.018 * k, new float[1], env(sine(0.15, f, f * 1.01), 0.0005, 0.03, 0.1)));
        }
        sfx[FREEZE] = mix(ice, env(crackle(highpass(noise(0.25), 3000), 0.006), 0.002, 0.06, 0.7));
        sfx[MIRROR] = mix(seq(0.07, env(fm(0.6, 1047, 3.5, 0.5, 0.1), 0.002, 0.2, 0.16), env(fm(0.6, 1319, 3.5, 0.5, 0.1), 0.002, 0.2, 0.16),
                env(fm(0.6, 1568, 3.5, 0.5, 0.1), 0.002, 0.2, 0.16), env(fm(0.8, 2093, 3.5, 0.5, 0.1), 0.002, 0.3, 0.16)),
                env(bandpass(noise(0.6), 3000, 1.0), 0.15, 0.2, 0.35));
        creatureSounds();
    }

    /** Voices of the creatures: hurt sounds and the odd noise they make now and then. */
    private void creatureSounds() {
        float[] chirp = buf(0.07);
        double ph = 0;
        for (int i = 0; i < chirp.length; i++) {
            double t = i / (double) chirp.length;
            ph += (2600 + 1500 * Math.sin(Math.PI * t)) / RATE;
            chirp[i] = (float) Math.sin(2 * Math.PI * ph);
        }
        sfx[SQUEAK] = seq(0.055, env(chirp.clone(), 0.002, 0.025, 0.3), env(chirp, 0.002, 0.02, 0.22));
        float[] bz = buf(0.35);
        ph = 0;
        for (int i = 0; i < bz.length; i++) {
            double t = i / (double) RATE;
            ph = (ph + (160 + 12 * Math.sin(t * 9)) / RATE) % 1.0;
            bz[i] = (float) ((ph * 2 - 1) * (0.65 + 0.35 * Math.sin(2 * Math.PI * 31 * t)));
        }
        sfx[BUZZ] = env(bandpass(bz, 950, 1.6), 0.03, 0.15, 0.9);
        sfx[GROWL] = mix(env(voice(0.7, 92, 62, 330, 680, 0.2), 0.05, 0.28, 0.75),
                env(crackle(lowpass(noise(0.6), 500), 0.025), 0.05, 0.2, 0.5));
        sfx[SQUELCH] = mix(env(lowpass(crackle(noise(0.18), 0.007), 1300), 0.002, 0.05, 1.3),
                env(sine(0.14, 270, 110), 0.002, 0.045, 0.5));
        sfx[OOF] = env(voice(0.17, 235, 165, 760, 1350, 0.02), 0.004, 0.06, 0.75);
        sfx[RATTLE] = seq(0.05, env(bandpass(noise(0.03), 1500, 8), 0.0005, 0.01, 3.0),
                env(bandpass(noise(0.03), 1100, 8), 0.0005, 0.01, 2.6), env(bandpass(noise(0.03), 1700, 8), 0.0005, 0.01, 2.4),
                env(bandpass(noise(0.03), 1250, 8), 0.0005, 0.012, 2.0));
        sfx[BLUB] = mix(env(sine(0.12, 430, 170), 0.003, 0.04, 0.4), env(lowpass(noise(0.05), 700), 0.002, 0.015, 0.25));
        sfx[SQUAWK] = env(voice(0.2, 540, 390, 950, 1900, 0.06), 0.006, 0.07, 0.55);
    }

    // ---- tests --------------------------------------------------------------

    /** Renders music without an audio device (for tests). */
    float[][] renderMusicForTest(int id, double seconds) {
        music.request(id);
        int n = (int) (seconds * RATE);
        float[] l = new float[n], r = new float[n];
        int block = 512;
        float[] bl = new float[block], br = new float[block];
        for (int i = 0; i < n; i += block) {
            int m = Math.min(block, n - i);
            java.util.Arrays.fill(bl, 0);
            java.util.Arrays.fill(br, 0);
            music.render(bl, br, m, 1.0);
            System.arraycopy(bl, 0, l, i, m);
            System.arraycopy(br, 0, r, i, m);
        }
        return new float[][]{l, r};
    }

    float[] sfxForTest(int id) {
        return sfx[id];
    }
}
