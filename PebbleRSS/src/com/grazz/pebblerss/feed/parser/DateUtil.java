package com.grazz.pebblerss.feed.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {

	private final static SimpleDateFormat dateFormats[] = new SimpleDateFormat[] { new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH), new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH), new SimpleDateFormat("EEE, d MMM yy HH:mm:ss z", Locale.ENGLISH),
			new SimpleDateFormat("EEE, d MMM yy HH:mm z", Locale.ENGLISH), new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH),
			new SimpleDateFormat("EEE, d MMM yyyy HH:mm z", Locale.ENGLISH), new SimpleDateFormat("EEE d MMM yy HH:mm:ss z", Locale.ENGLISH),
			new SimpleDateFormat("EEE d MMM yy HH:mm z", Locale.ENGLISH), new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss z", Locale.ENGLISH),
			new SimpleDateFormat("EEE d MMM yyyy HH:mm z", Locale.ENGLISH), new SimpleDateFormat("d MMM yy HH:mm z", Locale.ENGLISH),
			new SimpleDateFormat("d MMM yy HH:mm:ss z", Locale.ENGLISH), new SimpleDateFormat("d MMM yyyy HH:mm z", Locale.ENGLISH),
			new SimpleDateFormat("d MMM yyyy HH:mm:ss z", Locale.ENGLISH), };

	static {
		TimeZone zone = TimeZone.getTimeZone("UTC");
		for (SimpleDateFormat format : dateFormats)
			format.setTimeZone(zone);
	}

	public static Date parseDate(String date) {
		if (date == null)
			return null;

		for (SimpleDateFormat format : dateFormats) {
			try {
				return format.parse(date);
			} catch (ParseException e) {
			}
		}

		return null;
	}

}
