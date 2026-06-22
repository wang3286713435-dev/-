package com.zhuoyu.delivery.datasteward.storage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StorageObjectificationQueueWorker {

    private static final Logger log = LoggerFactory.getLogger(StorageObjectificationQueueWorker.class);
    private static final int POLL_INTERVAL_SECONDS = 3;

    private final StorageMigrationApplicationService storageMigrationApplicationService;
    private ScheduledExecutorService scheduler;

    public StorageObjectificationQueueWorker(StorageMigrationApplicationService storageMigrationApplicationService) {
        this.storageMigrationApplicationService = storageMigrationApplicationService;
    }

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "storage-objectification-queue-worker");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::pollSafely, 5, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Storage objectification queue worker started (poll interval: {}s)", POLL_INTERVAL_SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("Storage objectification queue worker stopped");
        }
    }

    void pollSafely() {
        try {
            storageMigrationApplicationService.pollObjectificationQueue();
        } catch (Exception exception) {
            log.warn("Storage objectification queue poll failed: {}", exception.getMessage());
        }
    }
}
