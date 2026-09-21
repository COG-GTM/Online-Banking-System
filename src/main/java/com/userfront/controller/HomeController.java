package com.userfront.controller;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.userfront.dao.RoleDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.domain.dto.SignupForm;
import com.userfront.domain.security.UserRole;
import com.userfront.service.UserService;

@Controller
public class HomeController {

	private static final int MIN_PASSWORD_LENGTH = 12;
	
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
	
	@RequestMapping(value = "/signup", method = RequestMethod.GET)
    public String signup(Model model) {
        model.addAttribute("user", new SignupForm());

        return "signup";
    }
	
	@RequestMapping(value = "/signup", method = RequestMethod.POST)
    public String signupPost(@ModelAttribute("user") SignupForm signupForm,  Model model) {

        if (!isPasswordAcceptable(signupForm.getPassword())) {
            model.addAttribute("passwordInvalid", true);

            return "signup";
        }

        if(userService.checkUserExists(signupForm.getUsername(), signupForm.getEmail()))  {

            if (userService.checkEmailExists(signupForm.getEmail())) {
                model.addAttribute("emailExists", true);
            }

            if (userService.checkUsernameExists(signupForm.getUsername())) {
                model.addAttribute("usernameExists", true);
            }

            return "signup";
        } else {
            User user = new User();
            user.setUsername(signupForm.getUsername());
            user.setPassword(signupForm.getPassword());
            user.setFirstName(signupForm.getFirstName());
            user.setLastName(signupForm.getLastName());
            user.setEmail(signupForm.getEmail());
            user.setPhone(signupForm.getPhone());

        	 Set<UserRole> userRoles = new HashSet<>();
             userRoles.add(new UserRole(user, roleDao.findByName("ROLE_USER")));

            userService.createUser(user, userRoles);

            return "redirect:/";
        }
    }

    private static boolean isPasswordAcceptable(String password) {
        return password != null
                && password.length() >= MIN_PASSWORD_LENGTH
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
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
