/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.interezen.t24.redis_finder.receiver;

import ch.qos.logback.classic.Logger;
import com.google.gson.Gson;
import com.interezen.t24.redis_finder.cfg.DynamicProperties;
import com.interezen.t24.redis_finder.define.ProcessDefine;
import com.interezen.t24.redis_finder.logger.ReceiveLogger;
import com.interezen.t24.redis_finder.monitor.CacheCommon;
import com.interezen.t24.redis_finder.utils.PacketUtils;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.ragdoll.express.pool.lettuce.instance.ExpressLettucePool;
import org.ragdoll.express.pool.lettuce.object.ExpressLettucePoolObject;
import org.ragdoll.express.pool.lettuce.object.ExpressLettuceSyncMode;
import org.ragdoll.express.utils.ExpressExceptionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

/**
 * Handler implementation for the echo server.
 */
public class ReceiveServerHandler extends ChannelInboundHandlerAdapter {
	private Logger logger = ReceiveLogger.getInstance().getLogger();

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg == null) {
			return;
		}
		
		boolean printTimeUse = DynamicProperties.getInstance().getBoolean("print.time.use", false);
		Integer limitValue = 0;
		String data = null, result = null;
		long start = 0l;
		
		if(printTimeUse)
			start = System.currentTimeMillis();
		
		// TPS 측정위한 로직
		if (DynamicProperties.getInstance().getBoolean("tps.stats.use", false)) {
			CacheCommon.getInstance().put(UUID.randomUUID().toString(), "");
		}

		data = (StringUtils.defaultString((String) msg, "")).trim();
		logger.debug("{} => {}", ctx.channel(), "data : " + data);

		if (!data.isEmpty()) {
			result = process(ctx, data);
			
			if (result == null) {
				result = DynamicProperties.getInstance().getString("result.request.parsing.exception", "-2");
			} else {
				limitValue = DynamicProperties.getInstance().getInt("result.true.limit", 0);
				
				logger.debug("{} => {}", ctx.channel(), "redis personal value : " + result);
				logger.debug("{} => {}", ctx.channel(), "limitValue : " + limitValue);

				if(NumberUtils.isNumber(result) && Integer.parseInt(result) >= 0)
					result = (Integer.parseInt(result) > limitValue) ? "T" : "F";
			}
		} else {
			logger.error("{} => {}", ctx.channel(), "Request data is empty...");
			
			// 입력된 데이터가 비어있기 때문에 오류를 return한다.
			result = DynamicProperties.getInstance().getString("result.request.empty", "-1");
		}
		
		logger.debug("{} => {}", ctx.channel(), "Result : " + result);

		if(printTimeUse) {
			printTime(ctx, "channelRead", start);
		}
		
		ctx.channel().writeAndFlush(result);
		// 테스트용.
