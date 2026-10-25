package cn.zzyok.zzyokphone.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** 物品名称拼音匹配：支持首字母（如 bh → 白桦）和全拼（baihua）。 */
public final class PinyinUtil {
	private static final Map<Character, String[]> MAP = new HashMap<>();

	static {
		try (InputStream in = PinyinUtil.class.getResourceAsStream("/assets/zzyokphone/pinyin.txt")) {
			if (in != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						String[] parts = line.trim().split("\\|");
						if (parts.length >= 3 && parts[0].length() == 1) {
							MAP.put(parts[0].charAt(0), new String[] {parts[1], parts[2]});
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}
	}

	private PinyinUtil() {}

	public static boolean matches(String name, String query) {
		if (name == null || query == null) return false;
		String q = query.toLowerCase().replaceAll("\\s+", "");
		if (q.isEmpty()) return true;
		StringBuilder initials = new StringBuilder();
		StringBuilder full = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			String[] entry = MAP.get(ch);
			if (entry != null) {
				initials.append(entry[0]);
				full.append(entry[1]);
			} else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
				initials.append(Character.toLowerCase(ch));
				full.append(Character.toLowerCase(ch));
			}
		}
		return initials.indexOf(q) >= 0 || full.indexOf(q) >= 0;
	}
}
