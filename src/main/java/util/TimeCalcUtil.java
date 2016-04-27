package util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeCalcUtil {

	private static final Logger logger = LoggerFactory.getLogger(TimeCalcUtil.class);
	
	private static ThreadLocal<Long> startTime = new ThreadLocal<Long>();
	
	private static ThreadLocal<String> url = new ThreadLocal<String>();
	
	private static ThreadLocal<String> reqType = new ThreadLocal<String>();
	
	public static void setReqType(String reqType) {
		TimeCalcUtil.reqType.set(reqType);
	}
	
	public static String getReqType() {
		return reqType.get();
	}
	
	public static void setStartTimeUrl(long startTime, String url) {
		if (StringUtils.isEmpty(url)) {
			logger.error("url is empty or null!");
			return;
		}
		TimeCalcUtil.startTime.set(startTime);
		TimeCalcUtil.url.set(url);
		logger.info("Request: ");
		logger.info("{} {}", getReqType(), url);
	}
	
	public static void logRunTime() {
		Long start = startTime.get();
		if (null == start || 0 == start) {
			return;
		}
		String thisUrl = url.get();
		if (StringUtils.isEmpty(thisUrl)) {
			logger.error("url is empty or null!");
			return;
		}
		logger.info("url {} runs {}s...", thisUrl, (double)((System.currentTimeMillis() - start) / 1000.000));
	}
	
}
