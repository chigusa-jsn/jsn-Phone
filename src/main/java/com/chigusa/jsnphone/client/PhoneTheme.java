package com.chigusa.jsnphone.client;

import net.minecraft.client.gui.GuiGraphics;

/** スマホの色（テーマ）と壁紙 */
public final class PhoneTheme {
    public static final String[] THEME_NAMES = {"空色", "桜", "若葉", "夕焼け", "ぶどう", "レモン", "海", "いちご", "ミント", "金", "銀", "すみ"};
    public static final int[] THEMES = {0xFF3A9AF0, 0xFFF080B0, 0xFF50C060, 0xFFF08030, 0xFF9060E0, 0xFFE0C030,
            0xFF2060C0, 0xFFE03050, 0xFF40D0B0, 0xFFE6B836, 0xFFB0B8C8, 0xFF505868};
    public static final String[] WALL_NAMES = {"夜空", "海", "森", "夕焼け", "桜", "カジノ", "雪", "シンプル白", "シンプル黒"};

    public static int accent(int theme) {
        return THEMES[Math.floorMod(theme, THEMES.length)];
    }

    public static int kindColor(String kind) {
        return switch (kind) {
            case "message" -> 0xFF40C060;
            case "friend" -> 0xFF3A9AF0;
            case "call" -> 0xFFF0C040;
            case "sns", "like" -> 0xFF40A0F0;
            default -> 0xFFE0E0E0;
        };
    }

    static int lerp(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    /** 壁紙を描く（上から下へのグラデーション＋飾り） */
    public static void wallpaper(GuiGraphics g, int wall, int x, int y, int w, int h, int ticks) {
        int top, bot;
        switch (Math.floorMod(wall, WALL_NAMES.length)) {
            case 0 -> { top = 0x0A0A30; bot = 0x402060; }
            case 1 -> { top = 0x60C0F0; bot = 0x1050A0; }
            case 2 -> { top = 0x80C860; bot = 0x205020; }
            case 3 -> { top = 0xF09040; bot = 0x602050; }
            case 4 -> { top = 0xFFE0F0; bot = 0xF0A0C0; }
            case 5 -> { top = 0x0E5A36; bot = 0x063020; }
            case 6 -> { top = 0xD8E8F8; bot = 0x90A8C8; }
            case 7 -> { top = 0xF4F4F4; bot = 0xE0E0E0; }
            default -> { top = 0x202024; bot = 0x101012; }
        }
        for (int i = 0; i < h; i += 2) g.fill(x, y + i, x + w, y + Math.min(h, i + 2), lerp(top, bot, i / (float) h));
        int seed = 12345;
        switch (Math.floorMod(wall, WALL_NAMES.length)) {
            case 0 -> {   // 星
                for (int i = 0; i < 40; i++) {
                    seed = seed * 1103515245 + 12345;
                    int sx = x + Math.floorMod(seed >> 8, w), sy = y + Math.floorMod(seed >> 3, h * 2 / 3);
                    boolean tw = ((ticks / 10 + i) % 7) == 0;
                    g.fill(sx, sy, sx + 1, sy + 1, tw ? 0xFFFFFFA0 : 0xC0FFFFFF);
                }
                g.fill(x + w - 30, y + 20, x + w - 18, y + 32, 0xFFF0F0C0);
                g.fill(x + w - 26, y + 20, x + w - 16, y + 30, 0xFF0A0A30 | 0);
            }
            case 1 -> {   // 波
                for (int k = 0; k < 4; k++) {
                    int yy = y + h / 2 + k * 18;
                    for (int xx = 0; xx < w; xx += 2) {
                        int dy = (int) (Math.sin((xx + ticks * 0.6 + k * 20) / 10.0) * 2);
                        g.fill(x + xx, yy + dy, x + xx + 2, yy + dy + 1, 0x60FFFFFF);
                    }
                }
            }
            case 2 -> {   // 木
                for (int i = 0; i < 6; i++) {
                    int tx = x + 8 + i * (w / 6), ty = y + h - 40 - (i % 2) * 10;
                    g.fill(tx + 4, ty + 14, tx + 7, ty + 30, 0xFF5A3A20);
                    g.fill(tx, ty, tx + 11, ty + 16, 0xFF2E7A30);
                }
            }
            case 3 -> g.fill(x + w / 2 - 14, y + h / 2 - 4, x + w / 2 + 14, y + h / 2 + 24, 0xC0FFD060);
            case 4 -> {   // 花びら
                for (int i = 0; i < 24; i++) {
                    seed = seed * 1103515245 + 12345;
                    int sx = x + Math.floorMod((seed >> 8) + ticks / 3, w), sy = y + Math.floorMod((seed >> 3) + ticks / 2, h);
                    g.fill(sx, sy, sx + 2, sy + 2, 0xFFFFFFFF);
                    g.fill(sx + 1, sy, sx + 2, sy + 1, 0xFFF06090);
                }
            }
            case 5 -> {   // トランプのマーク
                String[] s = {"♠", "♥", "♦", "♣"};
                var font = net.minecraft.client.Minecraft.getInstance().font;
                for (int i = 0; i < 12; i++) {
                    int sx = x + 10 + (i % 4) * (w - 20) / 3, sy = y + 30 + (i / 4) * (h / 3);
                    g.drawString(font, (i % 4 == 1 || i % 4 == 2 ? "§4" : "§0") + s[i % 4], sx, sy, 0xFFFFFF, false);
                }
            }
            case 6 -> {   // 雪
                for (int i = 0; i < 40; i++) {
                    seed = seed * 1103515245 + 12345;
                    int sx = x + Math.floorMod((seed >> 8), w), sy = y + Math.floorMod((seed >> 3) + ticks / 2, h);
                    g.fill(sx, sy, sx + 1, sy + 1, 0xFFFFFFFF);
                }
            }
            default -> {}
        }
    }

    /** 文字が読みやすい色（明るい壁紙なら黒） */
    public static int textOn(int wall) {
        int w = Math.floorMod(wall, WALL_NAMES.length);
        return w == 4 || w == 6 || w == 7 ? 0xFF202020 : 0xFFFFFFFF;
    }

    private PhoneTheme() {}
}
