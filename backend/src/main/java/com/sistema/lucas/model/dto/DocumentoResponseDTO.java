// backend/src/main/java/com/sistema/lucas/model/dto/DocumentoResponseDTO.java
package com.sistema.lucas.model.dto;

import com.sistema.lucas.model.Documento;
import com.sistema.lucas.model.enums.TipoDocumento;
import java.time.LocalDateTime;

public record DocumentoResponseDTO(
    Long id,
    TipoDocumento tipo,
    String titulo,
    String conteudoTexto,
    String nomeArquivo,
    String arquivoBase64,
    String nomePaciente,
    String nomeProfissional,
    LocalDateTime criadoEm,
    boolean disponivel
) {
    public DocumentoResponseDTO(Documento d) {
        this(
            d.getId(),
            d.getTipo(),
            d.getTitulo(),
            d.getConteudoTexto(),
            d.getNomeArquivo(),
            d.getArquivoBase64(),
            d.getPaciente().getName(),
            d.getProfissional().getName(),
            d.getCriadoEm(),
            d.isDisponivel()
        );
    }
}