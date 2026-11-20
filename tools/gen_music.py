"""
jsn-Phone の音楽アプリの曲を、ゼロから作る（著作権フリー：すべてこのプログラムで合成したオリジナル）。
  python3 tools/gen_music.py  →  src/main/resources/assets/jsn_phone/sounds/music/*.ogg
"""
import json, os, subprocess, sys, wave
import numpy as np
from scipy.signal import fftconvolve

SR = 32000
OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/jsn_phone/sounds/music')
rng_master = np.random.default_rng(7)

# ---------------- 音のもと ----------------
def mtof(m): return 440.0 * 2 ** ((m - 69) / 12)

def env(n, a=0.005, d=0.1, s=0.7, r=0.05, dur=None):
    """ADSR。n はサンプル数（release 込み）"""
    t = np.arange(n) / SR
    a = max(a, 1e-4)
    e = np.ones(n) * s
    e = np.where(t < a, t / a, e)
    dm = (t >= a) & (t < a + d)
    e = np.where(dm, 1 - (1 - s) * (t - a) / max(d, 1e-4), e)
    if dur is not None:
        rm = t >= dur
        e = np.where(rm, e * np.exp(-(t - dur) / max(r, 1e-4) * 3), e)
    return e

def osc_square(f, n, duty=0.5):
    ph = (np.arange(n) * f / SR) % 1.0
    return np.where(ph < duty, 1.0, -1.0)

def osc_tri(f, n):
    ph = (np.arange(n) * f / SR) % 1.0
    return 4 * np.abs(ph - 0.5) - 1

def osc_saw(f, n, phase=0.0):
    ph = (np.arange(n) * f / SR + phase) % 1.0
    return 2 * ph - 1

def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    # 1次のローパス（scipy の lfilter）
    from scipy.signal import lfilter
    return lfilter([1 - a], [1, -a], x)

def highpass(x, cutoff):
    return x - lowpass(x, cutoff)

def inst(name, f, dur, vel=1.0):
    """楽器ごとに音を作る。dur は鳴らす長さ（秒）。返すのはリリース込みの配列"""
    rel = {'pad': 0.6, 'bell': 2.5, 'box': 2.0, 'pluck': 0.8, 'ep': 0.8, 'nylon': 1.2, 'strings': 0.5, 'piano': 1.2}.get(name, 0.06)
    n = int((dur + rel) * SR)
    t = np.arange(n) / SR
    if name == 'sq':
        x = osc_square(f, n, 0.5) * 0.5 * env(n, 0.003, 0.08, 0.6, 0.04, dur)
    elif name == 'sq25':
        x = osc_square(f, n, 0.25) * 0.5 * env(n, 0.003, 0.1, 0.55, 0.04, dur)
        x *= 1 + 0.003 * np.sin(2 * np.pi * 5.5 * t)
    elif name == 'tri':
        x = osc_tri(f, n) * 0.8 * env(n, 0.003, 0.05, 0.9, 0.03, dur)
    elif name == 'bass':   # やわらかいベース
        x = (np.sin(2 * np.pi * f * t) + 0.3 * osc_tri(f, n) + 0.15 * np.sin(4 * np.pi * f * t)) * 0.7
        x *= env(n, 0.005, 0.2, 0.6, 0.06, dur)
    elif name == 'pad':
        x = sum(osc_saw(f * k, n, p) for k, p in ((1, 0), (1.004, .3), (0.996, .6), (2.002, .1))) * 0.18
        x = lowpass(x, 1400)
        x *= env(n, 0.35, 0.3, 0.8, 0.6, dur)
    elif name == 'strings':
        vib = 1 + 0.004 * np.sin(2 * np.pi * 5 * t) * np.clip(t * 2, 0, 1)
        ph = np.cumsum(f * vib / SR)
        x = sum((2 * ((ph * k + o) % 1) - 1) / k for k, o in ((1, 0), (1.003, .5), (2, .2))) * 0.2
        x = lowpass(x, 2600)
        x *= env(n, 0.12, 0.2, 0.85, 0.5, dur)
    elif name in ('pluck', 'nylon'):
        # 弦をはじいた音：倍音ほど早く消える
        bright = 1.0 if name == 'pluck' else 0.6
        x = np.zeros(n)
        for k in range(1, 12):
            if f * k > SR / 2.2: break
            amp = (1 / k ** (1.2 if name == 'pluck' else 1.6)) * bright ** (k - 1)
            dec = (1.6 if name == 'nylon' else 2.2) + k * (0.9 if name == 'nylon' else 1.4)
            x += amp * np.sin(2 * np.pi * f * k * t + k) * np.exp(-t * dec)
        x *= 0.6 * np.clip(t / 0.002, 0, 1)
        x *= np.where(t > dur + rel * 0.6, np.exp(-(t - dur - rel * 0.6) * 12), 1)
    elif name == 'ep':     # エレピ（FM）
        idx = 2.2 * np.exp(-t * 3) + 0.3
        x = np.sin(2 * np.pi * f * t + idx * np.sin(2 * np.pi * f * t)) * np.exp(-t * 1.4) * 0.6
        x += 0.15 * np.sin(2 * np.pi * f * 14 * t) * np.exp(-t * 30)   # 叩いた瞬間のキン
        x *= np.where(t > dur, np.exp(-(t - dur) * 8), 1)
    elif name == 'piano':
        x = np.zeros(n)
        for k, a in ((1, 1), (2, .5), (3, .25), (4, .15), (5, .08), (6, .05)):
            x += a * np.sin(2 * np.pi * f * k * 1.0005 ** k * t) * np.exp(-t * (1.2 + k * 0.8))
        x *= 0.45 * np.clip(t / 0.003, 0, 1)
        x *= np.where(t > dur, np.exp(-(t - dur) * 6), 1)
    elif name in ('bell', 'box'):
        parts = ((1, 1, 1.2), (2.76, .35, 3), (5.4, .15, 6), (8.93, .07, 9)) if name == 'box' else ((1, 1, .8), (3.5, .4, 2.5), (2, .3, 1.5))
        x = sum(a * np.sin(2 * np.pi * f * r * t) * np.exp(-t * d) for r, a, d in parts) * 0.4
        x *= np.clip(t / 0.001, 0, 1)
    else:
        raise ValueError(name)
    return x * vel

