package com.chigusa.jsnphone.client;

import com.chigusa.jsnphone.JsnPhone;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** サーバーからもらった写真を、テクスチャにしてとっておく（古いものから捨てる） */
public final class PhotoCache {
    public record Tex(ResourceLocation rl, int w, int h) {}

    private static final int KEEP = 48;
    private static final LinkedHashMap<String, Tex> TEX = new LinkedHashMap<>(16, 0.75F, true);
    private static final Set<String> ASKED = new HashSet<>(), GONE = new HashSet<>();
    private static final Map<String, Long> ASKED_AT = new HashMap<>();

    public static Tex get(String id) {
        if (id == null || id.isEmpty()) return null;
        Tex t = TEX.get(id);
        if (t != null || GONE.contains(id)) return t;
        long now = System.currentTimeMillis();
        if (!ASKED.contains(id) || now - ASKED_AT.getOrDefault(id, 0L) > 10_000) {
            ASKED.add(id);
            ASKED_AT.put(id, now);
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("id", id);
            PhoneClient.request("photos", "fetch", o);
        }
        return null;
    }

    public static boolean gone(String id) {
        return GONE.contains(id);
    }

    /** 自分で撮ったばかりの写真は、サーバーに聞かなくてもすぐ見られるようにする */
    public static void putLocal(String id, NativeImage img) {
        register(id, img);
    }

    public static void onData(String id, byte[] data) {
        ASKED.remove(id);
        if (data.length == 0) {
            GONE.add(id);
            return;
        }
        try {
            BufferedImage b = ImageIO.read(new ByteArrayInputStream(data));
            if (b == null) {
                GONE.add(id);
                return;
            }
            NativeImage img = new NativeImage(b.getWidth(), b.getHeight(), false);
            for (int y = 0; y < b.getHeight(); y++)
                for (int x = 0; x < b.getWidth(); x++) {
                    int c = b.getRGB(x, y);
                    int r = c >> 16 & 255, g = c >> 8 & 255, bl = c & 255;
                    img.setPixelRGBA(x, y, 0xFF000000 | bl << 16 | g << 8 | r);   // NativeImage は ABGR
                }
            register(id, img);
        } catch (Exception e) {
            GONE.add(id);
        }
    }

    private static void register(String id, NativeImage img) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation rl = JsnPhone.id("photo/" + id);
        DynamicTexture tex = new DynamicTexture(img);
        tex.setFilter(true, false);
        mc.getTextureManager().register(rl, tex);
        TEX.put(id, new Tex(rl, img.getWidth(), img.getHeight()));
        while (TEX.size() > KEEP) {
            String old = TEX.keySet().iterator().next();
            Tex t = TEX.remove(old);
            mc.getTextureManager().release(t.rl());
        }
    }

    /** 写真を枠に合わせて描く（crop=true ならはみ出すところを切って枠いっぱいに） */
    public static void draw(GuiGraphics g, String id, int x, int y, int w, int h, boolean crop) {
        Tex t = get(id);
        if (t == null) {
            g.fill(x, y, x + w, y + h, 0xFF2A2A30);
            String m = gone(id) ? "§8消えた写真" : "§8…";
            var f = Minecraft.getInstance().font;
            g.drawString(f, m, x + (w - f.width(m)) / 2, y + (h - 8) / 2, 0xFFFFFF, false);
            return;
        }
        float tw = t.w(), th = t.h();
        if (crop) {
            // 枠の比に合わせて真ん中を切りぬく
            float scale = Math.max(w / tw, h / th);
            float uw = w / scale, vh = h / scale;
            float u = (tw - uw) / 2, v = (th - vh) / 2;
            g.blit(t.rl(), x, y, w, h, u, v, (int) uw, (int) vh, t.w(), t.h());
        } else {
            float scale = Math.min(w / tw, h / th);
            int dw = Math.round(tw * scale), dh = Math.round(th * scale);
            g.fill(x, y, x + w, y + h, 0xFF000000);
            g.blit(t.rl(), x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, 0, 0, t.w(), t.h(), t.w(), t.h());
        }
    }

    /** 写真の縦横比（わからなければ 16:9） */
    public static float aspect(String id) {
        Tex t = TEX.get(id);
        return t == null ? 9F / 16F : t.h() / (float) t.w();
    }

    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        for (Tex t : TEX.values()) mc.getTextureManager().release(t.rl());
        TEX.clear();
        ASKED.clear();
        GONE.clear();
    }

    private PhotoCache() {}
}
