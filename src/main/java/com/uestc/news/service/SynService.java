/**
 * project name: myNews
 * created at 2013-3-8 - 下午8:15:14
 * author:yuer
 * email:yuerguang.cl@gmail.com
 */
package com.uestc.news.service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.uestc.news.entity.News;
import com.uestc.news.utils.Config;

@Component
public class SynService {
	private static Logger logger = LoggerFactory.getLogger(SynService.class);

	@Autowired
	private BBSLoginService bbsLoginService;

	@Autowired
	private WeiboService weiboService;

	@Autowired
	private NewsService newsService;

	/**
	 * @throws Exception
	 * @throws IOException
	 * 
	 */
	public void posting() {
		try {
			if (Config.getIsToQSH()) {// 设置不同步到清水河畔
				List<News> newsList = newsService.findAllByTime();
				if (newsList == null || newsList.size() < 1) {
					logger.info("[没有发现新的新闻：" + new Date() + "]");
				}
				bbsLoginService.posting(newsList.get(0));
			} else {
				logger.info("[没有发布到清水河畔]");
			}
			if (Config.getIsToWeibo()) {
				News news = newsService.getNewsMaxId();
				if (news != null) {
					weiboService.post(news);
				} else {
					logger.info("[没有发布到微博]");
				}
			} else {
				logger.info("[没有发布到微博]");
			}
		} catch (UnknownHostException e) {
			logger.error("出错了----UnknownHostException", e);
		} catch (SocketTimeoutException e) {
			logger.error("出错了----SocketTimeoutException", e);
		} catch (HttpHostConnectException e) {
			logger.error("出错了----HttpHostConnectException", e);
		} catch (IOException e) {
			logger.error("出错了----IOException", e);
		} catch (Exception e) {
			logger.error("出错了----Exception", e);
		}

	}
}
