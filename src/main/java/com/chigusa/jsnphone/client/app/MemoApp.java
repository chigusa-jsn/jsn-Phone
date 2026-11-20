package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.PhoneClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** メモ：ちょっとしたメモ（サーバーに保存されるので、どのスマホでも見られる） */
public class MemoApp extends App {
    private EditBox box;
    private int editing = -1;

    @Override
    public String title() {
        return "メモ";
    }

    @Override
    public void init() {
        box = s.editBox(s.sx + 4, s.sy + s.sh - 18, s.sw - 34, 14, 300, "メモを書く");
    }

    @Override
    public void open() {
        request("memo", "list", null);
    }

    @Override
    public boolean enter() {
        if (box == null || box.getValue().isBlank()) return false;
        if (editing >= 0) request("memo", "edit", obj("index", editing, "text", box.getValue()));
        else request("memo", "add", obj("text", box.getValue()));
        box.setValue("");
        editing = -1;
        return true;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFFF4EEC8);
        JsonObject o = PhoneClient.CACHE.get("memo");
        JsonArray memos = arr(o, "memos");
        int yy = y + 22 - scroll;
        for (int i = 0; i < memos.size(); i++) {
            String t = memos.get(i).getAsString();
            List<FormattedCharSequence> lines = wrap(t, w - 22);
            int mh = lines.size() * 9 + 8;
            if (editing == i) g.fill(x + 2, yy, x + w - 2, yy + mh, 0x40F0C040);
            int ly = yy + 4;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, x + 6, ly, 0xFF303030, false);
                ly += 9;
            }
            int idx = i;
            s.hot(x, yy, w - 16, mh, () -> {
                editing = idx;
                box.setValue(t);
                s.setFocused(box);
                box.setFocused(true);
            });
            g.drawString(font, "§c✕", x + w - 12, yy + 4, 0xFFFFFF, false);
            s.hot(x + w - 14, yy + 2, 12, 12, () -> request("memo", "delete", obj("index", idx)));
            g.fill(x + 4, yy + mh, x + w - 4, yy + mh + 1, 0x30000000);
            yy += mh + 2;
        }
        if (memos.isEmpty()) g.drawCenteredString(font, "§8下の欄にメモを書いて Enter", x + w / 2, y + 60, 0xFFFFFF);
        contentH = yy + scroll - y + 24;
        g.fill(x, y + h - 22, x + w, y + h, 0xFFE0D8A8);
        s.button(g, x + w - 28, y + h - 18, 24, 14, editing >= 0 ? "直す" : "追加", s.accent(), mx, my, this::enter);
        header(g, x, y, w, title(), mx, my);
    }
}
