package mingeriacc;

/**
 * Software instruments for the music: plucked strings (Karplus-Strong), an
 * FM electric piano and bells, soft pads, a breathy flute, bass and gentle
 * percussion, plus a stereo reverb and an echo. Everything is synthesized.
 */
final class Synth {
    private Synth() {}

    static final int RATE = 44100;

    static final int PLUCK = 0, EPIANO = 1, PAD = 2, FLUTE = 3, BELL = 4, BASS = 5, STRINGS = 6, MARIMBA = 7,
            KICK = 8, SHAKER = 9, BRUSH = 10, TOM = 11, RIM = 12, BOOM = 13, HARP = 14, CHOIR = 15, PLUCK_BASS = 16;

    static final int TABLE = 2048;
    static final float[] SINE = table(1);
    static final float[] SOFTSAW = sawTable(24, 1.0, 0.9);
    static final float[] WARM = sawTable(16, 1.25, 0.93);
    static final float[] FLUTE_T = table(1, 0.32, 0.11, 0.05, 0.02);
    static final float[] BASS_T = table(1, 0.3, 0.1, 0.03);
    static final float[] CHOIR_T = table(0.7, 0.45, 0.55, 0.35, 0.2, 0.1, 0.06, 0.03);

    static float[] table(double... amps) {
        float[] t = new float[TABLE];
        double peak = 0;
        double[] v = new double[TABLE];
        for (int i = 0; i < TABLE; i++) {
            double x = 2 * Math.PI * i / TABLE;
            double s = 0;
            for (int h = 0; h < amps.length; h++) s += amps[h] * Math.sin(x * (h + 1));
            v[i] = s;
            peak = Math.max(peak, Math.abs(s));
        }
        for (int i = 0; i < TABLE; i++) t[i] = (float) (v[i] / peak);
        return t;
    }

    static float[] sawTable(int harmonics, double power, double rolloff) {
        double[] a = new double[harmonics];
        for (int h = 0; h < harmonics; h++) a[h] = Math.pow(rolloff, h) / Math.pow(h + 1, power);
        return table(a);
    }

    static double tab(float[] t, double ph) {
        double idx = ph * TABLE;
        int i = (int) idx;
        double f = idx - i;
        i &= TABLE - 1;
        return t[i] + (t[(i + 1) & (TABLE - 1)] - t[i]) * f;
    }

    static double midiFreq(double midi) {
        return 440.0 * Math.pow(2, (midi - 69) / 12.0);
    }

    /** One sounding note. */
    static final class Voice {
        boolean active;
        int inst;
        double freq, vel, pan;
        double rev, echo;          // effect sends
        long age, len, delay;      // in samples
        double ph0, ph1, ph2, ph3;
        double lp, lp2, hp;
        final float[] ks = new float[2400];
        int ksN, ksI;
        double ksDamp, ksBright;
        int noise = 0x1234567;
        double relTau = 0.1;

        private double rnd() {
            noise ^= noise << 13;
            noise ^= noise >>> 17;
            noise ^= noise << 5;
            return (noise & 0xffffff) / (double) 0x800000 - 1;
        }

        void start(int inst, double freq, double vel, double pan, long lenSamples, long delay, int seed) {
            this.active = true;
            this.inst = inst;
            this.freq = freq;
            this.vel = vel;
            this.pan = pan;
            this.len = lenSamples;
            this.delay = delay;
            this.age = 0;
            ph0 = ph1 = ph2 = ph3 = 0;
            lp = lp2 = hp = 0;
            noise = seed | 1;
            switch (inst) {
                case PLUCK: case HARP: case PLUCK_BASS: {
                    ksN = Math.max(2, Math.min(ks.length - 1, (int) Math.round(RATE / freq)));
                    ksI = 0;
                    double decay = inst == PLUCK_BASS ? 1.8 : inst == HARP ? 3.5 : 2.6;
                    ksDamp = Math.pow(0.001, 1.0 / (freq * decay));
                    ksBright = inst == HARP ? 0.62 : inst == PLUCK_BASS ? 0.5 : 0.56;
                    double smooth = inst == HARP ? 0.55 : inst == PLUCK_BASS ? 0.2 : 0.4;
                    double y = 0;
                    for (int i = 0; i < ksN; i++) {
                        y += (rnd() - y) * smooth;
                        ks[i] = (float) y;
                    }
                    // remove the offset so the string does not thump
                    double mean = 0;
                    for (int i = 0; i < ksN; i++) mean += ks[i];
                    mean /= ksN;
                    for (int i = 0; i < ksN; i++) ks[i] = (float) ((ks[i] - mean) * 2.2);
                    relTau = 0.12;
                    break;
                }
                case PAD: case CHOIR: relTau = 0.9; break;
                case STRINGS: relTau = 0.45; break;
                case FLUTE: relTau = 0.1; break;
                case BASS: relTau = 0.08; break;
                case EPIANO: relTau = 0.35; break;
                default: relTau = 0.3;
            }
        }

