package by.vs.erp; // Укажите ваш правильный пакет

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(MockInfrastructureConfig.class)
public abstract class BaseIntegrationTest {
}
