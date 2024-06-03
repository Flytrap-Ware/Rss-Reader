package com.flytrap.rssreader.api.post.infrastructure.system;

import com.flytrap.rssreader.api.admin.infrastructure.system.PostCollectionThreadPoolExecutor;
import com.flytrap.rssreader.api.alert.business.event.NewPostAlertEvent;
import com.flytrap.rssreader.api.parser.RssPostParser;
import com.flytrap.rssreader.api.parser.dto.RssPostsData;
import com.flytrap.rssreader.api.post.infrastructure.entity.PostEntity;
import com.flytrap.rssreader.api.post.infrastructure.entity.PostSystemEntity;
import com.flytrap.rssreader.api.post.infrastructure.repository.PostJpaRepository;
import com.flytrap.rssreader.api.post.infrastructure.repository.PostSystemJpaRepository;
import com.flytrap.rssreader.api.subscribe.infrastructure.entity.RssSourceEntity;
import com.flytrap.rssreader.api.subscribe.infrastructure.repository.RssResourceJpaRepository;
import com.flytrap.rssreader.global.event.GlobalEventPublisher;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCollectSystem {

    private final SubscribeCollectionPriorityQueue collectionQueue;
    private final RssResourceJpaRepository rssResourceRepository;
    private final PostJpaRepository postRepository;
    private final PostSystemJpaRepository postSystemJpaRepository;
    private final RssPostParser postParser;
    private final GlobalEventPublisher globalEventPublisher;
    private final PostCollectionThreadPoolExecutor postCollectionThreadPoolExecutor;

    public void loadAndEnqueueRssResources(int selectBatchSize) {
        var pageable = PageRequest.of(
            0, selectBatchSize,
            Sort.by(Sort.Direction.ASC, "lastCollectedAt"));
        var rssResources =
            rssResourceRepository.findAll(pageable).getContent();

        collectionQueue.addAll(rssResources, CollectPriority.LOW);
    }

    public void dequeueAndSaveRssResource() {
        Instant start = Instant.now();
        List<CompletableFuture<CollectionResult>> futures = new ArrayList<>();

        while (!collectionQueue.isQueueEmpty()) {

            RssSourceEntity rssResource = collectionQueue.poll();

            CompletableFuture<CollectionResult> future = postCollectionThreadPoolExecutor.supplyAsync(
                () -> postParser.parseRssDocuments(rssResource.getUrl())
                    .map(rssPostsData -> {
                        List<PostEntity> postEntities = postRepository.saveAll(
                            generateCollectedPostsForUpsert(rssPostsData, rssResource));
                        rssResource.updateTitle(rssPostsData.rssSourceTitle());
                        rssResource.updateLastCollectedAt(start);
                        rssResourceRepository.save(rssResource);

                        return new CollectionResult(1, postEntities.size(), 0);
                    })
                    .orElse(new CollectionResult(0, 0, 1)));

            futures.add(future);
        }

        summarizeAndSaveCollectionResults(start, futures);
    }

    public void enqueueHighPrioritySubscription(RssSourceEntity rssSourceEntity) {
        collectionQueue.add(rssSourceEntity, CollectPriority.HIGH);
    }

    /**
     * 멀티 스레드로 동시 작업한 게시글 수집 결과를 집계하여 DB에 저장한다.
     *
     * @param start 게시글 수집 시작 시간
     * @param futures 게시글 수집 작업의 CompletableFuture 목록
     */
    private void summarizeAndSaveCollectionResults(
        Instant start, List<CompletableFuture<CollectionResult>> futures
    ) {
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        allOf.thenApply(voidResult -> futures.stream()
                .map(CompletableFuture::join)
                .reduce(new CollectionResult(0, 0, 0), (result1, result2) ->
                    new CollectionResult(
                        result1.getRssCount() + result2.getRssCount(),
                        result1.getPostCount() + result2.getPostCount(),
                        result1.getParsingFailureCount() + result2.getParsingFailureCount()
                    )
                ))
            .thenAccept(collectionResult -> {
                Instant end = Instant.now();

                postSystemJpaRepository.save(
                    PostSystemEntity.builder()
                        .startTime(start)
                        .endTime(end)
                        .elapsedTimeMillis(Duration.between(start, end).toMillis())
                        .rssCount(collectionResult.getRssCount())
                        .postCount(collectionResult.getPostCount())
                        .threadCount(postCollectionThreadPoolExecutor.getCorePoolSize())
                        .parsingFailureCount(collectionResult.getParsingFailureCount())
                        .build()
                );
            }).join();
    }

    private List<PostEntity> generateCollectedPostsForUpsert(RssPostsData postData,
        RssSourceEntity rssResource) {
        List<PostEntity> existingPosts = postRepository
            .findAllByRssSourceId(rssResource.getId());

        Map<String, PostEntity> existingPostsMap = convertListToHashSet(existingPosts);
        List<PostEntity> collectedPosts = new ArrayList<>();
        List<PostEntity> newPosts = new ArrayList<>();

        for (RssPostsData.RssItemData itemData : postData.itemData()) {
            PostEntity post;

            if (existingPostsMap.containsKey(itemData.guid())) {
                post = existingPostsMap.get(itemData.guid());
                post.updateBy(itemData);
            } else {
                post = PostEntity.from(itemData, rssResource.getId());
                newPosts.add(post);
            }
            collectedPosts.add(post);
        }

        if (!newPosts.isEmpty()) {
            globalEventPublisher.publish(new NewPostAlertEvent(rssResource, newPosts));
        }

        return collectedPosts;
    }

    private static Map<String, PostEntity> convertListToHashSet(
        List<PostEntity> postEntities
    ) {
        Map<String, PostEntity> map = new HashMap<>();

        for (PostEntity postEntity : postEntities) {
            map.put(postEntity.getGuid(), postEntity);
        }

        return Collections.unmodifiableMap(map);
    }

}
