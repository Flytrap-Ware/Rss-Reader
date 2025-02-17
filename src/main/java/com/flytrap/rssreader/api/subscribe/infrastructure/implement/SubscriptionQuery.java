package com.flytrap.rssreader.api.subscribe.infrastructure.implement;

import com.flytrap.rssreader.api.folder.domain.FolderId;
import com.flytrap.rssreader.api.subscribe.domain.Subscription;
import com.flytrap.rssreader.api.subscribe.domain.SubscriptionId;
import com.flytrap.rssreader.api.subscribe.infrastructure.output.SubscriptionOutput;
import com.flytrap.rssreader.api.subscribe.infrastructure.repository.SubscriptionDslRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionQuery {

    private final SubscriptionDslRepository subscriptionDslRepository;

    public Optional<Subscription> read(SubscriptionId subscriptionId) {
        return subscriptionDslRepository
            .findById(subscriptionId.value())
            .map(SubscriptionOutput::toReadOnly);
    }

    public List<Subscription> readAllByFolder(FolderId folderId) {
        return subscriptionDslRepository
            .findAllByFolder(folderId.value())
            .stream()
            .map(SubscriptionOutput::toReadOnly)
            .toList();
    }
}
