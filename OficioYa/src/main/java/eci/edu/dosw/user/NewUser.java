package eci.edu.dosw.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import eci.edu.dosw.orchestrator.dto.PhoneDTO;
import java.util.List;

/**
 * Datos para crear un perfil en el modulo User (POST /api/v1/users).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NewUser(String name, String email, List<PhoneDTO> phones, List<String> roles, String birthDate, Integer localityId) {

    public NewUser(String name, String email, List<PhoneDTO> phones, List<String> roles, String birthDate) {
        this(name, email, phones, roles, birthDate, null);
    }

    public NewUser(String name, String email, List<PhoneDTO> phones, List<String> roles) {
        this(name, email, phones, roles, null, null);
    }
}
