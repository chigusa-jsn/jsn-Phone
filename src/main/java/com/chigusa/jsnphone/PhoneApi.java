package com.chigusa.jsnphone;

import com.chigusa.jsnphone.data.PhoneData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * ほかのMOD（jsn-Craft など）から使う入り口。
 * jsn-Craft は jsn-Phone がなくても動くように、リフレクションでここを呼ぶ：
 *   Class.forName("com.chigusa.jsnphone.PhoneApi").getMethod("news", ServerPlayer.class, String.class, String.class).invoke(null, p, "…", "level")
 */
public final class PhoneApi {

    /** お知らせに載せる。kind は絵の種類（level / item / boss / dungeon / casino / join / advancement / other） */
    public static void news(ServerPlayer player, String text, String kind) {
        PhoneData.get(player.server).addNews(text, kind, player.getStringUUID());
    }

    public static void news(MinecraftServer server, String text, String kind) {
        PhoneData.get(server).addNews(text, kind, "");
    }

    private PhoneApi() {}
}
