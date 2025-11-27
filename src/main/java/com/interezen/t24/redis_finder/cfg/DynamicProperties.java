package com.interezen.t24.redis_finder.cfg;

import ch.qos.logback.classic.Logger;
import com.interezen.t24.redis_finder.logger.SysLogger;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

import java.util.List;

/**
 * Created by root on 2015-08-20.
 */
public class DynamicProperties {
    private static final String PROPERTIES_FILE_PATH = "config/dynamic.properties";
    private static final int PROPERTIES_REFRESH_DELAY = StaticProperties.getInstance().getInt("dynamic.delay", 10000);
    private static volatile DynamicProperties _instance = null;
    private PropertiesConfiguration configuration;
    private FileChangedReloadingStrategy fileChangedReloadingStrategy = new FileChangedReloadingStrategy();
    private Logger logger = SysLogger.getInstance().getLogger();

    public static DynamicProperties getInstance() {
        if( _instance == null ) {
			/* 제일 처음에만 동기화 하도록 함 */
            synchronized(DynamicProperties.class) {
                if( _instance == null ) {
                    _instance = new DynamicProperties();
                }
            }
        }
        return _instance;
    }

    private DynamicProperties() {
        init();
    }

    private void init() {
        try {
            logger.info("Loading the dynamic properties file: {}", PROPERTIES_FILE_PATH);
            configuration = new PropertiesConfiguration(PROPERTIES_FILE_PATH);
            //Create new FileChangedReloadingStrategy to reload the properties file based on the given time interval

            fileChangedReloadingStrategy.setRefreshDelay(PROPERTIES_REFRESH_DELAY);
            configuration.setReloadingStrategy(fileChangedReloadingStrategy);

        } catch (org.apache.commons.configuration.ConfigurationException e) {
            //e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    public String getString(String key) {
        return configuration.getString(key);
    }

    public String getString(String key, String defaultValue) {
        return configuration.getString(key, defaultValue);
    }

    public String getStringList(String key) {
        List<Object> returnValue;
        try {
            returnValue = configuration.getList(key);
        } catch (Exception e) {
            return null;
        }
        return returnValue.toString().replaceAll("\\]", "").replaceAll("\\[", "").replaceAll(" ", "");
    }

    public String[] getStringArray(String key) {
        String[] returnValue = null;
        try {
            returnValue = configuration.getStringArray(key);
        } catch (Exception e) {}

        return returnValue;
    }

    public int getInt(String key) {
        int returnValue = 0;
        try {
            returnValue = configuration.getInt(key);
        } catch (Exception e) {}
        return returnValue;
    }

    public int getInt(String key, int defaultValue) {
        int returnValue;
        try {
            returnValue = configuration.getInt(key, defaultValue);
        } catch (Exception e) {
            returnValue = 0;
        }
        return returnValue;
    }

    public long getLong(String key) {
        long returnValue;
        try {
            returnValue = configuration.getLong(key);
        } catch (Exception e) {
            returnValue = 0;
        }
        return returnValue;
    }

    public long getLong(String key, long defaultValue) {
        long returnValue;
        try {
            returnValue = configuration.getLong(key, defaultValue);
        } catch (Exception e) {
            returnValue = 0;
        }
        return returnValue;
    }

    public boolean getBoolean(String key) {
        boolean returnValue = false;
        try {
            returnValue = configuration.getBoolean(key);
        } catch (Exception e) {}
        return returnValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        boolean returnValue;
        try {
            returnValue = configuration.getBoolean(key, defaultValue);
        } catch (Exception e) {
            returnValue = false;
        }
        return returnValue;
    }

    public List<Object> getList(String key) {
        List<Object> returnValue;
        try {
            returnValue = configuration.getList(key);
        } catch (Exception e) {
            returnValue = null;
        }
        return returnValue;
    }

    public Object getProperty(String key) {
        return configuration.getProperty(key);
    }

    public static void main(String[] args) {
        //System.out.println(DynamicProperties.getInstance().getInt("test1", -1));
        while(true) {
            System.out.println(DynamicProperties.getInstance().getStringList("interface.list"));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
