package com.chigusa.jsnphone.client;

import com.chigusa.jsnphone.client.app.App;
import com.chigusa.jsnphone.client.app.Apps;
import com.chigusa.jsnphone.client.app.HomeApp;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * スマホの画面。まわりの本体（ケース）＋上のステータスバー＋アプリ＋下のホームボタン。
 * アプリは client/app の App。ボタンは描くときに登録して、クリックで当たり判定する。
 */
public class PhoneScreen extends Screen {
    public static final int W = 168, H = 288;
    public final ItemStack stack;
    public int left, top, ticks;
    /** 画面（アプリが描ける場所） */
    public int sx, sy, sw, sh;
    private App app;
    private final HomeApp home = new HomeApp();

    /** 描いたボタンの当たり判定 */
    private record Hot(int x, int y, int w, int h, Runnable run) {}

    private final List<Hot> hots = new ArrayList<>(), nextHots = new ArrayList<>();

    public PhoneScreen(ItemStack stack) {
        super(Component.literal("スマホ"));
        this.stack = stack;
    }

    public int caseColor() {
        return PhoneColors.caseColor(stack, ticks);
    }

    public int theme() {
        JsonObject h = PhoneClient.CACHE.get("home");
        return h != null && h.has("theme") ? h.get("theme").getAsInt() : 0;
    }

    public int wallpaper() {
        JsonObject h = PhoneClient.CACHE.get("home");
        return h != null && h.has("wallpaper") ? h.get("wallpaper").getAsInt() : 0;
    }

    public int accent() {
        return PhoneTheme.accent(theme());
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        sx = left + 8;
        sy = top + 24;
        sw = W - 16;
        sh = H - 48;
        if (app == null) {
            app = home;
            app.attach(this);
            PhoneClient.request("home", "get", null);
        }
        clearWidgets();
        app.attach(this);
        app.init();
    }

    public void openApp(App a) {
        app = a;
        clearWidgets();
        a.attach(this);
        a.init();
        a.open();
        sound(1.6F);
    }

    public void goHome() {
        if (app == home) return;
        openApp(home);
        PhoneClient.request("home", "get", null);
    }

    public void refreshHomeBadges() {
        PhoneClient.request("home", "get", null);
    }

    public void onData(String id, JsonObject o) {
        if (app != null) app.onData(id, o);
    }

    public <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T add(T w) {
        return addRenderableWidget(w);
    }

    public EditBox editBox(int x, int y, int w, int h, int max, String hint) {
        EditBox b = new EditBox(font, x, y, w, h, Component.literal(hint));
        b.setMaxLength(max);
        b.setHint(Component.literal(hint).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        return addRenderableWidget(b);
    }

    /** ボタンを描いて当たり判定を登録する */
    public void button(GuiGraphics g, int x, int y, int w, int h, String label, int color, int mx, int my, Runnable run) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, hover ? brighter(color) : color);
        if (label != null && !label.isEmpty()) g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFFFFFF);
        hot(x, y, w, h, run);
    }

    public void hot(int x, int y, int w, int h, Runnable run) {
        nextHots.add(new Hot(x, y, w, h, run));
    }

    static int brighter(int c) {
        int a = c >>> 24, r = Math.min(255, (c >> 16 & 255) + 30), g = Math.min(255, (c >> 8 & 255) + 30), b = Math.min(255, (c & 255) + 30);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public void sound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25F));
    }

    @Override
    public void tick() {
        ticks++;
        if (app != null) app.tick();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        nextHots.clear();
        int c = caseColor();
        // 本体（ケース）
        g.fill(left + 2, top, left + W - 2, top + H, 0xFF101014);
        g.fill(left, top + 2, left + W, top + H - 2, 0xFF101014);
        g.fill(left + 3, top + 1, left + W - 3, top + H - 1, c);
        g.fill(left + 1, top + 3, left + W - 1, top + H - 3, c);
        g.fill(left + 5, top + 4, left + W - 5, top + H - 4, 0xFF08080A);
        // 横のボタン
        g.fill(left - 2, top + 50, left, top + 70, PhoneColors.darker(c));
        g.fill(left + W, top + 60, left + W + 2, top + 90, PhoneColors.darker(c));
        // 画面
        g.fill(sx, sy - 14, sx + sw, sy + sh + 12, 0xFF000000);
        g.enableScissor(sx, sy, sx + sw, sy + sh);
        if (app != null) app.render(g, sx, sy, sw, sh, mx, my, pt);
        g.disableScissor();
        // ステータスバー
        g.fill(sx, sy - 14, sx + sw, sy, 0xFF000000);
        g.drawString(font, PhoneTime.clock(), sx + 6, sy - 11, 0xFFFFFF, false);
        g.fill(sx + sw / 2 - 16, sy - 12, sx + sw / 2 + 16, sy - 6, 0xFF1A1A1E);   // カメラの穴
        g.fill(sx + sw / 2 + 8, sy - 11, sx + sw / 2 + 11, sy - 8, 0xFF203048);
        drawSignal(g, sx + sw - 34, sy - 11);
        drawBattery(g, sx + sw - 18, sy - 11);
        // ホームボタン（下のバー）
        int hb = sy + sh + 4;
        boolean hover = mx >= sx + sw / 2 - 24 && mx < sx + sw / 2 + 24 && my >= hb - 2 && my < hb + 8;
        g.fill(sx + sw / 2 - 22, hb + 2, sx + sw / 2 + 22, hb + 4, hover ? 0xFFFFFFFF : 0xFFB0B0B0);
        hot(sx + sw / 2 - 24, hb - 2, 48, 10, this::goHome);
        super.render(g, mx, my, pt);
        hots.clear();
        hots.addAll(nextHots);
    }

    private void drawSignal(GuiGraphics g, int x, int y) {
        for (int i = 0; i < 4; i++) g.fill(x + i * 3, y + 6 - i * 2, x + i * 3 + 2, y + 7, 0xFFFFFFFF);
    }

    private void drawBattery(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 13, y + 7, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 12, y + 6, 0xFF000000);
        g.fill(x + 13, y + 2, x + 14, y + 5, 0xFFFFFFFF);
        // 電池はおまけ：ゲームの時間で少しずつ減って、夜明けに満タン
        long t = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getDayTime() % 24000;
        int lvl = 11 - (int) (t * 9 / 24000);
        g.fill(x + 2, y + 2, x + 2 + Math.max(1, lvl - 2), y + 5, lvl > 4 ? 0xFF50E050 : 0xFFE05050);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        for (int i = hots.size() - 1; i >= 0; i--) {
            Hot h = hots.get(i);
            if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                sound(1.8F);
                h.run.run();
                return true;
            }
        }
        return app != null && app.click(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (app != null && app.scroll(sy)) return true;
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && app != null && app.enter()) return true;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (app != null && app != home) {
                if (!app.back()) goHome();
                return true;
            }
            onClose();
            return true;
        }
        // 文字を打っているときは P キーなどで閉じない
        if (getFocused() instanceof EditBox eb && eb.isFocused()) return super.keyPressed(key, scan, mods);
        if (PhoneClient.OPEN.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public App home() {
        return home;
    }

    /** アプリが自分の部品（文字入力など）を置き直すとき */
    public void clearWidgetsFor(App a) {
        clearWidgets();
    }

    public void openById(String id) {
        openApp(Apps.create(id));
    }
}
