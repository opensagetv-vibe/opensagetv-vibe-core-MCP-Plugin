package org.opensagetv.vibe.coremcp;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bounded public-plugin commissioning used by compatibility gates.
 *
 * <p>The caller cannot provide a URL, path, class, API name, or arbitrary
 * plugin identifier.  Only reviewed plugin IDs are accepted, installation is
 * version-pinned and explicit, and STVI installation runs in an existing UI
 * context so SageTV owns the import and rollback metadata.</p>
 */
final class PluginCommissioner {
    static final String SAGEMC_ID = "opensagetv-vibe-sagemc";
    private static final Set<String> ALLOWED = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList("AutoAspect", "nielm_autoaspect", SAGEMC_ID)));
    private final CompanionSettings.Api api;

    PluginCommissioner(CompanionSettings.Api api) { this.api = api; }

    synchronized Map<String, Object> available(String id, boolean refresh) {
        requireAllowed(id);
        if (refresh) api.global("RefreshAvailablePlugins");
        Object plugin = api.global("GetAvailablePluginForID", id);
        Map<String, Object> out = identity(id);
        out.put("available", Boolean.valueOf(plugin != null));
        if (plugin != null) {
            Metadata metadata = metadata(id, plugin);
            out.put("availableVersion", metadata.version);
            out.put("pluginType", metadata.type);
            Boolean compatible = strictBoolean(api.global("IsPluginCompatible", plugin), "compatibility");
            out.put("compatible", compatible);
            if (!compatible.booleanValue())
                out.put("incompatibilityReason", String.valueOf(api.global("GetPluginIncompatabilityReason", plugin)));
        }
        if (SAGEMC_ID.equals(id)) {
            Object current = findInstalled(id);
            out.put("installed", Boolean.valueOf(current != null));
            if (current != null) out.put("installedVersion", metadata(id, current).version);
        }
        return out;
    }

    synchronized Map<String, Object> install(String context, String id,
            String expectedVersion, boolean confirm) {
        requireAllowed(id);
        if (!confirm) throw new IllegalArgumentException("Plugin commissioning requires confirm=true");
        // A full STV is installed globally, not imported/enabled into an
        // arbitrary current UI. The fixed ID/type and independently idle
        // server boundary are mandatory; no caller-supplied paths or APIs.
        if (SAGEMC_ID.equals(id)) return installSageMc(expectedVersion);
        requireContext(context);
        Object plugin = api.global("GetAvailablePluginForID", id);
        if (plugin == null) throw new IllegalStateException("Reviewed plugin is absent from available repository");
        Metadata metadata = metadata(id, plugin);
        if (!metadata.version.equals(expectedVersion))
            throw new IllegalStateException("Available plugin version changed since checkpoint");
        if (!strictBoolean(api.ui(context, "IsPluginCompatible", plugin), "compatibility").booleanValue())
            throw new IllegalStateException("Reviewed plugin is incompatible with this UI/server");
        Object result = api.ui(context, "InstallPlugin", plugin);
        if (!("OK".equals(result) || "RESTART".equals(result)))
            throw new IllegalStateException("Plugin installation failed: " + String.valueOf(result));
        boolean restart = "RESTART".equals(result);
        boolean reloaded = false;
        boolean enabled = false;
        if (!restart) {
            Object installed = installed(id);
            metadata(id, installed);
            // STVI enablement belongs to a UIManager, not the server-global
            // plugin registry. A global EnablePlugin call receives no UIManager
            // and can return true without setting this client's enabled flag.
            // Keep the exact caller context for both mutation and readback.
            enabled = strictBoolean(pluginState(context, metadata.type,
                    "IsPluginEnabled", installed), "enabled").booleanValue();
            if (!enabled && "STVI".equals(metadata.type)) {
                if (!Boolean.TRUE.equals(pluginState(context, metadata.type,
                        "EnablePlugin", installed)))
                    throw new IllegalStateException("Installed plugin could not be enabled");
            }
            if ("STVI".equals(metadata.type)) {
                // Rebuild this UI's STV after enabling the import, following
                // the stock Plugin Manager order, then verify that UI's state.
                Object currentStv = api.ui(context, "GetCurrentSTVFile");
                if (!(currentStv instanceof String) || ((String) currentStv).length() == 0)
                    throw new IllegalStateException("Current STV path is unavailable");
                if (!Boolean.TRUE.equals(api.ui(context, "LoadSTVFile", currentStv)))
                    throw new IllegalStateException("STVI installed but UI reload failed");
                reloaded = true;
            }
            installed = installed(id);
            enabled = strictBoolean(pluginState(context, metadata.type,
                    "IsPluginEnabled", installed), "enabled readback").booleanValue();
            // Library dependencies are passive in stock Core. They have no
            // enable/disable lifecycle, so IsPluginEnabled is false even when
            // installed correctly. Require activation only for a UI import.
            if (!enabled && "STVI".equals(metadata.type))
                throw new IllegalStateException("Installed plugin enable readback failed");
        }
        Map<String, Object> out = identity(id);
        out.put("installedVersion", metadata.version);
        out.put("pluginType", metadata.type);
        out.put("installResult", result);
        out.put("restartRequired", Boolean.valueOf(restart));
        out.put("restarted", Boolean.FALSE);
        out.put("uiReloaded", Boolean.valueOf(reloaded));
        out.put("enabledReadback", Boolean.valueOf(enabled));
        out.put("activationRequired", Boolean.valueOf("STVI".equals(metadata.type)));
        return out;
    }

    private Object pluginState(String context, String type, String name, Object plugin) {
        return "STVI".equals(type) ? api.ui(context, name, plugin) : api.global(name, plugin);
    }

    private Object installed(String id) {
        Object found = findInstalled(id);
        if (found == null) throw new IllegalStateException("Installed plugin metadata is unavailable");
        return found;
    }

    private Object findInstalled(String id) {
        Object values = api.global("GetInstalledPlugins"), found = null;
        if (!(values instanceof Object[])) throw new IllegalStateException("Installed plugin status unavailable");
        for (Object value : (Object[]) values) {
            if (!id.equalsIgnoreCase(String.valueOf(api.global("GetPluginIdentifier", value)))) continue;
            if (found != null) throw new IllegalStateException("Ambiguous installed plugin: " + id);
            found = value;
        }
        return found;
    }

    private Map<String, Object> installSageMc(String expectedVersion) {
        Object candidate = api.global("GetAvailablePluginForID", SAGEMC_ID);
        if (candidate == null) throw new IllegalStateException("SageMC development catalog is absent");
        Metadata checkpoint = metadata(SAGEMC_ID, candidate);
        if (!checkpoint.version.equals(expectedVersion))
            throw new IllegalStateException("Available SageMC version changed since checkpoint");
        if (!strictBoolean(api.global("IsPluginCompatible", candidate), "compatibility").booleanValue())
            throw new IllegalStateException("SageMC is incompatible with this server");
        for (String name : new String[]{"GetCurrentlyRecordingMediaFiles", "GetUIContextNames", "GetConnectedClients"}) {
            Object values = api.global(name);
            if (!(values instanceof Object[]) || ((Object[]) values).length != 0)
                throw new IllegalStateException("SageMC installation requires independently idle recording/client status");
        }
        Object result = api.global("InstallPlugin", candidate);
        if (!("OK".equals(result) || "RESTART".equals(result)))
            throw new IllegalStateException("SageMC installation failed: " + String.valueOf(result));
        Metadata current = metadata(SAGEMC_ID, installed(SAGEMC_ID));
        if (!expectedVersion.equals(current.version))
            throw new IllegalStateException("Installed SageMC version differs from checkpoint");
        Map<String, Object> out = identity(SAGEMC_ID);
        out.put("installedVersion", current.version);
        out.put("pluginType", current.type);
        out.put("installResult", result);
        out.put("restartRequired", Boolean.valueOf("RESTART".equals(result)));
        out.put("restarted", Boolean.FALSE);
        out.put("uiReloaded", Boolean.FALSE);
        out.put("activationRequired", Boolean.TRUE);
        // Full STV selection is deliberately left to the user's UI setup.
        // Passive STVs must never be falsely reported as enabled Standard/STVI.
        return out;
    }

    private void requireContext(String context) {
        Object values = api.global("GetUIContextNames");
        if (!(values instanceof Object[])) throw new IllegalStateException("UI context status unavailable");
        for (Object value : (Object[]) values)
            if (context.equals(String.valueOf(value))) return;
        throw new IllegalArgumentException("Unknown SageTV UI context: " + context);
    }

    private Metadata metadata(String expectedId, Object plugin) {
        String actualId = String.valueOf(api.global("GetPluginIdentifier", plugin));
        if (!expectedId.equalsIgnoreCase(actualId)) throw new IllegalStateException("Unexpected plugin identity");
        String type = String.valueOf(api.global("GetPluginType", plugin));
        if (SAGEMC_ID.equals(expectedId) ? !"STV".equals(type)
                : !("STVI".equals(type) || "Library".equals(type)))
            throw new IllegalStateException("Unexpected plugin type: " + type);
        String version = String.valueOf(api.global("GetPluginVersion", plugin));
        if (!version.matches("[0-9]+(?:\\.[0-9]+)+"))
            throw new IllegalStateException("Unexpected plugin version: " + version);
        return new Metadata(type, version);
    }

    private static void requireAllowed(String id) {
        if (!ALLOWED.contains(id)) throw new IllegalArgumentException("Plugin is not allowlisted: " + id);
    }

    private static Boolean strictBoolean(Object value, String label) {
        if (!(value instanceof Boolean)) throw new IllegalStateException("Plugin " + label + " status unavailable");
        return (Boolean) value;
    }

    private static Map<String, Object> identity(String id) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("pluginId", id);
        return out;
    }

    private static final class Metadata {
        final String type, version;
        Metadata(String type, String version) { this.type = type; this.version = version; }
    }
}
