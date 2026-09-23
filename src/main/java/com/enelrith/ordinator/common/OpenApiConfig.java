package com.enelrith.ordinator.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Ordinator API",
                version = "v1"
        )
)
public class OpenApiConfig {
        @Bean
        OpenApiCustomizer authEndpoints() {
                return openApi -> {
                        final String emailLabel = "email";
                        final String passwordLabel = "password";
                        var loginSchema = new ObjectSchema();

                        loginSchema.addProperty(emailLabel, new StringSchema()
                                .format("email")
                                .description("Account email")
                        );

                        loginSchema.addProperty(passwordLabel, new StringSchema()
                                .format("password")
                                .writeOnly(true)
                                .description("Account password")
                        );

                        loginSchema.setRequired(List.of(emailLabel, passwordLabel));

                        openApi.path("/api/auth/login", new PathItem().post(new Operation()
                                .addTagsItem("Authentication")
                                .summary("Authenticates the user using email and password credentials and creates an HTTP session")
                                .requestBody(new RequestBody()
                                        .required(true)
                                        .content(new Content().addMediaType(MediaType.APPLICATION_FORM_URLENCODED_VALUE, new io.swagger.v3.oas.models.media.MediaType()
                                                                .schema(loginSchema))))
                                .responses(new ApiResponses()
                                        .addApiResponse("204", new ApiResponse().description("Authenticated"))
                                        .addApiResponse("401", new ApiResponse().description("Invalid credentials"))
                                        .addApiResponse("403", new ApiResponse().description("Missing or invalid CSRF token"))
                                ))
                        );

                        openApi.path("/api/auth/logout", new PathItem().post(new Operation()
                                .addTagsItem("Authentication")
                                .summary("Invalidates the current HTTP session and deletes the CSRF cookie")
                                .responses(new ApiResponses()
                                        .addApiResponse("204", new ApiResponse().description("Logged out"))
                                        .addApiResponse("403", new ApiResponse().description("Missing or invalid CSRF token"))
                                ))
                        );
                };
        }
}
