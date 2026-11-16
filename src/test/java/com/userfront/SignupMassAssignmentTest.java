package com.userfront;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.dao.RoleDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;
import com.userfront.domain.security.UserRole;
import com.userfront.service.UserService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class SignupMassAssignmentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private RoleDao roleDao;

    private Role userRole;

    @Before
    public void setUp() {
        userRole = roleDao.findByName("ROLE_USER");
        if (userRole == null) {
            userRole = new Role();
            userRole.setRoleId(1);
            userRole.setName("ROLE_USER");
            userRole = roleDao.save(userRole);
        }
    }

    @Test
    public void signupWithExistingUserIdDoesNotOverwriteThatUser() throws Exception {
        User victim = createUser("victim", "victim@example.com", "victimPassword");
        Long victimId = victim.getUserId();
        String victimPasswordHash = victim.getPassword();

        mockMvc.perform(post("/signup")
                .param("userId", victimId.toString())
                .param("firstName", "Mallory")
                .param("lastName", "Attacker")
                .param("phone", "000-000-0000")
                .param("email", "mallory@example.com")
                .param("username", "mallory")
                .param("password", "malloryPassword")
                .param("enabled", "true")
                .param("recipientList[0].name", "injected"))
                .andExpect(status().is3xxRedirection());

        User reloadedVictim = userDao.findByEmail("victim@example.com");
        assertNotNull(reloadedVictim);
        assertEquals(victimId, reloadedVictim.getUserId());
        assertEquals("victim", reloadedVictim.getUsername());
        assertEquals(victimPasswordHash, reloadedVictim.getPassword());

        User attacker = userDao.findByUsername("mallory");
        assertNotNull(attacker);
        assertNotEquals(victimId, attacker.getUserId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createUserRejectsUserWithPresetId() {
        User existing = createUser("existing", "existing@example.com", "existingPassword");

        User forged = new User();
        forged.setUserId(existing.getUserId());
        forged.setUsername("forged");
        forged.setEmail("forged@example.com");
        forged.setPassword("forgedPassword");

        userService.createUser(forged, rolesFor(forged));
    }

    private User createUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        return userService.createUser(user, rolesFor(user));
    }

    private Set<UserRole> rolesFor(User user) {
        Set<UserRole> roles = new HashSet<>();
        roles.add(new UserRole(user, userRole));
        return roles;
    }
}
