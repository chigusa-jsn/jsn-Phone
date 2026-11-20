package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/** フレンド：フレンドの一覧・申請・さがす。レーダーで、フレンドのいる方向と距離が見える */
public class FriendsApp extends App {
    private int tab;   // 0=フレンド 1=レーダー 2=申請 3=さがす
    private static final String[] TABS = {"フレンド", "レーダー", "申請", "さがす"};
    private int range = 256;

    @Override
    public String title() {
        return "フレンド";
    }

    @Override
    public void open() {
        request("friends", "list", null);
    }

    @Override
    public void tick() {
        if (s.ticks % 40 == 0) request("friends", "list", null);   // 2秒ごとに位置を更新
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF121620);
        JsonObject o = PhoneClient.CACHE.get("friends");
        int tw = w / 4;
        for (int i = 0; i < 4; i++) {
            int t = i;
            String label = TABS[i] + (i == 2 && !arr(o, "requests").isEmpty() ? "§c●" : "");
            s.button(g, x + i * tw, y + 19, tw, 14, label, tab == i ? s.accent() : 0xFF2A2A34, mx, my, () -> {
                tab = t;
                scroll = 0;
            });
        }
        int top = y + 36;
        g.enableScissor(x, top, x + w, y + h);
        switch (tab) {
            case 0 -> list(g, x, top, w, mx, my, o);
            case 1 -> radar(g, x, top, w, h - 36, mx, my, o);
            case 2 -> requests(g, x, top, w, mx, my, o);
            default -> others(g, x, top, w, mx, my, o);
        }
        g.disableScissor();
        header(g, x, y, w, title(), mx, my);
    }

    private String where(JsonObject f) {
        Player me = Minecraft.getInstance().player;
        if (!bool(f, "online")) return "§7オフライン";
        if (!f.has("x")) return "§aオンライン §8（場所はひみつ）";
        String dim = str(f, "dim");
        if (me == null || !dim.equals(me.level().dimension().location().toString())) return "§aオンライン §7（" + dimName(dim) + "）";
        double dx = f.get("x").getAsDouble() - me.getX(), dz = f.get("z").getAsDouble() - me.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        return "§a" + dist + "m §7" + dir(dx, dz) + "  §8" + (int) f.get("x").getAsDouble() + ", " + (int) f.get("y").getAsDouble() + ", " + (int) f.get("z").getAsDouble();
    }

    static String dimName(String d) {
        return switch (d) {
            case "minecraft:overworld" -> "地上";
            case "minecraft:the_nether" -> "ネザー";
            case "minecraft:the_end" -> "エンド";
            default -> d.contains(":") ? d.substring(d.indexOf(':') + 1) : d;
        };
    }

    static String dir(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        String[] n = {"北", "北東", "東", "南東", "南", "南西", "西", "北西"};
        return n[(int) Math.floorMod(Math.round(a / 45), 8)];
    }

    private void list(GuiGraphics g, int x, int y, int w, int mx, int my, JsonObject o) {
        int yy = y + 2 - scroll;
        for (JsonElement e : arr(o, "friends")) {
            JsonObject f = e.getAsJsonObject();
            avatar(g, str(f, "name"), x + 4, yy + 3, 18);
            g.fill(x + 18, yy + 15, x + 23, yy + 20, bool(f, "online") ? 0xFF40E040 : 0xFF707070);
            g.drawString(font, "§l" + cut(str(f, "name"), w - 60), x + 26, yy + 2, 0xFFFFFF);
            g.pose().pushPose();
            g.pose().translate(x + 26, yy + 13, 0);
            g.pose().scale(0.8F, 0.8F, 1);
            g.drawString(font, cut(where(f), (int) ((w - 30) / 0.8)), 0, 0, 0xFFFFFF);
            g.pose().popPose();
            String id = str(f, "uuid"), n = str(f, "name");
            s.button(g, x + w - 26, yy + 2, 22, 10, "§7✉", 0xFF2A2A34, mx, my, () -> s.openApp(new MessagesApp(id, n)));
            yy += 26;
        }
        if (arr(o, "friends").isEmpty()) g.drawCenteredString(font, "§7「さがす」でフレンドをふやそう", x + w / 2, y + 30, 0xFFFFFF);
        contentH = yy + scroll - y + 40;
    }

    private void radar(GuiGraphics g, int x, int y, int w, int h, int mx, int my, JsonObject o) {
        Player me = Minecraft.getInstance().player;
        int cx = x + w / 2, cy = y + w / 2 + 4, r = w / 2 - 8;
        for (int rr = r; rr > 0; rr -= r / 3) circle(g, cx, cy, rr, 0x6040E080);
        g.fill(cx - r, cy, cx + r, cy + 1, 0x3040E080);
        g.fill(cx, cy - r, cx + 1, cy + r, 0x3040E080);
        // 回る線
        double sweep = (s.ticks % 60) / 60.0 * Math.PI * 2;
        for (int i = 0; i < r; i += 2) g.fill(cx + (int) (Math.sin(sweep) * i), cy - (int) (Math.cos(sweep) * i), cx + (int) (Math.sin(sweep) * i) + 1, cy - (int) (Math.cos(sweep) * i) + 1, 0x8040FF80);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        g.drawCenteredString(font, "§7↑ 向いている方", cx, y + 2, 0xFFFFFF);
        if (me != null) {
            double yaw = Math.toRadians(me.getYRot());
            String myDim = me.level().dimension().location().toString();
            int listY = cy + r + 8;
            for (JsonElement e : arr(o, "friends")) {
                JsonObject f = e.getAsJsonObject();
                if (!f.has("x")) continue;
                if (!str(f, "dim").equals(myDim)) {
                    g.drawString(font, "§7" + cut(str(f, "name"), w - 60) + "：" + dimName(str(f, "dim")), x + 4, listY, 0xFFFFFF);
                    listY += 10;
                    continue;
                }
                double dx = f.get("x").getAsDouble() - me.getX(), dz = f.get("z").getAsDouble() - me.getZ();
                // 自分の向きに合わせて回す（上が前）
                double rx = dx * Math.cos(yaw) + dz * Math.sin(yaw), rz = -dx * Math.sin(yaw) + dz * Math.cos(yaw);
                double dist = Math.sqrt(dx * dx + dz * dz);
                double k = Math.min(1, dist / range);
                double ang = Math.atan2(rx, rz);
                int px = cx - (int) (Math.sin(ang) * k * r), py = cy - (int) (Math.cos(ang) * k * r);
                g.fill(px - 2, py - 2, px + 3, py + 3, dist > range ? 0xFFF0A040 : 0xFF40E0FF);
                g.pose().pushPose();
                g.pose().translate(px + 4, py - 3, 0);
                g.pose().scale(0.7F, 0.7F, 1);
                g.drawString(font, str(f, "name") + " " + (int) dist + "m", 0, 0, 0xFFFFFF);
                g.pose().popPose();
            }
            s.button(g, x + 4, cy + r - 6, 46, 12, "§7範囲 " + range, 0xFF2A2A34, mx, my, () -> range = range >= 2048 ? 64 : range * 2);
        }
    }

    private static void circle(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int i = 0; i < 360; i += 3) {
            double a = Math.toRadians(i);
            int px = cx + (int) (Math.cos(a) * r), py = cy + (int) (Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, col);
        }
    }

    private void requests(GuiGraphics g, int x, int y, int w, int mx, int my, JsonObject o) {
        int yy = y + 2 - scroll;
        for (JsonElement e : arr(o, "requests")) {
            JsonObject f = e.getAsJsonObject();
            String id = str(f, "uuid");
            avatar(g, str(f, "name"), x + 4, yy + 3, 18);
            g.drawString(font, cut(str(f, "name"), w - 90), x + 26, yy + 8, 0xFFFFFF);
            s.button(g, x + w - 60, yy + 4, 28, 14, "OK", 0xFF30A050, mx, my, () -> request("friends", "accept", obj("uuid", id)));
            s.button(g, x + w - 30, yy + 4, 26, 14, "✕", 0xFF803030, mx, my, () -> request("friends", "decline", obj("uuid", id)));
            yy += 24;
        }
        if (arr(o, "requests").isEmpty()) g.drawCenteredString(font, "§7申請はとどいていないよ", x + w / 2, y + 30, 0xFFFFFF);
        contentH = yy + scroll - y;
    }

    private void others(GuiGraphics g, int x, int y, int w, int mx, int my, JsonObject o) {
        int yy = y + 2 - scroll;
        g.drawString(font, "§7いまオンラインの人", x + 4, yy, 0xFFFFFF);
        yy += 12;
        for (JsonElement e : arr(o, "others")) {
            JsonObject f = e.getAsJsonObject();
            String id = str(f, "uuid");
            avatar(g, str(f, "name"), x + 4, yy + 3, 18);
            g.drawString(font, cut(str(f, "name"), w - 80), x + 26, yy + 8, 0xFFFFFF);
            if (bool(f, "sent")) g.drawString(font, "§7申請ずみ", x + w - 44, yy + 8, 0xFFFFFF);
            else s.button(g, x + w - 44, yy + 4, 40, 14, "申請", s.accent(), mx, my, () -> request("friends", "request", obj("uuid", id)));
            yy += 24;
        }
        if (arr(o, "others").isEmpty()) g.drawCenteredString(font, "§7ほかにオンラインの人はいないよ", x + w / 2, yy + 10, 0xFFFFFF);
        contentH = yy + scroll - y;
    }
}
