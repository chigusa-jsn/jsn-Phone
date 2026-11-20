package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.chigusa.jsnphone.client.PhoneScreen;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** スマホのアプリの元。画面の中（x, y, w, h）に描く。 */
public abstract class App {
    protected PhoneScreen s;
    protected Font font;
    protected int scroll;
    /** 中身の高さ（スクロールの上限に使う） */
    protected int contentH;

    public void attach(PhoneScreen s) {
        this.s = s;
        this.font = net.minecraft.client.Minecraft.getInstance().font;
    }

    public abstract String title();

    /** 部品（文字入力など）を置く。画面の大きさが変わったときも呼ばれる */
    public void init() {}

    /** アプリを開いたとき（データを取りに行く） */
    public void open() {}

    public void tick() {}

    public abstract void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt);

    public void onData(String app, JsonObject o) {}

    public boolean click(double mx, double my, int button) {
        return false;
    }

    public boolean scroll(double amount) {
        int max = Math.max(0, contentH - (s.sh - 22));
        scroll = Math.max(0, Math.min(max, scroll - (int) (amount * 16)));
        return true;
    }

    /** Enter キー */
    public boolean enter() {
        return false;
    }

    /** 戻る（Esc）。アプリの中で戻れたら true */
    public boolean back() {
        return false;
    }

    // ---------------- 道具 ----------------
    protected void header(GuiGraphics g, int x, int y, int w, String title, int mx, int my) {
        g.fill(x, y, x + w, y + 18, 0xF0202028);
        g.fill(x, y + 18, x + w, y + 19, s.accent());
        g.drawCenteredString(font, "§l" + title, x + w / 2, y + 5, 0xFFFFFF);
        boolean hover = mx >= x && mx < x + 18 && my >= y && my < y + 18;
        g.drawString(font, hover ? "§e＜" : "＜", x + 4, y + 5, 0xFFFFFF);
        s.hot(x, y, 18, 18, () -> {
            if (!back()) s.goHome();
        });
    }

    protected List<FormattedCharSequence> wrap(String text, int width) {
        return font.split(FormattedText.of(text), width);
    }

    protected void request(String app, String action, JsonObject data) {
        PhoneClient.request(app, action, data);
    }

    protected static JsonObject obj(Object... kv) {
        JsonObject o = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Number n) o.addProperty((String) kv[i], n);
            else if (v instanceof Boolean b) o.addProperty((String) kv[i], b);
            else o.addProperty((String) kv[i], String.valueOf(v));
        }
        return o;
    }

    protected static String str(JsonObject o, String k) {
        JsonElement e = o == null ? null : o.get(k);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    protected static int num(JsonObject o, String k) {
        JsonElement e = o == null ? null : o.get(k);
        return e == null || e.isJsonNull() ? 0 : e.getAsInt();
    }

    protected static long lng(JsonObject o, String k) {
        JsonElement e = o == null ? null : o.get(k);
        return e == null || e.isJsonNull() ? 0 : e.getAsLong();
    }

    protected static boolean bool(JsonObject o, String k) {
        JsonElement e = o == null ? null : o.get(k);
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }

    protected static JsonArray arr(JsonObject o, String k) {
        JsonElement e = o == null ? null : o.get(k);
        return e == null || !e.isJsonArray() ? new JsonArray() : e.getAsJsonArray();
    }

    /** 文字の長いものを切る */
    protected String cut(String t, int width) {
        if (font.width(t) <= width) return t;
        return font.plainSubstrByWidth(t, width - font.width("…")) + "…";
    }

    /** まるいアイコン（名前の頭文字） */
    protected void avatar(GuiGraphics g, String name, int x, int y, int size) {
        int col = 0xFF000000 | (name.hashCode() * 0x9E3779B1 >>> 8 & 0x7F7F7F) + 0x404040;
        g.fill(x + 1, y, x + size - 1, y + size, col);
        g.fill(x, y + 1, x + size, y + size - 1, col);
        String c = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        g.drawCenteredString(font, c, x + size / 2, y + (size - 8) / 2, 0xFFFFFF);
    }
}
