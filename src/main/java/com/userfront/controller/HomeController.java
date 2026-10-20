package com.userfront.controller;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.userfront.config.SignupRateLimiter;
import com.userfront.dao.RoleDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.domain.security.UserRole;
import com.userfront.service.UserService;

@Controller
public class HomeController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
    private RoleDao roleDao;

	@Autowired
    private SignupRateLimiter signupRateLimiter;
	
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
        User user = new User();

        model.addAttribute("user", user);

        return "signup";
    }
	
	@RequestMapping(value = "/signup", method = RequestMethod.POST)
    public String signupPost(@ModelAttribute("user") User user, Model model,
                             HttpServletRequest request, HttpServletResponse response) {

        if (!signupRateLimiter.tryAcquire(request.getRemoteAddr())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            model.addAttribute("signupRateLimited", true);
            return "signup";
        }

        if (userService.checkUserExists(user.getUsername(), user.getEmail())) {
            model.addAttribute("signupUnavailable", true);
            return "signup";
        } else {
        	 Set<UserRole> userRoles = new HashSet<>();
             userRoles.add(new UserRole(user, roleDao.findByName("ROLE_USER")));

            try {
                userService.createUser(user, userRoles);
            } catch (DataIntegrityViolationException e) {
                model.addAttribute("signupUnavailable", true);
                return "signup";
            }

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
