package com.example.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.StructuredOutputReminder;

import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：结构化输出示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/10 13:07
 */
public class StructuredOutputExample {
    public static void main(String[] args) throws JsonProcessingException {
        // 模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("智能助手")
                .sysPrompt("你是一个专业的简历解析助手。请从用户的简历文本中提取关键信息，输出结构化的简历数据。")
                .model(model)
                .structuredOutputReminder(StructuredOutputReminder.TOOL_CHOICE)
                .build();

        // 调用智能体
        String resumeText = """
                张三，5年Java开发经验。
                2019-2022 就职于阿里巴巴，担任高级Java工程师，负责电商核心系统开发。
                2022-至今 就职于字节跳动，担任技术专家，负责微服务架构设计。
                精通 Java、Spring Boot、Redis、MySQL、Kubernetes。
                邮箱: zhangsan@mail.com，电话: 1234567890
                住址: 山东省 淄博市
                """;
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("请解析以下简历: \n" + resumeText)
                .build();
        Msg msg = agent.call(userMsg, Resume.class).block();
        // 结构化输出
        Resume resume = msg.getStructuredData(Resume.class);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(resume);
        System.out.println(jsonString);
    }

    // 简历
    public static class Resume {
        public String name; // 姓名
        public String email; // 邮箱
        public String phone; // 手机
        @JsonProperty("experience_years")
        public Integer yearsOfExperience; // 工作年限
        @JsonProperty("experiences")
        public List<WorkExperience> workExperiences; // 工作经验
        public List<String> skills; // 技能
        public Address address; // 家庭地址
    }

    // 工作经验
    public static class WorkExperience {
        public String company; // 公司
        public String position; // 职位
        public String duration; // 工作时长
        @JsonProperty("start_time")
        public String startTime; // 工作开始时间
        @JsonProperty("end_time")
        public String endTime; // 工作截止时间
        public List<String> responsibilities; // 职责
    }

    // 家庭地址
    public static class Address {
        public String prov;
        public String city;
    }
}
