package com.aicoding;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AI Coding - AI 零代码应用生成平台
 */
@SpringBootApplication
@EnableDubbo
@MapperScan("com.aicoding.mapper")
@ConfigurationPropertiesScan
public class AiCodingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodingApplication.class, args);
    }
}
