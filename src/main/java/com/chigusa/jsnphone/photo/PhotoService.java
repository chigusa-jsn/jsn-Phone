package com.chigusa.jsnphone.photo;

import com.chigusa.jsnphone.JsnPhone;
import com.chigusa.jsnphone.data.PhoneData;
import com.chigusa.jsnphone.net.PhoneNet;
import com.chigusa.jsnphone.net.PhoneServer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 写真：スマホのカメラで撮った画像（JPEG）を、ワールドのフォルダ jsn_phone_photos に保存する。
 * 送るときは小分け（パケットの大きさに上限がある）。
 */
public final class PhotoService {
    public static final int MAX_BYTES = 600_000, MAX_PER_PLAYER = 60, CHUNK = 30_000;

    private static final class Upload {
        String id;
        int total;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int next;
        String meta;
    }

    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();

    static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("jsn_phone_photos");
    }

    static boolean validId(String id) {
        return id != null && id.matches("[0-9a-f]{8,40}");
    }

    /** 届いたかけら。最後まで届いたら保存する */
    public static void chunk(ServerPlayer p, String upId, int index, int total, byte[] data, String meta) {
        if (!validId(upId) || total <= 0 || total > MAX_BYTES / CHUNK + 1) return;
        Upload u = UPLOADS.get(p.getUUID());
        if (index == 0) {
            u = new Upload();
            u.id = upId;
            u.total = total;
            u.meta = meta;
            UPLOADS.put(p.getUUID(), u);
        }
        if (u == null || !u.id.equals(upId) || u.next != index) return;
        u.buf.writeBytes(data);
        u.next++;
        if (u.buf.size() > MAX_BYTES) {
            UPLOADS.remove(p.getUUID());
            PhoneServer.notify(p, "photo", "📷 写真", "写真が大きすぎて保存できなかった");
            return;
        }
        if (u.next < u.total) return;
        UPLOADS.remove(p.getUUID());
        save(p, u);
    }

    private static void save(ServerPlayer p, Upload u) {
        PhoneData d = PhoneData.get(p.server);
        long mine = d.c.photos.stream().filter(ph -> ph.owner.equals(p.getStringUUID())).count();
        if (mine >= MAX_PER_PLAYER) {
            PhoneServer.notify(p, "photo", "📷 写真がいっぱい", "写真アプリで古い写真を消してね（" + MAX_PER_PLAYER + "枚まで）");
            return;
        }
        byte[] bytes = u.buf.toByteArray();
        // JPEG か確かめる
        if (bytes.length < 4 || (bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8) return;
        try {
            Files.createDirectories(dir(p.server));
            Files.write(dir(p.server).resolve(u.id + ".jpg"), bytes);
        } catch (Exception e) {
            JsnPhone.LOG.warn("写真を保存できなかった", e);
            return;
        }
        PhoneData.Photo ph = new PhoneData.Photo();
        ph.id = u.id;
        ph.owner = p.getStringUUID();
        ph.time = System.currentTimeMillis();
        String[] m = u.meta == null ? new String[0] : u.meta.split("\\|", 3);
        try {
            ph.w = m.length > 0 ? Integer.parseInt(m[0]) : 0;
            ph.h = m.length > 1 ? Integer.parseInt(m[1]) : 0;
        } catch (NumberFormatException ignored) {
        }
        ph.place = m.length > 2 ? m[2].substring(0, Math.min(60, m[2].length())) : "";
        d.c.photos.add(0, ph);
        d.setDirty();
        handle(p, d, "list", new JsonObject());
    }

    public static PhoneData.Photo find(PhoneData d, String id) {
        for (PhoneData.Photo ph : d.c.photos) if (ph.id.equals(id)) return ph;
        return null;
    }

    public static void handle(ServerPlayer p, PhoneData d, String action, JsonObject in) {
        String me = p.getStringUUID();
        String id = in.has("id") ? in.get("id").getAsString() : "";
        switch (action) {
            case "fetch" -> {
                // 画像をとどける（写真のIDを知っている人なら見られる：つぶやき・メッセージに貼られたもの）
                if (!validId(id) || find(d, id) == null) {
                    PacketDistributor.sendToPlayer(p, new PhoneNet.PhotoDown(id, new byte[0]));
                    return;
                }
                try {
                    byte[] b = Files.readAllBytes(dir(p.server).resolve(id + ".jpg"));
                    PacketDistributor.sendToPlayer(p, new PhoneNet.PhotoDown(id, b));
                } catch (Exception e) {
                    PacketDistributor.sendToPlayer(p, new PhoneNet.PhotoDown(id, new byte[0]));
                }
                return;
            }
            case "delete" -> {
                PhoneData.Photo ph = find(d, id);
                if (ph != null && ph.owner.equals(me)) {
                    d.c.photos.remove(ph);
                    d.setDirty();
                    try {
                        Files.deleteIfExists(dir(p.server).resolve(id + ".jpg"));
                    } catch (Exception ignored) {
                    }
                }
            }
            default -> {}
        }
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        for (PhoneData.Photo ph : d.c.photos) {
            if (!ph.owner.equals(me)) continue;
            JsonObject j = new JsonObject();
            j.addProperty("id", ph.id);
            j.addProperty("time", ph.time);
            j.addProperty("place", ph.place == null ? "" : ph.place);
            j.addProperty("w", ph.w);
            j.addProperty("h", ph.h);
            a.add(j);
        }
        o.add("photos", a);
        o.addProperty("max", MAX_PER_PLAYER);
        o.add("people", PhoneServer.people(p, d));
        PhoneServer.send(p, "photos", o);
    }

    public static void logout(ServerPlayer p) {
        UPLOADS.remove(p.getUUID());
    }

    private PhotoService() {}
}
