$.ajaxSetup({contentType: "application/json; charset=utf-8"});

let checkNames = ["CheckUniqueMessageId", "CheckUniqueReference", "ValidateAccountState",
    "ValidateOperationDate", "RaiseTestError", "UseFixReference"];

let strNames = ["FixReference"];

$(function () {
    $.getJSON(apiUrl + '/setting/' + serviceName, function (data) {
        initForm(data)
    });
});

function initForm(data) {
    let itemNames = data.map(a => a.key);
    let itemValues = data.map(a => a.value);
    $.each(checkNames, function (key, checkName) {
        let ind = itemNames.indexOf(checkName);
        let checked = ind < 0 ? false : itemValues[ind];
        let checkEl = $('#' + checkName);
        checkEl.prop('checked', checked);
        setCheckName(checkName);
    });
    $.each(strNames, function (key, strName) {
        let ind = itemNames.indexOf(strName);
        let strVal = ind < 0 ? '' : itemValues[ind];
        let strEl = $('#' + strName);
        strEl.val(strVal);
    });
}

function setCheckName(element) {
    let checkEl = $('#' + element);
    let descrEl = $('#lbl' + element);
    let val = $(checkEl).is(':checked') ? "Включено" : "Выключено";
    descrEl.text(val);
    if (element == 'UseFixReference'){
        let strEl = $('#FixReference');
        strEl.prop( "disabled", ! $(checkEl).is(':checked') );
    }
}

function save() {
    $.each(checkNames, function (key, name) {
        let el = $('#' + name);
        let val = $(el).is(':checked')
        $.post(apiUrl + '/setting/' + serviceName + '/' + name, JSON.stringify(val));
    });
    $.each(strNames, function (key, name) {
        let el = $('#' + name);
        let val = $(el).val();
        $.post(apiUrl + '/setting/' + serviceName + '/' + name, JSON.stringify(val));
    });
    location.reload();
}
