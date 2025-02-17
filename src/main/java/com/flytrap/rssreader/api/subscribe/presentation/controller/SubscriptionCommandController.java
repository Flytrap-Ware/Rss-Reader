package com.flytrap.rssreader.api.subscribe.presentation.controller;

import com.flytrap.rssreader.api.auth.presentation.dto.AccountCredentials;
import com.flytrap.rssreader.api.folder.domain.FolderId;
import com.flytrap.rssreader.api.subscribe.business.service.SubscriptionCommandService;
import com.flytrap.rssreader.api.subscribe.domain.Subscription;
import com.flytrap.rssreader.api.subscribe.domain.SubscriptionId;
import com.flytrap.rssreader.api.subscribe.presentation.controller.swagger.SubscriptionCommandControllerApi;
import com.flytrap.rssreader.api.subscribe.presentation.dto.AddSubscriptionRequest;
import com.flytrap.rssreader.api.subscribe.presentation.dto.SubscriptionResponse;
import com.flytrap.rssreader.global.model.ApplicationResponse;
import com.flytrap.rssreader.global.presentation.resolver.Login;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubscriptionCommandController implements SubscriptionCommandControllerApi {

    private final SubscriptionCommandService subscriptionCommandService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/folders/{folderId}/subscriptions")
    public ApplicationResponse<SubscriptionResponse> addSubscribeToFolder(
        @PathVariable Long folderId,
        @Valid @RequestBody AddSubscriptionRequest request,
        @Login AccountCredentials accountCredentials
    ) {
        Subscription subscription = subscriptionCommandService
            .addSubscriptionToFolder(
                accountCredentials.id(),
                new FolderId(folderId),
                request.blogUrl()
            );

        return new ApplicationResponse<>(SubscriptionResponse.from(subscription));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/folders/{folderId}/subscriptions/{subscriptionId}")
    public ApplicationResponse<Void> removeSubscriptionToFolder(
        @PathVariable Long folderId,
        @PathVariable Long subscriptionId,
        @Login AccountCredentials accountCredentials
    ) {
        subscriptionCommandService.removeSubscriptionToFolder(
            accountCredentials.id(),
            new FolderId(folderId),
            new SubscriptionId(subscriptionId)
        );

        return new ApplicationResponse<>(null);
    }
}