        /** Next sample (mono, before panning). Sets active = false when done. */
        double next() {
            if (delay > 0) {
                delay--;
                return 0;
            }
            double t = age / (double) RATE;
            double rel = 1;
            if (age > len) {
                rel = Math.exp(-(age - len) / (double) RATE / relTau);
                if (rel < 0.0008) {
                    active = false;
                    return 0;
                }
            }
            age++;
            double f = freq / RATE;
            double v;
            switch (inst) {
                case PLUCK: case HARP: case PLUCK_BASS: {
                    int j = ksI + 1;
                    if (j >= ksN) j = 0;
                    float cur = ks[ksI];
                    ks[ksI] = (float) ((cur * ksBright + ks[j] * (1 - ksBright)) * ksDamp);
                    ksI = j;
                    v = cur * rel;
                    if (t > 6) active = false;
                    break;
                }
                case EPIANO: {
                    double mi = 1.3 * Math.exp(-t / 0.28) + 0.22;
                    ph0 += f;
                    ph2 += f * 4;
                    v = Math.sin(2 * Math.PI * ph0 + mi * Math.sin(2 * Math.PI * ph0))
                            + 0.05 * Math.sin(2 * Math.PI * ph2) * Math.exp(-t / 0.1);
                    v *= Math.min(1, t / 0.004) * Math.exp(-t / 1.7) * rel * 0.8;
                    if (t > 8) active = false;
                    break;
                }
                case BELL: {
                    double mi = 1.1 * Math.exp(-t / 0.22);
                    ph0 += f;
                    ph1 += f * 3.5;
                    v = Math.sin(2 * Math.PI * ph0 + mi * Math.sin(2 * Math.PI * ph1));
                    v *= Math.min(1, t / 0.002) * Math.exp(-t / 1.2) * rel * 0.7;
                    if (t > 7) active = false;
                    break;
                }
                case MARIMBA: {
                    ph0 += f;
                    ph2 += f * 4;
                    v = Math.sin(2 * Math.PI * ph0) + 0.35 * Math.sin(2 * Math.PI * ph2) * Math.exp(-t / 0.04);
                    v *= Math.min(1, t / 0.002) * Math.exp(-t / 0.42) * rel * 0.8;
                    if (t > 3) active = false;
                    break;
                }
                case PAD: case CHOIR: {
                    float[] tb = inst == PAD ? SOFTSAW : CHOIR_T;
                    double vib = 1 + 0.0025 * Math.sin(2 * Math.PI * 4.7 * t);
                    ph0 += f * 0.9963 * vib;
                    ph1 += f * vib;
                    ph2 += f * 1.0042 * vib;
                    double raw = (tab(tb, ph0) + tab(tb, ph1) + tab(tb, ph2)) / 3;
                    lp += (raw - lp) * 0.12;
                    lp2 += (lp - lp2) * 0.2;
                    double a = Math.min(1, t / 1.1);
                    v = lp2 * a * a * (3 - 2 * a) * rel;
                    break;
                }
                case STRINGS: {
                    double vib = 1 + 0.003 * Math.sin(2 * Math.PI * 5.2 * t) * Math.min(1, t / 0.5);
                    ph0 += f * 0.998 * vib;
                    ph1 += f * 1.002 * vib;
                    double raw = (tab(WARM, ph0) + tab(WARM, ph1)) * 0.5;
                    lp += (raw - lp) * 0.2;
                    double a = Math.min(1, t / 0.28);
                    v = lp * a * rel;
                    break;
                }
                case FLUTE: {
                    double depth = 0.0045 * Math.min(1, Math.max(0, (t - 0.25) / 0.4));
                    ph0 += f * (1 + depth * Math.sin(2 * Math.PI * 5.1 * t));
                    double breath = rnd();
                    lp += (breath - lp) * 0.08;
                    double a = Math.min(1, t / 0.07);
                    double swell = 0.85 + 0.15 * Math.min(1, t / 0.5);
                    v = (tab(FLUTE_T, ph0) * swell + lp * (0.12 + 0.3 * Math.exp(-t / 0.08))) * a * rel;
                    break;
                }
                case BASS: {
                    ph0 += f;
                    double raw = tab(BASS_T, ph0);
                    lp += (raw - lp) * 0.25;
                    double a = Math.min(1, t / 0.008);
                    v = lp * a * (0.62 + 0.38 * Math.exp(-t / 0.35)) * rel * 1.3;
                    break;
                }
                case KICK: {
                    double fk = 44 + 72 * Math.exp(-t / 0.028);
                    ph0 += fk / RATE;
                    v = Math.sin(2 * Math.PI * ph0) * Math.exp(-t / 0.16);
                    if (t > 0.8) active = false;
                    break;
                }
                case SHAKER: {
                    double n = rnd();
                    lp += (n - lp) * 0.45;
                    v = (n - lp) * Math.min(1, t / 0.004) * Math.exp(-t / 0.032) * 0.5;
                    if (t > 0.3) active = false;
                    break;
                }
                case BRUSH: {
                    double n = rnd();
                    lp += (n - lp) * 0.35;
                    lp2 += (lp - lp2) * 0.35;
                    v = (lp - lp2) * Math.min(1, t / 0.006) * Math.exp(-t / 0.11) * 0.9;
                    if (t > 0.6) active = false;
                    break;
                }
                case TOM: {
                    double ft = freq * (1 + 0.6 * Math.exp(-t / 0.04));
                    ph0 += ft / RATE;
                    v = Math.sin(2 * Math.PI * ph0) * Math.exp(-t / 0.24);
                    if (t > 1.2) active = false;
                    break;
                }
                case RIM: {
                    ph0 += 1650.0 / RATE;
                    v = Math.sin(2 * Math.PI * ph0) * Math.exp(-t / 0.012) + rnd() * Math.exp(-t / 0.004) * 0.3;
                    if (t > 0.2) active = false;
                    break;
                }
                case BOOM: {
                    double fb = 36 + 28 * Math.exp(-t / 0.09);
                    ph0 += fb / RATE;
                    double n = rnd();
                    lp += (n - lp) * 0.02;
                    v = (Math.sin(2 * Math.PI * ph0) + lp * 2.5 * Math.exp(-t / 0.3)) * Math.exp(-t / 0.9);
                    if (t > 3) active = false;
                    break;
                }
                default:
                    v = 0;
                    active = false;
            }
            return v * vel;
        }
    }

