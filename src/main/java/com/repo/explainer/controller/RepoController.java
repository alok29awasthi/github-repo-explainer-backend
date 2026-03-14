package com.repo.explainer.controller;

import com.repo.explainer.service.GeminiService;
import com.repo.explainer.service.GithubService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/repo")
public class RepoController {

    private final GithubService githubService;
    private final GeminiService geminiService;

    public RepoController(GithubService githubService,
                          GeminiService geminiService) {
        this.githubService = githubService;
        this.geminiService = geminiService;
    }

    @GetMapping("/explain")
    public String explainRepo(@RequestParam String owner,
                              @RequestParam String repo) {

        String readme = githubService.getReadme(owner, repo);
        List<String> blobFilePaths = githubService.getCompressedRepoPaths(owner, repo);

        return geminiService.explainRepo(readme, blobFilePaths);
    }
}
