# Procedural sound effects for FactoryCraft machines. Mono 44.1 kHz, encoded to Ogg Vorbis.
# Loops are built with circular operations so the seam is inaudible.
import numpy as np, subprocess, os, wave
SR = 44100
OUT = 'src/main/resources/assets/factorycraft/sounds/'
rng = np.random.default_rng(7)

def band_noise(n, lo, hi, circular=True):
    spec = np.fft.rfft(rng.standard_normal(n))
    f = np.fft.rfftfreq(n, 1 / SR)
    gain = ((f >= lo) & (f <= hi)).astype(float)
    # soft edges
    gain = np.convolve(gain, np.hanning(31) / np.hanning(31).sum(), mode='same')
    x = np.fft.irfft(spec * gain, n)
    return x / (np.abs(x).max() + 1e-9)

def lowpass(x, cutoff):
    n = len(x); spec = np.fft.rfft(x); f = np.fft.rfftfreq(n, 1 / SR)
    spec *= 1 / (1 + (f / cutoff) ** 4)
    return np.fft.irfft(spec, n)

def circ_add(buf, start, grain):
    n = len(buf); idx = (np.arange(len(grain)) + start) % n
    np.add.at(buf, idx, grain)

def lin_add(buf, start, grain):
    if start < 0: grain = grain[-start:]; start = 0
    end = min(len(buf), start + len(grain))
    if start < len(buf): buf[start:end] += grain[:end - start]

def mix(*xs):
    out = np.zeros(max(len(x) for x in xs))
    for x in xs: out[:len(x)] += x
    return out

def damped(freq, dur, decay, phase=0.0):
    t = np.arange(int(dur * SR)) / SR
    return np.sin(2 * np.pi * freq * t + phase) * np.exp(-t * decay)

def burst(dur, decay, lo, hi):
    n = int(dur * SR); t = np.arange(n) / SR
    return band_noise(n, lo, hi, False) * np.exp(-t * decay)

def normalize(x, peak=0.85):
    return x / (np.abs(x).max() + 1e-9) * peak

def fade(x, a=0.01, b=0.05):
    na, nb = int(a * SR), int(b * SR)
    x = x.copy(); x[:na] *= np.linspace(0, 1, na); x[-nb:] *= np.linspace(1, 0, nb)
    return x

def firing(f0, n, circular=True, amp=None, jitter=0.03):
    """Diesel combustion pulses at f0 Hz: thump + knock."""
    buf = np.zeros(n)
    period = SR / f0
    count = int(round(n / period))
    if circular: period = n / count
    for k in range(count):
        a = 1.0 if amp is None else amp(k * period / SR)
        start = int(k * period + rng.normal(0, jitter * period))
        g = mix(damped(f0 * 1.9, 0.09, 45) * 0.9, damped(f0 * 3.1, 0.05, 70, 1.0) * 0.5,
                burst(0.03, 120, 300, 2200) * 0.22)  # thump + knock
        g *= a * rng.uniform(0.85, 1.1)
        (circ_add if circular else lin_add)(buf, start, g)
    return buf