    /** Stereo reverb (Freeverb style: parallel combs and series all-passes). */
    static final class Reverb {
        private static final int[] COMB = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
        private static final int[] ALLPASS = {556, 441, 341, 225};
        private final float[][] cL = new float[8][], cR = new float[8][], aL = new float[4][], aR = new float[4][];
        private final int[] ciL = new int[8], ciR = new int[8], aiL = new int[4], aiR = new int[4];
        private final float[] fsL = new float[8], fsR = new float[8];
        float feedback = 0.9f, damp = 0.3f;
        float outL, outR;

        Reverb() {
            for (int i = 0; i < 8; i++) {
                cL[i] = new float[COMB[i]];
                cR[i] = new float[COMB[i] + 23];
            }
            for (int i = 0; i < 4; i++) {
                aL[i] = new float[ALLPASS[i]];
                aR[i] = new float[ALLPASS[i] + 23];
            }
        }

        void process(float inL, float inR) {
            float in = (inL + inR) * 0.015f;
            float l = 0, r = 0;
            for (int i = 0; i < 8; i++) {
                float[] b = cL[i];
                float o = b[ciL[i]];
                fsL[i] = o * (1 - damp) + fsL[i] * damp;
                b[ciL[i]] = in + fsL[i] * feedback;
                if (++ciL[i] >= b.length) ciL[i] = 0;
                l += o;
                b = cR[i];
                o = b[ciR[i]];
                fsR[i] = o * (1 - damp) + fsR[i] * damp;
                b[ciR[i]] = in + fsR[i] * feedback;
                if (++ciR[i] >= b.length) ciR[i] = 0;
                r += o;
            }
            for (int i = 0; i < 4; i++) {
                float[] b = aL[i];
                float bo = b[aiL[i]];
                b[aiL[i]] = l + bo * 0.5f;
                l = bo - l;
                if (++aiL[i] >= b.length) aiL[i] = 0;
                b = aR[i];
                bo = b[aiR[i]];
                b[aiR[i]] = r + bo * 0.5f;
                r = bo - r;
                if (++aiR[i] >= b.length) aiR[i] = 0;
            }
            outL = l;
            outR = r;
        }
    }

    /** Stereo ping-pong echo with a soft feedback filter. */
    static final class Echo {
        private final float[] bl = new float[RATE * 2], br = new float[RATE * 2];
        private int pos;
        int time = RATE / 3;
        float feedback = 0.38f;
        private float fl, fr;
        float outL, outR;

        void process(float inL, float inR) {
            int rp = pos - time;
            if (rp < 0) rp += bl.length;
            float dl = bl[rp], dr = br[rp];
            fl += (dl - fl) * 0.4f;
            fr += (dr - fr) * 0.4f;
            bl[pos] = inL + fr * feedback;
            br[pos] = inR + fl * feedback;
            if (++pos >= bl.length) pos = 0;
            outL = dl;
            outR = dr;
        }
    }
}
