package com.flytrap.rssreader.presentation.dto;

import java.util.List;

@Deprecated
public record NameSearch(String name) {

    public record Result(List<MemberSummary> memberSummary) {
    }
}
