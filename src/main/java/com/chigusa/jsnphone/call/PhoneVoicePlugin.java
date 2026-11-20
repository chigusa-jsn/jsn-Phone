package com.chigusa.jsnphone.call;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

/** シンプルボイスチャットのプラグイン（ボイスチャットがあるときだけ読まれる） */
@ForgeVoicechatPlugin
public class PhoneVoicePlugin implements VoicechatPlugin {
    static VoicechatServerApi api;

    @Override
    public String getPluginId() {
        return "jsn_phone";
    }

    @Override
    public void registerEvents(EventRegistration r) {
        r.registerEvent(VoicechatServerStartedEvent.class, e -> api = e.getVoicechat());
    }
}
