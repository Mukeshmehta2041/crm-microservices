package com.crm.platform.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {
    "com.crm.platform.users",
    "com.crm.platform.common"
})
@EnableJpaAuditing
@EnableAsync
@EnableTransactionManagement
@EnableCaching
@EnableJpaRepositories(basePackages = "com.crm.platform.users.repository")
@EnableRedisRepositories(basePackages = "com.crm.platform.users.cache") // Empty package to disable Redis repos
public class UsersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsersServiceApplication.class, args);
    }
}