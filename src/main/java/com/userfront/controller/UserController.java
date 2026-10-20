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
import com.userfront.service.EmailAlreadyExistsException;
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
    public String profilePost(@ModelAttribute("user") User newUser, Principal principal, Model model) {
        String username = principal.getName();

        try {
            model.addAttribute("user", userService.updateProfile(username, newUser));
        } catch (EmailAlreadyExistsException e) {
            showEmailExists(newUser, username, model);
        } catch (DataIntegrityViolationException e) {
            if (!isEmailOwnedByAnotherUser(newUser.getEmail(), username)) {
                throw e;
            }
            showEmailExists(newUser, username, model);
        }

        return "profile";
    }

    private boolean isEmailOwnedByAnotherUser(String email, String username) {
        if (email == null) {
            return false;
        }
        User owner = userService.findByEmail(email);
        return owner != null && !owner.getUsername().equals(username);
    }

    private void showEmailExists(User submitted, String username, Model model) {
        User current = userService.findByUsername(username);
        submitted.setUsername(current.getUsername());
        submitted.setPrimaryAccount(current.getPrimaryAccount());
        submitted.setSavingsAccount(current.getSavingsAccount());

        model.addAttribute("emailExists", true);
        model.addAttribute("user", submitted);
    }
}

