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
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
            for(Entry<String, Timer> entry : mojoTimes.entrySet()) {
                int length = entry.getKey().length();
                if (length > max) {
                    max = length;
                }
            }
            StringBuilder line = new StringBuilder();
            line.append("Collecting data since: ");
            Date date = new Date(Long.parseLong((String) p.get("start.time")));
            line.append(date);
            line.append("\n");
            line.append("------------------------------------------------------------------------\n");
            renderFile.write(line.toString().getBytes());
            if (logActive) {
                logger.info("------------------------------------------------------------------------");
            }
            line = new StringBuilder();
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

            List<Entry<String, Timer>> ordered = getOrderedList(mojoTimes);
            for(Entry<String, Timer> entry : ordered) {
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

    private List<Entry<String,Timer>> getOrderedList(Map<String, Timer> mojoTimes) {
        // Create something orderable
        Entry<String, Timer>[] entries = new Entry[mojoTimes.size()];
        int i = 0;
        for (Entry<String, Timer> entry : mojoTimes.entrySet()) {
            entries[i] = entry;
            i++;
        }
        // re-order
        for (i = 0; i < entries.length; i++) {
            for (int j = i + 1; j < entries.length; j++) {
                if (entries[i].getValue().getTime() < entries[j].getValue().getTime()) {
                    Entry<String, Timer> temp = entries[j];
                    entries[j] = entries[i];
                    entries[i] = temp;
                }
            }
        }
        return Arrays.asList(entries);
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
            setStartTime(p);
            Enumeration<?> props = p.propertyNames();
            while (props.hasMoreElements()) {
                String key = (String) props.nextElement();
                String value = p.getProperty(key);
                if ("start.time".equals(key)) {
                    continue;
                }
                mojoTimes.put(key, new DefaultTimer(Long.parseLong(value)));
            }
        } catch (IOException e) {
            logger.error("[TIMING] Can not open the timing file", e);
        }
    }

    private void setStartTime(Properties p) {
        if (p.get("start.time") == null) {
            p.setProperty("start.time", String.valueOf(System.currentTimeMillis()));
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
