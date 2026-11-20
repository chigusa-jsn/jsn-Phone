package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.chigusa.jsnphone.client.PhoneTheme;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;

/** 設定：テーマの色・壁紙・位置の共有・通知 */
public class SettingsApp extends App {
    @Override
    public String title() {
        return "設定";
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF16161C);
        JsonObject home = PhoneClient.CACHE.get("home");
        int yy = y + 24 - scroll;
        g.drawString(font, "§lテーマの色", x + 6, yy, 0xFFFFFF);
        yy += 12;
        for (int i = 0; i < PhoneTheme.THEMES.length; i++) {
            int t = i, bx = x + 6 + (i % 6) * 22, by = yy + (i / 6) * 22;
            if (num(home, "theme") == i) g.fill(bx - 2, by - 2, bx + 20, by + 20, 0xFFFFFFFF);
            g.fill(bx, by, bx + 18, by + 18, PhoneTheme.THEMES[i]);
            s.hot(bx, by, 18, 18, () -> request("settings", "set", obj("theme", t)));
        }
        yy += 48;
        g.drawString(font, "§7" + PhoneTheme.THEME_NAMES[Math.floorMod(num(home, "theme"), PhoneTheme.THEME_NAMES.length)], x + 6, yy - 4, 0xFFFFFF);
        yy += 8;
        g.drawString(font, "§l壁紙", x + 6, yy, 0xFFFFFF);
        yy += 12;
        for (int i = 0; i < PhoneTheme.WALL_NAMES.length; i++) {
            int t = i, bx = x + 6 + (i % 3) * 44, by = yy + (i / 3) * 40;
            if (num(home, "wallpaper") == i) g.fill(bx - 2, by - 2, bx + 40, by + 34, 0xFFFFFFFF);
            g.enableScissor(bx, by, bx + 38, by + 26);
            PhoneTheme.wallpaper(g, i, bx, by, 38, 26, s.ticks);
            g.disableScissor();
            g.pose().pushPose();
            g.pose().translate(bx + 19, by + 27, 0);
            g.pose().scale(0.7F, 0.7F, 1);
            g.drawCenteredString(font, PhoneTheme.WALL_NAMES[i], 0, 0, 0xFFFFFF);
            g.pose().popPose();
            s.hot(bx, by, 38, 32, () -> request("settings", "set", obj("wallpaper", t)));
        }
        yy += 124;
        boolean share = home == null || bool(home, "share");
        boolean notify = home == null || bool(home, "notify");
        g.drawString(font, "フレンドに場所を見せる", x + 6, yy + 3, 0xFFFFFF);
        s.button(g, x + w - 34, yy, 28, 14, share ? "ON" : "OFF", share ? 0xFF30A050 : 0xFF505058, mx, my, () -> request("settings", "set", obj("share", !share)));
        yy += 20;
        g.drawString(font, "通知", x + 6, yy + 3, 0xFFFFFF);
        s.button(g, x + w - 34, yy, 28, 14, notify ? "ON" : "OFF", notify ? 0xFF30A050 : 0xFF505058, mx, my, () -> request("settings", "set", obj("notify", !notify)));
        yy += 24;
        g.drawString(font, "§8ケースの色は、作業台で", x + 6, yy, 0xFFFFFF);
        g.drawString(font, "§8スマホと染料を並べて変えられる", x + 6, yy + 10, 0xFFFFFF);
        contentH = yy + scroll - y + 30;
        header(g, x, y, w, title(), mx, my);
    }
}
