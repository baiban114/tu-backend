package com.tu.backend.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PageIndexFlushScheduler {

    private static final Logger log = LoggerFactory.getLogger(PageIndexFlushScheduler.class);

    private final PageIndexCoordinator pageIndexCoordinator;

    public PageIndexFlushScheduler(PageIndexCoordinator pageIndexCoordinator) {
        this.pageIndexCoordinator = pageIndexCoordinator;
    }

    @Scheduled(fixedDelayString = "${index.flush-interval-ms:300000}")
    public void flushDirtyPages() {
        pageIndexCoordinator.flushAllDirty();
        log.debug("Scheduled flush of dirty page indexes completed");
    }
}
