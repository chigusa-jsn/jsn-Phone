package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.JsnPhone;
import com.chigusa.jsnphone.client.PhoneClient;
import com.chigusa.jsnphone.client.PhoneTheme;
import com.chigusa.jsnphone.client.PhoneTime;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** ホーム画面：時計と、アプリのアイコン */
public class HomeApp extends App {
    @Override
    public String title() {
        return "ホーム";
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        int wall = s.wallpaper();
        PhoneTheme.wallpaper(g, wall, x, y, w, h, s.ticks);
        int tc = PhoneTheme.textOn(wall);
        // 大きな時計
        g.pose().pushPose();
        g.pose().translate(x + w / 2F, y + 10, 0);
        g.pose().scale(2.4F, 2.4F, 1);
        String clock = PhoneTime.clock();
        g.drawString(font, clock, -font.width(clock) / 2, 0, tc, tc != 0xFF202020);
        g.pose().popPose();
        String day = PhoneTime.day() + "日目";
        g.drawString(font, day, x + w / 2 - font.width(day) / 2, y + 34, tc, false);
        // アイコン
        JsonObject home = PhoneClient.CACHE.get("home");
        JsonObject av = home != null && home.has("apps") ? home.getAsJsonObject("apps") : new JsonObject();
        int cols = 4, size = 26, gapX = (w - cols * size) / (cols + 1), top = y + 52;
        for (int i = 0; i < Apps.ALL.size(); i++) {
            Apps.Info info = Apps.ALL.get(i);
            int cx = x + gapX + (i % cols) * (size + gapX), cy = top + (i / cols) * (size + 16);
            boolean usable = info.need().isEmpty() || bool(av, info.need());
            boolean hover = mx >= cx && mx < cx + size && my >= cy && my < cy + size;
            g.blit(ResourceLocation.fromNamespaceAndPath(JsnPhone.MODID, "textures/gui/app/" + info.id() + ".png"), cx, cy - (hover ? 1 : 0), size, size, 0, 0, 32, 32, 32, 32);
            if (!usable) g.fill(cx, cy, cx + size, cy + size, 0x90000000);
            String n = info.name();
            g.pose().pushPose();
            g.pose().translate(cx + size / 2F, cy + size + 2, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, n, -font.width(n) / 2, 0, tc, tc != 0xFF202020);
            g.pose().popPose();
            int badge = switch (info.id()) {
                case "messages" -> num(home, "messages");
                case "news" -> Math.min(99, num(home, "news"));
                case "friends" -> num(home, "friends");
                default -> 0;
            };
            if (badge > 0) {
                String b = badge > 99 ? "99+" : String.valueOf(badge);
                int bw = Math.max(9, font.width(b) + 4);
                g.fill(cx + size - bw + 3, cy - 3, cx + size + 3, cy + 7, 0xFFE03030);
                g.drawCenteredString(font, b, cx + size - bw / 2 + 3, cy - 2, 0xFFFFFF);
            }
            String id = info.id();
            s.hot(cx, cy, size, size, () -> s.openById(id));
        }
    }

    @Override
    public void onData(String app, JsonObject o) {}
}
