package org.opensagetv.vibe.coremcp;

import java.net.InetAddress;
import java.util.Map;

import sage.SageTVEventListener;
import sage.SageTVPlugin;
import sage.SageTVPluginRegistry;

public final class SageTVCoreMcpPlugin implements SageTVPlugin {
    public static final String NAME = "OpenSageTV Vibe Core MCP Plugin";
    public static final String VERSION = "0.1.1";
    private static final String[] SETTINGS = {
            "enabled", "bind_address", "port", "allow_lan", "token",
            "status", "action.restart", "action.rotate_token"
    };

    private volatile BridgeHttpServer bridge;
    private volatile String status = "stopped";

    public SageTVCoreMcpPlugin(SageTVPluginRegistry registry) { this(registry, false); }
    public SageTVCoreMcpPlugin(SageTVPluginRegistry registry, boolean reset) {
        if (reset) resetConfig();
    }

    public synchronized void start() {
        stopBridge();
        if (!PluginConfig.bool("enabled", true)) {
            status = "disabled";
            log(status);
            return;
        }
        String bind = PluginConfig.get("bind_address", "127.0.0.1").trim();
        boolean allowLan = PluginConfig.bool("allow_lan", false);
        if (!allowLan && !isLoopback(bind)) {
            status = "refused: LAN bind requires allow_lan=true";
            log(status);
            return;
        }
        int port = PluginConfig.integer("port", 8270, 1024, 65535);
        String token = PluginConfig.ensureToken();
        try {
            BridgeHttpServer created = new BridgeHttpServer(bind, port, token, new ControlService());
            created.start();
            bridge = created;
            status = "listening on " + bind + ":" + port;
        } catch (Exception error) {
            status = "failed: " + error.getClass().getSimpleName() + ": " + error.getMessage();
        }
        log(status);
    }

    public synchronized void stop() { stopBridge(); status = "stopped"; }
    public void destroy() { stop(); }
    public void sageEvent(String eventName, Map eventVars) { }

    public String[] getConfigSettings() { return SETTINGS.clone(); }
    public String getConfigValue(String setting) {
        if ("enabled".equals(setting)) return PluginConfig.get("enabled", "true");
        if ("bind_address".equals(setting)) return PluginConfig.get("bind_address", "127.0.0.1");
        if ("port".equals(setting)) return PluginConfig.get("port", "8270");
        if ("allow_lan".equals(setting)) return PluginConfig.get("allow_lan", "false");
        if ("token".equals(setting)) return PluginConfig.ensureToken();
        if ("status".equals(setting)) return status;
        return "";
    }
    public String[] getConfigValues(String setting) { return null; }
    public int getConfigType(String setting) {
        if ("enabled".equals(setting) || "allow_lan".equals(setting)) return CONFIG_BOOL;
        if ("port".equals(setting)) return CONFIG_INTEGER;
        if ("token".equals(setting)) return CONFIG_PASSWORD;
        if (setting != null && setting.startsWith("action.")) return CONFIG_BUTTON;
        return CONFIG_TEXT;
    }
    public void setConfigValue(String setting, String value) {
        if ("enabled".equals(setting) || "bind_address".equals(setting) || "port".equals(setting) ||
                "allow_lan".equals(setting) || "token".equals(setting)) {
            PluginConfig.set(setting, value);
            status = "configuration changed; restart bridge or plugin to apply";
        } else if ("action.restart".equals(setting)) {
            start();
        } else if ("action.rotate_token".equals(setting)) {
            PluginConfig.set("token", "");
            PluginConfig.ensureToken();
            start();
        }
    }
    public void setConfigValues(String setting, String[] values) { }
    public String[] getConfigOptions(String setting) { return null; }
    public String getConfigHelpText(String setting) {
        if ("bind_address".equals(setting)) return "Defaults to 127.0.0.1. Set allow_lan=true before using a non-loopback bind such as 0.0.0.0.";
        if ("allow_lan".equals(setting)) return "Explicitly permits a non-loopback listener. Use only on a trusted network with a unique token.";
        if ("token".equals(setting)) return "Bearer token required by every control request. Generated automatically and stored in Sage.properties.";
        if ("action.rotate_token".equals(setting)) return "Generate a new token immediately; existing MCP configuration will stop working.";
        return "Restart the bridge after changing listener settings.";
    }
    public String getConfigLabel(String setting) {
        if ("enabled".equals(setting)) return "Enable Control Bridge";
        if ("bind_address".equals(setting)) return "Bind Address";
        if ("port".equals(setting)) return "Port";
        if ("allow_lan".equals(setting)) return "Allow LAN Listener";
        if ("token".equals(setting)) return "Bearer Token";
        if ("status".equals(setting)) return "Bridge Status";
        if ("action.restart".equals(setting)) return "Restart Bridge";
        if ("action.rotate_token".equals(setting)) return "Rotate Token";
        return setting;
    }
    public void resetConfig() {
        PluginConfig.set("enabled", "true");
        PluginConfig.set("bind_address", "127.0.0.1");
        PluginConfig.set("port", "8270");
        PluginConfig.set("allow_lan", "false");
        PluginConfig.set("token", "");
        status = "reset; restart required";
    }

    private synchronized void stopBridge() {
        BridgeHttpServer current = bridge;
        bridge = null;
        if (current != null) current.stop();
    }

    private static boolean isLoopback(String bind) {
        try { return InetAddress.getByName(bind).isLoopbackAddress(); }
        catch (Exception error) { return false; }
    }

    static void log(String message) { System.out.println("[VibeCoreMCP] " + message); }
}
