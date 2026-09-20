package org.opensagetv.vibe.coremcp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sage.SageTV;

final class ControlService {
    static final int CAPABILITY_VERSION = 1;
    private static final Set<String> COMMANDS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    "TV", "Back", "Home", "Options", "Info", "Play", "Pause", "Stop",
                    "Skip Fwd", "Skip Bkwd", "Channel Up", "Channel Down",
                    "Up", "Down", "Left", "Right", "Select")));
    private static final List<String> ACTIONS = Collections.unmodifiableList(Arrays.asList(
            "capabilities", "ui.list", "ui.state", "ui.command",
            "media.resolve_exact_path", "media.watch", "media.seek", "media.control",
            "media.clear_watched", "channel.tune", "captions.get", "captions.set",
            "library.scan", "diagnostics.snapshot"));
    private volatile Map<String, Object> mediaPathIndex = Collections.emptyMap();
    private volatile long mediaPathIndexTime;

    Map<String, Object> execute(Map<String, String> request) {
        String action = required(request, "action");
        if (!ACTIONS.contains(action)) throw new IllegalArgumentException("Unsupported action: " + action);
        Map<String, Object> result;
        if ("capabilities".equals(action)) result = capabilities();
        else if ("ui.list".equals(action)) result = uiList();
        else if ("ui.state".equals(action)) result = uiState(context(request));
        else if ("ui.command".equals(action)) result = uiCommand(context(request), required(request, "command"));
        else if ("media.resolve_exact_path".equals(action)) result = mediaInfo(resolveExactPath(required(request, "path")));
        else if ("media.watch".equals(action)) result = watch(request);
        else if ("media.seek".equals(action)) result = seek(request);
        else if ("media.control".equals(action)) result = control(request);
        else if ("media.clear_watched".equals(action)) result = clearWatched(request);
        else if ("channel.tune".equals(action)) result = tune(context(request), required(request, "channel"));
        else if ("captions.get".equals(action)) result = captionGet(context(request));
        else if ("captions.set".equals(action)) result = captionSet(context(request), required(request, "state"));
        else if ("library.scan".equals(action)) result = libraryScan(bool(request, "wait_until_done", false));
        else result = diagnostics(request.get("context"));
        result.put("ok", Boolean.TRUE);
        result.put("action", action);
        result.put("capabilityVersion", Integer.valueOf(CAPABILITY_VERSION));
        result.put("timestampMs", Long.valueOf(System.currentTimeMillis()));
        return result;
    }

    private Map<String, Object> capabilities() {
        Map<String, Object> out = map();
        out.put("plugin", SageTVCoreMcpPlugin.NAME);
        out.put("pluginVersion", SageTVCoreMcpPlugin.VERSION);
        out.put("actions", ACTIONS);
        out.put("uiCommands", COMMANDS);
        out.put("stockSageJarRequired", Boolean.TRUE);
        out.put("arbitraryApi", Boolean.FALSE);
        out.put("arbitraryFilesystem", Boolean.FALSE);
        out.put("miniClientProtocolExtensions", Boolean.FALSE);
        return out;
    }

    private Map<String, Object> uiList() {
        Map<String, Object> out = map();
        out.put("contexts", strings(api("GetUIContextNames")));
        out.put("connectedClients", strings(api("GetConnectedClients")));
        return out;
    }

    private Map<String, Object> uiState(String context) {
        requireContext(context);
        Map<String, Object> out = map();
        out.put("context", context);
        out.put("fullyLoaded", boolObject(apiUI(context, "IsMediaPlayerFullyLoaded")));
        out.put("loading", boolObject(apiUI(context, "IsMediaPlayerLoading")));
        Object media = apiUI(context, "GetCurrentMediaFile");
        if (media != null) out.put("media", mediaInfo(media));
        out.put("mediaTimeMs", number(apiUI(context, "GetMediaTime")));
        out.put("rawMediaTimeMs", safeNumber(context, "GetRawMediaTime"));
        out.put("durationMs", number(apiUI(context, "GetMediaDuration")));
        out.put("seekStartMs", number(apiUI(context, "GetAvailableSeekingStart")));
        out.put("seekEndMs", number(apiUI(context, "GetAvailableSeekingEnd")));
        out.put("playbackRate", number(apiUI(context, "GetPlaybackRate")));
        out.put("captionState", string(apiUI(context, "GetMediaPlayerClosedCaptionState")));
        return out;
    }

    private Map<String, Object> uiCommand(String context, String command) {
        requireContext(context);
        if (!COMMANDS.contains(command)) throw new IllegalArgumentException("Command is not allowlisted: " + command);
        apiUI(context, "SageCommand", new Object[]{command});
        Map<String, Object> out = map();
        out.put("context", context);
        out.put("command", command);
        out.put("accepted", Boolean.TRUE);
        return out;
    }

    private Map<String, Object> watch(Map<String, String> request) {
        String context = context(request);
        requireContext(context);
        Object media;
        if (present(request, "media_id")) media = mediaById(intValue(request, "media_id", 0, 1, Integer.MAX_VALUE));
        else media = resolveExactPath(required(request, "path"));
        int id = mediaId(media);
        apiUI(context, "Watch", new Object[]{media});
        int waitMs = intValue(request, "wait_ms", 15000, 1000, 60000);
        boolean observed = waitForMedia(context, id, waitMs);
        boolean fromBeginning = bool(request, "from_beginning", false);
        Long soughtTo = null;
        if (observed && fromBeginning) {
            long start = seekStart(context);
            apiUI(context, "Seek", new Object[]{Long.valueOf(Math.max(0, start))});
            soughtTo = Long.valueOf(Math.max(0, start));
        }
        Map<String, Object> out = map();
        out.put("context", context);
        out.put("media", mediaInfo(media));
        out.put("accepted", Boolean.TRUE);
        out.put("observed", Boolean.valueOf(observed));
        out.put("fromBeginning", Boolean.valueOf(fromBeginning));
        if (soughtTo != null) out.put("seekTargetMs", soughtTo);
        out.put("state", uiState(context));
        return out;
    }

    private Map<String, Object> seek(Map<String, String> request) {
        String context = context(request);
        requireContext(context);
        long requested = longValue(request, "target_ms", 0, Long.MAX_VALUE);
        boolean absolute = bool(request, "absolute", false);
        boolean dvd = boolObject(apiUI(context, "IsCurrentMediaFileDVD")).booleanValue();
        long start = seekStart(context);
        long end = number(apiUI(context, "GetAvailableSeekingEnd")).longValue();
        long target = absolute || dvd ? requested : safeAdd(start, requested);
        target = Math.max(dvd ? 0 : start, target);
        if (end > 0) target = Math.min(end, target);
        apiUI(context, "Seek", new Object[]{Long.valueOf(target)});
        Map<String, Object> out = map();
        out.put("context", context);
        out.put("requestedTargetMs", Long.valueOf(requested));
        out.put("absoluteTargetMs", Long.valueOf(target));
        out.put("coordinate", absolute ? "absolute" : "media_relative");
        out.put("dvd", Boolean.valueOf(dvd));
        out.put("accepted", Boolean.TRUE);
        return out;
    }

    private Map<String, Object> control(Map<String, String> request) {
        String context = context(request);
        requireContext(context);
        String operation = required(request, "operation").toLowerCase();
        if ("play".equals(operation)) apiUI(context, "Play");
        else if ("pause".equals(operation)) apiUI(context, "Pause");
        else if ("play_pause".equals(operation)) apiUI(context, "PlayPause");
        else if ("stop".equals(operation)) apiUI(context, "CloseAndWaitUntilClosed");
        else if ("skip_forward".equals(operation)) apiUI(context, "SkipForward");
        else if ("skip_forward2".equals(operation)) apiUI(context, "SkipForward2");
        else if ("skip_backward".equals(operation)) apiUI(context, "SkipBackwards");
        else if ("skip_backward2".equals(operation)) apiUI(context, "SkipBackwards2");
        else if ("rate".equals(operation)) {
            double rate = doubleValue(request, "rate", -64.0, 64.0);
            apiUI(context, "SetPlaybackRate", new Object[]{Float.valueOf((float) rate)});
        } else throw new IllegalArgumentException("Unsupported media operation: " + operation);
        Map<String, Object> out = map();
        out.put("context", context);
        out.put("operation", operation);
        out.put("accepted", Boolean.TRUE);
        return out;
    }

    private Map<String, Object> tune(String context, String channel) {
        requireContext(context);
        if (!channel.matches("[0-9A-Za-z._-]{1,32}")) throw new IllegalArgumentException("Invalid channel syntax");
        apiUI(context, "ChannelSet", new Object[]{channel});
        Map<String, Object> out = map();
        out.put("context", context);
        out.put("channel", channel);
        out.put("accepted", Boolean.TRUE);
        return out;
    }

    private Map<String, Object> captionGet(String context) {
        requireContext(context);
        Map<String, Object> out = map();
        out.put("context", context);
        out.put("state", string(apiUI(context, "GetMediaPlayerClosedCaptionState")));
        return out;
    }

    private Map<String, Object> captionSet(String context, String rawState) {
        requireContext(context);
        String normalized = rawState.trim();
        if ("off".equalsIgnoreCase(normalized)) normalized = "Captions Off";
        else if ("cc1".equalsIgnoreCase(normalized)) normalized = "CC1";
        else if ("cc2".equalsIgnoreCase(normalized)) normalized = "CC2";
        else if ("text1".equalsIgnoreCase(normalized)) normalized = "Text1";
        else if ("text2".equalsIgnoreCase(normalized)) normalized = "Text2";
        else throw new IllegalArgumentException("Caption state must be Off, CC1, CC2, Text1, or Text2");
        apiUI(context, "SetMediaPlayerClosedCaptionState", new Object[]{normalized});
        return captionGet(context);
    }

    private Map<String, Object> libraryScan(boolean wait) {
        api("RunLibraryImportScan", new Object[]{Boolean.valueOf(wait)});
        mediaPathIndex = Collections.emptyMap();
        mediaPathIndexTime = 0;
        Map<String, Object> out = map();
        out.put("requested", Boolean.TRUE);
        out.put("waitUntilDone", Boolean.valueOf(wait));
        return out;
    }

    private Map<String, Object> clearWatched(Map<String, String> request) {
        if (!bool(request, "confirm", false)) throw new IllegalArgumentException("confirm=true is required");
        Object media = mediaById(intValue(request, "media_id", 0, 1, Integer.MAX_VALUE));
        Object airing = api("GetMediaFileAiring", new Object[]{media});
        if (airing == null) throw new IllegalArgumentException("MediaFile has no content Airing");
        api("ClearWatched", new Object[]{airing});
        Map<String, Object> out = map();
        out.put("media", mediaInfo(media));
        out.put("cleared", Boolean.TRUE);
        return out;
    }

    private Map<String, Object> diagnostics(String requestedContext) {
        Map<String, Object> out = capabilities();
        out.put("os", string(api("GetOS")));
        out.put("javaVersion", System.getProperty("java.version", "unknown"));
        out.put("contexts", strings(api("GetUIContextNames")));
        if (requestedContext != null && requestedContext.trim().length() > 0) {
            out.put("state", uiState(requestedContext.trim()));
        }
        return out;
    }

    private Object resolveExactPath(String requested) {
        String wanted = canonical(requested);
        Map<String, Object> snapshot = mediaPathIndex;
        if (System.currentTimeMillis() - mediaPathIndexTime > 60000 || snapshot.isEmpty()) {
            snapshot = rebuildMediaPathIndex();
        }
        Object match = snapshot.get(wanted);
        if (match != null) return match;
        snapshot = rebuildMediaPathIndex();
        match = snapshot.get(wanted);
        if (match != null) return match;
        throw new IllegalArgumentException("No SageTV-indexed MediaFile has exact path: " + requested);
    }

    private synchronized Map<String, Object> rebuildMediaPathIndex() {
        if (System.currentTimeMillis() - mediaPathIndexTime <= 60000 && !mediaPathIndex.isEmpty())
            return mediaPathIndex;
        Map<String, Object> rebuilt = new LinkedHashMap<String, Object>();
        Object mediaFiles = api("GetMediaFiles");
        for (Object media : objects(mediaFiles)) {
            int segments = number(api("GetNumberOfSegments", new Object[]{media})).intValue();
            for (int segment = 0; segment < segments; segment++) {
                Object raw = api("GetFileForSegment", new Object[]{media, Integer.valueOf(segment)});
                if (raw instanceof File) rebuilt.put(canonical(((File) raw).getPath()), media);
            }
        }
        mediaPathIndex = Collections.unmodifiableMap(rebuilt);
        mediaPathIndexTime = System.currentTimeMillis();
        return mediaPathIndex;
    }

    private Object mediaById(int id) {
        Object media = api("GetMediaFileForID", new Object[]{Integer.valueOf(id)});
        if (media == null) throw new IllegalArgumentException("Unknown SageTV MediaFile ID: " + id);
        return media;
    }

    private Map<String, Object> mediaInfo(Object media) {
        Map<String, Object> out = map();
        out.put("mediaFileId", Integer.valueOf(mediaId(media)));
        out.put("title", string(api("GetMediaTitle", new Object[]{media})));
        int segments = number(api("GetNumberOfSegments", new Object[]{media})).intValue();
        List<String> paths = new ArrayList<String>();
        for (int segment = 0; segment < segments; segment++) {
            Object path = api("GetFileForSegment", new Object[]{media, Integer.valueOf(segment)});
            if (path instanceof File) paths.add(((File) path).getPath());
        }
        out.put("paths", paths);
        return out;
    }

    private int mediaId(Object media) {
        return number(api("GetMediaFileID", new Object[]{media})).intValue();
    }

    private boolean waitForMedia(String context, int id, int waitMs) {
        long end = System.currentTimeMillis() + waitMs;
        while (System.currentTimeMillis() < end) {
            Object current = apiUI(context, "GetCurrentMediaFile");
            if (current != null && mediaId(current) == id &&
                    boolObject(apiUI(context, "IsMediaPlayerFullyLoaded")).booleanValue()) return true;
            try { Thread.sleep(100); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
        }
        return false;
    }

    private long seekStart(String context) {
        return Math.max(0, number(apiUI(context, "GetAvailableSeekingStart")).longValue());
    }

    private static long safeAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private String context(Map<String, String> request) { return required(request, "context"); }

    private void requireContext(String context) {
        if (!strings(api("GetUIContextNames")).contains(context)) {
            throw new IllegalArgumentException("Unknown SageTV UI context: " + context);
        }
    }

    private static String canonical(String path) {
        if (path == null || path.indexOf('\0') >= 0 || path.indexOf('\n') >= 0 || path.indexOf('\r') >= 0)
            throw new IllegalArgumentException("Invalid media path");
        try { return new File(path).getCanonicalPath(); }
        catch (IOException error) { throw new IllegalArgumentException("Invalid media path: " + path, error); }
    }

    private static Object api(String method) { return api(method, new Object[0]); }
    private static Object api(String method, Object[] arguments) {
        try { return SageTV.api(method, arguments); }
        catch (InvocationTargetException error) { throw failure(method, error); }
    }
    private static Object apiUI(String context, String method) { return apiUI(context, method, new Object[0]); }
    private static Object apiUI(String context, String method, Object[] arguments) {
        try { return SageTV.apiUI(context, method, arguments); }
        catch (InvocationTargetException error) { throw failure(method, error); }
    }
    private static IllegalStateException failure(String method, InvocationTargetException error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        return new IllegalStateException("SageTV API " + method + " failed: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    private static List<Object> objects(Object value) {
        if (value == null) return Collections.emptyList();
        if (value instanceof Iterable) {
            List<Object> out = new ArrayList<Object>();
            for (Object item : (Iterable) value) out.add(item);
            return out;
        }
        if (value.getClass().isArray()) {
            List<Object> out = new ArrayList<Object>();
            for (int i = 0; i < Array.getLength(value); i++) out.add(Array.get(value, i));
            return out;
        }
        return Collections.singletonList(value);
    }
    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<String>();
        for (Object item : objects(value)) if (item != null) out.add(String.valueOf(item));
        return out;
    }
    private static Number number(Object value) {
        if (value instanceof Number) return (Number) value;
        if (value == null || String.valueOf(value).trim().length() == 0) return Long.valueOf(0);
        try { return Double.valueOf(String.valueOf(value)); }
        catch (NumberFormatException error) { return Long.valueOf(0); }
    }
    private static Number safeNumber(String context, String method) {
        try { return number(apiUI(context, method)); }
        catch (RuntimeException unavailable) { return Long.valueOf(-1); }
    }
    private static Boolean boolObject(Object value) {
        return Boolean.valueOf(value instanceof Boolean ? ((Boolean) value).booleanValue() : Boolean.parseBoolean(String.valueOf(value)));
    }
    private static String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private static Map<String, Object> map() { return new LinkedHashMap<String, Object>(); }
    private static boolean present(Map<String, String> request, String key) {
        return request.containsKey(key) && request.get(key) != null && request.get(key).trim().length() > 0;
    }
    private static String required(Map<String, String> request, String key) {
        String value = request.get(key);
        if (value == null || value.trim().length() == 0) throw new IllegalArgumentException(key + " is required");
        return value.trim();
    }
    private static boolean bool(Map<String, String> request, String key, boolean fallback) {
        return present(request, key) ? Boolean.parseBoolean(request.get(key)) : fallback;
    }
    private static int intValue(Map<String, String> request, String key, int fallback, int min, int max) {
        if (!present(request, key)) return fallback;
        try {
            int value = Integer.parseInt(request.get(key));
            if (value < min || value > max) throw new IllegalArgumentException(key + " is outside allowed range");
            return value;
        } catch (NumberFormatException error) { throw new IllegalArgumentException(key + " must be an integer"); }
    }
    private static long longValue(Map<String, String> request, String key, long min, long max) {
        try {
            long value = Long.parseLong(required(request, key));
            if (value < min || value > max) throw new IllegalArgumentException(key + " is outside allowed range");
            return value;
        } catch (NumberFormatException error) { throw new IllegalArgumentException(key + " must be an integer"); }
    }
    private static double doubleValue(Map<String, String> request, String key, double min, double max) {
        try {
            double value = Double.parseDouble(required(request, key));
            if (Double.isNaN(value) || Double.isInfinite(value) || value < min || value > max)
                throw new IllegalArgumentException(key + " is outside allowed range");
            return value;
        } catch (NumberFormatException error) { throw new IllegalArgumentException(key + " must be numeric"); }
    }
}
