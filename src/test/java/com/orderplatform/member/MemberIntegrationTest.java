package com.orderplatform.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.member.adapter.in.web.dto.LoginRequest;
import com.orderplatform.member.adapter.in.web.dto.SignupRequest;
import com.orderplatform.member.adapter.out.persistence.MemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @BeforeEach
    void setUp() {
        memberJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        SignupRequest request = new SignupRequest("test@example.com", "password123", "테스트");

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("이메일 중복 회원가입 실패")
    void signup_duplicateEmail_fail() throws Exception {
        SignupRequest request = new SignupRequest("dup@example.com", "password123", "테스트");
        String body = objectMapper.writeValueAsString(request);

        // 첫 번째 가입 성공
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // 같은 이메일로 두 번째 가입 실패
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // 회원가입
        signup("login@example.com", "password123", "로그인유저");

        // 로그인
        LoginRequest loginRequest = new LoginRequest("login@example.com", "password123");

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 실패")
    void login_wrongPassword_fail() throws Exception {
        // 회원가입
        signup("wrong@example.com", "password123", "테스트");

        // 잘못된 비밀번호로 로그인
        LoginRequest loginRequest = new LoginRequest("wrong@example.com", "wrongpassword");

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_success() throws Exception {
        // 회원가입 + 로그인
        signup("me@example.com", "password123", "내정보");
        String token = login("me@example.com", "password123");

        // 내 정보 조회
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.name").value("내정보"));
    }

    @Test
    @DisplayName("토큰 없이 내 정보 조회 실패")
    void getMyInfo_noToken_fail() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("전체 플로우: 회원가입 → 로그인 → 내 정보 조회")
    void fullFlow() throws Exception {
        // 1. 회원가입
        SignupRequest signupRequest = new SignupRequest("flow@example.com", "password123", "플로우");

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("flow@example.com"));

        // 2. 로그인 → JWT 토큰 획득
        LoginRequest loginRequest = new LoginRequest("flow@example.com", "password123");

        MvcResult loginResult = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // 3. JWT로 내 정보 조회
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@example.com"))
                .andExpect(jsonPath("$.name").value("플로우"));
    }

    // 헬퍼 메서드
    private void signup(String email, String password, String name) throws Exception {
        SignupRequest request = new SignupRequest(email, password, name);
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
