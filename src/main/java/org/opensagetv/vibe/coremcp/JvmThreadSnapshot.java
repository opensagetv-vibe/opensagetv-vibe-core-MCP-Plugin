package org.opensagetv.vibe.coremcp;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Manual, bounded JVM observation through public Java management APIs only.
 * No Core reflection, thread suspension, stack arguments, shell, heap dump,
 * sampling loop or user-supplied thread/API selector. It intentionally avoids
 * apiUI/GetMediaTime: that call may itself wait behind the lock under diagnosis.
 */
final class JvmThreadSnapshot {
    static final int MAX_THREADS=128, MAX_FRAMES=24, MAX_TEXT=192;
    static Map<String,Object> capture() {
        ThreadMXBean bean=ManagementFactory.getThreadMXBean();
        long[] all=bean.getAllThreadIds();Arrays.sort(all);
        ThreadInfo[] info=bean.getThreadInfo(Arrays.copyOf(all,Math.min(all.length,MAX_THREADS)),MAX_FRAMES);
        Map<String,Object> out=new LinkedHashMap<String,Object>();
        List<Object> rows=new ArrayList<Object>();
        for(ThreadInfo thread:info)if(thread!=null)rows.add(row(thread));
        out.put("schemaVersion",1);out.put("capturedAtMs",System.currentTimeMillis());
        out.put("totalThreads",all.length);out.put("threadLimit",MAX_THREADS);out.put("frameLimit",MAX_FRAMES);
        out.put("truncated",all.length>MAX_THREADS);out.put("threads",rows);return out;
    }
    static Map<String,Object> row(ThreadInfo thread) {
        Map<String,Object> row=new LinkedHashMap<String,Object>();List<String> stack=new ArrayList<String>();
        row.put("id",thread.getThreadId());row.put("name",text(thread.getThreadName()));
        row.put("state",thread.getThreadState().name());row.put("lock",text(thread.getLockName()));
        row.put("lockOwnerId",thread.getLockOwnerId());
        StackTraceElement[] frames=thread.getStackTrace();
        for(int i=0;i<Math.min(frames.length,MAX_FRAMES);i++)stack.add(text(frames[i].toString()));
        row.put("stack",stack);row.put("framesTruncated",frames.length>=MAX_FRAMES);return row;
    }
    static String text(String value) {
        if(value==null)return "";
        // Thread names can contain a producer's opaque ID. Retain useful role
        // names without exporting lease/token-looking hexadecimal identities.
        String safe=value.replaceAll("(?i)[a-f0-9]{8,}","[id]").replaceAll("[\\r\\n\\t]"," ");
        return safe.substring(0,Math.min(safe.length(),MAX_TEXT));
    }
    private JvmThreadSnapshot() {}
}
