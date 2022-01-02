package com.example.demo.controllers;

import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@WebMvcTest(UserController.class)
@ContextConfiguration
public class UserControllerTest {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
    private static final String STUBBED_ENCODED_PASSWORD = "stubbedEncodedPassword";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserRepository userRepository;

    @MockBean
    CartRepository cartRepository;

    @MockBean
    OrderRepository orderRepository;

    @MockBean
    ItemRepository itemRepository;

    @MockBean
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    UserDetailsServiceImpl userDetailsService;

    @Before
    public void beforeEach() {
        when(bCryptPasswordEncoder.encode(anyString())).thenReturn("stubbedEncodedPassword");
    }

    @Test
    @WithMockUser("logaran")
    public void createUser() throws Exception {
        User user = new User(1L, "testuser", "testpassw");
        CreateUserRequest userRequest = new CreateUserRequest(
                user.getUsername(),
                user.getPassword(),
                user.getPassword());


        when(userRepository.save(any())).thenReturn(user);

        ObjectMapper objectMapper = new ObjectMapper();
        String userRequestStr = objectMapper.writeValueAsString(userRequest);

        MvcResult result = mockMvc.perform(post("/api/user/create")
                .contentType(APPLICATION_JSON_UTF8)
                .content(userRequestStr))
                        .andExpect(status().isOk())
                                .andReturn();

        String response = result.getResponse().getContentAsString();
        String responseUsername = JsonPath.parse(response).read("$.username");
        int responseUserId = JsonPath.parse(response).read("$.id");
        assertEquals(user.getUsername(), responseUsername);
        assertEquals(user.getId(), responseUserId);


        verify(userRepository, times(1)).save(any());
    }
}
