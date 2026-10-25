package cn.zzyok.zzyokphone.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Mod 配置：自动传送应答模式，持久化到 config/zzyokphone.json。 */
public final class ModConfig {
	public enum TpMode { OFF, ACCEPT, DENY }

	public enum LoginCommandMode { MSG, TELL }

	public static volatile TpMode tpMode = TpMode.OFF;
	public static volatile LoginCommandMode loginCommandMode = LoginCommandMode.MSG;
	public static volatile String tpAcceptCommand = "tpaccept";
	public static volatile String tpDenyCommand = "tpdeny";
	public static volatile boolean hdMode = true;
	public static volatile boolean uiSounds = true;
	public static volatile boolean requireRenamedTotem = true;
	public static volatile String totemTriggerName = "ZZYOK Phone";
	public static volatile String cloudBase = "https://zzyok.cn";
	public static volatile String platformBase = "https://bot.zzyok.cn";
	public static volatile String botName = "machel11";
	public static volatile String botCommandPrefix = "!";
	public static volatile String rechargeCommandTemplate = "{prefix}充值 {amount}";

	private ModConfig() {}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("zzyokphone.json");
	}

	public static synchronized void load() {
		try {
			Path path = path();
			if (!Files.exists(path)) return;
			JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			if (root.has("tpMode")) {
				tpMode = TpMode.valueOf(root.get("tpMode").getAsString());
			}
			if (root.has("loginCommandMode")) {
				loginCommandMode = LoginCommandMode.valueOf(root.get("loginCommandMode").getAsString());
			}
			if (root.has("tpAcceptCommand")) tpAcceptCommand = root.get("tpAcceptCommand").getAsString();
			if (root.has("tpDenyCommand")) tpDenyCommand = root.get("tpDenyCommand").getAsString();
			if (root.has("hdMode")) hdMode = root.get("hdMode").getAsBoolean();
			if (root.has("uiSounds")) uiSounds = root.get("uiSounds").getAsBoolean();
			if (root.has("requireRenamedTotem")) requireRenamedTotem = root.get("requireRenamedTotem").getAsBoolean();
			if (root.has("totemTriggerName")) totemTriggerName = root.get("totemTriggerName").getAsString();
			if (root.has("cloudBase")) cloudBase = root.get("cloudBase").getAsString();
			if (root.has("platformBase")) platformBase = root.get("platformBase").getAsString();
			if (root.has("botName")) botName = root.get("botName").getAsString();
			if (root.has("botCommandPrefix")) botCommandPrefix = root.get("botCommandPrefix").getAsString();
			if (root.has("rechargeCommandTemplate")) rechargeCommandTemplate = root.get("rechargeCommandTemplate").getAsString();
		} catch (Throwable ignored) {
			tpMode = TpMode.OFF;
		}
	}

	public static synchronized void save() {
		try {
			JsonObject root = new JsonObject();
			root.addProperty("tpMode", tpMode.name());
			root.addProperty("loginCommandMode", loginCommandMode.name());
			root.addProperty("tpAcceptCommand", tpAcceptCommand);
			root.addProperty("tpDenyCommand", tpDenyCommand);
			root.addProperty("hdMode", hdMode);
			root.addProperty("uiSounds", uiSounds);
			root.addProperty("requireRenamedTotem", requireRenamedTotem);
			root.addProperty("totemTriggerName", totemTriggerName);
			root.addProperty("cloudBase", cloudBase);
			root.addProperty("platformBase", platformBase);
			root.addProperty("botName", botName);
			root.addProperty("botCommandPrefix", botCommandPrefix);
			root.addProperty("rechargeCommandTemplate", rechargeCommandTemplate);
			Path path = path();
			Files.createDirectories(path.getParent());
			Files.writeString(path, root.toString());
		} catch (Throwable ignored) {
		}
	}
}
