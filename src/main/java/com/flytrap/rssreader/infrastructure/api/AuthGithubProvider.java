package com.flytrap.rssreader.infrastructure.api;

import com.flytrap.rssreader.infrastructure.api.dto.AccessToken;
import com.flytrap.rssreader.infrastructure.api.dto.UserEmailResource;
import com.flytrap.rssreader.infrastructure.api.dto.UserResource;
import com.flytrap.rssreader.infrastructure.properties.OauthProperties;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public record AuthGithubProvider(WebClient githubAuthorizationServer,
                                 WebClient githubResourceServer,
                                 WebClient githubResourceEmailServer,
                                 OauthProperties oauthProperties) implements AuthProvider {

    @Override
    public Mono<AccessToken> requestAccessToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", oauthProperties.github().clientId());
        formData.add("client_secret", oauthProperties.github().clientSecret());

        return githubAuthorizationServer
            .post()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .onStatus(status -> status.is4xxClientError()
                    || status.is5xxServerError()
                , clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .map(body -> new Exception("exception"))) // TODO 외부 API 오류시 처리
            .bodyToMono(AccessToken.class);
    }

    @Override
    public Mono<UserResource> requestUserResource(AccessToken accessToken) {

        Mono<List<UserEmailResource>> userEmailResource = getEmailResource(accessToken);

        return githubResourceServer
            .get()
            .header(HttpHeaders.AUTHORIZATION, accessToken.getHeadValue())
            .header("scope", "user:email")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(status -> status.is4xxClientError()
                    || status.is5xxServerError()
                , clientResponse ->
                    clientResponse.bodyToMono(String.class).flatMap(body -> {
                        log.error(">>>> Error response from GitHub: Status Code: {}, Body: {}",
                            clientResponse.statusCode(), body);
                        return Mono.error(
                            new Exception("Exception due to error response from server"));
                    })) // TODO 외부 API 오류시 처리
            .bodyToMono(UserResource.class)
            .publishOn(Schedulers.boundedElastic())
            .map(userResource -> {
                Objects.requireNonNull(userEmailResource.block()).stream()
                    .filter(UserEmailResource::primary).findFirst()
                    .ifPresent(userResource::updateEmail);
                return userResource;
            });
    }

    private Mono<List<UserEmailResource>> getEmailResource(AccessToken accessToken) {
        return githubResourceEmailServer
            .get()
            .header(HttpHeaders.AUTHORIZATION, accessToken.getHeadValue())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(status -> status.is4xxClientError()
                    || status.is5xxServerError()
                , clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .map(body -> new Exception(
                            "exception"))) // TODO 외부 API 오류시 처리
            .bodyToMono(new ParameterizedTypeReference<List<UserEmailResource>>() {
            });
    }

}
