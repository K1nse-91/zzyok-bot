package cn.zzyok.zzyokphone.bot;

import cn.zzyok.zzyokphone.api.ZzyokAddon;
import cn.zzyok.zzyokphone.ui.PhoneScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** ZZYOK BOT：原手机内全部 BOT / 云仓库功能独立成 App。 */
public final class BotAddon implements ZzyokAddon {
    @Override
    public String id() {
        return "zzyokbot";
    }

    @Override
    public String label() {
        return "我的BOT";
    }

    @Override
    public String icon() {
        return "texture:zzyokbot:icon.png";
    }

    @Override
    public Screen createScreen(Minecraft minecraft) {
        return new PhoneScreen();
    }
}