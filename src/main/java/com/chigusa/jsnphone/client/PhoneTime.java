package com.chigusa.jsnphone.client;

import net.minecraft.client.Minecraft;

/** 時刻の表示 */
public final class PhoneTime {

    /** ゲームの中の時刻（0 tick ＝ 朝6時） */
    public static String clock() {
        var level = Minecraft.getInstance().level;
        if (level == null) return "--:--";
        long t = (level.getDayTime() + 6000) % 24000;
        int h = (int) (t / 1000), m = (int) (t % 1000 * 60 / 1000);
        return String.format("%d:%02d", h, m);
    }

    public static long day() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getDayTime() / 24000 + 1;
    }

    /** 「たった今」「3分前」など（本物の時間） */
    public static String ago(long time) {
        long s = (System.currentTimeMillis() - time) / 1000;
        if (s < 60) return "たった今";
        if (s < 3600) return (s / 60) + "分前";
        if (s < 86400) return (s / 3600) + "時間前";
        return (s / 86400) + "日前";
    }

    private PhoneTime() {}
}
