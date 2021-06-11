package viewbot;

import config.Config;
import controller.ControllerMain;
import javafx.application.Platform;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import service.TwitchUtil;
import utils.HttpClient;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static service.TwitchUtil.CLIENT_ID;


public class ViewBot {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";
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


    private ExecutorService threadPool;
    private LinkedBlockingQueue<String> proxyQueue;
    private String target;
    private final ControllerMain controllerMain;
    private final TwitchUtil twitchUtil = new TwitchUtil();

    private int threads;

    private Thread waitingThread;

    public ViewBot(ControllerMain controllerMain, LinkedBlockingQueue<String> proxyQueue, String target) {
        this.controllerMain = controllerMain;
        this.proxyQueue = proxyQueue;
        this.target = target;
    }

    private void writeToLog(String msg) {
        Platform.runLater(() ->
                controllerMain.writeToLog(msg)
        );
    }

    public void prepareToStart() {
        if (Config.startWhenLiveValue) {
            try {
                String channelId = twitchUtil.getChannelId(target);
                Runnable waitingRunnable = getWaitingRunnable(channelId);
                waitingThread = new Thread(waitingRunnable);
                waitingThread.start();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                writeToLog("Failed to get channel status");
            }
        }
    }

    private Runnable getWaitingRunnable(String channelId) {
        return () -> {
            synchronized (this) {
                while (true) {
                    try {
                        if (Thread.currentThread().isInterrupted()) break;
                        writeToLog("Waiting when channel goes live");
                        if (twitchUtil.isChannelLive(channelId)) break;
                    } catch (IOException e) {
                        writeToLog("Can't get channel status");
                    }
                    try {
                        wait((long) Config.repeatEveryMinutesValue * 1000 * 60);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
    }


    public void start() {
        threadPool = Executors.newFixedThreadPool(threads);
        writeToLog("Viewbot has been started with: " + threads + " threads");
        for (int i = 0; i < threads; i++) {
            this.threadPool.execute(getExecutable());
        }
        try {
            if (!Config.stopWhenOfflineValue) {
                Thread.sleep((long) Config.stopAfterHsValue * 1000 * 60 * 60);
            } else {
                while (controllerMain.getStartButton().getText().equals("START")) {
                    if (!twitchUtil.isChannelLive(target)) break;
                    Thread.sleep(2000);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            writeToLog("Failed get channel status");
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
                        HttpClient httpClient;
            if (fullIp.length == 4) {
                String user = fullIp[2];
                String pass = fullIp[3];
                httpClient = new HttpClient(ip, port, user, pass);
            } else if (fullIp.length == 2) {
                httpClient = new HttpClient(ip, port);
            } else {
                writeToLog("Invalid proxy configuration. Continuing...");
                return;
            }
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
                        Platform.runLater(controllerMain::addCount);
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
        if (waitingThread.isAlive()) {
            waitingThread.interrupt();
        } else if (threadPool != null) {
            new Thread(() -> {
                writeToLog("Shutdowning threads...");
                writeToLog("Wait until console has been not cleared");
                threadPool.shutdown();
                threadPool.shutdownNow();
                try {
                    if (threadPool.awaitTermination(20000, TimeUnit.MILLISECONDS)) {
                        threadPool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    threadPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(controllerMain::stopViewBot);
            }).start();
        }
    }


    private void sendView(HttpClient client, String url) throws IOException {
        HttpHead headRequest = new HttpHead(url);
        headRequest.setHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        headRequest.setHeader(HttpHeaders.ACCEPT, ACCEPT_VIDEO);
        client.client.execute(headRequest);
    }

    private String getVideoSequence(HttpClient client, String token, String sig) throws IOException {
        String url = String.format(GET_VIDEO, target, sig, token);
        System.out.println(url);
        HttpGet getRequest = new HttpGet(url);
        getRequest.setHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        getRequest.setHeader(HttpHeaders.ACCEPT, ACCEPT_VIDEO);
        CloseableHttpResponse response = client.client.execute(getRequest);
        String body;
        body = EntityUtils.toString(response.getEntity());

        if (body == null) {
            writeToLog("Can't get video sequence");
            return "";
        }
        return "https://" + body.substring(body.indexOf("https://") + "https://".length(), body.indexOf(".m3u8")) + ".m3u8";
    }

    private String[] getInfo(HttpClient httpClient) throws JSONException, IOException {
        String[] resultArray = new String[2];
        String url = String.format(GET_INFO, target, CLIENT_ID);
        HttpGet getRequest = new HttpGet(url);
        getRequest.setHeader(HttpHeaders.USER_AGENT, USER_AGENT);
        getRequest.setHeader(HttpHeaders.ACCEPT, ACCEPT_INFO);
        getRequest.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_INFO);
        getRequest.setHeader(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANG);
        getRequest.setHeader(HttpHeaders.REFERER, REFERER + target);
        CloseableHttpResponse response = httpClient.client.execute(getRequest);
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
	
	    public ViewBot setProxyQueue(LinkedBlockingQueue<String> proxyQueue) {
        this.proxyQueue = proxyQueue;
        return this;
    }


    public void setThreads(int threads) {
        this.threads = threads;
    }

    public ViewBot setTarget(String target) {
        this.target = target;
        return this;
    }


}
