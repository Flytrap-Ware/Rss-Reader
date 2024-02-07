package com.flytrap.rssreader.infrastructure.api;

import com.flytrap.rssreader.global.utill.DateConvertor;
import com.flytrap.rssreader.infrastructure.api.dto.RssSubscribeResource;
import com.flytrap.rssreader.infrastructure.api.dto.RssSubscribeResource.RssItemResource;
import com.flytrap.rssreader.infrastructure.api.dto.RssTag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 구독한 블로그의 RSS 문서에서 게시글을 파싱해 그 결과를 반환하는 RSS 게시글 파서
 */
@Slf4j
@AllArgsConstructor
@Component
public class RssPostParser {

    private static final Predicate<String> IS_RSS_ROOT_TAG = rootTagName
        -> RssTag.RSS.getRootTagName().equals(rootTagName);
    private static final Predicate<String> IS_ATOM_ROOT_TAG = rootTagName
        -> RssTag.ATOM.getRootTagName().equals(rootTagName);

    private static final String RSS_PARSING_ERROR_MESSAGE = "RSS문서를 파싱할 수 없습니다.";

    private final HTMLImageParser htmlImageParser;

    public Optional<RssSubscribeResource> parseRssDocuments(String url) {

        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(url);

            String subscribeTitle;
            List<RssItemResource> itemResources;

            String rootTagName = document.getDocumentElement().getTagName();
            if (IS_RSS_ROOT_TAG.test(rootTagName)) {
                subscribeTitle = extractSubscribeTitleFromRss(document);
                itemResources = extractItemResourcesFromRss(document);
            } else if (IS_ATOM_ROOT_TAG.test(rootTagName)) {
                subscribeTitle = extractSubscribeTitleFromAtom(document);
                itemResources = extractEntryResourcesFromAtom(document);
            } else {
                throw new ParserConfigurationException();
            }

            return Optional.of(new RssSubscribeResource(subscribeTitle, itemResources));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.error(RSS_PARSING_ERROR_MESSAGE);
            return Optional.empty();
        }
    }

    private String extractSubscribeTitleFromRss(Document document) {
        Element chanel = (Element) document.getElementsByTagName(
            RssTag.RssSubscribeTag.CHANNEL.getTagName()).item(0);

        return chanel.getElementsByTagName(RssTag.RssSubscribeTag.TITLE.getTagName()).item(0)
            .getTextContent();
    }

    private String extractSubscribeTitleFromAtom(Document document) {
        Element chanel = (Element) document.getElementsByTagName(
            RssTag.AtomSubscribeTag.FEED.getTagName()).item(0);

        return chanel.getElementsByTagName(RssTag.AtomSubscribeTag.TITLE.getTagName()).item(0)
            .getTextContent();
    }

    private List<RssItemResource> extractItemResourcesFromRss(Document document) {
        List<RssItemResource> itemResources = new ArrayList<>();

        NodeList itemList = document.getElementsByTagName(RssTag.RssSubscribeTag.ITEM.getTagName());

        for (int i = 0; i < itemList.getLength(); i++) {
            Node node = itemList.item(i);

            String description = getTagValue(node, RssTag.RssItemTag.DESCRIPTION.getTagName());
            if (description.isEmpty()) {
                description = getTagValue(node, RssTag.RssItemTag.CONTENT.getTagName());
            }

            itemResources.add(
                new RssItemResource(
                    getTagValue(node, RssTag.RssItemTag.GUID.getTagName()),
                    getTagValue(node, RssTag.RssItemTag.TITLE.getTagName()),
                    description,
                    DateConvertor.convertToInstant(
                        getTagValue(node, RssTag.RssItemTag.PUB_DATE.getTagName())),
                    htmlImageParser.extractImageUrl(description)
                )
            );
        }

        return itemResources;
    }

    private List<RssItemResource> extractEntryResourcesFromAtom(Document document) {
        List<RssItemResource> itemResources = new ArrayList<>();

        NodeList itemList = document.getElementsByTagName(
            RssTag.AtomSubscribeTag.ENTRY.getTagName());

        for (int i = 0; i < itemList.getLength(); i++) {
            Node node = itemList.item(i);

            String description = getTagValue(node, RssTag.AtomEntryTag.CONTENT.getTagName());

            itemResources.add(
                new RssItemResource(
                    getTagValue(node, RssTag.AtomEntryTag.ID.getTagName()),
                    getTagValue(node, RssTag.AtomEntryTag.TITLE.getTagName()),
                    description,
                    DateConvertor.convertToInstant(
                        getTagValue(node, RssTag.AtomEntryTag.UPDATED.getTagName())),
                    htmlImageParser.extractImageUrl(description)
                )
            );
        }

        return itemResources;
    }

    private String getTagValue(Node parentNode, String tagName) {
        Element element = (Element) parentNode;
        Node item = element.getElementsByTagName(tagName).item(0);

        return (item != null) ? item.getTextContent() : "";
    }

}
