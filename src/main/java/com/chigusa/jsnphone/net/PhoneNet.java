package com.chigusa.jsnphone.net;

import com.chigusa.jsnphone.JsnPhone;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * スマホの通信。中身は JSON の文字列で送る（アプリがふえても通信の形を変えなくてよい）。
 *  Req：画面 → サーバー（app と action と data）
 *  Res：サーバー → 画面（app の最新の様子）
 *  Notify：サーバー → 本人（通知のバナー）
 */
public final class PhoneNet {

    public record Req(String app, String action, String data) implements CustomPacketPayload {
        public static final Type<Req> TYPE = new Type<>(JsnPhone.id("req"));
        public static final StreamCodec<ByteBuf, Req> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Req::app, ByteBufCodecs.stringUtf8(64), Req::action, ByteBufCodecs.stringUtf8(8192), Req::data, Req::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Res(String app, String data) implements CustomPacketPayload {
        public static final Type<Res> TYPE = new Type<>(JsnPhone.id("res"));
        public static final StreamCodec<ByteBuf, Res> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(64), Res::app, ByteBufCodecs.stringUtf8(262144), Res::data, Res::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** kind：message / friend / news / call など（音とアイコンが変わる） */
    public record Notify(String kind, String title, String text) implements CustomPacketPayload {
        public static final Type<Notify> TYPE = new Type<>(JsnPhone.id("notify"));
        public static final StreamCodec<ByteBuf, Notify> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32), Notify::kind, ByteBufCodecs.stringUtf8(128), Notify::title, ByteBufCodecs.stringUtf8(512), Notify::text, Notify::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 写真のかけら：画面 → サーバー（meta は最初のかけらだけ。「幅|高さ|場所」） */
    public record PhotoUp(String id, int index, int total, byte[] data, String meta) implements CustomPacketPayload {
        public static final Type<PhotoUp> TYPE = new Type<>(JsnPhone.id("photo_up"));
        public static final StreamCodec<ByteBuf, PhotoUp> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(40), PhotoUp::id, ByteBufCodecs.VAR_INT, PhotoUp::index, ByteBufCodecs.VAR_INT, PhotoUp::total,
                ByteBufCodecs.byteArray(31000), PhotoUp::data, ByteBufCodecs.stringUtf8(128), PhotoUp::meta, PhotoUp::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 写真の画像：サーバー → 画面（空なら「もうない」） */
    public record PhotoDown(String id, byte[] data) implements CustomPacketPayload {
        public static final Type<PhotoDown> TYPE = new Type<>(JsnPhone.id("photo_down"));
        public static final StreamCodec<ByteBuf, PhotoDown> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(40), PhotoDown::id, ByteBufCodecs.byteArray(700_000), PhotoDown::data, PhotoDown::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(Req.TYPE, Req.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer sp) PhoneServer.handle(sp, p.app(), p.action(), p.data());
        }));
        r.playToClient(Res.TYPE, Res.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> com.chigusa.jsnphone.client.PhoneClient.onRes(p.app(), p.data())));
        r.playToServer(PhotoUp.TYPE, PhotoUp.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer sp) com.chigusa.jsnphone.photo.PhotoService.chunk(sp, p.id(), p.index(), p.total(), p.data(), p.meta());
        }));
        r.playToClient(PhotoDown.TYPE, PhotoDown.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> com.chigusa.jsnphone.client.PhotoCache.onData(p.id(), p.data())));
        r.playToClient(Notify.TYPE, Notify.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> com.chigusa.jsnphone.client.PhoneClient.onNotify(p.kind(), p.title(), p.text())));
    }

    private PhoneNet() {}
}
