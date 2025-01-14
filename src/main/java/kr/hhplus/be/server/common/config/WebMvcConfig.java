package kr.hhplus.be.server.common.config;

import kr.hhplus.be.server.common.interceptor.QueueTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private QueueTokenInterceptor queueTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(queueTokenInterceptor)
                .addPathPatterns("/api/concert/**", "/api/reservation/**");
                //.excludePathPatterns("");  // 제외할 경로가 있다면 추가
    }

}
