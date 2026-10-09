package org.opensagetv.vibe.coremcp;

import java.util.Map;

/** Fixed-ID public plugin manager contract; no real server or registry edits. */
public final class CompanionInstallerTest {
    static final class Api implements CompanionSettings.Api {
        final Object candidate = new Object(), current = new Object();
        boolean available = true, installed, enabled, compatible = true, badId, badType, duplicate;
        boolean enableRefused, badReadback, unknownInstalled;
        String version = "0.1.0", installedVersion = "0.1.0", installResult = "OK";
        Object recordings = new Object[0], contexts = new String[0], clients = new String[0];
        int installs, enables, refreshes;
        public Object global(String name, Object... args) {
            if (name.equals("RefreshAvailablePlugins")) { refreshes++; return null; }
            if (name.equals("GetAvailablePluginForID")) {
                check(args.length == 1 && CompanionSettings.ID.equals(args[0]), "fixed ID only");
                return available ? candidate : null;
            }
            if (name.equals("GetInstalledPlugins")) return unknownInstalled ? null : !installed ? new Object[0]
                    : duplicate ? new Object[]{current, current} : new Object[]{current};
            if (name.equals("GetCurrentlyRecordingMediaFiles")) return recordings;
            if (name.equals("GetUIContextNames")) return contexts;
            if (name.equals("GetConnectedClients")) return clients;
            check(args.length == 1 && (args[0] == candidate || args[0] == current), "typed plugin metadata only");
            if (name.equals("GetPluginIdentifier")) return badId ? "another-plugin" : CompanionSettings.ID;
            if (name.equals("GetPluginType")) return badType ? "STVI" : "Standard";
            if (name.equals("GetPluginVersion")) return args[0] == candidate ? version : installedVersion;
            if (name.equals("IsPluginCompatible")) return compatible;
            if (name.equals("IsPluginEnabled")) { check(args[0] == current, "installed enabled read"); return enabled; }
            if (name.equals("InstallPlugin")) {
                check(args[0] == candidate, "available candidate install only"); installs++;
                if (installResult.equals("OK") || installResult.equals("RESTART")) installed = true;
                return installResult;
            }
            if (name.equals("EnablePlugin")) {
                check(args[0] == current, "exact installed enable only"); enables++;
                if (!enableRefused && !badReadback) enabled = true;
                return !enableRefused;
            }
            throw new AssertionError("Unexpected API " + name);
        }
        public Object ui(String context, String name, Object... args) { throw new AssertionError("No UI mutation allowed"); }
    }
    static void check(boolean valid, String why) { if (!valid) throw new AssertionError(why); }
    static void rejects(Runnable work) {
        try { work.run(); throw new AssertionError("Expected refusal"); }
        catch (IllegalArgumentException | IllegalStateException expected) { }
    }
    public static void main(String[] args) {
        Api api = new Api(); CompanionInstaller service = new CompanionInstaller(api);
        check(Boolean.TRUE.equals(service.available(false).get("available")), "candidate visible");
        check(Boolean.FALSE.equals(service.status().get("installed")), "absent status valid");
        check(api.installs == 0 && api.enables == 0 && api.refreshes == 0, "inspection read-only");
        service.available(true); check(api.refreshes == 1, "explicit refresh only");
        rejects(() -> service.install("0.1.0", false));
        rejects(() -> service.install("https://example.invalid/plugin.zip", true));
        rejects(() -> service.install("0.1.1", true));
        api.available = false; check(Boolean.FALSE.equals(service.available(false).get("available")), "absent repository valid");
        rejects(() -> service.install("0.1.0", true)); api.available = true;
        api.badId = true; rejects(() -> service.install("0.1.0", true)); api.badId = false;
        api.badType = true; rejects(() -> service.install("0.1.0", true)); api.badType = false;
        api.compatible = false; rejects(() -> service.install("0.1.0", true)); api.compatible = true;
        api.recordings = new Object[]{new Object()}; rejects(() -> service.install("0.1.0", true)); api.recordings = new Object[0];
        api.contexts = null; rejects(() -> service.install("0.1.0", true)); api.contexts = new String[0];
        api.clients = new String[]{"viewer"}; rejects(() -> service.install("0.1.0", true)); api.clients = new String[0];
        check(api.installs == 0 && api.enables == 0, "negative guards precede writes");
        Map<String, Object> result = service.install("0.1.0", true);
        check(api.installs == 1 && api.enables == 1 && api.enabled, "stock install and exact enable");
        check(Boolean.FALSE.equals(result.get("restartRequired")) && Boolean.FALSE.equals(result.get("restarted")), "never restarts");
        api.installResult = "RESTART"; api.enabled = false;
        result = service.install("0.1.0", true);
        check(Boolean.TRUE.equals(result.get("restartRequired")) && api.enables == 1, "pending Java enable deferred");
        api.installResult = "FAILED - TEST"; rejects(() -> service.install("0.1.0", true));
        api.installResult = "OK"; api.enableRefused = true; rejects(() -> service.install("0.1.0", true));
        api.enableRefused = false; api.badReadback = true; rejects(() -> service.install("0.1.0", true)); api.badReadback = false;
        api.installedVersion = "0.1.1"; rejects(() -> service.install("0.1.0", true)); api.installedVersion = "0.1.0";
        api.duplicate = true; rejects(() -> service.status()); api.duplicate = false;
        api.unknownInstalled = true; rejects(() -> service.status());
        System.out.println("PASS: fixed companion installer public API, idle, metadata and restart-defer guards");
    }
}
