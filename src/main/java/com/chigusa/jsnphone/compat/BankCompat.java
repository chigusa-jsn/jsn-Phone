package com.chigusa.jsnphone.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Lightman's Currency の銀行を、リフレクションで使う（ライトマンがなくても動くように、直接はつながない）。
 * お金は「いちばん小さいコイン（銅貨）何枚ぶん」の数で扱う。
 */
public final class BankCompat {
    private static final String P = "io.github.lightman314.lightmanscurrency.api.money.";
    private static boolean tried, ok;
    private static Method getApi, refOf, refGet, storageOf, valueOf, fromNumber, uniqueName, coreValue, valueText, transfer, allText, containsValue;

    private static synchronized boolean init() {
        if (tried) return ok;
        tried = true;
        try {
            Class<?> api = Class.forName(P + "bank.BankAPI");
            Class<?> ref = Class.forName(P + "bank.reference.BankReference");
            Class<?> pref = Class.forName(P + "bank.reference.builtin.PlayerBankReference");
            Class<?> acc = Class.forName(P + "bank.IBankAccount");
            Class<?> storage = Class.forName(P + "value.MoneyStorage");
            Class<?> money = Class.forName(P + "value.MoneyValue");
            Class<?> coin = Class.forName(P + "value.builtin.CoinValue");
            getApi = api.getMethod("getApi");
            refOf = pref.getMethod("of", UUID.class);
            refGet = ref.getMethod("get");
            storageOf = acc.getMethod("getMoneyStorage");
            valueOf = storage.getMethod("valueOf", String.class);
            allText = storage.getMethod("getAllValueText");
            containsValue = storage.getMethod("containsValue", money);
            fromNumber = coin.getMethod("fromNumber", String.class, long.class);
            uniqueName = money.getMethod("getUniqueName");
            coreValue = money.getMethod("getCoreValue");
            valueText = money.getMethod("getString");
            transfer = api.getMethod("BankTransfer", Player.class, ref, money, acc);
            ok = true;
        } catch (Throwable t) {
            com.chigusa.jsnphone.JsnPhone.LOG.warn("Lightman's Currency の銀行につながらなかった: {}", t.toString());
        }
        return ok;
    }

    public static boolean ready() {
        return init();
    }

    private static Object account(UUID player) throws Exception {
        Object ref = refOf.invoke(null, player);
        return ref == null ? null : refGet.invoke(ref);
    }

    private static Object money(long amount) throws Exception {
        return fromNumber.invoke(null, "main", amount);
    }

    /** 残高（銅貨の枚数ぶん。メインのコインだけ） */
    public static long balance(UUID player) {
        if (!init()) return -1;
        try {
            Object acc = account(player);
            if (acc == null) return -1;
            Object st = storageOf.invoke(acc);
            Object v = valueOf.invoke(st, (String) uniqueName.invoke(money(1)));
            return (long) coreValue.invoke(v);
        } catch (Throwable t) {
            return -1;
        }
    }

    /** 残高を、ライトマンの書き方（例：1g 5i 3c）で */
    public static String balanceText(UUID player) {
        if (!init()) return "";
        try {
            Object acc = account(player);
            if (acc == null) return "";
            return ((Component) allText.invoke(storageOf.invoke(acc))).getString();
        } catch (Throwable t) {
            return "";
        }
    }

    public static String text(long amount) {
        if (!init()) return String.valueOf(amount);
        try {
            return (String) valueText.invoke(money(amount));
        } catch (Throwable t) {
            return String.valueOf(amount);
        }
    }

    /** 送金。成功したらnull、だめなら理由 */
    public static String send(Player from, UUID to, long amount) {
        if (!init()) return "銀行につながらないよ";
        try {
            if (amount <= 0) return "金額をえらんでね";
            Object fromRef = refOf.invoke(null, from.getUUID());
            Object fromAcc = refGet.invoke(fromRef);
            Object dest = account(to);
            if (fromAcc == null || dest == null) return "口座が見つからないよ";
            Object m = money(amount);
            if (!(boolean) containsValue.invoke(storageOf.invoke(fromAcc), m)) return "残高がたりないよ";
            transfer.invoke(getApi.invoke(null), from, fromRef, m, dest);
            return null;
        } catch (Throwable t) {
            return "送金できなかった";
        }
    }

    private BankCompat() {}
}
