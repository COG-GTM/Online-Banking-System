package com.userfront.controller;

import java.security.Principal;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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

        model.addAttribute("user", new ProfileForm(user));
        model.addAttribute("account", user);

        return "profile";
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public String profilePost(@Valid @ModelAttribute("user") ProfileForm profileForm, BindingResult bindingResult, Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());

        if (bindingResult.hasErrors()) {
            model.addAttribute("account", user);
            return "profile";
        }

        user.setFirstName(profileForm.getFirstName());
        user.setLastName(profileForm.getLastName());
        user.setEmail(profileForm.getEmail());
        user.setPhone(profileForm.getPhone());

        userService.saveUser(user);

        model.addAttribute("account", user);

        return "profile";
    }


}
