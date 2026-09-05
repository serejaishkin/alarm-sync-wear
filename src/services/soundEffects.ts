// Web Audio API generator for alarm sounds, timer chimes, and bedtime sleep sounds

class SoundEffectsService {
  private audioCtx: AudioContext | null = null;
  private currentAmbientNode: { stop: () => void } | null = null;
  public playingAmbientType: string | null = null;

  private getAudioContext(): AudioContext {
    if (!this.audioCtx) {
      const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.audioCtx = new AudioContextClass();
    }
    if (this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
    return this.audioCtx;
  }

  /**
   * Play melodic alarm chime
   */
  public playAlarmChime() {
    try {
      const ctx = this.getAudioContext();
      const now = ctx.currentTime;
      const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6
      
      notes.forEach((freq, idx) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, now + idx * 0.15);
        gain.gain.setValueAtTime(0, now + idx * 0.15);
        gain.gain.linearRampToValueAtTime(0.2, now + idx * 0.15 + 0.05);
        gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.15 + 0.6);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + idx * 0.15);
        osc.stop(now + idx * 0.15 + 0.6);
      });
    } catch {
      // Audio context might be restricted before user gesture
    }
  }

  /**
   * Play timer completion alert
   */
  public playTimerAlert() {
    try {
      const ctx = this.getAudioContext();
      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(880, now);
      osc.frequency.setValueAtTime(1100, now + 0.12);
      osc.frequency.setValueAtTime(880, now + 0.24);
      gain.gain.setValueAtTime(0.25, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.6);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.6);
    } catch {}
  }

  /**
   * Play button click / lap feedback
   */
  public playClickSound() {
    try {
      const ctx = this.getAudioContext();
      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(600, now);
      gain.gain.setValueAtTime(0.1, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.05);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.05);
    } catch {}
  }

  /**
   * Play relaxing ambient sleep sound
   * Types: 'rain' | 'ocean' | 'forest' | 'white-noise'
   */
  public toggleAmbientSound(type: string, onStateChange?: (isPlaying: boolean, type: string | null) => void) {
    if (this.playingAmbientType === type) {
      this.stopAmbientSound();
      onStateChange?.(false, null);
      return false;
    }

    this.stopAmbientSound();
    const ctx = this.getAudioContext();

    // Create noise buffer
    const bufferSize = ctx.sampleRate * 2;
    const noiseBuffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const output = noiseBuffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      output[i] = Math.random() * 2 - 1;
    }

    const whiteNoise = ctx.createBufferSource();
    whiteNoise.buffer = noiseBuffer;
    whiteNoise.loop = true;

    const filter = ctx.createBiquadFilter();
    const masterGain = ctx.createGain();
    masterGain.gain.setValueAtTime(0.08, ctx.currentTime);

    if (type === 'rain') {
      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(1000, ctx.currentTime);
      filter.Q.setValueAtTime(2, ctx.currentTime);
    } else if (type === 'ocean') {
      filter.type = 'bandpass';
      filter.frequency.setValueAtTime(350, ctx.currentTime);
      // LFO for wave swelling
      const lfo = ctx.createOscillator();
      const lfoGain = ctx.createGain();
      lfo.frequency.setValueAtTime(0.12, ctx.currentTime); // slow wave period ~8s
      lfoGain.gain.setValueAtTime(250, ctx.currentTime);
      lfo.connect(filter.frequency);
      lfo.start();
    } else if (type === 'forest') {
      filter.type = 'bandpass';
      filter.frequency.setValueAtTime(2400, ctx.currentTime);
      masterGain.gain.setValueAtTime(0.04, ctx.currentTime);
    } else {
      // White noise
      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(1400, ctx.currentTime);
    }

    whiteNoise.connect(filter);
    filter.connect(masterGain);
    masterGain.connect(ctx.destination);
    whiteNoise.start();

    this.currentAmbientNode = {
      stop: () => {
        try {
          whiteNoise.stop();
          whiteNoise.disconnect();
        } catch {}
      }
    };
    this.playingAmbientType = type;
    onStateChange?.(true, type);
    return true;
  }

  public stopAmbientSound() {
    if (this.currentAmbientNode) {
      this.currentAmbientNode.stop();
      this.currentAmbientNode = null;
    }
    this.playingAmbientType = null;
  }
}

export const soundEffects = new SoundEffectsService();
