// backend/src/main/java/com/sistema/lucas/model/Documento.java
package com.sistema.lucas.model;

import com.sistema.lucas.config.jpa.EncryptionConverter;
import com.sistema.lucas.model.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipo;

    // Pode conter diagnóstico (ex.: "Laudo — Transtorno Depressivo Maior") — mesma categoria
    // de dado sensível que conteudoTexto/arquivoBase64. Coluna alargada pra TEXT (migration
    // V19) porque o valor cifrado é maior que VARCHAR(255) comporta.
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String titulo;

    // Para documentos digitados diretamente
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String conteudoTexto;

    // Para upload de PDF — armazena o nome original do arquivo. Cifrado pelo mesmo motivo
    // que titulo (nome de arquivo pode ser tão revelador quanto o título).
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String nomeArquivo;

    // Conteúdo do PDF em Base64
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String arquivoBase64;

    @ManyToOne
    @JoinColumn(name = "paciente_id")
    private Patient paciente;

    @ManyToOne
    @JoinColumn(name = "profissional_id")
    private Professional profissional;

    private LocalDateTime criadoEm;

    // true = visível para o paciente, false = ainda em rascunho
    private boolean disponivel = false;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }
}