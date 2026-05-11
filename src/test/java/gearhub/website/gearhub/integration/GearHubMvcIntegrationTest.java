package gearhub.website.gearhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gearhub.website.gearhub.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GearHubMvcIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Nested
    class PublicAndHealth {
        @Test
        @DisplayName("GET /api/health returns UP")
        void apiHealth() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("GET /api/products is public (wrapped payload)")
        void productsPublic() throws Exception {
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    class AuthAndValidation {
        @Test
        @DisplayName("register rejects invalid email with 400")
        void registerInvalidEmail() throws Exception {
            String body = """
                    {"username":"bad","email":"not-an-email","password":"password1","role":"CUSTOMER"}
                    """;
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("register rejects ADMIN role with 400")
        void registerAdminForbidden() throws Exception {
            String body = """
                    {"username":"root","email":"root@x.test","password":"password1","role":"ADMIN"}
                    """;
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("duplicate email returns 409")
        void registerDuplicateEmail() throws Exception {
            String body1 = registerJson("trader1", "dup@order.test", Role.TRADER);
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body1))
                    .andExpect(status().isOk());

            String body2 = registerJson("trader2", "dup@order.test", Role.CUSTOMER);
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body2))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    class SecurityEnforcement {
        @Test
        @DisplayName("CUSTOMER cannot create products — 403")
        void customerForbiddenPostProduct() throws Exception {
            register("buyer_blk", "buyer_blk@sec.test", "password01", Role.CUSTOMER);
            String buyerToken = login("buyer_blk@sec.test", "password01");

            String product = """
                    {"name":"Leak","description":"—","price":9.99,"stockQuantity":1,"category":"Test"}
                    """;
            mockMvc.perform(post("/api/products")
                            .header("Authorization", bearer(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(product))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Invalid Bearer token is rejected before role checks · 401 from entry point")
        void invalidJwtCannotAccessCart() throws Exception {
            mockMvc.perform(get("/api/cart").header("Authorization", "Bearer not.a.valid.jwt.token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    class MarketplaceFlow {

        @Test
        @DisplayName("Trader lists product · customer carts & checkouts · trader updates status")
        void traderCustomerOrderLifecycle() throws Exception {
            register("flow_trader", "flow.trader@int.test", "password01", Role.TRADER);
            register("flow_buyer", "flow.buyer@int.test", "password01", Role.CUSTOMER);

            String traderToken = login("flow.trader@int.test", "password01");
            String buyerToken = login("flow.buyer@int.test", "password01");

            String productJson = """
                    {"name":"CV Boot Kit","description":"Heavy duty","price":45.50,"stockQuantity":8,"category":"Drive"}
                    """;
            mockMvc.perform(post("/api/products")
                            .header("Authorization", bearer(traderToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(productJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get("/api/products").param("search", "cv"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));

            long productId = objectMapper.readTree(
                                    mockMvc.perform(get("/api/products"))
                                            .andReturn()
                                            .getResponse()
                                            .getContentAsString())
                            .path("data")
                            .path(0)
                            .path("id")
                            .asLong();

            String cartPayload = String.format(Locale.US, "{\"productId\":%d,\"quantity\":2}", productId);
            mockMvc.perform(post("/api/cart/items")
                            .header("Authorization", bearer(buyerToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cartPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].quantity").value(2));

            String checkout = mockMvc.perform(post("/api/orders/checkout").header("Authorization", bearer(buyerToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalAmount").value(91.0))
                    .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            long orderId = objectMapper.readTree(checkout).path("data").path("id").asLong();

            mockMvc.perform(put("/api/orders/" + orderId + "/status")
                            .header("Authorization", bearer(traderToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PROCESSING\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PROCESSING"));
        }

        @Test
        @DisplayName("Trader can delete own listing · product no longer resolves by id")
        void traderDeletesOwnProduct() throws Exception {
            register("trad_del_flow", "trader.selfdel@int.test", "password01", Role.TRADER);

            String traderToken = login("trader.selfdel@int.test", "password01");

            mockMvc.perform(post("/api/products")
                            .header("Authorization", bearer(traderToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Cheap mirror","price":7.99,"stockQuantity":2,"category":"Body"}
                                    """))
                    .andExpect(status().isOk());

            long productId =
                    extractFirstProductId(mockMvc.perform(get("/api/products")).andReturn());

            mockMvc.perform(delete("/api/products/" + productId).header("Authorization", bearer(traderToken)))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/products/{id}", productId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    /**
     * ——— helpers ———
     */
    String registerJson(String username, String email, Role role) {
        String r = "\"" + role.name() + "\"";
        return "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"password01\",\"role\":%s}"
                .formatted(username, email, r);
    }

    void register(String username, String email, String password, Role role) throws Exception {
        String body = "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}"
                .formatted(username, email, password, role.name());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    String login(String email, String password) throws Exception {
        String body =
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        MvcResult r = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        JsonNode tree = objectMapper.readTree(r.getResponse().getContentAsString());
        return tree.path("accessToken").asText();
    }

    String bearer(String jwt) {
        return "Bearer " + jwt;
    }

    long extractFirstProductId(MvcResult listing) throws Exception {
        return objectMapper
                .readTree(listing.getResponse().getContentAsString())
                .path("data")
                .path(0)
                .path("id")
                .asLong();
    }
}
