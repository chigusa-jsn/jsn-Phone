package com.chigusa.jsnphone;

import com.chigusa.jsnphone.net.PhoneNet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** jsn-Phone：マイクラの中のスマートフォン */
@Mod(JsnPhone.MODID)
public class JsnPhone {
    public static final String MODID = "jsn_phone";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredRegister<net.minecraft.core.component.DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    /** ケースの色（RGB）。-2 は虹色 */
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> CASE_COLOR =
            COMPONENTS.register("case_color", () -> net.minecraft.core.component.DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT).build());

    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<PhoneDyeRecipe>> PHONE_DYE =
            SERIALIZERS.register("phone_dye", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(PhoneDyeRecipe::new));

    public static final DeferredItem<PhoneItem> PHONE = ITEMS.register("smartphone", () -> new PhoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.literal("jsn-Phone"))
            .icon(() -> new ItemStack(PHONE.get()))
            .displayItems((p, out) -> {
                out.accept(PHONE.get());
                for (int c : PhoneDyeRecipe.SHOWCASE) {
                    ItemStack s = new ItemStack(PHONE.get());
                    s.set(CASE_COLOR.get(), c);
                    out.accept(s);
                }
            })
            .build());

    /** 画面を開く（クライアントだけがセットする） */
    public static Runnable openClient;

    public JsnPhone(IEventBus modBus) {
        COMPONENTS.register(modBus);
        SERIALIZERS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(PhoneNet::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
