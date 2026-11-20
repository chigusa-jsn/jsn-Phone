package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.chigusa.jsnphone.client.PhoneTime;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** つぶやき：サーバーのみんなの掲示板（140文字・いいね・返信） */
public class SnsApp extends App {
    private EditBox box;
    private long replyTo;
    private String replyName = "";

    @Override
    public String title() {
        return "つぶやき";
    }

    @Override
    public void init() {
        box = s.editBox(s.sx + 4, s.sy + s.sh - 18, s.sw - 34, 14, 140, "いまどうしてる？");
    }

    @Override
    public void open() {
        request("sns", "list", null);
    }

    @Override
    public boolean enter() {
        if (box == null || box.getValue().isBlank()) return false;
        request("sns", "post", replyTo > 0 ? obj("text", box.getValue(), "reply", replyTo) : obj("text", box.getValue()));
        box.setValue("");
        replyTo = 0;
        scroll = 0;
        return true;
    }

    @Override
    public void tick() {
        if (s.ticks % 200 == 0) request("sns", "list", null);   // 10秒ごとに新しいつぶやき
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF101418);
        JsonObject o = PhoneClient.CACHE.get("sns");
        int yy = y + 22 - scroll;
        for (JsonElement e : arr(o, "posts")) {
            JsonObject p = e.getAsJsonObject();
            List<FormattedCharSequence> lines = wrap(str(p, "text"), w - 34);
            int ph = 24 + lines.size() * 9 + 12;
            if (yy + ph > y && yy < y + h) {
                avatar(g, str(p, "name"), x + 4, yy + 4, 18);
                g.drawString(font, "§l" + cut(str(p, "name"), w - 80), x + 26, yy + 4, 0xFFFFFF);
                String t = PhoneTime.ago(lng(p, "time"));
                g.drawString(font, "§8" + t, x + w - font.width(t) - 4, yy + 4, 0xFFFFFF);
                int ly = yy + 15;
                if (lng(p, "reply") > 0) {
                    g.drawString(font, "§8↪ 返信", x + 26, ly, 0xFFFFFF);
                    ly += 9;
                }
                for (FormattedCharSequence l : lines) {
                    g.drawString(font, l, x + 26, ly, 0xFFFFFF, false);
                    ly += 9;
                }
                long id = lng(p, "id");
                boolean liked = bool(p, "liked");
                String heart = (liked ? "§c♥ " : "§7♡ ") + num(p, "likes");
                g.drawString(font, heart, x + 26, ly + 1, 0xFFFFFF);
                s.hot(x + 24, ly, 30, 10, () -> request("sns", "like", obj("id", id)));
                g.drawString(font, "§7返信", x + 62, ly + 1, 0xFFFFFF);
                String n = str(p, "name");
                s.hot(x + 60, ly, 24, 10, () -> {
                    replyTo = id;
                    replyName = n;
                    box.setFocused(true);
                    s.setFocused(box);
                });
                if (bool(p, "mine")) {
                    g.drawString(font, "§8消す", x + w - 28, ly + 1, 0xFFFFFF);
                    s.hot(x + w - 30, ly, 26, 10, () -> request("sns", "delete", obj("id", id)));
                }
                g.fill(x, yy + ph - 1, x + w, yy + ph, 0x30FFFFFF);
            }
            yy += ph;
        }
        if (arr(o, "posts").isEmpty()) g.drawCenteredString(font, "§7まだつぶやきはないよ", x + w / 2, y + 60, 0xFFFFFF);
        contentH = yy + scroll - y + 24;
        g.fill(x, y + h - 22, x + w, y + h, 0xFF202028);
        if (replyTo > 0) {
            g.fill(x, y + h - 32, x + w, y + h - 22, 0xFF2A2A34);
            g.drawString(font, "§7↪ " + cut(replyName, w - 40) + " に返信", x + 4, y + h - 31, 0xFFFFFF);
            s.hot(x + w - 12, y + h - 32, 12, 10, () -> replyTo = 0);
            g.drawString(font, "§c✕", x + w - 10, y + h - 31, 0xFFFFFF);
        }
        String count = (box == null ? 0 : box.getValue().length()) + "/140";
        s.button(g, x + w - 28, y + h - 18, 24, 14, "投稿", s.accent(), mx, my, this::enter);
        header(g, x, y, w, title(), mx, my);
        g.pose().pushPose();
        g.pose().translate(x + w - 4, y + 6, 0);
        g.pose().scale(0.6F, 0.6F, 1);
        g.drawString(font, "§7" + count, -font.width(count), 0, 0xFFFFFF);
        g.pose().popPose();
    }
}
