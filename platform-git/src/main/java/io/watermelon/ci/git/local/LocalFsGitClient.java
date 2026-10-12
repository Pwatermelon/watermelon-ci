package io.watermelon.ci.git.local;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.git.config.GitProperties;
import io.watermelon.ci.git.spi.GitHostingClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Локальное хранилище репозиториев для демо/редактора без Forgejo.
 * Корень: watermelon.git.local-root (по умолчанию .data/repos).
 */
@Component
@ConditionalOnProperty(name = "watermelon.git.provider", havingValue = "local", matchIfMissing = true)
public class LocalFsGitClient implements GitHostingClient {

    private final Path root;

    public LocalFsGitClient(GitProperties properties) {
        this.root = Path.of(properties.getLocalRoot() == null ? ".data/repos" : properties.getLocalRoot())
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "cannot init local git root: " + ex.getMessage(), ex);
        }
    }

    private Path repoDir(String owner, String name) {
        return root.resolve(owner).resolve(name);
    }

    @Override
    public RemoteRepository createRepository(String owner, String name, String description, boolean isPrivate) {
        try {
            Path dir = repoDir(owner, name);
            Files.createDirectories(dir);
            Path readme = dir.resolve("README.md");
            if (!Files.exists(readme)) {
                Files.writeString(readme, "# " + name + "\n\n" + (description == null ? "" : description) + "\n", StandardCharsets.UTF_8);
            }
            return toRepo(owner, name);
        } catch (IOException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "create repository failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<RemoteRepository> findRepository(String owner, String name) {
        Path dir = repoDir(owner, name);
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        return Optional.of(toRepo(owner, name));
    }

    @Override
    public List<RemoteRepository> listRepositories(String owner) {
        Path ownerDir = root.resolve(owner);
        if (!Files.isDirectory(ownerDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(ownerDir)) {
            return stream.filter(Files::isDirectory)
                    .map(p -> toRepo(owner, p.getFileName().toString()))
                    .sorted(Comparator.comparing(RemoteRepository::name))
                    .toList();
        } catch (IOException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "list repositories failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<String> readFile(String owner, String name, String path, String ref) {
        Path file = resolve(owner, name, path);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "read file failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<RemoteFile> listFiles(String owner, String name, String path, String ref) {
        Path base = resolve(owner, name, path == null ? "" : path);
        if (!Files.isDirectory(base)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(base)) {
            List<RemoteFile> out = new ArrayList<>();
            Path repo = repoDir(owner, name);
            for (Path p : stream.sorted().toList()) {
                String rel = repo.relativize(p).toString().replace('\\', '/');
                String type = Files.isDirectory(p) ? "dir" : "file";
                long size = Files.isRegularFile(p) ? Files.size(p) : 0L;
                out.add(new RemoteFile(rel, type, size, shaOf(p)));
            }
            return out;
        } catch (IOException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "list files failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public WriteResult writeFile(
            String owner, String name, String path, String content, String message, String ref, String sha) {
        try {
            Path file = resolve(owner, name, path);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
            Path log = repoDir(owner, name).resolve(".wm-commits.log");
            Files.writeString(
                    log,
                    InstantNow() + " " + (message == null ? "update " + path : message) + "\n",
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            return new WriteResult(path, shaOf(file), message == null ? "update" : message);
        } catch (IOException ex) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "write file failed: " + ex.getMessage(), ex);
        }
    }

    private Path resolve(String owner, String name, String path) {
        Path repo = repoDir(owner, name);
        Path target = (path == null || path.isBlank()) ? repo : repo.resolve(path).normalize();
        if (!target.startsWith(repo)) {
            throw new PlatformException(ErrorCode.GIT_ERROR, "path escapes repository");
        }
        return target;
    }

    private RemoteRepository toRepo(String owner, String name) {
        return new RemoteRepository(owner, name, "file://" + repoDir(owner, name), "/app/repos/" + owner + "/" + name, "main");
    }

    private static String shaOf(Path p) {
        try {
            if (!Files.isRegularFile(p)) {
                return "";
            }
            byte[] bytes = Files.readAllBytes(p);
            return Integer.toHexString(java.util.Arrays.hashCode(bytes));
        } catch (IOException ex) {
            return "";
        }
    }

    private static String InstantNow() {
        return java.time.Instant.now().toString();
    }
}
