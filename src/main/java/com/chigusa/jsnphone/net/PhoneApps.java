package com.chigusa.jsnphone.net;

import com.chigusa.jsnphone.data.PhoneData;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** ほかのMODがあるときだけ使えるアプリ（電話・銀行・jsn-Craft）。 */
public final class PhoneApps {

    public static JsonObject available() {
        JsonObject o = new JsonObject();
        ModList m = ModList.get();
        o.addProperty("voicechat", m.isLoaded("voicechat"));
        o.addProperty("lightman", m.isLoaded("lightmanscurrency"));
        o.addProperty("jsncraft", m.isLoaded("jsn_craft"));
        return o;
    }

    static void handle(ServerPlayer p, PhoneData d, String app, String action, JsonObject in) {
        // まだないアプリ（あとで電話・銀行・jsn-Craft を入れる）
    }

    private PhoneApps() {}
}
