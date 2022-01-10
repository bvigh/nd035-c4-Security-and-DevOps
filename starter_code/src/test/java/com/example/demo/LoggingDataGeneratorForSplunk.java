package com.example.demo;

import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import com.example.demo.util.ItemCount;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class LoggingDataGeneratorForSplunk {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    // same as data.sql
    private static final Item ITEM_1 = new Item(1L, "Round Widget", BigDecimal.valueOf(2.99), "A widget that is round");
    private static final Item ITEM_2 = new Item(2L, "Square Widget", BigDecimal.valueOf(1.99), "A widget that is square");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper;

    public LoggingDataGeneratorForSplunk() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    public void setup() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    public void contextLoads() {}

    @ParameterizedTest
    @MethodSource("provideData")
    public void order(User user, List<List<ItemCount>> orders) throws Exception {
        // create credentials for signup
        CreateUserRequest userRequest = new CreateUserRequest(user.getUsername(), user.getPassword(), user.getPassword());
        String userRequestStr = objectMapper.writeValueAsString(userRequest);

        // singup user
        mockMvc.perform(post("/api/user/create")
                        .contentType(APPLICATION_JSON_UTF8)
                        .content(userRequestStr))
                .andExpect(status().isOk());


        // create credentials for login
        String loginStr = new JSONObject()
                .put("username", userRequest.getUsername())
                .put("password", userRequest.getPassword())
                .toString();

        // login user
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(APPLICATION_JSON_UTF8)
                        .content(loginStr))
                .andExpect(status().isOk())
                .andReturn();

        // get the JWT token
        String bearer = result.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
        assertNotNull(bearer);
        assertTrue(bearer.startsWith("Bearer "));

        // add items to cart

        for (List<ItemCount> order : orders) {
            for (ItemCount itemCount : order) {
                Item item = itemCount.getItem();
                Integer count = itemCount.getCount();
                BigDecimal totalPrice = item.getPrice().multiply(BigDecimal.valueOf(count));
                ModifyCartRequest cartRequest = new ModifyCartRequest();
                cartRequest.setUsername(userRequest.getUsername());
                cartRequest.setItemId(item.getId());
                cartRequest.setQuantity(count);

                result = mockMvc.perform(post("/api/cart/addToCart")
                                .header(HttpHeaders.AUTHORIZATION, bearer)
                                .contentType(APPLICATION_JSON_UTF8)
                                .content(objectMapper.writeValueAsString(cartRequest)))
                        .andExpect(status().isOk())
                        .andReturn();

            }
            // submit order
            result = mockMvc.perform(post("/api/order/submit/" + userRequest.getUsername())
                            .header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isOk())
                    .andReturn();
        }
    }

    private static Stream<Arguments> provideData() {
        return Stream.of(
                Arguments.of(
                        new User(2L, "jackie", "passw1234"),
                        Arrays.asList(
                                Arrays.asList(
                                        new ItemCount(ITEM_1, 2),
                                        new ItemCount(ITEM_2, 1),
                                        new ItemCount(ITEM_1, 3)),
                                Arrays.asList(
                                        new ItemCount(ITEM_2, 1),
                                        new ItemCount(ITEM_1,5)
                                ),
                                Arrays.asList(
                                        new ItemCount(ITEM_1, 2)
                                )
                        )
                )
        );
    }

}
