package utils;

public final class Configs {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36";

    public static final String ACCEPT_VIDEO = "application/x-mpegURL, application/vnd.apple.mpegurl, application/json, text/plain";

    public static final String GET_VIDEO = "https://usher.ttvnw.net/api/channel/hls/%s.m3u8?" +
            "allow_source=true&baking_bread=true&baking_brownies=true&baking_brownies_timeout=1050&" +
            "fast_bread=true&p=3168255&player_backend=mediaplayer&playlist_include_framerate=true&" +
            "reassignments_supported=false&rtqos=business_logic_reverse&cdm=wv&sig=%s&token=%s";

    public static final String ACCEPT_INFO = "application/vnd.twitchtv.v5+json; charset=UTF-8";

    public static final String GET_INFO = "https://api.twitch.tv/api/channels/" +
            "%s/access_token?need_https=true&oauth_token=&" +
            "platform=web&player_backend=mediaplayer&player_type=site&client_id=%s";

    public static final String ACCEPT_LANG = "en-us";

    public static final String CONTENT_INFO = "application/json; charset=UTF-8";

    public static final String REFERER = "https://www.twitch.tv/";

    private Configs() {
        // to prevent from being explicitly instantiated
    }

}
