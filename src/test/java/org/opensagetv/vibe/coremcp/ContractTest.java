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
        System.out.println("PASS stock SageTV plugin and JSON contracts");
    }
}

