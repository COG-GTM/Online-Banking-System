/**
 * Created by z00382545 on 10/20/16.
 */

(function ($) {
    $.toggleShowPassword = function (options) {
        var settings = $.extend({
            field: "#password",
            control: "#toggle_show_password",
        }, options);

        var control = $(settings.control);
        var field = $(settings.field)

        control.bind('click', function () {
            if (control.is(':checked')) {
                field.attr('type', 'text');
            } else {
                field.attr('type', 'password');
            }
        })
    };

    $.transferDisplay = function () {
        $("#transferFrom").change(function() {
            if ($("#transferFrom").val() == 'Primary') {
                $('#transferTo').val('Savings');
            } else if ($("#transferFrom").val() == 'Savings') {
                $('#transferTo').val('Primary');
            }
        });

        $("#transferTo").change(function() {
            if ($("#transferTo").val() == 'Primary') {
                $('#transferFrom').val('Savings');
            } else if ($("#transferTo").val() == 'Savings') {
                $('#transferFrom').val('Primary');
            }
        });
    };


    $.confirmDialog = function (options) {
        var settings = $.extend({
            title: "",
            message: "",
            cancelLabel: "Cancel",
            confirmLabel: "Confirm",
            callback: function () {}
        }, options);

        var titleEl = $('<h4>', {"class": "modal-title"}).text(settings.title);
        var closeBtn = $('<button>', {
            type: "button",
            "class": "close",
            "data-dismiss": "modal",
            "aria-label": "Close"
        }).append($('<span>', {"aria-hidden": "true"}).html("&times;"));

        var cancelBtn = $('<button>', {type: "button", "class": "btn btn-default"})
            .append($('<i>', {"class": "fa fa-times"}), document.createTextNode(" " + settings.cancelLabel));
        var confirmBtn = $('<button>', {type: "button", "class": "btn btn-primary"})
            .append($('<i>', {"class": "fa fa-check"}), document.createTextNode(" " + settings.confirmLabel));

        var modal = $('<div>', {"class": "modal fade", tabindex: "-1", role: "dialog"}).append(
            $('<div>', {"class": "modal-dialog", role: "document"}).append(
                $('<div>', {"class": "modal-content"}).append(
                    $('<div>', {"class": "modal-header"}).append(closeBtn, titleEl),
                    $('<div>', {"class": "modal-body"}).append($('<p>').text(settings.message)),
                    $('<div>', {"class": "modal-footer"}).append(cancelBtn, confirmBtn)
                )
            )
        );

        var result = false;
        confirmBtn.on('click', function () {
            result = true;
            modal.modal('hide');
        });
        cancelBtn.on('click', function () {
            modal.modal('hide');
        });
        modal.on('hidden.bs.modal', function () {
            modal.remove();
            settings.callback(result);
        });

        $('body').append(modal);
        modal.modal('show');
    };

}(jQuery));

$(document).ready(function() {
    var confirm = function() {
        $.confirmDialog({
            title: "Appointment Confirmation",
            message: "Do you really want to schedule this appointment?",
            callback: function (result) {
                if (result == true) {
                    $('#appointmentForm').submit();
                } else {
                    console.log("Scheduling cancelled.");
                }
            }
        });
    };

    $.toggleShowPassword({
        field: '#password',
        control: "#showPassword"
    });

    $.transferDisplay();

    $(".form_datetime").datetimepicker({
        format: "yyyy-mm-dd hh:mm",
        autoclose: true,
        todayBtn: true,
        startDate: "2013-02-14 10:00",
        minuteStep: 10
    });

    $('#submitAppointment').click(function () {
        confirm();
    });

});




