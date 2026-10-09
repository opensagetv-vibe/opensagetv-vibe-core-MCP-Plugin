package org.opensagetv.vibe.coremcp;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only stock API boundary for the installed Client Extension. This is
 * not an arbitrary settings/class-loading proxy. Global changes require an
 * optimistic checkpoint; the only UI write selects/removes the known DVD
 * adapter on one connected MiniClient. Third-party players are never replaced.
 * No player/decoder/socket API is read while changing configuration.
 */
final class CompanionSettings {
    interface Api {
        Object global(String name, Object... args);
        Object ui(String context, String name, Object... args);
    }
    static final String ID = "opensagetvvibeclientextensionplugin";
    static final String PLAYER = "org.opensagetv.vibe.clientextension.ClientExtensionDvdPlayer";
    static final String PROPERTY = "media_player_plugin_class/dvd";
    private static final List<String> KEYS = Arrays.asList(
            "enabled", "reload.recovery", "trace.dvd", "dvd.tv_skip_keys");
    private final Api api;
    CompanionSettings(Api api) { this.api = api; }

    synchronized String get(String key) {
        requireKey(key);
        Object plugin = plugin();
        requireSupported(plugin, key);
        return bool(api.global("GetPluginConfigValue", plugin, key));
    }

    synchronized String set(String key, String expected, String value, boolean confirm) {
        requireKey(key);
        requireConfirm(confirm);
        String before = bool(expected), after = bool(value);
        Object plugin = plugin();
        requireSupported(plugin, key);
        if (!before.equals(bool(api.global("GetPluginConfigValue", plugin, key))))
            throw new IllegalStateException("Companion setting changed since checkpoint");
        Object failure = api.global("SetPluginConfigValue", plugin, key, after);
        if (failure != null) throw new IllegalStateException("Companion rejected setting");
        String actual = bool(api.global("GetPluginConfigValue", plugin, key));
        if (!actual.equals(after)) throw new IllegalStateException("Companion readback differs");
        return actual;
    }

    synchronized Map<String, Object> hook(String context, String expected,
            Boolean enabled, boolean confirm) {
        Object plugin = plugin();
        requireContext(context);
        String current = String.valueOf(api.ui(context, "GetProperty", PROPERTY, ""));
        if (enabled != null) {
            requireConfirm(confirm);
            // Never accept a caller-supplied class name, including on restore.
            if (!("".equals(expected) || PLAYER.equals(expected)))
                throw new IllegalArgumentException("Only blank or known adapter checkpoints allowed");
            if (!expected.equals(current))
                throw new IllegalStateException("DVD player changed since checkpoint");
            if (enabled.booleanValue() && !"true".equals(
                    bool(api.global("GetPluginConfigValue", plugin, "enabled"))))
                throw new IllegalStateException("Companion feature is disabled");
            String target = enabled.booleanValue() ? PLAYER : "";
            api.ui(context, "SetProperty", PROPERTY, target);
            current = String.valueOf(api.ui(context, "GetProperty", PROPERTY, ""));
            if (!target.equals(current)) throw new IllegalStateException("DVD hook readback differs");
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("context", context);
        result.put("playerClass", current);
        result.put("adapterSelected", Boolean.valueOf(PLAYER.equals(current)));
        return result;
    }

    private Object plugin() {
        Object installed = api.global("GetInstalledPlugins"), selected = null;
        if (installed instanceof Object[]) for (Object plugin : (Object[]) installed) {
            if (!ID.equalsIgnoreCase(String.valueOf(api.global("GetPluginIdentifier", plugin)))) continue;
            if (selected != null) throw new IllegalStateException("Ambiguous companion plugin");
            selected = plugin;
        }
        if (selected == null || !Boolean.TRUE.equals(api.global("IsPluginEnabled", selected)))
            throw new IllegalStateException("Installed companion unavailable or disabled");
        return selected;
    }
    private void requireSupported(Object plugin, String key) {
        Object settings = api.global("GetPluginConfigSettings", plugin);
        if (settings instanceof Object[]) for (Object setting : (Object[]) settings)
            if (key.equals(setting)) return;
        throw new IllegalStateException("Installed companion lacks requested option");
    }
    private void requireContext(String context) {
        if (context == null || !context.matches("[0-9A-Fa-f]{12}"))
            throw new IllegalArgumentException("Exact connected MiniClient context required");
        Object names = api.global("GetUIContextNames");
        if (names instanceof Object[]) for (Object name : (Object[]) names)
            if (context.equals(name)) return;
        throw new IllegalStateException("MiniClient is not connected");
    }
    private static void requireKey(String key) {
        if (!KEYS.contains(key)) throw new IllegalArgumentException("Companion setting not allowlisted");
    }
    private static void requireConfirm(boolean confirm) {
        if (!confirm) throw new IllegalArgumentException("Companion change requires confirm=true");
    }
    static Boolean enabledValue(String raw) {
        return Boolean.valueOf(bool(raw));
    }
    private static String bool(Object value) {
        if (value != null && "true".equalsIgnoreCase(value.toString())) return "true";
        if (value != null && "false".equalsIgnoreCase(value.toString())) return "false";
        throw new IllegalArgumentException("Boolean companion value required");
    }
}
