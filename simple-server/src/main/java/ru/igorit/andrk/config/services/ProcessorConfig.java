package ru.igorit.andrk.config.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.igorit.andrk.service.DataProcessor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class ProcessorConfig {

    private List<DataProcessor> processors;

    private Map<String,DataProcessor> processorMap;

    public ProcessorConfig(List<DataProcessor> processors) {
        this.processors = processors;
        processorMap = processors.stream().collect(Collectors.toMap(DataProcessor::document, p->p));
    }

    @Bean(name = "processorMap")
    public Map<String, DataProcessor> getProcessorMap() {
        return processorMap;
    }
}
