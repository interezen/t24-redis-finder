package com.interezen.t24.redis_finder.pool.redis;

import ch.qos.logback.classic.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.interezen.t24.redis_finder.cfg.StaticProperties;
import com.interezen.t24.redis_finder.define.ProcessDefine;
import com.interezen.t24.redis_finder.logger.RedisLogger;
import org.apache.commons.lang3.StringUtils;
import org.ragdoll.express.configuration.ExpressConfigurationUtils;
import org.ragdoll.express.pool.lettuce.instance.ExpressLettucePool;
import org.ragdoll.express.pool.lettuce.object.ExpressLettucePoolObject;
import org.ragdoll.express.utils.ExpressExceptionUtils;
import org.ragdoll.express.utils.ExpressJsonUtils;

public class RedisPool {
	private volatile static RedisPool _instance = null;
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
		// lettuce init 하게
		try {
			String poolPath = StaticProperties.getInstance().getString("pool.configuration.path", "config/pool.yml");
			JsonObject root = null;
			try {
				root = ExpressConfigurationUtils.loadYamlToJsonElement(poolPath).getAsJsonObject();
			} catch (Exception e) {
				logger.error("{} => {}", this.getClass().getName(), ExpressExceptionUtils.getStackTrace(e));
			}
			if (root == null) {
				logger.error(" * pool resource element is empty");
				System.exit(1);
			} else {
				JsonArray redisRoot = root.getAsJsonArray("redis");
				if (redisRoot == null) {
					logger.error("redis configuration is empty.");
					System.exit(1);
				}
				boolean completed = false;
				int size = redisRoot.size();
				JsonObject row;
				for (int i = 0; i < size; i++) {
					row = redisRoot.get(i).getAsJsonObject();
					if (!row.has("id")) {
						continue;
					}
					String id = row.get("id").getAsString();
					if (StringUtils.isEmpty(id)) {
						continue;
					}
					if (!ProcessDefine.REDIS_ID.equals(id)) {
						continue;
					}
					if (ExpressLettucePool.getInstance().containsKey(id)) {
						continue;
					}
					boolean use = row.has("use") && row.get("use").getAsBoolean();
					if (!use) {
						continue;
					}
					int workers = ExpressJsonUtils.getAsInt(row, "workers", 0);
					if(workers <= 0) {
						workers = Math.max(1, Runtime.getRuntime().availableProcessors() * 3);
						row.addProperty("workers", workers);
					}
					try {
						ExpressLettucePoolObject lettucePoolObject = new ExpressLettucePoolObject(row);
						ExpressLettucePool.getInstance().put(id, lettucePoolObject);
						logger.info("lettuce info\n" + lettucePoolObject);
					} catch (Exception e) {
						logger.error(" * pool configuration redis init exception : {}", ExpressExceptionUtils.getStackTrace(e));
					}
				}
			}
		} catch (Exception e) {
			logger.error(" * pool configuration exception : {}", ExpressExceptionUtils.getStackTrace(e));
			System.exit(1);
		}
	}
}
