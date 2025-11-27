package com.interezen.t24.redis_finder.pool.redis.object;

import ch.qos.logback.classic.Logger;
import com.interezen.t24.redis_finder.logger.RedisLogger;
import org.apache.commons.lang3.StringUtils;
import org.ragdoll.express.utils.ExpressExceptionUtils;
import redis.clients.jedis.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by yhsong
 * Date : 2017-06-20.
 * Time : 오후 12:26.
 * Description :
 */
public abstract class AbstractRedisPoolObject {
    private String instanceName;
    private RedisConnectionObject redisConnectionObject;
    private JedisPool redisPool;
    private JedisSentinelPool redisSentinelPool;
    private boolean isSentinel;
    private boolean isClustered;
    private String lineBreaker = System.getProperty("line.separator") == null ? "\n" : System.getProperty("line.separator");
    private Logger logger = RedisLogger.getInstance().getLogger();
    private JedisCluster jedisCluster;
    
    public AbstractRedisPoolObject(String instanceName) {
        this.instanceName = instanceName;
    }
    
    public void init(RedisConnectionObject redisConnectionObject) {
        this.redisConnectionObject = redisConnectionObject;
        this.isSentinel = redisConnectionObject.isSentinel();
        this.isClustered = redisConnectionObject.isClustered();
        
        if(isClustered) {
            initCluster();
        } else if(isSentinel) {
            initSentinel();
        } else {
            initSingle();
        }
    }

    private void initSingle() {
        String redisHost = redisConnectionObject.getHost();
        int redisPort = redisConnectionObject.getPort();
        if(redisPort == 0) {
            logger.error("{} => Redis Port Required ", instanceName);
            return;
        }
        int redisTimeout = redisConnectionObject.getConnectionTimeout();
        String redisPassword = redisConnectionObject.getPassword();
        JedisPoolConfig jedisPoolConfig = redisConnectionObject.getPoolConfig();
        if(StringUtils.isEmpty(redisPassword)) {
            redisPool = new JedisPool(jedisPoolConfig, redisHost, redisPort, redisTimeout);
        } else {
            redisPool = new JedisPool(jedisPoolConfig, redisHost, redisPort, redisTimeout, redisPassword);
        }

        if(testAction()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(lineBreaker);
            stringBuilder.append("================ [").append(instanceName).append("] ================").append(lineBreaker);
            stringBuilder.append(" 1. Redis Type            : Single").append(lineBreaker);
            stringBuilder.append(" 2. Redis Host            : ").append(redisHost).append(lineBreaker);
            stringBuilder.append(" 3. Redis Port            : ").append(redisPort).append(lineBreaker);
            stringBuilder.append(" 4. Redis Timeout         : ").append(redisTimeout).append(" ms").append(lineBreaker);
            stringBuilder.append(" 5. Redis Password        : ").append(redisPassword).append(lineBreaker);
            stringBuilder.append(" 6. Redis ConnectionCount : ").append(jedisPoolConfig.getMaxTotal()).append(lineBreaker);
            stringBuilder.append(" 7. Redis Pool Configuration").append(lineBreaker);
            stringBuilder.append("   - Pool Max : ").append(jedisPoolConfig.getMaxTotal()).append(lineBreaker);
            stringBuilder.append("   - Pool Max Idle : ").append(jedisPoolConfig.getMaxIdle()).append(lineBreaker);
            stringBuilder.append("   - Pool Min Idle : ").append(jedisPoolConfig.getMinIdle()).append(lineBreaker);
            stringBuilder.append("   - Pool MaxWaitMillis : ").append(jedisPoolConfig.getMaxWaitMillis()).append(lineBreaker);
            stringBuilder.append("   - Pool TestOnCreate : ").append(jedisPoolConfig.getTestOnCreate()).append(lineBreaker);
            stringBuilder.append("   - Pool TestOnBorrow : ").append(jedisPoolConfig.getTestOnBorrow()).append(lineBreaker);
            stringBuilder.append("   - Pool TestOnReturn : ").append(jedisPoolConfig.getTestOnReturn()).append(lineBreaker);
            stringBuilder.append("   - Pool BlockWhenExhausted : ").append(jedisPoolConfig.getBlockWhenExhausted()).append(lineBreaker);
            stringBuilder.append("   - Pool Lifo : ").append(jedisPoolConfig.getLifo()).append(lineBreaker);
            stringBuilder.append("============================================================");
            logger.info("{} Information => {}", instanceName, stringBuilder.toString());
        }
    }

