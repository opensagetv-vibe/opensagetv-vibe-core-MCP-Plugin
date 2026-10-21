package org.opensagetv.vibe.coremcp;

import java.util.Map;

public final class PluginCommissionerTest {
    static final class Api implements CompanionSettings.Api {
        final Object candidate = new Object(), installed = new Object();
        String id = "AutoAspect", type = "STVI", version = "1.3.4.2.2610151949";
        boolean available = true, compatible = true;
        boolean enabled, enableTakesEffect = true, idle = true, unknownActivity;
        String installResult = "OK";
        int refreshes, installs, enables, reloads;
        public Object global(String name, Object... args) {
            if (name.equals("RefreshAvailablePlugins")) { refreshes++; return null; }
            if (name.equals("GetAvailablePluginForID")) return available ? candidate : null;
            if (name.equals("GetUIContextNames"))
                return "STV".equals(type) ? new Object[0] : new String[]{"test-ui"};
            if (name.equals("GetCurrentlyRecordingMediaFiles") || name.equals("GetConnectedClients"))
                return unknownActivity ? null : (idle ? new Object[0] : new Object[]{new Object()});
            if (name.equals("InstallPlugin")) {
                check("STV".equals(type) && args[0] == candidate, "only fixed full STV installs globally");
                installs++; return installResult;
            }
            if (name.equals("GetInstalledPlugins")) return installs == 0 ? new Object[0] : new Object[]{installed};
            if (name.equals("GetPluginIdentifier")) return id;
            if (name.equals("GetPluginType")) return type;
            if (name.equals("GetPluginVersion")) return version;
            if (name.equals("IsPluginCompatible")) return Boolean.valueOf(compatible);
            if (name.equals("GetPluginIncompatabilityReason")) return "fixture dependency unavailable";
            if (name.equals("IsPluginEnabled")) {
                check("Library".equals(type), "STVI state must use its exact UI context");
                return Boolean.FALSE;
            }
            if (name.equals("EnablePlugin"))
                throw new AssertionError("Passive Library must not be enabled globally");
            throw new AssertionError("Unexpected API " + name);
        }
        public Object ui(String context, String name, Object... args) {
            check("test-ui".equals(context), "exact UI context");
            if ("IsPluginCompatible".equals(name)) return Boolean.valueOf(compatible);
            if ("InstallPlugin".equals(name)) {
                check(args.length == 1 && args[0] == candidate, "exact UI install");
                installs++; return "OK";
            }
            if ("GetCurrentSTVFile".equals(name)) return "/srv/STVs/SageTV7/SageTV7.xml";
            if ("LoadSTVFile".equals(name)) { reloads++; return Boolean.TRUE; }
            if ("IsPluginEnabled".equals(name)) return Boolean.valueOf(enabled);
            if ("EnablePlugin".equals(name)) {
                check(args[0] == installed, "installed object enabled");
                enables++; enabled = enableTakesEffect; return Boolean.TRUE;
            }
            throw new AssertionError("Unexpected UI API " + name);
        }
    }
    static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    static void rejects(Runnable work) {
        try { work.run(); throw new AssertionError("Expected refusal"); }
        catch (IllegalArgumentException | IllegalStateException expected) { }
    }
    public static void main(String[] args) {
        Api api = new Api(); PluginCommissioner service = new PluginCommissioner(api);
        Map<String, Object> state = service.available("AutoAspect", true);
        check(Boolean.TRUE.equals(state.get("available")) && api.refreshes == 1, "reviewed candidate visible");
        rejects(() -> service.available("not-reviewed", false));
        rejects(() -> service.install("test-ui", "AutoAspect", api.version, false));
        rejects(() -> service.install("missing-ui", "AutoAspect", api.version, true));
        rejects(() -> service.install("test-ui", "AutoAspect", "1.3.4.2", true));
        api.compatible = false;
        check("fixture dependency unavailable".equals(service.available("AutoAspect", false).get("incompatibilityReason")),
                "stock incompatibility reason is not hidden");
        rejects(() -> service.install("test-ui", "AutoAspect", api.version, true));
        api.compatible = true;
        state = service.install("test-ui", "AutoAspect", api.version, true);
        check(api.installs == 1 && api.enables == 1 && api.reloads == 1
                && "OK".equals(state.get("installResult")), "public UI install/enable/reload used");
        check(Boolean.FALSE.equals(state.get("restartRequired")), "no implicit restart");
        check(Boolean.TRUE.equals(state.get("uiReloaded")), "STVI reload recorded");
        check(Boolean.TRUE.equals(state.get("enabledReadback")),
                "selected UI confirms STVI enablement after reload");
        Api ineffective = new Api(); ineffective.enableTakesEffect = false;
        rejects(() -> new PluginCommissioner(ineffective).install(
                "test-ui", "AutoAspect", ineffective.version, true));
        Api library = new Api(); library.id = "nielm_autoaspect"; library.type = "Library";
        state = new PluginCommissioner(library).install("test-ui", library.id, library.version, true);
        check(library.installs == 1 && library.enables == 0 && library.reloads == 0,
                "passive Library installation has no activation lifecycle");
        check(Boolean.FALSE.equals(state.get("enabledReadback"))
                && Boolean.FALSE.equals(state.get("activationRequired")),
                "passive Library false enable readback is not an STVI activation success");
        Api stv = new Api(); stv.id = PluginCommissioner.SAGEMC_ID; stv.type = "STV";
        PluginCommissioner full = new PluginCommissioner(stv);
        rejects(() -> full.install("", stv.id, stv.version, false));
        rejects(() -> full.install("", stv.id, "0.0.0", true));
        stv.id = "other"; rejects(() -> full.available(PluginCommissioner.SAGEMC_ID, false));
        stv.id = PluginCommissioner.SAGEMC_ID;
        stv.type = "Standard"; rejects(() -> full.install("", stv.id, stv.version, true));
        stv.type = "STV"; stv.idle = false;
        rejects(() -> full.install("", stv.id, stv.version, true));
        stv.idle = true; stv.unknownActivity = true;
        rejects(() -> full.install("", stv.id, stv.version, true));
        stv.unknownActivity = false; stv.installResult = "RESTART";
        state = full.install("", stv.id, stv.version, true);
        check(stv.installs == 1 && stv.enables == 0 && stv.reloads == 0,
                "full STV installs only once, never enables/rebinds a UI");
        check(Boolean.TRUE.equals(state.get("restartRequired"))
                && Boolean.FALSE.equals(state.get("restarted")), "restart is reported, never executed");
        check(Boolean.TRUE.equals(full.available(stv.id, false).get("installed")), "installed STV readback");
        System.out.println("PASS: bounded reviewed-plugin commissioning uses public SageTV plugin API");
    }
}
