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
    /** 載せる写真（写真アプリから来たとき） */
    private String attach = "";

    public SnsApp() {}

    public SnsApp(String photo) {
        attach = photo == null ? "" : photo;
    }

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
        if (box == null || (box.getValue().isBlank() && attach.isEmpty())) return false;
        JsonObject d = replyTo > 0 ? obj("text", box.getValue(), "reply", replyTo) : obj("text", box.getValue());
        if (!attach.isEmpty()) d.addProperty("photo", attach);
        request("sns", "post", d);
        attach = "";
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
            String photo = str(p, "photo");
            int pw = w - 30, phH = photo.isEmpty() ? 0 : Math.round(pw * com.chigusa.jsnphone.client.PhotoCache.aspect(photo)) + 4;
            int ph = 24 + lines.size() * 9 + 12 + phH + (lng(p, "reply") > 0 ? 9 : 0);
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
                if (!photo.isEmpty()) {
                    com.chigusa.jsnphone.client.PhotoCache.draw(g, photo, x + 26, ly + 1, pw, phH - 4, false);
                    ly += phH;
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
        if (!attach.isEmpty()) {
            int ah = replyTo > 0 ? 42 : 32;
            g.fill(x, y + h - 22 - ah, x + w, y + h - 22 - (replyTo > 0 ? 10 : 0), 0xFF2A2A34);
            com.chigusa.jsnphone.client.PhotoCache.draw(g, attach, x + 4, y + h - 20 - ah, 40, 28 - 2, true);
            g.drawString(font, "§7［写真］をつける", x + 48, y + h - 14 - ah, 0xFFFFFF);
            g.drawString(font, "§c✕", x + w - 10, y + h - 14 - ah, 0xFFFFFF);
            s.hot(x + w - 12, y + h - 16 - ah, 12, 12, () -> attach = "");
        }
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
