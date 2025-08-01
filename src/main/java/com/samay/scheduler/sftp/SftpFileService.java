package com.samay.scheduler.sftp;

import com.samay.scheduler.config.SftpMonitorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SftpFileService {

    @Autowired
    private SftpMonitorProperties monitorProperties;


    public List<SftpClient.DirEntry> readFiles(SftpClient client) throws IOException {
        log.debug("Entering method readFiles");
        log.debug("Reading files from SFTP directory: {}", monitorProperties.getPollingDir());
        List<SftpClient.DirEntry> files = new ArrayList<>();
        client.readDir(monitorProperties.getPollingDir()).forEach(entry -> {
            String filename = entry.getFilename();
            if (!isSkippableFile(filename)) {
                files.add(entry);
            }
        });
        log.debug("Total valid files found (excluding '.' and '..'): {}", files.size());
        log.debug("Exiting method readFiles");
        return files;
    }


    public void moveFile(SftpClient client, String src, String dst, String reason) {
        try {
            client.rename(src, dst);
            log.debug("[{}] File moved from '{}' to '{}'", reason, src, dst);
        } catch (Exception e) {
            log.error("Failed to move file from '{}' to '{}'. Reason: {}", src, dst, e.getMessage(), e);
        }
    }

    public void moveAllFiles(SftpClient client, String from, String to, String reason) {
        try {
            List<SftpClient.DirEntry> files = new ArrayList<>();
            client.readDir(from).forEach(entry -> {
                if (!isSkippableFile(entry.getFilename())) {
                    files.add(entry);
                }
            });
            log.debug("Moving {} files from '{}' to '{}' with reason: {}", files.size(), from, to, reason);
            for (SftpClient.DirEntry file : files) {
                moveFile(client, from + "/" + file.getFilename(), to + "/" + file.getFilename(), reason);
            }
        } catch (Exception e) {
            log.error("Failed to move files from '{}' to '{}': {}", from, to, e.getMessage(), e);
        }
    }

    public boolean fileExists(SftpClient client, String dir, String filename) {
        try {
            List<SftpClient.DirEntry> entries = new ArrayList<>();
            client.readDir(dir).forEach(entry -> {
                if (!isSkippableFile(entry.getFilename())) {
                    entries.add(entry);
                }
            });
            boolean exists = entries.stream().anyMatch(e -> e.getFilename().equals(filename));
            log.debug("File '{}' exists in '{}': {}", filename, dir, exists);
            return exists;
        } catch (Exception e) {
            log.error("Failed to check if file '{}' exists in '{}': {}", filename, dir, e.getMessage(), e);
            return false;
        }
    }

    public List<SftpClient.DirEntry> countFiles(SftpClient client, String dir, String filePattern) {
        try {
            Pattern regexPattern = Pattern.compile(convertPatternToRegex(filePattern));
            List<SftpClient.DirEntry> entries = new ArrayList<>();
            client.readDir(dir).forEach(entry -> {
                String fileName = entry.getFilename();
                if (!isSkippableFile(entry.getFilename()) && regexPattern.matcher(fileName).matches()) {
                    entries.add(entry);
                }else if(!regexPattern.matcher(fileName).matches()){
                    log.debug("File pattern is not matching. current : {}, expected : {}", fileName, filePattern);
                }
            });
            long count = entries.stream().filter(f -> f.getFilename().endsWith(monitorProperties.getExtension())).count();
            log.debug("Counted {} valid files in directory '{}'.", count, dir);
            return entries;
        } catch (Exception e) {
            log.error("Failed to count files in '{}': {}", dir, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String convertPatternToRegex(String wildcardPattern) {
        // Escape dots and replace * with .*
        return wildcardPattern.replace(".", "\\.").replace("*", ".*");
    }

    public void ensureDir(SftpClient client, String path) {
        try {
            client.stat(path);
            log.debug("Directory '{}' already exists.", path);
        } catch (IOException e) {
            try {
                client.mkdir(path);
                log.debug("Created missing directory: '{}'", path);
            } catch (IOException ex) {
                log.error("Failed to create directory '{}': {}", path, ex.getMessage(), ex);
            }
        }
    }

    public void mergeBacklogFiles(SftpClient client, String backlogDir, String jobDir) {
        try {
            client.readDir(backlogDir).forEach(file -> {
                if (!isSkippableFile(file.getFilename())) {
                    moveFile(client, backlogDir + "/" + file.getFilename(), jobDir + "/" + file.getFilename(), "Moved from backlog");
                }
            });
        } catch (Exception e) {
            log.error("Failed to merge backlog files for '{}': {}", jobDir, e.getMessage(), e);
        }
    }

    public boolean isSkippableFile(String filename) {
        return ".".equals(filename) || "..".equals(filename);
    }


}
