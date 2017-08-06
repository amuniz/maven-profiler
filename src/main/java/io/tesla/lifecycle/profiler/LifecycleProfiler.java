/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.lifecycle.profiler;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

// pom deserialization
// app dependency download
// plugin dependency download
// mojo execution

/**
 * @author Jason van Zyl
 * @author Antonio Muñiz
 */
@Named
@Singleton
@Component(role = EventSpy.class)
public class LifecycleProfiler extends AbstractEventSpy {

    //
    // Components
    //
    private SessionProfileRenderer profiler;

    @Requirement
    private Logger logger;


    @Inject
    public LifecycleProfiler(SessionProfileRenderer profiler) {
        this.profiler = profiler;
    }

    @Override
    public void init(Context context) throws Exception {
        profiler.setLogger(logger);
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;
            if (executionEvent.getType() == ExecutionEvent.Type.MojoStarted) {
                String phase = executionEvent.getMojoExecution().getLifecyclePhase();
                profiler.start(executionEvent.getMojoExecution());
            } else if (executionEvent.getType() == ExecutionEvent.Type.MojoSucceeded || executionEvent.getType() == ExecutionEvent.Type.MojoFailed) {
                profiler.finish(executionEvent.getMojoExecution());
            } else if (executionEvent.getType() == ExecutionEvent.Type.SessionEnded) {
                profiler.render();
            }
        }
    }
}
