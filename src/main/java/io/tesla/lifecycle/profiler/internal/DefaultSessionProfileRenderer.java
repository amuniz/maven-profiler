/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.lifecycle.profiler.internal;

import io.tesla.lifecycle.profiler.SessionProfileRenderer;
import io.tesla.lifecycle.profiler.Timer;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Named
@Singleton
public class DefaultSessionProfileRenderer implements SessionProfileRenderer {

    private Timer timer;
    private Map<String, Timer> mojoTimes;
    private Properties p;

    private Logger logger;

    @Inject
    public DefaultSessionProfileRenderer() {
        this.mojoTimes = new HashMap<String, Timer>();
        this.load();
    }

    @Override
    public void start(MojoExecution mojo) {
        Timer timer = mojoTimes.get(getMojoKey(mojo));
        if (timer == null) {
            timer = new DefaultTimer(0);
            mojoTimes.put(getMojoKey(mojo), timer);
        }
        timer.start();
    }

    @Override
    public void finish(MojoExecution mojo) {
        Timer timer = mojoTimes.get(getMojoKey(mojo));
        timer.stop();
        p.setProperty(getMojoKey(mojo), String.valueOf(timer.getTime()));
    }

    @Override
    public void render() {
        boolean logActive = System.getProperty("maven.timing") != null;
        try {
            save();
        } catch (IOException e) {
            logger.error("[TIMING] Can not save the timing file", e);
        }
        try (FileOutputStream renderFile = new FileOutputStream(getRenderFile())) {
            int max = 0;
            for(Map.Entry<String, Timer> entry : mojoTimes.entrySet()) {
                int length = entry.getKey().length();
                if (length > max) {
                    max = length;
                }
            }
            StringBuilder line = new StringBuilder();
            if (logActive) {
                logger.info("------------------------------------------------------------------------");
            }
            line.append("MOJO");
            for (int i = 0; i < max - 1; i++) {
                line.append(" ");
            }
            line.append("TIMING");
            if (logActive) {
                logger.info(line.toString());
            }
            line.append("\n\n"); // Only for file
            renderFile.write(line.toString().getBytes());

            for(Map.Entry<String, Timer> entry : mojoTimes.entrySet()) {
                line = new StringBuilder();
                line.append(entry.getKey().replace("--", ":"));
                int keyLength = entry.getKey().length();
                while (keyLength < max) {
                    line.append(" ");
                    keyLength++;
                }
                line.append("    "); // 4 spaces
                long time = entry.getValue().getTime();
                line.append(DefaultTimer.format(time));
                if (logActive) {
                    logger.info(line.toString());
                }
                line.append("\n"); // only for file
                renderFile.write(line.toString().getBytes());

            }
        } catch (IOException e) {
            logger.error("[TIMING] Can not save the timing render file", e);
        }

    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    private synchronized void save() throws IOException {
        try (FileOutputStream stream = new FileOutputStream(getTimingFile())) {
            p.store(stream, "Mojo timing information");
        }
    }

    private synchronized void load() {
        p = new Properties();
        try (FileInputStream inputStream = new FileInputStream(getTimingFile())) {
            p.load(inputStream);
            Enumeration<?> props = p.propertyNames();
            while (props.hasMoreElements()) {
                String key = (String) props.nextElement();
                String value = p.getProperty(key);
                mojoTimes.put(key, new DefaultTimer(Long.parseLong(value)));
            }
        } catch (IOException e) {
            logger.error("[TIMING] Can not open the timing file", e);
        }
    }

    private File getTimingFile() throws IOException {
        String userHome = System.getProperty("user.home");
        File file = new File(userHome + "/.m2/timing");
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    private File getRenderFile() throws IOException {
        String userHome = System.getProperty("user.home");
        File file = new File(userHome + "/.m2/timing-render");
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    private String getMojoKey(MojoExecution mojoEx) {
        return mojoEx.getArtifactId() + "--" + mojoEx.getGoal();
    }
}
