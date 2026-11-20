package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.chigusa.jsnphone.client.PhotoCache;
import com.chigusa.jsnphone.client.PhoneTime;
import com.chigusa.jsnphone.client.camera.CameraMode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 写真：撮った写真の一覧。開くと大きく見られて、つぶやく・メッセージで送る・消すができる */
public class PhotosApp extends App {
    private String open = "";     // 大きく見ている写真
    private boolean picking;      // 送る相手をえらび中
    private boolean confirmDelete;
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm").withZone(ZoneId.systemDefault());

    @Override
    public String title() {
        return "写真";
    }

    @Override
    public void open() {
        request("photos", "list", null);
    }

    @Override
    public boolean back() {
        if (picking) {
            picking = false;
            return true;
        }
        if (!open.isEmpty()) {
            open = "";
            confirmDelete = false;
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF0C0C10);
        JsonObject o = PhoneClient.CACHE.get("photos");
        JsonArray photos = arr(o, "photos");
        if (!open.isEmpty()) {
            JsonObject ph = null;
            for (JsonElement e : photos) if (str(e.getAsJsonObject(), "id").equals(open)) ph = e.getAsJsonObject();
            if (ph == null) open = "";
            else {
                if (picking) pick(g, x, y, w, h, mx, my, o);
                else detail(g, x, y, w, h, mx, my, ph);
                return;
            }
        }
        int cols = 3, gap = 2, cw = (w - gap * (cols + 1)) / cols;
        int top = y + 20;
        g.enableScissor(x, top, x + w, y + h);
        int yy = top + gap - scroll;
        for (int i = 0; i < photos.size(); i++) {
            JsonObject ph = photos.get(i).getAsJsonObject();
            int cx = x + gap + (i % cols) * (cw + gap), cy = yy + (i / cols) * (cw + gap);
            if (cy + cw > top && cy < y + h) {
                String id = str(ph, "id");
                PhotoCache.draw(g, id, cx, cy, cw, cw, true);
                if (mx >= cx && mx < cx + cw && my >= cy && my < cy + cw && my >= top) g.fill(cx, cy, cx + cw, cy + cw, 0x30FFFFFF);
                if (cy >= top) s.hot(cx, cy, cw, cw, () -> {
                    open = id;
                    confirmDelete = false;
                });
            }
        }
        g.disableScissor();
        int rows = (photos.size() + cols - 1) / cols;
        contentH = rows * (cw + gap) + 40;
        if (photos.isEmpty()) {
            g.drawCenteredString(font, "§7まだ写真はないよ", x + w / 2, y + 70, 0xFFFFFF);
            g.drawCenteredString(font, "§8カメラで撮ってみよう", x + w / 2, y + 84, 0xFFFFFF);
        }
        String c = photos.size() + " / " + num(o, "max");
        header(g, x, y, w, title(), mx, my);
        g.drawString(font, "§7" + c, x + w - 4 - font.width(c), y + 5, 0xFFFFFF, false);
        s.button(g, x + w - 30, y + h - 30, 26, 26, "📷", s.accent(), mx, my, CameraMode::startSoon);
    }

    private void detail(GuiGraphics g, int x, int y, int w, int h, int mx, int my, JsonObject ph) {
        String id = str(ph, "id");
        int ih = Math.round(w * PhotoCache.aspect(id));
        int iy = y + 22;
        PhotoCache.draw(g, id, x, iy, w, ih, false);
        int yy = iy + ih + 6;
        g.drawString(font, "§f" + F.format(Instant.ofEpochMilli(lng(ph, "time"))) + " §8" + PhoneTime.ago(lng(ph, "time")), x + 6, yy, 0xFFFFFF, false);
        yy += 11;
        for (var l : wrap("§7📍 " + str(ph, "place"), w - 12)) {
            g.drawString(font, l, x + 6, yy, 0xFFFFFF, false);
            yy += 10;
        }
        yy += 6;
        int bw = w - 12;
        s.button(g, x + 6, yy, bw, 18, "つぶやきに載せる", 0xFF3A8AE0, mx, my, () -> s.openApp(new SnsApp(id)));
        yy += 22;
        s.button(g, x + 6, yy, bw, 18, "メッセージで送る", 0xFF30A050, mx, my, () -> {
            picking = true;
            scroll = 0;
        });
        yy += 22;
        if (confirmDelete) {
            s.button(g, x + 6, yy, bw / 2 - 1, 18, "§l消す", 0xFFC03030, mx, my, () -> {
                request("photos", "delete", obj("id", id));
                open = "";
                confirmDelete = false;
            });
            s.button(g, x + 6 + bw / 2 + 1, yy, bw / 2 - 1, 18, "やめる", 0xFF34343C, mx, my, () -> confirmDelete = false);
        } else {
            s.button(g, x + 6, yy, bw, 18, "§c写真を消す", 0xFF34343C, mx, my, () -> confirmDelete = true);
        }
        header(g, x, y, w, "写真", mx, my);
    }

    private void pick(GuiGraphics g, int x, int y, int w, int h, int mx, int my, JsonObject o) {
        int top = y + 22;
        g.enableScissor(x, top, x + w, y + h);
        int yy = top - scroll;
        for (JsonElement e : arr(o, "people")) {
            JsonObject p = e.getAsJsonObject();
            String uid = str(p, "uuid"), n = str(p, "name");
            avatar(g, n, x + 6, yy + 3, 18);
            g.drawString(font, cut(n, w - 40), x + 30, yy + 8, 0xFFFFFF, false);
            if (yy >= top) s.hot(x, yy, w, 24, () -> {
                request("messages", "send", obj("to", uid, "text", "", "photo", open));
                picking = false;
                s.openApp(new MessagesApp(uid, n));
            });
            yy += 26;
        }
        g.disableScissor();
        if (arr(o, "people").isEmpty()) g.drawCenteredString(font, "§7送れる相手がいないよ", x + w / 2, y + 60, 0xFFFFFF);
        contentH = yy + scroll - top;
        header(g, x, y, w, "だれに送る？", mx, my);
    }
}
