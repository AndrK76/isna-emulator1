package ru.igorit.andrk.config.soap;

import kz.icode.gov.integration.kgd.ErrorInfo;
import kz.icode.gov.integration.kgd.ObjectFactory;
import lombok.extern.log4j.Log4j2;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapFaultDetail;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;
import ru.igorit.andrk.config.services.ServiceFaultException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.time.LocalDateTime;

import static ru.igorit.andrk.utils.DataHandler.toXmlDate;

@Log4j2
public class DetailSoapFaultDefinitionExceptionResolver extends SoapFaultMappingExceptionResolver {

    private static final ObjectFactory FACTORY = new ObjectFactory();
    private final Marshaller marshaller;
    private static final QName CODE = new QName("errorCode");
    private static final QName MESSAGE = new QName("errorMessage");

    public DetailSoapFaultDefinitionExceptionResolver() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ErrorInfo.class);
        marshaller = jaxbContext.createMarshaller();
    }

    @Override
    protected void customizeFault(Object endpoint, Exception ex, SoapFault fault) {
        log.warn("err processed {}", ex.getMessage());
        if (ex instanceof ServiceFaultException) {
            var errorInfo = ((ServiceFaultException) ex).getErrorInfo();
            if (errorInfo.getErrorDate() == null){
                errorInfo.setErrorDate(toXmlDate(LocalDateTime.now()));
            }

            SoapFaultDetail detail = fault.addFaultDetail();
            try{
                marshaller.marshal(FACTORY.createSendMessageFault1SendMessageFault(errorInfo),detail.getResult());
            } catch (JAXBException e) {
                log.error("Could not marshall SubscriptionManagementFault", e);
            }
        }
        super.customizeFault(endpoint, ex, fault);
    }
}
