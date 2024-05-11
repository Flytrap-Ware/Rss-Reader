package com.flytrap.rssreader.api.subscribe.presentation.controller;

import com.flytrap.rssreader.api.auth.presentation.dto.AccountCredentials;
import com.flytrap.rssreader.api.folder.domain.FolderId;
import com.flytrap.rssreader.api.subscribe.business.service.SubscriptionService;
import com.flytrap.rssreader.api.subscribe.domain.FolderSubscription;
import com.flytrap.rssreader.api.subscribe.presentation.controller.swagger.SubscriptionControllerApi;
import com.flytrap.rssreader.api.subscribe.presentation.dto.AddSubscriptionRequest;
import com.flytrap.rssreader.api.subscribe.presentation.dto.SubscriptionResponse;
import com.flytrap.rssreader.global.model.ApplicationResponse;
import com.flytrap.rssreader.global.presentation.resolver.Login;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionControllerApi {

    private final SubscriptionService subscriptionService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/folders/{folderId}/rss")
    public ApplicationResponse<SubscriptionResponse> addSubscribeToFolder(
        @PathVariable Long folderId,
        @Valid @RequestBody AddSubscriptionRequest request,
        @Login AccountCredentials accountCredentials
    ) {
        FolderSubscription folderSubscription = subscriptionService
            .addSubscriptionToFolder(
                accountCredentials.id(),
                new FolderId(folderId),
                request.blogUrl()
            );

        return new ApplicationResponse<>(SubscriptionResponse.from(folderSubscription));
    }
}
