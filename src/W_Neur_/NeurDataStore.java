package W_Neur_;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NeurDataStore {
    public static class PointState {
        public final int windowId;
        public float x;
        public float y;
        public String clusterUid;
        public int clusterId;
        public float confidence;
        public float signalQuality;
        public long tsMs;
        public int color;
        public int previousColor;
        public float colorTransition;

        PointState(int windowId) {
            this.windowId = windowId;
        }
    }

    public static class ClusterState {
        public String clusterUid;
        public int clusterId;
        public float weight;
        public int count;
        public float centerX;
        public float centerY;
        public float cov00;
        public float cov01;
        public float cov10;
        public float cov11;
        public String status = "active";
        public float fade = 1f;
        public long updatedMs = 0L;
    }

    public static class EventState {
        public String event;
        public String clusterUid;
        public long tsMs;
        public float life = 1f;
    }

    public static class ParamsState {
        public int activeClusters = 0;
        public int maxClusters = 0;
        public float acceptanceRate = 0f;
        public float nll = 0f;
        public float gpNoiseVar = 0f;
        public float dpAlpha = 0f;
        public float[] clusterWeights = new float[0];
        public float birthThreshold = 0f;
        public float deathWeightThreshold = 0f;
        public float mergeDistThreshold = 0f;
        public long tsMs = 0L;
    }

    private static final int MAX_POINTS = 1200;
    private static final int MAX_EVENTS = 40;

    private final LinkedHashMap<Integer, PointState> pointsByWindow = new LinkedHashMap<Integer, PointState>();
    private final LinkedHashMap<String, ClusterState> clustersByUid = new LinkedHashMap<String, ClusterState>();
    private final LinkedList<EventState> events = new LinkedList<EventState>();

    private final Map<String, Integer> colorByUid = new HashMap<String, Integer>();
    private int nextColorIndex = 0;

    private final ParamsState params = new ParamsState();
    private float stability = 0f;
    private long lastPointMs = 0L;
    private long lastSnapshotMs = 0L;
    private long lastParamsMs = 0L;
    private String lastServerError = "";

    public synchronized void onPoint(JSONObject payload, long tsMs) {
        int windowId = payload.optInt("window_id", -1);
        if (windowId < 0) {
            return;
        }
        JSONArray latent = payload.optJSONArray("latent");
        if (latent == null || latent.length() < 2) {
            return;
        }

        PointState p = pointsByWindow.get(windowId);
        if (p == null) {
            p = new PointState(windowId);
            pointsByWindow.put(windowId, p);
        }
        p.x = (float) latent.optDouble(0, 0.0);
        p.y = (float) latent.optDouble(1, 0.0);
        p.clusterUid = payload.optString("cluster_uid", "unknown");
        p.clusterId = payload.optInt("cluster_id", -1);
        p.confidence = clamp01((float) payload.optDouble("confidence", 0.0));
        p.signalQuality = clamp01((float) payload.optDouble("signal_quality", 0.0));
        p.tsMs = tsMs > 0 ? tsMs : System.currentTimeMillis();

        int c = colorForUid(p.clusterUid);
        if (p.color != 0 && p.color != c) {
            p.previousColor = p.color;
            p.colorTransition = 1f;
        }
        p.color = c;

        trimPointsIfNeeded();
        lastPointMs = p.tsMs;
    }

    public synchronized void onPatch(JSONObject payload, long tsMs) {
        JSONArray updates = payload.optJSONArray("updates");
        if (updates == null) {
            return;
        }
        for (int i = 0; i < updates.length(); i++) {
            JSONObject up = updates.optJSONObject(i);
            if (up == null) {
                continue;
            }
            int windowId = up.optInt("window_id", -1);
            if (windowId < 0) {
                continue;
            }

            PointState p = pointsByWindow.get(windowId);
            if (p == null) {
                p = new PointState(windowId);
                pointsByWindow.put(windowId, p);
            }

            JSONArray latent = up.optJSONArray("latent");
            if (latent != null && latent.length() >= 2) {
                p.x = (float) latent.optDouble(0, p.x);
                p.y = (float) latent.optDouble(1, p.y);
            }
            String oldUid = p.clusterUid;
            p.clusterUid = up.optString("cluster_uid", oldUid == null ? "unknown" : oldUid);
            p.clusterId = up.optInt("cluster_id", p.clusterId);
            p.confidence = clamp01((float) up.optDouble("confidence", p.confidence));
            p.tsMs = tsMs > 0 ? tsMs : System.currentTimeMillis();

            int c = colorForUid(p.clusterUid);
            if (p.color != 0 && p.color != c) {
                p.previousColor = p.color;
                p.colorTransition = 1f;
            }
            p.color = c;
        }
    }

    public synchronized void onSnapshot(JSONObject payload, long tsMs) {
        int activeClusters = payload.optInt("active_clusters", params.activeClusters);
        int maxClusters = payload.optInt("max_clusters", params.maxClusters);
        params.activeClusters = activeClusters;
        params.maxClusters = maxClusters;

        JSONArray clusters = payload.optJSONArray("clusters");
        if (clusters != null) {
            LinkedHashMap<String, ClusterState> next = new LinkedHashMap<String, ClusterState>();
            for (int i = 0; i < clusters.length(); i++) {
                JSONObject c = clusters.optJSONObject(i);
                if (c == null) {
                    continue;
                }
                String uid = c.optString("cluster_uid", "");
                if (uid.isEmpty()) {
                    continue;
                }
                ClusterState cs = clustersByUid.get(uid);
                if (cs == null) {
                    cs = new ClusterState();
                    cs.clusterUid = uid;
                    cs.fade = 0f;
                }
                cs.clusterUid = uid;
                cs.clusterId = c.optInt("cluster_id", cs.clusterId);
                cs.weight = (float) c.optDouble("weight", cs.weight);
                cs.count = c.optInt("count", cs.count);
                JSONArray center = c.optJSONArray("center");
                if (center != null && center.length() >= 2) {
                    cs.centerX = (float) center.optDouble(0, cs.centerX);
                    cs.centerY = (float) center.optDouble(1, cs.centerY);
                }
                JSONArray cov = c.optJSONArray("cov");
                if (cov != null && cov.length() >= 2) {
                    JSONArray r0 = cov.optJSONArray(0);
                    JSONArray r1 = cov.optJSONArray(1);
                    if (r0 != null && r0.length() >= 2 && r1 != null && r1.length() >= 2) {
                        cs.cov00 = (float) r0.optDouble(0, cs.cov00);
                        cs.cov01 = (float) r0.optDouble(1, cs.cov01);
                        cs.cov10 = (float) r1.optDouble(0, cs.cov10);
                        cs.cov11 = (float) r1.optDouble(1, cs.cov11);
                    }
                }
                cs.status = c.optString("status", "active");
                cs.updatedMs = tsMs > 0 ? tsMs : System.currentTimeMillis();
                next.put(uid, cs);
            }

            for (Map.Entry<String, ClusterState> e : clustersByUid.entrySet()) {
                if (!next.containsKey(e.getKey())) {
                    ClusterState cs = e.getValue();
                    if (cs != null) {
                        cs.status = "inactive";
                        next.put(e.getKey(), cs);
                    }
                }
            }

            clustersByUid.clear();
            clustersByUid.putAll(next);
        }

        JSONArray ev = payload.optJSONArray("events");
        if (ev != null) {
            for (int i = 0; i < ev.length(); i++) {
                JSONObject e = ev.optJSONObject(i);
                if (e == null) {
                    continue;
                }
                EventState es = new EventState();
                es.event = e.optString("event", "update");
                es.clusterUid = e.optString("cluster_uid", "");
                es.tsMs = (long) (e.optDouble("ts", (System.currentTimeMillis() / 1000.0)) * 1000.0);
                es.life = 1f;
                events.add(es);
            }
        }

        JSONObject metrics = payload.optJSONObject("metrics");
        if (metrics != null) {
            stability = clamp01((float) metrics.optDouble("stability", stability));
        }

        while (events.size() > MAX_EVENTS) {
            events.poll();
        }
        lastSnapshotMs = tsMs > 0 ? tsMs : System.currentTimeMillis();
    }

    public synchronized void onParams(JSONObject payload, long tsMs) {
        params.activeClusters = payload.optInt("active_clusters", params.activeClusters);
        params.maxClusters = payload.optInt("max_clusters", params.maxClusters);
        params.acceptanceRate = clamp01((float) payload.optDouble("acceptance_rate", params.acceptanceRate));
        params.nll = (float) payload.optDouble("nll", params.nll);

        JSONArray cw = payload.optJSONArray("cluster_weights");
        if (cw != null) {
            float[] arr = new float[cw.length()];
            for (int i = 0; i < cw.length(); i++) {
                arr[i] = clamp01((float) cw.optDouble(i, 0.0));
            }
            params.clusterWeights = arr;
        }

        JSONObject hypers = payload.optJSONObject("hypers");
        if (hypers != null) {
            JSONObject gp = hypers.optJSONObject("gp");
            if (gp != null) {
                params.gpNoiseVar = (float) gp.optDouble("noise_var", params.gpNoiseVar);
            }
            JSONObject dp = hypers.optJSONObject("dp");
            if (dp != null) {
                params.dpAlpha = (float) dp.optDouble("alpha", params.dpAlpha);
            }
        }

        JSONObject controls = payload.optJSONObject("controls");
        if (controls != null) {
            params.birthThreshold = (float) controls.optDouble("birth_threshold", params.birthThreshold);
            params.deathWeightThreshold = (float) controls.optDouble("death_weight_threshold", params.deathWeightThreshold);
            params.mergeDistThreshold = (float) controls.optDouble("merge_dist_threshold", params.mergeDistThreshold);
        }

        params.tsMs = tsMs > 0 ? tsMs : System.currentTimeMillis();
        lastParamsMs = params.tsMs;
    }

    public synchronized void onServerError(String err) {
        lastServerError = err == null ? "" : err;
    }

    public synchronized void tickAnimation() {
        Iterator<Map.Entry<Integer, PointState>> pit = pointsByWindow.entrySet().iterator();
        long now = System.currentTimeMillis();
        while (pit.hasNext()) {
            PointState p = pit.next().getValue();
            if (p.colorTransition > 0f) {
                p.colorTransition = Math.max(0f, p.colorTransition - 0.08f);
            }
            if (now - p.tsMs > 25000L) {
                pit.remove();
            }
        }

        for (ClusterState c : clustersByUid.values()) {
            if ("inactive".equals(c.status)) {
                c.fade = Math.max(0f, c.fade - 0.03f);
            } else {
                c.fade = Math.min(1f, c.fade + 0.07f);
            }
        }

        Iterator<EventState> eit = events.iterator();
        while (eit.hasNext()) {
            EventState e = eit.next();
            e.life -= 0.02f;
            if (e.life <= 0f) {
                eit.remove();
            }
        }
    }

    public synchronized List<PointState> getPointsOrdered() {
        ArrayList<PointState> out = new ArrayList<PointState>(pointsByWindow.values());
        out.sort((a, b) -> Integer.compare(a.windowId, b.windowId));
        return out;
    }

    public synchronized List<ClusterState> getClusters() {
        return new ArrayList<ClusterState>(clustersByUid.values());
    }

    public synchronized List<EventState> getEvents() {
        return new ArrayList<EventState>(events);
    }

    public synchronized ParamsState getParamsCopy() {
        ParamsState out = new ParamsState();
        out.activeClusters = params.activeClusters;
        out.maxClusters = params.maxClusters;
        out.acceptanceRate = params.acceptanceRate;
        out.nll = params.nll;
        out.gpNoiseVar = params.gpNoiseVar;
        out.dpAlpha = params.dpAlpha;
        out.birthThreshold = params.birthThreshold;
        out.deathWeightThreshold = params.deathWeightThreshold;
        out.mergeDistThreshold = params.mergeDistThreshold;
        out.clusterWeights = params.clusterWeights.clone();
        out.tsMs = params.tsMs;
        return out;
    }

    public synchronized float getStability() {
        return stability;
    }

    public synchronized long getLastPointMs() {
        return lastPointMs;
    }

    public synchronized long getLastSnapshotMs() {
        return lastSnapshotMs;
    }

    public synchronized long getLastParamsMs() {
        return lastParamsMs;
    }

    public synchronized String getLastServerError() {
        return lastServerError;
    }

    private int colorForUid(String uid) {
        if (uid == null || uid.isEmpty()) {
            uid = "unknown";
        }
        Integer c = colorByUid.get(uid);
        if (c != null) {
            return c.intValue();
        }
        int[] palette = {
                0xFF4FD8FF, 0xFF77E89A, 0xFFFFB86B, 0xFFFF7E8C,
                0xFF9FA8FF, 0xFF7CF0D8, 0xFFE8A2FF, 0xFFFFE07A,
                0xFF66D1B7, 0xFFFF9AA0, 0xFFA3E061, 0xFF8CB8FF
        };
        int color = palette[nextColorIndex % palette.length];
        nextColorIndex++;
        colorByUid.put(uid, color);
        return color;
    }

    private void trimPointsIfNeeded() {
        while (pointsByWindow.size() > MAX_POINTS) {
            Iterator<Integer> it = pointsByWindow.keySet().iterator();
            if (it.hasNext()) {
                Integer key = it.next();
                pointsByWindow.remove(key);
            } else {
                break;
            }
        }
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }
}
