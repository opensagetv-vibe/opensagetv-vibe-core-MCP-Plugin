package org.opensagetv.vibe.coremcp;

/** No live server/process mutation: exact public API and lifecycle guards. */
public final class PluginLifecycleTest {
    static final class Api implements CompanionSettings.Api {
        final Object plugin = new Object(); boolean on = true, recording, unknown, refuse;
        int writes; String context = "444556303031";
        public Object global(String name, Object... args) {
            if (name.equals("GetUIContextNames")) return new String[]{context};
            if (name.equals("GetCurrentlyRecordingMediaFiles")) return unknown ? null : recording ? new Object[]{plugin} : new Object[0];
            if (name.equals("GetConnectedClients")) return new Object[0];
            if (name.equals("GetInstalledPlugins")) return new Object[]{plugin};
            if (name.equals("GetPluginIdentifier")) return "SageTVFFmpegPluginLinux";
            if (name.equals("IsPluginEnabled")) return on;
            if (name.equals("EnablePlugin") || name.equals("DisablePlugin")) {
                writes++; on = name.equals("EnablePlugin"); return true;
            }
            if (name.equals("SetPluginConfigValue")) {
                check(args[0] == plugin && args[1].equals("action.testDvdProcessFailure") && args[2].equals("12"));
                writes++; return refuse ? "failure" : null;
            }
            throw new AssertionError(name);
        }
        public Object ui(String context, String name, Object... args) {
            check(context.equals(this.context) && name.equals("IsCurrentMediaFileDVD")); return true;
        }
    }
    static void check(boolean valid) { if (!valid) throw new AssertionError(); }
    static void rejects(Runnable run) {
        try { run.run(); throw new AssertionError("Expected rejection"); }
        catch (IllegalStateException | IllegalArgumentException expected) { }
    }
    public static void main(String[] args) {
        Api api = new Api(); PluginLifecycle test = new PluginLifecycle(api);
        String ui = api.context, id = "sagetvffmpegpluginlinux";
        rejects(() -> test.lifecycle(ui,id,true,false,false));
        rejects(() -> test.lifecycle(ui,"unknown",true,false,true));
        rejects(() -> test.lifecycle("another",id,true,false,true));
        api.recording=true; rejects(() -> test.lifecycle(ui,id,true,false,true)); api.recording=false;
        api.unknown=true; rejects(() -> test.lifecycle(ui,id,true,false,true)); api.unknown=false;
        check(api.writes == 0);
        check(test.lifecycle(ui,id,true,false,true).get("enabled").equals(false));
        rejects(() -> test.lifecycle(ui,id,true,true,true));
        test.lifecycle(ui,id,false,true,true);
        rejects(() -> test.failDvdProcess(ui,id,"../pid",true));
        rejects(() -> test.failDvdProcess(ui,id,"12",false));
        check(test.failDvdProcess(ui,id,"12",true).get("failureRequested").equals(true));
        api.refuse=true; rejects(() -> test.failDvdProcess(ui,id,"12",true));
        System.out.println("PASS: isolated reviewed-plugin lifecycle and exact DVD session failure guards");
    }
}
