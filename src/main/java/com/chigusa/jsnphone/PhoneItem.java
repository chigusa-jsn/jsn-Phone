package com.chigusa.jsnphone;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** スマートフォン。右クリック（またはPキー）で開く。 */
public class PhoneItem extends Item {
    public PhoneItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide && JsnPhone.openClient != null) JsnPhone.openClient.run();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("右クリック、または Pキーで開く").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("持ち物に入っていれば、メッセージの通知が届く").withStyle(ChatFormatting.DARK_GRAY));
    }
}
