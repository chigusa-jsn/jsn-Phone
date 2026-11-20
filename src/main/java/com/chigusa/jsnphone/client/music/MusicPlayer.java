package com.chigusa.jsnphone.client.music;

import com.chigusa.jsnphone.JsnPhone;
import com.chigusa.jsnphone.client.PhoneClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 音楽プレーヤー：スマホに入っているオリジナル曲を流す（自分にだけ聞こえる。イヤホンのイメージ）。
 * スマホを閉じても流れ続ける。スマホを手放すと止まる。
 */
@EventBusSubscriber(modid = JsnPhone.MODID, value = Dist.CLIENT)
public final class MusicPlayer {
    public record Track(String id, String title, String genre, float seconds) {}

    public static final int MODE_ALL = 0, MODE_ONE = 1, MODE_SHUFFLE = 2;
    private static List<Track> tracks;
    private static int index = -1;
    private static Sound playing;
    private static int ticks;          // 流れている時間（ゲームが止まっている間は数えない）
    public static int mode = MODE_ALL;
    public static float volume = 0.8F;
    private static long shownAt;
    private static final Random R = new Random();

    static final class Sound extends AbstractTickableSoundInstance {
        Sound(Track t) {
            super(SoundEvent.createVariableRangeEvent(JsnPhone.id("music." + t.id())), SoundSource.MUSIC, RandomSource.create());
            this.looping = false;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = MusicPlayer.volume;
            this.x = this.y = this.z = 0;
        }

        @Override
        public void tick() {
            this.volume = MusicPlayer.volume;
        }

        void end() {
            stop();
        }
    }

    public static List<Track> tracks() {
        if (tracks == null) {
            tracks = new ArrayList<>();
            try {
                var res = Minecraft.getInstance().getResourceManager().getResource(JsnPhone.id("music.json"));
                if (res.isPresent()) {
                    try (var in = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                        JsonArray a = JsonParser.parseReader(in).getAsJsonArray();
                        for (JsonElement e : a) {
                            JsonObject o = e.getAsJsonObject();
                            tracks.add(new Track(o.get("id").getAsString(), o.get("title").getAsString(), o.get("genre").getAsString(), o.get("seconds").getAsFloat()));
                        }
                    }
                }
            } catch (Exception e) {
                JsnPhone.LOG.warn("曲の一覧が読めなかった", e);
            }
        }
        return tracks;
    }

    public static boolean isPlaying() {
        return playing != null;
    }

    public static int index() {
        return index;
    }

    public static Track current() {
        return index >= 0 && index < tracks().size() ? tracks().get(index) : null;
    }

    /** 流れている秒数 */
    public static float elapsed() {
        return ticks / 20F;
    }

    public static void play(int i) {
        List<Track> t = tracks();
        if (t.isEmpty()) return;
        stop();
        index = Math.floorMod(i, t.size());
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying();   // マイクラのBGMは止める
        playing = new Sound(t.get(index));
        mc.getSoundManager().play(playing);
        ticks = 0;
        shownAt = System.currentTimeMillis();
    }

    public static void stop() {
        if (playing != null) {
            Minecraft.getInstance().getSoundManager().stop(playing);
            playing.end();
            playing = null;
        }
    }

    public static void toggle() {
        if (playing != null) stop();
        else play(Math.max(0, index));
    }

    public static void next() {
        List<Track> t = tracks();
        if (t.isEmpty()) return;
        if (mode == MODE_SHUFFLE && t.size() > 1) {
            int n;
            do n = R.nextInt(t.size()); while (n == index);
            play(n);
        } else play(index + 1);
    }

    public static void prev() {
        // 3秒より先なら、いまの曲を頭から
        if (playing != null && elapsed() > 3) play(index);
        else play(index - 1);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (playing == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || PhoneClient.phoneStack(mc.player).isEmpty()) {
            stop();
            return;
        }
        if (!mc.isPaused()) ticks++;
        Track t = current();
        boolean over = t != null && elapsed() > t.seconds() + 1;
        if (ticks > 40 && (!mc.getSoundManager().isActive(playing) || over)) {
            playing = null;
            if (mode == MODE_ONE) play(index);
            else next();
        }
    }

    /** 自分の曲が流れている間は、マイクラのBGMを流さない */
    @SubscribeEvent
    public static void music(SelectMusicEvent event) {
        if (playing != null) event.setMusic(null);
    }

    /** 曲が変わったとき、左下に小さく曲名を出す */
    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        if (playing == null || Minecraft.getInstance().screen != null) return;
        long since = System.currentTimeMillis() - shownAt;
        if (since > 4000) return;
        Track t = current();
        if (t == null) return;
        GuiGraphics g = event.getGuiGraphics();
        var f = Minecraft.getInstance().font;
        String s = "♪ " + t.title();
        int a = since > 3500 ? (int) (255 * (4000 - since) / 500.0) : 255;
        if (a < 8) return;
        int x = 6, y = g.guiHeight() - 60;
        g.fill(x, y, x + f.width(s) + 10, y + 14, (a * 3 / 4) << 24 | 0x181820);
        g.drawString(f, s, x + 5, y + 3, a << 24 | 0xFFFFFF);
    }

    public static void reset() {
        stop();
        tracks = null;
    }

    private MusicPlayer() {}
}
