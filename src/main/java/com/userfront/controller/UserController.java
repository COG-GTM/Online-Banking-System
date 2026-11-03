package com.userfront.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        String newUsername = profileForm.getUsername();
        String newEmail = profileForm.getEmail();

        if (newUsername != null && !newUsername.equals(user.getUsername())
                && userService.checkUsernameExists(newUsername)) {
            model.addAttribute("usernameExists", true);
            return "profile";
        }

        if (newEmail != null && !newEmail.equals(user.getEmail())
                && userService.checkEmailExists(newEmail)) {
            model.addAttribute("emailExists", true);
            return "profile";
        }

        boolean usernameChanged = newUsername != null && !newUsername.equals(user.getUsername());

        user.setUsername(newUsername);
        user.setFirstName(profileForm.getFirstName());
        user.setLastName(profileForm.getLastName());
        user.setEmail(newEmail);
        user.setPhone(profileForm.getPhone());

        user = userService.saveUser(user);
        model.addAttribute("user", user);

        if (usernameChanged) {
            Authentication current = SecurityContextHolder.getContext().getAuthentication();
            UsernamePasswordAuthenticationToken updated =
                    new UsernamePasswordAuthenticationToken(user, current.getCredentials(), user.getAuthorities());
            updated.setDetails(current.getDetails());
            SecurityContextHolder.getContext().setAuthentication(updated);
        }

        return "profile";
    }


}

