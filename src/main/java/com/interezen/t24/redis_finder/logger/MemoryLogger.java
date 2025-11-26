package com.interezen.t24.redis_finder.logger;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;


public class MemoryLogger {

	private volatile static MemoryLogger _instance = null;
	private Logger logger = (Logger) LoggerFactory.getLogger(MemoryLogger.class);

	private MemoryLogger() {
	}

	public static MemoryLogger getInstance() {
		if( _instance == null ) {
			/* 제일 처음에만 동기화 하도록 함 */
			synchronized(MemoryLogger.class) {
				if( _instance == null ) {
					_instance = new MemoryLogger();
				}
			}
		}
		return _instance;
	}

	public Logger getLogger() {
		return logger;
	}

	public static void main(String[] args) {
		MemoryLogger.getInstance().getLogger();
	}
} 
