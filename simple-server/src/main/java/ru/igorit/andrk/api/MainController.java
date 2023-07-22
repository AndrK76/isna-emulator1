package ru.igorit.andrk.api;

import lombok.extern.log4j.Log4j2;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.igorit.andrk.service.processor.ProcessorFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static ru.igorit.andrk.config.services.Constants.*;

@Log4j2
@Controller
public class MainController implements ErrorController {

    private static Map<String, String> listModes = new HashMap<>();
    private static Map<String, String> manageModes = new HashMap<>();
    private static Map<String, String> serviceNames = new HashMap<>();

    static {
        listModes.put("general", "Все запросы");
        listModes.put("open-close", "Уведомления об открытии, закрытии и изменении счетов");
        manageModes.put("open-close", "Уведомления об открытии, закрытии и изменении счетов");
        serviceNames.put("open-close", OPEN_CLOSE_SERVICE);
    }

    private final int docPerPage;
    private final ProcessorFactory processorFactory;

    public MainController(
            @Value("${api.doc_per_page}") int docPerPage, ProcessorFactory processorFactory) {
        this.docPerPage = docPerPage;
        this.processorFactory = processorFactory;
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) throws MalformedURLException {
        URL url = new URL(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString());
        String host = url.getHost();
        Integer port = url.getPort();
        String protocol = url.getProtocol();

        model.addAttribute("restApiVersion", API_VERSION);
        model.addAttribute("openCloseWsdl", "/" + SERVICE_PATH + "/" + OPEN_CLOSE_WSDL);
        var openCloseAddress = new URL(protocol, host, port, "/" + SERVICE_PATH + "/" + SEND_MESSAGE).toString();
        model.addAttribute("openCloseAddress", openCloseAddress);
        return "index";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        HttpStatus statusCode = HttpStatus.resolve(Integer.parseInt(status.toString()));


        model.addAttribute("code", status.toString());
        model.addAttribute("name", statusCode.getReasonPhrase());
        return "error";
    }

    @RequestMapping("/error404")
    public String handleError(Model model) {

        HttpStatus statusCode = HttpStatus.resolve(404);
        model.addAttribute("code", "404");
        model.addAttribute("name", statusCode.getReasonPhrase());
        return "error";
    }

    @GetMapping("/list")
    public String showStatistics(
            @RequestParam(name = "mode", required = false, defaultValue = "general") String mode,
            Model model) {
        model.addAttribute("mode", mode);
        model.addAttribute("modeName", listModes.getOrDefault(mode, "Не определено"));
        model.addAttribute("restApiVersion", API_VERSION);
        model.addAttribute("perPage", docPerPage);
        return "list";
    }

    @GetMapping("/manage")
    public String manage(
            @RequestParam(name = "mode", required = false, defaultValue = "open-close") String mode,
            Model model) {
        model.addAttribute("mode", mode);
        model.addAttribute("modeName", manageModes.getOrDefault(mode, "Не определено"));
        model.addAttribute("restApiVersion", API_VERSION);
        model.addAttribute("serviceName", serviceNames.getOrDefault(mode, "Unknown"));
        return "manage";
    }

    @GetMapping("/config/{config}")
    public void downloadConfig(
            @PathVariable(name = "config") String configName,
            HttpServletResponse response) throws IOException {
        String serviceName = serviceNames.getOrDefault(configName, null);
        if (serviceName == null) {
            response.setStatus(404);
        }
        byte[] data = processorFactory.getProcCfg(serviceName);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + serviceName + ".cfg\"");
        InputStream dataStream = new ByteArrayInputStream(data);
        dataStream.transferTo(response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/api")
    public String swagger(Model model) {
        return "api";
    }
}
