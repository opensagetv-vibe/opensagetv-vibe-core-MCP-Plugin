package org.opensagetv.vibe.coremcp;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Deterministic cold-cache oracle: unrelated blocked media must not gate an indexed hit. */
public final class ExactPathLookupTest {
    static final class Lookup implements ControlService.MediaPathLookup {
        final Map<File, Object> files = new HashMap<File, Object>();
        final Object dvd = new Object();
        int indexedCalls;
        int fallbackCalls;
        boolean blockUnrelatedLibrary = true;
        Object fallbackResult;
        public Object byIndexedFile(File file) { indexedCalls++; return files.get(file); }
        public boolean isDvd(Object media) { return media == dvd; }
        public Object byCanonicalPath(String path) {
            fallbackCalls++;
            if (blockUnrelatedLibrary) throw new IllegalStateException("Unrelated canonicalization blocked");
            return fallbackResult;
        }
    }

    public static void main(String[] args) throws Exception {
        Lookup fast = new Lookup();
        String literal = new File("indexed-recording.ts").getAbsolutePath();
        Object media = new Object();
        fast.files.put(new File(literal), media);
        // The previous cold ordering necessarily entered this expensive path
        // before looking up the requested file. Model the observed dependency,
        // not a flaky elapsed-time/sleep test or an actual network mount.
        try {
            fast.byCanonicalPath(literal);
            throw new AssertionError("Old cold-order reproduction must fail");
        } catch (IllegalStateException expected) { }
        fast.fallbackCalls = 0;
        if (ControlService.resolveIndexedPathFirst(literal, fast) != media ||
                fast.indexedCalls != 1 || fast.fallbackCalls != 0)
            throw new AssertionError("Indexed hit must bypass all-library fallback");

        Lookup canonical = new Lookup();
        String relative = "path/../indexed-recording.ts";
        canonical.files.put(new File(relative).getCanonicalFile(), media);
        if (ControlService.resolveIndexedPathFirst(relative, canonical) != media || canonical.fallbackCalls != 0)
            throw new AssertionError("Canonical indexed spelling must stay fast");

        Lookup root = new Lookup();
        String dvdRoot = new File("authored-disc").getCanonicalPath();
        root.files.put(new File(dvdRoot, "VIDEO_TS"), root.dvd);
        if (ControlService.resolveIndexedPathFirst(dvdRoot, root) != root.dvd || root.fallbackCalls != 0)
            throw new AssertionError("DVD parent alias must use indexed VIDEO_TS");

        Lookup rejectAlias = new Lookup();
        rejectAlias.files.put(new File(dvdRoot, "VIDEO_TS"), media);
        rejectAlias.blockUnrelatedLibrary = false;
        if (ControlService.resolveIndexedPathFirst(dvdRoot, rejectAlias) != null || rejectAlias.fallbackCalls != 1)
            throw new AssertionError("Non-DVD alias must not be accepted");

        Lookup legacy = new Lookup();
        legacy.blockUnrelatedLibrary = false;
        legacy.fallbackResult = legacy.dvd;
        if (ControlService.resolveIndexedPathFirst(dvdRoot, legacy) != legacy.dvd || legacy.fallbackCalls != 1)
            throw new AssertionError("Authored IFO-root compatibility fallback was lost");

        for (String invalid : new String[]{null, "bad\npath", "bad\rpath", "bad\0path"}) {
            Lookup guard = new Lookup();
            try {
                ControlService.resolveIndexedPathFirst(invalid, guard);
                throw new AssertionError("Invalid path accepted");
            } catch (IllegalArgumentException expected) { }
            if (guard.indexedCalls != 0 || guard.fallbackCalls != 0)
                throw new AssertionError("Invalid path reached SageTV API");
        }
        System.out.println("PASS exact indexed lookup cold-cache, DVD aliases, fallback, invalid-path guards");
    }
}
