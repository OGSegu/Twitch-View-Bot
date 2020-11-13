import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ViewBot {
    public final HttpClient client = HttpClient.newHttpClient();
    public final List<Thread> threadList = new ArrayList<>();
    private String target = "";
    private int threads;

    private final String clientId = "jzkbprff40iqj646a697cyrvl0zt2m6";


    public ViewBot(String target, int threads) {
        this.target = target;
        this.threads = threads;
    }

    public ViewBot() {
    }


    public void start() {
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean stopCall = new AtomicBoolean(false);
        while (counter.get() < threads) {
            Thread thread = new Thread(() -> {
//                String[] fullIp = fullProxyList.poll().split(":");
//                String ip = fullIp[0];
//                int port = Integer.parseInt(fullIp[1]);
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .proxy(ProxySelector.of(new InetSocketAddress("176.119.142.63", 10227)))
                        .build();
                String[] info = new String[0];
                try {
                    info = getInfo(client);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (info.length == 0) {
                    System.out.println("Bad Proxy. Continuing...");
                    return;
                }
                String token = info[0];
                String sig = info[1];
                System.out.println("token: " + token + "\n" + "sig: " + sig);
                String videoSequenceURL = getVideoSequence(client, token, sig);
                if (!sendView(client, videoSequenceURL)) {
                    Thread.currentThread().interrupt();
                    stopCall.set(true);
                }
                System.out.println(counter.getAndIncrement());
            });
            if (stopCall.get()) {
                break;
            }
            threadList.add(thread);
            thread.start();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stop();
    }

    private void stop() {
        threadList.forEach(Thread::interrupt);
    }

    private boolean sendView(HttpClient client, String url) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .setHeader("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain")
                    .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36")
                    .build();
            HttpResponse<String> response = null;
            try {
                response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Proxy overflow. ");
                return false;
            }
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
    }

    private String getVideoSequence(HttpClient client, String token, String sig) {
        String url = String.format("https://usher.ttvnw.net/api/channel/hls/%s.m3u8?" +
                "allow_source=true&baking_bread=true&baking_brownies=true&baking_brownies_timeout=1050&fast_bread=true&p=3168255&player_backend=mediaplayer&" +
                "playlist_include_framerate=true&reassignments_supported=false&rtqos=business_logic_reverse&cdm=wv&sig=%s&token=%s", target, sig, token);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .setHeader("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain")
                .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36")
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        if (response == null) return "";
        String responseBody = response.body();
        String result = "https://" + responseBody.substring(responseBody.indexOf("https://") + "https://".length(), responseBody.indexOf(".m3u8")) + ".m3u8";
        System.out.println(result);
        return result;
    }

    private String[] getInfo(HttpClient client) throws JSONException {
        String[] resultArray = new String[2];
        String url = String.format("https://api.twitch.tv/api/channels/" +
                "%s/access_token?need_https=true&oauth_token=&" +
                "platform=web&player_backend=mediaplayer&player_type=site&client_id=%s", target, clientId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36")
                .setHeader("Accept", "application/vnd.twitchtv.v5+json; charset=UTF-8")
                .setHeader("Content-Type", "application/json; charset=UTF-8")
                .setHeader("Accept-Language", "en-us")
                .setHeader("Referer", "https://www.twitch.tv/" + target)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            return new String[0];
        }
        if (response == null) return new String[0];
        JSONObject jsonObject = new JSONObject(response.body());
        try {
            resultArray[0] = URLEncoder.encode(jsonObject.getString("token").replace("\\", ""), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        resultArray[1] = jsonObject.getString("sig").replace("\\", "");
        return resultArray;
    }

    public ViewBot setTarget(String target) {
        this.target = target;
        return this;
    }

    public ViewBot setThreads(int threads) {
        this.threads = threads;
        return this;
    }

    public String getTarget() {
        return target;
    }

    public int getThreads() {
        return threads;
    }
}