def drum(name, vel=1.0):
    n = int(0.5 * SR)
    t = np.arange(n) / SR
    r = rng_master
    if name == 'kick':
        f = 45 + 110 * np.exp(-t * 30)
        x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 7) * 1.0
        x += 0.3 * r.standard_normal(n) * np.exp(-t * 300)
    elif name == 'snare':
        x = highpass(r.standard_normal(n), 900) * np.exp(-t * 18) * 0.5 + np.sin(2 * np.pi * 190 * t) * np.exp(-t * 25) * 0.4
    elif name == 'clap':
        e = sum(np.exp(-np.clip(t - d, 0, None) * 60) * (t >= d) for d in (0, .01, .02)) / 2
        x = highpass(r.standard_normal(n), 1200) * e * 0.5
    elif name == 'hat':
        x = highpass(r.standard_normal(n), 7000) * np.exp(-t * 70) * 0.35
    elif name == 'ohat':
        x = highpass(r.standard_normal(n), 6000) * np.exp(-t * 12) * 0.25
    elif name == 'brush':
        x = highpass(r.standard_normal(n), 3000) * np.exp(-t * 25) * 0.18
    elif name == 'shaker':
        x = highpass(r.standard_normal(n), 5000) * np.exp(-((t - 0.02) / 0.015) ** 2) * 0.18
    elif name == 'noise_snare':   # ファミコン風
        x = np.sign(r.standard_normal(n)) * np.exp(-t * 20) * 0.3
    elif name == 'chip_kick':
        f = 60 + 200 * np.exp(-t * 40)
        x = osc_square(1, n) * 0
        x = np.sign(np.sin(2 * np.pi * np.cumsum(f) / SR)) * np.exp(-t * 18) * 0.45
    elif name == 'rim':
        x = np.sin(2 * np.pi * 1700 * t) * np.exp(-t * 90) * 0.3
    else:
        raise ValueError(name)
    return x * vel

