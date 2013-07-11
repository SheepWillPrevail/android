package com.grazz.pebblereactor.dataprovider;

import twitter4j.DirectMessage;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.os.Bundle;
import android.util.Log;

import com.grazz.pebblereactor.DataProvider;
import com.grazz.pebblereactor.ReactorService;

public class TwitterDataProvider extends DataProvider {

	private final static String CONSUMER_KEY = "4IDFoSIAZvZR72tv7h55nw";
	private final static String CONSUMER_SECRET = "7yCVAO9syyHdJmlKlNTr3FxQWooQMiHrl3ULyS47Z0";
	private String _accessToken, _accessSecret;

	public TwitterDataProvider(ReactorService reactor) {
		super(reactor);

		AccountManager accountManager = AccountManager.get(getContext());
		Account[] accounts = accountManager.getAccountsByType("com.twitter.android.auth.login");
		if (accounts.length > 0) {
			Account account = accounts[0];

			accountManager.getAuthToken(account, "com.twitter.android.oauth.token", null, true, new AccountManagerCallback<Bundle>() {

				@Override
				public void run(AccountManagerFuture<Bundle> future) {
					try {
						setToken(future.getResult().getString(AccountManager.KEY_AUTHTOKEN));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}, null);

			accountManager.getAuthToken(account, "com.twitter.android.oauth.token.secret", null, true, new AccountManagerCallback<Bundle>() {

				@Override
				public void run(AccountManagerFuture<Bundle> future) {
					try {
						setSecret(future.getResult().getString(AccountManager.KEY_AUTHTOKEN));
						doSomething();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}, null);
		} else
			Log.w(TwitterDataProvider.class.getName(), "Twitter account not found.");
	}

	private void setToken(String token) {
		if (token != null)
			Log.d("setToken", token);
		_accessToken = token;
	}

	private void setSecret(String secret) {
		if (secret != null)
			Log.d("setSecret", secret);
		_accessSecret = secret;
	}

	private void doSomething() {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(CONSUMER_KEY);
				builder.setOAuthConsumerSecret(CONSUMER_SECRET);
				builder.setOAuthAccessToken(_accessToken);
				builder.setOAuthAccessTokenSecret(_accessSecret);
				builder.setUseSSL(true);

				Twitter twitter = new TwitterFactory(builder.build()).getInstance();
				try {
					ResponseList<DirectMessage> directMessages = twitter.getDirectMessages();
					for (DirectMessage msg : directMessages) {
						Log.d("doSomething", msg.getText());
					}

				} catch (TwitterException e) {
					e.printStackTrace();
				}
			}

		});
		thread.start();
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStop() {

	}

}
