package com.chigusa.jsnphone.client.camera;

import com.chigusa.jsnphone.JsnPhone;
import com.chigusa.jsnphone.client.PhoneClient;
import com.chigusa.jsnphone.client.PhotoCache;
import com.chigusa.jsnphone.net.PhoneNet;
import com.chigusa.jsnphone.photo.PhotoService;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * カメラ：スマホを閉じて、ファインダーごしに景色を見る。
 * 左クリック＝撮る、右クリック＝自撮り、ホイール＝ズーム、P／Esc＝スマホにもどる。
 */
@EventBusSubscriber(modid = JsnPhone.MODID, value = Dist.CLIENT)
public final class CameraMode {
    private static final float[] ZOOMS = {1F, 1.5F, 2F, 3F, 4F, 6F};
    private static boolean active, startNext;
    private static boolean selfie;
    private static CameraType before;
    private static int zoom;
    private static int capture = -1;     // 撮るまでの残りフレーム
    private static boolean hideBefore;
    private static long flashAt;
    private static int shots;

    public static boolean active() {
        return active;
    }

    /** スマホの画面から呼ぶ（次のティックで画面を閉じてカメラに） */
    public static void startSoon() {
        startNext = true;
    }

    private static void start() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(null);
        active = true;
        selfie = false;
        zoom = 0;
        shots = 0;
        before = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SPYGLASS_USE, 1.4F, 0.6F));
    }

    public static void stop() {
        if (!active) return;
        active = false;
        Minecraft mc = Minecraft.getInstance();
        if (before != null) mc.options.setCameraType(before);
        if (capture >= 0) {
            mc.options.hideGui = hideBefore;
            capture = -1;
        }
    }

    /** スマホにもどる（撮っていたら写真アプリを開く） */
    public static void backToPhone() {
        int n = shots;
        stop();
        PhoneClient.open();
        if (n > 0 && Minecraft.getInstance().screen instanceof com.chigusa.jsnphone.client.PhoneScreen ps) ps.openById("photos");
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (startNext) {
            startNext = false;
            if (mc.player != null) start();
        }
        if (!active) return;
        if (mc.player == null || PhoneClient.phoneStack(mc.player).isEmpty()) stop();
    }

    @SubscribeEvent
    public static void click(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isAttack()) shoot();
        else if (event.isUseItem()) {
            selfie = !selfie;
            Minecraft.getInstance().options.setCameraType(selfie ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
        }
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        if (!active) return;
        event.setCanceled(true);
        zoom = Math.max(0, Math.min(ZOOMS.length - 1, zoom + (event.getScrollDeltaY() > 0 ? 1 : -1)));
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        if (active && !selfie) event.setFOV(event.getFOV() / ZOOMS[zoom]);
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        if (active) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void screen(ScreenEvent.Opening event) {
        if (!active || event.getNewScreen() == null) return;
        if (event.getNewScreen() instanceof PauseScreen) {
            // Esc：スマホにもどる
            event.setCanceled(true);
            backToPhone();
            return;
        }
        if (!(event.getNewScreen() instanceof com.chigusa.jsnphone.client.PhoneScreen)) stop();
    }

    private static void shoot() {
        if (capture >= 0) return;
        Minecraft mc = Minecraft.getInstance();
        hideBefore = mc.options.hideGui;
        mc.options.hideGui = true;
        capture = 2;
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (capture < 0) return;
        if (--capture > 0) return;
        capture = -1;
        Minecraft mc = Minecraft.getInstance();
        try {
            NativeImage full = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            mc.options.hideGui = hideBefore;
            save(full);
        } catch (Exception e) {
            mc.options.hideGui = hideBefore;
            JsnPhone.LOG.warn("写真が撮れなかった", e);
        }
    }

    private static void save(NativeImage full) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        int fw = full.getWidth(), fh = full.getHeight();
        int w = Math.min(640, fw), h = Math.max(1, Math.round(fh * (w / (float) fw)));
        NativeImage small = new NativeImage(w, h, false);
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        // 小さくする（いくつかの点の平均で、なめらかに）
        float sx = fw / (float) w, sy = fh / (float) h;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int r = 0, g = 0, bl = 0, n = 0;
                for (int dy = 0; dy < 2; dy++)
                    for (int dx = 0; dx < 2; dx++) {
                        int px = Math.min(fw - 1, (int) ((x + dx * 0.5F + 0.25F) * sx)), py = Math.min(fh - 1, (int) ((y + dy * 0.5F + 0.25F) * sy));
                        int c = full.getPixelRGBA(px, py);   // ABGR
                        r += c & 255;
                        g += c >> 8 & 255;
                        bl += c >> 16 & 255;
                        n++;
                    }
                r /= n;
                g /= n;
                bl /= n;
                small.setPixelRGBA(x, y, 0xFF000000 | bl << 16 | g << 8 | r);
                b.setRGB(x, y, r << 16 | g << 8 | bl);
            }
        full.close();
        byte[] jpg = jpeg(b, 0.85F);
        if (jpg.length > PhotoService.MAX_BYTES) jpg = jpeg(b, 0.6F);
        String id = UUID.randomUUID().toString().replace("-", "");
        PhotoCache.putLocal(id, small);
        String meta = w + "|" + h + "|" + place();
        int total = (jpg.length + PhotoService.CHUNK - 1) / PhotoService.CHUNK;
        for (int i = 0; i < total; i++) {
            byte[] part = Arrays.copyOfRange(jpg, i * PhotoService.CHUNK, Math.min(jpg.length, (i + 1) * PhotoService.CHUNK));
            PacketDistributor.sendToServer(new PhoneNet.PhotoUp(id, i, total, part, i == 0 ? meta : ""));
        }
        flashAt = System.currentTimeMillis();
        shots++;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.6F, 0.9F));
    }

    private static byte[] jpeg(BufferedImage b, float quality) throws Exception {
        ImageWriter wr = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
            wr.setOutput(ios);
            ImageWriteParam p = wr.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(quality);
            wr.write(null, new IIOImage(b, null, null), p);
        } finally {
            wr.dispose();
        }
        return out.toByteArray();
    }

    /** 撮った場所：「地上・平原（120, 64, -30）」 */
    private static String place() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return "";
        BlockPos pos = mc.player.blockPosition();
        String dim = switch (mc.level.dimension().location().toString()) {
            case "minecraft:overworld" -> "地上";
            case "minecraft:the_nether" -> "ネザー";
            case "minecraft:the_end" -> "エンド";
            default -> mc.level.dimension().location().getPath();
        };
        String biome = mc.level.getBiome(pos).unwrapKey()
                .map(k -> Component.translatable("biome." + k.location().getNamespace() + "." + k.location().getPath()).getString()).orElse("");
        return dim + (biome.isEmpty() ? "" : "・" + biome) + "（" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "）";
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        if (!active || capture >= 0) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        int m = 14, len = 16, col = 0xD0FFFFFF;
        // 四すみ
        int[][] cs = {{m, m, 1, 1}, {w - m, m, -1, 1}, {m, h - m, 1, -1}, {w - m, h - m, -1, -1}};
        for (int[] c : cs) {
            int x = c[0], y = c[1];
            g.fill(Math.min(x, x + c[2] * len), y - (c[3] < 0 ? 2 : 0), Math.max(x, x + c[2] * len), y + (c[3] > 0 ? 2 : 0), col);
            g.fill(x - (c[2] < 0 ? 2 : 0), Math.min(y, y + c[3] * len), x + (c[2] > 0 ? 2 : 0), Math.max(y, y + c[3] * len), col);
        }
        // 三分割の線
        for (int i = 1; i < 3; i++) {
            g.fill(w * i / 3, m + 4, w * i / 3 + 1, h - m - 4, 0x30FFFFFF);
            g.fill(m + 4, h * i / 3, w - m - 4, h * i / 3 + 1, 0x30FFFFFF);
        }
        // まんなかの印
        g.fill(w / 2 - 6, h / 2, w / 2 + 7, h / 2 + 1, 0x90FFFFFF);
        g.fill(w / 2, h / 2 - 6, w / 2 + 1, h / 2 + 7, 0x90FFFFFF);
        var f = mc.font;
        String top = (selfie ? "◎ 自撮り" : "◎ カメラ") + (ZOOMS[zoom] > 1 && !selfie ? String.format("  ×%.1f", ZOOMS[zoom]) : "") + (shots > 0 ? "  §7撮った " + shots + "枚" : "");
        g.drawCenteredString(f, top, w / 2, m + 6, 0xFFFFFF);
        g.drawCenteredString(f, "§7左クリック：撮る  右クリック：自撮り  ホイール：ズーム  P・Esc：もどる", w / 2, h - m - 14, 0xFFFFFF);
        // 撮ったときの白い光
        long since = System.currentTimeMillis() - flashAt;
        if (since < 250) g.fill(0, 0, w, h, (int) (0xC0 * (1 - since / 250.0)) << 24 | 0xFFFFFF);
    }

    private CameraMode() {}
}
