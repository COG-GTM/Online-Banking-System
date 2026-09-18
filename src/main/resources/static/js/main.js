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

        var modal = $('<div class="modal fade" tabindex="-1" role="dialog"><div class="modal-dialog" role="document"><div class="modal-content">' +
            '<div class="modal-header"><button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
            '<h4 class="modal-title"></h4></div>' +
            '<div class="modal-body"><p></p></div>' +
            '<div class="modal-footer"><button type="button" class="btn btn-default" data-dismiss="modal"></button>' +
            '<button type="button" class="btn btn-primary"></button></div>' +
            '</div></div></div>');

        modal.find('.modal-title').text(settings.title);
        modal.find('.modal-body p').text(settings.message);
        modal.find('.modal-footer .btn-default').text(settings.cancelLabel);
        modal.find('.modal-footer .btn-primary').text(settings.confirmLabel);

        var confirmed = false;
        modal.find('.modal-footer .btn-primary').on('click', function () {
            confirmed = true;
            modal.modal('hide');
        });

        modal.on('hidden.bs.modal', function () {
            modal.remove();
            settings.callback(confirmed);
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




