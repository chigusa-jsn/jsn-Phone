package com.chigusa.jsnphone.client;

import com.chigusa.jsnphone.JsnPhone;
import com.chigusa.jsnphone.net.PhoneNet;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** スマホのクライアント側：開く・届いたデータ・通知のバナー・Pキー。 */
@EventBusSubscriber(modid = JsnPhone.MODID, value = Dist.CLIENT)
public final class PhoneClient {

    public static final KeyMapping OPEN = new KeyMapping("key.jsn_phone.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.categories.jsn_phone");
    /** アプリごとの最新のデータ */
    public static final Map<String, JsonObject> CACHE = new HashMap<>();

    record Banner(String kind, String title, String text, long until) {}

    private static final ArrayDeque<Banner> BANNERS = new ArrayDeque<>();

    @EventBusSubscriber(modid = JsnPhone.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            JsnPhone.openClient = PhoneClient::open;
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
        }
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PhoneScreen) return;
        PhoneScreen ps = new PhoneScreen(phoneStack(mc.player));
        mc.setScreen(ps);
        // 電話が鳴っていたら、すぐ電話の画面へ
        String st = callState();
        if (st.equals("ringing") || st.equals("talking") || st.equals("calling")) ps.openById("call");
    }

    public static String callState() {
        JsonObject c = CACHE.get("call");
        return c != null && c.has("state") ? c.get("state").getAsString() : "";
    }

    /** 持っているスマホ（手に持っているもの優先） */
    public static ItemStack phoneStack(Player p) {
        return com.chigusa.jsnphone.PhoneHolder.find(p);
    }

    public static void request(String app, String action, JsonObject data) {
        PacketDistributor.sendToServer(new PhoneNet.Req(app, action, data == null ? "" : data.toString()));
    }

    public static void onRes(String app, String raw) {
        JsonObject o;
        try {
            o = JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        if (app.equals("messages_push")) {
            // 相手から届いた：開いている会話ならそのまま更新
            JsonObject cur = CACHE.get("messages");
            if (cur != null && cur.has("with") && o.has("with") && cur.get("with").getAsString().equals(o.get("with").getAsString())) app = "messages";
            else return;
        }
        o.addProperty("_at", System.currentTimeMillis());
        if (app.equals("call")) {
            String before = callState(), now = o.has("state") ? o.get("state").getAsString() : "";
            Minecraft mc = Minecraft.getInstance();
            // 通話がはじまった／終わった音
            if (!before.equals("talking") && now.equals("talking")) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.4F, 0.7F));
            if ((before.equals("talking") || before.equals("calling")) && now.equals("idle")) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.8F));
            // 着信：スマホを開いていたら電話の画面へ
            if (now.equals("ringing") && !before.equals("ringing") && mc.screen instanceof PhoneScreen ps) {
                CACHE.put(app, o);
                ps.openById("call");
            }
        }
        CACHE.put(app, o);
        if (Minecraft.getInstance().screen instanceof PhoneScreen s) s.onData(app, o);
    }

    public static void onNotify(String kind, String title, String text) {
        Minecraft mc = Minecraft.getInstance();
        if (phoneStack(mc.player).isEmpty() && !kind.equals("call")) return;   // スマホを持っていないと届かない
        BANNERS.add(new Banner(kind, title, text, System.currentTimeMillis() + 4500));
        while (BANNERS.size() > 3) BANNERS.poll();
        SoundEvent s = switch (kind) {
            case "message" -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case "friend" -> SoundEvents.NOTE_BLOCK_BELL.value();
            case "call" -> SoundEvents.NOTE_BLOCK_PLING.value();
            default -> SoundEvents.NOTE_BLOCK_XYLOPHONE.value();
        };
        mc.getSoundManager().play(SimpleSoundInstance.forUI(s, 1.6F, 0.8F));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(s, 2.0F, 0.6F));
        if (mc.screen instanceof PhoneScreen ps) ps.refreshHomeBadges();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (com.chigusa.jsnphone.client.camera.CameraMode.active()) {
                com.chigusa.jsnphone.client.camera.CameraMode.backToPhone();
                continue;
            }
            if (mc.player != null && !phoneStack(mc.player).isEmpty()) open();
            else if (mc.player != null) mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("スマホを持っていないよ"), true);
        }
        // 着信音（「プルル、プルル」をくり返す）／呼び出し音
        if (mc.player != null && !mc.isPaused()) {
            String st = callState();
            long t = mc.player.tickCount;
            if (st.equals("ringing") && (t % 40 == 0 || t % 40 == 6))
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), t % 40 == 0 ? 1.5F : 1.25F, 0.9F));
            if (st.equals("calling") && t % 60 == 0 && mc.screen instanceof PhoneScreen)
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), 1.0F, 0.4F));
        }
        long now = System.currentTimeMillis();
        while (!BANNERS.isEmpty() && BANNERS.peek().until() < now) BANNERS.poll();
    }

    @SubscribeEvent
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        CACHE.clear();
        BANNERS.clear();
        com.chigusa.jsnphone.client.camera.CameraMode.stop();
        PhotoCache.clear();
        com.chigusa.jsnphone.client.music.MusicPlayer.stop();
    }

    /** 画面の右上に通知を出す */
    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        if (BANNERS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int w = 150, x = g.guiWidth() - w - 6, y = 6;
        long now = System.currentTimeMillis();
        for (Banner b : BANNERS) {
            long left = b.until() - now;
            int slide = left < 300 ? (int) ((300 - left) / 300.0 * (w + 10)) : 0;
            int bx = x + slide;
            g.fill(bx, y, bx + w, y + 26, 0xE0181820);
            g.fill(bx, y, bx + 3, y + 26, PhoneTheme.kindColor(b.kind()));
            g.drawString(mc.font, "§l" + b.title(), bx + 8, y + 4, 0xFFFFFF);
            String t = b.text();
            if (mc.font.width(t) > w - 14) t = mc.font.plainSubstrByWidth(t, w - 20) + "…";
            g.drawString(mc.font, t, bx + 8, y + 15, 0xD0D0D0);
            y += 30;
        }
    }

    private PhoneClient() {}
}
