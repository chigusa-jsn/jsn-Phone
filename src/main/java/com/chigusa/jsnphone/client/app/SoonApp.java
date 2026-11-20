package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;

/** まだ作っていないアプリ（電話・カメラ・写真・音楽・銀行・jsn-Craft） */
public class SoonApp extends App {
    private final String id;

    public SoonApp(String id) {
        this.id = id;
    }

    @Override
    public String title() {
        return Apps.name(id);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF16161C);
        JsonObject av = PhoneClient.CACHE.get("home");
        av = av != null && av.has("apps") ? av.getAsJsonObject("apps") : new JsonObject();
        String need = switch (id) {
            case "call" -> bool(av, "voicechat") ? "" : "シンプルボイスチャットMODが必要だよ";
            case "bank" -> bool(av, "lightman") ? "" : "Lightman's Currency MODが必要だよ";
            case "jsncraft" -> bool(av, "jsncraft") ? "" : "jsn-Craft MODが必要だよ";
            default -> "";
        };
        g.drawCenteredString(font, "§l" + title(), x + w / 2, y + 70, 0xFFFFFF);
        g.drawCenteredString(font, "§7じゅんび中…", x + w / 2, y + 90, 0xFFFFFF);
        if (!need.isEmpty()) {
            int ly = y + 112;
            for (var line : wrap(need, w - 16)) {
                g.drawString(font, line, x + 8, ly, 0xFFF0A040, false);
                ly += 10;
            }
        }
        header(g, x, y, w, title(), mx, my);
    }
}
