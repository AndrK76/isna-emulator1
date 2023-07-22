function getData(fromIdx) {
    $.getJSON(apiUrl + '/opencloserequest?perPage=' + perPage + '&after=' + fromIdx, function (data) {
        $('#listTable  > tbody').empty();
        $.each(data.content, function (key, val) {
            prevIdx = val.id;
            let responseCol = '<td/>';
            let responseDatCol = '<td/>';
            if (val.response != null) {
                responseDatCol = '<td>' + val.response.notifyDate.replace('T', ' ').replace('+', ' +') + '</td>';
                responseCol = '<td><a href="#" onclick="showHideResponseAccounts(' + val.id + ', '
                    + val.response.id + ', false)">' + val.response.codeForm + '</td>';
            }

            $('#listTable  > tbody').append('<tr id="row_' + val.id + '">'
                + '<td><a href="#" onclick="showHideSrcRequest(' + val.id + ', ' + val.rawRequestId + ', false)">' + val.messageId + '</a></td>'
                + '<td>' + val.reference + '</td>'
                + '<td><a href="#" onclick="showHideRequestAccounts(' + val.id + ', false)">' + val.codeForm + '</td>'
                + '<td>' + val.notifyDate.replace('T', ' ').replace('+', ' +') + '</td>'
                + responseCol
                + responseDatCol
                + '</tr>');
        });
        enableButton($('#btNext'), !data.last);
        enableButton($('#btPrev'), true);
        if (fromIdx !== '') {
            startIdx = '';
            $.getJSON(apiUrl + '/opencloserequest/' + fromIdx + '/getnewest?offset=' + perPage, function (data) {
                startIdx = data;
            });
        }
    });
}


function showHideSrcRequest(idRow, idRequest, onlyHide) {
    let testEl = $('#request_' + idRow);
    if (onlyHide) {
        if (testEl.length) {
            testEl.hide();
        }
    } else {
        saveScrollPos();
        showHideRequestAccounts(idRow, true);
        showHideResponseAccounts(idRow, null, true);
        if (!testEl.length) {
            $.get(apiUrl + '/request/' + idRequest, function (data) {
                let content = '<tr id="request_' + idRow + '"><td colspan = "6" class="p-0">';
                content += '<table class="table table-bordered table-responsive w-100">'
                content += '<tr><th>Id сообщения</th><td>' + data.messageId + '</td>';
                content += '<td rowspan="3"><pre>' + data.data + '</pre></td></tr>';
                content += '<tr><th>Дата сообщения</th><td>' + data.messageDate.replace('T', ' ').replace('+', ' +') + '</td></tr>';
                content += '<tr><th>Сервис</th><td>' + data.serviceId + '</td></tr>';
                if (data.response != null) {
                    content += '<tr><th>Статус ответа</th><td>' + data.response.statusCode + '</td>';
                    content += '<td rowspan="4"><pre>' + data.response.data + '</pre></td></tr>';
                    content += '<tr><th>Id ответа</th><td>' + data.response.messageId + '</td></tr>';
                    content += '<tr><th>Текст статуса</th><td>' + data.response.statusMessage + '</td></tr>';
                    content += '<tr><th>Дата ответа</th><td>' + data.response.responseDate.replace('T', ' ').replace('+', ' +') + '</td></tr>';
                }
                content += '</table></td></tr>'
                $('#row_' + idRow).after(content);
                restoreScrollPos();
            });
        } else {
            testEl.toggle();
            restoreScrollPos();
        }
    }
}

