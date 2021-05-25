package service;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utils.HttpClient;

import java.io.IOException;

public class TwitchUtil {

    public static final String CLIENT_ID = "b31o4btkqth5bzbvr9ub2ovr79umhh";
    private static final String CLIENT_ID_HEADER_NAME = "Client-ID";

    private static final String CHANNEL_INFO = "https://api.twitch.tv/kraken/users?login=";
    private static final String CHANNEL_STREAM = "https://api.twitch.tv/kraken/streams/";

    private final HttpClient httpClient = new HttpClient();


    public HttpGet createHttpGet(String url) {
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader(HttpHeaders.ACCEPT, "application/vnd.twitchtv.v5+json");
        httpGet.addHeader(CLIENT_ID_HEADER_NAME, CLIENT_ID);
        return httpGet;
    }

    public String getChannelId(String login) throws IOException, JSONException {
        HttpGet httpGet = createHttpGet(CHANNEL_INFO + login);
        CloseableHttpResponse response = httpClient.client.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);
        EntityUtils.consume(entity);
        response.close();

        String channelId;
        JSONObject jsonObject = new JSONObject(body);
        JSONArray usersArray = jsonObject.getJSONArray("users");
        JSONObject userObject = usersArray.getJSONObject(0);
        channelId = userObject.getString("_id");

        return channelId;
    }

    public boolean isChannelLive(String channelId) throws IOException, JSONException {
        HttpGet httpGet = createHttpGet(CHANNEL_STREAM + channelId);
        CloseableHttpResponse response = httpClient.client.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String body = EntityUtils.toString(entity);
        EntityUtils.consume(entity);
        response.close();

        JSONObject jsonObject = new JSONObject(body);
        return !jsonObject.isNull("stream");
    }
}
