package com.flytrap.rssreader.api.post.infrastructure.output;

public interface PostSubscribeCountOutput {

    long getSubscriptionId();

    int getPostCount();
}
