package io.watermelon.ci.git.spi;

import java.util.List;
import java.util.Optional;

public interface GitHostingClient {

    RemoteRepository createRepository(String owner, String name, String description, boolean isPrivate);

    Optional<RemoteRepository> findRepository(String owner, String name);

    List<RemoteRepository> listRepositories(String owner);

    Optional<String> readFile(String owner, String name, String path, String ref);

    List<RemoteFile> listFiles(String owner, String name, String path, String ref);

    /** Create or update file; returns content sha / commit id when available. */
    WriteResult writeFile(String owner, String name, String path, String content, String message, String ref, String sha);

    record RemoteRepository(String owner, String name, String cloneUrl, String webUrl, String defaultBranch) {}

    record RemoteFile(String path, String type, long size, String sha) {
        public RemoteFile(String path, String type, long size) {
            this(path, type, size, "");
        }
    }

    record WriteResult(String path, String sha, String commitMessage) {}
}
