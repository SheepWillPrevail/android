package com.grazz.pebblerss.feed;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import android.net.Uri;

public class ExtendedHttpClient {

	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";

	private class GzipInflatingEntity extends HttpEntityWrapper {
		public GzipInflatingEntity(final HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}

	private static class TrustingSSLSocketFactory implements LayeredSocketFactory {
		private static TrustingSSLSocketFactory _instance;

		public static TrustingSSLSocketFactory getSocketFactory() {
			if (_instance == null) {
				try {
					SSLContext context = SSLContext.getInstance("TLS");
					if (context != null) {
						context.init(null, new TrustManager[] { new X509TrustManager() {
							@Override
							public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
							}

							@Override
							public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
							}

							@Override
							public X509Certificate[] getAcceptedIssuers() {
								return null;
							}
						} }, new SecureRandom());
						_instance = new TrustingSSLSocketFactory();
						_instance._factory = context.getSocketFactory();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return _instance;
		}

		private SSLSocketFactory _factory;

		@Override
		public Socket connectSocket(Socket sock, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException,
				UnknownHostException, ConnectTimeoutException {
			if (sock == null)
				sock = createSocket();
			if (localAddress != null)
				sock.bind(new InetSocketAddress(localAddress, localPort));
			sock.connect(new InetSocketAddress(host, port));
			return sock;
		}

		@Override
		public Socket createSocket() throws IOException {
			return _factory.createSocket();
		}

		@Override
		public boolean isSecure(Socket sock) throws IllegalArgumentException {
			return true;
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
			return _factory.createSocket(socket, host, port, autoClose);
		}
	}

	private InputStream _stream;

	public ExtendedHttpClient(Uri uri, String username, String password) {
		try {
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", TrustingSSLSocketFactory.getSocketFactory(), 443));

			BasicHttpParams params = new BasicHttpParams();
			params.setParameter(CoreProtocolPNames.USER_AGENT, "PebbleRSS/1.1");

			ClientConnectionManager manager = new ThreadSafeClientConnManager(params, registry);
			DefaultHttpClient client = new DefaultHttpClient(manager, params);

			if (username != null) {
				CredentialsProvider provider = new BasicCredentialsProvider();
				provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
				client.setCredentialsProvider(provider);
			}

			client.addRequestInterceptor(new HttpRequestInterceptor() {
				public void process(HttpRequest request, HttpContext context) {
					if (!request.containsHeader(HEADER_ACCEPT_ENCODING))
						request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
				}
			});

			client.addResponseInterceptor(new HttpResponseInterceptor() {
				public void process(HttpResponse response, HttpContext context) {
					final HttpEntity entity = response.getEntity();
					final Header encoding = entity.getContentEncoding();
					if (encoding != null)
						for (HeaderElement element : encoding.getElements())
							if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
								response.setEntity(new GzipInflatingEntity(response.getEntity()));
								break;
							}
				}
			});

			HttpGet get = new HttpGet(uri.toString());
			HttpResponse response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200)
				_stream = response.getEntity().getContent();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public InputStream getInputStream() {
		return _stream;
	}

}
