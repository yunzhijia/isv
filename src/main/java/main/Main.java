package main;

import java.util.HashMap;
import java.util.Map;

import util.AuthorizationUtil;
import util.HttpHelperAsync;
import util.HttpHelperAsync.Headers;
import util.HttpHelperAsync.Response;

public class Main {

	public static Response get(String consumerKey, String consumerSecret, 
			String signatureMethod, long timestamp, 
			String nonce, float version, 
			String oauthToken, String oauthTokenSecret, 
			String verifier, String url, Headers headers, Map<String, Object> parameters, long timeoutMillis) throws Exception {
		headers = headers(consumerKey, consumerSecret, signatureMethod,
				timestamp, nonce, version, oauthToken, oauthTokenSecret,
				verifier, url, headers, parameters, timeoutMillis, "GET");
		return HttpHelperAsync.get(url, headers, parameters, timeoutMillis);
	}
	
	public static Response post(String consumerKey, String consumerSecret, 
			String signatureMethod, long timestamp, 
			String nonce, float version, 
			String oauthToken, String oauthTokenSecret, 
			String verifier, String url, Headers headers, Map<String, Object> parameters, long timeoutMillis) throws Exception {
		headers = headers(consumerKey, consumerSecret, signatureMethod,
				timestamp, nonce, version, oauthToken, oauthTokenSecret,
				verifier, url, headers, parameters, timeoutMillis, "POST");
		return HttpHelperAsync.post(url, headers, parameters, timeoutMillis);
	}
	
	private static Headers headers(String consumerKey, String consumerSecret, 
			String signatureMethod, long timestamp, 
			String nonce, float version, 
			String oauthToken, String oauthTokenSecret, 
			String verifier, String url, Headers headers, Map<String, Object> parameters, long timeoutMillis, String reqType) throws Exception {
		if (null != headers) {
			Object contentType = headers.get("Content-Type");
			if (null != contentType && HttpHelperAsync.APPLICATION_JSON.equals(contentType.toString())) {
				headers.put("Authorization", AuthorizationUtil.generateAuthorizationHeader(consumerKey, consumerSecret, 
						signatureMethod, timestamp, nonce, version, 
						oauthToken, oauthTokenSecret, verifier, url, null, reqType));
				return headers;
			}
		} else {
			headers = new Headers();
		}
		headers.put("Authorization", AuthorizationUtil.generateAuthorizationHeader(consumerKey, consumerSecret, 
				signatureMethod, timestamp, nonce, version, 
				oauthToken, oauthTokenSecret, verifier, url, parameters, reqType));
		return headers;
	}
	
	/**
	 * 企业所有组织人员[需要管理员授权]
	 * @param oauth_consumer_key  appid
	 * @param oauth_consumer_secret appSecret
	 * @param oauth_signature_method
	 * @param oauth_timestamp
	 * @param oauth_nonce
	 * @param oauth_version
	 * @param oauth_token
	 * @param oauth_token_secret
	 * @param oauth_verifier
	 * @throws Exception
	 */
	public static void getallpersons(String oauth_consumer_key, String oauth_consumer_secret, 
			long oauth_timestamp,String oauth_nonce, float oauth_version) throws Exception {
		String url = "https://www.yunzhijia.com/openapi/third/v1/opendata-control/data/getallpersons";
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("eid", "4027242");
		parameters.put("time", "2008-01-01 00:00:00");
		parameters.put("begin", "0");//默认0
		parameters.put("count", "1000");//默认1000
		post(oauth_consumer_key, oauth_consumer_secret, 
				null, oauth_timestamp, 
				oauth_nonce, oauth_version, null, null, null, url, null, parameters, 0);
	}
	
	/**
	 * 个人信息[只需要个人授权，返回字段由申请权限决定]
	 * @param oauth_consumer_key  appid
	 * @param oauth_consumer_secret appSecret001abc002
	 * @param oauth_signature_method
	 * @param oauth_timestamp
	 * @param oauth_nonce
	 * @param oauth_version
	 * @param oauth_token
	 * @param oauth_token_secret
	 * @param oauth_verifier
	 * @throws Exception
	 */
	public static void getperson(String oauth_consumer_key, String oauth_consumer_secret, 
			long oauth_timestamp,String oauth_nonce, float oauth_version) throws Exception {
		String url = "https://www.yunzhijia.com/openapi/third/v1/opendata-control/data/getperson";
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("eid", "4027242");
		parameters.put("openId", "5710ebdbe4b0cf1802e897ae");
		post(oauth_consumer_key, oauth_consumer_secret, 
				null, oauth_timestamp, 
				oauth_nonce, oauth_version, null, null, null, url, null, parameters, 0);
	}
	
	/**
	 * 获取当前部门基本信息，部门负责人[需要管理员授权]
	 * @param oauth_consumer_key  appid
	 * @param oauth_consumer_secret appSecret001abc002
	 * @param oauth_signature_method
	 * @param oauth_timestamp
	 * @param oauth_nonce
	 * @param oauth_version
	 * @param oauth_token
	 * @param oauth_token_secret
	 * @param oauth_verifier
	 * @throws Exception
	 */
	public static void getorg(String oauth_consumer_key, String oauth_consumer_secret, 
			long oauth_timestamp, String oauth_nonce, float oauth_version) throws Exception {
		String url = "https://www.yunzhijia.com/openapi/third/v1/opendata-control/data/getorg";
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("eid", "4027242");
		parameters.put("orgId", "0f5b48a7-eb3b-11e5-9fc9-ecf4bbea1498");
		post(oauth_consumer_key, oauth_consumer_secret, 
				null, oauth_timestamp, 
				oauth_nonce, oauth_version, null, null, null, url, null, parameters, 0);
	}
	
	public static void main(String[] args) throws Exception{
		String oauth_consumer_key = "10194";//appid,应用id
		String oauth_consumer_secret = "appsecret001a002b003c";//appsecret,长度应该足够复杂以保证安全性
		long oauth_timestamp = System.currentTimeMillis()/1000;
		String oauth_nonce = String.valueOf(oauth_timestamp + AuthorizationUtil.RAND.nextInt());
		float oauth_version = 1.0f;
		
		//企业所有组织人员[需要管理员授权]
		getallpersons(oauth_consumer_key, oauth_consumer_secret, 
				oauth_timestamp,oauth_nonce, oauth_version);
		
//		//个人信息[只需要个人授权，返回字段由申请权限决定]
//		getperson(oauth_consumer_key, oauth_consumer_secret, 
//				oauth_timestamp,oauth_nonce, oauth_version);
		
//		//获取当前部门基本信息，部门负责人[需要管理员授权]
//		getorg(oauth_consumer_key, oauth_consumer_secret, 
//				oauth_timestamp,oauth_nonce, oauth_version);
		
		System.exit(0);
	}
	
}
