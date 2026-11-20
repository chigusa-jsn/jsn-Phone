package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;

/** jsn-Craft：いまの職業・レベル・ステータス・スキル・カジノの記録 */
public class CraftApp extends App {
    private int tab;   // 0=職業 1=スキル 2=カジノ
    private static final String[] TABS = {"職業", "スキル", "カジノ"};

    @Override
    public String title() {
        return "jsn-Craft";
    }

    @Override
    public void open() {
        request("jsncraft", "get", null);
    }

    @Override
    public void tick() {
        if (s.ticks % 60 == 0) request("jsncraft", "get", null);
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF16121E);
        JsonObject o = PhoneClient.CACHE.get("jsncraft");
        int tw = w / 3;
        for (int i = 0; i < 3; i++) {
            int t = i;
            s.button(g, x + i * tw, y + 19, i == 2 ? w - tw * 2 : tw, 14, TABS[i], tab == i ? 0xFF7A40C0 : 0xFF2A2434, mx, my, () -> {
                tab = t;
                scroll = 0;
            });
        }
        int top = y + 36;
        g.enableScissor(x, top, x + w, y + h);
        if (o == null) g.drawCenteredString(font, "§7よみこみ中…", x + w / 2, top + 30, 0xFFFFFF);
        else if (o.has("error")) g.drawCenteredString(font, "§c" + str(o, "error"), x + w / 2, top + 30, 0xFFFFFF);
        else switch (tab) {
            case 0 -> jobs(g, x, top, w, o);
            case 1 -> skills(g, x, top, w, o);
            default -> casino(g, x, top, w, o);
        }
        g.disableScissor();
        header(g, x, y, w, title(), mx, my);
    }

    private int bar(GuiGraphics g, int x, int y, int w, String label, long v, long max, int color) {
        g.drawString(font, label, x, y, 0xFFFFFF, false);
        String t = v + " / " + max;
        g.drawString(font, "§7" + t, x + w - font.width(t), y, 0xFFFFFF, false);
        g.fill(x, y + 10, x + w, y + 14, 0xFF000000);
        if (max > 0) g.fill(x, y + 10, x + (int) (w * Math.min(1, v / (double) max)), y + 14, color);
        return y + 18;
    }

    private void jobs(GuiGraphics g, int x, int y, int w, JsonObject o) {
        int yy = y + 4 - scroll;
        if (!str(o, "title").isEmpty()) {
            g.drawCenteredString(font, "§6《" + cut(str(o, "title"), w - 20) + "》", x + w / 2, yy, 0xFFFFFF);
            yy += 13;
        }
        String[][] kinds = {{"combat", "⚔ 戦闘職", "FFE05050"}, {"life", "⚒ 生活職", "FF50C070"}};
        for (String[] k : kinds) {
            JsonObject j = o.has(k[0]) ? o.getAsJsonObject(k[0]) : new JsonObject();
            g.fill(x + 4, yy, x + w - 4, yy + 40, 0xFF221C2E);
            g.fill(x + 4, yy, x + 6, yy + 40, (int) Long.parseLong(k[2], 16));
            g.drawString(font, "§7" + k[1], x + 10, yy + 3, 0xFFFFFF, false);
            if (!j.has("name")) {
                g.drawString(font, "§8まだ就いていない", x + 10, yy + 16, 0xFFFFFF, false);
            } else {
                g.drawString(font, "§l" + cut(str(j, "name"), w - 70), x + 10, yy + 14, 0xFFFFFF, false);
                String lv = "Lv." + num(j, "level") + (num(j, "level") >= num(j, "max") ? " §6MAX" : "");
                g.drawString(font, lv, x + w - 10 - font.width(lv), yy + 14, 0xFFFFFF, false);
                int next = num(j, "next");
                int bw = w - 20;
                g.fill(x + 10, yy + 28, x + 10 + bw, yy + 32, 0xFF000000);
                if (next > 0) g.fill(x + 10, yy + 28, x + 10 + (int) (bw * Math.min(1, num(j, "xp") / (double) next)), yy + 32, 0xFF9060F0);
                else g.fill(x + 10, yy + 28, x + 10 + bw, yy + 32, 0xFFE0B040);
            }
            yy += 44;
        }
        JsonObject st = o.has("stats") ? o.getAsJsonObject("stats") : new JsonObject();
        yy += 2;
        yy = bar(g, x + 6, yy, w - 12, "§c❤ HP", lng(st, "hp"), lng(st, "maxHp"), 0xFFE04040);
        yy = bar(g, x + 6, yy, w - 12, "§9❤ マナ", lng(st, "mana"), lng(st, "maxMana"), 0xFF4070F0);
        yy = bar(g, x + 6, yy, w - 12, "§e❤ スタミナ", lng(st, "stamina"), lng(st, "maxStamina"), 0xFFE0C030);
        g.drawString(font, "§a☘ 運 " + (st.has("luck") ? st.get("luck").getAsString() : "0"), x + 6, yy, 0xFFFFFF, false);
        yy += 14;
        g.drawString(font, "§7これまでの職業", x + 6, yy, 0xFFFFFF, false);
        yy += 11;
        for (JsonElement e : arr(o, "jobs")) {
            JsonObject j = e.getAsJsonObject();
            boolean combat = str(j, "type").equals("combat");
            g.drawString(font, (combat ? "§c⚔ " : "§a⚒ ") + "§f" + cut(str(j, "name"), w - 60), x + 8, yy, 0xFFFFFF, false);
            String lv = "§7Lv." + num(j, "level");
            g.drawString(font, lv, x + w - 8 - font.width(lv), yy, 0xFFFFFF, false);
            yy += 10;
        }
        contentH = yy + scroll - y + 10;
    }

    private void skills(GuiGraphics g, int x, int y, int w, JsonObject o) {
        int yy = y + 4 - scroll;
        g.drawString(font, "§7セットしているスキル", x + 6, yy, 0xFFFFFF, false);
        yy += 12;
        int i = 1;
        for (JsonElement e : arr(o, "skills")) {
            g.fill(x + 4, yy, x + w - 4, yy + 14, 0xFF221C2E);
            g.drawString(font, "§d" + i + " §f" + cut(e.getAsString(), w - 24), x + 8, yy + 3, 0xFFFFFF, false);
            yy += 16;
            i++;
        }
        if (arr(o, "skills").isEmpty()) g.drawString(font, "§8まだないよ（Kキーでスキルボード）", x + 6, yy, 0xFFFFFF, false);
        contentH = yy + scroll - y + 10;
    }

    private void casino(GuiGraphics g, int x, int y, int w, JsonObject o) {
        JsonObject c = o.has("casino") ? o.getAsJsonObject("casino") : new JsonObject();
        int yy = y + 6 - scroll;
        g.fill(x + 8, yy, x + w - 8, yy + 34, 0xFF3A1020);
        g.drawCenteredString(font, "§7持っているチップ", x + w / 2, yy + 4, 0xFFFFFF);
        g.drawCenteredString(font, "§6§l" + lng(c, "chips") + " 枚", x + w / 2, yy + 18, 0xFFFFFF);
        yy += 42;
        long bet = lng(c, "bet"), won = lng(c, "won");
        String[][] rows = {
                {"遊んだ回数", lng(c, "plays") + " 回"},
                {"かけた合計", bet + " 枚"},
                {"もどった合計", won + " 枚"},
                {"収支", (won - bet >= 0 ? "§a+" : "§c") + (won - bet) + " 枚"},
                {"いちばんの当たり", lng(c, "best") + " 枚"},
                {"大当たり", lng(c, "jackpots") + " 回"},
                {"還元率", bet > 0 ? String.format("%.1f%%", won * 100.0 / bet) : "-"}};
        for (String[] r : rows) {
            g.drawString(font, "§7" + r[0], x + 10, yy, 0xFFFFFF, false);
            g.drawString(font, r[1], x + w - 10 - font.width(r[1]), yy, 0xFFFFFF, false);
            g.fill(x + 8, yy + 11, x + w - 8, yy + 12, 0x20FFFFFF);
            yy += 15;
        }
        contentH = yy + scroll - y + 10;
    }
}
