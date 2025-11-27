package com.interezen.t24.redis_finder.monitor;

import ch.qos.logback.classic.Logger;
import com.interezen.t24.redis_finder.logger.MemoryLogger;

import java.text.NumberFormat;


public class SysMonitor {
	/** 정형화된 형식에 맞게 숫자를 표현하기 위한 변수 */
	private NumberFormat nf = null;
	// -- Instance	
	private volatile static SysMonitor _sysInstance = null;
	private Logger logger = MemoryLogger.getInstance().getLogger();
	private StringBuilder log = new StringBuilder();
	
	public static SysMonitor getInstance() {
		/**
		 * 변경이력
		 * -----------------------------------------------------------------
		 *   변경일        작성자       변경내용  
		 * ------------ ------------ ---------------------------------------
		 *   
		 * -----------------------------------------------------------------
		 */
		if( _sysInstance == null ) {
			/* 제일 처음에만 동기화 하도록 함 */
			synchronized(SysMonitor.class) {
				if( _sysInstance == null ) {
					_sysInstance = new SysMonitor();
				}
			}
		}
		return _sysInstance;
	} 
	
	private SysMonitor() {
		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
	}

	public void memoryTrace() {

		long totalMemory = Runtime.getRuntime().totalMemory();	// Total Memory
		long freeMemory  = Runtime.getRuntime().freeMemory();	// Free Memory
		long usedMemory  = totalMemory - freeMemory;			// Used Memory
		long maxMemory   = Runtime.getRuntime().maxMemory();
		log.setLength(0);

		log.append("[")
			.append(nf.format((float) (usedMemory) / 1024.0 / 1024.0))
			.append("MB(")
			.append(nf.format((float) (usedMemory) / (float) (maxMemory) * 100.0))
			.append("%)")
			.append("],")
			.append("[TPS : ").append(CacheCommon.getInstance().getSize() / 10).append("]");
		logger.info(log.toString());
	}
}
