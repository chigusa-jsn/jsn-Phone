package com.chigusa.jsnphone.net;

import com.chigusa.jsnphone.call.CallService;
import com.chigusa.jsnphone.compat.BankCompat;
import com.chigusa.jsnphone.data.PhoneData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

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
        switch (app) {
            case "call" -> CallService.handle(p, action, in);
            case "bank" -> bank(p, d, action, in);
            case "jsncraft" -> jsnCraft(p);
            default -> {}
        }
    }

    // ================= 銀行（Lightman's Currency） =================
    static void bank(ServerPlayer p, PhoneData d, String action, JsonObject in) {
        if (!ModList.get().isLoaded("lightmanscurrency") || !BankCompat.ready()) {
            JsonObject o = new JsonObject();
            o.addProperty("error", "Lightman's Currency MODが入っていないよ");
            PhoneServer.send(p, "bank", o);
            return;
        }
        String msg = null, err = null;
        if (action.equals("send")) {
            String to = in.has("to") ? in.get("to").getAsString() : "";
            long amount = in.has("amount") ? in.get("amount").getAsLong() : 0;
            UUID toId;
            try {
                toId = UUID.fromString(to);
            } catch (Exception e) {
                toId = null;
            }
            if (toId == null || toId.equals(p.getUUID())) err = "送る相手をえらんでね";
            else {
                err = BankCompat.send(p, toId, amount);
                if (err == null) {
                    String text = BankCompat.text(amount);
                    String note = in.has("note") ? PhoneServer.clip(in.get("note").getAsString(), 40) : "";
                    d.addBankLog(p.getStringUUID(), to, amount, false, note);
                    d.addBankLog(to, p.getStringUUID(), amount, true, note);
                    msg = d.name(to) + " に " + text + " を送ったよ";
                    ServerPlayer o = PhoneServer.online(p.server, to);
                    if (o != null) {
                        PhoneServer.notify(o, "bank", "💰 入金", p.getName().getString() + " から " + text + (note.isEmpty() ? "" : "「" + note + "」"));
                        bankState(o, d, null, null);
                    }
                }
            }
        }
        bankState(p, d, msg, err);
    }

    static void bankState(ServerPlayer p, PhoneData d, String msg, String err) {
        JsonObject o = new JsonObject();
        o.addProperty("balance", BankCompat.balance(p.getUUID()));
        o.addProperty("text", BankCompat.balanceText(p.getUUID()));
        // コインの単位（1, 10, 100…）が何と書かれるか
        JsonArray units = new JsonArray();
        for (long u = 1; u <= 100000; u *= 10) {
            JsonObject j = new JsonObject();
            j.addProperty("value", u);
            j.addProperty("text", BankCompat.text(u));
            units.add(j);
        }
        o.add("units", units);
        JsonArray log = new JsonArray();
        var list = d.c.bank == null ? null : d.c.bank.get(p.getStringUUID());
        if (list != null) for (PhoneData.BankLog l : list) {
            JsonObject j = new JsonObject();
            j.addProperty("name", d.name(l.with));
            j.addProperty("text", BankCompat.text(l.amount));
            j.addProperty("in", l.in);
            j.addProperty("time", l.time);
            j.addProperty("note", l.note == null ? "" : l.note);
            log.add(j);
        }
        o.add("log", log);
        o.add("people", PhoneServer.people(p, d));
        if (msg != null) o.addProperty("msg", msg);
        if (err != null) o.addProperty("error", err);
        PhoneServer.send(p, "bank", o);
    }

    // ================= jsn-Craft =================
    private static Method craftInfo;

    static void jsnCraft(ServerPlayer p) {
        JsonObject o;
        try {
            if (craftInfo == null) craftInfo = Class.forName("com.chigusa.jsncraft.compat.PhoneBridge").getMethod("info", ServerPlayer.class);
            o = JsonParser.parseString((String) craftInfo.invoke(null, p)).getAsJsonObject();
        } catch (Throwable t) {
            o = new JsonObject();
            o.addProperty("error", "jsn-Craft MODが入っていないよ");
        }
        PhoneServer.send(p, "jsncraft", o);
    }

    private PhoneApps() {}
}
