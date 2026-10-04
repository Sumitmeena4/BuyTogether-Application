package com.hackathon.gemma_app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.gemma_app.ai.GemmaService;
import com.hackathon.gemma_app.dto.CreateGroupRequest;
import com.hackathon.gemma_app.dto.GemmaRequestDto;
import com.hackathon.gemma_app.exception.InvalidAiResponseException;
import com.hackathon.gemma_app.repository.OrderItemRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShoppingGroupServiceTest {
    @Autowired
    private ShoppingGroupService groupService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GemmaService gemmaService;

    @Test
    void analyzesRequestsAndAggregatesWithoutAskingGemmaForTotals() {
        when(gemmaService.analyze(anyString())).thenReturn(List.of(
                request("Rahul", "I need 3 blue pens for the workshop.", "blue pens", "Blue Pen", 3, false),
                request("Priya", "Can someone get me 2 spiral notebooks?", "spiral notebooks", "Spiral Notebook", 2, false),
                request("Aman", "I need five blue pens too.", "blue pens", "Blue Pen", 5, false),
                request("Neha", "I'll need one spiral notebook.", "spiral notebook", "Spiral Notebook", 1, false),
                request("Rahul", "Also get me one black marker.", "black marker", "Black Marker", 1, false),
                request("Aman", "Actually make that two black markers for me.", "black markers", "Black Marker", 2, false)));

        var group = groupService.create(new CreateGroupRequest("Workshop supplies"));
        var result = groupService.analyze(group.id(), "demo conversation");

        assertEquals(6, result.requestCount());
        assertEquals(3, result.productCount());
        var totals = result.group().items().stream()
                .collect(java.util.stream.Collectors.toMap(item -> item.name(), item -> item.totalQuantity()));
        assertEquals(8, totals.get("Blue Pen"));
        assertEquals(3, totals.get("Spiral Notebook"));
        assertEquals(3, totals.get("Black Marker"));
        assertTrue(result.group().items().stream()
                .filter(item -> item.name().equals("Blue Pen"))
                .flatMap(item -> item.requests().stream())
                .anyMatch(request -> request.originalText().equals("I need 3 blue pens for the workshop.")));
        assertEquals(3, orderItemRepository.findByGroup_IdOrderByNameAsc(group.id()).size());
        verify(gemmaService).analyze("demo conversation");
    }

    @Test
    void analyzeApiValidatesAndReturnsJavaAggregatedTotals() throws Exception {
        when(gemmaService.analyze(anyString())).thenReturn(List.of(
                request("Rahul", "I need 3 blue pens.", "blue pens", "Blue Pen", 3, false),
                request("Aman", "I need 5 blue pens.", "blue pens", "Blue Pen", 5, false)));
        var group = groupService.create(new CreateGroupRequest("API supplies"));

        mockMvc.perform(post("/api/groups/{id}/analyze", group.id())
                        .contentType(APPLICATION_JSON)
                        .content("{\"conversation\":\"Rahul: 3 pens; Aman: 5 pens\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestCount").value(2))
                .andExpect(jsonPath("$.productCount").value(1))
                .andExpect(jsonPath("$.group.items[0].totalQuantity").value(8));
    }

    @Test
    void itemApiSupportsCreateEditCompleteDeleteAndList() throws Exception {
        var group = groupService.create(new CreateGroupRequest("API lifecycle"));
        MvcResult created = mockMvc.perform(post("/api/groups/{id}/items", group.id())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Blue Pens","totalQuantity":2,"unit":"pieces"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Blue Pen"))
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andReturn();
        String itemId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("id").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/items/{id}", itemId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Blue Pen","totalQuantity":4,"unit":"pieces"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(4));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/items/{id}/status", itemId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/groups/{id}/items", group.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Blue Pen"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/items/{id}", itemId))
                .andExpect(status().isNoContent());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/groups/{id}/items", group.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void rejectsInvalidItemQuantitiesAtTheRestBoundary() throws Exception {
        var group = groupService.create(new CreateGroupRequest("Validation"));

        mockMvc.perform(post("/api/groups/{id}/items", group.id())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Blue Pen","totalQuantity":0,"unit":"pieces"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void keepsUnspecifiedQuantitiesAmbiguousAndOutOfTotals() {
        when(gemmaService.analyze(anyString())).thenReturn(List.of(
                new GemmaRequestDto("Rahul", "I need some pens.", "pens", "Pen",
                        null, "pieces", 0.9, true, "Quantity not specified")));
        var group = groupService.create(new CreateGroupRequest("Stationery"));

        var result = groupService.analyze(group.id(), "I need some pens.");

        assertEquals(1, result.ambiguousRequests().size());
        assertEquals(0, result.productCount());
        assertEquals(0, result.group().items().size());
    }

    @Test
    void rejectsNonPositiveQuantitiesReturnedByTheModel() {
        when(gemmaService.analyze(anyString())).thenReturn(List.of(
                request("Rahul", "I need 0 pens.", "pens", "Pen", 0, false)));
        var group = groupService.create(new CreateGroupRequest("Stationery"));

        assertThrows(InvalidAiResponseException.class,
                () -> groupService.analyze(group.id(), "I need 0 pens."));
    }

    @Test
    void rejectsAggregatedQuantitiesThatWouldOverflowTheStoredInteger() {
        when(gemmaService.analyze(anyString())).thenReturn(List.of(
                request("Rahul", "I need pens.", "pens", "Pen", Integer.MAX_VALUE, false),
                request("Aman", "I need one pen.", "pen", "Pen", 1, false)));
        var group = groupService.create(new CreateGroupRequest("Large order"));

        assertThrows(IllegalArgumentException.class,
                () -> groupService.analyze(group.id(), "large order"));
    }

    private GemmaRequestDto request(String person, String source, String item, String normalized,
            Integer quantity, boolean ambiguous) {
        return new GemmaRequestDto(person, source, item, normalized, quantity,
                "pieces", 0.98, ambiguous, null);
    }
}
