package com.faculdade.estandetiro.model.service.ia;

/** Bytes da imagem devolvida pela IA e o tipo (ex.: image/png). */
public record ImagemGerada(byte[] bytes, String mimeType) {
}
