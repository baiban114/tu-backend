package com.tu.backend.search;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SearchSchemaInitializer implements ApplicationRunner {

    private final SearchIndexService searchIndexService;

    public SearchSchemaInitializer(SearchIndexService searchIndexService) {
        this.searchIndexService = searchIndexService;
    }

    @Override
    public void run(ApplicationArguments args) {
        searchIndexService.ensureIndexBestEffort();
    }
}
