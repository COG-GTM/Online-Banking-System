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

    $.confirmDialog = function (options) {
        var settings = $.extend({
            title: "Confirm",
            message: "",
            cancelLabel: "Cancel",
            confirmLabel: "Confirm",
            callback: function () {}
        }, options);

        var modal = $('<div class="modal fade" tabindex="-1" role="dialog"></div>');
        var dialog = $('<div class="modal-dialog" role="document"></div>').appendTo(modal);
        var content = $('<div class="modal-content"></div>').appendTo(dialog);

        var header = $('<div class="modal-header"></div>').appendTo(content);
        $('<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>').appendTo(header);
        $('<h4 class="modal-title"></h4>').text(settings.title).appendTo(header);

        $('<div class="modal-body"></div>').append($('<p></p>').text(settings.message)).appendTo(content);

        var footer = $('<div class="modal-footer"></div>').appendTo(content);
        $('<button type="button" class="btn btn-default" data-dismiss="modal"></button>')
            .text(settings.cancelLabel).appendTo(footer);
        var confirm = $('<button type="button" class="btn btn-primary"></button>')
            .text(settings.confirmLabel).appendTo(footer);

        var confirmed = false;
        confirm.on('click', function () {
            confirmed = true;
            modal.modal('hide');
        });
        modal.on('hidden.bs.modal', function () {
            modal.remove();
            settings.callback(confirmed);
        });

        modal.appendTo('body').modal('show');
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



}(jQuery));

$(document).ready(function() {
    var confirm = function() {
        $.confirmDialog({
            title: "Appointment Confirmation",
            message: "Do you really want to schedule this appointment?",
            cancelLabel: "Cancel",
            confirmLabel: "Confirm",
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




