package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAGFlow API 配置
 *
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "ragflow")
public class RagFlowConfig
{
    /** RAGFlow 服务地址，例如 http://127.0.0.1:9380 */
    private String url;

    /** RAGFlow API Key */
    private String apiKey;

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }
}
