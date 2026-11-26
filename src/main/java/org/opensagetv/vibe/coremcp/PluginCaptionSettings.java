package org.opensagetv.vibe.coremcp;

import java.util.Locale;

/**
 * Commissioning-only control for the FFmpeg plugin caption listener. Uses a
 * typed installed Plugin from public APIs, never arbitrary expressions or a
 * direct INI write. Only one non-secret Boolean setting is exposed. Caller
 * checkpoints it and supplies the expected current value before any change.
 */
final class PluginCaptionSettings {
    interface Api { Object call(String name,Object... arguments); }
    private final Api api;
    static final String SETTING="caption_side_channel.enabled";
    static final String RATE_SETTING="stdinctrl.runtime_bitrate_control";
    PluginCaptionSettings(Api api) { this.api=api; }

    String get(String pluginId,String setting) {
        if(!SETTING.equals(setting)&&!RATE_SETTING.equals(setting))throw new IllegalArgumentException("Boolean setting not allowlisted");
        Object plugin=plugin(pluginId,setting);
        return bool(api.call("GetPluginConfigValue",plugin,setting));
    }
    /** Fixed optional settings diagnostic, never an arbitrary plugin action. */
    String bitrateTest(String pluginId,boolean start,boolean confirm){
        if(start&&!confirm)throw new IllegalArgumentException("Bitrate test requires confirm=true");
        Object selected=plugin(pluginId,start?"action.runBitrateTest":"runtimeBitrateTest.status");
        if(start){
            Object error=api.call("SetPluginConfigValue",selected,"action.runBitrateTest","run");
            if(error!=null)throw new IllegalStateException("Plugin refused bitrate test");
            if(String.valueOf(api.call("GetPluginConfigValue",selected,"action.last")).startsWith("FAILED:"))
                throw new IllegalStateException("Plugin refused bitrate test; previous result was not a new run");
        }
        return String.valueOf(api.call("GetPluginConfigValue",selected,"runtimeBitrateTest.status"));
    }
    String set(String pluginId,String setting,String expected,String requested,boolean confirm) {
        if(!SETTING.equals(setting)&&!RATE_SETTING.equals(setting))throw new IllegalArgumentException("Boolean setting not allowlisted");
        if (!confirm) throw new IllegalArgumentException("Plugin setting change requires confirm=true");
        String before=bool(expected), after=bool(requested);
        Object plugin=plugin(pluginId,setting);
        if (!before.equals(bool(api.call("GetPluginConfigValue",plugin,setting))))
            throw new IllegalStateException("Plugin setting changed since checkpoint");
        Object failure=api.call("SetPluginConfigValue",plugin,setting,after);
        if (failure !=null) throw new IllegalStateException("Plugin rejected setting change");
        String actual=bool(api.call("GetPluginConfigValue",plugin,setting));
        if (!actual.equals(after)) throw new IllegalStateException("Plugin setting readback differs");
        return actual;
    }
    String adaptiveTest(String pluginId,String operation,String session,String revision,String target,String seconds,boolean confirm){
        if(!"status".equals(operation)&&!"override".equals(operation)&&!"clear".equals(operation))
            throw new IllegalArgumentException("Adaptive test operation not allowlisted");
        Object selected=plugin(pluginId,"adaptiveTest.sessions");
        if(!"status".equals(operation)){
            if(!confirm||session==null||!session.matches("[0-9a-f]{32}")||revision==null||!revision.matches("[0-9]{1,12}"))
                throw new IllegalArgumentException("Confirmed exact adaptive session/revision required");
            if("clear".equals(operation)){target="0";seconds="0";}
            if(target==null||!target.matches("[0-9]{1,5}")||Integer.parseInt(target)<250||Integer.parseInt(target)>50000){
                if(!"clear".equals(operation))throw new IllegalArgumentException("Bitrate must be 250..50000 kbps");
            }
            if(seconds==null||!seconds.matches("[0-9]{1,3}")||!"clear".equals(operation)&&(Integer.parseInt(seconds)<15||Integer.parseInt(seconds)>300))
                throw new IllegalArgumentException("Override lease must be 15..300 seconds");
            plugin(pluginId,"action.adaptiveTest");
            Object failure=api.call("SetPluginConfigValue",selected,"action.adaptiveTest",session+":"+revision+":"+target+":"+seconds);
            if(failure!=null)throw new IllegalStateException("Adaptive test override refused");
        }
        return String.valueOf(api.call("GetPluginConfigValue",selected,"adaptiveTest.sessions"));
    }
    private Object plugin(String pluginId,String setting) {
        String id=pluginId ==null ? "" : pluginId.toLowerCase(Locale.US);
        if (!("sagetvffmpegpluginlinux".equals(id) || "sagetvffmpegpluginwinx64".equals(id))
                || !(SETTING.equals(setting)||RATE_SETTING.equals(setting)||"action.runBitrateTest".equals(setting)||"runtimeBitrateTest.status".equals(setting)||"adaptiveTest.sessions".equals(setting)||"action.adaptiveTest".equals(setting))) throw new IllegalArgumentException("Plugin setting not allowlisted");
        Object installed=api.call("GetInstalledPlugins");
        Object selected=null;
        if (installed instanceof Object[]) for (Object plugin:(Object[])installed) {
            if (!id.equals(String.valueOf(api.call("GetPluginIdentifier",plugin)).toLowerCase(Locale.US))) continue;
            if (selected !=null) throw new IllegalStateException("Ambiguous installed plugin");
            selected=plugin;
        }
        if (selected ==null || !Boolean.TRUE.equals(api.call("IsPluginEnabled",selected)))
            throw new IllegalStateException("Installed plugin unavailable or disabled");
        Object settings=api.call("GetPluginConfigSettings",selected);
        boolean found=false;
        if (settings instanceof Object[]) for (Object key:(Object[])settings) if (setting.equals(key)) found=true;
        if (!found) throw new IllegalStateException("Installed plugin lacks caption setting");
        return selected;
    }
    private static String bool(Object value) {
        if (value !=null && "true".equalsIgnoreCase(value.toString())) return "true";
        if (value !=null && "false".equalsIgnoreCase(value.toString())) return "false";
        throw new IllegalArgumentException("Boolean plugin value required");
    }
}
