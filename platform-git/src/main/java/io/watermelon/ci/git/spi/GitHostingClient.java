package io.watermelon.ci.git.spi;

import java.util.List;
import java.util.Optional;

public interface GitHostingClient {

    RemoteRepository createRepository(String owner, String name, String description, boolean isPrivate);

    Optional<RemoteRepository> findRepository(String owner, String name);

    Optional<String> readFile(String owner, String name, String path, String ref);

    List<RemoteFile> listFiles(String owner, String name, String path, String ref);

    record RemoteRepository(String owner, String name, String cloneUrl, String webUrl, String defaultBranch) {}

    record RemoteFile(String path, String type, long size) {}
}
