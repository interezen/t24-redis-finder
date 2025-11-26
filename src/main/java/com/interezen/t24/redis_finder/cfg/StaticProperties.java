package com.interezen.t24.redis_finder.cfg;

import ch.qos.logback.classic.Logger;
import com.interezen.t24.redis_finder.logger.SysLogger;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by root on 2015-08-20.
 */
public class StaticProperties {
    private static final String PROPERTIES_FILE_PATH = "config/static.properties";
    private static volatile StaticProperties _instance = null;
    private PropertiesConfiguration configuration;
    private Logger logger = SysLogger.getInstance().getLogger();

    public static StaticProperties getInstance() {
        if( _instance == null ) {
			/* 제일 처음에만 동기화 하도록 함 */
            synchronized(StaticProperties.class) {
                if( _instance == null ) {
                    _instance = new StaticProperties();
                }
            }
        }
        return _instance;
    }

    private StaticProperties() {
        init();
    }

    private void init() {
        try {
            logger.info("Loading the static properties file: {}", PROPERTIES_FILE_PATH);
            configuration = new PropertiesConfiguration(PROPERTIES_FILE_PATH);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            logger.error(e.getMessage());
        }
    }

    public String getString(String key) {
        return configuration.getString(key);
    }

    public String getString(String key, String defaultValue) {
        return configuration.getString(key, defaultValue);
    }

    public int getInt(String key) {
        int retrunValue = 0;
        try {
            retrunValue = configuration.getInt(key);
        } catch (Exception e) {}
        return retrunValue;
    }

    public int getInt(String key, int defaultValue) {
        int retrunValue;
        try {
            retrunValue = configuration.getInt(key, defaultValue);
        } catch (Exception e) { retrunValue = 0; }
        return retrunValue;
    }

    public boolean getBoolean(String key) {
        boolean retrunValue = false;
        try {
            retrunValue = configuration.getBoolean(key);
        } catch (Exception e) {}
        return retrunValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        boolean retrunValue;
        try {
            retrunValue = configuration.getBoolean(key, defaultValue);
        } catch (Exception e) { retrunValue = false; }
        return retrunValue;
    }

    public List<Object> getList(String key) {
        List<Object> returnValue = null;
        try {
            returnValue = configuration.getList(key);
        } catch (Exception e) {}
        return returnValue;
    }

    public String[] getStringArray(String key) {
        String[] returnValue = null;
        try {
            returnValue = configuration.getStringArray(key);
        } catch (Exception e) {}
        return returnValue;
    }

    public ArrayList<String> getManifestVer() {

        String path1 = getString("module.name", "bridge.jar");
        String path2 = "./jar/I3G.jar";
        String path  = "";
        ArrayList<String> manifestInfo = new ArrayList<String>();
        File file = null;
        JarFile jarfile = null;
        StringBuilder sb = new StringBuilder();

        try {

            file = new File(path1);

            if(file.isFile()){
                path = path1;
            } else {
                file = new File(path2);
                path = path2;
                if(!file.isFile()){
                    return null;
                }
            }

            jarfile = new JarFile(path);

            // 매니페스트 정보 취득
            Manifest manifest = jarfile.getManifest();

            // 속성 정보 취득
            Attributes mAttrib = manifest.getMainAttributes();

            // 속성 정보를 저장함
            for (Map.Entry<?, ?> key : mAttrib.entrySet()) {
                sb.append("  - ").append(key.getKey()).append(" : ").append(key.getValue());
                manifestInfo.add(sb.toString());
                sb.setLength(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jarfile != null) {
                try {
                    jarfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return manifestInfo;
    }

    public static void main(String[] args) {
        System.out.println(StaticProperties.getInstance().getInt("test1", -1));

    }
}
