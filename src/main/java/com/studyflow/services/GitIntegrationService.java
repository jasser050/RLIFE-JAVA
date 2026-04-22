package com.studyflow.services;

import com.studyflow.models.Assignment;
import com.studyflow.models.Project;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class GitIntegrationService {
    public GitOperationResult initializeRepository(Project project) {
        Path repoPath = resolveRepositoryPath(project);
        if (repoPath == null) {
            return GitOperationResult.failure("Repository path is required.", List.of());
        }

        try {
            Files.createDirectories(repoPath);
            try (Git git = Git.init().setDirectory(repoPath.toFile()).call()) {
                checkoutBranchIfNeeded(git, project == null ? null : project.getGitDefaultBranch());
                configureOriginRemote(git, project);
                return describeStatus(project);
            }
        } catch (Exception e) {
            return GitOperationResult.failure("Unable to initialize repository: " + e.getMessage(), List.of());
        }
    }

    public GitOperationResult cloneRepository(Project project) {
        Path repoPath = resolveRepositoryPath(project);
        if (repoPath == null) {
            return GitOperationResult.failure("Repository path is required.", List.of());
        }
        if (project == null || isBlank(project.getGitRemoteUrl())) {
            return GitOperationResult.failure("Remote URL is required to clone a repository.", List.of());
        }

        try {
            Path parent = repoPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            CloneCommand cloneCommand = Git.cloneRepository()
                    .setDirectory(repoPath.toFile())
                    .setURI(project.getGitRemoteUrl().trim());
            CredentialsProvider credentialsProvider = credentialsFor(project);
            if (credentialsProvider != null) {
                cloneCommand.setCredentialsProvider(credentialsProvider);
            }
            if (!isBlank(project.getGitDefaultBranch())) {
                cloneCommand.setBranch(project.getGitDefaultBranch().trim());
            }

            try (Git git = cloneCommand.call()) {
                configureOriginRemote(git, project);
                return describeStatus(project);
            }
        } catch (Exception e) {
            return GitOperationResult.failure(buildGitErrorMessage("clone repository", project, e), List.of());
        }
    }

    public GitOperationResult pull(Project project) {
        try (Git git = open(project)) {
            org.eclipse.jgit.api.PullCommand pullCommand = git.pull();
            CredentialsProvider credentialsProvider = credentialsFor(project);
            if (credentialsProvider != null) {
                pullCommand.setCredentialsProvider(credentialsProvider);
            }
            PullResult result = pullCommand.call();
            if (!result.isSuccessful()) {
                return GitOperationResult.failure("Pull failed. Check credentials or branch state.", List.of());
            }
            return describeStatus(project);
        } catch (Exception e) {
            return GitOperationResult.failure(buildGitErrorMessage("pull repository", project, e), List.of());
        }
    }

    public GitOperationResult describeStatus(Project project) {
        try (Git git = open(project)) {
            Repository repository = git.getRepository();
            Status status = git.status().call();
            List<String> changedFiles = collectChangedFiles(status);
            String branch = safe(repository.getBranch());
            String head = shortHead(repository);
            String summary = changedFiles.isEmpty()
                    ? "Repository is clean on " + branch + "."
                    : changedFiles.size() + " changed file(s) on " + branch + ".";
            String details = head.isBlank() ? summary : summary + " HEAD " + head + ".";
            return GitOperationResult.success(summary, details, changedFiles, head);
        } catch (Exception e) {
            return GitOperationResult.failure("Unable to read repository status: " + e.getMessage(), List.of());
        }
    }

    public GitOperationResult commitAssignment(Project project, Assignment assignment, boolean push) {
        if (assignment == null) {
            return GitOperationResult.failure("Assignment is required.", List.of());
        }
        String commitMessage = buildCommitMessage(assignment);
        try (Git git = open(project)) {
            stageChanges(git, assignment.getGitCommitPathspec());
            Status stagedStatus = git.status().call();
            if (collectChangedFiles(stagedStatus).isEmpty()) {
                return GitOperationResult.failure("No changed files were found to commit.", List.of());
            }

            RevCommit commit = git.commit().setMessage(commitMessage).call();
            String shortHash = commit.getName().substring(0, Math.min(8, commit.getName().length()));

            if (push) {
                org.eclipse.jgit.api.PushCommand pushCommand = git.push();
                CredentialsProvider credentialsProvider = credentialsFor(project);
                if (credentialsProvider != null) {
                    pushCommand.setCredentialsProvider(credentialsProvider);
                }
                Iterable<PushResult> pushResults = pushCommand.call();
                for (PushResult result : pushResults) {
                    if (result.getMessages() != null && result.getMessages().toLowerCase(Locale.ROOT).contains("error")) {
                        return GitOperationResult.failure(buildPushFailureMessage(project, result.getMessages()), List.of());
                    }
                }
            }

            return GitOperationResult.success(
                    push ? "Assignment committed and pushed." : "Assignment committed.",
                    "Commit " + shortHash + " created for \"" + safe(assignment.getTitle()) + "\".",
                    List.of(),
                    shortHash
            );
        } catch (Exception e) {
            return GitOperationResult.failure(buildCommitErrorMessage(project, push, e), List.of());
        }
    }

    public boolean isRepositoryConfigured(Project project) {
        Path path = resolveRepositoryPath(project);
        return path != null && Files.exists(path.resolve(".git"));
    }

    public GitOperationResult syncRemote(Project project) {
        if (project == null || isBlank(project.getGitRemoteUrl())) {
            return GitOperationResult.failure("Remote URL is required.", List.of());
        }
        try (Git git = open(project)) {
            configureOriginRemote(git, project);
            return describeStatus(project);
        } catch (Exception e) {
            return GitOperationResult.failure("Unable to sync repository remote: " + e.getMessage(), List.of());
        }
    }

    private Git open(Project project) throws Exception {
        Path repoPath = resolveRepositoryPath(project);
        if (repoPath == null) {
            throw new IllegalStateException("Repository path is not configured.");
        }
        if (!Files.exists(repoPath)) {
            throw new IllegalStateException("Repository path does not exist.");
        }
        return Git.open(repoPath.toFile());
    }

    private void checkoutBranchIfNeeded(Git git, String branchName) {
        if (isBlank(branchName)) {
            return;
        }
        try {
            git.checkout().setName(branchName.trim()).setCreateBranch(true).setForceRefUpdate(true).call();
        } catch (Exception ignored) {
            try {
                git.checkout().setName(branchName.trim()).call();
            } catch (Exception ignoredAgain) {
                // Keep the repository on the default branch if branch checkout fails.
            }
        }
    }

    private void configureOriginRemote(Git git, Project project) throws Exception {
        if (git == null || project == null || isBlank(project.getGitRemoteUrl())) {
            return;
        }
        StoredConfig config = git.getRepository().getConfig();
        String remoteUrl = project.getGitRemoteUrl().trim();
        config.setString("remote", "origin", "url", remoteUrl);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");

        if (!isBlank(project.getGitDefaultBranch())) {
            String branch = project.getGitDefaultBranch().trim();
            config.setString("branch", branch, "remote", "origin");
            config.setString("branch", branch, "merge", "refs/heads/" + branch);
        }

        config.save();
    }

    private void stageChanges(Git git, String pathSpec) throws Exception {
        String normalizedPath = isBlank(pathSpec) ? "." : pathSpec.trim();
        git.add().addFilepattern(normalizedPath).call();
        git.add().setUpdate(true).addFilepattern(normalizedPath).call();
    }

    private CredentialsProvider credentialsFor(Project project) {
        if (project == null || isBlank(project.getGitAccessToken())) {
            return null;
        }
        String username = isBlank(project.getGitUsername()) ? "oauth2" : project.getGitUsername().trim();
        return new UsernamePasswordCredentialsProvider(username, project.getGitAccessToken().trim());
    }

    private Path resolveRepositoryPath(Project project) {
        if (project == null || isBlank(project.getGitRepoPath())) {
            return null;
        }
        return Path.of(project.getGitRepoPath().trim());
    }

    private String buildCommitMessage(Assignment assignment) {
        if (assignment != null && !isBlank(assignment.getGitCommitMessage())) {
            return assignment.getGitCommitMessage().trim();
        }
        String title = assignment == null ? "assignment" : safe(assignment.getTitle());
        return "complete(assignment): " + title;
    }

    private List<String> collectChangedFiles(Status status) {
        Set<String> values = new TreeSet<>();
        values.addAll(status.getAdded());
        values.addAll(status.getChanged());
        values.addAll(status.getModified());
        values.addAll(status.getMissing());
        values.addAll(status.getRemoved());
        values.addAll(status.getUntracked());
        values.addAll(status.getConflicting());
        return values.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private String shortHead(Repository repository) throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) {
            return "";
        }
        return head.abbreviate(8).name();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildCommitErrorMessage(Project project, boolean push, Exception exception) {
        if (push) {
            String permissionMessage = permissionDeniedMessage(project, exception == null ? null : exception.getMessage());
            if (permissionMessage != null) {
                return permissionMessage;
            }
            return "Commit created locally, but push failed: " + safe(exception == null ? null : exception.getMessage());
        }
        return buildGitErrorMessage("commit assignment", project, exception);
    }

    private String buildPushFailureMessage(Project project, String message) {
        String permissionMessage = permissionDeniedMessage(project, message);
        if (permissionMessage != null) {
            return permissionMessage;
        }
        return "Commit created locally, but push failed: " + safe(message);
    }

    private String buildGitErrorMessage(String action, Project project, Exception exception) {
        String permissionMessage = permissionDeniedMessage(project, exception == null ? null : exception.getMessage());
        if (permissionMessage != null) {
            return permissionMessage;
        }
        return "Unable to " + action + ": " + safe(exception == null ? null : exception.getMessage());
    }

    private String permissionDeniedMessage(Project project, String message) {
        if (isBlank(message)) {
            return null;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("git-receive-pack not permitted")
                || normalized.contains("not authorized")
                || normalized.contains("authentication is required")
                || normalized.contains("authentication failed")
                || normalized.contains("not permitted")) {
            String remote = safe(project == null ? null : project.getGitRemoteUrl());
            String suffix = remote.isBlank() ? "" : " for " + remote;
            return "Git access denied" + suffix + ". Check that your GitHub account has write access and that the saved token has repository write permission.";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record GitOperationResult(
            boolean success,
            String summary,
            String details,
            List<String> changedFiles,
            String commitHash,
            LocalDateTime completedAt
    ) {
        public static GitOperationResult success(String summary, String details, List<String> changedFiles, String commitHash) {
            return new GitOperationResult(true, summary, details, new ArrayList<>(changedFiles), commitHash, LocalDateTime.now());
        }

        public static GitOperationResult failure(String summary, List<String> changedFiles) {
            return new GitOperationResult(false, summary, summary, new ArrayList<>(changedFiles), null, LocalDateTime.now());
        }
    }
}
