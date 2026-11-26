package org.opensagetv.vibe.coremcp;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reviewed-plugin lifecycle tests through the stock API, no Core/process access. */
final class PluginLifecycle {
    private final CompanionSettings.Api api;
    PluginLifecycle(CompanionSettings.Api api) { this.api = api; }
    private Object plugin(String id) {
        if (!(CompanionSettings.ID.equals(id) || "sagetvffmpegpluginlinux".equals(id) ||
                "sagetvffmpegpluginwinx64".equals(id)))
            throw new IllegalArgumentException("Plugin lifecycle ID is not allowlisted");
        Object plugins = api.global("GetInstalledPlugins"), selected = null;
        if (!(plugins instanceof Object[])) throw new IllegalStateException("Plugin inventory unavailable");
        for (Object plugin : (Object[])plugins)
            if (id.equalsIgnoreCase(String.valueOf(api.global("GetPluginIdentifier", plugin)))) {
                if (selected != null) throw new IllegalStateException("Ambiguous plugin");
                selected = plugin;
            }
        if (selected == null) throw new IllegalStateException("Reviewed plugin is not installed");
        return selected;
    }
    private void isolated(String context) {
        Object contexts = api.global("GetUIContextNames");
        if (!(contexts instanceof Object[]) || ((Object[])contexts).length != 1 ||
                !context.equals(String.valueOf(((Object[])contexts)[0])) ||
                ControlService.recordingCount(api.global("GetCurrentlyRecordingMediaFiles")) != 0 ||
                ControlService.activityArrayCount(api.global("GetConnectedClients"), "client") > 1)
            throw new IllegalStateException("Requires one exact test UI and no recordings/other clients");
    }
    private boolean enabled(Object plugin) {
        Object value = api.global("IsPluginEnabled", plugin);
        if (!(value instanceof Boolean)) throw new IllegalStateException("Unknown plugin lifecycle");
        return (Boolean)value;
    }
    synchronized Map<String,Object> lifecycle(String context, String id, boolean expected,
            boolean requested, boolean confirm) {
        if (!confirm) throw new IllegalArgumentException("Lifecycle change requires confirmation");
        isolated(context); Object plugin = plugin(id);
        if (enabled(plugin) != expected) throw new IllegalStateException("Lifecycle checkpoint changed");
        if (expected != requested && !Boolean.TRUE.equals(api.global(requested ? "EnablePlugin" : "DisablePlugin", plugin)))
            throw new IllegalStateException("Plugin lifecycle operation failed");
        if (enabled(plugin) != requested) throw new IllegalStateException("Lifecycle readback differs");
        Map<String,Object> result = new LinkedHashMap<String,Object>();
        result.put("pluginId", id); result.put("enabled", requested); return result;
    }
    synchronized Map<String,Object> failDvdProcess(String context, String id, String session, boolean confirm) {
        if (!confirm || session == null || !session.matches("[1-9][0-9]{0,17}") || CompanionSettings.ID.equals(id))
            throw new IllegalArgumentException("Confirmed exact DVD provider session required");
        isolated(context); Object plugin = plugin(id);
        if (!enabled(plugin) || !Boolean.TRUE.equals(api.ui(context, "IsCurrentMediaFileDVD")))
            throw new IllegalStateException("Active test DVD and provider required");
        Object failure = api.global("SetPluginConfigValue", plugin, "action.testDvdProcessFailure", session);
        if (failure != null) throw new IllegalStateException("Provider rejected exact-session fault");
        Map<String,Object> result = new LinkedHashMap<String,Object>();
        result.put("pluginId", id); result.put("sessionId", session); result.put("failureRequested", true);
        return result;
    }
}
