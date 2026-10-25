package cn.zzyok.zzyokphone.bot;

import cn.zzyok.zzyokphone.api.Api;
import cn.zzyok.zzyokphone.api.PhoneHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/** 内置接口说明：云仓库、钱包同步、BOT 平台接口。 */
public class BotApiDocsScreen extends Screen {
    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final String[] lines = {
            "云仓库 API",
            "Base: " + Api.cloudBase(),
            "",
            "登录 / 会话",
            "POST /api/login",
            "GET  /api/me",
            "POST /api/logout",
            "验证码登录：",
            "POST /api/login/challenge",
            "GET  /api/login/challenge",
            "POST /api/login/challenge/cancel",
            "",
            "钱包 / 钱的同步",
            "GET /api/zzko",
            "返回当前 zzko 余额；",
            "启动、登录、充值、订阅后自动刷新。",
            "GET /api/me 也会携带钱包信息。",
            "订阅购买：",
            "POST /api/subscriptions/purchase",
            "",
            "云仓库",
            "GET /api/inventory?owner=<uuid>",
            "管理员：GET /api/admin/inventory",
            "存入：POST /api/action/deposit",
            "取出：POST /api/action/withdraw",
            "修装备：POST /api/action/repair",
            "",
            "BOT 平台 API",
            "Base: " + Api.platformBase(),
            "GET  /api/bots",
            "POST /api/bots/start",
            "POST /api/bots/stop",
            "POST /api/bots/restart",
            "GET  /api/bots/logs?botId=...",
            "GET  /api/smart/tasks?botId=...",
            "GET  /api/smart/permissions?botId=...",
            "POST /api/smart/tasks",
            "POST /api/smart/permissions",
            "",
            "说明",
            "1. 请求使用 Cookie 会话，登录后自动保存。",
            "2. 所有接口返回 JSON；错误看 errorMsg。",
            "3. zzko 钱包由云仓库统一扣费。",
            "4. BOT 平台与云仓库是两个 Base。",
            "5. 第三方 App 请通过 ZZYOK Phone App SDK 接入。"
    };
    private int scroll;
    private String status = "";

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private record Layout(int sx, int sy, int sw, int sh, int contentY, int contentH) {
    }

    public BotApiDocsScreen(Screen parent) {
        super(Component.literal("接口说明"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Layout layout() {
        int sw = Math.min(430, Math.max(280, Math.round(this.width * 0.55f)));
        int sh = Math.min(this.height - 16, Math.round(sw * (660f / 360f)));
        if (sh > this.height - 16) {
            sh = this.height - 16;
            sw = Math.round(sh * (360f / 660f));
        }
        int sx = (this.width - sw) / 2;
        int sy = (this.height - sh) / 2;
        return new Layout(sx + 12, sy + 12, sw - 24, sh - 24, sy + 44, sh - 68);
    }

    private static void playClick() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getSoundManager() != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.1f));
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        Layout l = layout();
        g.fill(0, 0, this.width, this.height, 0xFFF3F7FF);
        try {
            this.extractBlurredBackground(g);
        } catch (Throwable ignored) {
        }
        g.fill(0, 0, this.width, this.height, 0x55DCE6F5);
        g.fillGradient(0, 0, this.width, this.height, 0x22F3F7FF, 0x55E8E4FF);
        fillRounded(g, l.sx - 2, l.sy - 2, l.sw + 4, l.sh + 4, 24, 0x33FFFFFF);
        fillRounded(g, l.sx, l.sy, l.sw, l.sh, 22, 0xFFF3F7FE);
        header(g, l, "接口说明", mouseX, mouseY);

        int top = l.contentY + 38;
        int lineHeight = 13;
        int maxLines = Math.max(4, (l.contentH - 76) / lineHeight);
        int maxScroll = Math.max(0, lines.length - maxLines);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        for (int i = 0; i < maxLines && scroll + i < lines.length; i++) {
            String text = lines[scroll + i];
            int color = text.endsWith("API") || text.startsWith("云仓库") || text.startsWith("BOT")
                    ? 0xFF16202E : (text.isBlank() ? 0xFF8A96A8 : 0xFF54627A);
            g.text(this.font, Component.literal(fitWidth(text, l.sw - 32)), l.sx + 16, top + i * lineHeight, color, false);
        }
        int pageY = l.contentY + l.contentH - 30;
        button(g, "同步余额", l.sx + 16, pageY, 82, 24, mouseX, mouseY, true, this::syncBalance);
        g.text(this.font, Component.literal(fitWidth(status.isBlank() ? "滚动查看 · ESC 返回" : status, l.sw - 120)),
                l.sx + 106, pageY + 8, status.isBlank() ? 0xFF8A96A8 : 0xFF1F9D63, false);
    }

    private void syncBalance() {
        status = "正在同步 zzko…";
        Thread thread = new Thread(() -> {
            try {
                double value = Api.loadZzko();
                Minecraft.getInstance().execute(() -> status = "zzko 余额 " + format(value));
            } catch (Throwable throwable) {
                String message = throwable.getMessage() == null ? "同步失败" : throwable.getMessage();
                Minecraft.getInstance().execute(() -> status = message);
            }
        }, "zzyokbot-balance-sync");
        thread.setDaemon(true);
        thread.start();
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) return String.valueOf((long) Math.rint(value));
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private void header(GuiGraphicsExtractor g, Layout l, String title, int mouseX, int mouseY) {
        button(g, "<", l.sx + 14, l.contentY + 2, 32, 26, mouseX, mouseY, false, () -> Minecraft.getInstance().setScreenAndShow(parent));
        g.text(this.font, Component.literal(title), l.sx + 64, l.contentY + 9, 0xFF16202E, false);
    }

    private void button(GuiGraphicsExtractor g, String label, int x, int y, int w, int h, int mouseX, int mouseY, boolean primary, Runnable action) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int color = primary ? (hover ? 0xE62F7CFF : 0xCC2F7CFF) : (hover ? 0xB3FFFFFF : 0x80FFFFFF);
        fillRounded(g, x, y, w, h, Math.min(11, h / 2), color);
        drawCentered(g, Component.literal(label), x + w / 2, y + (h - 8) / 2, primary ? 0xFFFFFFFF : 0xFF2A3446);
        hits.add(new Hit(x, y, w, h, () -> {
            playClick();
            action.run();
        }));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Hit hit : hits) {
            if (hit.contains(event.x(), event.y())) {
                hit.action().run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, scroll - (int) Math.signum(verticalAmount) * 2);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            Minecraft.getInstance().setScreenAndShow(parent);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    private void drawCentered(GuiGraphicsExtractor g, Component text, int cx, int y, int color) {
        g.text(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    private String fitWidth(String text, int maxWidth) {
        if (text == null) return "";
        if (this.font.width(text) <= maxWidth) return text;
        int end = text.length();
        while (end > 1 && this.font.width(text.substring(0, end) + "…") > maxWidth) end--;
        return text.substring(0, end) + "…";
    }

    private void fillRounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int row = 0; row < h; row++) {
            int inset = row < r ? r - (int) Math.sqrt(r * r - (r - row) * (r - row))
                    : row >= h - r ? r - (int) Math.sqrt(r * r - (row - (h - r) + 1) * (row - (h - r) + 1)) : 0;
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }
}
