package com.interezen.t24.redis_finder.pool.redis;

import ch.qos.logback.classic.Logger;
import com.interezen.t24.redis_finder.cfg.StaticProperties;
import com.interezen.t24.redis_finder.logger.RedisLogger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisPool {
	private volatile static RedisPool _instance = null;
    private String objectName = "RedisPool";
    private JedisPool pool = null;
	private Logger logger = RedisLogger.getInstance().getLogger();

	public static RedisPool getInstance() {
		/**
		 * 변경이력
		 * -----------------------------------------------------------------
		 *   변경일        작성자       변경내용
		 * ------------ ------------ ---------------------------------------
		 *
		 * -----------------------------------------------------------------
		 */
		if( _instance == null ) {
			/* 제일 처음에만 동기화 하도록 함 */
			synchronized(RedisPool.class) {
				if( _instance == null ) {
					_instance = new RedisPool();
				}
			}
		}
		return _instance;
	}

	private RedisPool() {
		initRedis();
	}

	private void initRedis() {

		String redisHost = StaticProperties.getInstance().getString("redis.host", "127.0.0.1");
		int redisPort  = StaticProperties.getInstance().getInt("redis.port", 17000);

		if(redisPort == 0) {
			logger.error("{}, Redis Port Required -> StaticProperties[redis.port]", objectName);
			return;
		}

		int redisTimeout = StaticProperties.getInstance().getInt("redis.connection.timeout", 3000);
		int redisConnectionCount = StaticProperties.getInstance().getInt("redis.connection.count", 16);
		String redisPassword = StaticProperties.getInstance().getString("redis.password", "inter501");

		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxTotal(redisConnectionCount);
		jedisPoolConfig.setMinIdle(redisConnectionCount/2);
		jedisPoolConfig.setMaxIdle(redisConnectionCount);
		pool = new JedisPool(jedisPoolConfig, redisHost, redisPort, redisTimeout, redisPassword);

		logger.info("");
		logger.info("{}, ================ [RedisPool Information] ================", objectName);
		logger.info("{}, 0. Redis Type : Single", objectName);
		logger.info("{}, 1. Redis Host : {}", objectName, redisHost);
		logger.info("{}, 2. Redis Port : {}", objectName, redisPort);
		logger.info("{}, 3. Redis Timeout : {}", objectName, redisTimeout);
		logger.info("{}, 4. Redis Password : {}", objectName, redisPassword);
		logger.info("{}, 5. Redis ConnectionCount : {}", objectName, redisConnectionCount);
		logger.info("{}, ============================================================", objectName);
	}

	public Jedis getResource() {
		return pool != null ? pool.getResource() : null ;
	}

	public int getActiveCount() {
		return pool != null ? pool.getNumActive() : 0;
	}
}