def encode(name, x):
    os.makedirs(OUT, exist_ok=True)
    pcm = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    tmp = f'/tmp/claude-0/{name}.wav'
    with wave.open(tmp, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', tmp, '-c:a', 'libvorbis', '-q:a', '4', OUT + name + '.ogg'], check=True)
    print(name, len(x) / SR, 's', os.path.getsize(OUT + name + '.ogg'), 'bytes')

# ---------------------------------------------------------------- loops
n = 2 * SR
eng = firing(30, n) + band_noise(n, 25, 140) * 0.35 + band_noise(n, 600, 3000) * 0.05
eng += np.sin(2 * np.pi * 60 * np.arange(n) / SR) * 0.12  # alternator hum (exact period in 2 s)
encode('engine_loop', normalize(lowpass(eng, 2500), 0.8))

trk = band_noise(n, 120, 700) * 0.25
for k in range(14):  # track links passing the sprocket
    start = int(k * n / 14 + rng.normal(0, 400))
    g = mix(damped(rng.uniform(850, 950), 0.12, 40), damped(rng.uniform(1350, 1500), 0.1, 55) * 0.6,
            damped(2300, 0.06, 80) * 0.3, burst(0.02, 150, 1500, 6000) * 0.5)
    circ_add(trk, start, g * rng.uniform(0.7, 1.0))
trk += np.sin(2 * np.pi * 7 * np.arange(n) / SR) * band_noise(n, 60, 200) * 0.3  # rumble
encode('tracks_loop', normalize(trk, 0.75))

t = np.arange(n) / SR
drl = band_noise(n, 250, 4000) * (0.6 + 0.4 * np.sin(2 * np.pi * 12 * t))
for h, a in ((110, 0.5), (220, 0.35), (330, 0.2), (660, 0.1)):
    drl += np.sin(2 * np.pi * h * t) * a
for k in range(40):  # rock chips
    circ_add(drl, int(rng.uniform(0, n)), burst(0.015, 200, 2000, 8000) * rng.uniform(0.3, 0.9))
encode('drill_loop', normalize(drl, 0.75))

# ---------------------------------------------------------------- one-shots
n = int(1.8 * SR); t = np.arange(n) / SR
starter = np.zeros(n)
crank_end = 1.0
mask = t < crank_end
f = 180 + 120 * (t / crank_end)
ph = 2 * np.cumsum(np.pi * f / SR)
starter[mask] = (np.sign(np.sin(ph)) * 0.25 + np.sin(ph * 2) * 0.15)[mask] * (0.6 + 0.4 * np.sin(2 * np.pi * 7 * t[mask]))
starter[mask] *= np.minimum(1, t[mask] / 0.05)
starter[mask] *= np.minimum(1, (crank_end - t[mask]) / 0.1)
chugs = firing(7, n, circular=False, amp=lambda s: 0.5 if s < crank_end else 0)
catch = firing(36, n, circular=False, amp=lambda s: 0 if s < crank_end - 0.05 else 1.4 * np.exp(-(s - crank_end) * 1.5) + 0.6)
start_snd = starter + chugs + catch + band_noise(n, 25, 150, False) * 0.3 * (t > crank_end)
encode('engine_start', normalize(fade(lowpass(start_snd, 3000), 0.005, 0.25), 0.85))

n = int(1.3 * SR); t = np.arange(n) / SR
stop = np.zeros(n); pos = 0.0; k = 0
while pos < 1.0:
    f0 = 30 - 24 * pos
    g = mix(damped(f0 * 1.9, 0.12, 30) * 0.9, burst(0.03, 120, 300, 1800) * 0.2) * (1 - pos * 0.8)
    lin_add(stop, int(pos * SR), g)
    pos += 1 / f0
lin_add(stop, int(1.02 * SR), mix(damped(70, 0.25, 18) * 0.8, burst(0.05, 60, 200, 3000) * 0.4))  # clunk
stop += band_noise(n, 25, 140, False) * 0.25 * np.clip(1 - t, 0, 1)
encode('engine_stop', normalize(fade(lowpass(stop, 2500), 0.005, 0.15), 0.8))

n = int(1.4 * SR); t = np.arange(n) / SR
f = 300 + 160 * np.minimum(1, t / 0.4)
ph = 2 * np.cumsum(np.pi * f / SR)
winch = (np.sin(ph) * 0.3 + np.sin(ph * 2) * 0.15 + np.sign(np.sin(ph * 0.5)) * 0.08)
for k in range(int(1.3 * 18)):  # ratchet / chain links
    lin_add(winch, int(k * SR / 18), mix(damped(rng.uniform(1800, 2600), 0.04, 90) * 0.5, burst(0.01, 300, 3000, 9000) * 0.3))
winch += band_noise(n, 400, 2500, False) * 0.08
encode('crane_winch', normalize(fade(winch, 0.02, 0.2), 0.8))

n = int(1.6 * SR); t = np.arange(n) / SR
env = np.minimum(1, t / 0.1) * np.clip((1.6 - t) / 0.5, 0, 1)
hyd = band_noise(n, 2500, 9000, False) * 0.35 * env
hyd += (np.sin(2 * np.pi * 120 * t) * 0.3 + np.sin(2 * np.pi * 240 * t) * 0.15) * env * (1 + 0.2 * np.sin(2 * np.pi * 9 * t))
hyd += band_noise(n, 80, 300, False) * 0.2 * env
encode('hydraulic', normalize(hyd, 0.75))
