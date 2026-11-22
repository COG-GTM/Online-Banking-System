package com.userfront.controller;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.userfront.dao.RoleDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.domain.security.UserRole;
import com.userfront.dto.SignupForm;
import com.userfront.service.UserService;

@Controller
public class HomeController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
    private RoleDao roleDao;
	
	@RequestMapping("/")
	public String home() {
		return "redirect:/index";
	}
	
	@RequestMapping("/index")
    public String index() {
        return "index";
    }
	
	@InitBinder("user")
	public void initSignupBinder(WebDataBinder binder) {
		binder.setAllowedFields("firstName", "lastName", "phone", "email", "username", "password");
		binder.registerCustomEditor(String.class, "firstName", new StringTrimmerEditor(true));
		binder.registerCustomEditor(String.class, "lastName", new StringTrimmerEditor(true));
		binder.registerCustomEditor(String.class, "phone", new StringTrimmerEditor(true));
		binder.registerCustomEditor(String.class, "email", new StringTrimmerEditor(true));
		binder.registerCustomEditor(String.class, "username", new StringTrimmerEditor(true));
	}

	@RequestMapping(value = "/signup", method = RequestMethod.GET)
    public String signup(Model model) {
        model.addAttribute("user", new SignupForm());

        return "signup";
    }

	@RequestMapping(value = "/signup", method = RequestMethod.POST)
    public String signupPost(@Valid @ModelAttribute("user") SignupForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            form.setPassword(null);
            return "signup";
        }

        boolean emailExists = userService.checkEmailExists(form.getEmail());
        boolean usernameExists = userService.checkUsernameExists(form.getUsername());

        if (emailExists || usernameExists) {
            if (emailExists) {
                model.addAttribute("emailExists", true);
            }

            if (usernameExists) {
                model.addAttribute("usernameExists", true);
            }

            form.setPassword(null);
            return "signup";
        }

        User user = form.toUser();
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(new UserRole(user, roleDao.findByName("ROLE_USER")));

        userService.createUser(user, userRoles);

        return "redirect:/";
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
