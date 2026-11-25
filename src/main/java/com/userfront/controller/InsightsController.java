package com.userfront.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.userfront.domain.User;
import com.userfront.service.InsightsService;
import com.userfront.service.UserService;

@Controller
@RequestMapping("/insights")
public class InsightsController {

    @Autowired
    private UserService userService;

    @Autowired
    private InsightsService insightsService;

    @RequestMapping(method = RequestMethod.GET)
    public String insights(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());

        model.addAttribute("user", user);
        model.addAttribute("insights", insightsService.buildInsights(user.getPrimaryAccount(), user.getSavingsAccount()));

        return "insights";
    }
}
