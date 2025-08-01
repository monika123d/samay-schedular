package com.samay.scheduler.config;

//import com.jcraft.jsch.ChannelSftp.LsEntry;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import java.io.File;
import java.io.IOException;

@Configuration
public class SftpConfig {

    @Value("${sftp.host}")
    private String host;

    @Value("${sftp.port}")
    private int port;

    @Value("${sftp.username}")
    private String username;

    @Value("${sftp.privateKey}")
    private String privateKey;

    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() throws IOException {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(host);
        factory.setPort(port);
        factory.setUser(username);
        factory.setPrivateKey(new FileSystemResource(privateKey)); // Convert Resource to File
        factory.setAllowUnknownKeys(true); // consider more secure option for production

        //return new CachingSessionFactory<>(factory);
        CachingSessionFactory<SftpClient.DirEntry> cachingSessionFactory = new CachingSessionFactory<>(factory);
        cachingSessionFactory.setPoolSize(10); // Increase pool size
        return cachingSessionFactory;
    }
}
