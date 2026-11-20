package com.chigusa.jsnphone;

import com.chigusa.jsnphone.data.PhoneData;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/** お知らせ（自動ニュース）：進捗・レベル・ボス討伐・はじめての参加。 */
@EventBusSubscriber(modid = JsnPhone.MODID)
public final class PhoneEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        PhoneData d = PhoneData.get(p.server);
        boolean first = !d.c.names.containsKey(p.getStringUUID());
        d.c.names.put(p.getStringUUID(), p.getName().getString());
        d.setDirty();
        if (first) d.addNews(p.getName().getString() + " がはじめてやってきた！", "join", p.getStringUUID());
        int req = d.requests(p.getStringUUID()).size();
        if (req > 0) com.chigusa.jsnphone.net.PhoneServer.notify(p, "friend", "フレンド申請", req + " 件のフレンド申請がとどいているよ");
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        AdvancementHolder a = event.getAdvancement();
        a.value().display().ifPresent(disp -> {
            if (!disp.shouldAnnounceChat()) return;
            String kind = switch (disp.getType()) {
                case CHALLENGE -> "challenge";
                case GOAL -> "goal";
                default -> "advancement";
            };
            PhoneApi.news(p, p.getName().getString() + " が進捗「" + disp.getTitle().getString() + "」を達成！", kind);
        });
    }

    @SubscribeEvent
    public static void onLevel(PlayerXpEvent.LevelChange event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getLevels() <= 0) return;
        int before = p.experienceLevel, after = before + event.getLevels();
        // 10レベルごと（10, 20, 30 …）をこえたとき
        if (after / 10 > before / 10 && after >= 10) PhoneApi.news(p, p.getName().getString() + " が経験値レベル " + (after / 10 * 10) + " を突破！", "level");
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        boolean boss = e.getType().is(Tags.EntityTypes.BOSSES) || e.getType() == EntityType.ELDER_GUARDIAN || e.getType() == EntityType.WARDEN;
        if (boss) PhoneApi.news(p, p.getName().getString() + " が " + e.getName().getString() + " をたおした！", "boss");
    }

    private PhoneEvents() {}
}
