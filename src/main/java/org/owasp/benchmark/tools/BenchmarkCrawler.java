/**
* OWASP Benchmark Project
*
* This file is part of the Open Web Application Security Project (OWASP)
* Benchmark Project For details, please see
* <a href="https://owasp.org/www-project-benchmark/">https://owasp.org/www-project-benchmark/</a>.
*
* The OWASP Benchmark is free software: you can redistribute it and/or modify it under the terms
* of the GNU General Public License as published by the Free Software Foundation, version 2.
*
* The OWASP Benchmark is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
* even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details
*
* @author Juan Gama
* @created 2017
*/

package org.owasp.benchmark.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.StopWatch;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.owasp.benchmark.helpers.Utils;
import org.owasp.benchmark.score.BenchmarkScore;

public class BenchmarkCrawler {
	public static String testSuiteVersion = "";

	protected void init() {
		try {
			String crawlerFile = Utils.DATA_DIR + "benchmark-crawler-http.xml";
			testSuiteVersion = Utils.getCrawlerBenchmarkVersion(new FileInputStream(crawlerFile));
			crawl(new FileInputStream(crawlerFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void crawl(InputStream http) throws Exception {
		CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(Utils.getSSLFactory()).build();
		long start = System.currentTimeMillis();

		List<AbstractTestCaseRequest> requests = Utils.parseHttpFile(http);

		for (AbstractTestCaseRequest request : requests) {
			try {
				sendRequest(httpclient, request);
			} catch (Exception e) {
				System.err.println("\n  FAILED: " + e.getMessage());
				e.printStackTrace();
			}
		}
		long stop = System.currentTimeMillis();
		double seconds = (stop - start) / 1000;

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();

		System.out.println("Crawl ran on " + dateFormat.format(date) + " for " + BenchmarkScore.TESTSUITE
				+ " v" + testSuiteVersion + " took " + seconds + " seconds");
	}

	/**
	 * Issue the requested request, measure the time required to execute, then
	 * output both to stdout and the global
	 * variable timeString the URL tested, the time required to execute and the
	 * response code.
	 * 
	 * @param httpclient
	 *            - The HTTP client to use to make the request
	 * @param request
	 *            - THe HTTP request to issue
	 * @throws IOException
	 */
	protected ResponseInfo sendRequest(CloseableHttpClient httpclient, AbstractTestCaseRequest requestTC) {
		ResponseInfo responseInfo = new ResponseInfo();
		HttpRequestBase request = requestTC.buildRequest();
		responseInfo.setRequestBase(request);
		CloseableHttpResponse response = null;

		boolean isPost = request instanceof HttpPost;
		System.out.println((isPost ? "POST " : "GET ") + request.getURI());
		StopWatch watch = new StopWatch();

		watch.start();
		try {
			response = httpclient.execute(request);
		} catch (IOException e) {
			e.printStackTrace();
		}
		watch.stop();

		try {
			HttpEntity entity = response.getEntity();
			int statusCode = response.getStatusLine().getStatusCode();
			responseInfo.setStatusCode(statusCode);
			double time = watch.getTime() / 1000;
			responseInfo.setTime(time);
			String outputString = "--> (" + String.valueOf(statusCode) + " : " + time + " sec) ";
			System.out.println(outputString);

			try {
				responseInfo.setResponseString(EntityUtils.toString(entity));
				EntityUtils.consume(entity);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} finally {
			if (response != null)
				try {
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return responseInfo;
	}

	public static void main(String[] args) throws Exception {
		BenchmarkCrawler crawler = new BenchmarkCrawler();
		crawler.init();
	}
}

class ResponseInfo {
	private String responseString;
	private double time;
	private int statusCode;
	private HttpRequestBase requestBase;

	public String getResponseString() {
		return responseString;
	}

	public void setResponseString(String responseString) {
		this.responseString = responseString;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public HttpRequestBase getRequestBase() {
		return requestBase;
	}

	public void setRequestBase(HttpRequestBase requestBase) {
		this.requestBase = requestBase;
	}
}

