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

    $.transactionLog = function () {
        $('.transaction-log').each(function () {
            var $log = $(this);
            var $table = $log.find('.transaction-log-table');
            var filters = {direction: '', from: null, to: null};

            $.fn.dataTable.ext.search.push(function (settings, data, dataIndex) {
                if (settings.nTable !== $table[0]) {
                    return true;
                }
                var row = settings.aoData[dataIndex].nTr;
                var day = row.getAttribute('data-day');
                if (filters.direction && row.getAttribute('data-direction') !== filters.direction) {
                    return false;
                }
                if (filters.from && day < filters.from) {
                    return false;
                }
                return !(filters.to && day > filters.to);
            });

            var table = $table.DataTable({
                order: [[0, 'desc']],
                pageLength: 10,
                lengthMenu: [10, 25, 50, 100],
                language: {
                    search: '',
                    searchPlaceholder: 'Search transactions',
                    lengthMenu: 'Show _MENU_ per page',
                    info: 'Showing _START_ to _END_ of _TOTAL_ transactions',
                    infoEmpty: 'No transactions to show',
                    infoFiltered: '(filtered from _MAX_)',
                    emptyTable: 'No transactions yet.',
                    zeroRecords: 'No transactions match your search or filters.'
                }
            });
            $log.find('.dataTables_filter input').attr('aria-label', 'Search transactions');
            $log.find('.dataTables_length select').attr('aria-label', 'Transactions per page');

            var $directionButtons = $log.find('.transaction-log-direction button');
            var setDirection = function (direction) {
                filters.direction = direction;
                $directionButtons.each(function () {
                    var active = $(this).data('direction') === direction;
                    $(this).toggleClass('active', active).attr('aria-pressed', active ? 'true' : 'false');
                });
            };

            $directionButtons.on('click', function () {
                setDirection($(this).data('direction'));
                table.draw();
            });

            $log.find('.transaction-log-from, .transaction-log-to').on('change', function () {
                filters.from = $log.find('.transaction-log-from').val() || null;
                filters.to = $log.find('.transaction-log-to').val() || null;
                table.draw();
            });

            $log.find('.transaction-log-clear').on('click', function () {
                setDirection('');
                filters.from = filters.to = null;
                $log.find('.transaction-log-from, .transaction-log-to').val('');
                $log.find('.dataTables_filter input').val('');
                table.search('').draw();
            });
        });
    };

}(jQuery));

$(document).ready(function() {
    var confirm = function() {
        bootbox.confirm({
            title: "Appointment Confirmation",
            message: "Do you really want to schedule this appointment?",
            buttons: {
                cancel: {
                    label: '<i class="fa fa-times"></i> Cancel'
                },
                confirm: {
                    label: '<i class="fa fa-check"></i> Confirm'
                }
            },
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

    $.transactionLog();

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




