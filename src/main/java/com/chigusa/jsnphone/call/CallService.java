package com.chigusa.jsnphone.call;

import com.chigusa.jsnphone.JsnPhone;
import com.chigusa.jsnphone.PhoneHolder;
import com.chigusa.jsnphone.data.PhoneData;
import com.chigusa.jsnphone.net.PhoneServer;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 電話：かける → 相手の着信音が鳴る → 出る／断る → 通話中（ボイスチャットの見えないグループ）→ 切る。
 * 30秒出なければ「不在着信」。
 */
@EventBusSubscriber(modid = JsnPhone.MODID)
public final class CallService {
    static final class Call {
        UUID caller, callee;
        long start;          // かけた時刻（ゲーム時刻）
        long talkStart = -1; // 話しはじめた時刻（本物の時刻）
    }

    /** プレイヤー → いまの通話 */
    private static final Map<UUID, Call> CALLS = new ConcurrentHashMap<>();
    private static final int RING_TICKS = 600;

    static boolean voiceLoaded() {
        return ModList.get().isLoaded("voicechat");
    }

    public static void handle(ServerPlayer p, String action, JsonObject in) {
        UUID me = p.getUUID();
        switch (action) {
            case "dial" -> {
                if (!voiceLoaded() || !Voice.ready()) { error(p, "ボイスチャットMODが入っていないよ"); return; }
                if (CALLS.containsKey(me)) { error(p, "いまは通話中だよ"); return; }
                String to = in.has("to") ? in.get("to").getAsString() : "";
                ServerPlayer o;
                try {
                    o = p.server.getPlayerList().getPlayer(UUID.fromString(to));
                } catch (Exception e) {
                    o = null;
                }
                if (o == null) { error(p, "相手はオフラインだよ"); return; }
                if (o == p) return;
                if (PhoneHolder.find(o).isEmpty()) { error(p, "相手はスマホを持っていないみたい"); return; }
                if (CALLS.containsKey(o.getUUID())) { error(p, "相手は話し中だよ"); return; }
                Call c = new Call();
                c.caller = me;
                c.callee = o.getUUID();
                c.start = p.server.getTickCount();
                CALLS.put(me, c);
                CALLS.put(o.getUUID(), c);
                PhoneServer.notify(o, "call", "📞 着信", p.getName().getString() + " から電話だよ（Pキーで出る）");
                send(p);
                send(o);
            }
            case "answer" -> {
                Call c = CALLS.get(me);
                if (c == null || !c.callee.equals(me) || c.talkStart >= 0) return;
                c.talkStart = System.currentTimeMillis();
                if (voiceLoaded()) Voice.join(c.caller, c.callee, "📞");
                sendBoth(p, c);
            }
            case "hangup" -> end(p, me, true);
            case "state" -> send(p);
            default -> send(p);
        }
    }

    private static void end(ServerPlayer p, UUID who, boolean byUser) {
        Call c = CALLS.get(who);
        if (c == null) {
            send(p);
            return;
        }
        CALLS.remove(c.caller);
        CALLS.remove(c.callee);
        if (voiceLoaded()) {
            Voice.leave(c.caller);
            Voice.leave(c.callee);
        }
        ServerPlayer callee = p.server.getPlayerList().getPlayer(c.callee);
        ServerPlayer caller = p.server.getPlayerList().getPlayer(c.caller);
        PhoneData d = PhoneData.get(p.server);
        if (c.talkStart < 0) {
            // 出なかった／断られた／かけた人がやめた
            boolean declined = byUser && who.equals(c.callee);
            if (declined) {
                if (caller != null) PhoneServer.notify(caller, "call", "📞 通話できなかった", d.name(c.callee.toString()) + " はいま出られないみたい");
            } else {
                if (callee != null) PhoneServer.notify(callee, "message", "📞 不在着信", d.name(c.caller.toString()) + " から電話があったよ");
                if (caller != null && !byUser) PhoneServer.notify(caller, "call", "📞 応答なし", d.name(c.callee.toString()) + " は出なかったよ");
            }
        } else {
            long sec = (System.currentTimeMillis() - c.talkStart) / 1000;
            String t = String.format("%d:%02d", sec / 60, sec % 60);
            for (ServerPlayer sp : new ServerPlayer[]{caller, callee}) if (sp != null) PhoneServer.notify(sp, "call", "📞 通話終了", "通話時間 " + t);
        }
        for (UUID u : new UUID[]{c.caller, c.callee}) {
            ServerPlayer sp = p.server.getPlayerList().getPlayer(u);
            if (sp != null) send(sp);
        }
    }

    private static void error(ServerPlayer p, String msg) {
        JsonObject o = state(p);
        o.addProperty("error", msg);
        PhoneServer.send(p, "call", o);
    }

    private static void sendBoth(ServerPlayer p, Call c) {
        for (UUID u : new UUID[]{c.caller, c.callee}) {
            ServerPlayer sp = p.server.getPlayerList().getPlayer(u);
            if (sp != null) send(sp);
        }
    }

    static void send(ServerPlayer p) {
        PhoneServer.send(p, "call", state(p));
    }

    static JsonObject state(ServerPlayer p) {
        JsonObject o = new JsonObject();
        o.addProperty("voice", voiceLoaded() && Voice.ready());
        o.addProperty("myVoice", voiceLoaded() && Voice.connected(p.getUUID()));
        Call c = CALLS.get(p.getUUID());
        if (c == null) {
            o.addProperty("state", "idle");
        } else {
            boolean out = c.caller.equals(p.getUUID());
            UUID other = out ? c.callee : c.caller;
            boolean talking = c.talkStart >= 0;
            if (talking && voiceLoaded()) o.addProperty("otherVoice", Voice.connected(other));
            o.addProperty("state", talking ? "talking" : out ? "calling" : "ringing");
            o.addProperty("with", other.toString());
            o.addProperty("name", PhoneData.get(p.server).name(other.toString()));
            o.addProperty("talkMs", c.talkStart >= 0 ? System.currentTimeMillis() - c.talkStart : 0);
        }
        o.add("people", PhoneServer.people(p, PhoneData.get(p.server)));
        return o;
    }

    public static boolean ringing(UUID player) {
        Call c = CALLS.get(player);
        return c != null && c.callee.equals(player) && c.talkStart < 0;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (CALLS.isEmpty() || event.getServer().getTickCount() % 20 != 0) return;
        long now = event.getServer().getTickCount();
        for (Call c : CALLS.values().stream().distinct().toList()) {
            if (c.talkStart < 0 && now - c.start > RING_TICKS) {
                ServerPlayer caller = event.getServer().getPlayerList().getPlayer(c.caller);
                ServerPlayer any = caller != null ? caller : event.getServer().getPlayerList().getPlayer(c.callee);
                if (any != null) end(any, c.caller, false);
                else {
                    CALLS.remove(c.caller);
                    CALLS.remove(c.callee);
                }
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && CALLS.containsKey(p.getUUID())) end(p, p.getUUID(), true);
    }

    private CallService() {}
}
