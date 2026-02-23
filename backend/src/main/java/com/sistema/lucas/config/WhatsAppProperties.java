package com.sistema.lucas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppProperties(
		String apiUrl,
		String apiKey,
		String instanceName
) {
}

