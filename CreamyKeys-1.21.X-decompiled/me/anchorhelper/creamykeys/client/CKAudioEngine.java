package me.anchorhelper.creamykeys.client;

import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import org.slf4j.Logger;

public final class CKAudioEngine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object CACHE_LOCK = new Object();
    private static final Object WAIT_LOCK = new Object();
    private static final int OUT_RATE = 44100;
    private static final int OUT_CHANNELS = 2;
    private static final int CHUNK_FRAMES = 256;
    private static final int MAX_CACHED = 256;
    private static final int MAX_PENDING = 1024;
    private static final int MAX_VOICES = 32;
    private static final int MAX_SAME_SOUND_VOICES = 2;
    private static final int FADE_OUT_FRAMES = 48;
    private static final AtomicInteger PENDING = new AtomicInteger(0);
    private static final LinkedHashMap<Path, SoundData> CACHE = new LinkedHashMap<Path, SoundData>(64, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, SoundData> eldest) {
            return this.size() > 256;
        }
    };
    private static final ConcurrentLinkedQueue<PlayRequest> QUEUE = new ConcurrentLinkedQueue();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static volatile boolean STOPPING;
    private static Thread MIX_THREAD;
    private static final AtomicBoolean WARMING;

    private CKAudioEngine() {
    }

    public static void ensureStarted() {
        CKAudioEngine.ensureThread();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clearCache() {
        Object object = CACHE_LOCK;
        synchronized (object) {
            CACHE.clear();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void shutdown() {
        STOPPING = true;
        RUNNING.set(false);
        Object object = WAIT_LOCK;
        synchronized (object) {
            WAIT_LOCK.notifyAll();
        }
        Thread t = MIX_THREAD;
        if (t != null) {
            try {
                t.interrupt();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        MIX_THREAD = null;
        QUEUE.clear();
        PENDING.set(0);
        Object object2 = CACHE_LOCK;
        synchronized (object2) {
            CACHE.clear();
        }
        STOPPING = false;
    }

    public static void warmup(List<Path> files, int max) {
        if (files == null || files.isEmpty()) {
            return;
        }
        if (WARMING.getAndSet(true)) {
            return;
        }
        int lim = Math.max(1, Math.min(1024, max));
        ArrayList<Path> copy = new ArrayList<Path>(files.size());
        for (Path p : files) {
            if (p != null) {
                copy.add(p);
            }
            if (copy.size() < lim) continue;
            break;
        }
        Thread t = new Thread(() -> {
            try {
                for (Path p : copy) {
                    if (p == null) continue;
                    CKAudioEngine.getOrLoad(p);
                }
            }
            finally {
                WARMING.set(false);
            }
        }, "CreamyKeys-AudioWarmup");
        t.setDaemon(true);
        t.start();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void play(Path file, float volume) {
        PlayRequest drop;
        if (file == null) {
            return;
        }
        float v = Math.max(0.0f, Math.min(1.0f, volume));
        SoundData data = CKAudioEngine.getOrLoad(file);
        if (data == null) {
            return;
        }
        if (PENDING.get() >= 1024 && (drop = QUEUE.poll()) != null) {
            PENDING.decrementAndGet();
        }
        QUEUE.add(new PlayRequest(data, v));
        PENDING.incrementAndGet();
        CKAudioEngine.ensureThread();
        Object object = WAIT_LOCK;
        synchronized (object) {
            WAIT_LOCK.notifyAll();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void ensureThread() {
        if (RUNNING.get()) {
            return;
        }
        Class<CKAudioEngine> clazz = CKAudioEngine.class;
        synchronized (CKAudioEngine.class) {
            if (RUNNING.get()) {
                // ** MonitorExit[var0] (shouldn't be in output)
                return;
            }
            RUNNING.set(true);
            MIX_THREAD = new Thread(CKAudioEngine::mixLoop, "CreamyKeys-AudioMix");
            MIX_THREAD.setDaemon(true);
            try {
                MIX_THREAD.setPriority(10);
            }
            catch (Exception exception) {
                // empty catch block
            }
            MIX_THREAD.start();
            // ** MonitorExit[var0] (shouldn't be in output)
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static SoundData getOrLoad(Path file) {
        Object object = CACHE_LOCK;
        synchronized (object) {
            SoundData cached = CACHE.get(file);
            if (cached != null) {
                return cached;
            }
        }
        SoundData decoded = CKAudioEngine.decodeToStereo44100(file);
        if (decoded == null) {
            return null;
        }
        Object object2 = CACHE_LOCK;
        synchronized (object2) {
            CACHE.put(file, decoded);
        }
        return decoded;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void mixLoop() {
        DataLine line = null;
        try {
            AudioFormat fmt = new AudioFormat(44100.0f, 16, 2, true, false);
            int bytesPerChunk = 1024;
            int lineBufferBytes = Math.max(4096, bytesPerChunk * 6);
            line = CKAudioEngine.openLine(fmt, lineBufferBytes);
            if (line == null) {
                return;
            }
            ArrayList<Voice> voices = new ArrayList<Voice>(32);
            ArrayDeque<Voice> recycle = new ArrayDeque<Voice>(32);
            int samples = 512;
            int[] mix = new int[samples];
            byte[] out = new byte[samples * 2];
            boolean wasPlaying = false;
            while (RUNNING.get() && !STOPPING) {
                PlayRequest pr;
                boolean drainedAny = false;
                while ((pr = QUEUE.poll()) != null) {
                    PENDING.decrementAndGet();
                    drainedAny = true;
                    CKAudioEngine.addVoice(voices, recycle, pr.data.samples, pr.volume);
                }
                if (voices.isEmpty()) {
                    if (wasPlaying) {
                        try {
                            line.flush();
                        }
                        catch (Exception exception) {
                            // empty catch block
                        }
                        wasPlaying = false;
                    }
                    if (drainedAny) continue;
                    Object object = WAIT_LOCK;
                    synchronized (object) {
                        if (!STOPPING && RUNNING.get() && QUEUE.isEmpty() && voices.isEmpty()) {
                            try {
                                WAIT_LOCK.wait(50L);
                            }
                            catch (InterruptedException interruptedException) {
                                // empty catch block
                            }
                        }
                        continue;
                    }
                }
                wasPlaying = true;
                Arrays.fill(mix, 0);
                for (int vi = 0; vi < voices.size(); ++vi) {
                    Voice voice = voices.get(vi);
                    short[] s = voice.data;
                    int p = voice.pos;
                    float g = voice.gain;
                    int remaining = s.length - p;
                    if (remaining <= 0) continue;
                    int len = Math.min(samples, remaining);
                    int fadeSamples = Math.min(48, remaining / 2) * 2;
                    for (int i = 0; i < len; ++i) {
                        int intoFade;
                        float gg = g;
                        int rp = remaining - i;
                        if (fadeSamples > 0 && rp <= fadeSamples && (gg = g * (1.0f - (float)(intoFade = fadeSamples - rp) / (float)fadeSamples)) < 0.0f) {
                            gg = 0.0f;
                        }
                        int n = i;
                        mix[n] = mix[n] + (int)((float)s[p + i] * gg);
                    }
                    voice.pos = p + len;
                }
                for (int i = voices.size() - 1; i >= 0; --i) {
                    Voice v = voices.get(i);
                    if (v.pos < v.data.length) continue;
                    voices.remove(i);
                    recycle.addLast(v);
                }
                int peak = 1;
                for (int i = 0; i < samples; ++i) {
                    int abs;
                    int a = mix[i];
                    int n = abs = a >= 0 ? a : -a;
                    if (abs <= peak) continue;
                    peak = abs;
                }
                float scale = peak > Short.MAX_VALUE ? 32767.0f / (float)peak : 1.0f;
                int bi = 0;
                for (int i = 0; i < samples; ++i) {
                    int v = Math.round((float)mix[i] * scale);
                    if (v > Short.MAX_VALUE) {
                        v = Short.MAX_VALUE;
                    }
                    if (v < Short.MIN_VALUE) {
                        v = Short.MIN_VALUE;
                    }
                    short sv = (short)v;
                    out[bi++] = (byte)(sv & 0xFF);
                    out[bi++] = (byte)(sv >>> 8 & 0xFF);
                }
                line.write(out, 0, out.length);
            }
        }
        catch (Throwable t) {
            LOGGER.error("CreamyKeys audio mixer crashed", t);
        }
        finally {
            block60: {
                try {
                    if (line == null) break block60;
                    try {
                        line.flush();
                    }
                    catch (Exception exception) {}
                    try {
                        line.stop();
                    }
                    catch (Exception exception) {}
                    try {
                        line.close();
                    }
                    catch (Exception exception) {}
                }
                catch (Exception exception) {}
            }
            RUNNING.set(false);
            MIX_THREAD = null;
        }
    }

    private static SourceDataLine openLine(AudioFormat fmt, int lineBufferBytes) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
        try {
            SourceDataLine line = (SourceDataLine)AudioSystem.getLine(info);
            line.open(fmt, lineBufferBytes);
            line.start();
            LOGGER.info("CreamyKeys opened audio output using default mixer");
            return line;
        }
        catch (Throwable t) {
            Mixer.Info[] mixers;
            LOGGER.warn("CreamyKeys default mixer failed, trying fallbacks: {}", (Object)t.toString());
            for (Mixer.Info mixerInfo : mixers = AudioSystem.getMixerInfo()) {
                try {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    if (!mixer.isLineSupported(info)) continue;
                    SourceDataLine line = (SourceDataLine)mixer.getLine(info);
                    line.open(fmt, lineBufferBytes);
                    line.start();
                    LOGGER.info("CreamyKeys opened audio output using mixer: {}", (Object)mixerInfo.getName());
                    return line;
                }
                catch (Throwable t2) {
                    LOGGER.debug("CreamyKeys mixer {} failed: {}", (Object)mixerInfo.getName(), (Object)t2.toString());
                }
            }
            LOGGER.error("CreamyKeys could not open any SourceDataLine for format {}", (Object)fmt);
            return null;
        }
    }

    private static void addVoice(ArrayList<Voice> voices, ArrayDeque<Voice> recycle, short[] data, float gain) {
        Voice v;
        int same = 0;
        int stealIndex = -1;
        int stealPos = -1;
        for (int i = 0; i < voices.size(); ++i) {
            Voice v2 = voices.get(i);
            if (v2.data != data) continue;
            ++same;
            if (v2.pos <= stealPos) continue;
            stealPos = v2.pos;
            stealIndex = i;
        }
        if (same >= 2 && stealIndex >= 0) {
            Voice v3 = voices.get(stealIndex);
            v3.pos = 0;
            v3.gain = gain;
            return;
        }
        if (voices.size() >= 32) {
            int idx = 0;
            int bestPos = -1;
            for (int i = 0; i < voices.size(); ++i) {
                Voice v4 = voices.get(i);
                if (v4.pos <= bestPos) continue;
                bestPos = v4.pos;
                idx = i;
            }
            Voice evict = voices.remove(idx);
            recycle.addLast(evict);
        }
        if ((v = recycle.pollFirst()) == null) {
            v = new Voice();
        }
        v.data = data;
        v.pos = 0;
        v.gain = gain;
        voices.add(v);
    }

    /*
     * Exception decompiling
     */
    private static SoundData decodeToStereo44100(Path file) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 6 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at software.coley.recaf.services.decompile.cfr.CfrDecompiler.decompileInternal(CfrDecompiler.java:61)
         *     at software.coley.recaf.services.decompile.AbstractJvmDecompiler.decompile(AbstractJvmDecompiler.java:49)
         *     at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
         *     at java.base/java.lang.reflect.Method.invoke(Method.java:565)
         *     at org.jboss.weld.bean.proxy.AbstractBeanInstance.invoke(AbstractBeanInstance.java:39)
         *     at org.jboss.weld.bean.proxy.ProxyMethodHandler.invoke(ProxyMethodHandler.java:109)
         *     at software.coley.recaf.services.decompile.Decompiler$JvmDecompiler$1269202896$Proxy$_$$_WeldClientProxy.decompile(Unknown Source)
         *     at software.coley.recaf.services.decompile.DecompilerManager.lambda$decompile$2(DecompilerManager.java:156)
         *     at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1789)
         *     at software.coley.recaf.util.threading.ThreadUtil.lambda$wrap$2(ThreadUtil.java:236)
         *     at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1090)
         *     at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:614)
         *     at java.base/java.lang.Thread.run(Thread.java:1474)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static short[] convertAndResampleToStereo44100(short[] in, int ch, int sr) {
        if (ch <= 0) {
            return null;
        }
        int framesIn = in.length / ch;
        if (framesIn <= 0) {
            return null;
        }
        if (ch > 2) {
            short[] mono = new short[framesIn];
            int idx = 0;
            for (int f = 0; f < framesIn; ++f) {
                int sum = 0;
                for (int c = 0; c < ch; ++c) {
                    sum += in[idx++];
                }
                int v = (int)Math.round((double)sum / (double)ch);
                if (v > Short.MAX_VALUE) {
                    v = Short.MAX_VALUE;
                }
                if (v < Short.MIN_VALUE) {
                    v = Short.MIN_VALUE;
                }
                mono[f] = (short)v;
            }
            return CKAudioEngine.convertAndResampleToStereo44100(mono, 1, sr);
        }
        int framesOut = (int)Math.max(1L, Math.round((double)framesIn * (44100.0 / (double)sr)));
        short[] out = new short[framesOut * 2];
        double step = (double)sr / 44100.0;
        for (int o = 0; o < framesOut; ++o) {
            double inPos = (double)o * step;
            int i0 = (int)Math.floor(inPos);
            int i1 = i0 + 1;
            if (i0 < 0) {
                i0 = 0;
            }
            if (i1 >= framesIn) {
                i1 = framesIn - 1;
            }
            double t = inPos - (double)i0;
            if (ch == 1) {
                short sv;
                short s0 = in[i0];
                short s1 = in[i1];
                int v = (int)Math.round((double)s0 + (double)(s1 - s0) * t);
                if (v > Short.MAX_VALUE) {
                    v = Short.MAX_VALUE;
                }
                if (v < Short.MIN_VALUE) {
                    v = Short.MIN_VALUE;
                }
                out[o * 2] = sv = (short)v;
                out[o * 2 + 1] = sv;
                continue;
            }
            int base0 = i0 * 2;
            int base1 = i1 * 2;
            short l0 = in[base0];
            short r0 = in[base0 + 1];
            short l1 = in[base1];
            short r1 = in[base1 + 1];
            int lv = (int)Math.round((double)l0 + (double)(l1 - l0) * t);
            int rv = (int)Math.round((double)r0 + (double)(r1 - r0) * t);
            if (lv > Short.MAX_VALUE) {
                lv = Short.MAX_VALUE;
            }
            if (lv < Short.MIN_VALUE) {
                lv = Short.MIN_VALUE;
            }
            if (rv > Short.MAX_VALUE) {
                rv = Short.MAX_VALUE;
            }
            if (rv < Short.MIN_VALUE) {
                rv = Short.MIN_VALUE;
            }
            out[o * 2] = (short)lv;
            out[o * 2 + 1] = (short)rv;
        }
        return out;
    }

    static {
        WARMING = new AtomicBoolean(false);
    }

    private static final class SoundData {
        short[] samples;

        private SoundData() {
        }
    }

    private static final class PlayRequest {
        final SoundData data;
        final float volume;

        PlayRequest(SoundData data, float volume) {
            this.data = data;
            this.volume = volume;
        }
    }

    private static final class Voice {
        short[] data;
        int pos;
        float gain;

        private Voice() {
        }
    }
}
