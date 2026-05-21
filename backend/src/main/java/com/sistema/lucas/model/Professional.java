// backend/src/main/java/com/sistema/lucas/model/Professional.java
package com.sistema.lucas.model;

import com.sistema.lucas.config.jpa.EncryptionConverter;
import com.sistema.lucas.model.enums.ModalidadeAtendimento;
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

    @Convert(converter = EncryptionConverter.class)
    private String cpf;
    @Convert(converter = EncryptionConverter.class)
    private String phone;
    private java.time.LocalDate birthDate;
    private String gender;
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_atendimento")
    private ModalidadeAtendimento modalidadeAtendimento = ModalidadeAtendimento.PRESENCIAL;

    @PrePersist
    @PreUpdate
    public void normalizeProfessional() {
        if (this.registroConselho != null) {
            this.registroConselho = this.registroConselho.trim().toUpperCase();
        }
        if (this.specialty != null) {
            this.specialty = this.specialty.trim();
        }
    }
}