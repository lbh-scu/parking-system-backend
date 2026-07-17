package com.smartparking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "init")
public class InitDataProperties {
    //true=强制清空全表重新导入Excel；false=仅表为空时导入
    private boolean forceRefresh;

    public boolean isForceRefresh() {
        return forceRefresh;
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }
}
