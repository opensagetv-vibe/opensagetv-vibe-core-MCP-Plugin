package org.opensagetv.vibe.coremcp;

import java.util.HashMap;
import java.util.Map;

public final class CompanionSettingsTest {
    static final String UI = "444556303031", OTHER = "444556303032";
    static final class Api implements CompanionSettings.Api {
        final Object plugin = new Object();
        final Map<String, String> settings = new HashMap<String, String>();
        final Map<String, String> players = new HashMap<String, String>();
        final Map<String, String> tsPlayers = new HashMap<String, String>();
        final Map<String, String> captionPlayers = new HashMap<String, String>();
        boolean available = true, refuse, duplicate, missingOption, pluginDisabled;
        int writes;
        String importedTitle="Before";
        String bookmark="";
        Api() { settings.put("enabled", "false"); settings.put("reload.recovery", "false"); settings.put("diagnostics.enabled", "false"); settings.put("video.restart_fix", "false"); settings.put("video.caption_bridge", "false"); settings.put("dvd.owned_navigation", "false"); }
        public Object global(String name, Object... args) {
            if(name.equals("GetServerProperty")) {
                check(args.length==2 && args[0].equals("vibe/client_extension/owned_bookmark/NDQ0NTU2MzAzMDMx")
                        && args[1].equals(""),"fixed selected UI bookmark key only");
                return bookmark;
            }
            if(name.equals("GetMediaFileForID"))return Integer.valueOf(1).equals(args[0]) ? "import" : null;
            if(name.equals("GetMediaFileMetadata"))return importedTitle;
            if(name.equals("IsLibraryFile"))return true;
            if(name.equals("IsTVFile")||name.equals("IsFileCurrentlyRecording"))return false;
            if (name.equals("GetInstalledPlugins")) return !available ? new Object[0]
                    : duplicate ? new Object[]{plugin, plugin} : new Object[]{plugin};
            if (name.equals("GetUIContextNames")) return new String[]{UI, OTHER};
            if (name.equals("GetPluginIdentifier")) return CompanionSettings.ID;
            check(args[0] == plugin, "typed installed plugin only");
            if (name.equals("IsPluginEnabled")) return !pluginDisabled;
            if (name.equals("GetPluginConfigSettings")) return missingOption
                    ? new String[]{"enabled"} : new String[]{"enabled", "reload.recovery", "trace.dvd", "dvd.tv_skip_keys", "diagnostics.enabled", "video.restart_fix", "video.caption_bridge", "video.state_bridge", "dvd.client_controls", "dvd.owned_navigation", "dvd.owned_mode", "dvd.skip_menus", "dvd.skip_previews", "action.dvd_save_resume", "action.dvd_resume", "action.dvd_beginning", "action.dvd_clear_resume", "action.metadata_preview"};
            if (name.equals("GetPluginConfigValue")) return settings.get(args[1]);
            if (name.equals("SetPluginConfigValue")) {
                writes++; if (refuse) return "refused";
                if(String.valueOf(args[1]).startsWith("action.dvd_"))settings.put("status","Queued explicit owned DVD resume");
                if("action.metadata_preview".equals(args[1]))settings.put("status","Preview ID=1");
                if("action.metadata_apply".equals(args[1])){importedTitle=settings.get("metadata.value");settings.put("status","Applied ID=1");}
                settings.put((String)args[1], (String)args[2]); return null;
            }
            throw new AssertionError(name);
        }
        public Object ui(String context, String name, Object... args) {
            String property=String.valueOf(args[0]);
            check(property.matches("media_player_plugin_class/(dvd|ts|mpg|mpeg|m2ts|mkv|mp4)"), "fixed media suffixes only");
            Map<String, String> target = CompanionSettings.TS_PROPERTY.equals(property) ? tsPlayers
                    : CompanionSettings.PROPERTY.equals(property) ? players : captionPlayers;
            String key=target==captionPlayers ? context+":"+property : context;
            if (name.equals("GetProperty")) return target.containsKey(key)
                    ? target.get(key) : args[1];
            if (name.equals("SetProperty")) { writes++; target.put(key, (String)args[1]); return null; }
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
        check(Boolean.FALSE.equals(service.dvdBookmark(UI).get("present")),"absent checkpoint");
        int writesBefore=api.writes;
        api.bookmark="cHJpdmF0ZS1kaXNj:2:12345";
        Map<String,Object> checkpoint=service.dvdBookmark(UI);
        check(Boolean.TRUE.equals(checkpoint.get("present")) && Boolean.TRUE.equals(checkpoint.get("valid"))
                && Long.valueOf(12345).equals(checkpoint.get("positionMs")),"bounded numeric checkpoint");
        check(!checkpoint.toString().contains("cHJpdmF0ZS") && !checkpoint.toString().contains("private-disc"),"disc never exposed");
        String fingerprint=String.valueOf(checkpoint.get("fingerprint"));
        check(fingerprint.matches("[a-f0-9]{64}"),"fixed SHA-256 fingerprint");
        api.bookmark="malformed";
        check(Boolean.TRUE.equals(service.dvdBookmark(UI).get("present"))
                && Boolean.FALSE.equals(service.dvdBookmark(UI).get("valid")),"malformed is present, never absent");
        api.bookmark=new String(new char[8193]);rejects(()->service.dvdBookmark(UI));
        api.bookmark="";
        rejects(()->service.dvdBookmark("ffffffffffff"));rejects(()->service.dvdBookmark("LocalUI"));
        api.pluginDisabled=true;rejects(()->service.dvdBookmark(UI));api.pluginDisabled=false;
        api.missingOption=true;rejects(()->service.dvdBookmark(UI));api.missingOption=false;
        check(api.writes==writesBefore,"all bookmark inspection/negative paths read-only");
        api.settings.put("dvd.client_controls","false");
        service.set("dvd.client_controls","false","true",true);
        rejects(()->service.set("dvd.client_controls","false","false",true));
        service.set("dvd.client_controls","true","false",true);
        api.settings.put("video.state_bridge","false");
        check(service.metadataGet(1,"Title").equals("Before"),"bounded imported field inspection");
        rejects(()->service.metadataGet(1,"Format.Container"));
        rejects(()->service.metadataGet(999,"Title"));
        service.set("video.state_bridge","false","true",true);
        service.set("video.state_bridge","true","false",true);
        api.settings.put("target.client",OTHER);
        rejects(()->service.dvdIntent(UI,"resume",false));
        rejects(()->service.dvdIntent(UI,"arbitrary",true));
        rejects(()->service.dvdIntent("ffffffffffff","resume",true));
        service.dvdIntent(UI,"resume",true);
        check(OTHER.equals(api.settings.get("target.client")),"DVD intent restores selected UI");
        api.settings.put("metadata.id","");api.settings.put("metadata.field","Title");api.settings.put("metadata.value","");
        check(service.metadataReview(1,"Title","Before","After",false).startsWith("Preview"),"metadata preview");
        check(api.importedTitle.equals("Before"),"no metadata write without confirmation");
        rejects(()->service.metadataReview(1,"Format.Container","Before","After",true));
        rejects(()->service.metadataReview(1,"Title","Stale","After",true));
        service.metadataReview(1,"Title","Before","After",true);
        check(api.importedTitle.equals("After")&&api.settings.get("metadata.id").isEmpty(),"confirmed fixed field and config restore");
        api.settings.put("dvd.owned_mode", "native");
        api.settings.put("dvd.skip_menus", "false");
        check(service.get("dvd.owned_mode").equals("native"), "owned mode is an explicit fixed choice");
        rejects(() -> service.set("dvd.owned_mode", "native", "copy", false));
        rejects(() -> service.set("dvd.owned_mode", "native", "arbitrary", true));
        service.set("dvd.owned_mode", "native", "copy", true);
        rejects(() -> service.set("dvd.owned_mode", "native", "transcode", true));
        service.set("dvd.owned_mode", "copy", "transcode", true);
        service.set("dvd.owned_mode", "transcode", "native", true);
        check(service.get("enabled").equals("false"), "effective default");
        check(service.get("diagnostics.enabled").equals("false"), "diagnostic opt-in default");
        rejects(() -> service.set("diagnostics.enabled", "false", "true", false));
        service.set("diagnostics.enabled", "false", "true", true);
        rejects(() -> service.set("diagnostics.enabled", "false", "false", true));
        service.set("diagnostics.enabled", "true", "false", true);
        rejects(() -> service.get("diagnostics.port"));
        api.writes = 0;
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
        rejects(() -> service.set("dvd.owned_navigation", "false", "true", false));
        service.set("dvd.owned_navigation", "false", "true", true);
        service.hook(UI, CompanionSettings.PLAYER, true, true);
        check(api.players.get(UI).equals(CompanionSettings.OWNED_PLAYER), "owned path requires explicit installed option");
        rejects(() -> service.hook(UI, CompanionSettings.PLAYER, false, true));
        service.hook(UI, CompanionSettings.OWNED_PLAYER, false, true);
        service.set("dvd.owned_navigation", "true", "false", true);
        service.hook(UI, "", true, true);
        check(api.players.get(OTHER).equals("third.party.Player"), "other player unchanged");
        rejects(() -> service.hook(UI, "", false, true));
        service.set("enabled", "true", "false", true);
        service.hook(UI, CompanionSettings.PLAYER, false, true);
        check(api.players.get(UI).equals(""), "restoration allowed while feature off");
        api.available = false; rejects(() -> service.get("enabled")); api.available = true;
        api.pluginDisabled = true; rejects(() -> service.get("enabled")); api.pluginDisabled = false;
        api.missingOption = true; rejects(() -> service.get("reload.recovery")); api.missingOption = false;
        api.missingOption = true; rejects(() -> service.get("diagnostics.enabled")); api.missingOption = false;
        api.duplicate = true; rejects(() -> service.get("enabled")); api.duplicate = false;
        api.refuse = true; rejects(() -> service.set("enabled", "false", "true", true));
        api.refuse = false;
        check(service.get("enabled").equals("false"), "original state restored");
        check(service.get("video.restart_fix").equals("false"), "TS fix defaults off");
        service.tsHook(UI, null, null, false);
        rejects(() -> service.tsHook(UI, "", true, true));
        service.set("enabled", "false", "true", true);
        rejects(() -> service.tsHook(UI, "", true, true));
        service.set("video.restart_fix", "false", "true", true);
        rejects(() -> service.tsHook(UI, "", true, false));
        rejects(() -> service.tsHook("FFFFFFFFFFFF", "", true, true));
        api.tsPlayers.put(OTHER, "third.party.Player");
        rejects(() -> service.tsHook(OTHER, "third.party.Player", true, true));
        service.tsHook(UI, "", true, true);
        check(CompanionSettings.TS_PLAYER.equals(api.tsPlayers.get(UI)), "fixed TS class selected");
        check("third.party.Player".equals(api.tsPlayers.get(OTHER)), "other TS UI unchanged");
        rejects(() -> service.tsHook(UI, "", false, true));
        service.set("video.restart_fix", "true", "false", true);
        service.tsHook(UI, CompanionSettings.TS_PLAYER, false, true);
        check("".equals(api.tsPlayers.get(UI)), "TS restoration allowed when feature off");
        api.missingOption = true;
        rejects(() -> service.tsHook(UI, null, null, false));
        api.missingOption = false;
        service.set("video.caption_bridge","false","true",true);
        service.set("video.caption_bridge","true","false",true);
        api.settings.put("dvd.skip_previews","false");
        service.set("dvd.skip_previews","false","true",true);
        service.set("dvd.skip_previews","true","false",true);
        service.set("enabled", "true", "false", true);
        System.out.println("PASS: bounded companion setting/UI hook scope and restoration");
    }
}
