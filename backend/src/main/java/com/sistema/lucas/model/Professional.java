// backend/src/main/java/com/sistema/lucas/model/Professional.java
package com.sistema.lucas.model;

import com.sistema.lucas.model.enums.TipoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Professional extends User {

    @Enumerated(EnumType.STRING)
    private TipoRegistro tipoRegistro; // ✅ CRM, CRP ou OUTRO

    private String registroConselho;   // ✅ era crm — número do CRM/CRP

    private String specialty;
}