package com.adminportal.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.domain.security.UserRole;
import com.userfront.service.UserService;

@Service
@Transactional
public class AdminUserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public User findByUsername(String username) {
        return userDao.findByUsername(username);
    }

    @Override
    public List<User> findUserList() {
        return userDao.findAll();
    }

    @Override
    public void enableUser(String username) {
        User user = findByUsername(username);
        user.setEnabled(true);
        userDao.save(user);
    }

    @Override
    public void disableUser(String username) {
        User user = findByUsername(username);
        user.setEnabled(false);
        userDao.save(user);
    }

    @Override
    public User findByEmail(String email) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public boolean checkUserExists(String username, String email) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public boolean checkUsernameExists(String username) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public boolean checkEmailExists(String email) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public void save(User user) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public User createUser(User user, Set<UserRole> userRoles) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public User saveUser(User user) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }
}