    private void initSentinel() {

        Set<String> sentinels = redisConnectionObject.getSentinels();
        if(sentinels.size() == 0) {
            logger.error("{} => Sentinel Host not exist.");
            return;
        }
        int redisTimeout = redisConnectionObject.getConnectionTimeout();
        String redisPassword = redisConnectionObject.getPassword();
        String redisSentinelMasterName = redisConnectionObject.getSentinelMasterName();
        JedisPoolConfig jedisPoolConfig = redisConnectionObject.getPoolConfig();

        if(StringUtils.isEmpty(redisPassword)) {
            redisSentinelPool = new JedisSentinelPool(redisSentinelMasterName, sentinels, jedisPoolConfig, redisTimeout);
        } else {
            redisSentinelPool = new JedisSentinelPool(redisSentinelMasterName, sentinels, jedisPoolConfig, redisTimeout, redisPassword);
        }

        if(testAction()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(lineBreaker);
            stringBuilder.append("================ [").append(instanceName).append("] ================").append(lineBreaker);
            stringBuilder.append(" 1. Redis Type            : Sentinel").append(lineBreaker);
            stringBuilder.append(" 2. Sentinel Info         ").append(lineBreaker);
            stringBuilder.append("   - urls : ");
            for (String str : sentinels) {
                stringBuilder.append(str).append(",");
            }
            stringBuilder.append(lineBreaker);
            stringBuilder.append("   - masterNode : ").append(redisSentinelPool.getCurrentHostMaster()).append(lineBreaker);
            stringBuilder.append(" 3. Redis Timeout         : ").append(redisTimeout).append(" ms").append(lineBreaker);
            stringBuilder.append(" 4. Redis Password        : ").append(redisPassword).append(lineBreaker);
            stringBuilder.append(" 5. Redis ConnectionCount : ").append(jedisPoolConfig.getMaxTotal()).append(lineBreaker);
            stringBuilder.append(" 6. Redis Pool Configuration").append(lineBreaker);
            stringBuilder.append("   - Pool Max : ").append(jedisPoolConfig.getMaxTotal()).append(lineBreaker);
            stringBuilder.append("   - Pool Max Idle : ").append(jedisPoolConfig.getMaxIdle()).append(lineBreaker);
            stringBuilder.append("   - Pool Min Idle : ").append(jedisPoolConfig.getMinIdle()).append(lineBreaker);
            stringBuilder.append("   - Pool MaxWaitMillis : ").append(jedisPoolConfig.getMaxWaitMillis()).append(lineBreaker);
            stringBuilder.append("   - Pool TestOnCreate : ").append(jedisPoolConfig.getTestOnCreate()).append(lineBreaker);
            stringBuilder.append("   - Pool TestOnBorrow : ").append(jedisPoolConfig.getTestOnBorrow()).append(lineBreaker);
            stringBuilder.append("   - Pool TestOnReturn : ").append(jedisPoolConfig.getTestOnReturn()).append(lineBreaker);
            stringBuilder.append("   - Pool BlockWhenExhausted : ").append(jedisPoolConfig.getBlockWhenExhausted()).append(lineBreaker);
            stringBuilder.append("   - Pool Lifo : ").append(jedisPoolConfig.getLifo()).append(lineBreaker);
            stringBuilder.append("============================================================");
            logger.info("{} Information => {}", instanceName, stringBuilder.toString());
        }
    }
    
