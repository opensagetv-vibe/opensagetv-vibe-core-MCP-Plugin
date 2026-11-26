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
            if (name.equals("GetPluginConfigSettings")) return new String[]{PluginCaptionSettings.SETTING,PluginCaptionSettings.RATE_SETTING,"action.runBitrateTest","runtimeBitrateTest.status","adaptiveTest.sessions","action.adaptiveTest"};
            if(args[1].equals("adaptiveTest.sessions"))return "[]";
            if(args[1].equals("action.adaptiveTest")){writes++;return refuse?"refused":null;}
            if(args[1].equals("runtimeBitrateTest.status"))return "Not run";
            if(args[1].equals("action.last"))return "running";
            if(args[1].equals("action.runBitrateTest")){writes++;return null;}
            check(args[1].equals(PluginCaptionSettings.SETTING)||args[1].equals(PluginCaptionSettings.RATE_SETTING),"fixed non-secret Boolean only");
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
        int before=api.writes;
        check(settings.bitrateTest(id,false,false).equals("Not run"),"diagnostic status read");
        rejected(()->settings.bitrateTest(id,true,false));
        check(api.writes==before,"diagnostic confirmation precedes write");
        check(settings.bitrateTest(id,true,true).equals("Not run")&&api.writes==before+1,"fixed settings diagnostic only");
        check(settings.get(id,PluginCaptionSettings.RATE_SETTING).equals("false"),"rate site allowlist read");
        check(settings.set(id,PluginCaptionSettings.RATE_SETTING,"false","true",true).equals("true"),"rate site confirmation/CAS");
        rejected(()->settings.set(id,"action.runBitrateTest","true","false",true));
        String handle="0123456789abcdef0123456789abcdef";
        before=api.writes;
        check(settings.adaptiveTest(id,"status",null,null,null,null,false).equals("[]"),"read only session discovery");
        rejected(()->settings.adaptiveTest(id,"override",handle,"0","2000","120",false));
        rejected(()->settings.adaptiveTest(id,"override",handle,"0","2000","301",true));
        rejected(()->settings.adaptiveTest(id,"override","bad","0","2000","120",true));
        rejected(()->settings.adaptiveTest(id,"override",handle,"0","249","120",true));
        rejected(()->settings.adaptiveTest("other","status",null,null,null,null,false));
        check(api.writes==before,"invalid adaptive commands never write");
        settings.adaptiveTest(id,"override",handle,"0","2000","120",true);
        settings.adaptiveTest(id,"clear",handle,"1",null,null,true);
        check(api.writes==before+2,"fixed override and clear only");
        api.refuse=true;rejected(()->settings.adaptiveTest(id,"override",handle,"2","2000","120",true));
        System.out.println("PASS: typed installed plugin Boolean scope/confirm/expected/readback/restore");
    }
}
