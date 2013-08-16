package com.grazz.pebblerss.rss;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.net.Uri;

public class FeedParserFactory {

	private final static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	public static FeedParser getParser(Uri link) {
		FeedParser parser = null;
		Document document = null;

		InputStream stream = null;
		try {
			stream = new URL(link.toString()).openStream();
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(stream);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
				}
		}

		if (document != null) {
			Element documentElement = document.getDocumentElement();
			if (documentElement != null)
				if (documentElement.getNodeName().equalsIgnoreCase("feed"))
					parser = new AtomParser(documentElement);
				else if (documentElement.getNodeName().equalsIgnoreCase("rss"))
					parser = new RSSParser(documentElement);
		}

		return parser;
	}

}
