package cn.zzyok.zzyokphone.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把服务器插件返回的英文附魔/附魔书名称翻译成中文。
 * 例如 "Sharpness V" -> "锋利 V"、"Enchanted Book (Mending)" -> "附魔书 (经验修补)"。
 */
public final class EnchantTranslator {
	private static final Map<String, String> NAMES = new LinkedHashMap<>();
	private static final List<String> KEYS;
	private static final Pattern FORMATTING = Pattern.compile("\u00a7[0-9A-Fa-fK-Ok-oRr]");

	static {
		// 附魔书 / 基名
		NAMES.put("enchanted book", "附魔书");
		NAMES.put("enchanted_book", "附魔书");
		// 盔甲附魔
		NAMES.put("fire protection", "火焰保护");
		NAMES.put("fire_protection", "火焰保护");
		NAMES.put("blast protection", "爆炸保护");
		NAMES.put("blast_protection", "爆炸保护");
		NAMES.put("projectile protection", "弹射物保护");
		NAMES.put("projectile_protection", "弹射物保护");
		NAMES.put("feather falling", "摔落保护");
		NAMES.put("feather_falling", "摔落保护");
		NAMES.put("protection", "保护");
		NAMES.put("respiration", "水下呼吸");
		NAMES.put("aqua affinity", "水下速掘");
		NAMES.put("aqua_affinity", "水下速掘");
		NAMES.put("thorns", "荆棘");
		NAMES.put("depth strider", "深海探索者");
		NAMES.put("depth_strider", "深海探索者");
		NAMES.put("frost walker", "冰霜行者");
		NAMES.put("frost_walker", "冰霜行者");
		NAMES.put("soul speed", "灵魂疾行");
		NAMES.put("soul_speed", "灵魂疾行");
		NAMES.put("swift sneak", "迅捷潜行");
		NAMES.put("swift_sneak", "迅捷潜行");
		// 武器附魔
		NAMES.put("sharpness", "锋利");
		NAMES.put("smite", "亡灵杀手");
		NAMES.put("bane of arthropods", "节肢杀手");
		NAMES.put("bane_of_arthropods", "节肢杀手");
		NAMES.put("knockback", "击退");
		NAMES.put("fire aspect", "火焰附加");
		NAMES.put("fire_aspect", "火焰附加");
		NAMES.put("looting", "抢夺");
		NAMES.put("sweeping edge", "横扫之刃");
		NAMES.put("sweeping_edge", "横扫之刃");
		// 工具附魔
		NAMES.put("efficiency", "效率");
		NAMES.put("silk touch", "精准采集");
		NAMES.put("silk_touch", "精准采集");
		NAMES.put("unbreaking", "耐久");
		NAMES.put("fortune", "时运");
		// 弓/弩
		NAMES.put("power", "力量");
		NAMES.put("punch", "冲击");
		NAMES.put("flame", "火矢");
		NAMES.put("infinity", "无限");
		// 钓鱼
		NAMES.put("luck of the sea", "海之眷顾");
		NAMES.put("luck_of_the_sea", "海之眷顾");
		NAMES.put("lure", "饵钓");
		// 通用
		NAMES.put("mending", "经验修补");
		NAMES.put("vanishing curse", "消失诅咒");
		NAMES.put("vanishing_curse", "消失诅咒");
		NAMES.put("curse of vanishing", "消失诅咒");
		NAMES.put("binding curse", "绑定诅咒");
		NAMES.put("binding_curse", "绑定诅咒");
		NAMES.put("curse of binding", "绑定诅咒");
		// 常见插件自定义附魔（可在 config/zzyokphone-enchant-zh.json 里继续添加）
		NAMES.put("aerial's bane", "空中克星");
		NAMES.put("antidote", "解毒");
		NAMES.put("outreach", "延伸");
		NAMES.put("swift soar", "迅捷翱翔");
		NAMES.put("wax wings", "蜡翼");
		NAMES.put("telekinesis", "念力");
		NAMES.put("smelting", "熔炼");
		NAMES.put("auto smelt", "自动熔炼");
		NAMES.put("autosmelt", "自动熔炼");
		NAMES.put("lifesteal", "吸血");
		NAMES.put("vampirism", "吸血");
		NAMES.put("vampire", "吸血");
		NAMES.put("berserk", "狂暴");
		NAMES.put("poison", "剧毒");
		NAMES.put("venom", "毒液");
		NAMES.put("wither", "凋零");
		NAMES.put("thunder", "雷击");
		NAMES.put("lightning", "雷击");
		NAMES.put("explosive", "爆炸");
		NAMES.put("freeze", "冰冻");
		NAMES.put("frost", "冰霜");
		NAMES.put("haste", "急速");
		NAMES.put("regeneration", "生命恢复");
		NAMES.put("resistance", "抗性");
		NAMES.put("speed", "速度");
		NAMES.put("jump", "跳跃");
		NAMES.put("luck", "幸运");
		NAMES.put("divine", "神圣");
		NAMES.put("void", "虚空");
		NAMES.put("meteor", "流星");
		NAMES.put("cosmic", "宇宙");
		NAMES.put("angel", "天使");
		NAMES.put("demon", "恶魔");
		// 1.21+ 新附魔
		NAMES.put("density", "密度");
		NAMES.put("breach", "破甲");
		NAMES.put("wind burst", "风爆");
		NAMES.put("wind_burst", "风爆");
		KEYS = new ArrayList<>(NAMES.keySet());
		KEYS.sort((a, b) -> b.length() - a.length());
	}

	private static volatile boolean customLoaded = false;

	private static void ensureCustom() {
		if (customLoaded) return;
		customLoaded = true;
		try {
			Path path = FabricLoader.getInstance().getConfigDir().resolve("zzyokphone-enchant-zh.json");
			if (!Files.exists(path)) {
				JsonObject defaults = new JsonObject();
				defaults.addProperty("Aerial's Bane", "空中克星");
				defaults.addProperty("Antidote", "解毒");
				defaults.addProperty("Outreach", "延伸");
				defaults.addProperty("Swift Soar", "迅捷翱翔");
				defaults.addProperty("Wax Wings", "蜡翼");
				Files.createDirectories(path.getParent());
				Files.writeString(path, defaults.toString());
			}
			JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			for (var entry : root.entrySet()) {
				if (entry.getValue().isJsonPrimitive()) {
					NAMES.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().getAsString());
				}
			}
			KEYS.clear();
			KEYS.addAll(NAMES.keySet());
			KEYS.sort((a, b) -> b.length() - a.length());
		} catch (Throwable ignored) {
		}
	}

	private EnchantTranslator() {
	}

	public static String translate(String raw) {
		ensureCustom();
		if (raw == null || raw.isBlank()) return raw;
		String plain = FORMATTING.matcher(raw).replaceAll("");
		if (plain.isBlank()) return raw;
		String lower = plain.toLowerCase(Locale.ROOT);
		String result = plain;
		boolean changed = false;
		for (String key : KEYS) {
			if (!lower.contains(key.split(" ")[0])) continue;
			String escaped = Pattern.quote(key).replace("_", "[ _]");
			try {
				Matcher matcher = Pattern.compile("(?i)\\b" + escaped + "\\b").matcher(result);
				if (matcher.find()) {
					result = matcher.replaceAll(Matcher.quoteReplacement(NAMES.get(key)));
					changed = true;
				}
			} catch (Throwable ignored) {
			}
		}
		return changed ? result : raw;
	}
}
