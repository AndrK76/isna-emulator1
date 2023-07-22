package ru.igorit.andrk.service.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import ru.igorit.andrk.service.DataProcessor;

import java.util.Map;

@Service
public class ProcessorFactoryImpl implements ProcessorFactory{
    private final Map<String, DataProcessor> processors;
    private static final String EXTERNAL_CFG = "external";
    private static final String INTERNAL_CFG = "internal";

    private static final Logger log = LoggerFactory.getLogger(ProcessorFactory.class);


    public ProcessorFactoryImpl(
            @Autowired @Qualifier("processorMap") Map<String, DataProcessor> processors) {
        this.processors = processors;
        for (var proc : processors.keySet()) {
            var cfg = getProcCfg(proc);
            if (cfg != null) {
                processors.get(proc).configure(cfg);
            }
        }
    }

    public DataProcessor getProcessor(String document) {
        return processors.get(document);
    }

    public byte[] getProcCfg(String procKey) {
        byte[] config = null;
        String[] cfgTypes = new String[]{EXTERNAL_CFG, INTERNAL_CFG};
        for (int i = 0; i < cfgTypes.length && config == null; i++) {
            try {
                var res = getResource(procKey, cfgTypes[i]);
                if (res != null){
                    config = FileCopyUtils.copyToByteArray(res.getInputStream());
                    log.debug("Found {} config for processor {}",cfgTypes[i],procKey);
                }
            } catch (Exception e) {
                log.trace("Not found {} config for processor {}",cfgTypes[i],procKey);
            }
        }
        return config;
    }

    private Resource getResource(String procName, String typeRes) {
        if (typeRes.equals(INTERNAL_CFG)) {
            return new ClassPathResource(String.format("proc_cfg/%s.cfg", procName));
        } else if (typeRes.equals(EXTERNAL_CFG)) {
            return new FileSystemResource(String.format("proc_cfg/%s.cfg", procName));
        } else {
            return null;
        }
    }

}
