package com.userfront.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.userfront.domain.User;
import com.userfront.dto.ProfileForm;
import com.userfront.service.UserService;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public String profile(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());

        model.addAttribute("user", user);

        return "profile";
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public String profilePost(@ModelAttribute("profileForm") ProfileForm profileForm, Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);

        if (isBlank(profileForm.getFirstName()) || isBlank(profileForm.getLastName())
                || isBlank(profileForm.getEmail()) || isBlank(profileForm.getPhone())) {
            model.addAttribute("missingFields", true);
            return "profile";
        }

        String newEmail = profileForm.getEmail().trim();
        if (!newEmail.equals(user.getEmail()) && userService.checkEmailExists(newEmail)) {
            model.addAttribute("emailExists", true);
            return "profile";
        }

        user.setFirstName(profileForm.getFirstName().trim());
        user.setLastName(profileForm.getLastName().trim());
        user.setEmail(newEmail);
        user.setPhone(profileForm.getPhone().trim());

        model.addAttribute("user", userService.saveUser(user));

        return "profile";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }


}

