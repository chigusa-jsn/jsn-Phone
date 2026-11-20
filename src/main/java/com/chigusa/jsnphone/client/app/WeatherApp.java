package com.chigusa.jsnphone.client.app;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.biome.Biome;

/** 天気：いまの天気・気温・バイオーム */
public class WeatherApp extends App {
    @Override
    public String title() {
        return "天気";
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        var p = mc.player;
        String weather = "晴れ", icon = "☀";
        int bg = 0xFF4A90D0;
        float temp = 0.8F;
        String biome = "";
        if (level != null && p != null) {
            var holder = level.getBiome(p.blockPosition());
            Biome b = holder.value();
            temp = b.getBaseTemperature();
            biome = holder.unwrapKey().map(k -> k.location().getPath()).orElse("");
            boolean cold = b.coldEnoughToSnow(p.blockPosition());
            boolean dry = !b.hasPrecipitation();
            if (level.isThundering() && !dry) { weather = "雷雨"; icon = "⚡"; bg = 0xFF303848; }
            else if (level.isRaining() && !dry) { weather = cold ? "雪" : "雨"; icon = cold ? "❄" : "☂"; bg = cold ? 0xFF8098B8 : 0xFF506078; }
            else if (level.isNight()) { weather = "晴れ（夜）"; icon = "☾"; bg = 0xFF182040; }
            if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) { weather = "天気はない"; icon = "？"; bg = 0xFF402030; }
        }
        g.fill(x, y, x + w, y + h, bg);
        g.pose().pushPose();
        g.pose().translate(x + w / 2F, y + 40, 0);
        g.pose().scale(4F, 4F, 1);
        g.drawCenteredString(font, icon, 0, 0, 0xFFFFFF);
        g.pose().popPose();
        g.drawCenteredString(font, "§l" + weather, x + w / 2, y + 82, 0xFFFFFF);
        int c = Math.round(temp * 25);   // マイクラの気温をだいたい ℃ に
        g.drawCenteredString(font, "気温 " + c + "℃", x + w / 2, y + 96, 0xFFFFFF);
        g.drawCenteredString(font, "§7" + biome, x + w / 2, y + 110, 0xFFFFFF);
        String tip = weather.startsWith("雷") ? "雷に気をつけて！" : weather.equals("雨") ? "釣りにいい天気かも" : weather.equals("雪") ? "あたたかくしてね" : "おでかけ日和！";
        g.drawCenteredString(font, "§f" + tip, x + w / 2, y + 140, 0xFFFFFF);
        header(g, x, y, w, title(), mx, my);
    }
}
