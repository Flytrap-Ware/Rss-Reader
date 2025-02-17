package com.flytrap.rssreader.global.batch.step;

import com.flytrap.rssreader.api.post.domain.PostBlogPlatformData;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StatItemReader {

    private final EntityManagerFactory emf;

    @Bean
    @StepScope
    public JpaCursorItemReader<PostBlogPlatformData> jpaCursorItemReader() {

        //TODO: 하루 Post가 얼마나 insert 되었는지 확인하는 쿼리
        String className = PostBlogPlatformData.class.getName(); // JPQL 에서 새로운 Entity로 반환하기 위해
        String queryString = String.format(
                "SELECT NEW com.flytrap.rssreader.api.post.domain.PostBlogPlatformData(s.platform, COUNT(*)) " +
                        "FROM PostEntity p " +
                        "JOIN SubscribeEntity s ON s.id = p.subscribe.id " +
                        "GROUP BY s.platform ", className);

        return new JpaCursorItemReaderBuilder<PostBlogPlatformData>()
                .name("jpaCursorItemReader")
                .entityManagerFactory(emf)
                .queryString(queryString)
                .build();
    }
}
