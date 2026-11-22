package com.userfront.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
        model.addAttribute("profileForm", ProfileForm.from(user));

        return "profile";
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public String profilePost(@ModelAttribute("profileForm") ProfileForm profileForm, Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);

        if (emailTakenByAnotherUser(profileForm.getEmail(), user)) {
            model.addAttribute("emailExists", true);
            return "profile";
        }

        ProfileForm previous = ProfileForm.from(user);
        profileForm.applyTo(user);
        try {
            userService.saveUser(user);
        } catch (DataIntegrityViolationException e) {
            previous.applyTo(user);
            if (!emailTakenByAnotherUser(profileForm.getEmail(), user)) {
                throw e;
            }
            model.addAttribute("emailExists", true);
            return "profile";
        }

        return "profile";
    }

    private boolean emailTakenByAnotherUser(String email, User user) {
        User emailOwner = userService.findByEmail(email);
        return emailOwner != null && !emailOwner.getUserId().equals(user.getUserId());
    }
}
