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
            cancelIcon: "fa fa-times",
            confirmLabel: "Confirm",
            confirmIcon: "fa fa-check",
            callback: function () {}
        }, options);

        var button = function (label, iconClass, cssClass) {
            var $button = $('<button type="button"></button>').addClass('btn').addClass(cssClass);
            if (iconClass) {
                $button.append($('<i></i>').addClass(iconClass)).append(document.createTextNode(' '));
            }
            return $button.append(document.createTextNode(label));
        };

        var $cancel = button(settings.cancelLabel, settings.cancelIcon, 'btn-default');
        var $confirm = button(settings.confirmLabel, settings.confirmIcon, 'btn-primary');

        var $modal = $('<div class="modal fade" tabindex="-1" role="dialog"></div>').append(
            $('<div class="modal-dialog" role="document"></div>').append(
                $('<div class="modal-content"></div>').append(
                    $('<div class="modal-header"></div>').append(
                        $('<h4 class="modal-title"></h4>').text(settings.title)
                    ),
                    $('<div class="modal-body"></div>').append(
                        $('<p></p>').text(settings.message)
                    ),
                    $('<div class="modal-footer"></div>').append($cancel, $confirm)
                )
            )
        );

        var result = false;

        $cancel.on('click', function () {
            $modal.modal('hide');
        });

        $confirm.on('click', function () {
            result = true;
            $modal.modal('hide');
        });

        $modal.on('hidden.bs.modal', function () {
            $modal.remove();
            settings.callback(result);
        });

        $('body').append($modal);
        $modal.modal('show');
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




