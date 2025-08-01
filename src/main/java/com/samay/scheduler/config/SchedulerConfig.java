package com.samay.scheduler.config;

//import com.jcraft.jsch.ChannelSftp;
//import org.hibernate.SessionFactory;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.autoconfigure.ssl.SslProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.file.remote.session.SessionFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;




@Configuration
@EnableAsync
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}