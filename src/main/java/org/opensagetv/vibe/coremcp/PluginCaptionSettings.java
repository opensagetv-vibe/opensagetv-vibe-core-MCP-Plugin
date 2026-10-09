package org.opensagetv.vibe.coremcp;

import java.util.Locale;

/**
 * Commissioning-only control for the FFmpeg plugin caption listener. Uses a
 * typed installed Plugin from public APIs, never arbitrary expressions or a
 * direct INI write. Only one non-secret Boolean setting is exposed. Caller
 * checkpoints it and supplies the expected current value before any change.
 */
final class PluginCaptionSettings {
    interface Api { Object call(String name,Object... arguments); }
    private final Api api;
    static final String SETTING="caption_side_channel.enabled";
    PluginCaptionSettings(Api api) { this.api=api; }

    String get(String pluginId,String setting) {
        Object plugin=plugin(pluginId,setting);
        return bool(api.call("GetPluginConfigValue",plugin,SETTING));
    }
    String set(String pluginId,String setting,String expected,String requested,boolean confirm) {
        if (!confirm) throw new IllegalArgumentException("Plugin setting change requires confirm=true");
        String before=bool(expected), after=bool(requested);
        Object plugin=plugin(pluginId,setting);
        if (!before.equals(bool(api.call("GetPluginConfigValue",plugin,SETTING))))
            throw new IllegalStateException("Plugin setting changed since checkpoint");
        Object failure=api.call("SetPluginConfigValue",plugin,SETTING,after);
        if (failure !=null) throw new IllegalStateException("Plugin rejected setting change");
        String actual=bool(api.call("GetPluginConfigValue",plugin,SETTING));
        if (!actual.equals(after)) throw new IllegalStateException("Plugin setting readback differs");
        return actual;
    }
    private Object plugin(String pluginId,String setting) {
        String id=pluginId ==null ? "" : pluginId.toLowerCase(Locale.US);
        if (!("sagetvffmpegpluginlinux".equals(id) || "sagetvffmpegpluginwinx64".equals(id))
                || !SETTING.equals(setting)) throw new IllegalArgumentException("Plugin setting not allowlisted");
        Object installed=api.call("GetInstalledPlugins");
        Object selected=null;
        if (installed instanceof Object[]) for (Object plugin:(Object[])installed) {
            if (!id.equals(String.valueOf(api.call("GetPluginIdentifier",plugin)).toLowerCase(Locale.US))) continue;
            if (selected !=null) throw new IllegalStateException("Ambiguous installed plugin");
            selected=plugin;
        }
        if (selected ==null || !Boolean.TRUE.equals(api.call("IsPluginEnabled",selected)))
            throw new IllegalStateException("Installed plugin unavailable or disabled");
        Object settings=api.call("GetPluginConfigSettings",selected);
        boolean found=false;
        if (settings instanceof Object[]) for (Object key:(Object[])settings) if (SETTING.equals(key)) found=true;
        if (!found) throw new IllegalStateException("Installed plugin lacks caption setting");
        return selected;
    }
    private static String bool(Object value) {
        if (value !=null && "true".equalsIgnoreCase(value.toString())) return "true";
        if (value !=null && "false".equalsIgnoreCase(value.toString())) return "false";
        throw new IllegalArgumentException("Boolean plugin value required");
    }
}
