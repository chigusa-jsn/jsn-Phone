package com.chigusa.jsnphone.net;

import com.chigusa.jsnphone.JsnPhone;
import com.chigusa.jsnphone.data.PhoneData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** スマホのアプリの、サーバー側の処理。 */
public final class PhoneServer {

    public static void handle(ServerPlayer p, String app, String action, String raw) {
        JsonObject in;
        try {
            in = raw == null || raw.isEmpty() ? new JsonObject() : JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        PhoneData d = PhoneData.get(p.server);
        String me = p.getStringUUID();
        d.c.names.put(me, p.getName().getString());
        switch (app) {
            case "home" -> send(p, "home", home(p, d));
            case "messages" -> messages(p, d, action, in);
            case "sns" -> sns(p, d, action, in);
            case "news" -> {
                d.c.newsRead.put(me, System.currentTimeMillis());
                d.setDirty();
                send(p, "news", news(d));
            }
            case "friends" -> friends(p, d, action, in);
            case "memo" -> memo(p, d, action, in);
            case "settings" -> settings(p, d, action, in);
            case "photos" -> com.chigusa.jsnphone.photo.PhotoService.handle(p, d, action, in);
            default -> PhoneApps.handle(p, d, app, action, in);
        }
    }

    public static void send(ServerPlayer p, String app, JsonObject o) {
        PacketDistributor.sendToPlayer(p, new PhoneNet.Res(app, o.toString()));
    }

    public static void notify(ServerPlayer p, String kind, String title, String text) {
        PhoneData d = PhoneData.get(p.server);
        if (!d.settings(p.getStringUUID()).notify && !kind.equals("call")) return;
        PacketDistributor.sendToPlayer(p, new PhoneNet.Notify(kind, title, text));
    }

    static ServerPlayer online(MinecraftServer server, String uuid) {
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(uuid));
        } catch (Exception e) {
            return null;
        }
    }

    static String clip(String s, int max) {
        if (s == null) return "";
        s = s.replace('\n', ' ').replace('§', '&').trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ================= ホーム：未読の数 =================
    static JsonObject home(ServerPlayer p, PhoneData d) {
        String me = p.getStringUUID();
        JsonObject o = new JsonObject();
        int unread = 0;
        for (Map.Entry<String, PhoneData.Thread> e : d.c.threads.entrySet()) {
            if (!e.getKey().contains(me)) continue;
            long read = e.getValue().read.getOrDefault(me, 0L);
            for (PhoneData.Message m : e.getValue().messages) if (m.time > read && !m.from.equals(me)) unread++;
        }
        long newsRead = d.c.newsRead.getOrDefault(me, 0L);
        int news = 0;
        for (PhoneData.News n : d.c.news) if (n.time > newsRead) news++;
        o.addProperty("messages", unread);
        o.addProperty("news", news);
        o.addProperty("friends", d.requests(me).size());
        PhoneData.Settings s = d.settings(me);
        o.addProperty("theme", s.theme);
        o.addProperty("wallpaper", s.wallpaper);
        o.addProperty("share", s.share);
        o.addProperty("notify", s.notify);
        o.add("apps", PhoneApps.available());
        return o;
    }

    // ================= メッセージ =================
    static void messages(ServerPlayer p, PhoneData d, String action, JsonObject in) {
        String me = p.getStringUUID();
        if (action.equals("send")) {
            String to = in.has("to") ? in.get("to").getAsString() : "";
            String text = clip(in.has("text") ? in.get("text").getAsString() : "", 200);
            String photo = in.has("photo") ? in.get("photo").getAsString() : null;
            if (photo != null && com.chigusa.jsnphone.photo.PhotoService.find(d, photo) == null) photo = null;
            if (to.isEmpty() || (text.isEmpty() && photo == null) || to.equals(me) || !d.c.names.containsKey(to)) return;
            PhoneData.Thread t = d.c.threads.computeIfAbsent(PhoneData.threadKey(me, to), k -> new PhoneData.Thread());
            PhoneData.Message m = new PhoneData.Message();
            m.from = me;
            m.text = text;
            m.photo = photo;
            m.time = System.currentTimeMillis();
            t.messages.add(m);
            while (t.messages.size() > PhoneData.MAX_MESSAGES) t.messages.remove(0);
            t.read.put(me, m.time);
            d.setDirty();
            ServerPlayer other = online(p.server, to);
            if (other != null) {
                notify(other, "message", p.getName().getString(), text.isEmpty() ? "📷 写真" : text);
                send(other, "messages_push", thread(d, other.getStringUUID(), me));
            }
            send(p, "messages", thread(d, me, to));
            return;
        }
        if (action.equals("open")) {
            String with = in.has("with") ? in.get("with").getAsString() : "";
            PhoneData.Thread t = d.c.threads.get(PhoneData.threadKey(me, with));
            if (t != null) {
                t.read.put(me, System.currentTimeMillis());
                d.setDirty();
            }
            send(p, "messages", thread(d, me, with));
            return;
        }
        // 一覧
        JsonObject o = new JsonObject();
        JsonArray list = new JsonArray();
        List<Object[]> rows = new ArrayList<>();
        for (Map.Entry<String, PhoneData.Thread> e : d.c.threads.entrySet()) {
            String[] ab = e.getKey().split("\\|");
            if (ab.length != 2 || (!ab[0].equals(me) && !ab[1].equals(me))) continue;
            String other = ab[0].equals(me) ? ab[1] : ab[0];
            PhoneData.Thread t = e.getValue();
            if (t.messages.isEmpty()) continue;
            PhoneData.Message last = t.messages.get(t.messages.size() - 1);
            long read = t.read.getOrDefault(me, 0L);
            int unread = 0;
            for (PhoneData.Message m : t.messages) if (m.time > read && !m.from.equals(me)) unread++;
            rows.add(new Object[]{other, last, unread});
        }
        rows.sort((x, y) -> Long.compare(((PhoneData.Message) y[1]).time, ((PhoneData.Message) x[1]).time));
        for (Object[] r : rows) {
            JsonObject j = new JsonObject();
            j.addProperty("uuid", (String) r[0]);
            j.addProperty("name", d.name((String) r[0]));
            PhoneData.Message lm = (PhoneData.Message) r[1];
            j.addProperty("last", lm.text.isEmpty() && lm.photo != null ? "📷 写真" : lm.text);
            j.addProperty("time", ((PhoneData.Message) r[1]).time);
            j.addProperty("unread", (int) r[2]);
            j.addProperty("online", online(p.server, (String) r[0]) != null);
            list.add(j);
        }
        o.add("threads", list);
        o.add("people", people(p, d));
        send(p, "messages", o);
    }

    /** メッセージを送れる相手：フレンドと、いまオンラインの人 */
    public static JsonArray people(ServerPlayer p, PhoneData d) {
        String me = p.getStringUUID();
        JsonArray a = new JsonArray();
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>(d.friends(me));
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            d.c.names.put(o.getStringUUID(), o.getName().getString());
            ids.add(o.getStringUUID());
        }
        ids.remove(me);
        for (String id : ids) {
            JsonObject j = new JsonObject();
            j.addProperty("uuid", id);
            j.addProperty("name", d.name(id));
            j.addProperty("online", online(p.server, id) != null);
            j.addProperty("friend", d.friends(me).contains(id));
            a.add(j);
        }
        return a;
    }

    static JsonObject thread(PhoneData d, String me, String with) {
        JsonObject o = new JsonObject();
        o.addProperty("with", with);
        o.addProperty("name", d.name(with));
        JsonArray msgs = new JsonArray();
        PhoneData.Thread t = d.c.threads.get(PhoneData.threadKey(me, with));
        if (t != null) {
            int from = Math.max(0, t.messages.size() - 60);
            for (int i = from; i < t.messages.size(); i++) {
                PhoneData.Message m = t.messages.get(i);
                JsonObject j = new JsonObject();
                j.addProperty("me", m.from.equals(me));
                j.addProperty("text", m.text);
                j.addProperty("time", m.time);
                if (m.photo != null) j.addProperty("photo", m.photo);
                msgs.add(j);
            }
            long otherRead = t.read.getOrDefault(with, 0L);
            o.addProperty("seen", otherRead);
        }
        o.add("messages", msgs);
        return o;
    }

    // ================= つぶやき（SNS） =================
    static void sns(ServerPlayer p, PhoneData d, String action, JsonObject in) {
        String me = p.getStringUUID();
        switch (action) {
            case "post" -> {
                String text = clip(in.has("text") ? in.get("text").getAsString() : "", 140);
                String photo = in.has("photo") ? in.get("photo").getAsString() : null;
                if (photo != null && com.chigusa.jsnphone.photo.PhotoService.find(d, photo) == null) photo = null;
                if (text.isEmpty() && photo == null) break;
                PhoneData.Post post = new PhoneData.Post();
                post.id = d.c.nextPost++;
                post.author = me;
                post.name = p.getName().getString();
                post.text = text;
                post.photo = photo;
                post.time = System.currentTimeMillis();
                post.replyTo = in.has("reply") ? in.get("reply").getAsLong() : 0;
                d.c.posts.add(0, post);
                while (d.c.posts.size() > PhoneData.MAX_POSTS) d.c.posts.remove(d.c.posts.size() - 1);
                d.setDirty();
                // 返信なら相手に通知
                if (post.replyTo != 0) for (PhoneData.Post q : d.c.posts) if (q.id == post.replyTo && !q.author.equals(me)) {
                    ServerPlayer o = online(p.server, q.author);
                    if (o != null) notify(o, "sns", p.getName().getString() + " が返信", text);
                }
            }
            case "like" -> {
                long id = in.has("id") ? in.get("id").getAsLong() : -1;
                for (PhoneData.Post q : d.c.posts) if (q.id == id) {
                    if (!q.likes.remove(me)) {
                        q.likes.add(me);
                        ServerPlayer o = q.author.equals(me) ? null : online(p.server, q.author);
                        if (o != null) notify(o, "like", p.getName().getString() + " がいいね", q.text);
                    }
                    d.setDirty();
                }
            }
            case "delete" -> {
                long id = in.has("id") ? in.get("id").getAsLong() : -1;
                boolean op = p.hasPermissions(2);
                d.c.posts.removeIf(q -> q.id == id && (q.author.equals(me) || op));
                d.setDirty();
            }
            default -> {}
        }
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        int n = 0;
        for (PhoneData.Post q : d.c.posts) {
            if (n++ >= 100) break;
            JsonObject j = new JsonObject();
            j.addProperty("id", q.id);
            j.addProperty("name", q.name);
            j.addProperty("text", q.text);
            j.addProperty("time", q.time);
            j.addProperty("likes", q.likes.size());
            j.addProperty("liked", q.likes.contains(me));
            j.addProperty("mine", q.author.equals(me));
            j.addProperty("reply", q.replyTo);
            if (q.photo != null) j.addProperty("photo", q.photo);
            a.add(j);
        }
        o.add("posts", a);
        send(p, "sns", o);
    }

    // ================= お知らせ =================
    static JsonObject news(PhoneData d) {
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        for (PhoneData.News n : d.c.news) {
            JsonObject j = new JsonObject();
            j.addProperty("text", n.text);
            j.addProperty("kind", n.kind);
            j.addProperty("time", n.time);
            a.add(j);
        }
        o.add("news", a);
        return o;
    }

    // ================= フレンドとレーダー =================
    static void friends(ServerPlayer p, PhoneData d, String action, JsonObject in) {
        String me = p.getStringUUID();
        String who = in.has("uuid") ? in.get("uuid").getAsString() : "";
        switch (action) {
            case "request" -> {
                if (who.isEmpty() || who.equals(me) || d.friends(me).contains(who)) break;
                if (d.requests(me).remove(who)) {   // 相手からも申し込まれていたら、そのままフレンド
                    d.friends(me).add(who);
                    d.friends(who).add(me);
                } else {
                    d.requests(who).add(me);
                    ServerPlayer o = online(p.server, who);
                    if (o != null) notify(o, "friend", "フレンド申請", p.getName().getString() + " からフレンド申請がきたよ");
                }
                d.setDirty();
            }
            case "accept" -> {
                if (d.requests(me).remove(who)) {
                    d.friends(me).add(who);
                    d.friends(who).add(me);
                    ServerPlayer o = online(p.server, who);
                    if (o != null) notify(o, "friend", "フレンド", p.getName().getString() + " とフレンドになったよ");
                    d.setDirty();
                }
            }
            case "decline" -> {
                d.requests(me).remove(who);
                d.setDirty();
            }
            case "remove" -> {
                d.friends(me).remove(who);
                d.friends(who).remove(me);
                d.setDirty();
            }
            default -> {}
        }
        JsonObject o = new JsonObject();
        JsonArray fr = new JsonArray();
        for (String id : d.friends(me)) {
            JsonObject j = new JsonObject();
            j.addProperty("uuid", id);
            j.addProperty("name", d.name(id));
            ServerPlayer op = online(p.server, id);
            j.addProperty("online", op != null);
            if (op != null && d.settings(id).share) {
                j.addProperty("dim", op.level().dimension().location().toString());
                j.addProperty("x", op.getX());
                j.addProperty("y", op.getY());
                j.addProperty("z", op.getZ());
            }
            fr.add(j);
        }
        o.add("friends", fr);
        JsonArray rq = new JsonArray();
        for (String id : d.requests(me)) {
            JsonObject j = new JsonObject();
            j.addProperty("uuid", id);
            j.addProperty("name", d.name(id));
            rq.add(j);
        }
        o.add("requests", rq);
        JsonArray others = new JsonArray();
        Set<String> f = d.friends(me);
        for (ServerPlayer op : p.server.getPlayerList().getPlayers()) {
            String id = op.getStringUUID();
            if (id.equals(me) || f.contains(id)) continue;
            JsonObject j = new JsonObject();
            j.addProperty("uuid", id);
            j.addProperty("name", op.getName().getString());
            j.addProperty("sent", d.requests(id).contains(me));
            others.add(j);
        }
        o.add("others", others);
        send(p, "friends", o);
    }

    // ================= メモ =================
    static void memo(ServerPlayer p, PhoneData d, String action, JsonObject in) {
        List<String> memos = d.c.memos.computeIfAbsent(p.getStringUUID(), k -> new ArrayList<>());
        int i = in.has("index") ? in.get("index").getAsInt() : -1;
        String text = clip(in.has("text") ? in.get("text").getAsString() : "", 300);
        switch (action) {
            case "add" -> { if (!text.isEmpty() && memos.size() < PhoneData.MAX_MEMOS) memos.add(0, text); }
            case "edit" -> { if (i >= 0 && i < memos.size() && !text.isEmpty()) memos.set(i, text); }
            case "delete" -> { if (i >= 0 && i < memos.size()) memos.remove(i); }
            default -> {}
        }
        if (!action.equals("list")) d.setDirty();
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        for (String m : memos) a.add(m);
        o.add("memos", a);
        send(p, "memo", o);
    }

    // ================= 設定 =================
    static void settings(ServerPlayer p, PhoneData d, String action, JsonObject in) {
        PhoneData.Settings s = d.settings(p.getStringUUID());
        if (action.equals("set")) {
            if (in.has("theme")) s.theme = Math.max(0, Math.min(31, in.get("theme").getAsInt()));
            if (in.has("wallpaper")) s.wallpaper = Math.max(0, Math.min(31, in.get("wallpaper").getAsInt()));
            if (in.has("share")) s.share = in.get("share").getAsBoolean();
            if (in.has("notify")) s.notify = in.get("notify").getAsBoolean();
            d.setDirty();
        }
        send(p, "home", home(p, d));
    }

    private PhoneServer() {}
}
