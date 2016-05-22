package util;

import java.io.IOException;
import java.io.Serializable;
import java.net.BindException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class HttpHelperAsync {
	
	private static Logger logger = LoggerFactory.getLogger(HttpHelperAsync.class);
	
	private static final int DEFAULT_ASYNC_TIME_OUT = 10000;
	private static final int MAX_TOTEL = 100;
    private static final int MAX_CONNECTION_PER_ROUTE = 100;
    public static final String UTF8 = "UTF-8";
	public static final String APPLICATION_JSON = "application/json";
	public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
	
	private static Get get = new Get();
	private static Post post = new Post();
	private static PostJSON postJSON = new PostJSON();
	
	private static class HttpHelperAsyncClientHolder {
		private static HttpHelperAsyncClient instance = new HttpHelperAsyncClient();
	}
	
	private static class HttpHelperAsyncClient {
		private CloseableHttpAsyncClient httpClient; 
		private PoolingNHttpClientConnectionManager cm;
		private HttpHelperAsyncClient() {}
		private DefaultConnectingIOReactor ioReactor;
		private static HttpHelperAsyncClient instance;
		private Logger logger = LoggerFactory.getLogger(HttpHelperAsyncClient.class);
		
		public static HttpHelperAsyncClient getInstance() {
			instance = HttpHelperAsyncClientHolder.instance;
			try {
				instance.init();
			} catch (Exception e) {
			}
			return instance;
		}
		
		private void init() throws Exception {
			ioReactor = new DefaultConnectingIOReactor();
			ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {
				public boolean handle(IOException ex) {
			        if (ex instanceof BindException) {
			            return true;
			        }
			        return false;
			    }
				public boolean handle(RuntimeException ex) {
			        if (ex instanceof UnsupportedOperationException) {
			            return true;
			        }
			        return false;
			    }
			});
			
			cm=new PoolingNHttpClientConnectionManager(ioReactor);
			cm.setMaxTotal(MAX_TOTEL);
			cm.setDefaultMaxPerRoute(MAX_CONNECTION_PER_ROUTE);			
			httpClient = HttpAsyncClients.custom()
							.addInterceptorFirst(new HttpRequestInterceptor() {
								
	                    public void process(
	                            final HttpRequest request,
	                            final HttpContext context) throws HttpException, IOException {
	                        if (!request.containsHeader("Accept-Encoding")) {
	                            request.addHeader("Accept-Encoding", "gzip");
	                        }
	                    }}).addInterceptorFirst(new HttpResponseInterceptor() {

	                    public void process(
	                            final HttpResponse response,
	                            final HttpContext context) throws HttpException, IOException {
	                    	
	                        HttpEntity entity = response.getEntity();
	                        if (entity != null) {
	                            Header ceheader = entity.getContentEncoding();
	                            if (ceheader != null) {
	                                HeaderElement[] codecs = ceheader.getElements();
	                                for (int i = 0; i < codecs.length; i++) {
	                                    if (codecs[i].getName().equalsIgnoreCase("gzip")) {
	                                        response.setEntity(
	                                                new GzipDecompressingEntity(response.getEntity()));
	                                        return;
	                                    }
	                                }
	                            }
	                        }
	                    }
	                })
	                .setConnectionManager(cm)
	                .build();
			httpClient.start();
	    }
		
		private Response execute(HttpUriRequest request, long timeoutmillis) throws Exception {
	        HttpEntity entity = null;
	        Future<HttpResponse> rsp = null;
	        Response respObject=new Response();
	        //default error code
	        respObject.setCode(400);
	        if (request == null) 
	        	return respObject;
	        try{
	        	if(httpClient == null){
	        		StringBuilder sbuilder=new StringBuilder();
	            	sbuilder.append("\n{").append(request.getURI().toString()).append("}\nreturn error "
	            			+ "{HttpHelperAsync.httpClient 获取异常！}");
	            	logger.info(sbuilder.toString());
	            	respObject.setError(sbuilder.toString());
	        		return respObject;        		
	        	}
	        	rsp = httpClient.execute(request, new FutureCallback<HttpResponse>() {
					public void completed(final HttpResponse response) {
                    	logger.info("completed successful!");
                    }
                    public void failed(final Exception ex) {
                    	logger.info("excute failed:",ex);
                    }
                    public void cancelled() {
                    	logger.info("excute canclled!");
                    }

                });
	        	HttpResponse resp = null;
	        	if(timeoutmillis > 0){
	        		resp = rsp.get(timeoutmillis,TimeUnit.MILLISECONDS);
	        	}else{
	        		resp = rsp.get(DEFAULT_ASYNC_TIME_OUT,TimeUnit.MILLISECONDS);
	        	}
	            entity = resp.getEntity();
	            StatusLine statusLine = resp.getStatusLine();
	            respObject.setCode(statusLine.getStatusCode());
	            logger.info("Response:");
	            logger.info(statusLine.toString());
	            headerLog(resp);
	            String result = new String();
	            if (respObject.getCode() == 200) {
	                String encoding = ("" + resp.getFirstHeader("Content-Encoding")).toLowerCase();
	                if (encoding.indexOf("gzip") > 0) {
	                    entity = new GzipDecompressingEntity(entity);
	                }
	                result = new String(EntityUtils.toByteArray(entity),UTF8);
	                respObject.setContent(result);
	            } else {
	            	StringBuilder sbuilder=new StringBuilder();
	            	sbuilder.append("\n{").append(request.getURI().toString()).append("}\nreturn error "
	            			+ "{").append(resp.getStatusLine().getStatusCode()).append("}");
	            	logger.info(sbuilder.toString());
	            	try {
	            		result = new String(EntityUtils.toByteArray(entity),UTF8);
	            		respObject.setError(result);
	            	} catch(Exception e) {
	            		logger.error(e.getMessage(), e);
	            		result = e.getMessage();
	            	}
	            }
	            TimeCalcUtil.logRunTime();
	            logger.info(result);

	        } finally {
	            EntityUtils.consumeQuietly(entity);
	        }
	        return respObject;
	    }
		
	}
	
	public static class Headers extends HashMap<String, Object> {
		private static final long serialVersionUID = -6699349634305847872L;

		public Headers() {
			this.put("Content-Type", APPLICATION_X_WWW_FORM_URLENCODED);
		}
	}
	
	public static class Response {
		//返回结果状态码
		private  int code;
		//返回内容
		private  String content;
		//返回错误
		private String error;
		
		public Response() {
			this.code = 400;
		}
		
		public String getError() {
			return error;
		}

		public void setError(String error) {
			this.error = error;
		}

		public  int getCode() {
			return code;
		}
		
		public  void setCode(int code) {
			this.code = code;
		}
		
		public  String getContent() {
			return content;
		}
		
		public  void setContent(String content) {
			this.content = content;
		}
		
		public JSONObject getJsonContent(){
			return JSONObject.parseObject(content);
		}
		
		public JSONArray getJsonArrayContent(){
			return JSONArray.parseArray(content);
		}
		
		public JSONObject getJsonError(){
			return JSONObject.parseObject(error);
		}
		
		public String toString(){
			return JSONObject.toJSON(this).toString();
		}
	}
	
	private static abstract class HttpAsyncRequest {
		// 钩子
		public Object setParameter(JSONObject parameters) {
			logger.info("{} Params: {}", TimeCalcUtil.getReqType(), parameters);
			return parameters;
		}
		
		// 钩子
		public Object setParameter(Map<String, Object> parameters) {
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			if (null == parameters || parameters.isEmpty()) {
				return params;
			}
			Iterator<String> iterator = parameters.keySet().iterator();
			StringBuffer paramsBuffer = new StringBuffer();
			for (; iterator.hasNext();) {
				String name = iterator.next();
				String value = parameters.get(name).toString();
				params.add(new BasicNameValuePair(name, value));
				if (iterator.hasNext()) {
					paramsBuffer.append(name).append("=").append(value).append("&");
				} else {
					paramsBuffer.append(name).append("=").append(value);
				}
			}
			logger.info("{} Params: {}", TimeCalcUtil.getReqType(), paramsBuffer);
			return params;
		}
		
		// 钩子
		@SuppressWarnings("unchecked")
		public HttpEntity setHttpEntity(Object params) throws Exception {
			List<NameValuePair> nameValuePairs = (List<NameValuePair>)params;
			UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs, UTF8);
			return urlEncodedFormEntity;
		}
		
		// 钩子
		public HttpUriRequest setHttpUriRequest(String url, HttpEntity httpEntity) throws Exception {
			HttpPost post = new HttpPost(url);
			post.setEntity(httpEntity);
			return post;
		}
		
		private void headers(HttpUriRequest request, Headers headers) {
			if (null != headers && !headers.isEmpty()) {
				Set<String> set = headers.keySet();
				for (String name : set) {
					String value = "" + headers.get(name);
					logger.info("{}: {}", name, value);
					request.addHeader(name, value);
				}
			}
		}
		
		public Response request(String url, Headers headers, Map<String, Object> parameters, long timeoutMillis) throws Exception {
			try {
				preOpera(url, timeoutMillis);
				UrlParam urlParam = parseUrlParam(url, parameters);
				Object contentTypeObject = (null == headers ? null : headers.get("Content-Type"));
				String contentType = (null == contentTypeObject ? APPLICATION_X_WWW_FORM_URLENCODED : contentTypeObject.toString());
				Response response = null;
				
				if (contentType.contains(APPLICATION_JSON)) {
					response = applicationJSON(url, headers, JSONObject.parseObject(JSONObject.toJSON(parameters).toString()), timeoutMillis);
				} else {
					Object params = setParameter(urlParam.parameters);
					params = params==null?new Object():params;
					HttpEntity httpEntity = setHttpEntity(params);
					HttpUriRequest request = setHttpUriRequest(urlParam.url, httpEntity);
					headers(request, headers);
					response = HttpHelperAsyncClient.getInstance().execute(request, timeoutMillis);
				}
				return response;
			} catch (AssertionError e) {
				throw new Exception(e);
			}
		}
		
		public Response request(String url, Headers headers, JSONObject parameters, long timeoutMillis) throws Exception {
			try {
				preOpera(url, timeoutMillis);
				return applicationJSON(url, headers, parameters, timeoutMillis);
			} catch (AssertionError e) {
				throw new Exception(e);
			}
		}
		
		private void preOpera(String url, long timeoutMillis) {
//			Assert.assertFalse("url is null or empty!", StringUtils.isEmpty(url));
			TimeCalcUtil.setStartTimeUrl(System.currentTimeMillis(), url); 
			if (0 == timeoutMillis) {
				timeoutMillis = DEFAULT_ASYNC_TIME_OUT;
			}
		}
		
		private UrlParam parseUrlParam(String url, Map<String, Object> params) {
//			Assert.assertFalse("url is null or empty!", StringUtils.isEmpty(url));
			int wenhao = url.indexOf("?");
			boolean hasUrlParam = -1 != wenhao && -1 != url.indexOf("=");
			
			if (null == params && hasUrlParam) {
				params = new HashMap<String, Object>();
			}
			
			if (hasUrlParam) {
				String srcUrl = url;
				url = url.substring(0, wenhao);
				String keyValues = srcUrl.substring(wenhao + 1);
				if (StringUtils.isNotEmpty(keyValues)) {
					String[] keyValueArray = keyValues.split("&");
					for (String keyValue : keyValueArray) {
						if (StringUtils.isNotEmpty(keyValue)) {
							String[] valuePair = keyValue.split("=");
							if (2 == valuePair.length) {
								String name = valuePair[0];
								String value = valuePair[1];
								if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
									params.put(name, value);
								}
							}
						}
					}
				}
			}
			return new UrlParam(url, params);
		}
		
		private Response applicationJSON(String url, Headers headers, JSONObject parameters, long timeoutMillis) throws Exception {
			Object params = setParameter(parameters);
			params = params==null?new Object():params;
			HttpEntity httpEntity = setHttpEntity(params);
			HttpUriRequest request = setHttpUriRequest(url, httpEntity);
			headers(request, headers);
			Response response = HttpHelperAsyncClient.getInstance().execute(request, timeoutMillis);
			return response;
		}
		
	}
	
	private static class UrlParam implements Serializable {
		
		private static final long serialVersionUID = -5041417788475125724L;
		private String url;
		private Map<String, Object> parameters;
		
		public UrlParam(String url, Map<String, Object> parameters) {
			this.url = url;
			this.parameters = parameters;
		}
		
	}

	private static class Get extends HttpAsyncRequest {
		@Override
		public HttpUriRequest setHttpUriRequest(String url, HttpEntity httpEntity) throws Exception {
			String param = EntityUtils.toString(httpEntity);
			url += (url.indexOf('?') != -1 ? "&" : "?") + param;
			HttpGet get = new HttpGet(url);
			return get;
		}
	}
	
	private static class Post extends HttpAsyncRequest {
	}

	private static class PostJSON extends HttpAsyncRequest {
		
		public HttpEntity setHttpEntity(Object params) throws Exception {
			JSONObject jsonParams = JSONObject.parseObject(params.toString());
			StringEntity stringEntity = new StringEntity(jsonParams.toString(), UTF8);
			stringEntity.setContentType(APPLICATION_JSON);
			return stringEntity;
		}
	}
	
	private final static void headerLog(HttpResponse response) {
		Header[] headers = response.getAllHeaders();
		headerLog(headers);
	}

	private final static void headerLog(Header[] headers) {
		for (Header header : headers) {
			String key = header.getName();
			String value = header.getValue();
			if(null != key){
				logger.info(key + ": " + value);
            }else{
            	logger.info(value);
            }
		}
	}
	
	public static Response get(String url, Headers headers, Map<String, Object> parameters, long timeoutMillis) throws Exception {
		TimeCalcUtil.setReqType("Get");
		return get.request(url, headers, parameters, timeoutMillis);
	}
	
	public static Response post(String url, Headers headers, Map<String, Object> parameters, long timeoutMillis) throws Exception {
		TimeCalcUtil.setReqType("Post");
		return post.request(url, headers, parameters, timeoutMillis);
	}
	
	public static Response postJSON(String url, Headers headers, JSONObject parameters, long timeoutMillis) throws Exception {
		TimeCalcUtil.setReqType("Post");
		return postJSON.request(url, headers, parameters, timeoutMillis);
	}
	
}
