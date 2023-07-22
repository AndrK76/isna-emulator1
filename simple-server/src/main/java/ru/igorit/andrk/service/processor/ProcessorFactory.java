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

public interface ProcessorFactory {

    DataProcessor getProcessor(String document);

    byte[] getProcCfg(String procKey);

}
