package org.opensagetv.vibe.coremcp;

/** No live server; typed public-API allowlist/concurrency/readback checks. */
public final class PluginCaptionSettingsTest {
    static final class Api implements PluginCaptionSettings.Api {
        final Object plugin=new Object();
        String value="false";
        boolean enabled=true, available=true, refuse;
        int writes;
        public Object call(String name,Object... args) {
            if (name.equals("GetInstalledPlugins")) return available ? new Object[]{plugin} : new Object[0];
            if (name.equals("GetPluginIdentifier")) return "SageTVFFmpegPluginLinux";
            check(args[0]==plugin,"typed installed object only");
            if (name.equals("IsPluginEnabled")) return enabled;
            if (name.equals("GetPluginConfigSettings")) return new String[]{PluginCaptionSettings.SETTING};
            check(args[1].equals(PluginCaptionSettings.SETTING),"one non-secret setting only");
            if (name.equals("GetPluginConfigValue")) return value;
            if (name.equals("SetPluginConfigValue")) {
                writes++; if (refuse) return "private error text";
                value=(String)args[2]; return null;
            }
            throw new AssertionError("Unexpected API "+name);
        }
    }
    static void check(boolean good,String why) { if (!good) throw new AssertionError(why); }
    static void rejected(Runnable request) {
        try { request.run(); throw new AssertionError("Expected rejection"); }
        catch (IllegalArgumentException | IllegalStateException expected) { }
    }
    public static void main(String[] args) {
        Api api=new Api(); PluginCaptionSettings settings=new PluginCaptionSettings(api);
        String id="sagetvffmpegpluginlinux", key=PluginCaptionSettings.SETTING;
        check(settings.get(id,key).equals("false"),"read effective Boolean");
        check(settings.set(id,key,"false","true",true).equals("true"),"confirmed set/readback");
        rejected(() -> settings.set(id,key,"false","false",true));
        rejected(() -> settings.set(id,key,"true","false",false));
        rejected(() -> settings.set(id,key,"true","password",true));
        rejected(() -> settings.get("arbitraryplugin",key));
        rejected(() -> settings.get(id,"caption_side_channel.api_port"));
        check(api.writes==1,"invalid/stale requests never mutate");
        api.enabled=false; rejected(() -> settings.get(id,key)); api.enabled=true;
        api.available=false; rejected(() -> settings.get(id,key)); api.available=true;
        api.refuse=true; rejected(() -> settings.set(id,key,"true","false",true));
        api.refuse=false;
        check(settings.set(id,key,"true","false",true).equals("false"),"checkpoint restore");
        System.out.println("PASS: typed installed plugin Boolean scope/confirm/expected/readback/restore");
    }
}
