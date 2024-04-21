package com.flytrap.rssreader.fixture;

import com.flytrap.rssreader.api.folder.domain.Folder;
import com.flytrap.rssreader.api.folder.infrastructure.entity.FolderEntity;
import com.flytrap.rssreader.api.account.domain.Account;

public class FolderFixtureFactory {

    public static class FolderFields {
        public static Long id = 1L;
        public static String name = "folderName";
        public static Account member = FixtureFactory.generateAccount();
    }

    public static FolderEntity generateFolderEntity() {
        return FolderEntity.builder()
                .id(FolderFields.id)
                .name(FolderFields.name)
                .memberId(FolderFields.member.getId().value())
                .isShared(false)
                .isDeleted(false)
                .build();
    }

    public static Folder generateFolder() {
        return Folder.builder()
                .id(FolderFields.id)
                .name(FolderFields.name)
                .memberId(FolderFields.member.getId().value())
                .isShared(false)
                .isDeleted(false)
                .build();
    }

    public static Folder generateSharedFolder() {
        return Folder.builder()
                .id(FolderFields.id)
                .name(FolderFields.name)
                .memberId(FolderFields.member.getId().value())
                .isShared(true)
                .isDeleted(false)
                .build();
    }

}
