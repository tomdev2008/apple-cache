package com.appleframework.cache.ehcache.spring;

import java.io.Serializable;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appleframework.cache.core.CacheObjectImpl;
import com.appleframework.cache.core.config.SpringCacheConfig;
import com.appleframework.cache.core.spring.BaseCacheOperation;
import com.appleframework.cache.ehcache.EhCacheExpiry;
import com.appleframework.cache.ehcache.factory.ConfigurationFactoryBean;

public class SpringCacheOperation implements BaseCacheOperation {

	private static Logger logger = LoggerFactory.getLogger(SpringCacheOperation.class);

	private String name;
	private int expire = 0;

	private Cache<String, Serializable> cache;

	private Cache<String, Serializable> getEhCache() {
		return cache;
	}

	private void init(CacheManager ehcacheManager) {
		SpringCacheExpiry expiry = null;
		if (expire > 0 && !SpringCacheConfig.isCacheObject()) {
			expiry = new SpringCacheExpiry(expire);
		} else {
			expiry = new SpringCacheExpiry();
		}
		CacheConfigurationBuilder<String, Serializable> configuration = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(String.class, Serializable.class,
						ResourcePoolsBuilder.newResourcePoolsBuilder()
								.heap(ConfigurationFactoryBean.getHeap(), MemoryUnit.MB)
								.offheap(ConfigurationFactoryBean.getOffheap(), MemoryUnit.MB))
				.withExpiry(expiry);
		cache = ehcacheManager.getCache(name, String.class, Serializable.class);
		if (null == cache) {
			try {
				cache = ehcacheManager.createCache(name, configuration);
			} catch (IllegalArgumentException e) {
				logger.warn("the cache name " + name + " is exist !");
				cache = ehcacheManager.getCache(name, String.class, Serializable.class);
			}
		}
	}

	public SpringCacheOperation(CacheManager ehcacheManager, String name) {
		this.name = name;
		this.init(ehcacheManager);
	}

	public SpringCacheOperation(CacheManager ehcacheManager, String name, int expire) {
		this.name = name;
		this.expire = expire;
		this.init(ehcacheManager);
	}

	public Object get(String key) {
		Object value = null;
		try {
			Object element = getEhCache().get(key);
			if (null != element) {
				if (SpringCacheConfig.isCacheObject()) {
					CacheObjectImpl cache = (CacheObjectImpl) element;
					if (null != cache) {
						if (cache.isExpired()) {
							this.resetCacheObject(key, cache);
						} else {
							value = cache.getObject();
						}
					}
				} else {
					value = element;
				}
			}
		} catch (Exception e) {
			logger.warn("cache error", e);
		}
		return value;
	}

	private void resetCacheObject(String key, CacheObjectImpl cache) {
		try {
			cache.setExpiredSecond(expire);
			getEhCache().put(key, cache);
		} catch (Exception e) {
			logger.warn("cache error", e);
		}
	}

	public void put(String key, Object value) {
		if (value == null)
			this.delete(key);
		try {
			if (SpringCacheConfig.isCacheObject()) {
				CacheObjectImpl object = CacheObjectImpl.create(value, expire);
				getEhCache().put(key, object);
			} else {
				if (expire > 0) {
					EhCacheExpiry.setExpiry(key, expire);
				}
				getEhCache().put(key, (Serializable) value);
			}

		} catch (Exception e) {
			logger.warn("cache error", e);
		}
	}

	public void clear() {
		try {
			getEhCache().clear();
		} catch (Exception e) {
			logger.warn("cache error", e);
		}
	}

	public void delete(String key) {
		try {
			getEhCache().remove(key);
		} catch (Exception e) {
			logger.warn("cache error", e);
		}
	}

	public int getExpire() {
		return expire;
	}

}
