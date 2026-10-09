package org.opensagetv.vibe.coremcp;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Commission only the fixed Client Extension through SageTV's public plugin
 * manager. Callers cannot supply a plugin ID, URL, path, class or API name.
 * Installing never restarts SageTV: RESTART is reported for a separately
 * authorized, recording-aware restart. Stock Core/FFmpeg remain untouched.
 */
final class CompanionInstaller {
    private final CompanionSettings.Api api;

    CompanionInstaller(CompanionSettings.Api api) { this.api = api; }

    /** Read metadata; repository network refresh happens only when explicit. */
    synchronized Map<String, Object> available(boolean refresh) {
        if (refresh) api.global("RefreshAvailablePlugins");
        Object plugin = api.global("GetAvailablePluginForID", CompanionSettings.ID);
        Map<String, Object> result = identity();
        result.put("available", Boolean.valueOf(plugin != null));
        if (plugin != null) {
            String version = requireMetadata(plugin);
            result.put("availableVersion", version);
            result.put("compatible", strictBoolean(api.global("IsPluginCompatible", plugin), "compatibility"));
        }
        return result;
    }

    /** Installed state is read-only and does not depend on repository access. */
    synchronized Map<String, Object> status() {
        Map<String, Object> result = identity();
        Object installed = installed();
        result.put("installed", Boolean.valueOf(installed != null));
        if (installed != null) {
            result.put("installedVersion", requireMetadata(installed));
            result.put("enabled", strictBoolean(api.global("IsPluginEnabled", installed), "enabled"));
        }
        return result;
    }

    synchronized Map<String, Object> install(String expectedVersion, boolean confirm) {
        if (!confirm) throw new IllegalArgumentException("Companion install requires confirm=true");
        requireVersion(expectedVersion);
        Object candidate = api.global("GetAvailablePluginForID", CompanionSettings.ID);
        if (candidate == null) throw new IllegalStateException("Companion is absent from available plugin repository");
        if (!expectedVersion.equals(requireMetadata(candidate)))
            throw new IllegalStateException("Available companion version changed since checkpoint");
        if (!strictBoolean(api.global("IsPluginCompatible", candidate), "compatibility").booleanValue())
            throw new IllegalStateException("Available companion is incompatible with this server");
        // No interpretation of null/failed queries as idle. This cannot stop
        // a recording or a user's connected UI merely to satisfy a test.
        requireIdle("GetCurrentlyRecordingMediaFiles", "Recording");
        requireIdle("GetUIContextNames", "UI context");
        requireIdle("GetConnectedClients", "Connected client");
        Object installResult = api.global("InstallPlugin", candidate);
        if (!("OK".equals(installResult) || "RESTART".equals(installResult)))
            throw new IllegalStateException("Companion installation failed: " + String.valueOf(installResult));
        Object current = installed();
        if (current == null || !expectedVersion.equals(requireMetadata(current)))
            throw new IllegalStateException("Installed companion metadata differs from requested version");
        boolean restart = "RESTART".equals(installResult);
        // Do not start pending Java classes before a required restart. For an
        // immediately usable install, enable only its exact installed object.
        if (!restart && !strictBoolean(api.global("IsPluginEnabled", current), "enabled").booleanValue()) {
            if (!Boolean.TRUE.equals(api.global("EnablePlugin", current)))
                throw new IllegalStateException("Companion installation succeeded but enabling failed");
            if (!strictBoolean(api.global("IsPluginEnabled", current), "enabled").booleanValue())
                throw new IllegalStateException("Companion enable readback failed");
        }
        Map<String, Object> result = status();
        result.put("installResult", installResult);
        result.put("restartRequired", Boolean.valueOf(restart));
        result.put("restarted", Boolean.FALSE);
        return result;
    }

    private Map<String, Object> identity() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("pluginId", CompanionSettings.ID);
        return result;
    }

    private Object installed() {
        Object values = api.global("GetInstalledPlugins"), found = null;
        if (!(values instanceof Object[])) throw new IllegalStateException("Installed plugin status unavailable");
        for (Object value : (Object[]) values) {
            if (!CompanionSettings.ID.equalsIgnoreCase(String.valueOf(api.global("GetPluginIdentifier", value)))) continue;
            if (found != null) throw new IllegalStateException("Ambiguous installed companion");
            found = value;
        }
        return found;
    }

    private String requireMetadata(Object plugin) {
        if (!CompanionSettings.ID.equalsIgnoreCase(String.valueOf(api.global("GetPluginIdentifier", plugin)))
                || !"Standard".equals(api.global("GetPluginType", plugin)))
            throw new IllegalStateException("Unexpected companion plugin identity/type");
        Object raw = api.global("GetPluginVersion", plugin);
        String version = raw == null ? null : raw.toString();
        requireVersion(version);
        return version;
    }

    private static void requireVersion(String version) {
        if (version == null || !version.matches("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))
            throw new IllegalArgumentException("Exact numeric companion version required");
    }

    private void requireIdle(String name, String label) {
        Object value = api.global(name);
        if (!(value instanceof Object[])) throw new IllegalStateException(label + " status unknown; install refused");
        if (((Object[]) value).length != 0) throw new IllegalStateException(label + " active; install refused");
    }

    private static Boolean strictBoolean(Object value, String label) {
        if (!(value instanceof Boolean)) throw new IllegalStateException("Companion " + label + " status unavailable");
        return (Boolean) value;
    }
}
