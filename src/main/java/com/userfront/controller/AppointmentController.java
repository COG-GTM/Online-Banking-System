package com.userfront.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.userfront.domain.Appointment;
import com.userfront.domain.User;
import com.userfront.service.AppointmentService;
import com.userfront.service.UserService;

@Controller
@RequestMapping("/appointment")
public class AppointmentController {

    private static final DateTimeFormatter APPOINTMENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/create",method = RequestMethod.GET)
    public String createAppointment(Model model) {
        Appointment appointment = new Appointment();
        model.addAttribute("appointment", appointment);
        model.addAttribute("dateString", "");

        return "appointment";
    }

    @RequestMapping(value = "/create",method = RequestMethod.POST)
    public String createAppointmentPost(@ModelAttribute("appointment") Appointment appointment, @ModelAttribute("dateString") String date, Model model, Principal principal) {

        LocalDateTime appointmentDate;
        try {
            appointmentDate = LocalDateTime.parse(date.trim(), APPOINTMENT_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            model.addAttribute("dateError", "Please pick a valid date and time (yyyy-MM-dd HH:mm).");
            return "appointment";
        }

        if (!appointmentDate.isAfter(LocalDateTime.now())) {
            model.addAttribute("dateError", "Please pick a date and time in the future.");
            return "appointment";
        }

        appointment.setDate(Date.from(appointmentDate.atZone(ZoneId.systemDefault()).toInstant()));

        User user = userService.findByUsername(principal.getName());
        appointment.setUser(user);

        appointmentService.createAppointment(appointment);

        return "redirect:/userFront";
    }


}
