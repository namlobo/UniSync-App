package unisync.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import service.ResourceService;
import service.StudentService;
import service.TransactionService;
import service.AdminService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UniSyncApiController.class)
@Import({StudentService.class, ResourceService.class, TransactionService.class, AdminService.class})
class UniSyncApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void health_returnsOkText() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("UniSync API Running"));
    }
}