    private void initCluster() {
        Set<HostAndPort> nodes = null;
        JedisPoolConfig jedisPoolConfig = null;
        Jedis jedis = null;
        
        nodes = this.redisConnectionObject.getNodes();
        
        if(nodes == null || nodes.isEmpty()) {
            logger.error("{} => Could't find connectable redis cluster nodes...");
            return;
        }
        
        // JedisCluster는 timeout, configuration 등이 이미 생성 시 입력되기 때문에 ClusterRedisPool instance에 있는 JedisCluster instance를 그냥 사용한다.
        /*
            redis cluster는 {지정된 port + 10000} port를 통해 tcp bus를 구성하여 ping-pong을 통해 각각의 node들의 상태정보를 공유한다.
            key hash를 통해 클라이언트의 접근 시 해당하는 node(slot)에 데이터를 입/출력 한다.
            (클라이언트는 JedisClient 구현 후 특정 node를 선택해서 입력을 시도할 필요가 없다!)
            (too many redirection exception에 대한 대응 필요)
         */
        // 중요 : JedisCluster instance는 자체에 pool기능이 탑재 되어있고, Jedis instance와 다르게 JedisCluster instance에 close method를 실행하면 전체 connection이 shutdown 된다.
        this.jedisCluster = this.redisConnectionObject.getJedisCluster();
        jedisPoolConfig = this.redisConnectionObject.getPoolConfig();
        
        if(testClusterAction()) {
            Set<String> keys = jedisCluster.getClusterNodes().keySet();
            
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(lineBreaker);
            stringBuilder.append("================ [").append(instanceName).append("] ================").append(lineBreaker);
            stringBuilder.append(" 1. Redis Type            : Cluster").append(lineBreaker);
            stringBuilder.append(" 2. Cluster Info         ").append(lineBreaker);
            stringBuilder.append("   - urls : ").append(lineBreaker);
            for (HostAndPort hostAndPort : nodes) {
                stringBuilder.append("      ").append(hostAndPort.toString()).append(lineBreaker);
            }
            stringBuilder.append(" 3. Redis ConnectionCount : ").append(jedisPoolConfig.getMaxTotal()).append(lineBreaker);
            stringBuilder.append(" 4. Redis Pool Configuration").append(lineBreaker);
            stringBuilder.append("   - Pool Max : ").append(jedisPoolConfig.getMaxIdle()).append(lineBreaker);
            stringBuilder.append("   - Pool Max Idle : ").append(jedisPoolConfig.getMaxIdle()).append(lineBreaker);
            stringBuilder.append("   - Pool Min Idle : ").append(jedisPoolConfig.getMinIdle()).append(lineBreaker);
            stringBuilder.append("   - Pool MaxWaitMillis : ").append(jedisPoolConfig.getMaxWaitMillis()).append(lineBreaker);
            stringBuilder.append("   - Pool TestOnCreate : ").append(jedisPoolConfig.getTestOnCreate()).append(lineBreaker);
            stringBuilder.append("   - Pool TestOnBorrow : ").append(jedisPoolConfig.getTestOnBorrow()).append(lineBreaker);
            stringBuilder.append("   - Pool TestOnReturn : ").append(jedisPoolConfig.getTestOnReturn()).append(lineBreaker);
            stringBuilder.append("   - Pool BlockWhenExhausted : ").append(jedisPoolConfig.getBlockWhenExhausted()).append(lineBreaker);
            stringBuilder.append("   - Pool MinEvictableIdleTimeMillis : ").append(jedisPoolConfig.getMinEvictableIdleTimeMillis()).append(lineBreaker);
            stringBuilder.append("   - Pool TimeBetweenEvictionRunsMillis : ").append(jedisPoolConfig.getTimeBetweenEvictionRunsMillis()).append(lineBreaker);
            stringBuilder.append("   - Pool Lifo : ").append(jedisPoolConfig.getLifo()).append(lineBreaker);
            
            stringBuilder.append(" 5. Cluster Nodes Connection Test").append(lineBreaker);
            for (String key : keys) {
                stringBuilder.append("   Test Target : " + key).append(lineBreaker);
                try {
                    jedis = jedisCluster.getClusterNodes().get(key).getResource();
                    
                    stringBuilder.append("   - IsConnected : " + jedis.isConnected()).append(lineBreaker);
                    stringBuilder.append("   - Asking : " + jedis.asking()).append(lineBreaker);
                    stringBuilder.append("   - Ping : " + jedis.ping()).append(lineBreaker);
                } catch(Exception e) {
                    stringBuilder.append("   - Not connected... Checking " + key + " redis instance...").append(lineBreaker);
                }
            }
            stringBuilder.append("============================================================");
            logger.info("{} Information => {}", instanceName, stringBuilder.toString());
        }
    }
    
