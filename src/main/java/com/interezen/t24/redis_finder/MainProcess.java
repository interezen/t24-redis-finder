package com.interezen.t24.redis_finder;

import ch.qos.logback.classic.Logger;
import com.interezen.api.cojlib.CoObject;
import com.interezen.api.cojlib.CoThread;
import com.interezen.api.cojlib.ILifeCycle;
import com.interezen.t24.redis_finder.cfg.DynamicProperties;
import com.interezen.t24.redis_finder.cfg.StaticProperties;
import com.interezen.t24.redis_finder.define.ProcessDefine;
import com.interezen.t24.redis_finder.logger.SysLogger;
import com.interezen.t24.redis_finder.monitor.SysMonitor;
import com.interezen.t24.redis_finder.pool.redis.RedisPool;
import com.interezen.t24.redis_finder.receiver.ReceiveServer;
import org.ragdoll.express.utils.ExpressExceptionUtils;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Created by jkpark on 2018-12-20.
 */
public class MainProcess extends CoObject implements ILifeCycle {
	private Logger logger = SysLogger.getInstance().getLogger();
	
	private CoThread thread = null;
	protected boolean bLoop = true;

	/** 기본 생성자 */
	public MainProcess() {
		setName("RedisFinderMainHandler#");
		start();
	}

	/** 쓰레드 생성/구동 매소드 */
	public void start()	{
		if(( thread = new CoThread( this )) != null ) {
			thread.start();
		}
	}

	/** 쓰레드 중지 변수를 false로 설정한다. */
	public void stop() {
		bLoop = false;
	}

	//////////////////////////////////  E V E N T  P R O C E S S  //////////////////////////////////
	public void OnInit() {
		thread.setDaemon(false);

		logger.info("");
		logger.info(" => Process Initializing.........");
		
		initResources();
	}

	public void OnBegin() {
		logger.info("");
		logger.info(" => Module Daemon Process Loading.........");
		
		int receiverPort = StaticProperties.getInstance().getInt("receiver.port", 21001);
		if(receiverPort > 0) {
			logger.info(" (*). Receiver Start");
			new ReceiveServer(receiverPort).start();
			sleep(50);
		}
		
		logger.info(" => Module Daemon Process Loaded.........");
	}

	public void OnDestroy()	{
		logger.info("");
		logger.info(" => Process Destroying.........");
	}
	
	private void initResources() {
		Properties p = System.getProperties();
		Enumeration keys = p.keys();
		
		logger.info("\n\n--------------------------------------------------------------------------------------------");
		logger.info(" +================================================================================+");
		logger.info(" | ### JVM for T24 Redis Finder...");
		logger.info(" | ### ");

		String key;
		while(keys.hasMoreElements()) {
			key = (String)keys.nextElement();
			logger.info(" | - {} : {}", key, p.get(key));
		}
		logger.info(" | - Available Processors(cores) : {}", Runtime.getRuntime().availableProcessors());
		logger.info(" | - Maximum memory (bytes) : {}", Runtime.getRuntime().totalMemory());
		logger.info(" +================================================================================+");

		logger.info(" => Module Resource Loading.........");

		StaticProperties.getInstance();
		DynamicProperties.getInstance();

		ProcessDefine.REDIS_ID = StaticProperties.getInstance().getString("pool.redis.id", "redis-finder");

		RedisPool.getInstance();

		
		logger.info(" => Module Resource Loaded.........");
	}

	public static void main(String[] args) {
		new MainProcess();
	}

	public void OnProcess() throws Exception {
		while (bLoop) {
			try {
				if(DynamicProperties.getInstance().getBoolean("memory.status.trace.use", false)) {
					SysMonitor.getInstance().memoryTrace();
				}
			} catch (OutOfMemoryError oe) {
				logger.error("{} OOME_ERROR => {}", getName(), oe);
				bLoop = false;
				System.exit(0);
			} catch (Exception e) {
				logger.error("{} => {}", getName(), ExpressExceptionUtils.getStackTrace(e));
			}
			sleep(DynamicProperties.getInstance().getInt("memory.sleep.time", 5000));
		}
	}
}