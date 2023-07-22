let startIdx = '';
let prevIdx = '';

$(function () {
    $('#perPage').val(perPage);
    getData(startIdx);
});

$.ajaxSetup({contentType: "application/json; charset=utf-8"});

$('#btPerPage').click(function () {
    startIdx = '';
    perPage = $('#perPage').val();
    getData(startIdx);
});

$('#btPrev').click(function () {
    getData(startIdx);
});

$('#btNext').click(function () {
    getData(prevIdx);
});

function enableButton(button, enable) {
    button.attr('disabled', !enable);
    if (enable) {
        button.removeClass('btn-light');
        button.addClass('btn-secondary');
    } else {
        button.removeClass('btn-secondary');
        button.addClass('btn-light');
    }
}

let scrollPos = 0;

function saveScrollPos(){
    scrollPos = $(window).scrollTop();
}

function restoreScrollPos(){
    $('html, body').animate({
        scrollTop: scrollPos
    }, 0);
    $('html, body').animate({
        scrollTop: scrollPos
    }, 500);
}
