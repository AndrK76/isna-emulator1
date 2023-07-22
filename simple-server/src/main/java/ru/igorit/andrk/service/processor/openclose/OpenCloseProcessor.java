package ru.igorit.andrk.service.processor.openclose;

import kz.icode.gov.integration.kgd.ErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.igorit.andrk.config.services.ConfigFormatException;
import ru.igorit.andrk.config.services.Constants;
import ru.igorit.andrk.model.*;
import ru.igorit.andrk.mt.structure.*;
import ru.igorit.andrk.mt.utils.MtComposer;
import ru.igorit.andrk.mt.utils.MtConfigParser;
import ru.igorit.andrk.mt.utils.MtParser;
import ru.igorit.andrk.service.DataProcessor;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.service.processor.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenCloseProcessor implements DataProcessor {
    private static final Logger log = LoggerFactory.getLogger(OpenCloseProcessor.class);
    private static final String DOCUMENT = Constants.OPEN_CLOSE_SERVICE;
    private final MtFormat inputFormat = new MtFormat();
    private final MtFormat outputFormat = new MtFormat();
    private final MainStoreService mainStoreService;
    private Map<String, OpenCloseResult> results = null;

    public OpenCloseProcessor(MainStoreService mainStoreService) {
        this.mainStoreService = mainStoreService;
    }

    @Override
    public String document() {
        return DOCUMENT;
    }

    @Override
    public ProcessResult process(Request request, UUID messageId) {
        try {

            OpenCloseDynamicSettings dynSettings = OpenCloseDynamicSettings.create(mainStoreService.getSettingsByGroup(this.document()));

            String data = request.getData();
            MtContent inputContent, outputContent;
            if (inputFormat.getNodes().size() == 0) {
                throw processError(new ConfigFormatException("Пустая конфигурация сервиса"), "SVC_CONFIG_ERROR");
            }

            try {
                inputContent = MtParser.parsePreview(data, inputFormat);
                validateContentOnFatalErrors(inputContent);

                var codeForm = (String) inputContent.getValue("code_form");
                if (codeForm.equals("A03")) {
                    inputContent.getNode("ACCOUNT").setCurrentCode("ACCOUNT_CHANGE");
                }
                MtParser.parseFinal(inputContent, inputFormat);
                log.debug(inputContent.dumpValues());
            } catch (DataFormatFatalException e) {
                throw processError(e, "SVC_DATAFORMAT_ERROR");
            }

            OpenCloseRequest ocRequest;
            Map<Integer, OpenCloseResult> accountResult;
            try {
                ocRequest = makeRequestEntity(request, inputContent);
                accountResult = makeRequestAccounts(ocRequest, inputContent, dynSettings);
            } catch (Exception e) {
                throw processError(e, "SVC_DATAFORMAT_ERROR");
            }
            ocRequest = mainStoreService.saveOpenCloseRequest(ocRequest);


            OpenCloseResponse ocResponse;
            try {
                outputContent = new MtContent(outputFormat);
                ocResponse = makeResponse(outputContent, outputFormat, ocRequest, accountResult, dynSettings);
            } catch (Exception e) {
                throw processError(e, "SVC_DATACOMPOSE_ERROR");
            }
            mainStoreService.saveOpenCloseResponse(ocResponse);

            var successResult = ProcessResult.successResult();
            successResult.setData(outputContent.getRawData());

            return successResult;
        } catch (ProcessorException e) {
            if (e.getErrorInfo().getErrorCode().equals("SVC_DATAFORMAT_ERROR")) {
                return ProcessResult.errorResult();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void configure(byte[] config) {
        log.debug("apply config");
        MtConfigParser.parseInputFormatFromXML(config, inputFormat);
        MtConfigParser.parseOutputFormatFromXML(config, outputFormat);

        log.trace("Input Config: {}", inputFormat);
        results = initResultValues(config);
        log.trace("Results: {}", results);
    }

    public Map<String, OpenCloseResult> initResultValues(byte[] config) {
        Map<String, OpenCloseResult> resList = new HashMap<>();
        NodeList resCfg = MtConfigParser.getCustomSection(config, "results");
        if (resCfg.getLength() != 0) {
            var childNodes = resCfg.item(0).getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                var node = childNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                var attributes = node.getAttributes();
                Map<String, String> attrs = new HashMap<>();
                for (int j = 0; j < attributes.getLength(); j++) {
                    attrs.put(attributes.item(j).getNodeName(), attributes.item(j).getNodeValue());
                }
                if (attrs.containsKey("id")) {
                    var res = new OpenCloseResult(attrs.get("id"),
                            attrs.getOrDefault("code", ""));
                    if (node.getChildNodes().getLength() > 0) {
                        List<Node> valueNodes = new ArrayList<>();
                        for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                            valueNodes.add(node.getChildNodes().item(j));
                        }
                        Optional<Node> valNode = valueNodes.stream()
                                .filter(r -> r.getNodeType() == Node.CDATA_SECTION_NODE).findFirst();
                        if (valNode.isEmpty()) {
                            valNode = Optional.of(valueNodes.get(0));
                        }
                        res.setText(valNode.get().getNodeValue());
                    }
                    resList.put(res.getId(), res);
                }
            }
        }
        return resList;
    }

    private ProcessorException processError(Exception e, String code) {
        ErrorInfo info = new ErrorInfo();
        info.setErrorCode(code);
        info.setErrorMessage(e.getMessage());
        return new ProcessorException(info, e);
    }

    private void validateContentOnFatalErrors(MtContent content) {
        String[] requiredItems = new String[]{"reference", "code_form", "notify_date"};
        if (!Arrays.stream(requiredItems).map(content::getValue).allMatch(Objects::nonNull)) {
            String reqToStr = String.join(", ", requiredItems);
            throw new DataFormatFatalException("Не найдены поля определяющие формат: " + reqToStr);
        }
        String[] validCodeForms = new String[]{"A01", "A03"};
        var codeForm = (String) content.getValue("code_form");
        if (Arrays.stream(validCodeForms).noneMatch(codeForm::equals)) {
            throw new DataFormatFatalException("Некорректная форма: " + codeForm);
        }
    }

    private OpenCloseRequest makeRequestEntity(Request request, MtContent content) {
        var ret = new OpenCloseRequest(request);
        ret.setReference((String) content.getValue("reference"));
        ret.setCodeForm((String) content.getValue("code_form"));
        ret.setNotifyDate((LocalDateTime) content.getValue("notify_date"));
        return ret;
    }

    private Map<Integer, OpenCloseResult> makeRequestAccounts(
            OpenCloseRequest request,
            MtContent content,
            OpenCloseDynamicSettings dynSettings) {
        Map<Integer, OpenCloseResult> parseDataResult = new HashMap<>();
        try {
            var accountBlocks = content.getBlocks().stream()
                    .filter(r -> r.getOwnerNode().getFormat().getNodeName().equals("ACCOUNT"))
                    .sorted(Comparator.comparing(MtBlock::getId))
                    .collect(Collectors.toList());
            for (var block : accountBlocks) {
                Set<String> emptyItems = new HashSet<>();
                var id = block.getId();
                OpenCloseRequestAccount account = new OpenCloseRequestAccount();
                request.getAccounts().add(account);
                account.setRequest(request);
                account.setSort(id);
                account.setAccount((String) getBlockValue("account", content, inputFormat, emptyItems, id));
                if (request.getCodeForm().equals("A01")) {
                    account.setOperType((Integer) getBlockValue("oper_type", content, inputFormat, emptyItems, id));
                } else {
                    account.setOperType(9);
                }
                account.setBic((String) getBlockValue("bic", content, inputFormat, emptyItems, id));
                account.setAccountType((String) getBlockValue("account_type", content, inputFormat, emptyItems, id));
                account.setOperDate((LocalDateTime) getBlockValue("oper_date", content, inputFormat, emptyItems, id));
                account.setRnn((String) getBlockValue("rnn", content, inputFormat, emptyItems, id));
                account.setDog((String) getBlockValue("dog", content, inputFormat, emptyItems, id));
                account.setBicOld((String) getBlockValue("bic_old", content, inputFormat, emptyItems, id));
                account.setAccountOld((String) getBlockValue("acc_old", content, inputFormat, emptyItems, id));
                account.setDateModify((LocalDateTime) getBlockValue("acc_date_ch", content, inputFormat, emptyItems, id));

                OpenCloseResult parseRowResult;
                if (dynSettings.isRaiseTestError()) {
                    parseRowResult = new OpenCloseResult(results.get("TEST_ERROR"));
                } else {
                    parseRowResult = checkMainConditions(request, dynSettings, account, emptyItems);
                    if (parseRowResult.getId().equals("SUCCESS")
                            && (dynSettings.isValidateAccountState() || dynSettings.isValidateOperationDate())) {
                        parseRowResult = checkDopConditions(dynSettings, account);
                    }
                }
                parseDataResult.put(id, parseRowResult);
            }

            return parseDataResult;
        } catch (Exception e) {
            if (e instanceof DataContentException) {
                throw e;
            } else {
                throw new DataContentException(e.getMessage(), e);
            }
        }
    }

    private Object getBlockValue(String itemName,
                                 MtContent content,
                                 MtFormat format,
                                 Set<String> emptyItems,
                                 int id) {
        if (!content.checkOnEmpty(itemName, id)) {
            if (format.getItem("dog") != null) {
                emptyItems.add(itemName);
            }
            return null;
        }
        try {
            return content.getValue(itemName, id);
        } catch (RuntimeException e) {
            if (content.getItems().get(itemName).isRequired()) {
                emptyItems.add(itemName);
            }
            return null;
        }
    }

    //TODO: захардкожено, можно тоже в настройки вынести секцией
    private boolean checkTypeOper(OpenCloseRequestAccount accountInfo) {
        String codeForm = accountInfo.getRequest().getCodeForm();
        int operType = accountInfo.getOperType();
        return (codeForm.equals("A01") && (operType == 1 || operType == 2))
                || (codeForm.equals("A03") && operType == 9);
    }

    //TODO: захардкожено, можно тоже в настройки вынести секцией
    private boolean checkTypeAccount(OpenCloseRequestAccount accountInfo) {
        String accType = accountInfo.getAccountType();
        var validTypes = new String[]{"00", "05", "09", "20"};
        return Arrays.asList(validTypes).contains(accType);
    }

    //TODO: автотесты не стал делать - обошёлся ручным
    // для реальной системы ошибки могут быть другими
    // проверки только из предполагаемого поведения эмулируемого сервиса
    private OpenCloseResult checkMainConditions(
            OpenCloseRequest request,
            OpenCloseDynamicSettings dynSettings,
            OpenCloseRequestAccount account,
            Set<String> emptyItems) {
        OpenCloseResult parseRowResult = new OpenCloseResult(results.get("SUCCESS"));
        if (dynSettings.isCheckUniqueMessageId() && mainStoreService.containRequestWithMessageId(request.getRawRequest())) {
            parseRowResult = new OpenCloseResult(results.get("DUPLICATE_MSGID"));
            parseRowResult.setText(
                    parseRowResult.getText().replace("%%{MESSAGE_ID}%%",
                            request.getRawRequest().getMessageId().toString()));
        } else if (dynSettings.isCheckUniqueReference() && mainStoreService.containOpenCloseRequestWithReference(request)) {
            parseRowResult = new OpenCloseResult(results.get("DUPLICATE_REFERENCE"));
            parseRowResult.setText(
                    parseRowResult.getText().replace("%%{REFERENCE}%%",
                            request.getReference()));
        } else if (emptyItems.size() > 0) {
            parseRowResult = new OpenCloseResult(results.get("EMPTY_FIELD"));
            parseRowResult.setText(
                    parseRowResult.getText().replace("%%{FIELD}%%",
                            String.join(", ", emptyItems)));
        } else if (!checkTypeOper(account)) {
            parseRowResult = new OpenCloseResult(results.get("INVALID_OPER_TYPE"));
            parseRowResult.setText(
                    parseRowResult.getText().replace("%%{TYPE_OPER}%%",
                            account.getOperType().toString()));
            parseRowResult.setText(
                    parseRowResult.getText().replace("%%{CODE_FORM}%%",
                            account.getRequest().getCodeForm()));
        } else if (!checkTypeAccount(account)) {
            parseRowResult = new OpenCloseResult(results.get("INVALID_ACC_TYPE"));
            parseRowResult.setText(
                    parseRowResult.getText().replace("%%{ACC_TYPE}%%",
                            account.getAccountType()));
        }
        return parseRowResult;
    }

    //TODO: Tag=NoTested
    // для реального приложения здесь обязательно нужны автотесты,
    // но в рамках эмулятора ошибка здесь не смертельна, делать не стал
    // Сделано просто для эмуляции осмысленного сообщения об ошибке
    // Поведение приложения для которого делается не зависит от полученного кода ошибки
    // и все они зависят от содержимого базы эмулятора

    private OpenCloseResult checkDopConditions(
            OpenCloseDynamicSettings dynSettings,
            OpenCloseRequestAccount account) {

        var lastOpen = mainStoreService.lastResponseForAccountByOperTypeAndResult(
                account.getAccount(), 1, "01");
        var lastClose = mainStoreService.lastResponseForAccountByOperTypeAndResult(
                account.getAccount(), 2, "01");
        var lastChange = mainStoreService.lastResponseForAccountByOperTypeAndResult(
                account.getAccount(), 9, "01");
        LocalDateTime lastOpenDate = (lastOpen == null ? null : lastOpen.getOperDate());
        if (lastChange != null && (lastOpen == null || lastChange.getOperDate().compareTo(lastOpenDate) > 0)) {
            lastOpenDate = lastChange.getOperDate();
        }
        LocalDateTime lastCloseDate = (lastClose == null ? null : lastClose.getOperDate());

        OpenCloseResult parseRowResult = new OpenCloseResult(results.get("SUCCESS"));
        if (dynSettings.isValidateAccountState()) {
            parseRowResult = checkOnAccountState(account, lastOpenDate, lastCloseDate);
        }
        if (dynSettings.isValidateOperationDate() && parseRowResult.getId().equals("SUCCESS")) {
            parseRowResult = checkOnOperationDate(account, lastOpenDate, lastCloseDate);
        }
        return parseRowResult;

    }

    //TODO: Tag=NoTested
    private OpenCloseResult checkOnAccountState(
            OpenCloseRequestAccount account,
            LocalDateTime lastOpenDate,
            LocalDateTime lastCloseDate
    ) {
        OpenCloseResult parseRowResult = new OpenCloseResult(results.get("SUCCESS"));
        if (account.getOperType() == 1) {
            if (lastOpenDate != null && (lastCloseDate == null || lastOpenDate.compareTo(lastCloseDate) >= 0)) {
                parseRowResult = new OpenCloseResult(results.get("EXIST_ACC"));
                parseRowResult.setText(
                        parseRowResult.getText().replace("%%{ACCOUNT}%%", account.getAccount()));
            } else if (lastOpenDate != null && lastOpenDate.compareTo(account.getOperDate()) > 0 ||
                    lastCloseDate != null && lastCloseDate.compareTo(account.getOperDate()) > 0) {
                parseRowResult = new OpenCloseResult(results.get("INVALID_DATE"));
                parseRowResult.setText(
                        parseRowResult.getText().replace("%%{ACCOUNT}%%", account.getAccount()));
            }
        } else if (account.getOperType() == 2) {
            if (lastOpenDate == null) {
                parseRowResult = new OpenCloseResult(results.get("NOEXIST_ACC"));
            } else if (
                    lastCloseDate != null && (lastOpenDate == null || lastOpenDate.compareTo(lastCloseDate) < 0)
                            && lastCloseDate.compareTo(account.getOperDate()) > 0) {
                parseRowResult = new OpenCloseResult(results.get("CLOSED_ACC"));
                parseRowResult.setText(
                        parseRowResult.getText().replace("%%{CLOSE_DATE}%%", account.getOperDate().toString()));
            }
        } else if (account.getOperType() == 9) {
            if (lastOpenDate != null && lastCloseDate == null) {
                parseRowResult = new OpenCloseResult(results.get("EXIST_ACC"));
                parseRowResult.setText(
                        parseRowResult.getText().replace("%%{ACCOUNT}%%", account.getAccount()));
            } else if (lastCloseDate != null) {
                parseRowResult = new OpenCloseResult(results.get("CLOSED_ACC"));
                parseRowResult.setText(
                        parseRowResult.getText().replace("%%{CLOSE_DATE}%%", account.getOperDate().toString()));
            }
        }
        return parseRowResult;
    }

    //TODO: Tag=NoTested
    private OpenCloseResult checkOnOperationDate(
            OpenCloseRequestAccount account,
            LocalDateTime lastOpenDate,
            LocalDateTime lastCloseDate
    ) {
        OpenCloseResult parseRowResult = new OpenCloseResult(results.get("SUCCESS"));
        if (account.getOperType() == 2) {
            if (lastOpenDate == null) {
                parseRowResult = new OpenCloseResult(results.get("NOEXIST_ACC"));
            } else if (account.getOperDate().compareTo(lastOpenDate) < 0) {
                parseRowResult = new OpenCloseResult(results.get("INVALID_CLOSEDATE"));
                parseRowResult.setText(
                        parseRowResult.getText()
                                .replace("%%{CLOSE_DATE}%%", account.getOperDate().toString())
                                .replace("%%{OPEN_DATE}%%", lastOpenDate.toString()));

            }
        }
        return parseRowResult;
    }

    private OpenCloseResponse makeResponse(MtContent content,
                                           MtFormat format,
                                           OpenCloseRequest data,
                                           Map<Integer, OpenCloseResult> processResult,
                                           OpenCloseDynamicSettings dynSettings) {
        OpenCloseResponse ret = new OpenCloseResponse(data);

        String[] constantNodeNames = new String[]{"HEAD", "ID", "MT_FORM", "SUBJECT"};
        Arrays.stream(constantNodeNames).forEach(nodeName -> {
            var node = content.getNode(nodeName, MtContent.FindNodeType.ByOrigCode);
            var block = new MtBlock(
                    0,
                    format.getOutBlockFormatString(nodeName),
                    node);
            node.getBlocks().add(block);
        });
        String respCodeForm = data.getCodeForm().equals("A01") ? "A1C" : "A3C";

        var idBlock = content.getNode("ID").getBlocks().get(0);
        idBlock.setItem(format.getItem("reference"),
                dynSettings.isUseFixReference() && !dynSettings.getFixReference().isEmpty()
                        ? dynSettings.getFixReference()
                        : data.getReference());

        var subjBlock = content.getNode("SUBJECT").getBlocks().get(0);
        ret.setCodeForm(respCodeForm);
        subjBlock.setItem(format.getItem("code_form"), respCodeForm);
        ret.setNotifyDate(LocalDateTime.now());
        subjBlock.setItem(format.getItem("notify_date"), ret.getNotifyDate());
        subjBlock.setItem(format.getItem("name_form"),
                respCodeForm.equals("A1C")
                        ? "Подтв. о получ. увед. об откр. и закр. банк. счетов"
                        : "Подтв.о получ.увед.об измен.номеров банк.счетов");

        MtNode accNode = respCodeForm.equals("A1C")
                ? content.getNode("ACCOUNT")
                : content.getNode("ACCOUNT_CHANGE");
        for (var account : data.getAccounts()) {
            var retAcc = new OpenCloseResponseAccount(ret, account);
            var accBlock = new MtBlock(
                    account.getSort(),
                    format.getOutBlockFormatString(accNode.getCurrentCode()),
                    accNode);
            accBlock.setBlockFormat(format.getOutBlockFormat(accNode.getCurrentCode()));

            accNode.getBlocks().add(accBlock);
            accBlock.setItem(format.getItem("bic"), retAcc.getBic());
            accBlock.setItem(format.getItem("account"), retAcc.getAccount());
            accBlock.setItem(format.getItem("account_type"), retAcc.getAccountType());
            accBlock.setItem(format.getItem("oper_type"), retAcc.getOperType());
            accBlock.setItem(format.getItem("oper_date"), retAcc.getOperDate());
            accBlock.setItem(format.getItem("rnn"), retAcc.getRnn());
            accBlock.setItem(format.getItem("dog"), retAcc.getDog());
            accBlock.setItem(format.getItem("dog_date"), retAcc.getDogDate());
            accBlock.setItem(format.getItem("acc_old"), retAcc.getAccountOld());
            accBlock.setItem(format.getItem("bic_old"), retAcc.getBicOld());
            accBlock.setItem(format.getItem("acc_date_ch"), retAcc.getDateModify());
            var res = processResult.get(retAcc.getSort());
            retAcc.setResultCode(res.getCode());
            accBlock.setItem(format.getItem("result_code"), retAcc.getResultCode());
            retAcc.setResultMessage(res.getText());
            accBlock.setItem(format.getItem("result_name"), retAcc.getResultMessage());
        }
        content.getItems().putAll(format.getItems());

        String contentText = MtComposer.Compose(content);
        content.setRawData(contentText);

        return ret;
    }

}
