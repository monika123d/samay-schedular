package com.samay.scheduler.config;

import com.samay.scheduler.persistence.TriggerType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sftp.monitor")
public class SftpMonitorProperties {
    private String baseDir;
    private String pollingDir;
    private String erroredDir;
    private String backlogDir;
    private String processedDir;
    private String processingDir;
    private String deletedDir;
    private String ignoredDir;
    private String extension;
    private long pollInterval;
    private String conditionsConfigPath;

    private TriggerType triggerType;

    private String localDownloadPath;
}
