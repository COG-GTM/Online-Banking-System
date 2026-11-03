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

        User emailOwner = userService.findByEmail(profileForm.getEmail());
        if (emailOwner != null && !emailOwner.getUserId().equals(user.getUserId())) {
            model.addAttribute("emailExists", true);
            return "profile";
        }

        user.setFirstName(profileForm.getFirstName());
        user.setLastName(profileForm.getLastName());
        user.setEmail(profileForm.getEmail());
        user.setPhone(profileForm.getPhone());

        userService.saveUser(user);

        return "profile";
    }
}
