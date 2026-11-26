package org.opensagetv.vibe.coremcp;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    static final String OWNED_PLAYER = "org.opensagetv.vibe.clientextension.PluginOwnedDvdPlayer";
    static final String PROPERTY = "media_player_plugin_class/dvd";
    static final String TS_PLAYER = "org.opensagetv.vibe.clientextension.ClientExtensionTsPlayer";
    static final String TS_PROPERTY = "media_player_plugin_class/ts";
    private static final List<String> KEYS = Arrays.asList(
            "enabled", "reload.recovery", "trace.dvd", "dvd.tv_skip_keys", "diagnostics.enabled",
            "video.restart_fix", "video.caption_bridge", "video.state_bridge", "dvd.client_controls", "dvd.owned_navigation", "dvd.owned_mode", "dvd.skip_menus", "dvd.skip_previews");
    private final Api api;
    CompanionSettings(Api api) { this.api = api; }

    /**
     * Read-only commissioning checkpoint for ONE connected UI's companion
     * bookmark. Tests must refuse a pre-existing bookmark before save/clear.
     * The disc identifier is never returned; a digest permits detecting a
     * concurrent change without exposing a media path or allowing restoration
     * of caller-supplied property values. Normal playback does not call MCP.
     */
    synchronized Map<String,Object> dvdBookmark(String context) {
        requireContext(context);
        requireSupported(plugin(), "action.dvd_save_resume");
        String key="vibe/client_extension/owned_bookmark/"+Base64.getUrlEncoder()
                .withoutPadding().encodeToString(context.getBytes(StandardCharsets.UTF_8));
        Object raw=api.global("GetServerProperty",key,"");
        if (!(raw instanceof String) || ((String)raw).length()>8192)
            throw new IllegalStateException("Owned bookmark checkpoint unavailable or oversized");
        String value=(String)raw;
        Map<String,Object> result=new LinkedHashMap<String,Object>();
        result.put("context",context);result.put("present",Boolean.valueOf(!value.isEmpty()));
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder digest=new StringBuilder(64);
            for(byte b:bytes)digest.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            result.put("fingerprint",digest.toString());
        } catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("SHA-256 unavailable",unavailable); }
        // Even malformed existing data is PRESENT and must be preserved.
        result.put("valid",Boolean.FALSE);
        String[] parts=value.split(":",-1);
        if(parts.length==3 && parts[0].matches("[A-Za-z0-9_-]+")) try {
            int title=Integer.parseInt(parts[1]);long position=Long.parseLong(parts[2]);
            if(title>0 && title<=999 && position>=0 && position<=86400000L) {
                result.put("valid",Boolean.TRUE);result.put("title",Integer.valueOf(title));
                result.put("positionMs",Long.valueOf(position));
            }
        } catch(NumberFormatException malformed) { /* Preserve without accepting it. */ }
        return result;
    }

    String metadataGet(int id,String field) {
        if(id<=0 || !Arrays.asList("Title","EpisodeName","Description","Year","SeasonNumber","EpisodeNumber").contains(field))
            throw new IllegalArgumentException("Exact imported ID and descriptive field required");
        Object media=api.global("GetMediaFileForID",Integer.valueOf(id));
        if(media==null || !Boolean.TRUE.equals(api.global("IsLibraryFile",media))
                || Boolean.TRUE.equals(api.global("IsTVFile",media))
                || Boolean.TRUE.equals(api.global("IsFileCurrentlyRecording",media)))
            throw new IllegalArgumentException("Only imported non-TV/non-recording media may be inspected");
        Object value=api.global("GetMediaFileMetadata",media,field);
        return value==null ? "" : String.valueOf(value);
    }

    synchronized String dvdIntent(String context,String operation,boolean confirm) {
        requireConfirm(confirm); requireContext(context);
        if (!Arrays.asList("save_resume","resume","beginning","clear_resume").contains(operation))
            throw new IllegalArgumentException("Only explicit owned DVD save/resume/beginning allowed");
        Object plugin=plugin();String action="action.dvd_"+operation;requireSupported(plugin,action);
            api.global("SetPluginConfigValue",plugin,action,context);
            String status=String.valueOf(api.global("GetPluginConfigValue",plugin,"status"));
            if (!status.startsWith("Saved owned DVD") && !status.startsWith("Queued explicit owned DVD") && !status.startsWith("Cleared owned DVD"))
                throw new IllegalStateException(status);
            return status;
    }

    /** Fixed descriptive fields; neither arbitrary plugin settings nor format edits. */
    synchronized String metadataReview(int id,String field,String expected,String value,boolean confirm) {
        if (id<=0 || !Arrays.asList("Title","EpisodeName","Description","Year","SeasonNumber","EpisodeNumber").contains(field)
                || value==null || value.length()>2048 || expected==null || expected.length()>2048)
            throw new IllegalArgumentException("Exact imported ID and bounded descriptive field required");
        Object plugin=plugin();requireSupported(plugin,"action.metadata_preview");
        Object media=api.global("GetMediaFileForID",Integer.valueOf(id));
        Object before=media==null ? null : api.global("GetMediaFileMetadata",media,field);
        if (media==null || !expected.equals(before==null ? "" : String.valueOf(before)))
            throw new IllegalStateException("Imported field changed since checkpoint");
        String[] keys={"metadata.id","metadata.field","metadata.value"};
        String[] previous=new String[keys.length];
        for(int i=0;i<keys.length;i++)previous[i]=String.valueOf(api.global("GetPluginConfigValue",plugin,keys[i]));
        try {
            String[] values={Integer.toString(id),field,value};
            for(int i=0;i<keys.length;i++)api.global("SetPluginConfigValue",plugin,keys[i],values[i]);
            api.global("SetPluginConfigValue",plugin,"action.metadata_preview","");
            String status=String.valueOf(api.global("GetPluginConfigValue",plugin,"status"));
            if (!status.startsWith("Preview ID=")) throw new IllegalStateException(status);
            if(confirm) {
                Object current=api.global("GetMediaFileMetadata",media,field);
                if(!expected.equals(current==null ? "" : String.valueOf(current)))
                    throw new IllegalStateException("Imported field changed while preparing review");
                api.global("SetPluginConfigValue",plugin,"action.metadata_apply","");
                status=String.valueOf(api.global("GetPluginConfigValue",plugin,"status"));
                if(!status.startsWith("Applied ID="))throw new IllegalStateException(status);
            }
            return status;
        } finally { for(int i=0;i<keys.length;i++)api.global("SetPluginConfigValue",plugin,keys[i],previous[i]); }
    }

    synchronized String get(String key) {
        requireKey(key);
        Object plugin = plugin();
        requireSupported(plugin, key);
        return value(key, api.global("GetPluginConfigValue", plugin, key));
    }

    synchronized String set(String key, String expected, String value, boolean confirm) {
        requireKey(key);
        requireConfirm(confirm);
        String before = value(key, expected), after = value(key, value);
        Object plugin = plugin();
        requireSupported(plugin, key);
        if (!before.equals(value(key, api.global("GetPluginConfigValue", plugin, key))))
            throw new IllegalStateException("Companion setting changed since checkpoint");
        Object failure = api.global("SetPluginConfigValue", plugin, key, after);
        if (failure != null) throw new IllegalStateException("Companion rejected setting");
        String actual = value(key, api.global("GetPluginConfigValue", plugin, key));
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
            if (!("".equals(expected) || PLAYER.equals(expected) || OWNED_PLAYER.equals(expected)))
                throw new IllegalArgumentException("Only blank or known adapter checkpoints allowed");
            if (!expected.equals(current))
                throw new IllegalStateException("DVD player changed since checkpoint");
            if (enabled.booleanValue() && !"true".equals(
                    bool(api.global("GetPluginConfigValue", plugin, "enabled"))))
                throw new IllegalStateException("Companion feature is disabled");
            String target = "";
            if (enabled.booleanValue()) {
                target = PLAYER;
                Object settings = api.global("GetPluginConfigSettings", plugin);
                if (settings instanceof Object[]) for (Object setting : (Object[]) settings) {
                    if ("dvd.owned_navigation".equals(setting)
                            && "true".equals(bool(api.global("GetPluginConfigValue", plugin, setting))))
                        target = OWNED_PLAYER;
                }
            }
            api.ui(context, "SetProperty", PROPERTY, target);
            current = String.valueOf(api.ui(context, "GetProperty", PROPERTY, ""));
            if (!target.equals(current)) throw new IllegalStateException("DVD hook readback differs");
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("context", context);
        result.put("playerClass", current);
        result.put("adapterSelected", Boolean.valueOf(PLAYER.equals(current) || OWNED_PLAYER.equals(current)));
        result.put("ownedNavigatorSelected", Boolean.valueOf(OWNED_PLAYER.equals(current)));
        return result;
    }

    /** Fixed TS counterpart; no caller-selected property/class or arbitrary API. */
    synchronized Map<String, Object> tsHook(String context, String expected,
            Boolean enabled, boolean confirm) {
        Object plugin = plugin();
        requireSupported(plugin, "video.restart_fix");
        requireContext(context);
        String current = String.valueOf(api.ui(context, "GetProperty", TS_PROPERTY, ""));
        if (enabled != null) {
            requireConfirm(confirm);
            if (!("".equals(expected) || TS_PLAYER.equals(expected)))
                throw new IllegalArgumentException("Only blank or known TS adapter checkpoint allowed");
            if (!expected.equals(current))
                throw new IllegalStateException("TS player changed since checkpoint");
            if (enabled.booleanValue() && (!"true".equals(
                    bool(api.global("GetPluginConfigValue", plugin, "enabled")))
                    || !"true".equals(bool(api.global("GetPluginConfigValue", plugin, "video.restart_fix")))))
                throw new IllegalStateException("TS restart feature is disabled");
            String target = enabled.booleanValue() ? TS_PLAYER : "";
            api.ui(context, "SetProperty", TS_PROPERTY, target);
            current = String.valueOf(api.ui(context, "GetProperty", TS_PROPERTY, ""));
            if (!target.equals(current)) throw new IllegalStateException("TS hook readback differs");
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("context", context);
        result.put("playerClass", current);
        result.put("adapterSelected", Boolean.valueOf(TS_PLAYER.equals(current)));
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
    private static String value(String key, Object value) {
        if (!"dvd.owned_mode".equals(key)) return bool(value);
        if ("native".equals(value) || "copy".equals(value) || "transcode".equals(value))
            return value.toString();
        throw new IllegalArgumentException("Only native, copy or transcode DVD mode allowed");
    }
}
