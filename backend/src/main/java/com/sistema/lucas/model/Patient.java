package com.sistema.lucas.model;

import java.time.LocalDate;

import com.sistema.lucas.config.jpa.EncryptionConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Patient extends User { // Herança estabelecida aqui
    @Convert(converter = EncryptionConverter.class)
    private String cpf;
    @Convert(converter = EncryptionConverter.class)
    private String phone;

    private LocalDate birthDate;
    @Convert(converter = EncryptionConverter.class)
    private String emergencyContactName;
    @Convert(converter = EncryptionConverter.class)
    private String emergencyContactPhone;
    private String gender;
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String allergies;
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String address;

    private java.time.LocalDateTime blockedUntil;

    @PrePersist
    @PreUpdate
    public void normalizePatient() {
        if (this.cpf != null) {
            String c = this.cpf.replaceAll("[^0-9]", "");
            if (c.length() == 11) {
                this.cpf = c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-" + c.substring(9, 11);
            } else {
                this.cpf = c;
            }
        }
        if (this.phone != null) {
            String p = this.phone.replaceAll("[^0-9]", "");
            if (p.length() == 11) {
                this.phone = "(" + p.substring(0, 2) + ") " + p.substring(2, 7) + "-" + p.substring(7, 11);
            } else if (p.length() == 10) {
                this.phone = "(" + p.substring(0, 2) + ") " + p.substring(2, 6) + "-" + p.substring(6, 10);
            } else {
                this.phone = p;
            }
        }
        if (this.emergencyContactPhone != null) {
            String ep = this.emergencyContactPhone.replaceAll("[^0-9]", "");
            if (ep.length() == 11) {
                this.emergencyContactPhone = "(" + ep.substring(0, 2) + ") " + ep.substring(2, 7) + "-" + ep.substring(7, 11);
            } else if (ep.length() == 10) {
                this.emergencyContactPhone = "(" + ep.substring(0, 2) + ") " + ep.substring(2, 6) + "-" + ep.substring(6, 10);
            } else {
                this.emergencyContactPhone = ep;
            }
        }
    }
}