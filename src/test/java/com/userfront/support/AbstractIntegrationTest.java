package com.userfront.support;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.dao.RoleDao;
import com.userfront.domain.security.Role;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ContextConfiguration(initializers = MySqlContainerInitializer.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private RoleDao roleDao;

    /** Signup looks up ROLE_USER, which the app expects to already exist in the database. */
    @Before
    public void seedUserRole() {
        if (roleDao.findByName("ROLE_USER") == null) {
            Role role = new Role();
            role.setRoleId(1);
            role.setName("ROLE_USER");
            roleDao.save(role);
        }
    }
}
