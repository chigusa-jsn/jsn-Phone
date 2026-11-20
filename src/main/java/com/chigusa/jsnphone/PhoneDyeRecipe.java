package com.chigusa.jsnphone;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * スマホのケースの色を変える：スマホ＋染料（いくつでも、まぜた色になる）。
 * 金インゴット＝金、鉄インゴット＝銀、アメジストのかけら＝虹色、水バケツ＝もとの色にもどす。
 */
public class PhoneDyeRecipe extends CustomRecipe {
    public static final int GOLD = 0xE6B836, SILVER = 0xC8CCD6, RAINBOW = -2;
    /** クリエイティブのタブに並べる色 */
    public static final int[] SHOWCASE = {0xF0F0F0, 0x202024, 0xE03050, 0xF080B0, 0x3A9AF0, 0x50C060, 0xE0C030, 0x9060E0, 0xF08030, 0x40D0B0, GOLD, SILVER, RAINBOW};

    public PhoneDyeRecipe(CraftingBookCategory cat) {
        super(cat);
    }

    @Override
    public boolean matches(CraftingInput in, Level level) {
        int phones = 0, others = 0;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(JsnPhone.PHONE.get())) phones++;
            else if (s.getItem() instanceof DyeItem || s.is(Items.GOLD_INGOT) || s.is(Items.IRON_INGOT) || s.is(Items.AMETHYST_SHARD) || s.is(Items.WATER_BUCKET)) others++;
            else return false;
        }
        return phones == 1 && others > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider reg) {
        ItemStack phone = ItemStack.EMPTY;
        int r = 0, g = 0, b = 0, n = 0;
        Integer special = null;
        boolean wash = false;
        for (int i = 0; i < in.size(); i++) {
            ItemStack s = in.getItem(i);
            if (s.is(JsnPhone.PHONE.get())) phone = s.copyWithCount(1);
            else if (s.getItem() instanceof DyeItem dye) {
                int c = dye.getDyeColor().getTextureDiffuseColor();
                r += c >> 16 & 255;
                g += c >> 8 & 255;
                b += c & 255;
                n++;
            } else if (s.is(Items.GOLD_INGOT)) special = GOLD;
            else if (s.is(Items.IRON_INGOT)) special = SILVER;
            else if (s.is(Items.AMETHYST_SHARD)) special = RAINBOW;
            else if (s.is(Items.WATER_BUCKET)) wash = true;
        }
        if (phone.isEmpty()) return ItemStack.EMPTY;
        if (wash) phone.remove(JsnPhone.CASE_COLOR.get());
        else if (special != null) phone.set(JsnPhone.CASE_COLOR.get(), special);
        else if (n > 0) phone.set(JsnPhone.CASE_COLOR.get(), (r / n) << 16 | (g / n) << 8 | (b / n));
        return phone;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return JsnPhone.PHONE_DYE.get();
    }
}