//		    ctx.channel().writeAndFlush(result + "\n");
	}

	private String process(ChannelHandlerContext ctx, String data) {
		boolean printTimeUse = DynamicProperties.getInstance().getBoolean("print.time.use", false);
		String type = DynamicProperties.getInstance().getString("data.type", "personal");
		String result = null;
		Map<String, String> parsedData = null;
		
		String primaryKey = null;
		String primaryValue = null;
		Map<String, String> necessaryFields = new HashMap<String, String>();
		Set<String> keySet = null;
		String value = null;
		String tempKey = null, tempValue = null;
		List<Object> necessaryKeyList = null;
		String primaryKeyException = DynamicProperties.getInstance().getString("result.primary.exception", "-3");
		String necessaryKeyException = DynamicProperties.getInstance().getString("result.necessary.exception", "-4");
		
		Gson gson = new Gson();

		long start = 0l;

		if(printTimeUse)
			start = System.currentTimeMillis();
		
		primaryKey = DynamicProperties.getInstance().getString("primary.key", null);
		necessaryKeyList = DynamicProperties.getInstance().getList("necessary.key");
		
		logger.debug("{} => {}", ctx.channel(), "primaryKey : " + primaryKey);
		
		if(primaryKey != null) {
			primaryValue = DynamicProperties.getInstance().getString(primaryKey + ".value", null);
			logger.debug("{} => {}", ctx.channel(), "primaryValue : " + primaryValue);
		}
		
		if(necessaryKeyList != null && !necessaryKeyList.isEmpty()) {
			for(Object obj : necessaryKeyList) {
				tempKey = (String)obj;
				
				if(tempKey != null && !tempKey.isEmpty()) {
					tempValue = DynamicProperties.getInstance().getString(tempKey + ".value", null);
					necessaryFields.put(tempKey, tempValue);
				}
 			}

			logger.debug("{} => {}", ctx.channel(), "necessaryFields : " + gson.toJson(necessaryFields));
		}
		
		if (necessaryFields.isEmpty()) {
			logger.debug("{} => {}", ctx.channel(), "necessaryKeyList is empty...");
		}
		
		// key-value parsing
		parsedData = parsing(ctx, data);

		logger.debug("{} => {}", ctx.channel(), "parsedData : " + gson.toJson(parsedData));
		
		if (parsedData == null) {
			logger.error("{} => {}", ctx.channel(), "parsing exception..." + System.lineSeparator() + "request data : " + data);

			if(printTimeUse) {
				printTime(ctx, "process", start);
			}
			
			return null;
		}
		
		if(primaryKey != null && !primaryKey.isEmpty()) {
			if(primaryValue == null || (primaryValue != null && primaryValue.isEmpty())) {
				// 설정에 primary key는 있지만 value가 없다
				logger.error("{} => {}", ctx.channel(), "Primary value missing... check dynamic.properties...");

				if(printTimeUse) {
					printTime(ctx, "process", start);
				}
				
				return primaryKeyException;
			} else if((primaryValue != null && !primaryValue.isEmpty()) && !parsedData.containsKey(primaryKey)) {
				// 설정에 primary key와 value는 있으나, request에 primary key가 없다.
				logger.error("{} => {}", ctx.channel(), "Primary value missing... check request data...");

				if(printTimeUse) {
					printTime(ctx, "process", start);
				}
				
				return primaryKeyException;
			} else if((primaryValue != null && !primaryValue.isEmpty()) && parsedData.containsKey(primaryKey) && !primaryValue.equals(parsedData.get(primaryKey))) {
				// 설정에 primary key와 value는 있으나, request의 primary value가 설정과 다르다
				logger.error("{} => {}", ctx.channel(), "Primary value not matched... check request data...");

				if(printTimeUse) {
					printTime(ctx, "process", start);
				}
				
				return primaryKeyException;
			}
			// 그 외에는 정상 통과
		} else {
			// primary key가 설정되어있지 않다. 통과.
			logger.debug("{} => {}", ctx.channel(), "Do not use the primary key...");
		}
		
		if(!necessaryFields.isEmpty()) {
			keySet = necessaryFields.keySet();
			
			logger.debug("{} => {}", ctx.channel(), "keys : " + gson.toJson(keySet));
			
			for(String key : keySet) {
				value = necessaryFields.get(key);
				
				if(!parsedData.containsKey(key)) {
					// request에 necessary key가 없다.
					logger.error("{} => {}", ctx.channel(), "Necessary key(" + key + ") missing... check dynamic.properties...");

					if(printTimeUse) {
						printTime(ctx, "process", start);
					}
					
					return necessaryKeyException;
				} else if(value != null && !value.isEmpty()) {
					if(!value.equals(parsedData.get(key))) {
						// 설정에 necessary key와 value가 설정되어있으나, request의 해당 value가 설정과 다르다.
						logger.error("{} => {}", ctx.channel(), "Necessary value not matched... check request data...");

						if(printTimeUse) {
							printTime(ctx, "process", start);
						}
						
						return necessaryKeyException;
					}
					
					// request의 value와 설정의 value가 같다면 통과
				}
				
				// 설정에 necessary key는 있으나, value가 없을 경우 request에 해당 key만 존재하면 값에 대한 검증은 하지 않는다.
			}
		}

		logger.debug("{} => {}", ctx.channel(), "Module processing type : " + type);
		
		// data type
		if (type.equals("personal")) {
			result = personal(ctx, parsedData);
		}

		if(printTimeUse) {
			printTime(ctx, "process", start);
		}

		return result;
	}

	private Map<String, String> parsing(ChannelHandlerContext ctx, String data) {
		boolean printTimeUse = DynamicProperties.getInstance().getBoolean("print.time.use", false);
		String keyValueDelimiter = DynamicProperties.getInstance().getString("request.key.value.delimiter", "=");
		String fieldDelimiter = DynamicProperties.getInstance().getString("request.field.delimiter", ",");
		Map<String, String> parsedData = null;

		long start = 0l;

		if(printTimeUse)
			start = System.currentTimeMillis();
		
		try {
			logger.debug("{} => {}", ctx.channel(), "Data parsing...");
			logger.debug("{} => {}", ctx.channel(), "keyValueDelimiter : " + keyValueDelimiter);
			logger.debug("{} => {}", ctx.channel(), "fieldDelimiter : " + fieldDelimiter);
			parsedData = PacketUtils.kv(data, fieldDelimiter, keyValueDelimiter);
		} catch (Exception e) {
			logger.error("{} => {}", ctx.channel(), ExpressExceptionUtils.getStackTrace(e));
			parsedData = null;
		}

		if(printTimeUse) {
			printTime(ctx, "parsing", start);
		}

		return parsedData;
	}

	private String personal(ChannelHandlerContext ctx, Map<String, String> parsedData) {
		boolean printTimeUse = DynamicProperties.getInstance().getBoolean("print.time.use", false);
		String personalPrefixStr = "persona";
		String personalDelimiter = DynamicProperties.getInstance().getString("personal.delimiter", "|||");
		String personalNo = DynamicProperties.getInstance().getString("personal.no", "0");
		String personalBasisFieldKey = DynamicProperties.getInstance().getString("personal.basis.field.key", "basisField");
		String personalTargetFieldKey = DynamicProperties.getInstance().getString("personal.target.field.key", "targetField");
		String personalBasisFieldValue = null, personalTargetFieldValue = null;
		String resultFieldKey = DynamicProperties.getInstance().getString("personal.result.field", "count");
		String redisKey = null;
		String result = null;
		JedisCluster jedisCluster = null;
		Jedis jedis = null;

		String basisFieldException = DynamicProperties.getInstance().getString("result.basis.field.exception", "-5");
		String redisException = DynamicProperties.getInstance().getString("result.redis.exception", "-6");

		long start = 0l;

		if(printTimeUse)
			start = System.currentTimeMillis();
		
		logger.debug("{} => {}", ctx.channel(), "personalPrefixStr : " + personalPrefixStr);
		logger.debug("{} => {}", ctx.channel(), "personalDelimiter : " + personalDelimiter);
		logger.debug("{} => {}", ctx.channel(), "personalNo : " + personalNo);
		logger.debug("{} => {}", ctx.channel(), "personalBasisFieldKey : " + personalBasisFieldKey);
		logger.debug("{} => {}", ctx.channel(), "personalTargetFieldKey : " + personalTargetFieldKey);
		logger.debug("{} => {}", ctx.channel(), "resultFieldKey : " + resultFieldKey);
		
		if (!parsedData.containsKey(personalBasisFieldKey)) {
			logger.error("{} => {}", ctx.channel(), "Basis field not exists...(key : " + personalBasisFieldKey + ")");

			if(printTimeUse) {
				printTime(ctx, "personal", start);
			}
			
			return basisFieldException;
		}
		
		personalBasisFieldValue = parsedData.get(personalBasisFieldKey);
		personalTargetFieldValue = parsedData.containsKey(personalTargetFieldKey) ? parsedData.get(personalTargetFieldKey) : null;
		
		redisKey = personalPrefixStr + personalDelimiter + personalNo + personalDelimiter + personalBasisFieldValue + personalDelimiter + personalTargetFieldValue;

		logger.debug("{} => {}", ctx.channel(), "personalBasisFieldValue : " + personalBasisFieldValue);
		logger.debug("{} => {}", ctx.channel(), "personalTargetFieldValue : " + personalTargetFieldValue);
		logger.debug("{} => {}", ctx.channel(), "redisKey : " + redisKey);

		ExpressLettucePoolObject lettucePoolObject = ExpressLettucePool.getInstance().get(ProcessDefine.REDIS_ID);

		StatefulRedisConnection<String, String> connection = null;
		StatefulRedisClusterConnection<String, String> connectionCluster = null;

		long redisCommandDuration = 0L;
		long totalDuration = 0L;

		try {
			RedisCommands<String, String> commands;
			RedisAdvancedClusterCommands<String, String> commandsCluster;
			StatefulConnection<String,String> statefulConnection = lettucePoolObject.borrow(ExpressLettuceSyncMode.SYNC, 500);

			if (statefulConnection instanceof StatefulRedisClusterConnection) {
				connectionCluster = (StatefulRedisClusterConnection<String, String>) statefulConnection;
				commandsCluster = connectionCluster.sync();

				result = commandsCluster.hget(redisKey, resultFieldKey);
			} else if (statefulConnection instanceof StatefulRedisConnection) {
				connection = (StatefulRedisConnection<String, String>) statefulConnection;
				commands = connection.sync();

				result = commands.hget(redisKey, resultFieldKey);
			}

			if (StringUtils.isEmpty(result)) {
				result = "0";
			}
		} catch (Exception e) {
			logger.error("{} => {}", ctx.channel(), ExpressExceptionUtils.getStackTrace(e));
			logger.error("{} => {}", ctx.channel(), "Redis select failed...");

			if(printTimeUse) {
				printTime(ctx, "personal", start);
			}

			return redisException;
		} finally {
			if (connection != null) {
				lettucePoolObject.release(ExpressLettuceSyncMode.SYNC, connection);
			}
			if (connectionCluster != null) {
				lettucePoolObject.release(ExpressLettuceSyncMode.SYNC, connectionCluster);
			}
		}

		if(printTimeUse) {
			printTime(ctx, "personal", start);
		}
		
		return result;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		// Close the connection when an exception is raised.
		logger.error("Handler[{}] Error => Handler when an exception is raised : {}", ctx.channel().remoteAddress(), cause.toString());
		ctx.close();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			if (e.state() == IdleState.READER_IDLE) {
				// Read Timeout 시 Connection Close
				ctx.channel().close();
			}
		}
	}
	
	private void printTime(ChannelHandlerContext ctx, String method, Long start) {
		long end = 0l, time = 0l;
		
		end = System.currentTimeMillis();
		time = end - start;
		logger.info("{} => {}", ctx.channel(), method + " start : " + Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDateTime());
		logger.info("{} => {}", ctx.channel(), method + " end : " + Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDateTime());
		logger.info("{} => {}", ctx.channel(), method + " duration : " + time + " milliseconds");
	}
}