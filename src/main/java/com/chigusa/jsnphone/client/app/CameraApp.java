package com.chigusa.jsnphone.client.app;

import com.chigusa.jsnphone.client.camera.CameraMode;
import net.minecraft.client.gui.GuiGraphics;

/** カメラ：開くとすぐファインダーになる */
public class CameraApp extends App {
    @Override
    public String title() {
        return "カメラ";
    }

    @Override
    public void open() {
        CameraMode.startSoon();
    }

    @Override
    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.drawCenteredString(font, "◎", x + w / 2, y + h / 2 - 10, 0xFFFFFF);
        g.drawCenteredString(font, "§7カメラを起動中…", x + w / 2, y + h / 2 + 4, 0xFFFFFF);
    }
}
