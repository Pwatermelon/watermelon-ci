package io.watermelon.ci.api.web;

import io.watermelon.ci.git.spi.GitHostingClient;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/editor")
public class CodeEditorController {

    private final GitHostingClient gitHostingClient;

    public CodeEditorController(GitHostingClient gitHostingClient) {
        this.gitHostingClient = gitHostingClient;
    }

    public record WriteFileRequest(
            @NotBlank String path,
            String content,
            String message,
            String ref,
            String sha) {}

    public record CreateRepoRequest(String description, Boolean isPrivate) {}

    @GetMapping("/orgs/{owner}/repos")
    public List<GitHostingClient.RemoteRepository> listRepos(@PathVariable String owner) {
        return gitHostingClient.listRepositories(owner);
    }

    @PostMapping("/orgs/{owner}/repos/{repo}")
    public GitHostingClient.RemoteRepository createRepo(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestBody(required = false) CreateRepoRequest body) {
        String desc = body == null || body.description() == null ? "" : body.description();
        boolean priv = body != null && Boolean.TRUE.equals(body.isPrivate());
        return gitHostingClient
                .findRepository(owner, repo)
                .orElseGet(() -> gitHostingClient.createRepository(owner, repo, desc, priv));
    }

    @GetMapping("/repos/{owner}/{repo}/tree")
    public List<GitHostingClient.RemoteFile> tree(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "") String path,
            @RequestParam(defaultValue = "main") String ref) {
        return gitHostingClient.listFiles(owner, repo, path, ref);
    }

    @GetMapping("/repos/{owner}/{repo}/file")
    public Map<String, String> file(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String path,
            @RequestParam(defaultValue = "main") String ref) {
        String content = gitHostingClient
                .readFile(owner, repo, path, ref)
                .orElseThrow(() -> new io.watermelon.ci.common.error.NotFoundException("file not found: " + path));
        return Map.of("path", path, "ref", ref, "content", content);
    }

    @PutMapping("/repos/{owner}/{repo}/file")
    public GitHostingClient.WriteResult writeFile(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestBody WriteFileRequest body) {
        String ref = body.ref() == null || body.ref().isBlank() ? "main" : body.ref();
        return gitHostingClient.writeFile(
                owner, repo, body.path(), body.content(), body.message(), ref, body.sha());
    }

    @PostMapping("/repos/{owner}/{repo}/file")
    public GitHostingClient.WriteResult createFile(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestBody WriteFileRequest body) {
        return writeFile(owner, repo, body);
    }
}
