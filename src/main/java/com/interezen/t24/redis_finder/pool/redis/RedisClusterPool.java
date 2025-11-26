package com.interezen.t24.redis_finder.pool.redis;

import ch.qos.logback.classic.Logger;
import com.interezen.t24.redis_finder.cfg.StaticProperties;
import com.interezen.t24.redis_finder.logger.RedisLogger;
import com.interezen.t24.redis_finder.pool.redis.object.AbstractRedisPoolObject;
import com.interezen.t24.redis_finder.pool.redis.object.RedisConnectionObject;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedisClusterPool extends AbstractRedisPoolObject {
	private volatile static RedisClusterPool _instance = null;
	private final static String instanceName = "RedisClusterPool";
	private Logger logger = RedisLogger.getInstance().getLogger();

	public static RedisClusterPool getInstance() {
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
			synchronized(RedisClusterPool.class) {
				if( _instance == null ) {
					try {
						_instance = new RedisClusterPool();
					} catch(Exception e) {
						return null;
					}
				}
			}
		}
		return _instance;
	}

	private RedisClusterPool() throws Exception {
		super(instanceName);
		initClusterRedis();
	}

	private void initClusterRedis() throws Exception {
		int cores = Runtime.getRuntime().availableProcessors() / 2;
		cores = Math.max(2, cores);
		
		int redisTimeout = 0, redisConnectionCount = 0;
		long evictionThreadTerm = 0L, evictionIdleTime = 0L;
		Set<HostAndPort> nodes = null;
		String[] hostAndPort = null;
		List<Object> hostAndPortList = null;
		JedisCluster jedisCluster = null;
		RedisConnectionObject redisConnectionObject = null;
		JedisPoolConfig jedisPoolConfig = null;

		nodes = new HashSet<HostAndPort>();

		hostAndPortList = StaticProperties.getInstance().getList("redis.cluster.hosts");
		evictionThreadTerm = Long.parseLong(StaticProperties.getInstance().getString("redis.cluster.connection.eviction.thread.term", "3000"));
		evictionIdleTime = Long.parseLong(StaticProperties.getInstance().getString("redis.cluster.connection.thread.idle.eviction.time", "1000"));
		redisTimeout = StaticProperties.getInstance().getInt("redis.cluster.connection.timeout", 3000);
		redisConnectionCount = StaticProperties.getInstance().getInt("redis.cluster.connection.count", 1);

		if(redisConnectionCount == 0) {
			redisConnectionCount = cores;
		}
		
		if(hostAndPortList == null || hostAndPortList.isEmpty()) {
			throw new Exception("Invalid host list");
		}

		if (hostAndPortList.size() > 1) {
			for (Object hostPort : hostAndPortList) {
				hostAndPort = ((String) hostPort).split(":");
				nodes.add(new HostAndPort(hostAndPort[0], Integer.parseInt(hostAndPort[1])));
			}
		} else {
			hostAndPort = ((String) hostAndPortList.get(0)).split(":");
			nodes.add(new HostAndPort(hostAndPort[0], Integer.parseInt(hostAndPort[1])));
		}

		jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxTotal(redisConnectionCount);
		jedisPoolConfig.setMaxIdle(redisConnectionCount);
		jedisPoolConfig.setMinIdle(redisConnectionCount / 2);
		jedisPoolConfig.setTestOnReturn(false);
		jedisPoolConfig.setTestOnBorrow(false);
		jedisPoolConfig.setTestOnCreate(false);
		jedisPoolConfig.setBlockWhenExhausted(true);
		jedisPoolConfig.setLifo(true);
		jedisPoolConfig.setMaxWaitMillis(redisTimeout);
		// 유효하지 않은 connection 제거 thread 동작 term
		jedisPoolConfig.setTimeBetweenEvictionRunsMillis(evictionThreadTerm);
		// 유효하지 않은 connection 판단 시간
		jedisPoolConfig.setMinEvictableIdleTimeMillis(evictionIdleTime);

		// jedisCluster는 Cluster instance에서 timeout을 설정한다.
		jedisCluster = new JedisCluster(nodes, redisTimeout, jedisPoolConfig);

		redisConnectionObject = new RedisConnectionObject();
		redisConnectionObject.setNodes(nodes);
		redisConnectionObject.setJedisCluster(jedisCluster);
		redisConnectionObject.setClustered(true);
		redisConnectionObject.setPoolConfig(jedisPoolConfig);

		init(redisConnectionObject);
	}

	public static void cleaningUp() {
		_instance = null;
	}
}