# ---------------- 曲の入れ物 ----------------
class Song:
    def __init__(self, bpm, bars, beats=4, swing=0.0):
        self.bpm, self.beats, self.swing = bpm, beats, swing
        self.beat = 60.0 / bpm
        self.length = bars * beats * self.beat
        self.tracks = {}

    def t(self, bar, beat):
        """小節と拍 → 秒（スウィングは8分の裏をうしろへ）"""
        b = beat
        frac = b % 1
        if self.swing and abs(frac - 0.5) < 1e-6:
            b += self.swing * 0.5
        return (bar * self.beats + b) * self.beat

    def bus(self, name):
        if name not in self.tracks:
            self.tracks[name] = np.zeros(int((self.length + 4) * SR))
        return self.tracks[name]

    def add(self, busname, x, at, gain=1.0, pan=0.0):
        b = self.bus(busname)
        i = max(0, int(at * SR))
        if i >= len(b): return
        m = min(len(x), len(b) - i)
        b[i:i + m] += x[:m] * gain

    def note(self, busname, name, midi, bar, beat, beats, vel=1.0, gain=1.0):
        x = inst(name, mtof(midi), beats * self.beat * 0.95, vel)
        self.add(busname, x, self.t(bar, beat) + rng_master.normal(0, 0.003), gain)

    def hit(self, busname, name, bar, beat, vel=1.0, gain=1.0):
        self.add(busname, drum(name, vel), self.t(bar, beat), gain)

def reverb(x, secs=1.8, mix=0.25, seed=1, damp=4000):
    r = np.random.default_rng(seed)
    n = int(secs * SR)
    t = np.arange(n) / SR
    ir = r.standard_normal(n) * np.exp(-t * 6.9 / secs)
    ir = lowpass(ir, damp)
    ir[:int(0.012 * SR)] = 0
    ir /= np.sqrt(np.sum(ir ** 2))
    wet = fftconvolve(x, ir)[:len(x)]
    return x * (1 - mix) + wet * mix * 1.4

def master(song, sends, out, fade_in=0.3, fade_out=3.0):
    mix = np.zeros(int((song.length + 4) * SR))
    for name, b in song.tracks.items():
        g, rv = sends.get(name, (1.0, 0.0))
        y = b * g
        if rv > 0: y = reverb(y, 2.0, rv, seed=hash(name) % 100)
        mix[:len(y)] += y
    n = int((song.length + 2.5) * SR)
    mix = mix[:n]
    mix /= np.max(np.abs(mix)) + 1e-9
    mix = np.tanh(mix * 1.3) / np.tanh(1.3) * 0.9
    fi, fo = int(fade_in * SR), int(fade_out * SR)
    mix[:fi] *= np.linspace(0, 1, fi)
    mix[-fo:] *= np.linspace(1, 0, fo) ** 1.5
    pcm = (mix * 32767).astype(np.int16)
    os.makedirs(OUT, exist_ok=True)
    wav = os.path.join(OUT, out + '.wav')
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    ogg = os.path.join(OUT, out + '.ogg')
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '2.5', ogg], check=True)
    os.remove(wav)
    return len(pcm) / SR

# ---------------- 和音とメロディ ----------------
NOTE = {'C': 0, 'C#': 1, 'Db': 1, 'D': 2, 'D#': 3, 'Eb': 3, 'E': 4, 'F': 5, 'F#': 6, 'Gb': 6, 'G': 7, 'G#': 8, 'Ab': 8, 'A': 9, 'A#': 10, 'Bb': 10, 'B': 11}
QUAL = {'': (0, 4, 7), 'm': (0, 3, 7), '7': (0, 4, 7, 10), 'm7': (0, 3, 7, 10), 'maj7': (0, 4, 7, 11), 'm7b5': (0, 3, 6, 10),
        '9': (0, 4, 7, 10, 14), 'maj9': (0, 4, 7, 11, 14), 'm9': (0, 3, 7, 10, 14), 'sus4': (0, 5, 7), 'add9': (0, 4, 7, 14), 'dim': (0, 3, 6)}

def chord(sym, octave=4):
    root = sym[:2] if len(sym) > 1 and sym[1] in '#b' else sym[:1]
    q = sym[len(root):]
    r = 12 * (octave + 1) + NOTE[root]
    return r, [r + i for i in QUAL[q]]

