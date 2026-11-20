package com.chigusa.jsnphone;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** Curios があるときだけ読まれる（ないときは呼ばない） */
final class CuriosCompat {
    static ItemStack find(Player p) {
        return CuriosApi.getCuriosInventory(p).flatMap(h -> h.findFirstCurio(JsnPhone.PHONE.get())).map(r -> r.stack()).orElse(ItemStack.EMPTY);
    }

    private CuriosCompat() {}
}
