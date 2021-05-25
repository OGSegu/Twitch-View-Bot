package viewbot;

import controller.Controller;
import javafx.application.Platform;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import utils.HttpClient;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class ViewBot {
    private static final String CLIENT_ID = "b31o4btkqth5bzbvr9ub2ovr79umhh";

    private static final String USER_AGENT_HEADER_NAME = "User-Agent";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";
    private static final String ACCEPT_HEADER_NAME = "Accept";
    private static final String ACCEPT_VIDEO = "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain";
    private static final String GET_VIDEO = "https://usher.ttvnw.net/api/channel/hls/%s.m3u8?" +
            "allow_source=true&baking_bread=true&baking_brownies=true&baking_brownies_timeout=1050&fast_bread=true&p=3168255&player_backend=mediaplayer&" +
            "playlist_include_framerate=true&reassignments_supported=false&rtqos=business_logic_reverse&cdm=wv&sig=%s&token=%s";
    private static final String ACCEPT_INFO = "application/vnd.twitchtv.v5+json; charset=UTF-8";
    private static final String GET_INFO = "https://api.twitch.tv/api/channels/" +
            "%s/access_token?need_https=true&oauth_token=&" +
            "platform=web&player_backend=mediaplayer&player_type=site&client_id=%s";
    private static final String ACCEPT_LANG = "en-us";
    private static final String CONTENT_INFO = "application/json; charset=UTF-8";
    private static final String REFERER = "https://www.twitch.tv/";
    public static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
    public static final String ACCEPT_LANGUAGE_HEADER_NAME = "Accept-Language";
    public static final String REFERER_HEADER_NAME = "Referer";


    private ExecutorService threadPool;
    private LinkedBlockingQueue<String> proxyQueue;
    private String target;
    private final Controller controller;

    private int threads;

    public ViewBot(Controller controller, LinkedBlockingQueue<String> proxyQueue, String target) {
        this.controller = controller;
        this.proxyQueue = proxyQueue;
        this.target = target;
    }

    private void writeToLog(String msg) {
        Platform.runLater(() ->
                controller.writeToLog(msg)
        );
    }

    public void start() {
        threadPool = Executors.newFixedThreadPool(threads);
        writeToLog("Viewbot has been started with: " + threads + " threads");
        for (int i = 0; i < threads; i++) {
            this.threadPool.execute(getExecutable());
        }
        while (!controller.getStartButton().getText().equals("START")) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        stop();
    }

    private Runnable getExecutable() {
        return () -> {
            String[] fullIp = new String[0];
            try {
                fullIp = proxyQueue.take().split(":");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String ip = fullIp[0];
            int port = Integer.parseInt(fullIp[1]);
            HttpClient httpClient = new HttpClient(ip, port);
            String[] info;
            try {
                info = getInfo(httpClient);
                if (info.length == 0) {
                    writeToLog("Bad proxy. Continuing...");
                    return;
                }
                String token = info[0];
                String sig = info[1];
                boolean viewWasSent = false;
                while (true) {
                    String videoSequenceURL = getVideoSequence(httpClient, token, sig);
                    if (videoSequenceURL.isEmpty())
                        return;
                    sendView(httpClient, videoSequenceURL);
                    if (!viewWasSent) {
                        viewWasSent = true;
                        Platform.runLater(controller::addCount);
                    }
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException | JSONException e) {
                writeToLog("Bad proxy: " + ip);
            }
        };
    }


    public void stop() {
        threadPool.shutdown();
        threadPool.shutdownNow();
        while (true) {
            try {
                writeToLog("Shutdowning threads...");
                if (threadPool.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Platform.runLater(controller::stopViewBot);
    }


    private void sendView(HttpClient client, String url) throws IOException {
        HttpHead headRequest = new HttpHead(url);
        headRequest.setHeader(USER_AGENT_HEADER_NAME, USER_AGENT);
        headRequest.setHeader(ACCEPT_HEADER_NAME, ACCEPT_VIDEO);
        client.client.execute(headRequest);
    }

    private String getVideoSequence(HttpClient client, String token, String sig) throws IOException {
        String url = String.format(GET_VIDEO, target, sig, token);
        System.out.println(url);
        HttpGet getRequest = new HttpGet(url);
        getRequest.setHeader(USER_AGENT_HEADER_NAME, USER_AGENT);
        getRequest.setHeader(ACCEPT_HEADER_NAME, ACCEPT_VIDEO);
        CloseableHttpResponse response = client.client.execute(getRequest);
        String body;
        body = EntityUtils.toString(response.getEntity());

        if (body == null) {
            writeToLog("Can't get video sequence");
            return "";
        }
        return "https://" + body.substring(body.indexOf("https://") + "https://".length(), body.indexOf(".m3u8")) + ".m3u8";
    }

    private String[] getInfo(HttpClient client) throws JSONException, IOException {
        String[] resultArray = new String[2];
        String url = String.format(GET_INFO, target, CLIENT_ID);
        HttpGet getRequest = new HttpGet(url);
        getRequest.setHeader(USER_AGENT_HEADER_NAME, USER_AGENT);
        getRequest.setHeader(ACCEPT_HEADER_NAME, ACCEPT_INFO);
        getRequest.setHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_INFO);
        getRequest.setHeader(ACCEPT_LANGUAGE_HEADER_NAME, ACCEPT_LANG);
        getRequest.setHeader(REFERER_HEADER_NAME, REFERER + target);
        CloseableHttpResponse response = client.client.execute(getRequest);
        String body;
        try {
            body = EntityUtils.toString(response.getEntity());
        } finally {
            response.close();
        }
        JSONObject jsonObject = new JSONObject(body);
        resultArray[0] = URLEncoder.encode(jsonObject.getString("token").replace("\\", ""), StandardCharsets.UTF_8);
        resultArray[1] = jsonObject.getString("sig").replace("\\", "");
        return resultArray;
    }

    public Queue<String> getProxyQueue() {
        return proxyQueue;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public ViewBot setTarget(String target) {
        this.target = target;
        return this;
    }

    public ViewBot setProxyQueue(LinkedBlockingQueue<String> proxyQueue) {
        this.proxyQueue = proxyQueue;
        return this;
    }
}