SCALES = {'major': (0, 2, 4, 5, 7, 9, 11), 'minor': (0, 2, 3, 5, 7, 8, 10), 'dorian': (0, 2, 3, 5, 7, 9, 10),
          'penta': (0, 2, 4, 7, 9), 'mpenta': (0, 3, 5, 7, 10), 'harm': (0, 2, 3, 5, 7, 8, 11)}

def scale_notes(key, kind, lo=48, hi=96):
    k = NOTE[key]
    return [m for m in range(lo, hi) if (m - k) % 12 in SCALES[kind]]

def voicing(sym, center=60):
    """真ん中あたりにまとめた和音"""
    _, notes = chord(sym, 4)
    out = []
    for m in notes:
        while m - center > 6: m -= 12
        while center - m > 6: m += 12
        out.append(m)
    return sorted(set(out))

class Melody:
    """モチーフ（2小節のリズムと形）をくり返して、和音に合わせて動かす"""
    def __init__(self, seed, key, kind, lo, hi, density=0.6, beats=4):
        self.r = np.random.default_rng(seed)
        self.scale = scale_notes(key, kind, lo, hi)
        self.density, self.beats = density, beats
        self.prev = self.scale[len(self.scale) // 2]

    def rhythm(self, bars=2, grid=0.5):
        """拍の位置と長さのリスト"""
        r = self.r
        out = []
        b = 0.0
        total = bars * self.beats
        while b < total - 1e-6:
            strong = abs(b % 1) < 1e-6
            if r.random() < (self.density if strong else self.density * 0.7):
                ln = r.choice([grid, grid, grid * 2, grid * 2, grid * 3, grid * 4], p=[.25, .15, .25, .15, .1, .1])
                ln = min(ln, total - b)
                out.append((b, ln))
                b += ln
            else:
                b += grid
        if not out: out = [(0, 1.0)]
        return out

    def shape(self, n):
        return list(self.r.integers(-2, 3, size=n))

    def nearest(self, target_set, around):
        cands = [m for m in self.scale if m % 12 in {t % 12 for t in target_set}]
        if not cands: return around
        return min(cands, key=lambda m: abs(m - around) + (0.5 if m == around else 0))

    def play(self, rhythm, shape, chords_by_bar, start_bar, lift=0):
        """chords_by_bar: 小節ごとの和音の音（list）。戻り値：(小節, 拍, 長さ, midi)"""
        out = []
        for (b, ln), step in zip(rhythm, shape):
            bar = start_bar + int(b // self.beats)
            beat = b % self.beats
            ch = chords_by_bar[(bar - start_bar) % len(chords_by_bar)]
            idx = min(range(len(self.scale)), key=lambda i: abs(self.scale[i] - self.prev))
            idx = max(0, min(len(self.scale) - 1, idx + int(step) + lift))
            m = self.scale[idx]
            if abs(beat % 1) < 1e-6 or ln >= 1:   # 強い拍や長い音は和音の音に
                m = self.nearest(ch, m)
            # 音域のまんなかへ戻ろうとする
            mid = (self.scale[0] + self.scale[-1]) / 2
            if abs(m - mid) > 10: m = self.nearest(ch, m - np.sign(m - mid) * 7)
            self.prev = m
            out.append((bar, beat, ln, m))
        return out

# ---------------- 曲 ----------------
def track_meadow():
    """はじまりの草原：明るいドット絵風（C, 140）"""
    prog = ['C', 'G', 'Am', 'F', 'C', 'G', 'F', 'G']
    S = Song(140, 64)
    mel = Melody(11, 'C', 'major', 64, 86, 0.75)
    sections = ['A', 'A', 'B', 'A', 'A', 'B', 'C', 'A']   # 8小節ずつ
    motifs = {k: (mel.rhythm(2), mel.shape(16)) for k in 'ABC'}
    for si, sec in enumerate(sections):
        p = prog if sec != 'B' else ['F', 'G', 'Em', 'Am', 'F', 'G', 'C', 'C']
        if sec == 'C': p = ['Am', 'Em', 'F', 'C', 'Dm', 'G', 'C', 'G']
        for b in range(8):
            bar = si * 8 + b
            root, notes = chord(p[b], 2)
            for beat in range(4):   # 8分のベース
                for h in (0, .5):
                    S.note('bass', 'tri', root + (12 if h else 0), bar, beat + h, 0.45, 0.9)
            # アルペジオ
            if si > 0:
                arp = voicing(p[b], 67)
                for i in range(8):
                    S.note('arp', 'sq25', arp[i % len(arp)] + (12 if i % 4 == 3 else 0), bar, i * 0.5, 0.4, 0.5)
            # ドラム
            if si > 0:
                for beat in range(4):
                    S.hit('drums', 'chip_kick' if beat % 2 == 0 else 'noise_snare', bar, beat)
                    S.hit('drums', 'hat', bar, beat + 0.5, 0.6)
        if si == 0: continue
        rh, sh = motifs[sec]
        chs = [chord(c)[1] for c in p]
        for rep in range(4):
            for (bar, beat, ln, m) in mel.play(rh, sh, chs[rep * 2:rep * 2 + 2], si * 8 + rep * 2, lift=1 if rep == 2 else 0):
                S.note('lead', 'sq', m, bar, beat, ln, 0.8)
    return S, {'bass': (0.9, 0), 'arp': (0.35, 0.2), 'lead': (0.55, 0.25), 'drums': (0.5, 0.1)}

def track_casino():
    """夜のカジノ：スウィングのジャズ（F, 118）"""
    prog = ['Gm7', 'C7', 'Fmaj7', 'D7', 'Gm7', 'C7', 'Am7', 'D7', 'Bbmaj7', 'Bbm7', 'Am7', 'D7', 'Gm7', 'C7', 'Fmaj7', 'C7']
    S = Song(118, 64, swing=0.33)
    mel = Melody(23, 'F', 'dorian', 62, 84, 0.7)
    for bar in range(64):
        c = prog[bar % 16]
        root, notes = chord(c, 2)
        # ウォーキングベース
        nxt = chord(prog[(bar + 1) % 16], 2)[0]
        line = [root, notes[1] if len(notes) > 1 else root + 4, notes[2], nxt - 1 if nxt > root else nxt + 1]
        for beat, m in enumerate(line):
            S.note('bass', 'bass', m, bar, beat, 0.9, 0.95)
        # ピアノのコンピング（2拍目の裏と4拍目）
        v = voicing(c, 62)
        for beat in (1.5, 3):
            if bar >= 2:
                for m in v: S.note('keys', 'ep', m, bar, beat, 0.4, 0.5)
        # ブラシとライド
        for beat in range(4):
            S.hit('drums', 'brush', bar, beat, 0.8)
            if beat in (1, 3): S.hit('drums', 'rim', bar, beat, 0.5)
            S.hit('drums', 'hat', bar, beat + 0.5, 0.45)
        if bar % 4 == 0: S.hit('drums', 'kick', bar, 0, 0.5)
    motifs = [(mel.rhythm(2), mel.shape(16)) for _ in range(3)]
    form = [0, 0, 1, 0, 2, 2, 1, 0]
    for fi, mi in enumerate(form):
        if fi == 0: continue
        start = fi * 8
        chs = [chord(prog[(start + i) % 16])[1] for i in range(8)]
        rh, sh = motifs[mi]
        for rep in range(4):
            for (bar, beat, ln, m) in mel.play(rh, sh, chs[rep * 2:rep * 2 + 2], start + rep * 2):
                S.note('lead', 'ep' if fi < 4 else 'piano', m + 12, bar, beat, ln, 0.75)
    return S, {'bass': (1.0, 0.05), 'keys': (0.45, 0.25), 'drums': (0.45, 0.15), 'lead': (0.6, 0.3)}

def track_lofi():
    """雨の日のローファイ（80）"""
    prog = ['Fmaj7', 'Em7', 'Dm7', 'Cmaj7', 'Fmaj7', 'Em7', 'Dm9', 'G7']
    S = Song(80, 32, swing=0.25)
    mel = Melody(37, 'C', 'major', 64, 81, 0.45)
    rh, sh = mel.rhythm(2), mel.shape(16)
    rh2, sh2 = mel.rhythm(2), mel.shape(16)
    for bar in range(32):
        c = prog[bar % 8]
        root, _ = chord(c, 2)
        S.note('bass', 'bass', root, bar, 0, 1.4, 0.9)
        S.note('bass', 'bass', root + 7, bar, 2.5, 1.0, 0.7)
        for m in voicing(c, 60):
            S.note('keys', 'ep', m, bar, 0, 3.0, 0.55)
            if bar % 2: S.note('keys', 'ep', m, bar, 2.5, 1.0, 0.35)
        if bar >= 2:
            S.hit('drums', 'kick', bar, 0)
            S.hit('drums', 'kick', bar, 2.5, 0.7)
            S.hit('drums', 'snare', bar, 1, 0.6)
            S.hit('drums', 'snare', bar, 3, 0.6)
            for i in range(8): S.hit('drums', 'hat', bar, i * 0.5, 0.5 if i % 2 else 0.7)
    for start in range(4, 32, 4):
        r, s_ = (rh, sh) if (start // 4) % 2 else (rh2, sh2)
        chs = [chord(prog[(start + i) % 8])[1] for i in range(4)]
        for rep in range(2):
            for (bar, beat, ln, m) in mel.play(r, s_, chs[rep * 2:rep * 2 + 2], start + rep * 2):
                S.note('lead', 'piano', m, bar, beat, ln, 0.7)
    # レコードのプチプチと雨
    b = S.bus('vinyl')
    r = np.random.default_rng(5)
    clicks = r.random(len(b)) < 0.0004
    b += clicks * r.standard_normal(len(b)) * 0.4
    b += lowpass(r.standard_normal(len(b)), 2500) * 0.02
    return S, {'bass': (0.9, 0), 'keys': (0.5, 0.2), 'drums': (0.55, 0.05), 'lead': (0.55, 0.35), 'vinyl': (0.5, 0)}

def track_musicbox():
    """星空のオルゴール（3拍子, 84）"""
    prog = ['D', 'Bm', 'G', 'A', 'D', 'F#m', 'G', 'A', 'Bm', 'G', 'Em', 'A', 'G', 'A', 'D', 'D']
    S = Song(84, 48, beats=3)
    mel = Melody(41, 'D', 'major', 74, 93, 0.7, beats=3)
    rh, sh = mel.rhythm(2), mel.shape(16)
    rh2, sh2 = mel.rhythm(2), mel.shape(16)
    for bar in range(48):
        c = prog[bar % 16]
        root, notes = chord(c, 3)
        v = voicing(c, 67)
        S.note('box', 'box', root, bar, 0, 2, 0.7)
        S.note('box', 'box', v[1 % len(v)], bar, 1, 1, 0.45)
        S.note('box', 'box', v[2 % len(v)], bar, 2, 1, 0.45)
        if bar % 2 == 0 and bar >= 8:
            for m in voicing(c, 60): S.note('pad', 'pad', m, bar, 0, 6, 0.45)
    for start in range(4, 46, 4):
        r, s_ = (rh, sh) if (start // 4) % 3 != 2 else (rh2, sh2)
        chs = [chord(prog[(start + i) % 16])[1] for i in range(4)]
        for rep in range(2):
            for (bar, beat, ln, m) in mel.play(r, s_, chs[rep * 2:rep * 2 + 2], start + rep * 2):
                S.note('bell', 'box', m, bar, beat, ln, 0.75)
    return S, {'box': (0.6, 0.45), 'pad': (0.35, 0.5), 'bell': (0.7, 0.5)}

def track_adventure():
    """冒険の朝：弦と弾く音で元気に（G, 124）"""
    prog = ['G', 'D', 'Em', 'C', 'G', 'D', 'C', 'D', 'Em', 'C', 'G', 'D', 'Am', 'C', 'D', 'D']
    S = Song(124, 64)
    mel = Melody(53, 'G', 'major', 62, 86, 0.65)
    mot = [(mel.rhythm(2), mel.shape(16)) for _ in range(2)]
    for bar in range(64):
        c = prog[bar % 16]
        root, _ = chord(c, 2)
        for beat in range(4): S.note('bass', 'bass', root + (12 if beat == 3 else 0), bar, beat, 0.9, 0.9)
        v = voicing(c, 64)
        if bar % 2 == 0:
            for m in v: S.note('strings', 'strings', m, bar, 0, 7.8, 0.5)
        if bar >= 4:
            pat = [0, 1, 2, 1, 2, 1, 0, 1]
            for i, k in enumerate(pat): S.note('pluck', 'pluck', v[k % len(v)] + 12, bar, i * 0.5, 0.45, 0.55)
        if bar >= 8:
            S.hit('drums', 'kick', bar, 0); S.hit('drums', 'kick', bar, 2)
            S.hit('drums', 'snare', bar, 1, 0.7); S.hit('drums', 'snare', bar, 3, 0.7)
            for i in range(8): S.hit('drums', 'shaker', bar, i * 0.5, 0.8)
            if bar % 8 == 7: [S.hit('drums', 'snare', bar, 3 + i * 0.25, 0.4 + i * 0.1) for i in range(4)]
    for start in range(8, 64, 8):
        r, s_ = mot[(start // 8) % 2]
        chs = [chord(prog[(start + i) % 16])[1] for i in range(8)]
        for rep in range(4):
            for (bar, beat, ln, m) in mel.play(r, s_, chs[rep * 2:rep * 2 + 2], start + rep * 2, lift=1 if rep == 3 else 0):
                S.note('lead', 'strings' if start >= 32 else 'pluck', m + (12 if start < 32 else 0), bar, beat, ln, 0.75)
    return S, {'bass': (0.85, 0), 'strings': (0.4, 0.4), 'pluck': (0.35, 0.3), 'drums': (0.5, 0.15), 'lead': (0.6, 0.35)}

def track_dungeon():
    """深いダンジョン：暗い短調（D minor, 92）"""
    prog = ['Dm', 'Bb', 'Gm', 'A', 'Dm', 'Bb', 'C', 'A']
    S = Song(92, 40)
    mel = Melody(67, 'D', 'harm', 57, 77, 0.5)
    rh, sh = mel.rhythm(2), mel.shape(16)
    for bar in range(40):
        c = prog[bar % 8]
        root, notes = chord(c, 1)
        S.note('drone', 'pad', root, bar, 0, 4, 0.8)
        S.note('drone', 'pad', root + 12, bar, 0, 4, 0.4)
        if bar >= 4:
            v = voicing(c, 62)
            for i in range(16): S.note('arp', 'pluck', v[(i * 2) % len(v)] + (12 if i % 8 >= 4 else 0), bar, i * 0.25, 0.22, 0.4)
        if bar >= 8:
            S.hit('drums', 'kick', bar, 0, 0.9); S.hit('drums', 'kick', bar, 1.5, 0.6)
            S.hit('drums', 'clap', bar, 2, 0.5)
            S.hit('drums', 'ohat', bar, 3.5, 0.4)
    for start in range(8, 38, 4):
        chs = [chord(prog[(start + i) % 8])[1] for i in range(4)]
        for rep in range(2):
            for (bar, beat, ln, m) in mel.play(rh, sh, chs[rep * 2:rep * 2 + 2], start + rep * 2):
                S.note('lead', 'bell', m, bar, beat, ln, 0.6)
    return S, {'drone': (0.6, 0.4), 'arp': (0.4, 0.4), 'drums': (0.55, 0.25), 'lead': (0.45, 0.6)}

def track_sunset():
    """海辺の夕焼け：ボサノバ風（96）"""
    prog = ['Am7', 'D9', 'Gmaj7', 'Cmaj7', 'F#m7b5', 'B7', 'Em7', 'E7']
    S = Song(96, 40)
    mel = Melody(79, 'G', 'major', 64, 83, 0.55)
    rh, sh = mel.rhythm(2), mel.shape(16)
    rh2, sh2 = mel.rhythm(2), mel.shape(16)
    for bar in range(40):
        c = prog[bar % 8]
        root, notes = chord(c, 2)
        S.note('bass', 'bass', root, bar, 0, 1.4, 0.9)
        S.note('bass', 'bass', root + 7, bar, 1.5, 0.4, 0.6)
        S.note('bass', 'bass', root + 7, bar, 2, 1.4, 0.8)
        S.note('bass', 'bass', root, bar, 3.5, 0.4, 0.6)
        v = voicing(c, 60)
        for b in (0, 1, 1.5, 2.5, 3):   # ボサのギター
            for m in v: S.note('guitar', 'nylon', m, bar, b + 0.02 * v.index(m), 0.5, 0.45)
        if bar >= 2:
            for i in range(8): S.hit('drums', 'shaker', bar, i * 0.5, 0.9 if i % 2 else 0.6)
            for b in (0, 1.5, 3): S.hit('drums', 'rim', bar, b, 0.5)
            S.hit('drums', 'kick', bar, 0, 0.5); S.hit('drums', 'kick', bar, 2, 0.4)
    for start in range(4, 40, 4):
        r, s_ = (rh, sh) if (start // 4) % 2 else (rh2, sh2)
        chs = [chord(prog[(start + i) % 8])[1] for i in range(4)]
        for rep in range(2):
            for (bar, beat, ln, m) in mel.play(r, s_, chs[rep * 2:rep * 2 + 2], start + rep * 2):
                S.note('lead', 'ep', m + 12, bar, beat, ln, 0.6)
    return S, {'bass': (0.9, 0.05), 'guitar': (0.5, 0.2), 'drums': (0.45, 0.1), 'lead': (0.5, 0.35)}

def track_parade():
    """ドット絵のパレード：はやいファミコン風（A, 160）"""
    prog = ['A', 'F#m', 'D', 'E', 'A', 'F#m', 'Bm', 'E']
    S = Song(160, 64)
    mel = Melody(97, 'A', 'major', 69, 93, 0.8)
    mots = [(mel.rhythm(2, 0.5), mel.shape(16)) for _ in range(3)]
    for bar in range(64):
        c = prog[bar % 8]
        root, notes = chord(c, 2)
        for i in range(8): S.note('bass', 'tri', root + (12 if i % 2 else 0), bar, i * 0.5, 0.4, 0.9)
        v = voicing(c, 72)
        for i in range(16): S.note('arp', 'sq25', v[i % len(v)], bar, i * 0.25, 0.2, 0.4)
        for beat in range(4):
            S.hit('drums', 'chip_kick', bar, beat)
            if beat % 2: S.hit('drums', 'noise_snare', bar, beat)
            S.hit('drums', 'hat', bar, beat + 0.5, 0.5)
    for start in range(8, 64, 8):
        r, s_ = mots[(start // 8) % 3]
        chs = [chord(prog[(start + i) % 8])[1] for i in range(8)]
        for rep in range(4):
            for (bar, beat, ln, m) in mel.play(r, s_, chs[rep * 2:rep * 2 + 2], start + rep * 2, lift=1 if rep == 2 else 0):
                S.note('lead', 'sq', m, bar, beat, ln, 0.8)
                S.note('echo', 'sq', m, bar, beat + 0.75, ln, 0.3)
    return S, {'bass': (0.8, 0), 'arp': (0.25, 0.15), 'drums': (0.45, 0.05), 'lead': (0.55, 0.2), 'echo': (0.3, 0.3)}

TRACKS = [
    ('meadow', 'はじまりの草原', 'ドット絵風', track_meadow),
    ('casino', '夜のカジノ', 'ジャズ', track_casino),
    ('lofi', '雨の日のローファイ', 'ローファイ', track_lofi),
    ('musicbox', '星空のオルゴール', 'オルゴール', track_musicbox),
    ('adventure', '冒険の朝', 'オーケストラ風', track_adventure),
    ('dungeon', '深いダンジョン', 'ダーク', track_dungeon),
    ('sunset', '海辺の夕焼け', 'ボサノバ', track_sunset),
    ('parade', 'ドット絵のパレード', 'チップチューン', track_parade),
]

if __name__ == '__main__':
    only = sys.argv[1:]
    info = []
    for tid, title, genre, fn in TRACKS:
        if only and tid not in only: continue
        S, sends = fn()
        secs = master(S, sends, tid)
        info.append({'id': tid, 'title': title, 'genre': genre, 'seconds': round(secs, 1)})
        print(tid, round(secs, 1), 's', os.path.getsize(os.path.join(OUT, tid + '.ogg')) // 1024, 'KB')
    if not only:
        json.dump(info, open(os.path.join(OUT, '..', '..', 'music.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
