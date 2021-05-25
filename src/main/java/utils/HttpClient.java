package utils;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

public class HttpClient {
    public final CloseableHttpClient client;

    public HttpClient(String ip, int port) {
        HttpHost superProxy = new HttpHost(ip, port);
        client = HttpClients.custom()
                .setConnectionManager(new BasicHttpClientConnectionManager())
                .setProxy(superProxy)
                .build();
    }

    public HttpClient() {
        client = HttpClients.createDefault();
    }
}