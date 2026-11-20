package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.music.MusicPlayer;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** 音楽：スマホに入っているオリジナル曲（著作権フリー）。閉じても流れ続ける */
public class MusicApp extends App {
    private static final int[][] ART = {
            {0xFF58C060, 0xFF2A7A40}, {0xFF8A2040, 0xFF2A0A20}, {0xFF5A6A8A, 0xFF2A3040}, {0xFF3A2A7A, 0xFF0A0A2A},
            {0xFFF0A040, 0xFFA04020}, {0xFF403050, 0xFF100810}, {0xFFF07050, 0xFF6A2A6A}, {0xFF40A0F0, 0xFFF040A0}};
    private static final String[] ICON = {"☘", "♠", "☂", "★", "⚔", "☠", "☀", "♪"};

    @Override
    public String title() {
        return "音楽";
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF101014);
        List<MusicPlayer.Track> tracks = MusicPlayer.tracks();
        int cur = MusicPlayer.index();
        MusicPlayer.Track t = MusicPlayer.current();
        int top = y + 22;
        // ジャケット
        int aw = 70, ax = x + (w - aw) / 2;
        art(g, cur < 0 ? 0 : cur, ax, top, aw, MusicPlayer.isPlaying());
        int yy = top + aw + 6;
        g.drawCenteredString(font, "§l" + cut(t == null ? "曲をえらんでね" : t.title(), w - 10), x + w / 2, yy, 0xFFFFFF);
        yy += 11;
        g.drawCenteredString(font, "§7" + (t == null ? "オリジナル曲 " + tracks.size() + "曲" : t.genre()), x + w / 2, yy, 0xFFFFFF);
        yy += 13;
        // 進み具合
        float total = t == null ? 1 : t.seconds(), el = MusicPlayer.isPlaying() ? Math.min(total, MusicPlayer.elapsed()) : 0;
        g.fill(x + 10, yy, x + w - 10, yy + 3, 0xFF34343C);
        g.fill(x + 10, yy, x + 10 + (int) ((w - 20) * el / total), yy + 3, s.accent());
        yy += 5;
        g.pose().pushPose();
        g.pose().translate(0, 0, 0);
        g.drawString(font, "§8" + time(el), x + 10, yy, 0xFFFFFF, false);
        String tt = time(total);
        g.drawString(font, "§8" + tt, x + w - 10 - font.width(tt), yy, 0xFFFFFF, false);
        g.pose().popPose();
        yy += 12;
        // ボタン
        int cx = x + w / 2;
        String[] modes = {"⟳ 全曲", "① 1曲", "⇄ シャッフル"};
        s.button(g, x + 6, yy + 4, 30, 14, "§7" + modes[MusicPlayer.mode].split(" ")[0], 0xFF24242C, mx, my, () -> MusicPlayer.mode = (MusicPlayer.mode + 1) % 3);
        s.button(g, cx - 40, yy, 22, 22, "⏮", 0xFF24242C, mx, my, MusicPlayer::prev);
        s.button(g, cx - 13, yy - 2, 26, 26, MusicPlayer.isPlaying() ? "§l■" : "§l▶", s.accent(), mx, my, MusicPlayer::toggle);
        s.button(g, cx + 18, yy, 22, 22, "⏭", 0xFF24242C, mx, my, MusicPlayer::next);
        yy += 27;
        g.drawCenteredString(font, "§8" + modes[MusicPlayer.mode], x + 21, yy - 6, 0xFFFFFF);
        // 音量
        g.drawString(font, "§7♪", x + 8, yy + 1, 0xFFFFFF, false);
        int vx = x + 22, vw = w - 44;
        g.fill(vx, yy + 4, vx + vw, yy + 6, 0xFF34343C);
        g.fill(vx, yy + 4, vx + (int) (vw * MusicPlayer.volume), yy + 6, 0xFFB0B0C0);
        g.fill(vx + (int) (vw * MusicPlayer.volume) - 2, yy + 1, vx + (int) (vw * MusicPlayer.volume) + 2, yy + 9, 0xFFFFFFFF);
        for (int i = 0; i <= 10; i++) {
            float v = i / 10F;
            s.hot(vx + (int) (vw * (i - 0.5F) / 10), yy, vw / 10 + 1, 10, () -> MusicPlayer.volume = v);
        }
        g.drawString(font, "§7♫", x + w - 18, yy + 1, 0xFFFFFF, false);
        yy += 14;
        // 曲の一覧
        int listTop = yy;
        g.fill(x, listTop - 1, x + w, listTop, 0x30FFFFFF);
        g.enableScissor(x, listTop, x + w, y + h);
        int ly = listTop + 2 - scroll;
        for (int i = 0; i < tracks.size(); i++) {
            MusicPlayer.Track tr = tracks.get(i);
            boolean on = i == cur && MusicPlayer.isPlaying();
            if (mx >= x && mx < x + w && my >= ly && my < ly + 20 && my >= listTop) g.fill(x, ly, x + w, ly + 20, 0x18FFFFFF);
            art(g, i, x + 4, ly + 2, 16, false);
            g.drawString(font, (on ? "§a" : "§f") + cut(tr.title(), w - 70), x + 24, ly + 2, 0xFFFFFF, false);
            g.drawString(font, "§8" + tr.genre(), x + 24, ly + 11, 0xFFFFFF, false);
            String d = on ? "♪" : time(tr.seconds());
            g.drawString(font, (on ? "§a" : "§8") + d, x + w - 6 - font.width(d), ly + 6, 0xFFFFFF, false);
            int idx = i;
            if (ly + 20 > listTop) s.hot(x, Math.max(ly, listTop), w, 20, () -> MusicPlayer.play(idx));
            ly += 21;
        }
        g.disableScissor();
        contentH = (listTop - y) + tracks.size() * 21 + 10;
        header(g, x, y, w, title(), mx, my);
    }

    @Override
    public boolean scroll(double amount) {
        int max = Math.max(0, MusicPlayer.tracks().size() * 21 - 100);
        scroll = Math.max(0, Math.min(max, scroll - (int) (amount * 16)));
        return true;
    }

    private void art(GuiGraphics g, int i, int x, int y, int size, boolean spin) {
        int[] c = ART[i % ART.length];
        for (int k = 0; k < size; k += 2) {
            float f = k / (float) size;
            int col = 0xFF000000 | lerp(c[0], c[1], f);
            g.fill(x, y + k, x + size, y + Math.min(size, k + 2), col);
        }
        if (size >= 40) {
            // レコードのような丸
            int cx = x + size / 2, cy = y + size / 2, r = size / 3;
            double a0 = spin ? s.ticks * 0.08 : 0;
            for (int k = 0; k < 360; k += 6) {
                double a = Math.toRadians(k) + a0;
                g.fill(cx + (int) (Math.cos(a) * r), cy + (int) (Math.sin(a) * r), cx + (int) (Math.cos(a) * r) + 2, cy + (int) (Math.sin(a) * r) + 2, 0x60FFFFFF);
            }
            g.pose().pushPose();
            g.pose().translate(cx, cy - 6, 0);
            g.pose().scale(1.6F, 1.6F, 1);
            g.drawCenteredString(font, ICON[i % ICON.length], 0, 0, 0xFFFFFF);
            g.pose().popPose();
        } else {
            g.drawCenteredString(font, ICON[i % ICON.length], x + size / 2, y + (size - 8) / 2, 0xFFFFFF);
        }
    }

    private static int lerp(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    private static String time(float sec) {
        int s = (int) sec;
        return s / 60 + ":" + String.format("%02d", s % 60);
    }
}
