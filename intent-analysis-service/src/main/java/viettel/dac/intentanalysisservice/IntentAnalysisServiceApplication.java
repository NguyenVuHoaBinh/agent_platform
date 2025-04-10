package viettel.dac.intentanalysisservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableAsync
public class IntentAnalysisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntentAnalysisServiceApplication.class, args);
    }
}
