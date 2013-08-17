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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
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
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.net.Uri;

public class RelaxedAuthenticatingHttpConnection {

	private static class TrustingSslSocketFactory implements LayeredSocketFactory {
		private static TrustingSslSocketFactory _instance;

		public static TrustingSslSocketFactory getSocketFactory() {
			if (_instance == null) {
				_instance = new TrustingSslSocketFactory();
				try {
					TrustManager manager = new X509TrustManager() {
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
					};
					_instance._context = SSLContext.getInstance("TLS");
					_instance._context.init(null, new TrustManager[] { manager }, new SecureRandom());
					_instance._factory = _instance._context.getSocketFactory();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return _instance;
		}

		private SSLContext _context;
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

	public RelaxedAuthenticatingHttpConnection(Uri uri, String username, String password) {
		try {
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", TrustingSslSocketFactory.getSocketFactory(), 443));

			BasicHttpParams params = new BasicHttpParams();
			ClientConnectionManager manager = new ThreadSafeClientConnManager(params, registry);
			DefaultHttpClient client = new DefaultHttpClient(manager, params);

			if (username != null) {
				CredentialsProvider provider = new BasicCredentialsProvider();
				provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
				client.setCredentialsProvider(provider);
			}

			HttpGet get = new HttpGet(uri.toString());
			HttpResponse response = client.execute(get);
			_stream = response.getEntity().getContent();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public InputStream getInputStream() {
		return _stream;
	}

}
