package by.vs.erp.crm.repository;

import by.vs.erp.BaseIntegrationTest;
import by.vs.erp.crm.entity.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ClientRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("Должен успешно сохранить клиента и найти его по ID")
    void shouldSaveAndFindClientById() {
        Client client = new Client();
        client.setFirstName("Иван");
        client.setLastName("Иванов");
        client.setPassword("123hadui9");
        client.setPhone("+375291112233");
        client.setRole("CLIENT");

        Client savedClient = clientRepository.save(client);

        assertNotNull(savedClient.getId(), "ID должен быть сгенерирован базой данных");

        Optional<Client> foundClient = clientRepository.findById(savedClient.getId());
        assertTrue(foundClient.isPresent());
        assertEquals("+375291112233", foundClient.get().getPhone());
    }

    @Test
    @DisplayName("Должен найти клиента по уникальному номеру телефона")
    void shouldFindClientByPhone() {
        Client client = new Client();
        client.setFirstName("Петр");
        client.setLastName("Петров");
        client.setPhone("+375294445566");
        client.setRole("CLIENT");
        client.setPassword("123hadui9");
        clientRepository.save(client);

        Optional<Client> foundClient = clientRepository.findByPhone("+375294445566");

        assertTrue(foundClient.isPresent());
        assertEquals("Петров", foundClient.get().getLastName());
    }

    @Test
    @DisplayName("Должен выбросить исключение при попытке сохранить дубликат телефона")
    void shouldThrowExceptionWhenPhoneIsNotUnique() {
        Client client1 = new Client();
        client1.setFirstName("Иван");
        client1.setLastName("Иванов");
        client1.setPhone("+375297778899");
        client1.setPassword("123hadui9");
        client1.setRole("CLIENT");
        clientRepository.saveAndFlush(client1); // Сохраняем и принудительно отправляем в БД

        Client client2 = new Client();
        client2.setFirstName("Алексей");
        client2.setLastName("Сидоров");
        client2.setPassword("123hadui9");
        client2.setRole("CLIENT");
        client2.setPhone("+375297778899"); // Такой же телефон

        assertThrows(DataIntegrityViolationException.class, () -> {
            clientRepository.saveAndFlush(client2);
        }, "База данных должна отклонить дубликат из-за UNIQUE констреинта");
    }
}