function showHideRequestAccounts(idRequest, onlyHide) {
    let testEl = $('#requestacc_' + idRequest);
    if (onlyHide) {
        if (testEl.length) {
            testEl.hide();
        }
    } else {
        saveScrollPos();
        showHideSrcRequest(idRequest, null, true);
        showHideResponseAccounts(idRequest, null, true);
        if (!testEl.length) {
            $.get(apiUrl + '/opencloserequest/' + idRequest + '/account', function (data) {
                let content = '<tr id="requestacc_' + idRequest + '"><td colspan = "6" class="p-0">';
                content += '<table class="table table-bordered table-responsive w-100 caption-top">';
                content += '<caption>Счета запроса</caption>';
                content += '<tr><th>Счет</th><th>БИК</th><th>Тип</th><th>Опер</th>'
                    + '<th>Дата</th><th>РНН</th><th>Договор</th>'
                    + '<th>от</th><th>БИК (стар)</th><th>Счет (стар)</th><th>Дат. изм</th></tr><tbody>';
                for (let i = 0; i < data.accounts.length; i++) {
                    let account = data.accounts[i];
                    content += '<tr>';
                    content += '<td>' + account.account + '</td>';
                    content += '<td>' + account.bic + '</td>';
                    content += '<td>' + account.accountType + '</td>';
                    content += '<td>' + account.operType + '</td>';
                    content += '<td>' + account.operDate.replace('T', ' ').replace('+', ' +') + '</td>';
                    content += '<td>' + account.rnn + '</td>';
                    content += '<td>' + account.dog + '</td>';
                    if (account.dogDate != null) {
                        content += '<td>' + account.dogDate.replace('T', ' ').replace('+', ' +') + '</td>';
                    } else {
                        content += '<td>' + account.dogDate + '</td>';
                    }
                    content += '<td>' + account.bicOld + '</td>';
                    content += '<td>' + account.accountOld + '</td>';
                    if (account.dateModify != null) {
                        content += '<td>' + account.dateModify.replace('T', ' ').replace('+', ' +') + '</td>';
                    } else {
                        content += '<td>' + account.dateModify + '</td>';
                    }
                    content += '</tr>';
                }
                content += '</tbody></table></td></tr>'
                $('#row_' + idRequest).after(content);
                restoreScrollPos();
            });
        } else {
            testEl.toggle();
            restoreScrollPos();
        }
    }
}

function showHideResponseAccounts(idRequest, idResponse, onlyHide) {
    let testEl = $('#responseacc_' + idRequest);
    if (onlyHide) {
        if (testEl.length) {
            testEl.hide();
        }
    } else {
        saveScrollPos();
        showHideSrcRequest(idRequest, null, true);
        showHideRequestAccounts(idRequest, true);
        if (!testEl.length) {
            $.get(apiUrl + '/opencloseresponse/' + idRequest + '/account', function (data) {
                let content = '<tr id="responseacc_' + idRequest + '"><td colspan = "6" class="p-0">';
                content += '<table class="table table-bordered table-responsive w-100 caption-top">';
                content += '<caption>Счета ответа</caption>';
                content += '<tr><th>Счет</th><th>БИК</th><th>Тип</th><th>Опер</th>'
                    + '<th>Дата</th><th>Результат</th><th>Ошибка</th><th>РНН</th><th>Договор</th>'
                    + '<th>от</th><th>БИК (стар)</th><th>Счет (стар)</th><th>Дат. изм</th></tr><tbody>';
                for (let i = 0; i < data.accounts.length; i++) {
                    let account = data.accounts[i];
                    content += '<tr>';
                    content += '<td>' + account.account + '</td>';
                    content += '<td>' + account.bic + '</td>';
                    content += '<td>' + account.accountType + '</td>';
                    content += '<td>' + account.operType + '</td>';
                    content += '<td>' + account.operDate.replace('T', ' ').replace('+', ' +') + '</td>';
                    content += '<td>' + account.resultCode + '</td>';
                    content += '<td>' + account.resultMessage + '</td>';
                    content += '<td>' + account.rnn + '</td>';
                    content += '<td>' + account.dog + '</td>';
                    if (account.dogDate != null) {
                        content += '<td>' + account.dogDate.replace('T', ' ').replace('+', ' +') + '</td>';
                    } else {
                        content += '<td>' + account.dogDate + '</td>';
                    }
                    content += '<td>' + account.bicOld + '</td>';
                    content += '<td>' + account.accountOld + '</td>';
                    if (account.dateModify != null) {
                        content += '<td>' + account.dateModify.replace('T', ' ').replace('+', ' +') + '</td>';
                    } else {
                        content += '<td>' + account.dateModify + '</td>';
                    }
                    content += '</tr>';
                }
                content += '</tbody></table></td></tr>'
                $('#row_' + idRequest).after(content);
                restoreScrollPos();
            });
        } else {
            testEl.toggle();
            restoreScrollPos();
        }
    }
}


