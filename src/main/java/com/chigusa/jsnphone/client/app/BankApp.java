package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 銀行：Lightman's Currency の口座の残高・送金・履歴 */
public class BankApp extends App {
    private int tab;   // 0=残高 1=送る 2=履歴
    private String to = "", toName = "";
    private long amount;
    private EditBox note;
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("M/d HH:mm").withZone(ZoneId.systemDefault());

    @Override
    public String title() {
        return "銀行";
    }

    @Override
    public void init() {
        note = s.editBox(s.sx + 6, s.sy + 200, s.sw - 12, 14, 40, "ひとこと（なくてもOK）");
        note.visible = tab == 1;
    }

    @Override
    public void open() {
        request("bank", "get", null);
    }

    @Override
    public void tick() {
        if (note != null) note.visible = tab == 1 && !to.isEmpty();
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF141A14);
        JsonObject o = PhoneClient.CACHE.get("bank");
        String[] tabs = {"残高", "送る", "履歴"};
        int tw = w / 3;
        for (int i = 0; i < 3; i++) {
            int t = i;
            s.button(g, x + i * tw, y + 19, i == 2 ? w - tw * 2 : tw, 14, tabs[i], tab == i ? 0xFFB08A20 : 0xFF2A2A24, mx, my, () -> {
                tab = t;
                scroll = 0;
            });
        }
        int top = y + 36;
        if (o != null && o.has("error") && !o.has("balance")) {
            for (var l : wrap("§c" + str(o, "error"), w - 12)) {
                g.drawString(font, l, x + 6, top + 10, 0xFFFFFF, false);
                top += 10;
            }
        } else {
            switch (tab) {
                case 0 -> balance(g, x, top, w, o);
                case 1 -> send(g, x, top, w, h - 36, mx, my, o);
                default -> log(g, x, top, w, h - 36, o);
            }
        }
        header(g, x, y, w, title(), mx, my);
    }

    private void balance(GuiGraphics g, int x, int y, int w, JsonObject o) {
        // カード
        g.fill(x + 8, y + 8, x + w - 8, y + 80, 0xFFB08A20);
        g.fill(x + 8, y + 8, x + w - 8, y + 30, 0xFFC8A030);
        g.drawString(font, "§lJSN BANK", x + 14, y + 14, 0xFF3A2A00, false);
        g.drawString(font, "§0残高", x + 14, y + 38, 0xFFFFFF, false);
        String t = str(o, "text");
        if (t.isEmpty()) t = "0";
        for (var l : wrap("§l" + t, w - 28)) {
            g.drawString(font, l, x + 14, y + 52, 0xFF201800, false);
            y += 10;
        }
        int yy = y + 96;
        if (o != null && o.has("msg")) {
            g.drawString(font, "§a" + cut(str(o, "msg"), w - 12), x + 6, yy, 0xFFFFFF, false);
            yy += 12;
        }
        for (var l : wrap("§7お金の出し入れは、銀行ATMでしてね。スマホでは送金ができるよ", w - 12)) {
            g.drawString(font, l, x + 6, yy, 0xFFFFFF, false);
            yy += 10;
        }
    }

    private void send(GuiGraphics g, int x, int y, int w, int h, int mx, int my, JsonObject o) {
        int yy = y + 4;
        if (to.isEmpty()) {
            g.drawString(font, "§7だれに送る？", x + 6, yy, 0xFFFFFF, false);
            yy += 12;
            g.enableScissor(x, yy, x + w, y + h);
            int base = yy;
            yy -= scroll;
            for (JsonElement e : arr(o, "people")) {
                JsonObject f = e.getAsJsonObject();
                String id = str(f, "uuid"), n = str(f, "name");
                avatar(g, n, x + 6, yy + 2, 16);
                g.drawString(font, cut(n, w - 40), x + 28, yy + 6, 0xFFFFFF, false);
                s.hot(x, Math.max(yy, base), w, 20, () -> {
                    to = id;
                    toName = n;
                    amount = 0;
                });
                yy += 22;
            }
            g.disableScissor();
            contentH = yy + scroll - y;
            return;
        }
        g.drawString(font, "§7送る相手", x + 6, yy, 0xFFFFFF, false);
        avatar(g, toName, x + 6, yy + 12, 16);
        g.drawString(font, "§l" + cut(toName, w - 70), x + 28, yy + 16, 0xFFFFFF, false);
        s.button(g, x + w - 40, yy + 12, 34, 14, "§7かえる", 0xFF2A2A24, mx, my, () -> to = "");
        yy += 36;
        g.drawString(font, "§7金額", x + 6, yy, 0xFFFFFF, false);
        yy += 11;
        g.fill(x + 6, yy, x + w - 6, yy + 18, 0xFF0A0E0A);
        g.drawCenteredString(font, "§e" + (amount <= 0 ? "0" : unitText(o, amount)), x + w / 2, yy + 5, 0xFFFFFF);
        yy += 22;
        // コインのボタン：左クリックでふやす、右クリックでへらす（下の行）
        var units = arr(o, "units");
        int n = Math.min(6, units.size());
        int bw = (w - 12) / 3;
        for (int i = 0; i < n; i++) {
            JsonObject u = units.get(i).getAsJsonObject();
            long v = lng(u, "value");
            int bx = x + 6 + (i % 3) * bw, by = yy + (i / 3) * 16;
            s.button(g, bx, by, bw - 2, 14, "+" + cut(str(u, "text"), bw - 10), 0xFF3A4A2A, mx, my, () -> amount += v);
        }
        yy += 34;
        s.button(g, x + 6, yy, (w - 12) / 2 - 1, 14, "§7クリア", 0xFF2A2A24, mx, my, () -> amount = 0);
        long bal = lng(o, "balance");
        s.button(g, x + w / 2 + 1, yy, (w - 12) / 2 - 1, 14, "§7ぜんぶ", 0xFF2A2A24, mx, my, () -> amount = Math.max(0, bal));
        yy += 20;
        // ひとこと欄は init で置いた場所（y+200）
        if (note != null) note.setY(s.sy + 36 + (yy - y));
        yy += 20;
        if (o != null && o.has("error")) {
            g.drawString(font, "§c" + cut(str(o, "error"), w - 12), x + 6, yy, 0xFFFFFF, false);
            yy += 11;
        }
        if (o != null && o.has("msg")) {
            g.drawString(font, "§a" + cut(str(o, "msg"), w - 12), x + 6, yy, 0xFFFFFF, false);
            yy += 11;
        }
        boolean can = amount > 0 && amount <= bal;
        s.button(g, x + 6, y + h - 22, w - 12, 18, can ? "§l送金する" : "§7送金する", can ? 0xFFB08A20 : 0xFF2A2A24, mx, my, () -> {
            if (amount <= 0 || amount > bal) return;
            request("bank", "send", obj("to", to, "amount", amount, "note", note == null ? "" : note.getValue()));
            amount = 0;
            if (note != null) note.setValue("");
        });
    }

    /** サーバーからもらった単位の名前で、金額を書く（例：3g 2i 5c） */
    static String unitText(JsonObject o, long amount) {
        var units = arr(o, "units");
        StringBuilder sb = new StringBuilder();
        long left = amount;
        for (int i = units.size() - 1; i >= 0; i--) {
            long v = lng(units.get(i).getAsJsonObject(), "value");
            if (v <= 0 || left < v) continue;
            long k = left / v;
            left -= k * v;
            String t = str(units.get(i).getAsJsonObject(), "text");
            // 「1g」→「3g」のように、先頭の 1 を数に置きかえる
            sb.append(sb.length() > 0 ? " " : "").append(t.startsWith("1") ? k + t.substring(1) : k + "×" + t);
        }
        return sb.length() == 0 ? String.valueOf(amount) : sb.toString();
    }

    private void log(GuiGraphics g, int x, int y, int w, int h, JsonObject o) {
        g.enableScissor(x, y, x + w, y + h);
        int yy = y + 4 - scroll;
        for (JsonElement e : arr(o, "log")) {
            JsonObject l = e.getAsJsonObject();
            boolean in = bool(l, "in");
            g.drawString(font, (in ? "§a＋ " : "§c－ ") + cut(str(l, "text"), w - 70), x + 6, yy, 0xFFFFFF, false);
            g.drawString(font, "§8" + F.format(Instant.ofEpochMilli(lng(l, "time"))), x + w - 50, yy, 0xFFFFFF, false);
            g.drawString(font, "§7" + (in ? "から " : "へ ") + cut(str(l, "name"), w - 40), x + 14, yy + 10, 0xFFFFFF, false);
            yy += 20;
            if (!str(l, "note").isEmpty()) {
                g.drawString(font, "§8「" + cut(str(l, "note"), w - 30) + "」", x + 14, yy, 0xFFFFFF, false);
                yy += 10;
            }
            g.fill(x + 4, yy + 1, x + w - 4, yy + 2, 0x20FFFFFF);
            yy += 4;
        }
        if (arr(o, "log").isEmpty()) g.drawCenteredString(font, "§7まだ送金の記録はないよ", x + w / 2, y + 30, 0xFFFFFF);
        g.disableScissor();
        contentH = yy + scroll - y;
    }
}
