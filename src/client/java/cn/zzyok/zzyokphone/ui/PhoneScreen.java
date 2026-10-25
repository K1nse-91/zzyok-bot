package cn.zzyok.zzyokphone.ui;

import cn.zzyok.zzyokphone.api.Api;
import cn.zzyok.zzyokphone.api.AddonRegistry;
import cn.zzyok.zzyokphone.api.PhoneHost;
import cn.zzyok.zzyokphone.config.ModConfig;
import cn.zzyok.zzyokphone.util.EnchantTranslator;
import cn.zzyok.zzyokphone.util.PinyinUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** ZZYOK Phone：云仓库 + Bot 平台手机界面。 */
public class PhoneScreen extends Screen {
	private static final int PHONE_W = 360;
	private static final int PHONE_H = 660;
	private static final int RADIUS = 34;
	private static final Identifier K1NSE_HEAD = Identifier.fromNamespaceAndPath("zzyokphone", "textures/gui/k1nse_head.png");

	private static final int KEY_ESC = 256;
	private static final int KEY_ENTER = 257;
	private static final int KEY_TAB = 258;
	private static final int KEY_BACKSPACE = 259;

	private static final String MOD_VERSION = FabricLoader.getInstance()
		.getModContainer("zzyokphone")
		.map(container -> container.getMetadata().getVersion().getFriendlyString())
		.orElse("1.0.0");

	private enum Page { HOME, LOGIN, WAREHOUSE, BOT, BOTLOG, BOTSETTINGS, TASKEDIT, BOTPERMS, SUBSCRIPTION, TUTORIAL, RECHARGE, GALLERY, SETTINGS, TRIGGER, SERVERCFG, ABOUT, REPAIR }

	private enum Focus { NONE, PASSWORD, TARGET }

	private Page page = Page.HOME;
	private Page pendingPage = Page.HOME;
	private Focus focus = Focus.NONE;

	private String password = "";
	private String targetInput = "";
	private String error = "";
	private String notice = "";
	private boolean busy = false;
	private boolean warehouseLoaded = false;
	private boolean botsLoaded = false;
	private int scroll = 0;
	private double warehouseScroll = 0D;
	private double warehouseScrollTarget = 0D;
	private double warehouseRowH = 40D;
	private double warehouseMaxScroll = 0D;

	private final Map<String, Integer> selected = new LinkedHashMap<>();
	private final List<InvItem> inventory = new ArrayList<>();
	private final List<BotItem> bots = new ArrayList<>();
	private final List<String> logLines = new ArrayList<>();
	private String logBotLabel = "";
	private int tabIndex = 0;
	private final List<String> tabMatches = new ArrayList<>();
	private String tabPrefix = "";
	private String lastTabPicked = "";
	private boolean permissionsLoaded = false;
	private JsonArray permissions = new JsonArray();
	private EditBox permNameBox;
	private EditBox permMinutesBox;
	private String permRole = "whitelist";
	private int tutorialMaxScroll = 0;
	private EditBox rechargeBox;
	private EditBox prefixBox;
	private EditBox templateBox;
	private EditBox triggerBox;
	private EditBox serverCloudBox;
	private EditBox serverPlatformBox;
	private EditBox serverBotBox;
	private EditBox repairTargetBox;
	private final List<File> galleryFiles = new ArrayList<>();
	private int homePage = 0;
	private int homePageCount = 1;
	private long lastRenderNanos = 0L;
	private double renderDelta = 1D / 60D;
	private final Map<String, Identifier> appIconTextures = new HashMap<>();
	private final java.util.Set<String> appIconTextureLoading = new java.util.HashSet<>();
	private int galleryIndex = 0;
	private boolean galleryLoaded = false;
	private Identifier galleryTexture;
	private int galleryTexW = 0;
	private int galleryTexH = 0;
	private String galleryError = "";
	private long galleryTextureCounter = 0L;
	private boolean galleryZoom = false;
	private long galleryLastClickAt = 0L;
	private int galleryLastClickIndex = -1;
	private long galleryDeleteArmedAt = 0L;

	private boolean challengePending = false;
	private boolean pollingChallenge = false;
	private long lastChallengePoll = 0L;
	private String challengeCommand = "";
	private String challengeCode = "";
	private EditBox passwordBox;
	private EditBox targetBox;
	private EditBox taskNameBox;
	private EditBox taskIntervalBox;
	private EditBox taskStepsBox;
	private EditBox focusedBox;
	private final List<BoxHit> boxHits = new ArrayList<>();
	private String settingsBotId = "";
	private String settingsBotLabel = "";
	private boolean tasksLoaded = false;
	private final List<TaskItem> tasks = new ArrayList<>();
	private boolean editingTask = false;
	private String editingTaskName = "";
	private JsonArray editingTaskParameters = new JsonArray();
	private EditBox searchBox;
	private EditBox tpAcceptBox;
	private EditBox tpDenyBox;
	private String zzkoText = "—";
	private boolean balanceLoading = false;
	private static String selectedOwnerUuid = "";
	private static String repairOwnerUuid = "";
	private boolean ownerPickerVisible = false;
	private static final List<InvItem> CACHED_INVENTORY = new ArrayList<>();
	private static String cachedInventoryOwner = "";
	private static long cachedInventoryAt = 0L;
	private double zzkoBalance = 0;
	private JsonObject subscriptions = null;
	private boolean subscriptionsLoaded = false;
	private final List<String> targetSuggestions = new ArrayList<>();
	private final List<ActionRow> actionRows = new ArrayList<>();
	private final List<EditBox> actionArgsBoxes = new ArrayList<>();
	private final List<EditBox> actionWaitBoxes = new ArrayList<>();
	private final List<Hit> pickerHits = new ArrayList<>();
	private int commandPickerRow = -1;
	private static final List<String> TASK_COMMANDS = List.of("上车", "下车", "清空", "拿出", "丢弃", "副手", "切换", "放置", "右键", "攻击", "挖掘", "移动", "注视", "指令");

	private static final class ActionRow {
		String command;
		String args;
		int wait;

		ActionRow(String command, String args, int wait) {
			this.command = command;
			this.args = args == null ? "" : args;
			this.wait = Math.max(1, wait);
		}
	}

	private int clickButton = 0;
	private boolean clickShift = false;
	private boolean clickAlt = false;
	private final List<Hit> hits = new ArrayList<>();
	private float uiScale = 1f;
	private int uiOffsetX = 0;
	private int uiOffsetY = 0;

	private record Hit(int x, int y, int w, int h, Runnable action) {
		boolean contains(double mx, double my) {
			return mx >= x && mx <= x + w && my >= y && my <= y + h;
		}
	}

	private record InvItem(String key, String ownerUuid, String itemKey, String itemId, String name, int amount, String ownerLabel, List<String> contents) {}

	private record BotItem(String id, String label, String state, String desired, String lastError, String gameId) {}

	private record TaskItem(String name, int stepCount, int intervalTicks, com.google.gson.JsonArray parameters, com.google.gson.JsonArray steps) {}

	private record BoxHit(EditBox box, int x, int y, int w, int h) {}


	private record Layout(int px, int py, int w, int h, int sx, int sy, int sw, int sh, int contentY, int contentH, boolean compact) {}

	public PhoneScreen() {
		super(Component.literal("ZZYOK Phone"));
		playLevelUp();
	}

	private static void playLevelUp() {
		playUi(SoundEvents.PLAYER_LEVELUP, 1.0f);
	}

	@Override
	public void onClose() {
		playUi(SoundEvents.UI_TOAST_OUT, 1.35f);
		releaseGalleryTexture();
		PhoneHost.returnToPhone();
	}

