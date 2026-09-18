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

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SignupForm;
import com.userfront.domain.User;
import com.userfront.service.UserService;

@Controller
public class HomeController {
	
	@Autowired
	private UserService userService;
	
	@RequestMapping("/")
	public String home() {
		return "redirect:/index";
	}
	
	@RequestMapping("/index")
    public String index() {
        return "index";
    }
	
	@RequestMapping(value = "/signup", method = RequestMethod.GET)
    public String signup(Model model) {
        model.addAttribute("signupForm", new SignupForm());

        return "signup";
    }
	
	@RequestMapping(value = "/signup", method = RequestMethod.POST)
    public String signupPost(@Valid @ModelAttribute("signupForm") SignupForm signupForm, BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            return "signup";
        }

        if (userService.checkUserExists(signupForm.getUsername(), signupForm.getEmail()))  {

            if (userService.checkEmailExists(signupForm.getEmail())) {
                model.addAttribute("emailExists", true);
            }

            if (userService.checkUsernameExists(signupForm.getUsername())) {
                model.addAttribute("usernameExists", true);
            }

            return "signup";
        } else {
            userService.createUser(signupForm);

            return "redirect:/";
        }
    }
	
	@RequestMapping("/userFront")
	public String userFront(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        PrimaryAccount primaryAccount = user.getPrimaryAccount();
        SavingsAccount savingsAccount = user.getSavingsAccount();

        model.addAttribute("primaryAccount", primaryAccount);
        model.addAttribute("savingsAccount", savingsAccount);

        return "userFront";
    }
}
