package com.interezen.t24.redis_finder.pool.redis.object;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Set;

/**
 * Created by yhsong
 * Date : 2017-06-20.
 * Time : 오후 12:31.
 * Description :
 */
public class RedisConnectionObject {
    private JedisPoolConfig poolConfig;

    private boolean isSentinel = false;
    private String host;
    private int port;
    private int connectionTimeout;
    private String password;

    private Set<String> sentinels;
    private String sentinelMasterName;

    private boolean isClustered = false;
    private Set<HostAndPort> nodes;
    private JedisCluster jedisCluster;

    public JedisPoolConfig getPoolConfig() {
        return poolConfig == null ? new JedisPoolConfig() : poolConfig;
    }

    public void setPoolConfig(JedisPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

    public boolean isSentinel() {
        return isSentinel;
    }

    public void setSentinel(boolean sentinel) {
        isSentinel = sentinel;
    }

    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getSentinels() {
        return sentinels;
    }

    public void setSentinels(Set<String> sentinels) {
        this.sentinels = sentinels;
    }

    public String getSentinelMasterName() {
        return sentinelMasterName;
    }

    public void setSentinelMasterName(String sentinelMasterName) {
        this.sentinelMasterName = sentinelMasterName;
    }

    public boolean isClustered() {
        return isClustered;
    }

    public void setClustered(boolean clustered) {
        isClustered = clustered;
    }

    public JedisCluster getJedisCluster() {
        return jedisCluster;
    }

    public void setJedisCluster(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public Set<HostAndPort> getNodes() {
        return nodes;
    }

    public void setNodes(Set<HostAndPort> nodes) {
        this.nodes = nodes;
    }

    // clearing configuration data
    public void cleaningUp() {
        this.poolConfig = null;
        this.isSentinel = false;
        this.host = null;
        this.port = 0;
        this.connectionTimeout = 0;
        this.password = null;
        this.sentinels = null;
        this.sentinelMasterName = null;
        this.isClustered = false;
        this.nodes = null;
        
        if(this.jedisCluster != null) {
            try {
                this.jedisCluster.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