    public void closeCluster() {
        Collection<JedisPool> jedisPools = null;
        List<Object> jedisPools1 = null;
        
        try {
            // clearing sub jedis pool connections
            jedisPools = jedisCluster.getClusterNodes().values();
            jedisPools1 = Arrays.asList(jedisPools.toArray());
            for(Object o : jedisPools1) {
                try {
                    ((JedisPool) o).destroy();
                } catch(Exception e) {
                }
            }
            
            // clearing jedis cluster instance    
            if(jedisCluster != null)
                jedisCluster.close();

            // clearing jedis cluster configuration data
            if(this.redisConnectionObject != null) {
                this.redisConnectionObject.cleaningUp();
            }
        } catch(Exception e) {
            logger.error("{} => " + ExpressExceptionUtils.getStackTrace(e));
        } finally {
            jedisCluster = null;
        }
    }
    
    private boolean testClusterAction() {
        JedisCluster connection = null;
        boolean success = false;
        String key = "TEST_" + instanceName;
        try {
            connection = getClusterResource();
            connection.set(key, LocalDate.now().toString());
            connection.del(key);
            success = true;
        } catch(Exception e) {
            success = false;
            logger.error("{} => redis cluster exception : {}", instanceName, ExpressExceptionUtils.getStackTrace(e));
        }
        
        if(!success) {
            logger.error("{} => >>>>> Redis Clsuter Connection Pool Fail....", instanceName);
        } else {
            logger.error("{} => >>>>> Redis Clsuter Connection Pool Success Loading", instanceName);
        }
        return success;
    }
    
    private boolean testAction() {
        Jedis connection = null;
        boolean success = false;
        String key = "TEST_" + instanceName;
        try {
            connection = getResource();
            connection.set(key, LocalDate.now().toString());
            connection.del(key);
            success = true;
        } catch(Exception e) {
            success = false;
            logger.error("{} => redis exception : {}", instanceName, ExpressExceptionUtils.getStackTrace(e));
        } finally {
            if( null != connection ) {
                connection.close();
            }
        }
        if(!success) {
            logger.error("{} => >>>>> Redis Connection Pool Fail, Verify Redis Server....", instanceName);
        } else {
            logger.error("{} => >>>>> Redis Connection Pool Success Loading", instanceName);
        }
        return success;
    }
    
    public JedisCluster getClusterResource() {
        return jedisCluster == null ? null : jedisCluster;
    }
    
    public Jedis getResource() {
        if(isSentinel) {
            return redisSentinelPool == null ? null : redisSentinelPool.getResource();
        } else {
            return redisPool == null ? null : redisPool.getResource();
        }
    }

    public int getActiveCount() {
        if(isSentinel) {
            return redisSentinelPool == null ? 0 : redisSentinelPool.getNumActive();
        } else {
            return redisPool == null ? 0 : redisPool.getNumActive();
        }
    }

    public int getIdleCount() {
        if(isSentinel) {
            return redisSentinelPool == null ? 0 : redisSentinelPool.getNumIdle();
        } else {
            return redisPool == null ? 0 : redisPool.getNumIdle();
        }
    }

    public int getWaitCount() {
        if(isSentinel) {
            return redisSentinelPool == null ? 0 : redisSentinelPool.getNumWaiters();
        } else {
            return redisPool == null ? 0 : redisPool.getNumWaiters();
        }
    }

    public boolean isClosed() {
        if(isSentinel) {
            return redisSentinelPool == null ? true : redisSentinelPool.isClosed();
        } else {
            return redisPool == null ? true : redisPool.isClosed();
        }
    }
    
    public void close(Jedis jedis) {
        try {
            if (jedis != null) {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error("{} => {}", instanceName, ExpressExceptionUtils.getStackTrace(e));
            jedis = null;
        }
    }

    public void returnResource(Jedis jedis) {
        if(jedis != null) {
            try {
                jedis.close();
            } catch (Exception e) {
                logger.error("{} => {}", instanceName, ExpressExceptionUtils.getStackTrace(e));
            } finally {
                jedis = null;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        // cluster mode에서 resource close가 실행되지 않았을 경우를 대비
        if(isClustered) {
            try {
                closeCluster();
            } catch(Exception e) {
                logger.error("{} => " + ExpressExceptionUtils.getStackTrace(e));
            }
        }
        
        super.finalize();
    }
}
