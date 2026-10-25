package cn.zzyok.zzyokphone.api;

import cn.zzyok.zzyokphone.config.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 云仓库 + Bot 自托管平台的客户端 API 封装。
 * 复用浏览器同款 Cookie 会话：登录一次后 cs_sid / bp_sid 由 CookieManager 自动携带。
 */
public final class Api {
	public static String cloudBase() {
		return normalizeBase(ModConfig.cloudBase, "https://zzyok.cn");
	}

	private static volatile String resolvedPlatformBase = null;
	private static volatile boolean cloudBaseChecked = false;

	public static String platformBase() {
		String resolved = resolvedPlatformBase;
		if (resolved != null && !resolved.isBlank()) return resolved;
		return normalizeBase(ModConfig.platformBase, "https://bot.zzyok.cn");
	}

	// 有些服务器 http 会 301 跳 https，Mod 手动跟随：探测一次并自动升级为 https
	private static void ensureCloudBase() {
		if (cloudBaseChecked) return;
		cloudBaseChecked = true;
		try {
			String base = normalizeBase(ModConfig.cloudBase, "https://zzyok.cn");
			HttpResponse<String> response = HTTP.send(
				HttpRequest.newBuilder(URI.create(base + "/api/me"))
					.timeout(Duration.ofSeconds(10))
					.header("Accept", "application/json")
					.GET().build(),
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			int code = response.statusCode();
			if (code == 301 || code == 302 || code == 307 || code == 308) {
				String location = response.headers().firstValue("location").orElse("");
				if (!location.isBlank()) {
					URI target = URI.create(base).resolve(location);
					if (target.getScheme() != null && target.getHost() != null && !target.getScheme().equalsIgnoreCase(URI.create(base).getScheme())) {
						String upgraded = target.getScheme() + "://" + target.getAuthority();
						ModConfig.cloudBase = upgraded;
						ModConfig.save();
						COOKIES.clear();
					}
				}
			}
		} catch (Throwable ignored) {
		}
	}

	private static String normalizeBase(String value, String fallback) {
		String text = value == null ? "" : value.trim();
		if (text.isBlank()) return fallback;
		if (!text.startsWith("http://") && !text.startsWith("https://")) text = "https://" + text;
		while (text.endsWith("/")) text = text.substring(0, text.length() - 1);
		return text;
	}

	private static final Gson GSON = new Gson();
	// 手动 Cookie 管理：Java HttpCookie 默认走 RFC2965 格式（$Version/引号），
	// cloud-store 的简单 Cookie 解析器识别不了，会导致登录后 /api/me 认不出会话。
	private static final java.util.Map<String, String> COOKIES = new java.util.concurrent.ConcurrentHashMap<>();
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NEVER)
		.connectTimeout(Duration.ofSeconds(15))
		.build();

	public static volatile boolean loggedIn = false;
	public static volatile String username = "";
	public static volatile String userUuid = "";
	public static volatile boolean admin = false;
	public static volatile String defaultOwnerUuid = "";
	public static volatile List<Owner> owners = new ArrayList<>();
	public static volatile String lastError = "";

	public record Owner(String uuid, String label) {}

	private Api() {}

	public static final class ApiException extends Exception {
		public ApiException(String message) { super(message); }
	}

	private static HttpRequest.Builder request(String url) {
		URI uri = URI.create(url);
		HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
			.timeout(Duration.ofSeconds(20))
			.header("Accept", "application/json")
			.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ZZYOKPhone/1.0")
			.header("Origin", cloudBase())
			.header("Referer", cloudBase() + "/");
		String cookies = cookieHeader(uri.getHost());
		if (!cookies.isBlank()) builder.header("Cookie", cookies);
		return builder;
	}

	private static String cookieHeader(String host) {
		StringBuilder builder = new StringBuilder();
		COOKIES.forEach((key, value) -> {
			int split = key.indexOf('|');
			if (split <= 0 || !key.substring(0, split).equals(host)) return;
			if (builder.length() > 0) builder.append("; ");
			builder.append(key.substring(split + 1)).append('=').append(value);
		});
		return builder.toString();
	}

