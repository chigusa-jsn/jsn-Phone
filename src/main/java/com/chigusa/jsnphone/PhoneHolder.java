package com.chigusa.jsnphone;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** スマホを持っているか（手・持ち物・Curios の「スマホ」の枠） */
public final class PhoneHolder {

    public static ItemStack find(Player p) {
        if (p == null) return ItemStack.EMPTY;
        if (p.getMainHandItem().is(JsnPhone.PHONE.get())) return p.getMainHandItem();
        if (p.getOffhandItem().is(JsnPhone.PHONE.get())) return p.getOffhandItem();
        if (ModList.get().isLoaded("curios")) {
            ItemStack c = CuriosCompat.find(p);
            if (!c.isEmpty()) return c;
        }
        for (ItemStack s : p.getInventory().items) if (s.is(JsnPhone.PHONE.get())) return s;
        return ItemStack.EMPTY;
    }

    private PhoneHolder() {}
}
