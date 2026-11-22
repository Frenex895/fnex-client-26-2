package dev.fnex.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.fnex.FnexClient;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Asks the Modrinth API whether a newer FNEX version exists. Silent on any failure. */
public final class UpdateChecker {
    public static final String SLUG = "fnex-client"; // change if your Modrinth project slug differs
    public static volatile String latest = null;

    public static void start() {
        Thread t = new Thread(() -> {
            try {
                String q = "https://api.modrinth.com/v2/project/" + SLUG + "/version?loaders="
                        + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
                HttpURLConnection con = (HttpURLConnection) URI.create(q).toURL().openConnection();
                con.setRequestProperty("User-Agent", "fnex-client/" + FnexClient.VERSION);
                con.setConnectTimeout(5000); con.setReadTimeout(5000);
                if (con.getResponseCode() != 200) return;
                try (InputStreamReader in = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonArray arr = JsonParser.parseReader(in).getAsJsonArray();
                    if (arr.size() == 0) return;
                    String v = arr.get(0).getAsJsonObject().get("version_number").getAsString();
                    if (newer(v, FnexClient.VERSION)) latest = v;
                }
            } catch (Exception ignored) {}
        }, "fnex-update-check");
        t.setDaemon(true);
        t.start();
    }

    static boolean newer(String a, String b) {
        String[] x = a.split("[^0-9]+"), y = b.split("[^0-9]+");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0;
            int q = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;
            if (p != q) return p > q;
        }
        return false;
    }
}