	private static void captureCookies(String host, HttpResponse<?> response) {
		for (String header : response.headers().allValues("set-cookie")) {
			String pair = header.split(";", 2)[0];
			int equals = pair.indexOf('=');
			if (equals <= 0) continue;
			String name = pair.substring(0, equals).trim();
			String value = pair.substring(equals + 1).trim();
			String key = host + "|" + name;
			if (value.isEmpty() || header.toLowerCase().contains("max-age=0")) COOKIES.remove(key);
			else COOKIES.put(key, value);
		}
	}

	private static HttpResponse<String> send(HttpRequest request) throws Exception {
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		captureCookies(request.uri().getHost(), response);
		return response;
	}

	private static JsonObject parseObject(String body) throws ApiException {
		try {
			JsonElement element = JsonParser.parseString(body == null ? "" : body);
			return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
		} catch (Throwable error) {
			throw new ApiException("服务器返回异常（" + (body == null ? 0 : body.length()) + " 字节）");
		}
	}

	private static String errorMessage(HttpResponse<String> response) {
		try {
			JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
			if (object.has("error") && !object.get("error").isJsonNull()) return object.get("error").getAsString();
			if (object.has("message") && !object.get("message").isJsonNull()) return object.get("message").getAsString();
		} catch (Throwable ignored) {
		}
		return "请求失败（HTTP " + response.statusCode() + "）";
	}

	private static JsonObject getJson(String url) throws Exception {
		HttpResponse<String> response = send(request(url).GET().build());
		JsonObject object = parseObject(response.body());
		if (response.statusCode() >= 400) throw new ApiException(errorMessage(response));
		if (object.has("ok") && !object.get("ok").getAsBoolean()) throw new ApiException(errorMessage(response));
		return object;
	}

	private static JsonObject postJson(String url, JsonObject body) throws Exception {
		HttpResponse<String> response = send(request(url)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
			.build());
		JsonObject object = parseObject(response.body());
		if (response.statusCode() >= 400) throw new ApiException(errorMessage(response));
		if (object.has("ok") && !object.get("ok").getAsBoolean()) throw new ApiException(errorMessage(response));
		return object;
	}

	/* ---------------- 登录 / 账号 ---------------- */

	public static void login(String password) throws Exception {
		ensureCloudBase();
		lastError = "";
		JsonObject body = new JsonObject();
		body.addProperty("token", password);
		JsonObject result = postJson(cloudBase() + "/api/login", body);
		if (result.has("autoRegistrationMessage")) {
			String message = result.get("autoRegistrationMessage").getAsString();
			if (message != null && !message.isBlank()) lastError = message;
		}
		loadMe();
	}

	public static void loadMe() throws Exception {
		ensureCloudBase();
		JsonObject me = getJson(cloudBase() + "/api/me");
		if (!me.has("loggedIn") || !me.get("loggedIn").getAsBoolean()) {
			loggedIn = false;
			throw new ApiException("登录状态失效，请重新登录");
		}
		JsonObject user = me.has("user") && me.get("user").isJsonObject() ? me.getAsJsonObject("user") : new JsonObject();
		username = user.has("username") ? user.get("username").getAsString() : "";
		userUuid = user.has("uuid") ? user.get("uuid").getAsString() : "";
		admin = user.has("isAdmin") && user.get("isAdmin").getAsBoolean();
		defaultOwnerUuid = me.has("defaultOwnerUuid") && !me.get("defaultOwnerUuid").isJsonNull() ? me.get("defaultOwnerUuid").getAsString() : "";

		List<Owner> list = new ArrayList<>();
		JsonArray ownerArray = me.has("owners") && me.get("owners").isJsonArray() ? me.getAsJsonArray("owners") : new JsonArray();
		for (JsonElement element : ownerArray) {
			if (!element.isJsonObject()) continue;
			JsonObject owner = element.getAsJsonObject();
			String uuid = owner.has("ownerUuid") ? owner.get("ownerUuid").getAsString() : "";
			String label = owner.has("label") ? owner.get("label").getAsString() : uuid;
			if (!uuid.isBlank()) list.add(new Owner(uuid, label));
		}
		if (list.isEmpty() && admin) {
			try {
				JsonObject adminOwners = getJson(cloudBase() + "/api/admin/owners");
				JsonArray rows = adminOwners.has("rows") ? adminOwners.getAsJsonArray("rows") : new JsonArray();
				for (JsonElement element : rows) {
					if (!element.isJsonObject()) continue;
					JsonObject owner = element.getAsJsonObject();
					String uuid = owner.has("ownerUuid") ? owner.get("ownerUuid").getAsString() : "";
					String label = owner.has("label") ? owner.get("label").getAsString() : uuid;
					if (!uuid.isBlank()) list.add(new Owner(uuid, label));
				}
			} catch (Throwable ignored) {
			}
		}
		owners = list;
		if (defaultOwnerUuid.isBlank() && !list.isEmpty()) defaultOwnerUuid = list.get(0).uuid();
		loggedIn = true;
		lastError = "";
		saveSession();
	}

