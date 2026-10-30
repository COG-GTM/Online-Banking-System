package com.userfront;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonIgnorePropertiesTest {

    public static class Account {
        public String name;
        public String role = "user";
    }

    public static class CaseInsensitiveWrapper {
        @JsonIgnoreProperties("role")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        public Account account;
    }

    @Test
    public void perPropertyIgnoreSurvivesCaseInsensitiveMatching() throws Exception {
        CaseInsensitiveWrapper wrapper = new ObjectMapper().readValue(
                "{\"account\":{\"name\":\"alice\",\"role\":\"admin\"}}", CaseInsensitiveWrapper.class);

        assertEquals("alice", wrapper.account.name);
        assertEquals("user", wrapper.account.role);
    }
}
