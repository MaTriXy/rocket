package fast.rocket.builder;

import java.io.File;
import java.util.Map;

import android.text.TextUtils;

import fast.rocket.Rocket;
import fast.rocket.cache.CachePolicy;
import fast.rocket.error.RocketError;
import fast.rocket.request.JsonRequest;
import fast.rocket.request.Request.Method;
import fast.rocket.response.JsonCallback;
import fast.rocket.response.Response.ErrorListener;
import fast.rocket.response.Response.Listener;

/**
 * The Class RocketJsonBuilder.
 */
public class RocketJsonBuilder implements JsonBuilder<RocketJsonBuilder>, 
	CacheBuilder<RocketJsonBuilder>, LoadBuilder<RocketJsonBuilder>{
	
	/** The future callback to be invoked after
	 *  the json string being parsed. 
	 **/
	@SuppressWarnings("rawtypes")
	private JsonCallback callback ;
	
	/** The class type to be parsed. */
	private Class<?> clazz;
	
	/** Http post or put params. */
	private Map<String, String> params;
	
	/** Http headers. */
	private Map<String, String> headers;
	
	/** The url. */
	private String url;
	
	/** The method. */
	private int method;
	
	/** The rocket instance. */
	private Rocket rocket;
	
	/** The request tag. */
	private Object tag;

    /** The enable cookie tag. */
    private boolean cookieEnable;

    /** The cache policy. */
    private CachePolicy cachePolicy;
    
    
	/**
	 * Instantiates a new rocket request builder.
	 *
	 * @param rocket the rocket
	 * @param clazz the clazz
	 */
	public RocketJsonBuilder(Rocket rocket, Class<?> clazz) {
		this.rocket = rocket;
		this.clazz = clazz;
	}
	
	/* (non-Javadoc)
	 * @see fast.rocket.builder.CacheBuilder#skipMemoryCache(boolean)
	 */
	@Override
	public RocketJsonBuilder skipMemoryCache(boolean skipMemoryCache) {
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.CacheBuilder#skipDiskCache(boolean)
	 */
	@Override
	public RocketJsonBuilder skipDiskCache(boolean skipDiskCache) {
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.LoadBuilder#load(java.io.File)
	 */
	@Override
	public RocketJsonBuilder load(File file) {
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.LoadBuilder#load(java.lang.String)
	 */
	@Override
	public RocketJsonBuilder load(String uri) {
		load(Method.POST, uri);
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.LoadBuilder#load(int, java.lang.String)
	 */
	@Override
	public RocketJsonBuilder load(int method, String url) {
		if(TextUtils.isEmpty(url)) {
			throw new IllegalArgumentException("Request url is null");
		}
		
		this.url = url;
		this.method = method;
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.JsonBuilder#invoke(fast.rocket.response.JsonCallback)
	 */
	@Override
	public RocketJsonBuilder invoke(JsonCallback<?> callback) {
		this.callback = callback;
		addRequest(method, url, clazz);
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.CacheBuilder#cachePolicy(fast.rocket.cache.CachePolicy)
	 */
	@Override
	public RocketJsonBuilder cachePolicy(CachePolicy cachePolicy) {
		this.cachePolicy = cachePolicy;
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.JsonBuilder#requestTag(java.lang.Object)
	 */
	@Override
	public RocketJsonBuilder requestTag(Object tag) {
		this.tag = tag;
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.JsonBuilder#enableCookie(boolean)
	 */
	@Override
	public RocketJsonBuilder enableCookie(boolean enable) {
		this.cookieEnable = enable;
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.JsonBuilder#requestParams(java.util.Map)
	 */
	@Override
	public RocketJsonBuilder requestParams(Map<String, String> params) {
		this.params = params;
		return this;
	}

	/* (non-Javadoc)
	 * @see fast.rocket.builder.JsonBuilder#requestHeaders(java.util.Map)
	 */
	@Override
	public RocketJsonBuilder requestHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	/**
	 * Adds the json request.
	 *
	 * @param <T> the generic type
	 * @param method the method
	 * @param uri the uri
	 * @param clazz the clazz
	 */
	private <T> void addRequest(int method, String uri, Class<T> clazz) {
		if(clazz == null || callback == null) {
			throw new IllegalArgumentException("Initialization params is null");
		}
		
		if(params != null && method == Method.GET) {
			method = Method.POST;//reset the http method
		}
		
		JsonRequest<T> request = new JsonRequest<T>(method, uri, clazz,
				headers, params, new Listener<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public void onResponse(T response) {
				if(callback != null) {
					callback.onCompleted(null, response);
				}
			}
		}, new ErrorListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onErrorResponse(RocketError error) {
				if(callback != null) {
					callback.onCompleted(error, null);
				}
			}
		});
		
		if(tag != null) request.setTag(tag);
        request.setCookieEnable(cookieEnable);
        request.setCacheStrategy(cachePolicy);
		rocket.getRequestQueue().add(request);
	}
	
}
