/* Copyright (C) Reazon Systems, Inc.  All rights reserved. */
package com.reazon.tool.irubricpatch.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author CD
 * 
 */
public class Helper {
	private static final Log LOG = LogFactory.getLog(Helper.class);

	public static final String EMPTY_STRING = "";

	/**
	 * Reading response data from the connection
	 * 
	 * @param connection
	 * @return
	 */
	public static String getResponseData(HttpURLConnection connection)
			throws Exception {
		BufferedReader rd = null;
		StringBuilder sb = null;
		String line = null;
		String result = null;

		// read the results from the server
		rd = new BufferedReader(new InputStreamReader(connection
				.getInputStream()));

		sb = new StringBuilder();

		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}

		// convert response data to UTF-8 encoding
		byte[] utf8 = sb.toString().trim().getBytes("UTF-8");
		result = new String(utf8, "UTF-8");

		return result;
	}

	/**
	 * Create an HTTP POST connection with the specified URL
	 * 
	 * @param url
	 * @param timeout
	 * @return An HTTP connection
	 * @throws IOException
	 */
	public static HttpURLConnection createHttpURLConnection(String url,
			int timeout)
			throws IOException {
		HttpURLConnection connection = null;
		URL serverAddress = null;

		serverAddress = new URL(url);

		if (LOG.isDebugEnabled()) {
			LOG.debug("iRubric Servlet: Open connection to " + url);
		}

		connection = (HttpURLConnection) serverAddress.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setReadTimeout(timeout);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setAllowUserInteraction(true);
		connection.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");

		return connection;
	}

	/**
	 * Create an HTTP GET connection with the specified URL
	 * 
	 * @param url
	 * @param timeout
	 * @param trustStorePws
	 *            A password which is used to authenticate with iRubric server
	 * @return An HTTP connection
	 * @throws IOException
	 */
	public static HttpURLConnection createHttpURLGetConnection(String url,
			int timeout)
			throws IOException {
		HttpURLConnection connection = null;
		URL serverAddress = null;

		serverAddress = new URL(url);

		connection = (HttpURLConnection) serverAddress.openConnection();
		connection.setRequestMethod("GET");
		connection.setDoOutput(true);
		connection.setReadTimeout(timeout);

		connection.connect();

		return connection;
	}

	/**
	 * Add a name-value pair to the URL parameter
	 * 
	 * @param strings
	 * @return a string
	 */
	public static void addUrlParam(StringBuilder builder, String paramName,
			String paramValue) {
		if (builder.toString().length() != 0) {
			builder.append("&");
		}
		builder.append(paramName);
		builder.append("=");
		builder.append(paramValue);
	}
}
