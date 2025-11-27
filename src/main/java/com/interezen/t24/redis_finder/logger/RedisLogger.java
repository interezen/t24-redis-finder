package com.interezen.t24.redis_finder.logger;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;


public class RedisLogger {

	private volatile static RedisLogger _instance = null;
	private Logger logger = (Logger) LoggerFactory.getLogger(RedisLogger.class);

	private RedisLogger() {
	}

	public static RedisLogger getInstance() {
		if( _instance == null ) {
			/* 제일 처음에만 동기화 하도록 함 */
			synchronized(RedisLogger.class) {
				if( _instance == null ) {
					_instance = new RedisLogger();
				}
			}
		}
		return _instance;
	}

	public Logger getLogger() {
		return logger;
	}

	public static void main(String[] args) {
		RedisLogger.getInstance().getLogger();
	}
} 
