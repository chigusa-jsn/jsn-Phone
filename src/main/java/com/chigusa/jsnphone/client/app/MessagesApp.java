package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.chigusa.jsnphone.client.PhoneTime;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** メッセージ：会話の一覧 → 会話（吹き出し）。オフラインの人にも送れる */
public class MessagesApp extends App {
    private String with = "", withName = "";
    private boolean picking;   // 新しく送る相手を選ぶ
    private EditBox box;

    public MessagesApp() {}

    /** いきなりこの人との会話を開く */
    public MessagesApp(String uuid, String name) {
        with = uuid;
        withName = name;
    }

    @Override
    public String title() {
        return "メッセージ";
    }

    @Override
    public void init() {
        box = null;
        if (!with.isEmpty()) {
            box = s.editBox(s.sx + 4, s.sy + s.sh - 18, s.sw - 34, 14, 200, "メッセージを入力");
        }
    }

    @Override
    public void open() {
        PhoneClient.CACHE.remove("messages");
        if (!with.isEmpty()) request("messages", "open", obj("with", with));
        else request("messages", "list", null);
    }

    private void openThread(String uuid, String name) {
        with = uuid;
        withName = name;
        picking = false;
        scroll = 0;
        s.clearWidgetsFor(this);
        init();
        request("messages", "open", obj("with", uuid));
    }

    @Override
    public boolean back() {
        if (picking) {
            picking = false;
            return true;
        }
        if (!with.isEmpty()) {
            with = "";
            s.clearWidgetsFor(this);
            init();
            request("messages", "list", null);
            return true;
        }
        return false;
    }

    @Override
    public boolean enter() {
        if (box == null || box.getValue().isBlank()) return false;
        request("messages", "send", obj("to", with, "text", box.getValue()));
        box.setValue("");
        return true;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF141418);
        JsonObject o = PhoneClient.CACHE.get("messages");
        if (!with.isEmpty()) {
            renderThread(g, x, y, w, h, mx, my, o);
            header(g, x, y, w, withName, mx, my);
            return;
        }
        int yy = y + 22 - scroll;
        if (picking) {
            for (JsonElement e : arr(o, "people")) {
                JsonObject p = e.getAsJsonObject();
                row(g, x, yy, w, mx, my, str(p, "name"), (bool(p, "online") ? "§aオンライン" : "§7オフライン") + (bool(p, "friend") ? " §b★フレンド" : ""), 0, 0);
                String id = str(p, "uuid"), n = str(p, "name");
                s.hot(x, yy, w, 26, () -> openThread(id, n));
                yy += 28;
            }
            if (arr(o, "people").isEmpty()) g.drawCenteredString(font, "§7送れる相手がいないよ", x + w / 2, yy + 10, 0xFFFFFF);
            contentH = yy + scroll - y;
            header(g, x, y, w, "だれに送る？", mx, my);
            return;
        }
        JsonArray threads = arr(o, "threads");
        for (JsonElement e : threads) {
            JsonObject t = e.getAsJsonObject();
            row(g, x, yy, w, mx, my, str(t, "name"), str(t, "last"), lng(t, "time"), num(t, "unread"));
            String id = str(t, "uuid"), n = str(t, "name");
            s.hot(x, yy, w, 26, () -> openThread(id, n));
            yy += 28;
        }
        if (threads.isEmpty()) g.drawCenteredString(font, "§7まだメッセージはないよ", x + w / 2, yy + 20, 0xFFFFFF);
        contentH = yy + scroll - y + 20;
        header(g, x, y, w, title(), mx, my);
        s.button(g, x + w - 30, y + h - 30, 24, 24, "§l＋", s.accent(), mx, my, () -> {
            picking = true;
            scroll = 0;
        });
    }

    private void row(GuiGraphics g, int x, int y, int w, int mx, int my, String name, String sub, long time, int unread) {
        if (mx >= x && mx < x + w && my >= y && my < y + 26) g.fill(x, y, x + w, y + 26, 0x20FFFFFF);
        avatar(g, name, x + 4, y + 3, 20);
        g.drawString(font, "§l" + cut(name, w - 70), x + 28, y + 3, 0xFFFFFF);
        g.drawString(font, cut(sub, w - 34), x + 28, y + 14, 0xFFB0B0B0);
        if (time > 0) {
            String t = PhoneTime.ago(time);
            g.drawString(font, "§8" + t, x + w - font.width(t) - 4, y + 3, 0xFFFFFF);
        }
        if (unread > 0) {
            g.fill(x + w - 14, y + 13, x + w - 4, y + 23, 0xFFE03030);
            g.drawCenteredString(font, String.valueOf(Math.min(9, unread)), x + w - 9, y + 14, 0xFFFFFF);
        }
        g.fill(x + 28, y + 26, x + w, y + 27, 0x20FFFFFF);
    }

    private void renderThread(GuiGraphics g, int x, int y, int w, int h, int mx, int my, JsonObject o) {
        JsonArray msgs = o != null && str(o, "with").equals(with) ? arr(o, "messages") : new JsonArray();
        long seen = lng(o, "seen");
        // 下から積む
        int bottom = y + h - 22, maxW = w * 2 / 3;
        int total = 0;
        for (JsonElement e : msgs) total += wrap(str(e.getAsJsonObject(), "text"), maxW - 8).size() * 9 + 12;
        contentH = total + 24;
        int yy = bottom - total + scroll;
        for (int i = 0; i < msgs.size(); i++) {
            JsonObject m = msgs.get(i).getAsJsonObject();
            boolean me = bool(m, "me");
            List<FormattedCharSequence> lines = wrap(str(m, "text"), maxW - 8);
            int bw = 0;
            for (FormattedCharSequence l : lines) bw = Math.max(bw, font.width(l));
            bw += 8;
            int bh = lines.size() * 9 + 6;
            int bx = me ? x + w - bw - 4 : x + 4;
            g.fill(bx, yy, bx + bw, yy + bh, me ? s.accent() : 0xFF34343C);
            int ly = yy + 3;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, bx + 4, ly, 0xFFFFFF, false);
                ly += 9;
            }
            String meta = PhoneTime.ago(lng(m, "time")) + (me && lng(m, "time") <= seen ? " 既読" : "");
            g.pose().pushPose();
            g.pose().translate(me ? bx - 2 : bx + bw + 2, yy + bh - 6, 0);
            g.pose().scale(0.6F, 0.6F, 1);
            g.drawString(font, "§7" + meta, me ? -font.width(meta) : 0, 0, 0xFFFFFF);
            g.pose().popPose();
            yy += bh + 6;
        }
        if (msgs.isEmpty()) g.drawCenteredString(font, "§7最初のメッセージを送ってみよう", x + w / 2, y + h / 2, 0xFFFFFF);
        g.fill(x, y + h - 22, x + w, y + h, 0xFF202028);
        s.button(g, x + w - 28, y + h - 18, 24, 14, "送信", s.accent(), mx, my, this::enter);
    }

    @Override
    public boolean scroll(double amount) {
        if (!with.isEmpty()) {
            int max = Math.max(0, contentH - (s.sh - 22));
            scroll = Math.max(0, Math.min(max, scroll + (int) (amount * 16)));
            return true;
        }
        return super.scroll(amount);
    }
}
