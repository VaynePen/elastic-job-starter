package com.vayne.elasticjob.autoconfig;

import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.vayne.elasticjob.parser.JobParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class JobParserAutoConfiguration {

    @Bean
    public ZookeeperRegistryCenter zookeeperRegistryCenter(ZookeeperProperties zookeeperProperties) {
        ZookeeperConfiguration zkConfig = new ZookeeperConfiguration(zookeeperProperties.getServerLists(), zookeeperProperties.getNamespace());
        zkConfig.setBaseSleepTimeMilliseconds(zookeeperProperties.getBaseSleepTimeMilliseconds());
        zkConfig.setConnectionTimeoutMilliseconds(zookeeperProperties.getConnectionTimeoutMilliseconds());
        zkConfig.setDigest(zookeeperProperties.getDigest());
        zkConfig.setMaxRetries(zookeeperProperties.getMaxRetries());
        zkConfig.setMaxSleepTimeMilliseconds(zookeeperProperties.getMaxSleepTimeMilliseconds());
        zkConfig.setSessionTimeoutMilliseconds(zookeeperProperties.getSessionTimeoutMilliseconds());
        return new ZookeeperRegistryCenter(zkConfig);
    }

    @Bean
    @DependsOn(value = "zookeeperRegistryCenter")
    public JobParser jobConfParser() {
        return new JobParser();
    }
}
