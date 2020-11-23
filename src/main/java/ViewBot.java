import javafx.application.Platform;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;


public class ViewBot {

    private ThreadPoolExecutor threadPoolExecutor;

    private String target = "";
    private int threads;

    private final Controller controller;

    private Queue<String> fullProxyList;


    public ViewBot(Controller controller) {
        this.controller = controller;
    }

    public void loadProxy(File file) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String proxy;
            fullProxyList = new ArrayBlockingQueue<>(100000);
            while ((proxy = bufferedReader.readLine()) != null) {
                fullProxyList.add(proxy);
            }
        } catch (IOException e) {
            System.out.println("Something went wrong");
        }
        writeToLog("Proxy loaded: " + fullProxyList.size());
    }

    private void writeToLog(String msg) {
        Platform.runLater(() ->
                controller.writeToLog(msg + "\n")
        );
    }

    public void start() {
        writeToLog("Viewbot has been started with: " + threads + " threads");
        while (!threadPoolExecutor.isTerminated()) {
            threadPoolExecutor.execute(() -> {
                if (fullProxyList.size() == 0) stop();
                String[] fullIp = fullProxyList.poll().split(":");
                String ip = fullIp[0];
                int port = Integer.parseInt(fullIp[1]);
                HttpClient httpClient = new HttpClient(ip, port);
                String[] info = new String[0];
                try {
                    info = getInfo(httpClient);
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                if (info.length == 0) {
                    writeToLog("Bad proxy. Continuing...");
                    return;
                }
                String token = info[0];
                String sig = info[1];
                boolean viewWasSent = false;
                do {
                    String videoSequenceURL;
                    try {
                        videoSequenceURL = getVideoSequence(httpClient, token, sig);
                        if (videoSequenceURL.isEmpty())
                            return;
                        sendView(httpClient, videoSequenceURL);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!viewWasSent) {
                        viewWasSent = true;
                        Platform.runLater(controller::addCount);
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (!controller.getStartButton().getText().equals("START"));
                stop();
            });
        }
    }


    public void stop() {
        threadPoolExecutor.shutdownNow();
        Platform.runLater(controller::resetCount);
    }

    private boolean sendView(HttpClient client, String url) throws IOException {
        HttpHead headRequest = new HttpHead(url);
        headRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        headRequest.setHeader("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain");
        client.client.execute(headRequest);
        return true;
    }

    private String getVideoSequence(HttpClient client, String token, String sig) throws IOException {
        String url = String.format("https://usher.ttvnw.net/api/channel/hls/%s.m3u8?" +
                "allow_source=true&baking_bread=true&baking_brownies=true&baking_brownies_timeout=1050&fast_bread=true&p=3168255&player_backend=mediaplayer&" +
                "playlist_include_framerate=true&reassignments_supported=false&rtqos=business_logic_reverse&cdm=wv&sig=%s&token=%s", target, sig, token);
        HttpGet getRequest = new HttpGet(url);
        getRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        getRequest.setHeader("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain");
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
        String clientId = "b31o4btkqth5bzbvr9ub2ovr79umhh"; // PUT ANY WORKING CLIENT-ID
        String url = String.format("https://api.twitch.tv/api/channels/" +
                "%s/access_token?need_https=true&oauth_token=&" +
                "platform=web&player_backend=mediaplayer&player_type=site&client_id=%s", target, clientId);
        HttpGet getRequest = new HttpGet(url);
        getRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        getRequest.setHeader("Accept", "application/vnd.twitchtv.v5+json; charset=UTF-8");
        getRequest.setHeader("Content-Type", "application/json; charset=UTF-8");
        getRequest.setHeader("Accept-Language", "en-us");
        getRequest.setHeader("Referer", "https://www.twitch.tv/" + target);
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

    public boolean isChannelNameValid(String target) {
        return !target.isBlank() && !target.isEmpty();
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    public String getTarget() {
        return target;
    }

    public int getThreads() {
        return threads;
    }

    public Controller getController() {
        return controller;
    }

    public Queue<String> getFullProxyList() {
        return fullProxyList;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void setFullProxyList(Queue<String> fullProxyList) {
        this.fullProxyList = fullProxyList;
    }
}
