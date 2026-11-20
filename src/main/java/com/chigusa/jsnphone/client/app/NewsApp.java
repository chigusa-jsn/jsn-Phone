package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.chigusa.jsnphone.client.PhoneTime;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** お知らせ：サーバーで起きたこと（進捗・レベル・ボス・カジノの大当たり など） */
public class NewsApp extends App {
    @Override
    public String title() {
        return "お知らせ";
    }

    @Override
    public void open() {
        request("news", "list", null);
    }

    static String mark(String kind) {
        return switch (kind) {
            case "level" -> "§a▲";
            case "boss" -> "§c☠";
            case "challenge" -> "§d★";
            case "goal" -> "§b◆";
            case "advancement" -> "§e●";
            case "join" -> "§f✿";
            case "casino" -> "§6♦";
            case "dungeon" -> "§5⚑";
            case "item" -> "§b✦";
            case "job" -> "§6✪";
            default -> "§7・";
        };
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF121216);
        JsonObject o = PhoneClient.CACHE.get("news");
        int yy = y + 22 - scroll;
        for (JsonElement e : arr(o, "news")) {
            JsonObject n = e.getAsJsonObject();
            List<FormattedCharSequence> lines = wrap(str(n, "text"), w - 22);
            int nh = lines.size() * 9 + 14;
            if (yy + nh > y && yy < y + h) {
                g.drawString(font, mark(str(n, "kind")), x + 4, yy + 3, 0xFFFFFF);
                int ly = yy + 3;
                for (FormattedCharSequence l : lines) {
                    g.drawString(font, l, x + 16, ly, 0xFFFFFF, false);
                    ly += 9;
                }
                g.pose().pushPose();
                g.pose().translate(x + 16, ly, 0);
                g.pose().scale(0.7F, 0.7F, 1);
                g.drawString(font, "§8" + PhoneTime.ago(lng(n, "time")), 0, 0, 0xFFFFFF);
                g.pose().popPose();
                g.fill(x + 4, yy + nh - 1, x + w - 4, yy + nh, 0x20FFFFFF);
            }
            yy += nh;
        }
        if (arr(o, "news").isEmpty()) g.drawCenteredString(font, "§7まだお知らせはないよ", x + w / 2, y + 60, 0xFFFFFF);
        contentH = yy + scroll - y;
        header(g, x, y, w, title(), mx, my);
    }
}
