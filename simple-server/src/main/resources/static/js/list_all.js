function showHideRequest(id, onlyHide) {
    let testEl = $('#detmsg_' + id);
    if (onlyHide) {
        if (testEl.length) {
            testEl.hide();
        }
    } else {
        saveScrollPos();
        showHideResponse(id, null, true)
        if (!testEl.length) {
            $.get(apiUrl + '/request/' + id + '/data', function (data) {
                addRowBelow('detmsg_', 'Message', id, data);
            });
        } else {
            testEl.toggle();
            restoreScrollPos();
        }
    }
}

function showHideResponse(requestId, responseId, onlyHide) {
    let testEl = $('#detresp_' + requestId);
    if (onlyHide) {
        if (testEl.length) {
            testEl.hide();
        }
    } else {
        saveScrollPos();
        showHideRequest(requestId, true);
        if (!testEl.length) {
            $.get(apiUrl + '/response/' + responseId + '/data', function (data) {
                addRowBelow('detresp_', 'Response', requestId, data);
            });
        } else {
            testEl.toggle();
            restoreScrollPos();
        }
    }
}

function getData(fromIdx) {
    $.getJSON(apiUrl + '/request?perPage=' + perPage + '&after=' + fromIdx, function (data) {
        $('#listTable  > tbody').empty();
        $.each(data.content, function (key, val) {
            let responseCol = '<td/>';
            let responseDatCol = '<td/>';
            if (val.response != null) {
                let status = val.response.statusMessage + ' (' + val.response.statusCode + ')';
                responseDatCol = '<td>' + val.response.responseDate.replace('T',' ').replace('+',' +') + '</td>';
                if (val.response.isSuccess) {
                    responseCol = '<td><a href="#" onclick="showHideResponse(' + val.id + ', ' + val.response.id + ', false)">'
                        + status + '</a></td>';
                } else {
                    responseCol = '<td>' + status + '</td>';
                }
            }
            prevIdx = val.id;
            $('#listTable  > tbody').append('<tr id="row_' + val.id + '">'
                + '<td><a href="#" onclick="showHideRequest(' + val.id + ', false)">' + val.messageId + '</a></td>'
                + '<td>' + val.messageDate.replace('T',' ').replace('+',' +') + '</td>'
                + '<td>' + val.serviceId + '</td>'
                + responseCol
                + responseDatCol
                + '</tr>');
        });
        enableButton($('#btNext'), !data.last);
        enableButton($('#btPrev'), true);
        if (fromIdx !== '') {
            startIdx = '';
            $.getJSON(apiUrl + '/request/' + fromIdx + '/getnewest?offset=' + perPage, function (data) {
                startIdx = data;
            });
        }
    });
}

function addRowBelow(prefix, title, id, data) {
    let content = '<tr id="' + prefix + id + '"><td colspan = "5">' + title + ':<pre>' + data + '</pre></td></tr>';
    $('#row_' + id).after(content);
    restoreScrollPos();
}
