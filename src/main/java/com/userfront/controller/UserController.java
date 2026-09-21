package com.userfront.controller;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.userfront.domain.User;
import com.userfront.dto.ProfileForm;
import com.userfront.service.UserService;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());

        model.addAttribute("user", ProfileForm.from(user));

        return "profile";
    }

    @PostMapping("/profile")
    public String profilePost(@Valid @ModelAttribute("user") ProfileForm form, BindingResult bindingResult,
                              Principal principal) {
        if (bindingResult.hasErrors()) {
            return "profile";
        }

        User user = userService.findByUsername(principal.getName());
        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());
        user.setEmail(form.getEmail());
        user.setPhone(form.getPhone());

        userService.saveUser(user);

        return "profile";
    }
}
