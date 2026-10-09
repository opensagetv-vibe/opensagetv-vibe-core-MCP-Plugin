package org.opensagetv.vibe.coremcp;

import java.util.HashMap;
import java.util.Map;

public final class CompanionSettingsTest {
    static final String UI = "444556303031", OTHER = "444556303032";
    static final class Api implements CompanionSettings.Api {
        final Object plugin = new Object();
        final Map<String, String> settings = new HashMap<String, String>();
        final Map<String, String> players = new HashMap<String, String>();
        boolean available = true, refuse, duplicate, missingOption, pluginDisabled;
        int writes;
        Api() { settings.put("enabled", "false"); settings.put("reload.recovery", "false"); }
        public Object global(String name, Object... args) {
            if (name.equals("GetInstalledPlugins")) return !available ? new Object[0]
                    : duplicate ? new Object[]{plugin, plugin} : new Object[]{plugin};
            if (name.equals("GetUIContextNames")) return new String[]{UI, OTHER};
            if (name.equals("GetPluginIdentifier")) return CompanionSettings.ID;
            check(args[0] == plugin, "typed installed plugin only");
            if (name.equals("IsPluginEnabled")) return !pluginDisabled;
            if (name.equals("GetPluginConfigSettings")) return missingOption
                    ? new String[]{"enabled"} : new String[]{"enabled", "reload.recovery", "trace.dvd", "dvd.tv_skip_keys"};
            if (name.equals("GetPluginConfigValue")) return settings.get(args[1]);
            if (name.equals("SetPluginConfigValue")) {
                writes++; if (refuse) return "refused";
                settings.put((String)args[1], (String)args[2]); return null;
            }
            throw new AssertionError(name);
        }
        public Object ui(String context, String name, Object... args) {
            check(CompanionSettings.PROPERTY.equals(args[0]), "fixed DVD property only");
            if (name.equals("GetProperty")) return players.containsKey(context)
                    ? players.get(context) : args[1];
            if (name.equals("SetProperty")) { writes++; players.put(context, (String)args[1]); return null; }
            throw new AssertionError(name);
        }
    }
    static void check(boolean valid, String why) { if (!valid) throw new AssertionError(why); }
    static void rejects(Runnable call) {
        try { call.run(); throw new AssertionError("Expected rejection"); }
        catch (IllegalArgumentException | IllegalStateException expected) { }
    }
    public static void main(String[] args) {
        Api api = new Api(); CompanionSettings service = new CompanionSettings(api);
        check(service.get("enabled").equals("false"), "effective default");
        rejects(() -> CompanionSettings.enabledValue("arbitrary"));
        check(CompanionSettings.enabledValue("true").booleanValue(), "strict hook Boolean");
        service.hook(UI, null, null, false); check(api.writes == 0, "inspection is read-only");
        rejects(() -> service.hook(UI, "", true, true));
        rejects(() -> service.set("password", "false", "true", true));
        rejects(() -> service.set("enabled", "false", "true", false));
        service.set("enabled", "false", "true", true);
        rejects(() -> service.set("enabled", "false", "false", true));
        api.players.put(OTHER, "third.party.Player");
        rejects(() -> service.hook(OTHER, "third.party.Player", true, true));
        rejects(() -> service.hook("LocalUI", "", true, true));
        rejects(() -> service.hook("FFFFFFFFFFFF", "", true, true));
        service.hook(UI, "", true, true);
        check(api.players.get(UI).equals(CompanionSettings.PLAYER), "known adapter selected");
        check(api.players.get(OTHER).equals("third.party.Player"), "other player unchanged");
        rejects(() -> service.hook(UI, "", false, true));
        service.set("enabled", "true", "false", true);
        service.hook(UI, CompanionSettings.PLAYER, false, true);
        check(api.players.get(UI).equals(""), "restoration allowed while feature off");
        api.available = false; rejects(() -> service.get("enabled")); api.available = true;
        api.pluginDisabled = true; rejects(() -> service.get("enabled")); api.pluginDisabled = false;
        api.missingOption = true; rejects(() -> service.get("reload.recovery")); api.missingOption = false;
        api.duplicate = true; rejects(() -> service.get("enabled")); api.duplicate = false;
        api.refuse = true; rejects(() -> service.set("enabled", "false", "true", true));
        api.refuse = false;
        check(service.get("enabled").equals("false"), "original state restored");
        System.out.println("PASS: bounded companion setting/UI hook scope and restoration");
    }
}
