package com.userfront.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.userfront.domain.User;
import com.userfront.service.UserService;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @InitBinder("user")
    public void initUserBinder(WebDataBinder binder) {
        binder.setAllowedFields("firstName", "lastName", "email", "phone");
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public String profile(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());

        model.addAttribute("user", user);

        return "profile";
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public String profilePost(@ModelAttribute("user") User newUser, Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());

        User emailOwner = userService.findByEmail(newUser.getEmail());
        if (emailOwner != null && !emailOwner.getUserId().equals(user.getUserId())) {
            return rejectEmailConflict(newUser, user, model);
        }

        user.setFirstName(newUser.getFirstName());
        user.setLastName(newUser.getLastName());
        user.setEmail(newUser.getEmail());
        user.setPhone(newUser.getPhone());

        try {
            userService.saveUser(user);
        } catch (DataIntegrityViolationException e) {
            return rejectEmailConflict(newUser, user, model);
        }

        model.addAttribute("user", user);

        return "profile";
    }

    private String rejectEmailConflict(User submitted, User current, Model model) {
        submitted.setUsername(current.getUsername());
        submitted.setPrimaryAccount(current.getPrimaryAccount());
        submitted.setSavingsAccount(current.getSavingsAccount());
        model.addAttribute("emailExists", true);
        model.addAttribute("user", submitted);
        return "profile";
    }
}