	private static void playUi(net.minecraft.sounds.SoundEvent sound, float pitch) {
		if (!ModConfig.uiSounds) return;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null || mc.getSoundManager() == null) return;
			mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
		} catch (Throwable ignored) {
		}
	}

	private static void playClick(float pitch) {
		if (!ModConfig.uiSounds) return;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null || mc.getSoundManager() == null) return;
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch));
		} catch (Throwable ignored) {
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private Layout layout() {
		int inset = 9;
		return new Layout(0, 0, PHONE_W, PHONE_H, inset, inset, PHONE_W - inset * 2, PHONE_H - inset * 2,
				inset + 40, PHONE_H - inset * 2 - 44, false);
	}

	/* ==================== 主渲染 ==================== */

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		long nowNanos = System.nanoTime();
		renderDelta = lastRenderNanos == 0L ? 1D / 60D : Math.min(0.1D, Math.max(0.001D, (nowNanos - lastRenderNanos) / 1_000_000_000D));
		lastRenderNanos = nowNanos;
		PhoneHost.setHdMode(ModConfig.hdMode);
		hits.clear();
		boxHits.clear();
		pickerHits.clear();
		if (passwordBox != null && page != Page.LOGIN) passwordBox.setFocused(false);
		if (targetBox != null && page != Page.WAREHOUSE) targetBox.setFocused(false);
		if (searchBox != null && page != Page.WAREHOUSE) searchBox.setFocused(false);
		if (page != Page.BOTPERMS) {
			if (permNameBox != null) permNameBox.setFocused(false);
			if (permMinutesBox != null) permMinutesBox.setFocused(false);
		}
		if (page != Page.RECHARGE) {
			if (rechargeBox != null) rechargeBox.setFocused(false);
			if (prefixBox != null) prefixBox.setFocused(false);
			if (templateBox != null) templateBox.setFocused(false);
		}
		if (page != Page.SETTINGS) {
			if (tpAcceptBox != null) tpAcceptBox.setFocused(false);
			if (tpDenyBox != null) tpDenyBox.setFocused(false);
		}
		if (page != Page.TRIGGER && triggerBox != null) triggerBox.setFocused(false);
		if (page != Page.SERVERCFG) {
			if (serverCloudBox != null) serverCloudBox.setFocused(false);
			if (serverPlatformBox != null) serverPlatformBox.setFocused(false);
			if (serverBotBox != null) serverBotBox.setFocused(false);
		}
		if (page != Page.REPAIR && repairTargetBox != null) repairTargetBox.setFocused(false);
		if (page != Page.TASKEDIT) {
			if (taskNameBox != null) taskNameBox.setFocused(false);
			if (taskIntervalBox != null) taskIntervalBox.setFocused(false);
			if (taskStepsBox != null) taskStepsBox.setFocused(false);
		}
		if (ModConfig.hdMode) {
			// 高清模式：不做整屏模糊，换成干净的深色渐变底，边缘更锐利
			g.fillGradient(0, 0, this.width, this.height, 0xA6111A2C, 0xD0080B12);
			g.fill(0, 0, this.width, this.height, 0x33000000);
		} else {
			try {
				this.extractBlurredBackground(g);
			} catch (Throwable ignored) {
			}
			g.fill(0, 0, this.width, this.height, 0x66DCE6F5);
			g.fillGradient(0, 0, this.width, this.height, 0x33F3F7FF, 0x55E8E4FF);
		}

		this.uiScale = Math.min(1f, Math.min((this.width - 8f) / PHONE_W, (this.height - 8f) / PHONE_H));
		if (this.uiScale <= 0f) this.uiScale = 1f;
		int drawW = Math.max(1, Math.round(PHONE_W * this.uiScale));
		int drawH = Math.max(1, Math.round(PHONE_H * this.uiScale));
		this.uiOffsetX = (this.width - drawW) / 2;
		this.uiOffsetY = (this.height - drawH) / 2;
		Matrix3x2fStack ui = g.pose();
		ui.pushMatrix();
		ui.translate(this.uiOffsetX, this.uiOffsetY);
		ui.scale(this.uiScale, this.uiScale);
		int vx = (int) ((mouseX - this.uiOffsetX) / this.uiScale);
		int vy = (int) ((mouseY - this.uiOffsetY) / this.uiScale);
		Layout l = layout();
		// 机身
		for (int i = 8; i >= 1; i--) {
			fillRounded(g, l.px - i, l.py - i + 7, l.w + i * 2, l.h + i * 2, RADIUS + i, 0x14000000);
		}
		fillRounded(g, l.px - 1, l.py - 1, l.w + 2, l.h + 2, RADIUS + 1, 0x66FFFFFF);
		fillRounded(g, l.px, l.py, l.w, l.h, RADIUS, 0xFFF5F8FE);
		fillRounded(g, l.px + 2, l.py + 2, l.w - 4, l.h - 4, RADIUS - 2, 0xFFE9EEF8);

		int screenRadius = Math.max(16, RADIUS - 12);
		fillRoundedGradient(g, l.sx, l.sy, l.sw, l.sh, screenRadius, 0xFFEAF3FF, 0xFFF3EBFF);
		if (!ModConfig.hdMode) {
			glow(g, l.sx + l.sw / 2 - 110, l.sy + 40, 220, 220, 0x33A8C8FF);
			glow(g, l.sx + 10, l.sy + l.sh - 260, 240, 240, 0x33C9A7FF);
			glow(g, l.sx + l.sw - 170, l.sy + l.sh - 190, 190, 190, 0x2FFFC0D8);
		}

		// 状态栏时间
		String time = String.format("%02d:%02d", LocalTime.now().getHour(), LocalTime.now().getMinute());
		g.text(this.font, Component.literal(time), l.sx + 18, l.sy + 12, 0xCC1D2733, false);
		String biome = currentBiomeName();
		if (!biome.isBlank()) {
			g.text(this.font, Component.literal("· " + biome), l.sx + 18 + this.font.width(time) + 8, l.sy + 12, 0xFF54627A, false);
		}
		int statusX = l.sx + l.sw - 52;
		drawWeather(g, statusX - 34, l.sy + 11);
		drawStatusIcons(g, statusX, l.sy + 11, currentPing());

		switch (page) {
			case HOME -> drawHome(g, l, vx, vy);
			case LOGIN -> drawLogin(g, l, vx, vy);
			case WAREHOUSE -> drawWarehouse(g, l, vx, vy);
			case BOT -> drawBots(g, l, vx, vy);
			case BOTLOG -> drawBotLog(g, l, vx, vy);
			case BOTSETTINGS -> drawBotSettings(g, l, vx, vy);
			case TASKEDIT -> drawTaskEdit(g, l, vx, vy);
			case BOTPERMS -> drawBotPerms(g, l, vx, vy);
			case SUBSCRIPTION -> drawSubscription(g, l, vx, vy);
			case TUTORIAL -> drawTutorial(g, l, vx, vy);
			case RECHARGE -> drawRecharge(g, l, vx, vy);
			case SETTINGS -> drawSettings(g, l, vx, vy);
			case TRIGGER -> drawTriggerSettings(g, l, vx, vy);
			case SERVERCFG -> drawServerCfg(g, l, vx, vy);
			case GALLERY -> drawGallery(g, l, vx, vy);
			case ABOUT -> drawAbout(g, l, vx, vy);
			case REPAIR -> drawRepair(g, l, vx, vy);
		}

		fillRounded(g, l.px + l.w / 2 - 48, l.sy + l.sh - 8, 96, 4, 2, 0x551D2733);
		ui.popMatrix();
	}

	private void drawWeather(GuiGraphicsExtractor g, int x, int y) {
		int weather = 0;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc != null && mc.level != null) {
				if (mc.level.isThundering()) weather = 2;
				else if (mc.level.isRaining()) weather = 1;
			}
		} catch (Throwable ignored) {
		}
		// emoji 图标：☀ 晴 / ☂ 雨 / ⚡ 雷雨
		String icon = weather == 2 ? "⚡" : weather == 1 ? "☂" : "☀";
		int color = weather == 2 ? 0xFFE0A020 : weather == 1 ? 0xFF4C8DD6 : 0xFFE0A020;
		g.text(this.font, Component.literal(icon), x, y + 1, color, false);
		String label = weather == 2 ? "雷" : weather == 1 ? "雨" : "晴";
		g.text(this.font, Component.literal(label), x + 13, y + 1, 0xFF54627A, false);
	}

	private static String currentBiomeName() {
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null || mc.level == null || mc.player == null) return "";
			var holder = mc.level.getBiome(mc.player.blockPosition());
			String path = holder.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
			if (path.isBlank()) return "";
			return switch (path) {
				case "plains" -> "平原";
				case "sunflower_plains" -> "向日葵平原";
				case "snowy_plains" -> "积雪平原";
				case "ice_spikes" -> "冰刺之地";
				case "desert" -> "沙漠";
				case "forest" -> "森林";
				case "flower_forest" -> "繁花森林";
				case "birch_forest" -> "桦木森林";
				case "old_growth_birch_forest" -> "原始桦木森林";
				case "dark_forest" -> "黑森林";
				case "taiga" -> "针叶林";
				case "snowy_taiga" -> "积雪针叶林";
				case "old_growth_pine_taiga" -> "原始松木针叶林";
				case "old_growth_spruce_taiga" -> "原始云杉针叶林";
				case "jungle" -> "丛林";
				case "sparse_jungle" -> "稀疏丛林";
				case "bamboo_jungle" -> "竹林";
				case "savanna" -> "热带草原";
				case "savanna_plateau" -> "热带草原高地";
				case "windswept_savanna" -> "风袭热带草原";
				case "badlands" -> "恶地";
				case "eroded_badlands" -> "风蚀恶地";
				case "wooded_badlands" -> "疏林恶地";
				case "swamp" -> "沼泽";
				case "mangrove_swamp" -> "红树林沼泽";
				case "river" -> "河流";
				case "frozen_river" -> "冻河";
				case "beach" -> "沙滩";
				case "snowy_beach" -> "积雪沙滩";
				case "stony_shore" -> "石岸";
				case "ocean" -> "海洋";
				case "warm_ocean" -> "温暖海洋";
				case "lukewarm_ocean" -> "温水海洋";
				case "cold_ocean" -> "寒冷海洋";
				case "frozen_ocean" -> "冰冻海洋";
				case "deep_ocean" -> "深海";
				case "deep_lukewarm_ocean" -> "温水深海";
				case "deep_cold_ocean" -> "寒冷深海";
				case "deep_frozen_ocean" -> "冰冻深海";
				case "windswept_hills" -> "风袭丘陵";
				case "windswept_forest" -> "风袭森林";
				case "windswept_gravelly_hills" -> "风袭砂砾丘陵";
				case "meadow" -> "草甸";
				case "cherry_grove" -> "樱花树林";
				case "grove" -> "雪林";
				case "snowy_slopes" -> "积雪山坡";
				case "jagged_peaks" -> "尖峭山峰";
				case "frozen_peaks" -> "冰封山峰";
				case "stony_peaks" -> "裸岩山峰";
				case "mushroom_fields" -> "蘑菇岛";
				case "dripstone_caves" -> "溶洞";
				case "lush_caves" -> "繁茂洞穴";
				case "deep_dark" -> "深暗之域";
				case "nether_wastes" -> "下界荒地";
				case "soul_sand_valley" -> "灵魂沙峡谷";
				case "crimson_forest" -> "绯红森林";
				case "warped_forest" -> "诡异森林";
				case "basalt_deltas" -> "玄武岩三角洲";
				case "the_end" -> "末地";
				case "end_highlands" -> "末地高地";
				case "end_midlands" -> "末地内陆";
				case "small_end_islands" -> "末地小岛";
				case "end_barrens" -> "末地荒岛";
				default -> path;
			};
		} catch (Throwable ignored) {
			return "";
		}
	}

	private int currentPing() {
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null || mc.player == null || mc.getConnection() == null) return -1;
			net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			return info == null ? -1 : info.getLatency();
		} catch (Throwable ignored) {
			return -1;
		}
	}

	private void drawStatusIcons(GuiGraphicsExtractor g, int x, int y, int ping) {
		// 信号格数按延迟变化：<=60ms 4 格，<=120ms 3 格，<=250ms 2 格，其余 1 格
		int baseY = y + 10;
		int bars = ping < 0 ? 3 : ping <= 60 ? 4 : ping <= 120 ? 3 : ping <= 250 ? 2 : 1;
		int barColor = bars >= 4 ? 0xFF1F9D63 : bars == 3 ? 0xCC1D2733 : bars == 2 ? 0xFFD08A1A : 0xFFE05656;
		for (int i = 0; i < 4; i++) {
			int bh = 3 + i * 3;
			g.fill(x + i * 5, baseY - bh, x + i * 5 + 3, baseY, i < bars ? barColor : 0x331D2733);
		}
		// 电池：按游戏世界昼夜变化（白天绿色、随时间减少；夜晚红色 0 格）
		float charge = daylightCharge();
		int bx = x + 26;
		int by = baseY - 10;
		fillRounded(g, bx, by, 20, 10, 2, 0x661D2733);
		fillRounded(g, bx + 1, by + 1, 18, 8, 1, 0x22FFFFFF);
		int fillW = Math.max(0, Math.round(16f * charge));
		int batteryColor = charge <= 0.02f ? 0xFFE05656 : charge < 0.3f ? 0xFFD08A1A : 0xFF2BB98A;
		if (fillW > 0) {
			fillRounded(g, bx + 2, by + 2, fillW, 6, 1, batteryColor);
		}
		g.fill(bx + 20, by + 3, bx + 21, by + 7, 0x881D2733);
	}

	private float daylightCharge() {
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null || mc.level == null) return 0.5f;
			long time = mc.level.getOverworldClockTime() % 24000L;
			// 0=日出，12000=日落：白天电量从满逐渐减少；夜晚为红色 0
			if (time < 12000L) {
				return Math.max(0f, 1f - (time / 12000f));
			}
			return 0f;
		} catch (Throwable ignored) {
			return 0.5f;
		}
	}

	/* ==================== 桌面 ==================== */

	private static final String[][] APPS = new String[][] {
		{"云仓库", "minecraft:chest"},
		{"我的BOT", "minecraft:command_block"},
		{"设置", "minecraft:comparator"},
		{"订阅", "minecraft:nether_star"},
		{"修装备", "minecraft:experience_bottle"},
		{"充值", "minecraft:gold_ingot"},
		{"教程", "minecraft:book"}
	};

	private void drawHome(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		int avatarSize = l.compact ? 28 : 36;
		int avatarX = l.sx + l.sw / 2 - avatarSize / 2;
		int avatarY = l.contentY + 2;
		fillRounded(g, avatarX - 5, avatarY - 3, avatarSize + 10, avatarSize + 10, (avatarSize + 10) / 2, 0x33FFFFFF);
		g.blit(K1NSE_HEAD, avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize, 0f, 1f, 0f, 1f);
		int headerOffset = avatarSize + 8;
		String title = "ZZYOK BOT";
		g.text(this.font, Component.literal(title), l.sx + 20, l.contentY + headerOffset, 0xFF16202E, false);
		int buttonW = l.sw < 250 ? 40 : 46;
		button(g, Api.loggedIn ? "切换" : "登录", l.sx + l.sw - buttonW - 2, l.contentY + headerOffset - 2, buttonW, 18, mouseX, mouseY, false, this::openLogin);
		String account = Api.loggedIn ? ("当前账号 " + Api.username) : "未登录 · 点击登录";
		g.text(this.font, Component.literal(trim(account, l.compact ? 16 : 22)), l.sx + 20, l.contentY + headerOffset + (l.compact ? 18 : 20), 0xFF6E7C93, false);
		int gridTop = headerOffset + (l.compact ? 32 : 44);
		if (Api.loggedIn && !l.compact) {
			g.text(this.font, Component.literal("zzko 余额 " + zzkoText), l.sx + 20, l.contentY + headerOffset + 34, 0xFF2F7CFF, false);
			if ("—".equals(zzkoText) && !balanceLoading) loadBalance();
			gridTop = headerOffset + 52;
		}

		int cols = 3;
		int rows = 3;
		int cellW = Math.max(1, (l.sw - 40) / cols);
		int gridRemainder = (l.sw - 40) - cellW * cols;
		int startX = l.sx + 20 + gridRemainder / 2;
		int startY = l.contentY + gridTop;

		int bottomY = l.contentY + l.contentH - 12;
		int availableH = Math.max(80, bottomY - startY);
		int rowGap = l.compact ? 12 : 18;
		int iconSize = Math.min(58, cellW - 10);
		iconSize = Math.min(iconSize, availableH / rows - rowGap);
		iconSize = Math.max(24, iconSize);
		int rowH = iconSize + rowGap;

		int totalApps = APPS.length;
		int pageSize = cols * rows;
		homePageCount = Math.max(1, (totalApps + pageSize - 1) / pageSize);
		if (homePage >= homePageCount) homePage = homePageCount - 1;
		if (homePage < 0) homePage = 0;
		int firstApp = homePage * pageSize;
		int lastApp = Math.min(totalApps, firstApp + pageSize);
		for (int i = firstApp; i < lastApp; i++) {
			int localIndex = i - firstApp;
			int col = localIndex % cols;
			int row = localIndex / cols;
			int ax = startX + col * cellW + (cellW - iconSize) / 2;
			int ay = startY + row * rowH;
			boolean hover = mouseX >= ax && mouseX <= ax + iconSize && mouseY >= ay && mouseY <= ay + iconSize;
			String appLabel = APPS[i][0];
			String appIcon = APPS[i][1];
			drawAppIcon(g, appIcon, hexColor(i), ax, ay, iconSize, hover);
			drawCenteredFitted(g, Component.literal(appLabel), ax + iconSize / 2, ay + iconSize + 4, 0xFF2A3446, cellW - 2);
			int index = i;
			hits.add(new Hit(ax, ay, iconSize, iconSize, () -> onClickApp(index)));
		}
	}

	private static int hexColor(int index) {
		return switch (index) {
			case 0 -> 0xFF2F7CFF;
			case 1 -> 0xFF6E3CFF;
			case 2 -> 0xFF66758C;
			case 3 -> 0xFFE39B20;
			case 4 -> 0xFF2BB98A;
			default -> 0xFFE05656;
		};
	}

	private void onClickApp(int index) {
		playClick(1.22f);
		if (index >= APPS.length) {
			try {
				cn.zzyok.zzyokphone.api.ZzyokAddon addon = AddonRegistry.all().get(index - APPS.length);
				Minecraft minecraft = Minecraft.getInstance();
				var screen = addon.createScreen(minecraft);
				if (screen != null) {
					PhoneHost.setPhoneScreen(this);
					minecraft.setScreenAndShow(screen);
				}
			} catch (Throwable ignored) {
			}
			return;
		}
		switch (index) {
			case 0 -> openPage(Page.WAREHOUSE);
			case 1 -> openPage(Page.BOT);
			case 2 -> openPage(Page.SETTINGS);
			case 3 -> openPage(Page.SUBSCRIPTION);
			case 4 -> openPage(Page.REPAIR);
			case 5 -> openPage(Page.RECHARGE);
			case 6 -> openPage(Page.TUTORIAL);
			default -> {
				notice = "暂未接入，后续更新";
			}
		}
	}

	/* ==================== 登录 ==================== */

	private void drawLogin(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "登录云仓库", mouseX, mouseY, () -> page = Page.HOME);
		int cardY = l.contentY + 30;
		int cardH = 232;
		fillGlass(g, l.sx + 18, cardY, l.sw - 36, cardH, 20, 0x73FFFFFF);
		g.text(this.font, Component.literal("推荐：一键验证码登录"), l.sx + 34, cardY + 12, 0xFF16202E, false);
		g.text(this.font, Component.literal("点击后在游戏内自动发送登录命令"), l.sx + 34, cardY + 28, 0xFF8A96A8, false);

		if (challengePending) {
			g.text(this.font, Component.literal("验证码 " + challengeCode), l.sx + 34, cardY + 48, 0xFF2F7CFF, false);
			g.text(this.font, Component.literal("命令模式 " + ModConfig.loginCommandMode.name().toLowerCase() + " · 等待游戏内确认…"), l.sx + 34, cardY + 64, 0xFF54627A, false);
			button(g, "取消", l.sx + l.sw - 60, cardY + 46, 44, 22, mouseX, mouseY, false, () -> {
				Api.cancelChallenge();
				challengePending = false;
				notice = "已取消验证码登录";
			});
		} else {
			button(g, busy ? "请稍候…" : "一键登录（自动发送命令）", l.sx + 34, cardY + 48, l.sw - 68, 26, mouseX, mouseY, true, this::startChallenge);
		}

		// 登录命令模式：部分服务器 /msg 有后缀不识别，可切 tell
		g.text(this.font, Component.literal("登录命令"), l.sx + 34, cardY + 80, 0xFF54627A, false);
		boolean msgMode = ModConfig.loginCommandMode == ModConfig.LoginCommandMode.MSG;
		button(g, "msg", l.sx + 96, cardY + 77, 42, 20, mouseX, mouseY, msgMode, () -> {
			ModConfig.loginCommandMode = ModConfig.LoginCommandMode.MSG;
			ModConfig.save();
			notice = "登录命令已切换为 /msg";
		});
		button(g, "tell", l.sx + 142, cardY + 77, 42, 20, mouseX, mouseY, !msgMode, () -> {
			ModConfig.loginCommandMode = ModConfig.LoginCommandMode.TELL;
			ModConfig.save();
			notice = "登录命令已切换为 /tell";
		});

		g.text(this.font, Component.literal("或使用密码登录"), l.sx + 34, cardY + 108, 0xFF8A96A8, false);
		EditBox pbox = ensurePasswordBox();
		int pbx = l.sx + 34;
		int pby = cardY + 124;
		int pbw = l.sw - 68;
		int pbh = 26;
		pbox.setBordered(false);
		pbox.setWidth(pbw - 12);
		pbox.setHeight(pbh);
		pbox.setX(pbx + 6);
		pbox.setY(pby + (pbh - 8) / 2);
		fillRounded(g, pbx, pby, pbw, pbh, 8, pbox.isFocused() ? 0xCCFFFFFF : 0x66FFFFFF);
		pbox.extractWidgetRenderState(g, mouseX, mouseY, 0f);
		button(g, "登录", l.sx + 34, cardY + 158, 84, 24, mouseX, mouseY, false, this::doLogin);
		button(g, "粘贴", l.sx + l.sw - 100, cardY + 158, 66, 24, mouseX, mouseY, false, () -> {
			try {
				String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
				if (clip != null && !clip.isBlank()) {
					pbox.setValue(clip.trim());
					pbox.setFocused(true);
					notice = "已粘贴密码（" + pbox.getValue().length() + " 位）";
				}
			} catch (Throwable ignored) {
			}
		});
		g.text(this.font, Component.literal("已输入 " + pbox.getValue().length() + " 位"), l.sx + 34, cardY + 188, 0xFF8A96A8, false);

		if (!error.isBlank()) {
			g.text(this.font, Component.literal(trim(error, 40)), l.sx + 34, cardY + 206, 0xFFE23D3D, false);
		} else if (!notice.isBlank()) {
			g.text(this.font, Component.literal(trim(notice, 40)), l.sx + 34, cardY + 206, 0xFF1F9D63, false);
		}

		int hintY = cardY + cardH + 12;
		g.text(this.font, Component.literal("首次使用：游戏内发送 /msg " + ModConfig.botName + " 设置密码 你的密码"), l.sx + 20, hintY, 0xFF54627A, false);
		g.text(this.font, Component.literal("账号 = 你的游戏 ID，密码 = 游戏内设置的网页密码"), l.sx + 20, hintY + 14, 0xFF8A96A8, false);
	}

	private void startChallenge() {
		if (busy || challengePending) return;
		doAsync(() -> {
			JsonObject challenge = Api.createChallenge();
			challengeCode = getString(challenge, "code");
			challengeCommand = getString(challenge, "command");
			return null;
		}, () -> {
			challengePending = true;
			pollingChallenge = false;
			lastChallengePoll = 0L;
			error = "";
			sendChallengeCommand();
		});
	}

	private void sendChallengeCommand() {
		try {
			if (challengeCommand.isBlank()) {
				error = "服务器没有返回登录命令";
				return;
			}
			Minecraft mc = Minecraft.getInstance();
			if (mc.getConnection() == null) {
				error = "当前不在服务器内";
				return;
			}
			String command = buildLoginCommand();
			mc.getConnection().sendCommand(command.startsWith("/") ? command.substring(1) : command);
			notice = "已发送：" + command + "（等待确认）";
		} catch (Throwable throwable) {
			error = "发送登录命令失败：" + throwable.getMessage();
		}
	}

	private String buildLoginCommand() {
		String command = challengeCommand == null ? "" : challengeCommand.trim();
		if (ModConfig.loginCommandMode == ModConfig.LoginCommandMode.TELL) {
			if (command.startsWith("/msg ")) command = "/tell " + command.substring(5);
			else if (command.startsWith("msg ")) command = "tell " + command.substring(4);
			else if (command.contains("/msg ")) command = command.replaceFirst("/msg ", "/tell ");
			else if (command.contains(" msg ")) command = command.replaceFirst(" msg ", " tell ");
		}
		return command;
	}

	private void pollChallengeOnce() {
		if (!challengePending || pollingChallenge) return;
		long now = System.currentTimeMillis();
		if (now - lastChallengePoll < 1500L) return;
		lastChallengePoll = now;
		pollingChallenge = true;
		Thread thread = new Thread(() -> {
			String status = "";
			String failure = null;
			boolean approved = false;
			try {
				JsonObject result = Api.challengeStatus();
				status = getString(result, "status");
				if ("approved".equals(status)) {
					Api.loadMe();
					approved = true;
				}
			} catch (Throwable throwable) {
				failure = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
			}
			final String finalStatus = status;
			final String finalFailure = failure;
			final boolean finalApproved = approved;
			Minecraft.getInstance().execute(() -> {
				pollingChallenge = false;
				if (finalApproved) {
					challengePending = false;
					password = "";
					focus = Focus.NONE;
					error = "";
					notice = "登录成功：" + Api.username;
					playLevelUp();
					loadBalance();
					Page target = pendingPage == Page.HOME ? Page.HOME : pendingPage;
					page = target;
					pendingPage = Page.HOME;
					if (target == Page.WAREHOUSE) loadWarehouse();
					if (target == Page.BOT) loadBots();
					if (target == Page.SUBSCRIPTION) loadSubscriptions();
				} else if ("expired".equals(finalStatus) || "cancelled".equals(finalStatus) || "none".equals(finalStatus)) {
					challengePending = false;
					error = "验证码已失效，请重新点击一键登录";
				} else if (finalFailure != null && !finalFailure.isBlank()) {
					error = finalFailure;
				}
			});
		}, "zzyokphone-challenge");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void tick() {
		super.tick();
		pollChallengeOnce();
	}

	private void loadBalance() {
		if (!Api.loggedIn || balanceLoading) return;
		balanceLoading = true;
		Thread thread = new Thread(() -> {
			double balance = -1;
			try {
				balance = Api.loadZzko();
			} catch (Throwable first) {
				try {
					JsonObject subs = Api.loadSubscriptions();
					if (subs.has("wallet") && subs.get("wallet").isJsonObject()) {
						JsonObject wallet = subs.getAsJsonObject("wallet");
						if (wallet.has("zzko") && !wallet.get("zzko").isJsonNull()) balance = wallet.get("zzko").getAsDouble();
					}
				} catch (Throwable ignored) {
				}
			}
			double finalBalance = balance;
			Minecraft.getInstance().execute(() -> {
				balanceLoading = false;
				if (finalBalance >= 0) {
					zzkoBalance = finalBalance;
					zzkoText = formatZzko(finalBalance);
				}
			});
		}, "zzyokphone-balance");
		thread.setDaemon(true);
		thread.start();
	}

	private static String formatZzko(double value) {
		if (value == Math.floor(value)) return String.valueOf((long) value);
		return String.format("%.2f", value);
	}

	private void doLogin() {
		if (busy) return;
		String pass = passwordBox != null ? passwordBox.getValue().trim() : "";
		if (pass.isBlank()) {
			error = "请输入密码";
			return;
		}
		doAsync(() -> {
			Api.login(pass);
			return null;
		}, () -> {
			if (passwordBox != null) passwordBox.setValue("");
			unfocusWidgets();
			error = Api.lastError;
			notice = "登录成功：" + Api.username;
			playLevelUp();
			loadBalance();
			Page target = pendingPage == Page.HOME ? Page.HOME : pendingPage;
			page = target;
			if (target == Page.WAREHOUSE) loadWarehouse();
			if (target == Page.BOT) loadBots();
			if (target == Page.SUBSCRIPTION) loadSubscriptions();
			pendingPage = Page.HOME;
		});
	}

	private void openLogin() {
		page = Page.LOGIN;
		password = "";
		error = "";
		if (passwordBox != null) {
			passwordBox.setValue("");
			passwordBox.setFocused(true);
		}
	}

	/* ==================== 云仓库 ==================== */

		private void ensureOwnerSelection() {
		if (!Api.loggedIn) return;
		boolean exists = false;
		for (Api.Owner owner : Api.owners) {
			if (owner.uuid().equals(selectedOwnerUuid)) {
				exists = true;
				break;
			}
		}
		if (!exists) {
			selectedOwnerUuid = Api.defaultOwnerUuid;
			if (selectedOwnerUuid == null || selectedOwnerUuid.isBlank()) {
				selectedOwnerUuid = Api.owners.isEmpty() ? "" : Api.owners.get(0).uuid();
			}
		}
	}

	private void loadWarehouse() {
		if (!Api.loggedIn) {
			pendingPage = Page.WAREHOUSE;
			openLogin();
			return;
		}
		if (warehouseLoaded) return;
		ensureOwnerSelection();
		String ownerUuid = selectedOwnerUuid;
		if (ownerUuid.equals(cachedInventoryOwner) && !CACHED_INVENTORY.isEmpty()) {
			inventory.clear();
			inventory.addAll(CACHED_INVENTORY);
			warehouseLoaded = true;
			if (System.currentTimeMillis() - cachedInventoryAt > 2 * 60 * 1000L) silentRefreshWarehouse(ownerUuid);
			return;
		}
		List<InvItem> disk = loadInventoryDiskCache(ownerUuid);
		if (!disk.isEmpty()) {
			inventory.clear();
			inventory.addAll(disk);
			CACHED_INVENTORY.clear();
			CACHED_INVENTORY.addAll(disk);
			cachedInventoryOwner = ownerUuid;
			cachedInventoryAt = System.currentTimeMillis();
			warehouseLoaded = true;
			silentRefreshWarehouse(ownerUuid);
			return;
		}
		doAsync(() -> {
			List<InvItem> list = fetchInventory(ownerUuid);
			saveInventoryDiskCache(ownerUuid, list);
			inventory.clear();
			inventory.addAll(list);
			CACHED_INVENTORY.clear();
			CACHED_INVENTORY.addAll(list);
			cachedInventoryOwner = ownerUuid;
			cachedInventoryAt = System.currentTimeMillis();
			selected.clear();
			warehouseLoaded = true;
			return null;
		}, null);
	}

	private List<InvItem> fetchInventory(String ownerUuid) throws Exception {
		JsonArray rows = Api.loadInventory(ownerUuid);
		List<InvItem> list = new ArrayList<>();
		for (JsonElement element : rows) {
			if (!element.isJsonObject()) continue;
			JsonObject row = element.getAsJsonObject();
			String itemKey = getString(row, "itemKey");
			String itemId = getString(row, "itemId");
			if (itemId.isBlank()) itemId = getString(row, "item_id");
			if (itemId.isBlank() && itemKey.contains(":")) itemId = itemKey;
			String name = getString(row, "displayName");
			if (name.isBlank()) name = getString(row, "baseDisplayName");
			if (name.isBlank()) name = getString(row, "itemName");
			if (name.isBlank()) name = itemKey;
			name = EnchantTranslator.translate(name);
			int amount = getInt(row, "amount");
			if (amount == 0) amount = getInt(row, "count");
			if (amount == 0) amount = getInt(row, "totalAmount");
			String owner = getString(row, "ownerUuid");
			String ownerLabel = getString(row, "ownerLabel");
			String key = owner + "::" + (itemKey.isBlank() ? name : itemKey);
			if (amount > 0) list.add(new InvItem(key, owner, itemKey, itemId, name, amount, ownerLabel, parseShulkerContents(row)));
		}
		return list;
	}

	private void silentRefreshWarehouse(String ownerUuid) {
		Thread thread = new Thread(() -> {
			try {
				List<InvItem> list = fetchInventory(ownerUuid);
				saveInventoryDiskCache(ownerUuid, list);
				Minecraft.getInstance().execute(() -> {
					if (!ownerUuid.equals(selectedOwnerUuid)) return;
					inventory.clear();
					inventory.addAll(list);
					CACHED_INVENTORY.clear();
					CACHED_INVENTORY.addAll(list);
					cachedInventoryOwner = ownerUuid;
					cachedInventoryAt = System.currentTimeMillis();
					warehouseLoaded = true;
				});
			} catch (Throwable ignored) {
			}
		}, "zzyokphone-warehouse-refresh");
		thread.setDaemon(true);
		thread.start();
	}

	private static Path inventoryCachePath() {
		return FabricLoader.getInstance().getConfigDir().resolve("zzyokphone-inventory-cache.json");
	}

	public static void preloadInventoryCache() {
		try {
			Path path = inventoryCachePath();
			if (!Files.exists(path)) return;
			JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			String savedBase = getString(root, "cloudBase");
			if (!savedBase.isBlank() && !savedBase.equals(Api.cloudBase())) return;
			String owner = getString(root, "ownerUuid");
			List<InvItem> list = parseInventoryCache(root);
			if (list.isEmpty() || owner.isBlank()) return;
			CACHED_INVENTORY.clear();
			CACHED_INVENTORY.addAll(list);
			cachedInventoryOwner = owner;
			cachedInventoryAt = root.has("savedAt") ? root.get("savedAt").getAsLong() : 0L;
		} catch (Throwable ignored) {
		}
	}

	private static void saveInventoryDiskCache(String ownerUuid, List<InvItem> list) {
		try {
			JsonObject root = new JsonObject();
			root.addProperty("cloudBase", Api.cloudBase());
			root.addProperty("ownerUuid", ownerUuid);
			root.addProperty("savedAt", System.currentTimeMillis());
			JsonArray items = new JsonArray();
			for (InvItem item : list) {
				JsonObject row = new JsonObject();
				row.addProperty("key", item.key);
				row.addProperty("ownerUuid", item.ownerUuid);
				row.addProperty("itemKey", item.itemKey);
				row.addProperty("itemId", item.itemId);
				row.addProperty("name", item.name);
				row.addProperty("amount", item.amount);
				row.addProperty("ownerLabel", item.ownerLabel);
				JsonArray contents = new JsonArray();
				if (item.contents != null) for (String line : item.contents) contents.add(line);
				row.add("contents", contents);
				items.add(row);
			}
			root.add("items", items);
			Files.createDirectories(inventoryCachePath().getParent());
			Files.writeString(inventoryCachePath(), root.toString());
		} catch (Throwable ignored) {
		}
	}

	private static List<InvItem> loadInventoryDiskCache(String ownerUuid) {
		try {
			Path path = inventoryCachePath();
			if (!Files.exists(path)) return new ArrayList<>();
			JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			String savedBase = getString(root, "cloudBase");
			if (!savedBase.isBlank() && !savedBase.equals(Api.cloudBase())) return new ArrayList<>();
			if (!ownerUuid.equals(getString(root, "ownerUuid"))) return new ArrayList<>();
			return parseInventoryCache(root);
		} catch (Throwable ignored) {
			return new ArrayList<>();
		}
	}

	private static List<InvItem> parseInventoryCache(JsonObject root) {
		List<InvItem> list = new ArrayList<>();
		try {
			if (!root.has("items") || !root.get("items").isJsonArray()) return list;
			for (JsonElement element : root.getAsJsonArray("items")) {
				if (!element.isJsonObject()) continue;
				JsonObject row = element.getAsJsonObject();
				List<String> contents = new ArrayList<>();
				if (row.has("contents") && row.get("contents").isJsonArray()) {
					for (JsonElement line : row.getAsJsonArray("contents")) contents.add(line.getAsString());
				}
				list.add(new InvItem(
					getString(row, "key"),
					getString(row, "ownerUuid"),
					getString(row, "itemKey"),
					getString(row, "itemId"),
					getString(row, "name"),
					getInt(row, "amount"),
					getString(row, "ownerLabel"),
					contents));
			}
		} catch (Throwable ignored) {
		}
		return list;
	}

	private void drawWarehouse(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "云仓库", mouseX, mouseY, () -> {
			selected.clear();
			page = Page.HOME;
		});
		button(g, "刷新", l.sx + l.sw - 62, l.contentY + 2, 42, 20, mouseX, mouseY, false, () -> {
			warehouseLoaded = false;
			cachedInventoryAt = 0L;
			loadWarehouse();
		});

		// 仓库归属选择
		ensureOwnerSelection();
		String ownerLabel = Api.ownerLabel(selectedOwnerUuid);
		if (ownerLabel == null || ownerLabel.isBlank()) ownerLabel = "选择仓库";
		button(g, ownerLabel, l.sx + 20, l.contentY + 30, 118, 22, mouseX, mouseY, false, () -> ownerPickerVisible = !ownerPickerVisible);
		// 搜索框
		EditBox sbox = ensureSearchBox();
		renderEditBox(g, sbox, l.sx + 146, l.contentY + 30, l.sw - 166, 22, mouseX, mouseY);
		if (sbox.getValue().isBlank()) {
			// hint 由 EditBox 处理
		}
		g.text(this.font, Component.literal("已选 " + selected.size() + " 项 / " + selectedTotal() + " 件"), l.sx + 20, l.contentY + 58, 0xFF54627A, false);

		// 过滤后的物品
		String filter = sbox.getValue().trim().toLowerCase();
		List<InvItem> filtered = new ArrayList<>();
		for (InvItem item : inventory) {
			if (filter.isEmpty() || matchesSearch(item, filter)) filtered.add(item);
		}

		// 物品网格
		int gridY = l.contentY + 78;
		int bottomBlock = 100;
		int gridH = Math.max(60, l.contentH - 78 - bottomBlock);
		int cols = 5;
		int gap = 7;
		int tile = (l.sw - 40 - gap * (cols - 1)) / cols;
		int rowH = tile + gap;
		int maxRows = Math.max(1, gridH / rowH);
		int maxScroll = Math.max(0, (filtered.size() + cols - 1) / cols - maxRows);
		warehouseRowH = rowH;
		warehouseMaxScroll = maxScroll * (double) rowH;
		warehouseScrollTarget = Math.max(0D, Math.min(warehouseMaxScroll, warehouseScrollTarget));
		warehouseScroll += (warehouseScrollTarget - warehouseScroll) * frameSmoothing();
		if (Math.abs(warehouseScrollTarget - warehouseScroll) < 0.6D) warehouseScroll = warehouseScrollTarget;
		int scrollOffset = (int) Math.round(warehouseScroll);
		int startX = l.sx + 20;
		int startY = gridY - scrollOffset;
		InvItem hoverShulker = null;
		InvItem hoverItem = null;

		if (busy && !warehouseLoaded) {
			drawCentered(g, Component.literal("加载库存中…"), l.sx + l.sw / 2, gridY + 60, 0xFF54627A);
		} else if (warehouseLoaded && filtered.isEmpty()) {
			drawCentered(g, Component.literal(filter.isEmpty() ? "仓库里还没有物品" : "没有匹配的物品"), l.sx + l.sw / 2, gridY + 60, 0xFF8A96A8);
		}

		for (int i = 0; i < filtered.size(); i++) {
			InvItem item = filtered.get(i);
			int col = i % cols;
			int row = i / cols;
			int x = startX + col * (tile + gap);
			int y = startY + row * rowH;
			if (y + tile < gridY || y > gridY + gridH) continue;
			if (y < gridY) continue;
			boolean hover = mouseX >= x && mouseX <= x + tile && mouseY >= y && mouseY <= y + tile;
			drawInvTile(g, item, x, y, tile, hover);
			hits.add(new Hit(x, y, tile, tile, () -> onInvClick(item)));
			if (hover) {
				hoverItem = item;
				if (item.itemId != null && item.itemId.endsWith("shulker_box")) hoverShulker = item;
			}
		}

		// 底部：提示 / 送货给（左下角） / 按钮
		if (hoverShulker != null) drawShulkerTooltip(g, hoverShulker, mouseX, mouseY);
		else if (hoverItem != null) drawItemNameTooltip(g, hoverItem, mouseX, mouseY);

		int hintY = l.contentY + l.contentH - 94;
		g.text(this.font, Component.literal("左键+1 · Shift+左键+64 · Alt+左键+1728 · 右键-1728"), l.sx + 20, hintY, 0xFF8A96A8, false);

		int targetY = l.contentY + l.contentH - 70;
		g.text(this.font, Component.literal("送货给"), l.sx + 20, targetY + 7, 0xFF54627A, false);
		EditBox tbox = ensureTargetBox();
		int tbx = l.sx + 66;
		int tbw = l.sw - 86;
		tbox.setX(tbx + 6);
		tbox.setY(targetY + 7);
		tbox.setWidth(tbw - 12);
		tbox.setHeight(22);
		fillRounded(g, tbx, targetY, tbw, 22, 8, tbox.isFocused() ? 0xCCFFFFFF : 0x66FFFFFF);
		tbox.extractWidgetRenderState(g, mouseX, mouseY, 0f);
		boxHits.add(new BoxHit(tbox, tbx, targetY, tbw, 22));
		if (tbox.isFocused()) {
			updateTargetSuggestions(tbox.getValue());
			if (!targetSuggestions.isEmpty()) {
				int sx = tbx;
				int sy = targetY - 24;
				int used = 0;
				for (int i = 0; i < targetSuggestions.size() && i < 4; i++) {
					String name = targetSuggestions.get(i);
					int w = this.font.width(name) + 14;
					if (sx + used + w > l.sx + l.sw - 20) break;
					int bx = sx + used;
					fillRounded(g, bx, sy, w, 18, 7, 0xE6FFFFFF);
					g.text(this.font, Component.literal(name), bx + 7, sy + 5, 0xFF2F7CFF, false);
					final String picked = name;
					hits.add(new Hit(bx, sy, w, 18, () -> tbox.setValue(picked)));
					used += w + 4;
				}
			}
		}

		int btnY = l.contentY + l.contentH - 34;
		button(g, "存入", l.sx + 20, btnY, 72, 26, mouseX, mouseY, false, this::depositToWarehouse);
		button(g, "取出选中", l.sx + l.sw - 120, btnY, 100, 26, mouseX, mouseY, true, this::withdrawSelected);
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 36)), l.sx + 20, btnY - 18, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 36)), l.sx + 20, btnY - 18, 0xFF1F9D63, false);
		if (ownerPickerVisible) drawOwnerPicker(g, l, mouseX, mouseY);
	}

	private void drawOwnerPicker(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		g.fill(l.sx, l.sy + 30, l.sx + l.sw, l.sy + l.sh, 0x66000000);
		int panelW = l.sw - 60;
		int rowH = 28;
		int shown = 0;
		List<Api.Owner> owners = Api.owners;
		int panelH = 40 + Math.min(owners.size(), 6) * rowH + 40;
		int px = l.sx + (l.sw - panelW) / 2;
		int py = l.sy + 70;
		fillGlass(g, px, py, panelW, panelH, 16, 0xF2FFFFFF);
		g.text(this.font, Component.literal("选择仓库"), px + 14, py + 10, 0xFF16202E, false);
		for (Api.Owner owner : owners) {
			if (shown >= 6) break;
			int ry = py + 32 + shown * rowH;
			boolean active = owner.uuid().equals(selectedOwnerUuid);
			fillRounded(g, px + 10, ry, panelW - 20, rowH - 4, 8, active ? 0xB32F7CFF : 0x99FFFFFF);
			g.text(this.font, Component.literal(trim(owner.label(), 24)), px + 20, ry + 7, active ? 0xFFFFFFFF : 0xFF2A3446, false);
			final String uuid = owner.uuid();
			pickerHits.add(new Hit(px + 10, ry, panelW - 20, rowH - 4, () -> {
				selectedOwnerUuid = uuid;
				ownerPickerVisible = false;
				warehouseLoaded = false;
				selected.clear();
				loadWarehouse();
			}));
			shown++;
		}
		int btnY = py + panelH - 26;
		pickerHits.add(new Hit(px + 10, btnY - 4, 90, 22, () -> {
			ownerPickerVisible = false;
			doAsync(() -> {
				Api.setDefaultOwner(selectedOwnerUuid);
				Api.defaultOwnerUuid = selectedOwnerUuid;
				return null;
			}, () -> notice = "已设为默认仓库");
		}));
		fillRounded(g, px + 10, btnY - 4, 90, 22, 8, 0xCC2F7CFF);
		drawCentered(g, Component.literal("设为默认"), px + 55, btnY + 2, 0xFFFFFFFF);
	}

	private boolean matchesSearch(InvItem item, String filter) {
		if (filter == null || filter.isBlank()) return true;
		String q = filter.toLowerCase().trim();
		if (item.name.toLowerCase().contains(q) || item.itemId.toLowerCase().contains(q)) return true;
		return PinyinUtil.matches(item.name, q);
	}

	private static List<String> parseShulkerContents(JsonObject row) {
		List<String> list = new ArrayList<>();
		try {
			JsonArray contents = row.has("shulkerContents") && row.get("shulkerContents").isJsonArray()
				? row.getAsJsonArray("shulkerContents")
				: row.has("contents") && row.get("contents").isJsonArray() ? row.getAsJsonArray("contents") : new JsonArray();
			for (JsonElement element : contents) {
				if (!element.isJsonObject()) continue;
				JsonObject item = element.getAsJsonObject();
				String name = getString(item, "displayName");
				if (name.isBlank()) name = getString(item, "baseDisplayName");
				if (name.isBlank()) name = getString(item, "itemId");
				int amount = getInt(item, "amount");
				if (amount <= 0) amount = getInt(item, "count");
				if (!name.isBlank()) list.add(EnchantTranslator.translate(name) + " ×" + Math.max(amount, 0));
			}
		} catch (Throwable ignored) {
		}
		return list;
	}

	private void drawItemNameTooltip(GuiGraphicsExtractor g, InvItem item, int mouseX, int mouseY) {
		String label = trim(item.name, 24) + (item.amount > 1 ? " ×" + item.amount : "");
		int width = this.font.width(label) + 16;
		int height = 18;
		int x = Math.min(Math.max(4, mouseX + 12), this.width - width - 4);
		int y = Math.min(Math.max(4, mouseY + 12), this.height - height - 4);
		fillRounded(g, x, y, width, height, 8, 0xF0101626);
		g.text(this.font, Component.literal(label), x + 8, y + 5, 0xFFFFFFFF, false);
	}

	private void drawShulkerTooltip(GuiGraphicsExtractor g, InvItem item, int mouseX, int mouseY) {
		List<String> lines = item.contents;
		if (lines == null || lines.isEmpty()) lines = List.of("（空 / 未索引内容）");
		int maxLines = 8;
		int titleWidth = this.font.width("潜影盒内容 · " + item.name);
		int width = Math.max(140, titleWidth + 20);
		int shown = Math.min(lines.size(), maxLines);
		for (int i = 0; i < shown; i++) width = Math.max(width, this.font.width("· " + lines.get(i)) + 20);
		int height = 24 + shown * 12 + (lines.size() > shown ? 12 : 0);
		int x = Math.min(Math.max(4, mouseX + 12), this.width - width - 4);
		int y = Math.min(Math.max(4, mouseY + 12), this.height - height - 4);
		fillRounded(g, x, y, width, height, 10, 0xF0101626);
		fillRounded(g, x, y, width, 18, 10, 0xFF1D2733);
		g.text(this.font, Component.literal("潜影盒内容 · " + trim(item.name, 16)), x + 8, y + 5, 0xFFFFFFFF, false);
		for (int i = 0; i < shown; i++) {
			g.text(this.font, Component.literal("· " + lines.get(i)), x + 10, y + 22 + i * 12, 0xFFB9C4DA, false);
		}
		if (lines.size() > shown) {
			g.text(this.font, Component.literal("… 还有 " + (lines.size() - shown) + " 项"), x + 10, y + 22 + shown * 12, 0xFF8A96A8, false);
		}
	}

	private EditBox ensureSearchBox() {
		if (searchBox == null) {
			searchBox = new EditBox(this.font, 0, 0, 120, 22, Component.literal("搜索"));
			searchBox.setTextShadow(false);
			searchBox.setMaxLength(32);
			searchBox.setBordered(false);
			searchBox.setTextColor(0xFF1D2733);
			searchBox.setHint(Component.literal("搜索物品…"));
		}
		return searchBox;
	}

	private void updateTargetSuggestions(String prefix) {
		targetSuggestions.clear();
		try {
			// 和聊天栏一致：没输入任何字符时不出补全列表
			if (prefix == null || prefix.isBlank()) return;
			Minecraft mc = Minecraft.getInstance();
			if (mc.getConnection() == null) return;
			String lower = prefix.toLowerCase();
			for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
				if (info == null || info.getProfile() == null) continue;
				String name = info.getProfile().name();
				if (name == null || name.isBlank() || name.equals(Api.username)) continue;
				if (name.toLowerCase().startsWith(lower)) targetSuggestions.add(name);
				if (targetSuggestions.size() >= 8) break;
			}
			targetSuggestions.sort(String.CASE_INSENSITIVE_ORDER);
		} catch (Throwable ignored) {
		}
	}

	private void drawInvTile(GuiGraphicsExtractor g, InvItem item, int x, int y, int size, boolean hover) {
		fillRounded(g, x, y, size, size, 12, hover ? 0xB3FFFFFF : 0x80FFFFFF);
		drawScaledItem(g, stackFor(item.itemId), x + size / 2f - 16f, y + size / 2f - 16f, 2f);
		String amount = item.amount >= 100000 ? (item.amount / 1000) + "k" : String.valueOf(item.amount);
		int amountW = this.font.width(amount) + 8;
		int amountH = 13;
		int amountX = x + size - amountW - 3;
		int amountY = y + size - amountH - 3;
		fillRounded(g, amountX, amountY, amountW, amountH, 6, 0xCC1D2733);
		g.text(this.font, Component.literal(amount), amountX + 4, amountY + 3, 0xFFFFFFFF, false);
		int sel = selected.getOrDefault(item.key, 0);
		if (sel > 0) {
			String badge = sel > 9999 ? (sel / 1000) + "k" : String.valueOf(sel);
			int bw = this.font.width(badge) + 8;
			fillRounded(g, x + 3, y + 3, bw, 13, 6, 0xE62F7CFF);
			g.text(this.font, Component.literal(badge), x + 7, y + 6, 0xFFFFFFFF, false);
		}
	}

	private void onInvClick(InvItem item) {
		playClick(1.5f);
		int current = selected.getOrDefault(item.key, 0);
		int delta;
		if (clickButton == 1) {
			// 右键：-1728；Shift+右键：-64
			delta = clickShift ? -64 : -1728;
		} else if (clickButton == 0) {
			// 左键：+1；Shift+左键：+64；Alt+左键：+1728
			if (clickShift) delta = 64;
			else if (clickAlt) delta = 1728;
			else delta = 1;
		} else {
			return;
		}
		int next = Math.max(0, Math.min(item.amount, current + delta));
		if (next == 0) selected.remove(item.key);
		else selected.put(item.key, next);
	}

	private int selectedTotal() {
		int total = 0;
		for (int value : selected.values()) total += value;
		return total;
	}

	private void depositToWarehouse() {
		if (busy || !Api.loggedIn) return;
		ensureOwnerSelection();
		String ownerUuid = selectedOwnerUuid;
		doAsync(() -> {
			Api.deposit(ownerUuid);
			return null;
		}, () -> {
			playLevelUp();
			selected.clear();
			cachedInventoryAt = 0L;
			notice = "已提交存入请求：Bot 会走到你身边收取物品";
		});
	}

	private void withdrawSelected() {
		if (!Api.loggedIn) {
			pendingPage = Page.WAREHOUSE;
			openLogin();
			return;
		}
		if (selected.isEmpty()) {
			error = "请先选择要取出的物品";
			return;
		}
		String target = targetBox != null && !targetBox.getValue().isBlank() ? targetBox.getValue().trim() : Api.username;
		playLevelUp();
		doAsync(() -> {
			Map<String, JsonArray> groups = new LinkedHashMap<>();
			for (InvItem item : inventory) {
				Integer amount = selected.get(item.key);
				if (amount == null || amount <= 0) continue;
				JsonArray group = groups.computeIfAbsent(item.ownerUuid.isBlank() ? Api.defaultOwnerUuid : item.ownerUuid, k -> new JsonArray());
				JsonObject entry = new JsonObject();
				if (!item.itemKey.isBlank()) entry.addProperty("itemKey", item.itemKey);
				entry.addProperty("displayName", item.name);
				entry.addProperty("amount", amount);
				group.add(entry);
			}
			StringBuilder messages = new StringBuilder();
			for (Map.Entry<String, JsonArray> entry : groups.entrySet()) {
				JsonObject result = Api.withdraw(entry.getKey(), entry.getValue(), target);
				if (result.has("message")) {
					if (messages.length() > 0) messages.append("；");
					messages.append(result.get("message").getAsString());
				}
			}
			notice = messages.length() > 0 ? messages.toString() : "取物品任务已提交，请留意游戏内提示";
			selected.clear();
			return null;
		}, null);
	}

	/* ==================== BOT ==================== */

	private void loadBots() {
		if (!Api.loggedIn) {
			pendingPage = Page.BOT;
			openLogin();
			return;
		}
		if (botsLoaded) return;
		doAsync(() -> {
			JsonArray rows = Api.loadBots();
			List<BotItem> list = new ArrayList<>();
			for (JsonElement element : rows) {
				if (!element.isJsonObject()) continue;
				JsonObject row = element.getAsJsonObject();
				String id = getString(row, "id");
				String label = getString(row, "label");
				if (label.isBlank()) label = getString(row, "botName");
				if (label.isBlank()) label = getString(row, "username");
				String state = getString(row, "runtimeState");
				String desired = getString(row, "desiredState");
				String lastError = getString(row, "lastError");
				String gameId = getString(row, "minecraftName");
				if (gameId.isBlank()) gameId = getString(row, "accountMinecraftName");
				list.add(new BotItem(id, label, state, desired, lastError, gameId));
			}
			bots.clear();
			bots.addAll(list);
			botsLoaded = true;
			return null;
		}, null);
	}

	private void drawBots(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "我的 BOT", mouseX, mouseY, () -> page = Page.HOME);
		button(g, "刷新", l.sx + l.sw - 62, l.contentY + 2, 42, 20, mouseX, mouseY, false, () -> {
			botsLoaded = false;
			loadBots();
		});
		int y = l.contentY + 40;
		if (busy && !botsLoaded) {
			drawCentered(g, Component.literal("加载 Bot 列表…"), l.sx + l.sw / 2, y + 60, 0xFF54627A);
			return;
		}
		if (!error.isBlank()) {
			g.text(this.font, Component.literal(error), l.sx + 20, y + 20, 0xFFE23D3D, false);
		}
		for (int i = 0; i < bots.size(); i++) {
			BotItem bot = bots.get(i);
			int cardY = y + i * 88;
			fillGlass(g, l.sx + 18, cardY, l.sw - 36, 80, 16, 0x80FFFFFF);
			g.text(this.font, Component.literal(bot.label), l.sx + 32, cardY + 8, 0xFF16202E, false);
			String state = "online".equalsIgnoreCase(bot.state) ? "在线" : "stopped".equalsIgnoreCase(bot.state) ? "已停止" : bot.state;
			int color = "online".equalsIgnoreCase(bot.state) ? 0xFF1F9D63 : 0xFF8A96A8;
			g.text(this.font, Component.literal(state), l.sx + 32 + this.font.width(bot.label) + 10, cardY + 8, color, false);
			String gameId = bot.gameId == null || bot.gameId.isBlank() ? "—" : bot.gameId;
			g.text(this.font, Component.literal("游戏ID：" + trim(gameId, 20)), l.sx + 32, cardY + 24, 0xFF6E7C93, false);
			int cardLeft = l.sx + 30;
			int cardInner = l.sw - 60;
			int gap = 4;
			int btnW = (cardInner - gap * 4) / 5;
			int btnY = cardY + 46;
			button(g, "启动", cardLeft, btnY, btnW, 21, mouseX, mouseY, false, () -> controlBot("start", bot));
			button(g, "停止", cardLeft + (btnW + gap), btnY, btnW, 21, mouseX, mouseY, false, () -> controlBot("stop", bot));
			button(g, "重启", cardLeft + (btnW + gap) * 2, btnY, btnW, 21, mouseX, mouseY, true, () -> controlBot("restart", bot));
			button(g, "日志", cardLeft + (btnW + gap) * 3, btnY, btnW, 21, mouseX, mouseY, false, () -> loadBotLogs(bot));
			button(g, "设置", cardLeft + (btnW + gap) * 4, btnY, btnW, 21, mouseX, mouseY, false, () -> openBotSettings(bot));
			if (!bot.lastError.isBlank()) {
				// 错误信息放在按钮行下方的小字，避免与游戏ID重叠
				g.text(this.font, Component.literal(trim(bot.lastError, 30)), l.sx + 32, cardY + 70, 0xFFC25050, false);
			}
		}
		if (!notice.isBlank()) g.text(this.font, Component.literal(notice), l.sx + 20, l.contentY + l.contentH - 16, 0xFF1F9D63, false);
	}

	private void controlBot(String action, BotItem bot) {
		doAsync(() -> {
			Api.botAction(action, bot.id);
			botsLoaded = false;
			return null;
		}, () -> notice = bot.label + " " + switch (action) {
			case "start" -> "启动请求已发送";
			case "stop" -> "停止请求已发送";
			default -> "重启请求已发送";
		});
	}

	private void loadBotLogs(BotItem bot) {
		doAsync(() -> {
			JsonArray rows = Api.loadBotLogs(bot.id);
			logLines.clear();
			for (JsonElement element : rows) {
				if (element.isJsonObject()) {
					JsonObject row = element.getAsJsonObject();
					String line = getString(row, "message");
					if (line.isBlank()) line = getString(row, "line");
					if (!line.isBlank()) logLines.add(line);
				}
			}
			logBotLabel = bot.label;
			return null;
		}, () -> page = Page.BOTLOG);
	}

	private void drawBotLog(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "日志 · " + logBotLabel, mouseX, mouseY, () -> page = Page.BOT);
		int y = l.contentY + 40;
		int maxLines = Math.max(1, (l.contentH - 60) / 14);
		int start = Math.max(0, logLines.size() - maxLines - scroll);
		for (int i = 0; i < maxLines && start + i < logLines.size(); i++) {
			String line = trim(logLines.get(start + i), 52);
			g.text(this.font, Component.literal(line), l.sx + 20, y + i * 14, 0xFF3A4556, false);
		}
		if (logLines.isEmpty()) drawCentered(g, Component.literal("暂无日志"), l.sx + l.sw / 2, y + 40, 0xFF8A96A8);
	}

	/* ==================== BOT 设置 / 自定义任务 ==================== */

	private void openBotPerms() {
		permissionsLoaded = false;
		permissions = new JsonArray();
		permRole = "whitelist";
		page = Page.BOTPERMS;
		loadPermissions();
	}

	private void loadPermissions() {
		if (settingsBotId.isBlank()) return;
		doAsync(() -> {
			JsonObject data = Api.loadSmartPermissions(settingsBotId);
			JsonArray rows = data.has("rows") ? data.getAsJsonArray("rows") : new JsonArray();
			permissions = rows;
			permissionsLoaded = true;
			return null;
		}, null);
	}

	private void ensurePermBoxes() {
		if (permNameBox == null) {
			permNameBox = new EditBox(this.font, 0, 0, 120, 22, Component.literal("玩家ID"));
			permNameBox.setTextShadow(false);
			permNameBox.setMaxLength(16);
			permNameBox.setBordered(false);
			permNameBox.setTextColor(0xFF1D2733);
			permNameBox.setHint(Component.literal("玩家 ID"));
		}
		if (permMinutesBox == null) {
			permMinutesBox = new EditBox(this.font, 0, 0, 80, 22, Component.literal("分钟"));
			permMinutesBox.setTextShadow(false);
			permMinutesBox.setMaxLength(8);
			permMinutesBox.setBordered(false);
			permMinutesBox.setTextColor(0xFF1D2733);
			permMinutesBox.setHint(Component.literal("分钟（可空）"));
		}
	}

	private static String permRoleLabel(String role) {
		return switch (role) {
			case "admin" -> "超管";
			case "blacklist" -> "黑名单";
			default -> "白名单";
		};
	}

	private void drawBotPerms(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "权限管理 · " + settingsBotLabel, mouseX, mouseY, () -> page = Page.BOTSETTINGS);
		ensurePermBoxes();
		button(g, "刷新", l.sx + l.sw - 62, l.contentY + 2, 42, 20, mouseX, mouseY, false, () -> {
			permissionsLoaded = false;
			loadPermissions();
		});
		g.text(this.font, Component.literal("超管=完全管理 / 白名单=可使用指令 / 黑名单=禁止使用"), l.sx + 20, l.contentY + 34, 0xFF8A96A8, false);

		int listY = l.contentY + 54;
		if (busy && !permissionsLoaded) {
			drawCentered(g, Component.literal("加载权限中…"), l.sx + l.sw / 2, listY + 20, 0xFF54627A);
		} else if (permissionsLoaded && permissions.isEmpty()) {
			drawCentered(g, Component.literal("还没有设置任何权限"), l.sx + l.sw / 2, listY + 16, 0xFF8A96A8);
		}
		int maxShow = Math.max(1, (l.contentH - 270) / 26);
		for (int i = 0; i < Math.min(permissions.size(), maxShow); i++) {
			JsonObject row = permissions.get(i).getAsJsonObject();
			String name = getString(row, "playerName");
			String role = getString(row, "role");
			int ry = listY + i * 26;
			int roleColor = "admin".equals(role) ? 0xFF6E3CFF : "blacklist".equals(role) ? 0xFFE05656 : 0xFF2BB98A;
			fillRounded(g, l.sx + 20, ry, l.sw - 40, 22, 8, 0x66FFFFFF);
			fillRounded(g, l.sx + 26, ry + 4, 44, 14, 7, roleColor);
			g.text(this.font, Component.literal(permRoleLabel(role)), l.sx + 30, ry + 7, 0xFFFFFFFF, false);
			g.text(this.font, Component.literal(trim(name, 18)), l.sx + 78, ry + 7, 0xFF1D2733, false);
			button(g, "×", l.sx + l.sw - 48, ry, 22, 22, mouseX, mouseY, false, () -> removePermission(name, role));
		}

		int formY = l.contentY + l.contentH - 152;
		g.text(this.font, Component.literal("添加玩家权限"), l.sx + 20, formY, 0xFF16202E, false);
		renderEditBox(g, permNameBox, l.sx + 20, formY + 16, l.sw - 40, 22, mouseX, mouseY);
		int roleY = formY + 44;
		String[] roles = {"白名单", "黑名单", "超管"};
		String[] roleKeys = {"whitelist", "blacklist", "admin"};
		int gap = 6;
		int roleW = (l.sw - 40 - gap * 2) / 3;
		for (int i = 0; i < 3; i++) {
			final String roleKey = roleKeys[i];
			boolean active = permRole.equals(roleKey);
			button(g, roles[i], l.sx + 20 + i * (roleW + gap), roleY, roleW, 22, mouseX, mouseY, active, () -> permRole = roleKey);
		}
		int minutesY = roleY + 28;
		int addW = 80;
		int minutesW = Math.max(60, (l.sx + l.sw - 100) - (l.sx + 20) - 8);
		renderEditBox(g, permMinutesBox, l.sx + 20, minutesY, minutesW, 22, mouseX, mouseY);
		button(g, "添加", l.sx + l.sw - 100, minutesY, addW, 22, mouseX, mouseY, true, this::addPermission);
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 40)), l.sx + 20, formY + 104, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 40)), l.sx + 20, formY + 104, 0xFF1F9D63, false);
	}

	private void addPermission() {
		if (busy) return;
		ensurePermBoxes();
		String name = permNameBox.getValue().trim();
		if (name.isBlank()) {
			error = "请输入玩家 ID";
			return;
		}
		String minutesText = permMinutesBox.getValue().trim();
		int minutes = 0;
		if (!minutesText.isBlank()) {
			try {
				minutes = Integer.parseInt(minutesText);
			} catch (NumberFormatException exception) {
				error = "分钟必须是整数";
				return;
			}
			if (minutes < 1 || minutes > 525600) {
				error = "临时时长必须在 1-525600 分钟";
				return;
			}
		}
		String role = permRole;
		int finalMinutes = minutes;
		doAsync(() -> {
			JsonObject payload = new JsonObject();
			payload.addProperty("botId", settingsBotId);
			payload.addProperty("playerName", name);
			payload.addProperty("role", role);
			if (finalMinutes > 0) payload.addProperty("minutes", finalMinutes);
			Api.saveSmartPermission(payload);
			permissionsLoaded = false;
			return null;
		}, () -> {
			notice = "已添加 " + name + " → " + permRoleLabel(role);
			permNameBox.setValue("");
			permMinutesBox.setValue("");
			loadPermissions();
		});
	}

	private void removePermission(String name, String role) {
		doAsync(() -> {
			JsonObject payload = new JsonObject();
			payload.addProperty("botId", settingsBotId);
			payload.addProperty("action", "remove");
			payload.addProperty("playerName", name);
			payload.addProperty("role", role);
			Api.saveSmartPermission(payload);
			permissionsLoaded = false;
			return null;
		}, () -> {
			notice = "已移除 " + name + " 的" + permRoleLabel(role);
			loadPermissions();
		});
	}

	private void openBotSettings(BotItem bot) {
		settingsBotId = bot.id;
		settingsBotLabel = bot.label;
		tasksLoaded = false;
		tasks.clear();
		editingTask = false;
		editingTaskName = "";
		page = Page.BOTSETTINGS;
		loadTasks();
	}

	private void loadTasks() {
		if (settingsBotId.isBlank()) return;
		doAsync(() -> {
			JsonArray rows = Api.loadSmartTasks(settingsBotId);
			List<TaskItem> list = new ArrayList<>();
			for (JsonElement element : rows) {
				if (!element.isJsonObject()) continue;
				JsonObject row = element.getAsJsonObject();
				String name = getString(row, "name");
				JsonArray parameters = row.has("parameters") && row.get("parameters").isJsonArray() ? row.getAsJsonArray("parameters") : new JsonArray();
				JsonArray steps = row.has("steps") && row.get("steps").isJsonArray() ? row.getAsJsonArray("steps") : new JsonArray();
				int interval = getInt(row, "intervalTicks");
				int actionCount = 0;
				for (JsonElement step : steps) {
					if (step.isJsonObject() && "command".equals(getString(step.getAsJsonObject(), "type"))) actionCount++;
				}
				list.add(new TaskItem(name, actionCount, interval, parameters, steps));
			}
			tasks.clear();
			tasks.addAll(list);
			tasksLoaded = true;
			return null;
		}, null);
	}

	private void drawBotSettings(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "Bot 设置 · " + settingsBotLabel, mouseX, mouseY, () -> page = Page.BOT);
		g.text(this.font, Component.literal("自定义任务"), l.sx + 20, l.contentY + 36, 0xFF16202E, false);
		g.text(this.font, Component.literal("任务通过游戏内指令调用，可设置定时自动执行"), l.sx + 20, l.contentY + 50, 0xFF8A96A8, false);

		int y = l.contentY + 72;
		if (busy && !tasksLoaded) {
			drawCentered(g, Component.literal("加载任务中…"), l.sx + l.sw / 2, y + 30, 0xFF54627A);
			return;
		}
		if (tasksLoaded && tasks.isEmpty()) {
			drawCentered(g, Component.literal("还没有自定义任务"), l.sx + l.sw / 2, y + 24, 0xFF8A96A8);
		}
		int maxShow = Math.max(1, (l.contentH - 140) / 58);
		for (int i = 0; i < Math.min(tasks.size(), maxShow); i++) {
			TaskItem task = tasks.get(i);
			int cardY = y + i * 58;
			fillGlass(g, l.sx + 18, cardY, l.sw - 36, 52, 14, 0x80FFFFFF);
			g.text(this.font, Component.literal(trim(task.name, 18)), l.sx + 30, cardY + 8, 0xFF16202E, false);
			String meta = "动作 " + task.stepCount + " · " + (task.intervalTicks > 0 ? "定时 " + task.intervalTicks + "gt" : "未定时");
			g.text(this.font, Component.literal(meta), l.sx + 30, cardY + 24, 0xFF6E7C93, false);
			button(g, "编辑", l.sx + l.sw - 96, cardY + 14, 32, 22, mouseX, mouseY, false, () -> openTaskEditor(task));
			button(g, "删", l.sx + l.sw - 58, cardY + 14, 26, 22, mouseX, mouseY, false, () -> deleteTask(task));
		}

		button(g, "＋ 新建任务", l.sx + 20, l.contentY + l.contentH - 34, (l.sw - 48) / 2, 26, mouseX, mouseY, true, () -> openTaskEditor(null));
		button(g, "权限管理", l.sx + 28 + (l.sw - 48) / 2, l.contentY + l.contentH - 34, (l.sw - 48) / 2, 26, mouseX, mouseY, false, () -> openBotPerms());
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 40)), l.sx + 20, l.contentY + l.contentH - 54, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 40)), l.sx + 20, l.contentY + l.contentH - 54, 0xFF1F9D63, false);
	}

	private void deleteTask(TaskItem task) {
		if (busy) return;
		doAsync(() -> {
			JsonObject payload = new JsonObject();
			payload.addProperty("botId", settingsBotId);
			payload.addProperty("action", "delete");
			payload.addProperty("name", task.name);
			Api.saveSmartTask(payload);
			tasksLoaded = false;
			return null;
		}, () -> {
			notice = "已删除任务：" + task.name;
			loadTasks();
		});
	}

	private void openTaskEditor(TaskItem task) {
		ensureTaskBoxes();
		editingTask = task != null;
		editingTaskName = task == null ? "" : task.name;
		editingTaskParameters = task == null ? new JsonArray() : task.parameters;
		taskNameBox.setValue(task == null ? "" : task.name);
		taskIntervalBox.setValue(task == null || task.intervalTicks <= 0 ? "" : String.valueOf(task.intervalTicks));
		actionRows.clear();
		if (task != null && task.steps != null) {
			for (JsonElement element : task.steps) {
				if (!element.isJsonObject()) continue;
				JsonObject step = element.getAsJsonObject();
				String type = getString(step, "type");
				if ("command".equals(type)) {
					JsonArray args = step.has("args") && step.get("args").isJsonArray() ? step.getAsJsonArray("args") : new JsonArray();
					StringBuilder join = new StringBuilder();
					for (JsonElement arg : args) {
						if (join.length() > 0) join.append(' ');
						join.append(arg.getAsString());
					}
					actionRows.add(new ActionRow(getString(step, "command"), join.toString(), 20));
				} else if ("wait".equals(type) && !actionRows.isEmpty()) {
					actionRows.get(actionRows.size() - 1).wait = Math.max(1, getInt(step, "ticks"));
				}
			}
		}
		if (actionRows.isEmpty()) actionRows.add(new ActionRow("指令", "", 20));
		rebuildActionBoxes();
		focusedBox = taskNameBox;
		taskNameBox.setFocused(true);
		commandPickerRow = -1;
		page = Page.TASKEDIT;
		notice = "";
		error = "";
	}

	private void rebuildActionBoxes() {
		while (actionArgsBoxes.size() < actionRows.size()) {
			EditBox box = new EditBox(this.font, 0, 0, 100, 22, Component.literal("参数"));
			box.setTextShadow(false);
			box.setMaxLength(256);
			box.setBordered(false);
			box.setTextColor(0xFF1D2733);
			box.setHint(Component.literal("参数（空格分隔）"));
			actionArgsBoxes.add(box);
		}
		while (actionArgsBoxes.size() > actionRows.size()) actionArgsBoxes.remove(actionArgsBoxes.size() - 1);
		while (actionWaitBoxes.size() < Math.max(0, actionRows.size() - 1)) {
			EditBox box = new EditBox(this.font, 0, 0, 50, 20, Component.literal("等待"));
			box.setTextShadow(false);
			box.setMaxLength(6);
			box.setBordered(false);
			box.setTextColor(0xFF1D2733);
			box.setHint(Component.literal("20"));
			actionWaitBoxes.add(box);
		}
		while (actionWaitBoxes.size() > Math.max(0, actionRows.size() - 1)) actionWaitBoxes.remove(actionWaitBoxes.size() - 1);
		for (int i = 0; i < actionRows.size(); i++) {
			actionArgsBoxes.get(i).setValue(actionRows.get(i).args);
			if (i < actionRows.size() - 1) actionWaitBoxes.get(i).setValue(String.valueOf(actionRows.get(i).wait));
		}
	}

	private void drawTaskEdit(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, editingTask ? "编辑任务 · " + editingTaskName : "新建任务", mouseX, mouseY, () -> {
			commandPickerRow = -1;
			page = Page.BOTSETTINGS;
		});
		ensureTaskBoxes();
		int y = l.contentY + 40;
		g.text(this.font, Component.literal("任务名（不含空格）"), l.sx + 20, y, 0xFF54627A, false);
		renderEditBox(g, taskNameBox, l.sx + 20, y + 14, l.sw - 40, 24, mouseX, mouseY);

		g.text(this.font, Component.literal("定时间隔（gt，留空=不定时）"), l.sx + 20, y + 44, 0xFF54627A, false);
		renderEditBox(g, taskIntervalBox, l.sx + 20, y + 58, 150, 24, mouseX, mouseY);

		g.text(this.font, Component.literal("动作步骤（点命令按钮可快捷选择）"), l.sx + 20, y + 88, 0xFF54627A, false);
		int actionY = y + 104;
		int rowH = 54;
		int maxRows = 5;
		for (int i = 0; i < actionRows.size() && i < maxRows; i++) {
			ActionRow row = actionRows.get(i);
			int ry = actionY + i * rowH;
			// 命令按钮（点击弹出快捷选择）
			final int rowIndex = i;
			button(g, row.command, l.sx + 20, ry, 76, 22, mouseX, mouseY, false, () -> commandPickerRow = commandPickerRow == rowIndex ? -1 : rowIndex);
			EditBox argsBox = actionArgsBoxes.get(i);
			renderEditBox(g, argsBox, l.sx + 102, ry, l.sw - 102 - 52, 22, mouseX, mouseY);
			button(g, "×", l.sx + l.sw - 44, ry, 22, 22, mouseX, mouseY, false, () -> removeActionRow(rowIndex));
			if (i < actionRows.size() - 1) {
				g.text(this.font, Component.literal("等待"), l.sx + 22, ry + 30, 0xFF8A96A8, false);
				renderEditBox(g, actionWaitBoxes.get(i), l.sx + 54, ry + 26, 48, 20, mouseX, mouseY);
				g.text(this.font, Component.literal("gt"), l.sx + 108, ry + 30, 0xFF8A96A8, false);
			}
		}
		if (actionRows.size() < maxRows) {
			button(g, "＋ 添加动作", l.sx + 20, actionY + actionRows.size() * rowH, 110, 24, mouseX, mouseY, false, this::addActionRow);
		}
		if (actionRows.size() >= maxRows) {
			g.text(this.font, Component.literal("最多 5 个动作（手机界面限制）"), l.sx + 20, actionY + maxRows * rowH, 0xFF8A96A8, false);
		}

		button(g, "保存", l.sx + 20, l.contentY + l.contentH - 34, 90, 26, mouseX, mouseY, true, this::saveTask);
		button(g, "取消", l.sx + 118, l.contentY + l.contentH - 34, 70, 26, mouseX, mouseY, false, () -> {
			commandPickerRow = -1;
			page = Page.BOTSETTINGS;
		});
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 42)), l.sx + 20, l.contentY + l.contentH - 52, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 42)), l.sx + 20, l.contentY + l.contentH - 52, 0xFF1F9D63, false);

		if (commandPickerRow >= 0) drawCommandPicker(g, l, mouseX, mouseY);
	}

	private void addActionRow() {
		if (actionRows.size() >= 5) return;
		actionRows.add(new ActionRow("指令", "", 20));
		rebuildActionBoxes();
	}

	private void removeActionRow(int index) {
		if (index < 0 || index >= actionRows.size() || actionRows.size() <= 1) return;
		actionRows.remove(index);
		rebuildActionBoxes();
	}

	private void drawCommandPicker(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		g.fill(l.sx, l.sy + 30, l.sx + l.sw, l.sy + l.sh, 0x66000000);
		int cols = 2;
		int btnW = 104;
		int btnH = 22;
		int gap = 6;
		int rows = (TASK_COMMANDS.size() + cols - 1) / cols;
		int panelW = cols * btnW + (cols - 1) * gap + 28;
		int panelH = rows * (btnH + gap) + 44;
		int px = l.sx + (l.sw - panelW) / 2;
		int py = l.sy + 60;
		fillGlass(g, px, py, panelW, panelH, 18, 0xF2FFFFFF);
		g.text(this.font, Component.literal("选择命令"), px + 14, py + 10, 0xFF16202E, false);
		boolean anyHit = false;
		for (int i = 0; i < TASK_COMMANDS.size(); i++) {
			String command = TASK_COMMANDS.get(i);
			int col = i % cols;
			int row = i / cols;
			int bx = px + 14 + col * (btnW + gap);
			int by = py + 32 + row * (btnH + gap);
			boolean hover = mouseX >= bx && mouseX <= bx + btnW && mouseY >= by && mouseY <= by + btnH;
			fillRounded(g, bx, by, btnW, btnH, 8, hover ? 0xB32F7CFF : 0x99FFFFFF);
			drawCentered(g, Component.literal(command), bx + btnW / 2, by + (btnH - 8) / 2, hover ? 0xFFFFFFFF : 0xFF2A3446);
			final int pickerRow = commandPickerRow;
			pickerHits.add(new Hit(bx, by, btnW, btnH, () -> {
				if (pickerRow >= 0 && pickerRow < actionRows.size()) {
					actionRows.get(pickerRow).command = command;
					rebuildActionBoxes();
				}
				commandPickerRow = -1;
			}));
		}
	}

	private void renderEditBox(GuiGraphicsExtractor g, EditBox box, int x, int y, int w, int h, int mouseX, int mouseY) {
		box.setBordered(false);
		box.setWidth(w - 12);
		box.setHeight(h);
		box.setX(x + 6);
		box.setY(y + (h - 8) / 2);
		fillRounded(g, x, y, w, h, 8, box.isFocused() ? 0xCCFFFFFF : 0x66FFFFFF);
		box.extractWidgetRenderState(g, mouseX, mouseY, 0f);
		boxHits.add(new BoxHit(box, x, y, w, h));
	}

	private void saveTask() {
		if (busy) return;
		ensureTaskBoxes();
		String name = taskNameBox.getValue().trim();
		if (name.isBlank() || name.contains(" ")) {
			error = "任务名不能为空且不能含空格";
			return;
		}
		if (actionRows.isEmpty()) {
			error = "至少需要添加一个动作";
			return;
		}
		JsonArray steps = new JsonArray();
		try {
			for (int i = 0; i < actionRows.size(); i++) {
				ActionRow row = actionRows.get(i);
				row.args = actionArgsBoxes.get(i).getValue().trim();
				if (i < actionRows.size() - 1) {
					String waitText = actionWaitBoxes.get(i).getValue().trim();
					row.wait = waitText.isBlank() ? 20 : Integer.parseInt(waitText);
					if (row.wait < 1) throw new Exception("等待时间至少 1gt");
				}
				JsonObject action = new JsonObject();
				action.addProperty("type", "command");
				action.addProperty("command", row.command);
				JsonArray args = new JsonArray();
				for (String arg : row.args.split("\\s+")) if (!arg.isBlank()) args.add(arg);
				action.add("args", args);
				steps.add(action);
				if (i < actionRows.size() - 1) {
					JsonObject wait = new JsonObject();
					wait.addProperty("type", "wait");
					wait.addProperty("ticks", row.wait);
					steps.add(wait);
				}
			}
		} catch (NumberFormatException exception) {
			error = "等待时间必须是整数 gt";
			return;
		} catch (Exception exception) {
			error = exception.getMessage();
			return;
		}
		String intervalText = taskIntervalBox.getValue().trim();
		int interval = 0;
		if (!intervalText.isBlank()) {
			try {
				interval = Integer.parseInt(intervalText);
			} catch (NumberFormatException exception) {
				error = "定时间隔必须是整数 gt";
				return;
			}
			if (interval < 1) {
				error = "定时间隔至少 1gt";
				return;
			}
		}
		int finalInterval = interval;
		JsonArray finalSteps = steps;
		doAsync(() -> {
			JsonObject payload = new JsonObject();
			payload.addProperty("botId", settingsBotId);
			payload.addProperty("name", name);
			if (editingTask && !editingTaskName.isBlank()) payload.addProperty("originalName", editingTaskName);
			payload.addProperty("schedulePersistent", true);
			payload.add("parameters", editingTaskParameters);
			payload.add("steps", finalSteps);
			Api.saveSmartTask(payload);
			if (finalInterval > 0) {
				JsonObject schedule = new JsonObject();
				schedule.addProperty("botId", settingsBotId);
				schedule.addProperty("action", "schedule");
				schedule.addProperty("name", name);
				schedule.addProperty("intervalTicks", finalInterval);
				Api.saveSmartTask(schedule);
			}
			tasksLoaded = false;
			return null;
		}, () -> {
			commandPickerRow = -1;
			page = Page.BOTSETTINGS;
			editingTask = false;
			editingTaskName = "";
			notice = "任务已保存";
			loadTasks();
		});
	}

	private static String serializeSteps(JsonArray steps) {
		StringBuilder builder = new StringBuilder();
		for (JsonElement element : steps) {
			if (!element.isJsonObject()) continue;
			JsonObject step = element.getAsJsonObject();
			String type = getString(step, "type");
			if ("wait".equals(type)) {
				if (builder.length() > 0) builder.append(" | ");
				builder.append("等待 ").append(getInt(step, "ticks"));
			} else {
				if (builder.length() > 0) builder.append(" | ");
				builder.append(getString(step, "command"));
				JsonArray args = step.has("args") && step.get("args").isJsonArray() ? step.getAsJsonArray("args") : new JsonArray();
				for (JsonElement arg : args) builder.append(' ').append(arg.getAsString());
			}
		}
		return builder.toString();
	}

	private static final java.util.Set<String> ALLOWED_TASK_COMMANDS = java.util.Set.of(
		"上车", "下车", "清空", "拿出", "丢弃", "副手", "切换", "放置", "右键", "攻击", "挖掘", "移动", "注视", "指令");

	private JsonArray parseSteps(String text) throws Exception {
		JsonArray steps = new JsonArray();
		Integer pendingWait = null;
		boolean hasAction = false;
		String[] parts = text == null ? new String[0] : text.split("\\|");
		for (String part : parts) {
			String value = part.trim();
			if (value.isEmpty()) continue;
			if (value.matches("^(等待|wait)\\s+\\d+$")) {
				pendingWait = Integer.parseInt(value.replaceAll("^\\D+", ""));
				continue;
			}
			String[] tokens = value.split("\\s+");
			String command = tokens[0];
			if (!ALLOWED_TASK_COMMANDS.contains(command)) throw new Exception("未知指令：" + command);
			if (hasAction) {
				JsonObject wait = new JsonObject();
				wait.addProperty("type", "wait");
				wait.addProperty("ticks", pendingWait == null ? 20 : pendingWait);
				steps.add(wait);
			}
			JsonObject action = new JsonObject();
			action.addProperty("type", "command");
			action.addProperty("command", command);
			JsonArray args = new JsonArray();
			for (int i = 1; i < tokens.length; i++) args.add(tokens[i]);
			action.add("args", args);
			steps.add(action);
			hasAction = true;
			pendingWait = null;
		}
		if (!hasAction) throw new Exception("至少需要一条指令步骤");
		return steps;
	}

	private void ensureTaskBoxes() {
		if (taskNameBox == null) {
			taskNameBox = new EditBox(this.font, 0, 0, 120, 24, Component.literal("任务名"));
			taskNameBox.setTextShadow(false);
			taskNameBox.setMaxLength(32);
			taskNameBox.setBordered(false);
			taskNameBox.setTextColor(0xFF1D2733);
			taskNameBox.setHint(Component.literal("例如 my_task"));
		}
		if (taskIntervalBox == null) {
			taskIntervalBox = new EditBox(this.font, 0, 0, 120, 24, Component.literal("间隔"));
			taskIntervalBox.setTextShadow(false);
			taskIntervalBox.setMaxLength(8);
			taskIntervalBox.setBordered(false);
			taskIntervalBox.setTextColor(0xFF1D2733);
			taskIntervalBox.setHint(Component.literal("例如 1200"));
		}
		if (taskStepsBox == null) {
			taskStepsBox = new EditBox(this.font, 0, 0, 120, 24, Component.literal("步骤"));
			taskStepsBox.setTextShadow(false);
			taskStepsBox.setMaxLength(512);
			taskStepsBox.setBordered(false);
			taskStepsBox.setTextColor(0xFF1D2733);
			taskStepsBox.setHint(Component.literal("右键 准星 | 等待 20 | 指令 !home"));
		}
	}

	/* ==================== 设置 / 关于 ==================== */

	private void loadSubscriptions() {
		if (!Api.loggedIn) {
			pendingPage = Page.SUBSCRIPTION;
			openLogin();
			return;
		}
		if (subscriptionsLoaded) return;
		doAsync(() -> {
			subscriptions = Api.loadSubscriptions();
			subscriptionsLoaded = true;
			return null;
		}, null);
	}

	private void drawSubscription(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "订阅与钱包", mouseX, mouseY, () -> page = Page.HOME);
		if (!Api.loggedIn) {
			drawCentered(g, Component.literal("请先登录"), l.sx + l.sw / 2, l.contentY + 80, 0xFF8A96A8);
			button(g, "去登录", l.sx + l.sw / 2 - 40, l.contentY + 100, 80, 26, mouseX, mouseY, true, this::openLogin);
			return;
		}
		if (busy && !subscriptionsLoaded) {
			drawCentered(g, Component.literal("加载订阅中…"), l.sx + l.sw / 2, l.contentY + 80, 0xFF54627A);
			return;
		}
		JsonObject data = subscriptions == null ? new JsonObject() : subscriptions;
		double wallet = 0;
		if (data.has("wallet") && data.get("wallet").isJsonObject()) {
			JsonObject walletObj = data.getAsJsonObject("wallet");
			if (walletObj.has("zzko") && !walletObj.get("zzko").isJsonNull()) wallet = walletObj.get("zzko").getAsDouble();
		}
		if (wallet == 0 && zzkoBalance > 0) wallet = zzkoBalance;
		g.text(this.font, Component.literal("zzko 余额 " + formatZzko(wallet)), l.sx + 20, l.contentY + 34, 0xFF2F7CFF, false);

		JsonArray plans = data.has("plans") && data.get("plans").isJsonArray() ? data.getAsJsonArray("plans") : new JsonArray();
		int shown = 0;
		int y = l.contentY + 58;
		for (JsonElement element : plans) {
			if (shown >= 4) break;
			if (!element.isJsonObject()) continue;
			JsonObject plan = element.getAsJsonObject();
			if (plan.has("visible") && !plan.get("visible").getAsBoolean()) continue;
			int cardY = y + shown * 60;
			fillGlass(g, l.sx + 18, cardY, l.sw - 36, 54, 14, 0x80FFFFFF);
			String name = getString(plan, "name");
			if (name.isBlank()) name = getString(plan, "id");
			g.text(this.font, Component.literal(trim(name, 18)), l.sx + 30, cardY + 8, 0xFF16202E, false);
			String price = plan.has("price") && !plan.get("price").isJsonNull() ? plan.get("price").getAsString() : "—";
			String scope = "custom".equals(getString(plan, "scope")) ? "组织" : "个人";
			String desc = getString(plan, "description");
			g.text(this.font, Component.literal(price + " zzko · " + scope), l.sx + 30, cardY + 24, 0xFF54627A, false);
			if (!desc.isBlank()) g.text(this.font, Component.literal(trim(desc, 22)), l.sx + 30, cardY + 38, 0xFF8A96A8, false);
			String planId = getString(plan, "id");
			button(g, "购买", l.sx + l.sw - 82, cardY + 16, 52, 22, mouseX, mouseY, true, () -> purchaseSubscription(planId));
			shown++;
		}
		if (plans.isEmpty()) {
			drawCentered(g, Component.literal("暂无可用订阅计划"), l.sx + l.sw / 2, y + 40, 0xFF8A96A8);
		}
		button(g, "刷新", l.sx + 20, l.contentY + l.contentH - 34, 72, 24, mouseX, mouseY, false, () -> {
			subscriptionsLoaded = false;
			loadSubscriptions();
		});
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 40)), l.sx + 20, l.contentY + l.contentH - 54, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 40)), l.sx + 20, l.contentY + l.contentH - 54, 0xFF1F9D63, false);
	}

	private void purchaseSubscription(String planId) {
		String owner = Api.defaultOwnerUuid;
		if (owner == null || owner.isBlank()) owner = Api.userUuid;
		if (owner == null || owner.isBlank()) {
			error = "没有可用的仓库归属";
			return;
		}
		String finalOwner = owner;
		doAsync(() -> {
			JsonObject result = Api.purchaseSubscription(finalOwner, planId);
			notice = result.has("message") ? result.get("message").getAsString() : "购买成功";
			subscriptionsLoaded = false;
			return null;
		}, () -> {
			loadBalance();
			loadSubscriptions();
		});
	}

	private void ensureTpBoxes() {
		if (tpAcceptBox == null) {
			tpAcceptBox = new EditBox(this.font, 0, 0, 120, 22, Component.literal("同意指令"));
			tpAcceptBox.setTextShadow(false);
			tpAcceptBox.setMaxLength(64);
			tpAcceptBox.setBordered(false);
			tpAcceptBox.setTextColor(0xFF1D2733);
			tpAcceptBox.setValue(ModConfig.tpAcceptCommand);
		}
		if (tpDenyBox == null) {
			tpDenyBox = new EditBox(this.font, 0, 0, 120, 22, Component.literal("拒绝指令"));
			tpDenyBox.setTextShadow(false);
			tpDenyBox.setMaxLength(64);
			tpDenyBox.setBordered(false);
			tpDenyBox.setTextColor(0xFF1D2733);
			tpDenyBox.setValue(ModConfig.tpDenyCommand);
		}
	}

	private void saveTpCommands() {
		ensureTpBoxes();
		String accept = tpAcceptBox.getValue().trim();
		String deny = tpDenyBox.getValue().trim();
		if (accept.isBlank() || deny.isBlank()) {
			error = "自动同意/拒绝指令不能为空";
			return;
		}
		ModConfig.tpAcceptCommand = accept;
		ModConfig.tpDenyCommand = deny;
		ModConfig.save();
		notice = "传送指令已保存";
	}

	private static String cloudHost() {
		try {
			java.net.URI uri = java.net.URI.create(Api.cloudBase());
			return uri.getHost() == null ? Api.cloudBase() : uri.getHost();
		} catch (Throwable ignored) {
			return Api.cloudBase();
		}
	}

	private static String platformHost() {
		try {
			java.net.URI uri = java.net.URI.create(Api.platformBase());
			return uri.getHost() == null ? Api.platformBase() : uri.getHost();
		} catch (Throwable ignored) {
			return Api.platformBase();
		}
	}

	private void ensureServerBoxes() {
		if (serverCloudBox == null) {
			serverCloudBox = new EditBox(this.font, 0, 0, 200, 24, Component.literal("云仓库地址"));
			serverCloudBox.setTextShadow(false);
			serverCloudBox.setMaxLength(128);
			serverCloudBox.setBordered(false);
			serverCloudBox.setTextColor(0xFF1D2733);
			serverCloudBox.setValue(ModConfig.cloudBase);
		}
		if (serverPlatformBox == null) {
			serverPlatformBox = new EditBox(this.font, 0, 0, 200, 24, Component.literal("Bot 平台地址"));
			serverPlatformBox.setTextShadow(false);
			serverPlatformBox.setMaxLength(128);
			serverPlatformBox.setBordered(false);
			serverPlatformBox.setTextColor(0xFF1D2733);
			serverPlatformBox.setValue(ModConfig.platformBase);
		}
		if (serverBotBox == null) {
			serverBotBox = new EditBox(this.font, 0, 0, 120, 24, Component.literal("机器人名称"));
			serverBotBox.setTextShadow(false);
			serverBotBox.setMaxLength(32);
			serverBotBox.setBordered(false);
			serverBotBox.setTextColor(0xFF1D2733);
			serverBotBox.setValue(ModConfig.botName);
		}
	}

	private void drawServerCfg(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "服务器设置", mouseX, mouseY, () -> page = Page.SETTINGS);
		ensureServerBoxes();
		int y = l.contentY + 40;
		g.text(this.font, Component.literal("适用于同一套云仓库/Bot 平台程序的其它服务器"), l.sx + 20, y, 0xFF8A96A8, false);

		g.text(this.font, Component.literal("云仓库地址（例如 https://你的域名）"), l.sx + 20, y + 24, 0xFF54627A, false);
		renderEditBox(g, serverCloudBox, l.sx + 20, y + 38, l.sw - 40, 24, mouseX, mouseY);
		g.text(this.font, Component.literal("Bot 平台地址（例如 https://bot.你的域名）"), l.sx + 20, y + 70, 0xFF54627A, false);
		renderEditBox(g, serverPlatformBox, l.sx + 20, y + 84, l.sw - 40, 24, mouseX, mouseY);
		g.text(this.font, Component.literal("游戏内机器人名称（用于登录提示）"), l.sx + 20, y + 116, 0xFF54627A, false);
		renderEditBox(g, serverBotBox, l.sx + 20, y + 130, l.sw - 40, 24, mouseX, mouseY);
		g.text(this.font, Component.literal("修改后需要重新登录，旧会话会失效"), l.sx + 20, y + 162, 0xFF8A96A8, false);

		button(g, "保存并重载", l.sx + 20, y + 184, 110, 26, mouseX, mouseY, true, () -> {
			String cloud = serverCloudBox.getValue() == null ? "" : serverCloudBox.getValue().trim();
			String platform = serverPlatformBox.getValue() == null ? "" : serverPlatformBox.getValue().trim();
			String bot = serverBotBox.getValue() == null ? "" : serverBotBox.getValue().trim();
			if (cloud.isBlank() || platform.isBlank()) {
				error = "云仓库地址和 Bot 平台地址不能为空";
				return;
			}
			if (!cloud.startsWith("http://") && !cloud.startsWith("https://")) cloud = "https://" + cloud;
			if (!platform.startsWith("http://") && !platform.startsWith("https://")) platform = "https://" + platform;
			if (bot.isBlank()) {
				error = "机器人名称不能为空";
				return;
			}
			ModConfig.cloudBase = cloud;
			ModConfig.platformBase = platform;
			ModConfig.botName = bot;
			ModConfig.save();
			Api.resetSession();
			warehouseLoaded = false;
			botsLoaded = false;
			subscriptionsLoaded = false;
			permissionsLoaded = false;
			inventory.clear();
			bots.clear();
			selected.clear();
			notice = "已保存，请重新登录";
			error = "";
		});
		button(g, "恢复默认", l.sx + 138, y + 184, 80, 26, mouseX, mouseY, false, () -> {
			serverCloudBox.setValue("https://zzyok.cn");
			serverPlatformBox.setValue("https://bot.zzyok.cn");
			serverBotBox.setValue("machel11");
			ModConfig.cloudBase = "https://zzyok.cn";
			ModConfig.platformBase = "https://bot.zzyok.cn";
			ModConfig.botName = "machel11";
			ModConfig.save();
			Api.resetSession();
			notice = "已恢复默认地址，请重新登录";
		});
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 42)), l.sx + 20, y + 222, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 42)), l.sx + 20, y + 222, 0xFF1F9D63, false);
	}

	private void ensureRepairBox() {
		if (repairTargetBox == null) {
			repairTargetBox = new EditBox(this.font, 0, 0, 160, 24, Component.literal("目标玩家"));
			repairTargetBox.setTextShadow(false);
			repairTargetBox.setMaxLength(16);
			repairTargetBox.setBordered(false);
			repairTargetBox.setTextColor(0xFF1D2733);
			repairTargetBox.setHint(Component.literal("玩家ID（TAB补全）"));
			repairTargetBox.setValue(Api.username == null ? "" : Api.username);
		}
	}

	private void ensureRepairOwner() {
		String current = repairOwnerUuid;
		boolean exists = false;
		for (Api.Owner owner : Api.owners) {
			if (owner.uuid().equals(current)) {
				exists = true;
				break;
			}
		}
		if (!exists) {
			String defaultOwner = Api.defaultOwnerUuid;
			if (defaultOwner != null && !defaultOwner.isBlank()) repairOwnerUuid = defaultOwner;
			else repairOwnerUuid = Api.owners.isEmpty() ? "" : Api.owners.get(0).uuid();
		}
	}

	private void cycleRepairOwner() {
		if (Api.owners.isEmpty()) return;
		int current = -1;
		for (int i = 0; i < Api.owners.size(); i++) {
			if (Api.owners.get(i).uuid().equals(repairOwnerUuid)) {
				current = i;
				break;
			}
		}
		repairOwnerUuid = Api.owners.get((current + 1) % Api.owners.size()).uuid();
	}

	private void drawRepair(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "修装备", mouseX, mouseY, () -> page = Page.HOME);
		ensureRepairBox();
		ensureRepairOwner();
		int y = l.contentY + 34;
		g.text(this.font, Component.literal("消耗所选仓库的经验瓶，BOT 会传送到目标附近修装备"), l.sx + 20, y, 0xFF54627A, false);
		g.text(this.font, Component.literal("只从指定仓库取，不会全仓库搜索"), l.sx + 20, y + 14, 0xFF8A96A8, false);
		g.text(this.font, Component.literal("来源仓库（点击切换）"), l.sx + 20, y + 36, 0xFF54627A, false);
		String ownerLabel = repairOwnerUuid.isBlank() ? "默认仓库" : Api.ownerLabel(repairOwnerUuid);
		button(g, trim(ownerLabel, 18), l.sx + 20, y + 50, l.sw - 40, 22, mouseX, mouseY, false, this::cycleRepairOwner);
		g.text(this.font, Component.literal("目标玩家 ID（留空 = 修自己）"), l.sx + 20, y + 82, 0xFF54627A, false);
		renderEditBox(g, repairTargetBox, l.sx + 20, y + 96, l.sw - 40, 24, mouseX, mouseY);
		button(g, "修自己", l.sx + 20, y + 128, 90, 26, mouseX, mouseY, true, () -> doRepair(Api.username));
		button(g, "修复目标", l.sx + 118, y + 128, 90, 26, mouseX, mouseY, false, this::doRepair);
		g.text(this.font, Component.literal("接受 BOT 传送请求，离开 4 格会取消"), l.sx + 20, y + 164, 0xFF8A96A8, false);
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 42)), l.sx + 20, y + 182, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 42)), l.sx + 20, y + 182, 0xFF1F9D63, false);
	}

	private void doRepair() {
		ensureRepairBox();
		String target = repairTargetBox.getValue() == null ? "" : repairTargetBox.getValue().trim();
		if (target.isBlank()) target = Api.username;
		doRepair(target);
	}

	private void doRepair(String target) {
		if (!Api.loggedIn) {
			pendingPage = Page.REPAIR;
			openLogin();
			return;
		}
		if (target == null || target.isBlank()) {
			error = "请输入目标玩家 ID";
			return;
		}
		ensureRepairOwner();
		String finalTarget = target;
		String finalOwner = repairOwnerUuid;
		doAsync(() -> {
			Api.repair(finalTarget, finalOwner);
			return null;
		}, () -> {
			playLevelUp();
			notice = "已提交修装备：" + finalTarget + "（等待 BOT 传送）";
		});
	}

	private void ensureTriggerBox() {
		if (triggerBox == null) {
			triggerBox = new EditBox(this.font, 0, 0, 160, 24, Component.literal("图腾名称"));
			triggerBox.setTextShadow(false);
			triggerBox.setMaxLength(48);
			triggerBox.setBordered(false);
			triggerBox.setTextColor(0xFF1D2733);
			triggerBox.setHint(Component.literal("例如 ZZYOK Phone"));
			triggerBox.setValue(ModConfig.totemTriggerName);
		}
	}

	private void drawTriggerSettings(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "图腾触发设置", mouseX, mouseY, () -> page = Page.SETTINGS);
		ensureTriggerBox();
		int y = l.contentY + 40;
		g.text(this.font, Component.literal("只有改名后的不死图腾才能打开手机界面"), l.sx + 20, y, 0xFF16202E, true);
		g.text(this.font, Component.literal("原名的不死图腾右键不会打开"), l.sx + 20, y + 18, 0xFF8A96A8, false);

		boolean required = ModConfig.requireRenamedTotem;
		int rowY = y + 44;
		fillRounded(g, l.sx + 20, rowY, l.sw - 40, 28, 9, required ? 0xB32F7CFF : 0x66FFFFFF);
		g.text(this.font, Component.literal("仅改名图腾打开"), l.sx + 32, rowY + 9, required ? 0xFFFFFFFF : 0xFF3A4556, false);
		hits.add(new Hit(l.sx + 20, rowY, l.sw - 40, 28, () -> {
			playClick(1.1f);
			ModConfig.requireRenamedTotem = !ModConfig.requireRenamedTotem;
			ModConfig.save();
			notice = ModConfig.requireRenamedTotem ? "已开启：必须改名图腾" : "已关闭：任意不死图腾都能打开";
		}));

		g.text(this.font, Component.literal("触发名称（铁砧改名，颜色代码会自动忽略）"), l.sx + 20, rowY + 44, 0xFF54627A, false);
		renderEditBox(g, triggerBox, l.sx + 20, rowY + 58, l.sw - 40, 26, mouseX, mouseY);
		g.text(this.font, Component.literal("示例：把图腾改名为 " + (ModConfig.totemTriggerName.isBlank() ? "ZZYOK Phone" : ModConfig.totemTriggerName)), l.sx + 20, rowY + 88, 0xFF8A96A8, false);
		button(g, "保存名称", l.sx + 20, rowY + 106, 90, 24, mouseX, mouseY, true, () -> {
			String value = triggerBox.getValue() == null ? "" : triggerBox.getValue().trim();
			if (value.isBlank()) {
				error = "触发名称不能为空";
				return;
			}
			ModConfig.totemTriggerName = value;
			ModConfig.save();
			notice = "图腾触发名称已保存：" + value;
			error = "";
		});
		button(g, "恢复默认", l.sx + 118, rowY + 106, 80, 24, mouseX, mouseY, false, () -> {
			triggerBox.setValue("ZZYOK Phone");
			ModConfig.totemTriggerName = "ZZYOK Phone";
			ModConfig.save();
			notice = "已恢复默认名称：ZZYOK Phone";
		});
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 42)), l.sx + 20, rowY + 140, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 42)), l.sx + 20, rowY + 140, 0xFF1F9D63, false);
	}

	private void drawSettings(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "设置", mouseX, mouseY, () -> page = Page.HOME);
		ensureTpBoxes();
		int y = l.contentY + 40;
		fillGlass(g, l.sx + 18, y, l.sw - 36, 96, 18, 0x73FFFFFF);
		g.text(this.font, Component.literal("账号"), l.sx + 32, y + 12, 0xFF16202E, false);
		g.text(this.font, Component.literal(Api.loggedIn ? Api.username + "（" + (Api.admin ? "管理员" : "玩家") + "）" : "未登录"), l.sx + 32, y + 28, 0xFF54627A, false);
		button(g, "图腾触发", l.sx + 96, y + 50, 62, 24, mouseX, mouseY, false, () -> {
			page = Page.TRIGGER;
		});
		button(g, "服务器", l.sx + 162, y + 50, 62, 24, mouseX, mouseY, false, () -> {
			page = Page.SERVERCFG;
		});
		button(g, Api.loggedIn ? "退出" : "登录", l.sx + 32, y + 50, 56, 24, mouseX, mouseY, false, () -> {
			if (Api.loggedIn) {
				doAsync(() -> {
					Api.logout();
					return null;
				}, () -> {
					inventory.clear();
					selected.clear();
					warehouseLoaded = false;
					bots.clear();
					botsLoaded = false;
					subscriptionsLoaded = false;
					selectedOwnerUuid = "";
					ownerPickerVisible = false;
					notice = "已退出登录";
				});
			} else {
				openLogin();
			}
		});

		int ty = y + 108;
		g.text(this.font, Component.literal("传送请求自动应答"), l.sx + 20, ty, 0xFF16202E, false);
		String[][] modes = new String[][] {{"OFF", "关闭"}, {"ACCEPT", "自动同意"}, {"DENY", "自动拒绝"}};
		for (int i = 0; i < modes.length; i++) {
			int ry = ty + 20 + i * 28;
			boolean active = ModConfig.tpMode.name().equals(modes[i][0]);
			fillRounded(g, l.sx + 20, ry, l.sw - 40, 24, 9, active ? 0xB32F7CFF : 0x66FFFFFF);
			g.text(this.font, Component.literal(modes[i][1]), l.sx + 34, ry + 7, active ? 0xFFFFFFFF : 0xFF3A4556, false);
			final int modeIndex = i;
			hits.add(new Hit(l.sx + 20, ry, l.sw - 40, 24, () -> {
				playClick(1.12f);
				ModConfig.tpMode = ModConfig.TpMode.valueOf(modes[modeIndex][0]);
				ModConfig.save();
				notice = "传送应答：" + modes[modeIndex][1];
			}));
		}

		int cy = ty + 108;
		g.text(this.font, Component.literal("自定义传送指令"), l.sx + 20, cy, 0xFF16202E, false);
		g.text(this.font, Component.literal("自动同意时发送（不带 /）"), l.sx + 20, cy + 18, 0xFF8A96A8, false);
		renderEditBox(g, tpAcceptBox, l.sx + 20, cy + 32, l.sw - 40, 22, mouseX, mouseY);
		g.text(this.font, Component.literal("自动拒绝时发送"), l.sx + 20, cy + 62, 0xFF8A96A8, false);
		renderEditBox(g, tpDenyBox, l.sx + 20, cy + 76, l.sw - 40, 22, mouseX, mouseY);
		button(g, "保存指令", l.sx + 20, cy + 108, 90, 24, mouseX, mouseY, true, this::saveTpCommands);
		boolean hd = ModConfig.hdMode;
		int halfW = (l.sw - 48) / 2;
		fillRounded(g, l.sx + 20, cy + 140, halfW, 26, 9, hd ? 0xB32F7CFF : 0x66FFFFFF);
		g.text(this.font, Component.literal("高清模式"), l.sx + 30, cy + 149, hd ? 0xFFFFFFFF : 0xFF3A4556, false);
		hits.add(new Hit(l.sx + 20, cy + 140, halfW, 26, () -> {
			playClick(1.1f);
			ModConfig.hdMode = !ModConfig.hdMode;
			ModConfig.save();
			notice = ModConfig.hdMode ? "高清模式已开启" : "高清模式已关闭（液态玻璃模糊效果）";
		}));
		boolean sounds = ModConfig.uiSounds;
		fillRounded(g, l.sx + 28 + halfW, cy + 140, halfW, 26, 9, sounds ? 0xB32F7CFF : 0x66FFFFFF);
		g.text(this.font, Component.literal("界面音效"), l.sx + 38 + halfW, cy + 149, sounds ? 0xFFFFFFFF : 0xFF3A4556, false);
		hits.add(new Hit(l.sx + 28 + halfW, cy + 140, halfW, 26, () -> {
			ModConfig.uiSounds = !ModConfig.uiSounds;
			ModConfig.save();
			playClick(1.1f);
			notice = ModConfig.uiSounds ? "界面音效已开启" : "界面音效已关闭";
		}));
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 40)), l.sx + 20, cy + 174, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 40)), l.sx + 20, cy + 174, 0xFF1F9D63, false);
	}

	private void ensureRechargeBox() {
		if (rechargeBox == null) {
			rechargeBox = new EditBox(this.font, 0, 0, 120, 24, Component.literal("金额"));
			rechargeBox.setTextShadow(false);
			rechargeBox.setMaxLength(10);
			rechargeBox.setBordered(false);
			rechargeBox.setTextColor(0xFF1D2733);
			rechargeBox.setHint(Component.literal("输入充值金额，例如 50"));
		}
		if (prefixBox == null) {
			prefixBox = new EditBox(this.font, 0, 0, 120, 22, Component.literal("前缀"));
			prefixBox.setTextShadow(false);
			prefixBox.setMaxLength(32);
			prefixBox.setBordered(false);
			prefixBox.setTextColor(0xFF1D2733);
			prefixBox.setValue(ModConfig.botCommandPrefix);
		}
		if (templateBox == null) {
			templateBox = new EditBox(this.font, 0, 0, 200, 22, Component.literal("模板"));
			templateBox.setTextShadow(false);
			templateBox.setMaxLength(128);
			templateBox.setBordered(false);
			templateBox.setTextColor(0xFF1D2733);
			templateBox.setValue(ModConfig.rechargeCommandTemplate);
		}
	}

	private void drawRecharge(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "充值", mouseX, mouseY, () -> page = Page.HOME);
		ensureRechargeBox();
		int y = l.contentY + 40;
		g.text(this.font, Component.literal("zzko 余额 " + zzkoText), l.sx + 20, y, 0xFF2F7CFF, false);

		g.text(this.font, Component.literal("Bot 命令前缀（! 或 Bot 名字）"), l.sx + 20, y + 22, 0xFF8A96A8, false);
		renderEditBox(g, prefixBox, l.sx + 20, y + 34, 150, 22, mouseX, mouseY);
		g.text(this.font, Component.literal("充值命令模板"), l.sx + 20, y + 64, 0xFF8A96A8, false);
		renderEditBox(g, templateBox, l.sx + 20, y + 76, l.sw - 40, 22, mouseX, mouseY);
		g.text(this.font, Component.literal("{prefix}=前缀  {amount}=金额 例：{prefix}充值 {amount}"), l.sx + 20, y + 104, 0xFF8A96A8, false);
		button(g, "保存前缀设置", l.sx + 20, y + 118, 110, 22, mouseX, mouseY, false, this::saveRechargeSettings);

		g.text(this.font, Component.literal("充值金额"), l.sx + 20, y + 154, 0xFF8A96A8, false);
		renderEditBox(g, rechargeBox, l.sx + 20, y + 166, l.sw - 40, 26, mouseX, mouseY);
		int quickY = y + 202;
		String[] quick = {"10", "50", "100", "500"};
		for (int i = 0; i < quick.length; i++) {
			String amount = quick[i];
			button(g, amount, l.sx + 20 + i * 58, quickY, 52, 24, mouseX, mouseY, false, () -> rechargeBox.setValue(amount));
		}
		button(g, "充值", l.sx + 20, quickY + 34, l.sw - 40, 28, mouseX, mouseY, true, this::submitRecharge);
		g.text(this.font, Component.literal("示例：" + buildRechargeCommand("50")), l.sx + 20, quickY + 70, 0xFF54627A, false);
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 42)), l.sx + 20, quickY + 88, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 42)), l.sx + 20, quickY + 88, 0xFF1F9D63, false);
	}

	private void saveRechargeSettings() {
		ensureRechargeBox();
		String prefix = prefixBox.getValue() == null ? "" : prefixBox.getValue().trim();
		String template = templateBox.getValue() == null ? "" : templateBox.getValue().trim();
		if (prefix.isBlank()) { error = "前缀不能为空"; return; }
		if (template.isBlank() || !template.contains("{amount}")) { error = "模板必须包含 {amount}"; return; }
		ModConfig.botCommandPrefix = prefix;
		ModConfig.rechargeCommandTemplate = template;
		ModConfig.save();
		notice = "前缀/模板已保存";
		error = "";
	}

	private String buildRechargeCommand(String amount) {
		String prefix = ModConfig.botCommandPrefix == null || ModConfig.botCommandPrefix.isBlank() ? "!" : ModConfig.botCommandPrefix.trim();
		String template = ModConfig.rechargeCommandTemplate == null || ModConfig.rechargeCommandTemplate.isBlank() ? "{prefix}充值 {amount}" : ModConfig.rechargeCommandTemplate.trim();
		String spacer = prefix.matches(".*[A-Za-z0-9_]$") ? " " : "";
		if (template.contains("{prefix}")) {
			// {prefix}充值 {amount} → !充值 50；前缀是名字时自动补空格：MUKU 充值 50
			String rest = template.replace("{prefix}", "").replace("{amount}", amount).trim();
			return prefix + spacer + rest;
		}
		return prefix + spacer + template.replace("{amount}", amount).trim();
	}

	private void submitRecharge() {
		ensureRechargeBox();
		String raw = rechargeBox.getValue() == null ? "" : rechargeBox.getValue().trim();
		if (!raw.matches("^\\d+(?:\\.\\d{1,2})?$") || Double.parseDouble(raw) <= 0) {
			error = "请输入正确的充值金额（例如 50）";
			return;
		}
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc.getConnection() == null) {
				error = "当前不在服务器内";
				return;
			}
			String command = buildRechargeCommand(raw);
			mc.getConnection().sendChat(command);
			playLevelUp();
			notice = "已发送：" + command + "（请留意 Bot 回复）";
			error = "";
			rechargeBox.setValue("");
		} catch (Throwable throwable) {
			error = "发送充值请求失败：" + throwable.getMessage();
		}
	}

	private static final String[][] TUTORIAL_LINES = new String[][] {
		{"#", "机器人指令教程"},
		{"-", "/msg {bot} 设置密码 1234  设置网页登录密码"},
		{"-", "/msg {bot} 登录 123456  用临时验证码登录网页"},
		{"-", "/msg {bot} 存  把背包物品存入默认仓库"},
		{"-", "/msg {bot} 取 石头64  从默认仓库取出物品"},
		{"-", "/msg {bot} 取 石头5 玩家ID  代取并交给别人"},
		{"-", "/msg {bot} 查 石头  查询物品数量"},
		{"-", "/msg {bot} 设置默认仓库 仓库名  切换默认仓库"},
		{"-", "/msg {bot} 个人 存  强制存入个人仓库"},
		{"-", "/msg {bot} 个人 取 石头64  只从个人仓库取"},
		{"-", "/msg {bot} 我的仓库  查看已加入的组织仓库"},
		{"-", "/msg {bot} 额度  查看仓库格数占用"},
		{"-", "/msg {bot} 过来  管理员：让 Bot 传送到你身边"},
		{"-", "/msg {bot} 回家  管理员：让 Bot 回到仓库"},
		{"#", "云仓库使用教程"},
		{"-", "网页：打开 {cloud}，用密码或临时验证码登录"},
		{"-", "我的库存：选择仓库归属，可搜索、切换组织仓库"},
		{"-", "选中物品：左键+1，Shift+左键+64，Alt+左键+1728，右键-1728"},
		{"-", "选好后点「取出选中」，Bot 会把物品送到目标玩家身边"},
		{"-", "送货给：输入玩家 ID，支持 TAB 补全和点击标签"},
		{"-", "发起存物品：Bot 会到你身边收集丢出的物品并存桶"},
		{"-", "背包满时 Bot 会提前结束收集，避免物品消失"},
		{"-", "订阅与钱包：查看 zzko 余额、购买仓库订阅"},
		{"-", "物品别名：在「物品别名」里给常用物品起短名"},
		{"-", "高清模式：设置里可切换锐化/液态玻璃模糊效果"},
		{"-", "传送自动应答：设置里可开启自动同意或拒绝并自定义指令"},
		{"#", "更多"},
		{"-", "网页「帮助」页有完整指令速查"},
		{"-", "遇到库存异常，管理员可在网页后台用「库存异常/重读木桶」处理"}
	};

		private void drawTutorial(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "使用教程", mouseX, mouseY, () -> page = Page.HOME);
		int lineH = 14;
		int top = l.contentY + 40;
		int visible = Math.max(1, (l.contentH - 54) / lineH);
		tutorialMaxScroll = Math.max(0, TUTORIAL_LINES.length - visible);
		if (scroll > tutorialMaxScroll) scroll = tutorialMaxScroll;
		int maxWidth = l.sw - 40;
		for (int i = 0; i < visible && scroll + i < TUTORIAL_LINES.length; i++) {
			String[] line = TUTORIAL_LINES[scroll + i];
			int y = top + i * lineH;
			if ("#".equals(line[0])) {
				g.text(this.font, Component.literal(line[1]), l.sx + 20, y + 2, 0xFF16202E, false);
			} else {
				String content = line[1];
				while (this.font.width(content) > maxWidth && content.length() > 2) content = content.substring(0, content.length() - 1);
				g.text(this.font, Component.literal("· " + content), l.sx + 24, y, 0xFF54627A, false);
			}
		}
		if (tutorialMaxScroll > 0) {
			g.text(this.font, Component.literal("滚轮上下查看 · " + (scroll + 1) + "/" + (tutorialMaxScroll + 1)), l.sx + 20, l.contentY + l.contentH - 12, 0xFF8A96A8, false);
		}
	}

	private void openGallery() {
		galleryFiles.clear();
		galleryIndex = 0;
		galleryError = "";
		notice = "";
		error = "";
		galleryDeleteArmedAt = 0L;
		try {
			Minecraft mc = Minecraft.getInstance();
			File dir = new File(mc.gameDirectory, "screenshots");
			File[] files = dir.listFiles(file -> file.isFile() && file.getName().toLowerCase().endsWith(".png"));
			if (files != null) {
				java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified).reversed());
				galleryFiles.addAll(java.util.Arrays.asList(files));
			}
		} catch (Throwable throwable) {
			galleryError = "读取截图失败：" + throwable.getMessage();
		}
		galleryLoaded = true;
		if (!galleryFiles.isEmpty()) loadGalleryTexture();
	}

	private void releaseGalleryTexture() {
		try {
			if (galleryTexture != null) {
				Minecraft.getInstance().getTextureManager().release(galleryTexture);
			}
		} catch (Throwable ignored) {
		}
		galleryTexture = null;
		galleryTexW = 0;
		galleryTexH = 0;
	}

	private void loadGalleryTexture() {
		releaseGalleryTexture();
		if (galleryFiles.isEmpty() || galleryIndex < 0 || galleryIndex >= galleryFiles.size()) return;
		File file = galleryFiles.get(galleryIndex);
		try (InputStream input = new FileInputStream(file)) {
			NativeImage image = NativeImage.read(input);
			DynamicTexture texture = new DynamicTexture(() -> "zzyokphone_gallery", image);
			Identifier id = Identifier.fromNamespaceAndPath("zzyokphone", "gallery/" + (galleryTextureCounter++));
			Minecraft.getInstance().getTextureManager().register(id, texture);
			galleryTexture = id;
			galleryTexW = image.getWidth();
			galleryTexH = image.getHeight();
			galleryError = "";
		} catch (Throwable throwable) {
			galleryError = "加载图片失败：" + throwable.getMessage();
		}
	}

	private void drawGallery(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		if (galleryZoom) {
			drawZoomedGallery(g, l);
			return;
		}
		header(g, l, "相册", mouseX, mouseY, () -> {
			releaseGalleryTexture();
			page = Page.HOME;
		});
		if (!galleryLoaded) openGallery();
		button(g, "刷新", l.sx + l.sw - 62, l.contentY + 2, 42, 20, mouseX, mouseY, false, () -> {
			playClick(1.1f);
			openGallery();
		});

		int areaX = l.sx + 16;
		int areaY = l.contentY + 34;
		int areaW = l.sw - 32;
		int areaH = l.contentH - 116;
		fillRounded(g, areaX, areaY, areaW, areaH, 14, 0x66FFFFFF);

		if (galleryFiles.isEmpty()) {
			drawCentered(g, Component.literal("截图文件夹为空"), l.sx + l.sw / 2, areaY + areaH / 2 - 10, 0xFF54627A);
			drawCentered(g, Component.literal(".minecraft/screenshots 里放入 png 截图"), l.sx + l.sw / 2, areaY + areaH / 2 + 8, 0xFF8A96A8);
			if (!galleryError.isBlank()) drawCentered(g, Component.literal(trim(galleryError, 34)), l.sx + l.sw / 2, areaY + areaH / 2 + 26, 0xFFE23D3D);
			return;
		}

		if (galleryTexture != null && galleryTexW > 0 && galleryTexH > 0) {
			float scale = Math.min(areaW / (float) galleryTexW, areaH / (float) galleryTexH);
			int dw = Math.max(1, Math.round(galleryTexW * scale));
			int dh = Math.max(1, Math.round(galleryTexH * scale));
			int dx = areaX + (areaW - dw) / 2;
			int dy = areaY + (areaH - dh) / 2;
			g.blit(galleryTexture, dx, dy, dx + dw, dy + dh, 0f, 1f, 0f, 1f);
			hits.add(new Hit(dx, dy, dw, dh, this::onGalleryImageClick));
		}

		String info = "第 " + (galleryIndex + 1) + " / " + galleryFiles.size() + " 张 · 滚轮切换 · 双击图片放大";
		drawCentered(g, Component.literal(info), l.sx + l.sw / 2, areaY + areaH + 8, 0xFF54627A);
		int dots = Math.min(galleryFiles.size(), 12);
		if (dots > 1) {
			int dotsY = areaY + areaH + 22;
			for (int i = 0; i < dots; i++) {
				boolean active = galleryFiles.size() <= 12 && i == galleryIndex;
				fillRounded(g, l.sx + l.sw / 2 - dots * 5 + i * 10, dotsY, 6, 6, 3, active ? 0xCC1D2733 : 0x331D2733);
				int target = i;
				hits.add(new Hit(l.sx + l.sw / 2 - dots * 5 + i * 10 - 2, dotsY - 2, 10, 10, () -> {
					galleryIndex = Math.min(target, galleryFiles.size() - 1);
					galleryZoom = false;
					loadGalleryTexture();
					playClick(1.2f);
				}));
			}
		}
		int gap = 6;
		int btnW = (l.sw - 40 - gap * 2) / 3;
		int rowY = l.contentY + l.contentH - 30;
		button(g, "上一张", l.sx + 20, rowY, btnW, 24, mouseX, mouseY, false, () -> {
			if (galleryFiles.isEmpty()) return;
			galleryIndex = (galleryIndex - 1 + galleryFiles.size()) % galleryFiles.size();
			galleryZoom = false;
			loadGalleryTexture();
		});
		button(g, "下一张", l.sx + 20 + btnW + gap, rowY, btnW, 24, mouseX, mouseY, false, () -> {
			if (galleryFiles.isEmpty()) return;
			galleryIndex = (galleryIndex + 1) % galleryFiles.size();
			galleryZoom = false;
			loadGalleryTexture();
		});
		boolean armed = System.currentTimeMillis() - galleryDeleteArmedAt <= 3000L;
		int delX = l.sx + 20 + (btnW + gap) * 2;
		fillRounded(g, delX, rowY, btnW, 24, 9, armed ? 0xE6E05656 : 0xCCFFFFFF);
		drawCentered(g, Component.literal(armed ? "确认删除" : "删除"), delX + btnW / 2, rowY + 8, armed ? 0xFFFFFFFF : 0xFFE05656);
		hits.add(new Hit(delX, rowY, btnW, 24, this::onGalleryDeleteClick));
		g.text(this.font, Component.literal(trim(galleryFiles.get(Math.min(galleryIndex, galleryFiles.size() - 1)).getName(), 28)), l.sx + 20, l.contentY + l.contentH - 48, 0xFF8A96A8, false);
		if (!error.isBlank()) g.text(this.font, Component.literal(trim(error, 36)), l.sx + 20, l.contentY + l.contentH - 64, 0xFFE23D3D, false);
		else if (!notice.isBlank()) g.text(this.font, Component.literal(trim(notice, 36)), l.sx + 20, l.contentY + l.contentH - 64, 0xFF1F9D63, false);
	}

	private void drawZoomedGallery(GuiGraphicsExtractor g, Layout l) {
		g.fill(0, 0, this.width, this.height, 0xC8000000);
		int areaW = Math.max(160, Math.round(this.width * 0.68f));
		int areaH = Math.max(120, Math.round(this.height * 0.72f));
		int areaX = (this.width - areaW) / 2;
		int areaY = (this.height - areaH) / 2;
		fillRounded(g, areaX, areaY, areaW, areaH, 16, 0xEE10141F);
		if (galleryTexture != null && galleryTexW > 0 && galleryTexH > 0) {
			float scale = Math.min((areaW - 24f) / galleryTexW, (areaH - 24f) / galleryTexH);
			int dw = Math.max(1, Math.round(galleryTexW * scale));
			int dh = Math.max(1, Math.round(galleryTexH * scale));
			int dx = areaX + (areaW - dw) / 2;
			int dy = areaY + (areaH - dh) / 2;
			g.blit(galleryTexture, dx, dy, dx + dw, dy + dh, 0f, 1f, 0f, 1f);
		}
		fillRounded(g, areaX + 12, areaY + 12, 132, 26, 9, 0xCC000000);
		drawCentered(g, Component.literal("双击退出放大"), areaX + 12 + 66, areaY + 21, 0xFFFFFFFF);
		hits.add(new Hit(areaX + 12, areaY + 12, 132, 26, () -> {
			galleryZoom = false;
			playClick(1.1f);
		}));
		String info = "第 " + (galleryIndex + 1) + " / " + galleryFiles.size() + " 张 · 滚轮切换 · 双击图片退出";
		int infoWidth = Math.min(areaW - 24, 300);
		fillRounded(g, this.width / 2 - infoWidth / 2, areaY + areaH - 40, infoWidth, 24, 9, 0xCC000000);
		drawCentered(g, Component.literal(info), this.width / 2, areaY + areaH - 33, 0xFFE8ECF7);
		hits.add(new Hit(0, 0, this.width, this.height, this::onGalleryImageClick));
	}

	private void onGalleryDeleteClick() {
		if (galleryFiles.isEmpty()) return;
		long now = System.currentTimeMillis();
		if (now - galleryDeleteArmedAt > 3000L) {
			galleryDeleteArmedAt = now;
			notice = "再点一次「确认删除」删除当前截图";
			error = "";
			playClick(1.1f);
			return;
		}
		galleryDeleteArmedAt = 0L;
		File file = galleryFiles.get(Math.min(galleryIndex, galleryFiles.size() - 1));
		try {
			releaseGalleryTexture();
			if (!file.delete() && file.exists()) {
				error = "删除失败：" + file.getName();
				return;
			}
			galleryFiles.remove(file);
			if (galleryIndex >= galleryFiles.size()) galleryIndex = Math.max(0, galleryFiles.size() - 1);
			galleryZoom = false;
			notice = "已删除：" + file.getName();
			error = "";
			playClick(1.2f);
			if (!galleryFiles.isEmpty()) loadGalleryTexture();
			else galleryError = "";
		} catch (Throwable throwable) {
			error = "删除失败：" + throwable.getMessage();
		}
	}

	private void onGalleryImageClick() {
		long now = System.currentTimeMillis();
		if (galleryLastClickIndex == galleryIndex && now - galleryLastClickAt <= 350L) {
			galleryZoom = !galleryZoom;
			galleryLastClickAt = 0L;
			galleryLastClickIndex = -1;
			playClick(galleryZoom ? 1.4f : 1.1f);
			return;
		}
		galleryLastClickAt = now;
		galleryLastClickIndex = galleryIndex;
	}

	private void drawAbout(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
		header(g, l, "关于", mouseX, mouseY, () -> page = Page.HOME);
		int cardX = l.sx + 18;
		int cardW = l.sw - 36;
		int cardX2 = cardX + cardW;
		int y = l.contentY + 36;

		// 信息卡（与下方致敬卡同宽居中）
		int infoH = 136;
		fillRounded(g, cardX, y, cardW, infoH, 16, 0x66FFFFFF);
		int tx = cardX + 16;
		int ty = y + 12;
		g.text(this.font, Component.literal("ZZYOK Phone v" + MOD_VERSION), tx, ty, 0xFF16202E, true);
		g.text(this.font, Component.literal("云仓库 & Bot 自托管平台 · 手机界面"), tx, ty + 18, 0xFF54627A, false);
		g.text(this.font, Component.literal("右键不死图腾打开 / 关闭"), tx, ty + 40, 0xFF8A96A8, false);
		g.text(this.font, Component.literal("ESC：子页面返回桌面，桌面 ESC 退出"), tx, ty + 54, 0xFF8A96A8, false);
		g.text(this.font, Component.literal("云仓库服务：" + cloudHost()), tx, ty + 74, 0xFF8A96A8, false);
		g.text(this.font, Component.literal("Bot 平台：" + platformHost()), tx, ty + 88, 0xFF8A96A8, false);
		g.text(this.font, Component.literal("Mod 制作：K1nse_"), tx, ty + 110, 0xFF2F7CFF, true);

		// 致敬原作者卡
		int creditY = y + infoH + 14;
		int creditH = 96;
		fillRounded(g, cardX, creditY, cardW, creditH, 16, 0x66FFFFFF);
		g.text(this.font, Component.literal("特别致敬 · 原作者"), tx, creditY + 12, 0xFF16202E, true);
		g.text(this.font, Component.literal("Damomo1"), tx, creditY + 30, 0xFF2F7CFF, true);
		g.text(this.font, Component.literal("感谢原作者设计并实现了云仓库与 Bot 自托管平台"), tx, creditY + 48, 0xFF54627A, false);
		g.text(this.font, Component.literal("本 Mod 基于其项目进行二次开发与移植"), tx, creditY + 64, 0xFF8A96A8, false);
	}

	/* ==================== 通用组件 ==================== */

	private void header(GuiGraphicsExtractor g, Layout l, String title, int mouseX, int mouseY, Runnable onBack) {
		button(g, "<", l.sx + 18, l.contentY + 2, 30, 24, mouseX, mouseY, false, onBack);
		g.text(this.font, Component.literal(title), l.sx + 56, l.contentY + 8, 0xFF16202E, false);
	}

	private void drawCenteredFitted(GuiGraphicsExtractor g, Component text, int centerX, int y, int color, int maxWidth) {
		int width = this.font.width(text);
		if (width <= maxWidth) {
			drawCentered(g, text, centerX, y, color);
			return;
		}
		float scale = Math.max(0.5f, maxWidth / (float) width);
		Matrix3x2fStack matrices = g.pose();
		matrices.pushMatrix();
		matrices.translate(centerX - width * scale / 2f, y);
		matrices.scale(scale, scale);
		g.text(this.font, text, 0, 0, color, false);
		matrices.popMatrix();
	}

	private void drawCentered(GuiGraphicsExtractor g, Component text, int centerX, int y, int color) {
		g.text(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
	}

	private void button(GuiGraphicsExtractor g, String label, int x, int y, int w, int h, int mouseX, int mouseY, boolean primary, Runnable onClick) {
		boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
		int color = primary ? (hover ? 0xE62F7CFF : 0xCC2F7CFF) : (hover ? 0xB3FFFFFF : 0x80FFFFFF);
		fillRounded(g, x, y, w, h, Math.min(12, h / 2), color);
		int textColor = primary ? 0xFFFFFFFF : 0xFF2A3446;
		drawCentered(g, Component.literal(label), x + w / 2, y + (h - 8) / 2, textColor);
		hits.add(new Hit(x, y, w, h, () -> {
			playClick(1.05f);
			onClick.run();
		}));
	}

	private void drawTextField(GuiGraphicsExtractor g, int x, int y, int w, int h, String value, boolean focused, String placeholder, int mouseX, int mouseY) {
		fillRounded(g, x, y, w, h, 8, focused ? 0xCCFFFFFF : 0x66FFFFFF);
		if (focused) fillRounded(g, x, y, w, h, 8, 0x00000000);
		String shown = value.isBlank() ? placeholder : value;
		int color = value.isBlank() ? 0xFF9AA6B8 : 0xFF1D2733;
		String clipped = shown;
		int maxWidth = w - 12;
		while (this.font.width(clipped) > maxWidth && clipped.length() > 1) clipped = clipped.substring(1);
		g.text(this.font, Component.literal(clipped), x + 6, y + (h - 8) / 2, color, false);
		if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
			int cx = x + 6 + this.font.width(value.isBlank() ? "" : clipped);
			g.fill(cx, y + 4, cx + 1, y + h - 4, 0xFF2F7CFF);
		}
	}

	private void drawAppIcon(GuiGraphicsExtractor g, String itemId, int accent, int x, int y, int size, boolean hover) {
		int radius = Math.max(15, size / 4);
		if (itemId != null && itemId.startsWith("texture:")) {
			Identifier texture = appIconTexture(itemId.substring(8));
			if (texture != null) {
				// 自定义图标：不要再铺强调色背景，否则圆角边缘会透出一圈颜色
				fillRounded(g, x - 2, y - 2, size + 4, size + 4, radius + 2, hover ? 0x66FFFFFF : 0x33FFFFFF);
				fillRounded(g, x, y, size, size, radius, 0xFFFFFFFF);
				try {
					g.blit(texture, x, y, x + size, y + size, 0f, 1f, 0f, 1f);
				} catch (Throwable ignored) {
				}
				return;
			}
		}
		fillRounded(g, x - 1, y - 1, size + 2, size + 2, radius + 1, hover ? 0xCCFFFFFF : 0x99FFFFFF);
		int top = lighten(accent, 0.6f);
		fillRoundedGradient(g, x, y, size, size, radius, top, accent);
		fillGlass(g, x, y, size, size, radius, hover ? 0x33FFFFFF : 0x14FFFFFF);
		int iconScale = Math.max(1, Math.round(size / 20f));
		int iconOffset = Math.round((size - 16f * iconScale) / 2f);
		drawScaledItem(g, stackFor(itemId), x + iconOffset, y + iconOffset, iconScale);
	}

	private Identifier appIconTexture(String key) {
		Identifier cached = appIconTextures.get(key);
		if (cached != null) return cached;
		if (!appIconTextureLoading.add(key)) return null;
		try {
			String namespace = "zzyokphone";
			String path = "textures/gui/" + key + ".png";
			if (key.contains(":")) {
				String[] parts = key.split(":", 2);
				namespace = parts[0];
				path = parts[1];
			}
			Identifier location = Identifier.fromNamespaceAndPath(namespace, path);
			var resource = Minecraft.getInstance().getResourceManager().getResource(location);
			if (resource.isEmpty()) return null;
			try (InputStream input = resource.get().open()) {
				NativeImage image = NativeImage.read(input);
				DynamicTexture texture = new DynamicTexture(() -> "zzyokphone_app_icon_" + key, image);
				Identifier id = Identifier.fromNamespaceAndPath("zzyokphone", "app_icon/" + key.replace(":", "/"));
				Minecraft.getInstance().getTextureManager().register(id, texture);
				appIconTextures.put(key, id);
				return id;
			}
		} catch (Throwable ignored) {
			return null;
		}
	}

	private double frameSmoothing() {
		return 1D - Math.exp(-22D * renderDelta);
	}

	private void drawScaledItem(GuiGraphicsExtractor g, ItemStack stack, float x, float y, float scale) {
		if (stack.isEmpty()) return;
		Matrix3x2fStack matrices = g.pose();
		matrices.pushMatrix();
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		g.item(stack, 0, 0);
		matrices.popMatrix();
	}

	private void fillGlass(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
		fillRounded(g, x, y, w, h, radius, color);
		fillRounded(g, x + 1, y + 1, w - 2, Math.max(1, h / 3), Math.max(1, radius - 2), 0x1AFFFFFF);
		fillRounded(g, x, y, w, 1, 0, 0x40FFFFFF);
	}

	private void fillRoundedGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int top, int bottom) {
		if (w <= 0 || h <= 0) return;
		int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		for (int row = 0; row < h; row++) {
			int inset = inset(r, h, row);
			float t = h <= 1 ? 0f : row / (float) (h - 1);
			g.fill(x + inset, y + row, x + w - inset, y + row + 1, lerpColor(top, bottom, t));
		}
	}

	private void fillRounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
		if (w <= 0 || h <= 0) return;
		int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		if (r == 0) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		for (int row = 0; row < h; row++) {
			int inset = inset(r, h, row);
			g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
		}
	}

	private static int inset(int r, int h, int row) {
		if (r <= 0) return 0;
		if (row < r) {
			int dy = r - row;
			return r - (int) Math.sqrt(Math.max(0, r * r - dy * dy));
		}
		if (row >= h - r) {
			int dy = row - (h - r) + 1;
			return r - (int) Math.sqrt(Math.max(0, r * r - dy * dy));
		}
		return 0;
	}

	private void glow(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
		fillRounded(g, x, y, w, h, Math.min(w, h) / 2, color);
	}

	private static int lerpColor(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
		int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
		return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
			| (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
	}

	private static int lighten(int color, float amount) {
		return lerpColor(color, 0xFFFFFFFF, amount);
	}

	private static ItemStack stackFor(String itemId) {
		try {
			if (itemId == null || itemId.isBlank()) return ItemStack.EMPTY;
			Identifier id = itemId.contains(":") ? Identifier.tryParse(itemId) : Identifier.withDefaultNamespace(itemId);
			if (id == null) return ItemStack.EMPTY;
			Item item = BuiltInRegistries.ITEM.getValue(id);
			return item == null ? ItemStack.EMPTY : new ItemStack(item);
		} catch (Throwable ignored) {
			return ItemStack.EMPTY;
		}
	}

	private static String getString(JsonObject object, String key) {
		try {
			if (object.has(key) && !object.get(key).isJsonNull()) return object.get(key).getAsString();
		} catch (Throwable ignored) {
		}
		return "";
	}

	private static int getInt(JsonObject object, String key) {
		try {
			if (object.has(key) && !object.get(key).isJsonNull()) return object.get(key).getAsInt();
		} catch (Throwable ignored) {
		}
		return 0;
	}

	private static String trim(String text, int max) {
		if (text == null) return "";
		String value = text.replace('\n', ' ');
		return value.length() <= max ? value : value.substring(0, max) + "…";
	}

	/* ==================== 页面跳转 ==================== */

	private void openPage(Page target) {
		if (page == Page.WAREHOUSE && target != Page.WAREHOUSE) selected.clear();
		if (target != Page.GALLERY) releaseGalleryTexture();
		if ((target == Page.WAREHOUSE || target == Page.BOT) && !Api.loggedIn) {
			pendingPage = target;
			openLogin();
			return;
		}
		page = target;
		error = "";
		notice = "";
		if (target == Page.WAREHOUSE) loadWarehouse();
		if (target == Page.BOT) loadBots();
		if (target == Page.SUBSCRIPTION) loadSubscriptions();
		if (target == Page.GALLERY) openGallery();
	}

	private EditBox ensurePasswordBox() {
		if (passwordBox == null) {
			passwordBox = new EditBox(this.font, 0, 0, 120, 24, Component.literal("密码"));
			passwordBox.setTextShadow(false);
			passwordBox.setMaxLength(64);
			passwordBox.setBordered(false);
			passwordBox.setTextColor(0xFF1D2733);
			passwordBox.setHint(Component.literal("网页登录密码"));
		}
		return passwordBox;
	}

	private EditBox ensureTargetBox() {
		if (targetBox == null) {
			targetBox = new EditBox(this.font, 0, 0, 120, 22, Component.literal("玩家"));
			targetBox.setTextShadow(false);
			targetBox.setMaxLength(16);
			targetBox.setBordered(false);
			targetBox.setTextColor(0xFF1D2733);
			targetBox.setHint(Component.literal("玩家ID（TAB补全）"));
			targetBox.setValue(Api.username == null ? "" : Api.username);
		}
		return targetBox;
	}

	private void unfocusWidgets() {
		if (passwordBox != null) passwordBox.setFocused(false);
		if (targetBox != null) targetBox.setFocused(false);
		if (taskNameBox != null) taskNameBox.setFocused(false);
		if (taskIntervalBox != null) taskIntervalBox.setFocused(false);
		if (taskStepsBox != null) taskStepsBox.setFocused(false);
		if (searchBox != null) searchBox.setFocused(false);
		if (tpAcceptBox != null) tpAcceptBox.setFocused(false);
		if (tpDenyBox != null) tpDenyBox.setFocused(false);
		focusedBox = null;
	}

	/* ==================== 输入处理 ==================== */

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		clickButton = event.button();
		clickShift = event.hasShiftDown();
		clickAlt = event.hasAltDown();
		double mouseX = (event.x() - this.uiOffsetX) / this.uiScale;
		double mouseY = (event.y() - this.uiOffsetY) / this.uiScale;
		event = new MouseButtonEvent(mouseX, mouseY, event.buttonInfo());
		Layout layout = layout();
		if (ownerPickerVisible) {
			for (Hit hit : pickerHits) {
				if (hit.contains(mouseX, mouseY)) {
					hit.action().run();
					return true;
				}
			}
			ownerPickerVisible = false;
			return true;
		}
		if (commandPickerRow >= 0) {
			for (Hit hit : pickerHits) {
				if (hit.contains(mouseX, mouseY)) {
					hit.action().run();
					return true;
				}
			}
			commandPickerRow = -1;
			return true;
		}
		for (BoxHit boxHit : boxHits) {
			if (mouseX >= boxHit.x && mouseX <= boxHit.x + boxHit.w && mouseY >= boxHit.y && mouseY <= boxHit.y + boxHit.h) {
				unfocusWidgets();
				boxHit.box.setFocused(true);
				focusedBox = boxHit.box;
				boxHit.box.mouseClicked(event, doubleClick);
				return true;
			}
		}
		if (page == Page.LOGIN && passwordBox != null) {
			int bx = layout.sx + 34;
			int by = layout.contentY + 30 + 124;
			if (mouseX >= bx && mouseX <= bx + layout.sw - 68 && mouseY >= by && mouseY <= by + 26) {
				unfocusWidgets();
				passwordBox.setFocused(true);
				passwordBox.mouseClicked(event, doubleClick);
				return true;
			}
		}
		for (Hit hit : hits) {
			if (hit.contains(mouseX, mouseY)) {
				unfocusWidgets();
				hit.action().run();
				return true;
			}
		}
		unfocusWidgets();
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (page == Page.HOME) {
			if (homePageCount > 1) {
				homePage = Math.max(0, Math.min(homePageCount - 1, homePage - (int) Math.signum(verticalAmount)));
				playClick(1.15f);
			}
			return true;
		}
		if (page == Page.WAREHOUSE) {
			double step = warehouseRowH > 0 ? Math.max(14D, warehouseRowH * 0.9D) : 40D;
			warehouseScrollTarget = Math.max(0D, Math.min(warehouseMaxScroll, warehouseScrollTarget - Math.signum(verticalAmount) * step));
			return true;
		}
		if (page == Page.BOTLOG) {
			scroll = (int) Math.max(0, scroll - Math.signum(verticalAmount));
			return true;
		}
		if (page == Page.GALLERY) {
			if (galleryFiles.isEmpty()) return true;
			int direction = verticalAmount > 0 ? -1 : 1;
			galleryIndex = (galleryIndex + direction + galleryFiles.size()) % galleryFiles.size();
			loadGalleryTexture();
			playClick(1.3f);
			return true;
		}
		if (page == Page.TUTORIAL) {
			scroll = (int) Math.max(0, Math.min(tutorialMaxScroll, scroll - Math.signum(verticalAmount)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == KEY_ESC) {
			if (ownerPickerVisible) {
				ownerPickerVisible = false;
				return true;
			}
			if (commandPickerRow >= 0) {
				commandPickerRow = -1;
				return true;
			}
			if (page == Page.WAREHOUSE) {
				selected.clear();
				page = Page.HOME;
				return true;
			}
			if (page == Page.HOME) return super.keyPressed(event);
			if (page == Page.BOTLOG || page == Page.BOTSETTINGS) {
				page = Page.BOT;
				return true;
			}
			if (page == Page.TASKEDIT) {
				page = Page.BOTSETTINGS;
				return true;
			}
			if (page == Page.BOTPERMS) {
				page = Page.BOTSETTINGS;
				return true;
			}
			if (page == Page.TRIGGER) {
				page = Page.SETTINGS;
				return true;
			}
			if (page == Page.SERVERCFG) {
				page = Page.SETTINGS;
				return true;
			}
			if (page == Page.GALLERY) {
				if (galleryZoom) {
					galleryZoom = false;
					return true;
				}
				releaseGalleryTexture();
				page = Page.HOME;
				return true;
			}
			page = Page.HOME;
			return true;
		}
		if (page == Page.REPAIR && key == KEY_ENTER) {
			doRepair();
			return true;
		}
		if ((page == Page.TASKEDIT || page == Page.SETTINGS || page == Page.WAREHOUSE || page == Page.BOTPERMS || page == Page.RECHARGE || page == Page.TRIGGER || page == Page.SERVERCFG || page == Page.REPAIR) && focusedBox != null) {
			if (focusedBox.keyPressed(event)) return true;
		}
		if (page == Page.TASKEDIT && key == KEY_ENTER) {
			saveTask();
			return true;
		}
		if (page == Page.LOGIN) {
			if (key == KEY_ENTER && passwordBox != null && passwordBox.isFocused()) {
				doLogin();
				return true;
			}
			if (passwordBox != null && passwordBox.isFocused() && passwordBox.keyPressed(event)) return true;
		}
		if (page == Page.WAREHOUSE && targetBox != null) {
			if (key == KEY_TAB && targetBox.isFocused()) {
				completeTarget(targetBox);
				return true;
			}
			if (targetBox.isFocused() && targetBox.keyPressed(event)) return true;
		}
		if (page == Page.REPAIR && repairTargetBox != null) {
			if (key == KEY_TAB && repairTargetBox.isFocused()) {
				completeTarget(repairTargetBox);
				return true;
			}
			if (repairTargetBox.isFocused() && repairTargetBox.keyPressed(event)) return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		int codepoint = event.codepoint();
		if (codepoint < 32 || codepoint == 127) return false;
		if (focusedBox != null && (page == Page.TASKEDIT || page == Page.SETTINGS || page == Page.WAREHOUSE || page == Page.BOTPERMS || page == Page.RECHARGE || page == Page.TRIGGER || page == Page.SERVERCFG || page == Page.REPAIR)) {
			return focusedBox.charTyped(event);
		}
		if (page == Page.LOGIN && passwordBox != null && passwordBox.isFocused()) {
			return passwordBox.charTyped(event);
		}
		if (page == Page.WAREHOUSE && targetBox != null && targetBox.isFocused()) {
			return targetBox.charTyped(event);
		}
		return super.charTyped(event);
	}

	private static String chop(String value) {
		if (value == null || value.isEmpty()) return "";
		int end = value.offsetByCodePoints(value.length(), -1);
		return value.substring(0, end);
	}

	private void completeTarget() {
		completeTarget(targetBox);
	}

	private void completeTarget(EditBox box) {
		try {
			if (box == null) return;
			String value = box.getValue();
			if (value == null || value.isBlank()) return;
			Minecraft mc = Minecraft.getInstance();
			if (mc.getConnection() == null) return;

			// 用户在手动输入（当前值不是上次 TAB 选中的结果）→ 重新计算候选
			if (tabMatches.isEmpty() || !value.equals(lastTabPicked)) {
				tabPrefix = value;
				tabMatches.clear();
				tabIndex = 0;
				String prefix = value.toLowerCase();
				for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
					if (info == null || info.getProfile() == null) continue;
					String name = info.getProfile().name();
					if (name != null && !name.isBlank() && name.toLowerCase().startsWith(prefix)) tabMatches.add(name);
				}
				tabMatches.sort(String.CASE_INSENSITIVE_ORDER);
				if (tabMatches.isEmpty()) return;
				// 多个候选先补公共前缀
				if (tabMatches.size() > 1) {
					String common = tabMatches.get(0);
					for (int i = 1; i < tabMatches.size(); i++) common = commonPrefix(common, tabMatches.get(i));
					if (common.length() > value.length()) {
						box.setValue(common);
						box.setCursorPosition(common.length());
						lastTabPicked = common;
						return;
					}
				}
			}
			if (tabMatches.isEmpty()) return;
			String picked = tabMatches.get(tabIndex % tabMatches.size());
			tabIndex++;
			box.setValue(picked);
			box.setCursorPosition(picked.length());
			lastTabPicked = picked;
			playClick(1.3f);
		} catch (Throwable ignored) {
		}
	}

	private static String commonPrefix(String a, String b) {
		int max = Math.min(a.length(), b.length());
		int i = 0;
		while (i < max && Character.toLowerCase(a.charAt(i)) == Character.toLowerCase(b.charAt(i))) i++;
		return a.substring(0, i);
	}

	/* ==================== 异步 ==================== */

	private interface ThrowingSupplier {
		String get() throws Exception;
	}

	private void doAsync(ThrowingSupplier action, Runnable onSuccess) {
		if (busy) return;
		busy = true;
		error = "";
		Thread thread = new Thread(() -> {
			String failure = null;
			try {
				failure = action.get();
			} catch (Throwable throwable) {
				failure = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
			}
			String finalFailure = failure;
			Minecraft.getInstance().execute(() -> {
				busy = false;
				if (finalFailure != null && !finalFailure.isBlank()) {
					error = finalFailure;
				} else {
					error = "";
					if (onSuccess != null) onSuccess.run();
					if (!Api.lastError.isBlank()) {
						error = Api.lastError;
						Api.lastError = "";
					}
				}
			});
		}, "zzyokphone-api");
		thread.setDaemon(true);
		thread.start();
	}
}
