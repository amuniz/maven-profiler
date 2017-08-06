/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.lifecycle.profiler.internal;

import io.tesla.lifecycle.profiler.Timer;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DefaultTimer implements Timer {

    public static final int MS_PER_SEC = 1000;
    public static final int SEC_PER_MIN = 60;
    private long start;
    private long accumulated;

    public DefaultTimer() {
        accumulated = 0;
    }

    public DefaultTimer(long accumulated) {
        this.accumulated = accumulated;
    }

    public void stop() {
        accumulated += elapsedTime();
    }

    public long getTime() {
        return accumulated;
    }

    public void start() {
        start = System.currentTimeMillis();
    }

    private long elapsedTime() {
        return System.currentTimeMillis() - start;
    }

    public static String format(long ms) {
        long secs = ms / MS_PER_SEC;
        long mins = secs / SEC_PER_MIN;
        long hours = mins / 60;
        secs = secs % SEC_PER_MIN;
        mins = mins % 60;
        long fractionOfASecond = ms - (secs * 1000);

        StringBuilder msg = new StringBuilder();
        if (hours > 0) {
            msg.append(hours);
            msg.append("h ");
        }
        if (mins > 0) {
            msg.append(mins);
            msg.append("m ");
        }
        if (secs > 0 && hours == 0) {
            msg.append(secs);
            msg.append("s");
        }

        if (secs == 0 && mins == 0 && hours == 0) {
            if (msg.length() > 0)
                msg.append(" ");
            msg.append(fractionOfASecond);
            msg.append("ms");
        }

        return msg.toString();
    }
}
