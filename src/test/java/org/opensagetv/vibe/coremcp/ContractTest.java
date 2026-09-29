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
        for (String command : Arrays.asList(
                "Full Screen On", "Full Screen Off",
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
        if (ControlService.isActionAllowed("filesystem.read"))
            throw new AssertionError("Arbitrary filesystem action unexpectedly allowlisted");
        assertDvdControl("DVD Menu", false, 201, 2);
        assertDvdControl("DVD Return", false, 209, 0);
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
