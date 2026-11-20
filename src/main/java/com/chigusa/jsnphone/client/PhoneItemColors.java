package com.chigusa.jsnphone.client;

import com.chigusa.jsnphone.JsnPhone;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** 持ち物や手に持ったスマホの色（ケースの部分＝絵の1枚目だけ色をつける） */
@EventBusSubscriber(modid = JsnPhone.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PhoneItemColors {
    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            if (layer != 0) return -1;
            var p = Minecraft.getInstance().player;
            return PhoneColors.caseColor(stack, p == null ? 0 : p.tickCount);
        }, JsnPhone.PHONE.get());
    }

    private PhoneItemColors() {}
}
