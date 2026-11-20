package com.chigusa.jsnphone;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 死んでもスマホはなくならない（持ち物の中のスマホは、生き返ったときに戻ってくる）。
 * ふつうに捨てたり、人にわたしたり、チェストにしまったりはできる。
 * Curios の「スマホ」の枠に入れたものは、枠の設定（ALWAYS_KEEP）でそのまま残る。
 */
@EventBusSubscriber(modid = JsnPhone.MODID)
public final class PhoneKeep {
    private static final String KEY = "jsn_phone_keep";

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        HolderLookup.Provider reg = p.registryAccess();
        ListTag kept = new ListTag();
        event.getDrops().removeIf(e -> {
            ItemStack s = e.getItem();
            if (!s.is(JsnPhone.PHONE.get())) return false;
            kept.add(s.save(reg));
            return true;
        });
        if (!kept.isEmpty()) p.getPersistentData().put(KEY, kept);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag old = event.getOriginal().getPersistentData();
        if (!old.contains(KEY)) return;
        ListTag list = old.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.parse(p.registryAccess(), list.getCompound(i)).orElse(ItemStack.EMPTY);
            if (!s.isEmpty() && !p.getInventory().add(s)) p.drop(s, false);
        }
        old.remove(KEY);
    }

    private PhoneKeep() {}
}
