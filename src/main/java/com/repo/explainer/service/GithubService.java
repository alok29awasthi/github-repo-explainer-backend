package com.repo.explainer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GithubService {

    private final RestTemplate restTemplate;

    public GithubService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getReadme(String owner, String repo) {

        String url =
                "https://api.github.com/repos/" + owner + "/" + repo + "/readme";

        Map response = restTemplate.getForObject(url, Map.class);

        String encoded = (String) response.get("content");

        byte[] decoded = Base64.getMimeDecoder().decode(encoded);

        return new String(decoded);
    }

    public List<String> getRepoFilePaths(String owner, String repo) {

        String url =
                "https://api.github.com/repos/" + owner + "/" + repo + "/git/trees/main?recursive=1";

        Map response = restTemplate.getForObject(url, Map.class);

        List<Map<String, Object>> tree = (List<Map<String, Object>>) response.get("tree");

        List<String> filePaths = new ArrayList<>();

        for (Map<String, Object> item : tree) {

            String type = (String) item.get("type");

            if ("blob".equals(type)) {

                String path = (String) item.get("path");

                filePaths.add(path);
            }
        }

        return filePaths;
    }

    public List<String> getCompressedRepoPaths(String owner, String repo) {

        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/git/trees/main?recursive=1";

        Map response = restTemplate.getForObject(url, Map.class);

        List<Map<String, Object>> tree = (List<Map<String, Object>>) response.get("tree");

        List<String> result = new ArrayList<>();

        for (Map<String, Object> item : tree) {

            String type = (String) item.get("type");

            if (!"blob".equals(type)) {
                continue;
            }

            String path = (String) item.get("path");

            if (!isRelevantFile(path)) {
                continue;
            }

            if (!isCoreCodePath(path)) {
                continue;
            }

            result.add(compressAroundCore(path));
        }

        return result.stream()
                .distinct()
                .limit(300)
                .toList();
    }

    private boolean isCoreCodePath(String path) {

        return path.contains("app/") ||
                path.contains("lib/") ||
                path.contains("server/") ||
                path.contains("api/") ||
                path.contains("config/") ||
                path.contains("service/") ||
                path.contains("controller/") ||
                path.contains("pages/") ||
                path.contains("components/");
    }

    private String compressPath(String path) {

        String[] parts = path.split("/");

        if (parts.length <= 6) {
            return path;
        }

        return parts[0] + "/" +
                parts[1] + "/" +
                parts[2] + "/.../" +
                parts[parts.length - 1];
    }

    private String compressAroundCore(String path) {

        String[] parts = path.split("/");

        List<String> coreKeywords = List.of(
                "lib",
                "server",
                "controller",
                "service",
                "repository",
                "config",
                "api",
                "pages",
                "components"
        );

        int coreIndex = -1;

        for (int i = 0; i < parts.length; i++) {

            if (coreKeywords.contains(parts[i].toLowerCase())) {
                coreIndex = i;
                break;
            }
        }

        String fileName = parts[parts.length - 1];

        if (coreIndex == -1) {

            if (parts.length <= 4) {
                return path;
            }

            return parts[0] + "/.../" + fileName;
        }

        return ".../" + parts[coreIndex] + "/.../" + fileName;
    }

    private boolean isRelevantFile(String path) {

        String fileName = path.substring(path.lastIndexOf("/") + 1).toLowerCase();
        String lowerPath = path.toLowerCase();

        // Remove abstract classes
        if (fileName.contains("abstract")) {
            return false;
        }

        // Remove tests by folder
        if (lowerPath.contains("/test/") ||
                lowerPath.contains("/tests/") ||
                lowerPath.contains("__tests__")) {
            return false;
        }

        // Remove test files
        if (fileName.contains("test")) {
            return false;
        }

        return path.endsWith(".java") ||
                path.endsWith(".kt") ||
                path.endsWith(".ts") ||
                path.endsWith(".tsx") ||
                path.endsWith(".js") ||
                path.endsWith(".py") ||
                path.endsWith(".go") ||
                path.endsWith(".rs") ||
                path.endsWith(".cpp") ||
                path.endsWith(".c") ||
                path.endsWith(".cs") ||
                path.endsWith(".swift") ||
                path.endsWith("pom.xml") ||
                path.endsWith("build.gradle") ||
                path.endsWith("package.json") ||
                path.endsWith("requirements.txt") ||
                path.endsWith("Dockerfile");
    }
}
