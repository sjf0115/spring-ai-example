package com.mcp.example.server;

import com.mcp.example.bean.CurrentCondition;
import com.mcp.example.bean.Weather;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 功能：WeatherService
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/2/7 20:34
 */
@Service
public class WeatherService {
    private static final String BASE_URL = "https://wttr.in";
    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", "application/geo+json")
                .defaultHeader("User-Agent", "WeatherApiClient/1.0 (your@email.com)")
                .build();
    }

    //------------------------------------------------------------------------------------------------------------------
    //  Tool
    @Tool(description = "根据指定的城市获取天气预报")
    public String getWeatherByCity(String cityName) {
        Weather weather = restClient.get()
                .uri("/{city_name}?format=j1&lang=zh", cityName)
                .retrieve()
                .body(Weather.class);
        List<CurrentCondition> conditions = weather.getCurrentConditions();
        if (conditions == null || conditions.isEmpty()) {
            return null;
        }
        CurrentCondition condition = conditions.get(0);
        String result = String.format("""
                    城市: %s
                    天气情况: %s
                    气压: %s（mb）
                    温度: %s°C (Feels like: %s°C)
                    湿度: %s%%
                    降水量:%s (mm)
                    风速: %s km/h (%s)
                    能见度: %s 公里
                    紫外线指数: %s
                    观测时间: %s
                    """,
                cityName,
                condition.getLangZh().get(0).getValue(),
                condition.getPressure(),
                condition.getTempC(),
                condition.getFeelsLikeC(),
                condition.getHumidity(),
                condition.getPrecipMM(),
                condition.getWindspeedKmph(),
                condition.getWinddir16Point(),
                condition.getVisibility(),
                condition.getUvIndex(),
                condition.getLocalObsDateTime()
        );
        return result;
    }

    public static void main(String[] args) {
        WeatherService client = new WeatherService();
        String weather = client.getWeatherByCity("北京");
        System.out.println(weather);
    }
}
