package com.chigusa.jsnphone.client;

import com.chigusa.jsnphone.JsnPhone;
import net.minecraft.world.item.ItemStack;

/** ケースの色（染料で変えた色。虹色は動く） */
public final class PhoneColors {
    public static final int RAINBOW = -2;

    public static int caseColor(ItemStack stack, int ticks) {
        Integer c = stack.isEmpty() ? null : stack.get(JsnPhone.CASE_COLOR.get());
        if (c == null) return 0xFF2A2A30;
        if (c == RAINBOW) return 0xFF000000 | java.awt.Color.HSBtoRGB((ticks % 120) / 120F, 0.6F, 1F) & 0xFFFFFF;
        return 0xFF000000 | c;
    }

    public static int darker(int c) {
        return 0xFF000000 | ((c >> 16 & 255) * 3 / 4) << 16 | ((c >> 8 & 255) * 3 / 4) << 8 | (c & 255) * 3 / 4;
    }

    private PhoneColors() {}
}
