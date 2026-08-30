package by.vs.erp.crm.mapper;

import by.vs.erp.crm.dto.ClientDto;
import by.vs.erp.crm.dto.ClientReadDto;
import by.vs.erp.crm.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "role", ignore = true)
    Client toEntity(ClientDto clientDto);

    ClientReadDto toDto(Client client);
}
