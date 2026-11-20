package com.chigusa.jsnphone.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * スマホのデータ（ワールドに保存）。中身はまとめて JSON にして1つのタグに入れる。
 */
public class PhoneData extends SavedData {
    private static final Gson GSON = new GsonBuilder().create();
    public static final int MAX_POSTS = 300, MAX_NEWS = 200, MAX_MESSAGES = 200, MAX_MEMOS = 50;

    public static class Message { public String from; public String text; public long time; }
    public static class Thread { public List<Message> messages = new ArrayList<>(); public Map<String, Long> read = new HashMap<>(); }
    public static class Post {
        public long id; public String author; public String name; public String text; public long time;
        public Set<String> likes = new LinkedHashSet<>(); public long replyTo;
    }
    public static class News { public String text; public String kind; public long time; public String player; }
    public static class Settings { public int theme = 0; public int wallpaper = 0; public boolean share = true; public boolean notify = true; }

    public static class Content {
        /** 会話：キーは2人のUUIDを並べたもの */
        public Map<String, Thread> threads = new HashMap<>();
        public List<Post> posts = new ArrayList<>();
        public long nextPost = 1;
        public List<News> news = new ArrayList<>();
        public Map<String, Set<String>> friends = new HashMap<>();
        public Map<String, Set<String>> requests = new HashMap<>();   // 受け取った人 → 申し込んだ人
        public Map<String, List<String>> memos = new HashMap<>();
        public Map<String, Settings> settings = new HashMap<>();
        public Map<String, String> names = new HashMap<>();
        public Map<String, Long> newsRead = new HashMap<>();
    }

    public Content c = new Content();

    public static PhoneData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PhoneData::new, PhoneData::load, null), "jsn_phone");
    }

    private static PhoneData load(CompoundTag tag, HolderLookup.Provider reg) {
        PhoneData d = new PhoneData();
        try {
            Content c = GSON.fromJson(tag.getString("json"), Content.class);
            if (c != null) d.c = c;
        } catch (Exception ignored) {
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putString("json", GSON.toJson(c));
        return tag;
    }

    public static String threadKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    public Settings settings(String uuid) {
        return c.settings.computeIfAbsent(uuid, k -> new Settings());
    }

    public Set<String> friends(String uuid) {
        return c.friends.computeIfAbsent(uuid, k -> new LinkedHashSet<>());
    }

    public Set<String> requests(String uuid) {
        return c.requests.computeIfAbsent(uuid, k -> new LinkedHashSet<>());
    }

    public String name(String uuid) {
        return c.names.getOrDefault(uuid, "？？？");
    }

    public void addNews(String text, String kind, String player) {
        News n = new News();
        n.text = text;
        n.kind = kind;
        n.time = System.currentTimeMillis();
        n.player = player;
        c.news.add(0, n);
        while (c.news.size() > MAX_NEWS) c.news.remove(c.news.size() - 1);
        setDirty();
    }
}
