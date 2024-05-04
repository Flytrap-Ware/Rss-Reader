package com.flytrap.rssreader.api.post.presentation.controller;

import com.flytrap.rssreader.api.auth.presentation.dto.AccountCredentials;
import com.flytrap.rssreader.api.folder.domain.FolderId;
import com.flytrap.rssreader.api.member.domain.AccountId;
import com.flytrap.rssreader.api.post.business.service.PostListReadService;
import com.flytrap.rssreader.api.post.domain.PostFilter;
import com.flytrap.rssreader.api.post.presentation.controller.swagger.PostListReadControllerApi;
import com.flytrap.rssreader.api.post.presentation.dto.response.PostResponse;
import com.flytrap.rssreader.api.subscribe.domain.SubscriptionId;
import com.flytrap.rssreader.global.model.ApplicationResponse;
import com.flytrap.rssreader.global.presentation.resolver.Login;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostListReadController implements PostListReadControllerApi {

    private final PostListReadService postListReadService;

    @GetMapping("/posts")
    public ApplicationResponse<PostResponse.PostListResponse> getPostsByAccount(
        PostFilter postFilter, // TODO: filter 가 request 부터 repository까지 계속 전달됨
        @PageableDefault(page = 0, size = 15) Pageable pageable,
        @Login AccountCredentials accountCredentials) {

        List<PostResponse> posts = postListReadService.getPostsByAccount(
                new AccountId(accountCredentials.id().value()), postFilter, pageable)
            .stream()
            .map(PostResponse::from)
            .toList();

        return new ApplicationResponse<>(
            new PostResponse.PostListResponse(posts));
    }

    @GetMapping("/folders/{folderId}/posts")
    public ApplicationResponse<PostResponse.PostListResponse> getPostsByFolder(
        @PathVariable Long folderId,
        PostFilter postFilter, // TODO: filter 가 request 부터 repository까지 계속 전달됨
        @PageableDefault(page = 0, size = 15) Pageable pageable,
        @Login AccountCredentials accountCredentials) {

        List<PostResponse> posts = postListReadService.getPostsByFolder(
                new AccountId(accountCredentials.id().value()), new FolderId(folderId), postFilter, pageable)
            .stream()
            .map(PostResponse::from)
            .toList();

        return new ApplicationResponse<>(
            new PostResponse.PostListResponse(posts));
    }

    @GetMapping("/subscriptions/{subscriptionId}/posts")
    public ApplicationResponse<PostResponse.PostListResponse> getPostsBySubscription(
        @PathVariable Long subscriptionId,
        PostFilter postFilter, // TODO: filter 가 request 부터 repository까지 계속 전달됨
        @PageableDefault(page = 0, size = 15) Pageable pageable,
        // TODO: pageable 도 마찬가지. service 에서 만들면 됨
        @Login AccountCredentials accountCredentials) {

        List<PostResponse> posts = postListReadService.getPostsBySubscription(
                new AccountId(accountCredentials.id().value()), new SubscriptionId(subscriptionId),
                postFilter, pageable)
            .stream()
            .map(PostResponse::from)
            .toList();

        return new ApplicationResponse<>(
            new PostResponse.PostListResponse(posts));
    }

    @GetMapping("/bookmarks")
    public ApplicationResponse<PostResponse.PostListResponse> getBookmarkedPosts(
        PostFilter postFilter,
        @PageableDefault(page = 0, size = 15) Pageable pageable,
        @Login AccountCredentials accountCredentials
    ) {

        List<PostResponse> posts = postListReadService.getBookmarkedPosts(
                new AccountId(accountCredentials.id().value()), postFilter, pageable)
            .stream()
            .map(PostResponse::from)
            .toList();

        return new ApplicationResponse<>(
            new PostResponse.PostListResponse(posts));
    }
}
