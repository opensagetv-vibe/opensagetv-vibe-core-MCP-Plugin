package org.opensagetv.vibe.coremcp;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import sage.SageTVPlugin;
import sage.SageTVPluginRegistry;

public final class ContractTest {
    public static void main(String[] args) throws Exception {
        if (!SageTVPlugin.class.isAssignableFrom(SageTVCoreMcpPlugin.class))
            throw new AssertionError("Plugin does not implement stock SageTVPlugin");
        SageTVCoreMcpPlugin.class.getConstructor(SageTVPluginRegistry.class);
        SageTVCoreMcpPlugin.class.getConstructor(SageTVPluginRegistry.class, Boolean.TYPE);

        Map<String, Object> sample = new LinkedHashMap<String, Object>();
        sample.put("ok", Boolean.TRUE);
        sample.put("text", "line\n\"quoted\"");
        sample.put("items", Arrays.asList("a", Integer.valueOf(2)));
        String encoded = Json.encode(sample);
        String expected = "{\"ok\":true,\"text\":\"line\\n\\\"quoted\\\"\",\"items\":[\"a\",2]}";
        if (!expected.equals(encoded)) throw new AssertionError("Unexpected JSON: " + encoded);

        if (ControlService.CAPABILITY_VERSION != 1)
            throw new AssertionError("Unexpected capability version");
        if(!ControlService.isActionAllowed("diagnostics.threads"))throw new AssertionError("Missing bounded thread observation");
        Map<String,Object> threads=JvmThreadSnapshot.capture();
        if(((java.util.List<?>)threads.get("threads")).size()>JvmThreadSnapshot.MAX_THREADS)
            throw new AssertionError("Unbounded thread output");
        if(!"worker-[id]".equals(JvmThreadSnapshot.text("worker-abcdef0123456789")))
            throw new AssertionError("Opaque thread identity leaked");
        if(JvmThreadSnapshot.text(new String(new char[500]).replace('\0','x')).length()!=JvmThreadSnapshot.MAX_TEXT)
            throw new AssertionError("Unbounded thread text");
        for (String command:Arrays.asList("TV","Back","Home","Options","Info","Full Screen On","Full Screen Off",
                "Play","Pause","Stop","Time Scroll","Skip Fwd","Skip Bkwd","Channel Up","Channel Down",
                "Up","Down","Left","Right","Select","DVD Menu","DVD Title Menu","DVD Return",
                "DVD Next Chapter","DVD Prev Chapter","DVD Audio Change","DVD Subtitle Change","DVD Subtitle Toggle","DVD Angle Change")) {
            if(sage.UserEvent.getEvtCodeForName(ControlService.canonicalCommand(command))<2)
                throw new AssertionError("Not a real stock event: "+command);
        }
        if (sage.UserEvent.getEvtCodeForName("Skip Fwd")!=0 || !"ff".equals(ControlService.canonicalCommand("Skip Fwd")))
            throw new AssertionError("Friendly alias regression must be proved against real stock binary");
        for (String command : Arrays.asList(
                "Full Screen On", "Full Screen Off", "Time Scroll",
                "DVD Menu", "DVD Title Menu", "DVD Return",
                "DVD Next Chapter", "DVD Prev Chapter", "DVD Audio Change",
                "DVD Subtitle Change", "DVD Subtitle Toggle")) {
            if (!ControlService.isUiCommandAllowed(command))
                throw new AssertionError("Missing stock DVD command: " + command);
        }
        if (ControlService.isUiCommandAllowed("EvaluateExpression"))
            throw new AssertionError("Arbitrary command unexpectedly allowlisted");
        if (!ControlService.isActionAllowed("library.add_import_path"))
            throw new AssertionError("Missing stock AddLibraryImportPath commissioning action");
        if (!ControlService.isActionAllowed("library.remove_import_path"))
            throw new AssertionError("Missing stock RemoveLibraryImportPath cleanup action");
        if (!ControlService.isActionAllowed("captions.trace"))
            throw new AssertionError("Missing bounded stock caption trace action");
        if (ControlService.isActionAllowed("filesystem.read"))
            throw new AssertionError("Arbitrary filesystem action unexpectedly allowlisted");
        if (!ControlService.isActionAllowed("server.activity"))
            throw new AssertionError("Missing read-only stock activity preflight");
        if (ControlService.recordingCount(new Object[0]) != 0 ||
                ControlService.recordingCount(new Object[]{new Object(), new Object(), new Object()}) != 3)
            throw new AssertionError("Bad recording count");
        for (Object unavailable : new Object[]{null, "unavailable", Integer.valueOf(0)}) {
            try {
                ControlService.recordingCount(unavailable);
                throw new AssertionError("Unknown activity must not mean idle");
            } catch (IllegalStateException expectedUnavailable) { }
            try {
                ControlService.activityArrayCount(unavailable, "UI context");
                throw new AssertionError("Unknown clients must not mean idle");
            } catch (IllegalStateException expectedUnavailable) { }
        }
        if (ControlService.defaultWatchWaitMs(false) != 0 ||
                ControlService.defaultWatchWaitMs(true) != 15000)
            throw new AssertionError("Watch must acknowledge ordinary playback without media-state polling");
        assertDvdControl("DVD Menu", false, 201, 2);
        assertDvdControl("DVD Return", false, 209, 0);
        assertDvdControl("DVD Angle Change", false, 213, -1);
        assertDvdControl("Down", true, 210, 3);
        assertDvdControl("Select", true, 208, 0);
        if (ControlService.dvdControlForCommand("Down", false) != null)
            throw new AssertionError("Non-menu arrow must remain an STV command");
        System.out.println("PASS stock SageTV plugin and JSON contracts");
    }

    private static void assertDvdControl(String command, boolean showingMenu,
            long code, long parameter) {
        long[] control = ControlService.dvdControlForCommand(command, showingMenu);
        if (control == null || control[0] != code || control[1] != parameter)
            throw new AssertionError("Bad DVD control mapping: " + command);
    }
}
