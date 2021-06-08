package utils;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

public class HttpClient {

    public final CloseableHttpClient client;

    public HttpClient(String ip, int port) {
        this(ip, port, null, null);
    }

    public HttpClient(String ip, int port, String user, String pass) {
        HttpHost superProxy = new HttpHost(ip, port);
        HttpClientBuilder builder = HttpClients.custom();
        if (user != null && pass != null) {
            CredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(new AuthScope(superProxy), new UsernamePasswordCredentials(user, pass));
            builder.setDefaultCredentialsProvider(provider);
        }
        client = builder
                .setConnectionManager(new BasicHttpClientConnectionManager())
                .setProxy(superProxy)
                .build();
    }

    public HttpClient() {
        client = HttpClients.createDefault();
    }

}
