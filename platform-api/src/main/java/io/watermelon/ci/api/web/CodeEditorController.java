package io.watermelon.ci.api.web;

import io.watermelon.ci.git.spi.GitHostingClient;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin code-editor backend: browse and open repository files via Forgejo.
 * Frontend (Monaco) consumes these endpoints.
 */
@RestController
@RequestMapping("/api/v1/editor")
public class CodeEditorController {

    private final GitHostingClient gitHostingClient;

    public CodeEditorController(GitHostingClient gitHostingClient) {
        this.gitHostingClient = gitHostingClient;
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
}
