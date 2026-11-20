package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;

/** 時計：ゲームの時刻・日にち・月の形、本物の時刻、タイマー */
public class ClockApp extends App {
    private static final String[] MOON = {"満月", "欠けていく月", "下弦の月", "細い月", "新月", "細い月", "上弦の月", "ふくらむ月"};
    private int timerLeft = -1, timerSet = 60 * 20;

    @Override
    public String title() {
        return "時計";
    }

    @Override
    public void tick() {
        if (timerLeft > 0 && --timerLeft == 0) {
            Minecraft mc = Minecraft.getInstance();
            for (int i = 0; i < 3; i++)
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), 1.5F + i * 0.2F, 0.8F));
            timerLeft = -1;
        }
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF0C0C14);
        var level = Minecraft.getInstance().level;
        // アナログ時計
        int cx = x + w / 2, cy = y + 70, r = 40;
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            int px = cx + (int) (Math.sin(a) * r), py = cy - (int) (Math.cos(a) * r);
            g.fill(px - 1, py - 1, px + 1, py + 1, i % 3 == 0 ? 0xFFFFFFFF : 0xFF808090);
        }
        if (level != null) {
            long t = (level.getDayTime() + 6000) % 24000;
            double hour = t / 1000.0, min = (t % 1000) * 60 / 1000.0;
            hand(g, cx, cy, hour / 12 * 360, r * 0.55, 0xFFFFFFFF);
            hand(g, cx, cy, min / 60 * 360, r * 0.85, s.accent());
        }
        g.drawCenteredString(font, "§l" + PhoneTime.clock(), cx, cy + r + 8, 0xFFFFFF);
        g.drawCenteredString(font, "§7" + PhoneTime.day() + "日目" + (level != null ? "・" + MOON[level.getMoonPhase()] : ""), cx, cy + r + 20, 0xFFFFFF);
        LocalTime now = LocalTime.now();
        g.drawCenteredString(font, "§8本物の時刻 " + String.format("%d:%02d", now.getHour(), now.getMinute()), cx, cy + r + 32, 0xFFFFFF);
        // タイマー
        int ty = y + h - 56;
        g.fill(x + 6, ty, x + w - 6, ty + 48, 0xFF1C1C28);
        int shown = timerLeft > 0 ? timerLeft : timerSet;
        g.drawCenteredString(font, "§lタイマー " + shown / 1200 + ":" + String.format("%02d", shown / 20 % 60), cx, ty + 6, timerLeft > 0 ? 0xFFF0C040 : 0xFFFFFFFF);
        s.button(g, x + 10, ty + 22, 26, 16, "-1分", 0xFF2A2A34, mx, my, () -> timerSet = Math.max(1200, timerSet - 1200));
        s.button(g, x + 40, ty + 22, 26, 16, "+1分", 0xFF2A2A34, mx, my, () -> timerSet = Math.min(1200 * 60, timerSet + 1200));
        s.button(g, x + w - 60, ty + 22, 50, 16, timerLeft > 0 ? "止める" : "スタート", s.accent(), mx, my, () -> timerLeft = timerLeft > 0 ? -1 : timerSet);
        header(g, x, y, w, title(), mx, my);
    }

    private static void hand(GuiGraphics g, int cx, int cy, double deg, double len, int col) {
        double a = Math.toRadians(deg);
        for (int i = 0; i < len; i++) {
            int px = cx + (int) Math.round(Math.sin(a) * i), py = cy - (int) Math.round(Math.cos(a) * i);
            g.fill(px, py, px + 2, py + 2, col);
        }
    }
}
