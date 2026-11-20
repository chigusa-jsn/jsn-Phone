package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;

/** 電話：連絡先からかける・着信に出る・通話中の時間・切る（声はシンプルボイスチャット） */
public class CallApp extends App {

    @Override
    public String title() {
        return "電話";
    }

    @Override
    public void open() {
        request("call", "state", null);
    }

    @Override
    public void tick() {
        if (s.ticks % 40 == 0) request("call", "state", null);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        JsonObject o = PhoneClient.CACHE.get("call");
        String state = str(o, "state");
        if (state.equals("ringing") || state.equals("calling") || state.equals("talking")) {
            inCall(g, x, y, w, h, mx, my, o, state);
            return;
        }
        g.fill(x, y, x + w, y + h, 0xFF101418);
        int yy = y + 22;
        if (o != null && !bool(o, "voice")) {
            for (var l : wrap("§eシンプルボイスチャットMODが入っていないので、電話はつかえないよ", w - 12)) {
                g.drawString(font, l, x + 6, yy, 0xFFFFFF, false);
                yy += 10;
            }
            yy += 4;
        } else if (o != null && !bool(o, "myVoice")) {
            for (var l : wrap("§7ボイスチャットにつながっていないみたい（Vキーで設定）", w - 12)) {
                g.drawString(font, l, x + 6, yy, 0xFFFFFF, false);
                yy += 10;
            }
            yy += 4;
        }
        if (o != null && o.has("error")) {
            g.drawString(font, "§c" + cut(str(o, "error"), w - 12), x + 6, yy, 0xFFFFFF, false);
            yy += 12;
        }
        g.drawString(font, "§7連絡先", x + 6, yy, 0xFFFFFF, false);
        yy += 12;
        int top = yy;
        g.enableScissor(x, top, x + w, y + h);
        yy -= scroll;
        int count = 0;
        for (JsonElement e : arr(o, "people")) {
            JsonObject f = e.getAsJsonObject();
            String id = str(f, "uuid"), name = str(f, "name");
            boolean on = bool(f, "online");
            avatar(g, name, x + 6, yy + 2, 18);
            g.drawString(font, (on ? "" : "§7") + cut(name, w - 70), x + 28, yy + 3, 0xFFFFFF, false);
            g.drawString(font, on ? "§aオンライン" : "§8オフライン", x + 28, yy + 13, 0xFFFFFF, false);
            if (on) s.button(g, x + w - 32, yy + 4, 26, 14, "☎", 0xFF30A050, mx, my, () -> {
                request("call", "dial", obj("to", id));
                s.sound(1.2F);
            });
            yy += 24;
            count++;
        }
        g.disableScissor();
        if (count == 0) g.drawCenteredString(font, "§7かけられる人がいないよ", x + w / 2, top + 20, 0xFFFFFF);
        contentH = yy + scroll - y;
        header(g, x, y, w, title(), mx, my);
    }

    private void inCall(GuiGraphics g, int x, int y, int w, int h, int mx, int my, JsonObject o, String state) {
        // 背景：上から下へのグラデーション
        for (int i = 0; i < h; i += 4) {
            float t = i / (float) h;
            int c = 0xFF000000 | (int) (0x18 + 0x10 * t) << 16 | (int) (0x30 + 0x30 * t) << 8 | (int) (0x40 + 0x10 * t);
            g.fill(x, y + i, x + w, y + i + 4, c);
        }
        String name = str(o, "name");
        int cx = x + w / 2;
        // 呼び出し中は輪っかが広がる
        if (!state.equals("talking")) {
            int r = 22 + (s.ticks % 30);
            int a = (int) (0x80 * (1 - (s.ticks % 30) / 30.0));
            ring(g, cx, y + 70, r, a << 24 | 0x80E0FF);
        }
        g.pose().pushPose();
        avatar(g, name, cx - 20, y + 50, 40);
        g.pose().popPose();
        g.drawCenteredString(font, "§l" + cut(name, w - 10), cx, y + 100, 0xFFFFFF);
        String sub = switch (state) {
            case "ringing" -> "着信中" + ".".repeat(s.ticks / 10 % 4);
            case "calling" -> "呼び出し中" + ".".repeat(s.ticks / 10 % 4);
            default -> {
                long ms = lng(o, "talkMs") + (System.currentTimeMillis() - lng(o, "_at"));
                long sec = Math.max(0, ms / 1000);
                yield String.format("%d:%02d", sec / 60, sec % 60);
            }
        };
        g.drawCenteredString(font, "§7" + sub, cx, y + 114, 0xFFFFFF);
        if (state.equals("talking")) {
            boolean mine = bool(o, "myVoice"), theirs = !o.has("otherVoice") || bool(o, "otherVoice");
            if (!mine) g.drawCenteredString(font, "§eボイスチャットにつながっていないよ", cx, y + 130, 0xFFFFFF);
            else if (!theirs) g.drawCenteredString(font, "§e相手がボイスチャットにいないみたい", cx, y + 130, 0xFFFFFF);
            else {
                // 声の波
                for (int i = -5; i <= 5; i++) {
                    int bh = 2 + (int) (Math.abs(Math.sin(s.ticks * 0.3 + i)) * 8);
                    g.fill(cx + i * 5 - 1, y + 136 - bh, cx + i * 5 + 2, y + 136 + bh, 0xC080E0A0);
                }
            }
        }
        int by = y + h - 50;
        if (state.equals("ringing")) {
            s.button(g, x + 14, by, 50, 30, "§l出る", 0xFF30A050, mx, my, () -> request("call", "answer", null));
            s.button(g, x + w - 64, by, 50, 30, "§l断る", 0xFFC03030, mx, my, () -> request("call", "hangup", null));
        } else {
            s.button(g, cx - 30, by, 60, 30, "§l切る", 0xFFC03030, mx, my, () -> request("call", "hangup", null));
        }
    }

    private static void ring(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int i = 0; i < 360; i += 4) {
            double a = Math.toRadians(i);
            int px = cx + (int) (Math.cos(a) * r), py = cy + (int) (Math.sin(a) * r);
            g.fill(px, py, px + 2, py + 2, col);
        }
    }
}
