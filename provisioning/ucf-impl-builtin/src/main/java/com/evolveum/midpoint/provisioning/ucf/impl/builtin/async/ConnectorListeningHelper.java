/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async;

import com.evolveum.midpoint.provisioning.ucf.api.ListeningActivity;
import com.evolveum.midpoint.provisioning.ucf.api.async.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Helper for listening on both active and passive sources.
 */
class ConnectorListeningHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorListeningHelper.class);

    private static final long RUNNABLE_CHECK_INTERVAL = 100L;

    @NotNull private final AsyncUpdateConnectorInstance connectorInstance;

    // do not forget to synchronize on these two lists
    @NotNull private final List<ListeningActivity> activities = new ArrayList<>();
    @NotNull private final List<PassiveAsyncUpdateSource> passiveSources = new ArrayList<>();

    @NotNull private final ChangeListener changeListener;
    private AsyncUpdateMessageListener messageListener;

    @Nullable private final Authentication authentication;

    private boolean stopped;

    ConnectorListeningHelper(@NotNull AsyncUpdateConnectorInstance connectorInstance,
            @NotNull ChangeListener changeListener,
            @Nullable Authentication authentication) {
        this.connectorInstance = connectorInstance;
        this.changeListener = changeListener;
        this.authentication = authentication;
    }

    void listenForChanges(ConnectorConfiguration configuration, Supplier<Boolean> canRunSupplier) throws SchemaException {

        start(configuration);
        try {
            main:
            while (canContinue(canRunSupplier)) {

                // passive sources
                boolean anyMessage = false;
                boolean anySourceOpen = false;
                for (PassiveAsyncUpdateSource passiveSource : getPassiveSourcesCopy()) {
                    if (passiveSource.isOpen()) {
                        anySourceOpen = true;
                        if (passiveSource.getNextUpdate(messageListener)) {
                            anyMessage = true;
                        }
                    }
                    if (!canContinue(canRunSupplier)) {
                        break main;
                    }
                }

                // active sources are processed asynchronously; here we only check if their listening activities are alive
                for (ListeningActivity listeningActivity : getActivitiesCopy()) {
                    if (listeningActivity.isAlive()) {
                        anySourceOpen = true;
                    }
                }

                if (!anySourceOpen) {
                    LOGGER.info("All message sources are closed now, stopping the listening in {}", connectorInstance);
                    break;
                }

                if (!anyMessage) {
                    Thread.sleep(RUNNABLE_CHECK_INTERVAL);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.debug("Got InterruptedException", e);
            LOGGER.info("Listening was interrupted");
        } finally {
            stop();
        }
    }

    private boolean canContinue(Supplier<Boolean> canRunSupplier) {
        if (!Boolean.TRUE.equals(canRunSupplier.get())) {
            LOGGER.info("Stopping listening for changes because canRun is not true any more");
            return false;
        } else if (stopped) {
            LOGGER.warn("Stopping listening for changes as requested to do so (maybe because another one was started in the meanwhile)");
            return false;
        } else {
            return true;
        }
    }

    private void start(ConnectorConfiguration configuration) throws SchemaException {
        LOGGER.info("Starting listening in {}", connectorInstance);  // todo debug

        this.messageListener = new TransformationalAsyncUpdateMessageListener(
                changeListener, authentication, connectorInstance, connectorInstance.getTracer(), connectorInstance.getTaskManager());
        Collection<AsyncUpdateSource> sources = connectorInstance.getSourceManager().createSources(configuration.getAllSources());
        try {
            for (AsyncUpdateSource source : sources) {
                if (source instanceof ActiveAsyncUpdateSource) {
                    addActivity(((ActiveAsyncUpdateSource) source).startListening(messageListener));
                } else if (source instanceof PassiveAsyncUpdateSource) {
                    addPassiveSource((PassiveAsyncUpdateSource) source);
                } else {
                    throw new IllegalStateException("Unsupported source: " + source);
                }
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Got unexpected exception while starting the listener(s). "
                    + "Stopping the listening. In: {}", t, connectorInstance);
            stop();
            throw t;
        }
    }

    private void addActivity(ListeningActivity activity) {
        synchronized (activities) {
            activities.add(activity);
        }
    }

    private void addPassiveSource(PassiveAsyncUpdateSource source) {
        synchronized (passiveSources) {
            passiveSources.add(source);
        }
    }

    public void stop() {
        stopInternal(true);
    }

    private void stopInternal(boolean permanently) {
        LOGGER.info("Stopping listening in {} (permanently={})", connectorInstance, permanently);    // todo debug

        if (permanently) {
            stopped = true;
        }

        List<ListeningActivity> activitiesCopy;
        synchronized (activities) {
            activitiesCopy = new ArrayList<>(activities);
            activities.clear();
        }
        for (ListeningActivity activity : activitiesCopy) {
            try {
                activity.stop();
            } catch (RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't stop listening on {} in {}", e, activity, connectorInstance);
            }
        }
        synchronized (passiveSources) {
            passiveSources.clear();
        }
    }

    void restart(ConnectorConfiguration configuration) throws SchemaException {
        stopInternal(false);
        start(configuration);
    }

    @Override
    public String toString() {
        return "ConnectorListeningState{" + activities + "}";
    }

    @NotNull
    private List<PassiveAsyncUpdateSource> getPassiveSourcesCopy() {
        synchronized (passiveSources) {
            return new ArrayList<>(passiveSources);
        }
    }

    @NotNull
    private List<ListeningActivity> getActivitiesCopy() {
        synchronized (activities) {
            return new ArrayList<>(activities);
        }
    }
}
