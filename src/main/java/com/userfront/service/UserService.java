package com.userfront.service;

import java.util.List;

import com.userfront.domain.SignupForm;
import com.userfront.domain.User;

public interface UserService {
	User findByUsername(String username);

    User findByEmail(String email);

    boolean checkUserExists(String username, String email);

    boolean checkUsernameExists(String username);

    boolean checkEmailExists(String email);
    
    void save (User user);
    
    User createUser(SignupForm signupForm);
    
    User saveUser (User user); 
    
    List<User> findUserList();

    void enableUser (String username);

    void disableUser (String username);
}