	/* ---------------- 临时验证码登录（无需输入密码） ---------------- */

	public static JsonObject createChallenge() throws Exception {
		ensureCloudBase();
		return postJson(cloudBase() + "/api/login/challenge", new JsonObject());
	}

	public static JsonObject challengeStatus() throws Exception {
		ensureCloudBase();
		return getJson(cloudBase() + "/api/login/challenge");
	}

	public static void cancelChallenge() {
		try {
			postJson(cloudBase() + "/api/login/challenge/cancel", new JsonObject());
		} catch (Throwable ignored) {
		}
	}

	public static void logout() throws Exception {
		try {
			postJson(cloudBase() + "/api/logout", new JsonObject());
		} catch (Throwable ignored) {
		}
		clearSession();
		loggedIn = false;
		username = "";
		userUuid = "";
		admin = false;
		defaultOwnerUuid = "";
		owners = new ArrayList<>();
		lastError = "";
	}

	private static Path sessionPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("zzyokphone-session.json");
	}

	private static volatile boolean validating = false;

	public static synchronized void saveSession() {
		if (!loggedIn) return;
		try {
			JsonObject root = new JsonObject();
			root.addProperty("cloudBase", cloudBase());
			root.addProperty("username", username);
			root.addProperty("userUuid", userUuid);
			root.addProperty("admin", admin);
			root.addProperty("defaultOwnerUuid", defaultOwnerUuid);
			JsonArray ownerArray = new JsonArray();
			for (Owner owner : owners) {
				JsonObject item = new JsonObject();
				item.addProperty("uuid", owner.uuid());
				item.addProperty("label", owner.label());
				ownerArray.add(item);
			}
			root.add("owners", ownerArray);
			JsonObject cookies = new JsonObject();
			COOKIES.forEach(cookies::addProperty);
			root.add("cookies", cookies);
			root.addProperty("savedAt", System.currentTimeMillis());
			Files.createDirectories(sessionPath().getParent());
			Files.writeString(sessionPath(), root.toString());
		} catch (Throwable ignored) {
		}
	}

	public static synchronized void restoreSession() {
		try {
			Path path = sessionPath();
			if (!Files.exists(path)) return;
			JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			String savedBase = root.has("cloudBase") ? root.get("cloudBase").getAsString() : "";
			if (!savedBase.equals(cloudBase())) return;
			COOKIES.clear();
			if (root.has("cookies") && root.get("cookies").isJsonObject()) {
				for (var entry : root.getAsJsonObject("cookies").entrySet()) {
					COOKIES.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
			username = root.has("username") ? root.get("username").getAsString() : "";
			userUuid = root.has("userUuid") ? root.get("userUuid").getAsString() : "";
			admin = root.has("admin") && root.get("admin").getAsBoolean();
			defaultOwnerUuid = root.has("defaultOwnerUuid") ? root.get("defaultOwnerUuid").getAsString() : "";
			List<Owner> list = new ArrayList<>();
			if (root.has("owners") && root.get("owners").isJsonArray()) {
				for (var element : root.getAsJsonArray("owners")) {
					if (!element.isJsonObject()) continue;
					JsonObject item = element.getAsJsonObject();
					String uuid = item.has("uuid") ? item.get("uuid").getAsString() : "";
					String label = item.has("label") ? item.get("label").getAsString() : uuid;
					if (!uuid.isBlank()) list.add(new Owner(uuid, label));
				}
			}
			owners = list;
			loggedIn = !username.isBlank();
		} catch (Throwable ignored) {
		}
	}

	public static synchronized void clearSession() {
		try {
			Files.deleteIfExists(sessionPath());
		} catch (Throwable ignored) {
		}
	}

	// 后台校验恢复的会话是否仍然有效，失效则自动清理
	public static void validateRestoredSession() {
		if (!loggedIn || validating) return;
		validating = true;
		Thread thread = new Thread(() -> {
			try {
				JsonObject me = getJson(cloudBase() + "/api/me");
				boolean stillLoggedIn = me.has("loggedIn") && me.get("loggedIn").getAsBoolean();
				if (!stillLoggedIn) {
					resetSession();
					return;
				}
				loadMe();
				saveSession();
			} catch (Throwable ignored) {
			} finally {
				validating = false;
			}
		}, "zzyokphone-session");
		thread.setDaemon(true);
		thread.start();
	}

	public static void resetSession() {
		COOKIES.clear();
		loggedIn = false;
		username = "";
		userUuid = "";
		admin = false;
		defaultOwnerUuid = "";
		owners = new ArrayList<>();
		lastError = "";
		clearSession();
	}

	public static boolean isAdmin() {
		return admin;
	}

	public static String ownerLabel(String uuid) {
		for (Owner owner : owners) {
			if (owner.uuid().equals(uuid)) return owner.label();
		}
		return uuid;
	}

	/* ---------------- 云仓库 ---------------- */

	public static JsonArray loadInventory() throws Exception {
		JsonArray merged = new JsonArray();
		if (admin) {
			JsonObject data = getJson(cloudBase() + "/api/admin/inventory");
			JsonArray rows = data.has("rows") ? data.getAsJsonArray("rows") : new JsonArray();
			for (JsonElement element : rows) merged.add(element);
			return merged;
		}
		for (Owner owner : owners) {
			JsonObject data = getJson(cloudBase() + "/api/inventory?owner=" + URLEncoder.encode(owner.uuid(), StandardCharsets.UTF_8));
			JsonArray rows = data.has("items") ? data.getAsJsonArray("items")
				: data.has("rows") ? data.getAsJsonArray("rows") : new JsonArray();
			for (JsonElement element : rows) {
				if (!element.isJsonObject()) continue;
				JsonObject row = element.getAsJsonObject();
				if (!row.has("ownerUuid")) row.addProperty("ownerUuid", owner.uuid());
				if (!row.has("ownerLabel")) row.addProperty("ownerLabel", owner.label());
				merged.add(row);
			}
		}
		return merged;
	}

	public static JsonArray loadInventory(String ownerUuid) throws Exception {
		JsonObject data = getJson(cloudBase() + "/api/inventory?owner=" + URLEncoder.encode(ownerUuid, StandardCharsets.UTF_8));
		JsonArray rows = data.has("items") ? data.getAsJsonArray("items")
			: data.has("rows") ? data.getAsJsonArray("rows") : new JsonArray();
		JsonArray result = new JsonArray();
		for (JsonElement element : rows) {
			if (!element.isJsonObject()) continue;
			JsonObject row = element.getAsJsonObject();
			if (!row.has("ownerUuid")) row.addProperty("ownerUuid", ownerUuid);
			if (!row.has("ownerLabel")) row.addProperty("ownerLabel", ownerLabel(ownerUuid));
			result.add(row);
		}
		return result;
	}
	
	public static JsonObject setDefaultOwner(String ownerUuid) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("ownerUuid", ownerUuid);
		return postJson(cloudBase() + "/api/default-owner", body);
	}

	public static JsonObject repair(String targetUsername) throws Exception {
		return repair(targetUsername, null);
	}

	public static JsonObject repair(String targetUsername, String ownerUuid) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("targetUsername", targetUsername);
		if (ownerUuid != null && !ownerUuid.isBlank()) body.addProperty("ownerUuid", ownerUuid);
		return postJson(cloudBase() + "/api/action/repair", body);
	}

	public static JsonObject deposit(String ownerUuid) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("ownerUuid", ownerUuid);
		return postJson(cloudBase() + "/api/action/deposit", body);
	}

	public static JsonObject withdraw(String ownerUuid, JsonArray items, String targetUsername) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("ownerUuid", ownerUuid);
		body.add("items", items);
		body.addProperty("targetUsername", targetUsername);
		return postJson(cloudBase() + "/api/action/withdraw", body);
	}

	/* ---------------- 余额 / 订阅 ---------------- */

	public static double loadZzko() throws Exception {
		JsonObject data = getJson(cloudBase() + "/api/zzko");
		if (data.has("zzko") && !data.get("zzko").isJsonNull()) return data.get("zzko").getAsDouble();
		return 0;
	}

	public static JsonObject loadSubscriptions() throws Exception {
		return getJson(cloudBase() + "/api/subscriptions");
	}

	public static JsonObject purchaseSubscription(String ownerUuid, String planId) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("ownerUuid", ownerUuid);
		body.addProperty("planId", planId);
		return postJson(cloudBase() + "/api/subscriptions/purchase", body);
	}

	/* ---------------- Bot 平台 ---------------- */

	private static void ensurePlatformSession() throws Exception {
		HttpResponse<String> entry = send(request(cloudBase() + "/bot-platform").GET().build());
		String location = entry.headers().firstValue("location").orElse("");
		if (location.isBlank()) {
			throw new ApiException("无法进入 Bot 平台（请先在云仓库登录）");
		}
		URI target = URI.create(location);
		if (target.getHost() != null && (target.getHost().equals("127.0.0.1") || target.getHost().equalsIgnoreCase("localhost"))) {
			URI configured = URI.create(platformBase());
			target = new URI(configured.getScheme(), configured.getHost(), target.getPath(), target.getQuery(), null);
		} else if (target.getScheme() != null && target.getHost() != null) {
			// 云仓库的 /bot-platform 会返回真正的平台地址：自动推导，无需手填
			resolvedPlatformBase = target.getScheme() + "://" + target.getAuthority();
			if (!resolvedPlatformBase.equals(normalizeBase(ModConfig.platformBase, ""))) {
				ModConfig.platformBase = resolvedPlatformBase;
				ModConfig.save();
			}
		}
		send(request(target.toString()).GET().build());
		saveSession();
	}

	public static JsonArray loadSmartTasks(String botId) throws Exception {
		ensurePlatformSession();
		JsonObject data = getJson(platformBase() + "/api/smart/tasks?botId=" + java.net.URLEncoder.encode(botId, java.nio.charset.StandardCharsets.UTF_8));
		return data.has("rows") ? data.getAsJsonArray("rows") : new JsonArray();
	}

	public static JsonObject loadSmartPermissions(String botId) throws Exception {
		ensurePlatformSession();
		return getJson(platformBase() + "/api/smart/permissions?botId=" + java.net.URLEncoder.encode(botId, java.nio.charset.StandardCharsets.UTF_8));
	}

	public static JsonObject saveSmartPermission(JsonObject payload) throws Exception {
		ensurePlatformSession();
		return postJson(platformBase() + "/api/smart/permissions", payload);
	}

	public static JsonObject saveSmartTask(JsonObject payload) throws Exception {
		ensurePlatformSession();
		return postJson(platformBase() + "/api/smart/tasks", payload);
	}

	public static JsonArray loadBots() throws Exception {
		ensurePlatformSession();
		JsonObject data = getJson(platformBase() + "/api/bots");
		return data.has("rows") ? data.getAsJsonArray("rows") : new JsonArray();
	}

	public static JsonObject botAction(String action, String botId) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("id", botId);
		return postJson(platformBase() + "/api/bots/" + action, body);
	}

	public static JsonArray loadBotLogs(String botId) throws Exception {
		JsonObject data = getJson(platformBase() + "/api/bots/logs?botId=" + URLEncoder.encode(botId, StandardCharsets.UTF_8));
		return data.has("rows") ? data.getAsJsonArray("rows") : new JsonArray();
	}
}
