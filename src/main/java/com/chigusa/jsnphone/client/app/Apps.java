package com.chigusa.jsnphone.client.app;

import java.util.List;

/** ホーム画面に並ぶアプリ */
public final class Apps {
    public record Info(String id, String name, String need) {}

    public static final List<Info> ALL = List.of(
            new Info("messages", "メッセージ", ""), new Info("sns", "つぶやき", ""), new Info("news", "お知らせ", ""), new Info("friends", "フレンド", ""),
            new Info("call", "電話", "voicechat"), new Info("camera", "カメラ", ""), new Info("photos", "写真", ""), new Info("music", "音楽", ""),
            new Info("bank", "銀行", "lightman"), new Info("jsncraft", "jsn-Craft", "jsncraft"), new Info("clock", "時計", ""), new Info("weather", "天気", ""),
            new Info("memo", "メモ", ""), new Info("settings", "設定", ""));

    public static App create(String id) {
        return switch (id) {
            case "messages" -> new MessagesApp();
            case "sns" -> new SnsApp();
            case "news" -> new NewsApp();
            case "friends" -> new FriendsApp();
            case "clock" -> new ClockApp();
            case "weather" -> new WeatherApp();
            case "memo" -> new MemoApp();
            case "settings" -> new SettingsApp();
            case "call" -> new CallApp();
            case "bank" -> new BankApp();
            case "jsncraft" -> new CraftApp();
            default -> new SoonApp(id);
        };
    }

    public static String name(String id) {
        for (Info i : ALL) if (i.id().equals(id)) return i.name();
        return id;
    }

    private Apps() {}
}
