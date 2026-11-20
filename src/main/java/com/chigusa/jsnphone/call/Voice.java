package com.chigusa.jsnphone.call;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;

import java.util.UUID;

/** 通話のための、2人だけの見えないグループ（ボイスチャットがあるときだけ呼ぶ） */
final class Voice {

    static boolean ready() {
        return PhoneVoicePlugin.api != null;
    }

    /** 声がつながるか（相手もボイスチャットに接続しているか） */
    static boolean connected(UUID player) {
        if (!ready()) return false;
        VoicechatConnection c = PhoneVoicePlugin.api.getConnectionOf(player);
        return c != null && c.isConnected();
    }

    /** 2人を同じグループに入れて、グループのIDを返す */
    static UUID join(UUID a, UUID b, String name) {
        if (!ready()) return null;
        Group g = PhoneVoicePlugin.api.groupBuilder().setName(name).setPassword(UUID.randomUUID().toString())
                .setHidden(true).setPersistent(false).setType(Group.Type.ISOLATED).build();
        for (UUID u : new UUID[]{a, b}) {
            VoicechatConnection c = PhoneVoicePlugin.api.getConnectionOf(u);
            if (c != null) c.setGroup(g);
        }
        return g.getId();
    }

    static void leave(UUID player) {
        if (!ready()) return;
        VoicechatConnection c = PhoneVoicePlugin.api.getConnectionOf(player);
        if (c != null) c.setGroup(null);
    }

    private Voice() {}
}
