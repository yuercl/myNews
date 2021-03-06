/**
 * project name: myNews
 * created at 2013-3-6 - 下午2:16:29
 * author:yuer
 * email:yuerguang.cl@gmail.com
 */
package com.uestc.news.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.parser.XmlTreeBuilder;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {
	private static Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	private static final int CONNECTION_POOL_SIZE = 10;
	private static final int TIMEOUT_SECONDS = 20;

	private HttpClient httpclient;
	private HttpResponse response;

	public HttpUtils() {
		PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
		cm.setMaxTotal(CONNECTION_POOL_SIZE);
		httpclient = new DefaultHttpClient(cm);
		httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
		HttpParams httpParams = httpclient.getParams();

		List<Header> headers = new ArrayList<Header>();
		BasicHeader header = new BasicHeader("User-Agent", Config.getUserAgent());
		headers.add(header);
		httpclient.getParams().setParameter("http.default-headers", headers);
		httpclient.getParams().setParameter("http.protocol.content-charset", "UTF-8");
		HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_SECONDS * 1000);
	}

	public List<Cookie> getCookies() {
		return ((AbstractHttpClient) httpclient).getCookieStore().getCookies();
	}

	public Map<String, String> getMapCookies() {
		Map<String, String> map = new HashMap<String, String>();
		List<Cookie> cookies = ((AbstractHttpClient) httpclient).getCookieStore().getCookies();
		for (Cookie c : cookies) {
			map.put(c.getName(), c.getValue());
		}
		return map;
	}

	public String getCookie(String key) {
		for (Cookie c : ((AbstractHttpClient) httpclient).getCookieStore().getCookies()) {
			if (c.getName().equals("key"))
				return c.getValue();
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	public String get(String url) throws Exception {
		HttpGet httpget = new HttpGet(url);
		response = httpclient.execute(httpget);
		HttpEntity httpEntity = response.getEntity();
		String html = null;
		if (httpEntity != null) {
			html = EntityUtils.toString(httpEntity, HTTP.UTF_8);
			EntityUtils.consumeQuietly(httpEntity);
		}
		httpget.releaseConnection();
		close();
		return html;
	}

	@SuppressWarnings("deprecation")
	public String post(String url, List<NameValuePair> nvps, String encode) throws Exception {
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(new UrlEncodedFormEntity(nvps, encode));
		response = httpclient.execute(httppost);
		HttpEntity httpEntity = response.getEntity();
		String html = null;
		if (httpEntity != null) {
			html = EntityUtils.toString(httpEntity, encode);
			httpEntity.consumeContent();
		}
		httppost.releaseConnection();
		close();
		return html;
	}

	@SuppressWarnings("deprecation")
	public String post(String url, List<NameValuePair> nvps) throws Exception {
		return this.post(url, nvps, HTTP.UTF_8);
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public String post(String url, Map map) throws IOException {
		List<NameValuePair> nvps = new ArrayList();
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			NameValuePair nvp = new BasicNameValuePair(key, value);
			nvps.add(nvp);
		}
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		response = httpclient.execute(httppost);
		HttpEntity httpEntity = response.getEntity();
		String html = null;
		if (httpEntity != null) {
			html = EntityUtils.toString(httpEntity);
			httpEntity.consumeContent();
		}
		httppost.releaseConnection();
		close();
		return html;
	}

	@SuppressWarnings("deprecation")
	public String post(String url, String str) throws Exception {
		httpclient.getConnectionManager().closeIdleConnections(30, TimeUnit.SECONDS);
		HttpPost httppost = new HttpPost(url);// "http://web-proxy.qq.com/conn_s"
		StringEntity reqEntity = new StringEntity(str);
		httppost.setEntity(reqEntity);
		response = httpclient.execute(httppost);
		HttpEntity httpEntity = response.getEntity();
		String html = null;
		if (httpEntity != null) {
			html = EntityUtils.toString(httpEntity);
			httpEntity.consumeContent();
		}
		httppost.releaseConnection();
		close();
		return html;
	}

	@SuppressWarnings("deprecation")
	public void getImg(String url, String path) throws IOException {
		HttpGet httpget = new HttpGet(url);
		response = httpclient.execute(httpget);
		HttpEntity httpEntity = response.getEntity();
		byte[] b = EntityUtils.toByteArray(httpEntity);
		File storeFile = new File(path);
		FileOutputStream output = new FileOutputStream(storeFile);
		output.write(b);
		output.close();
		if (httpEntity != null) {
			httpEntity.consumeContent();
		}
		httpget.releaseConnection();
		close();
	}

	public void close() throws IOException {
		httpclient.getConnectionManager().shutdown();
	}

	@SuppressWarnings("deprecation")
	public String postQSHP(String url, List<NameValuePair> nvps) throws Exception {
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		response = httpclient.execute(httppost);
		if (response.containsHeader("Location")) {
			Header locationHeader = response.getFirstHeader("Location");
			logger.info("Location" + locationHeader.getValue());
			return this.get("http://uc.stuhome.net/" + locationHeader.getValue());
		} else {
			HttpEntity httpEntity = response.getEntity();
			String html = null;
			if (httpEntity != null) {
				html = EntityUtils.toString(httpEntity, HTTP.UTF_8);
				httpEntity.consumeContent();
			}
			close();
			return html;
		}
	}

	@SuppressWarnings("deprecation")
	public String postWeibo(String url, List<NameValuePair> nvps) throws Exception {
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		response = httpclient.execute(httppost);
		if (response.containsHeader("Location")) {
			Header locationHeader = response.getFirstHeader("Location");
			logger.info("Location" + locationHeader.getValue());
			// http://newlogin.sina.cn/crossDomain/?g=4uF7CpOz1BP7zANozP5Ra5dLxfG&t=1362741224&m=a5f7&r=&u=http%3A%2F%2Fweibo.cn%2F%3Fs2w%3Dlogin%26gsid%3D4uF7CpOz1BP7zANozP5Ra5dLxfG%26vt%3D4&cross=1&vt=4
			return this.get(locationHeader.getValue());
		} else {
			HttpEntity httpEntity = response.getEntity();
			String html = null;
			if (httpEntity != null) {
				html = EntityUtils.toString(httpEntity, HTTP.UTF_8);
				httpEntity.consumeContent();
			}
			close();
			return html;
		}
	}

	/**
	 * 根据页面body获取字符编码
	 * 
	 * @param html
	 * @param charset
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public String _getCharSetByBody(String html) {
		String charset = HTTP.UTF_8;

		Document document = null;
		if (html.startsWith("<?xml")) {
			Node node = Jsoup.parse(html, "", new Parser(new XmlTreeBuilder())).childNode(0);
			String comment = node.attr("comment").toLowerCase();
			if (comment.contains("gb2312")) {
				charset = "gb2312";
			} else if (comment.contains("utf-8")) {
				charset = "utf-8";
			}
		} else {
			document = Jsoup.parse(html);
			Elements elements = document.select("meta");
			for (Element metaElement : elements) {
				if (metaElement != null && StringUtils.isNotBlank(metaElement.attr("http-equiv"))
						&& metaElement.attr("http-equiv").toLowerCase().equals("content-type")) {
					String content = metaElement.attr("content");
					charset = _getCharSet(content);
					break;
				}
			}
		}
		return charset;
	}

	/**
	 * 正则获取字符编码
	 * 
	 * @param content
	 * @return
	 */
	private String _getCharSet(String content) {
		String regex = ".*charset=([^;]*).*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}
}
