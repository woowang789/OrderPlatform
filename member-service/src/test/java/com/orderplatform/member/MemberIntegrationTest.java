package com.orderplatform.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.member.common.AbstractIntegrationTest;
import com.orderplatform.member.adapter.in.web.dto.LoginRequest;
import com.orderplatform.member.adapter.in.web.dto.SignupRequest;
import com.orderplatform.member.adapter.out.persistence.MemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.orderplatform.common.test.InternalTokenTestSupport.internalToken;
import static com.orderplatform.common.test.InternalTokenTestSupport.internalTokenWithUser;
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
                        .with(internalToken())
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
                        .with(internalToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // 같은 이메일로 두 번째 가입 실패
        mockMvc.perform(post("/api/members/signup")
                        .with(internalToken())
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
                        .with(internalToken())
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
                        .with(internalToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_success() throws Exception {
        // 회원가입
        Long memberId = signup("me@example.com", "password123", "내정보");

        // 내 정보 조회 — Gateway가 X-User-Id 헤더를 주입하는 것을 시뮬레이션
        mockMvc.perform(get("/api/members/me")
                        .with(internalTokenWithUser(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.name").value("내정보"));
    }

    @Test
    @DisplayName("토큰 없이 내 정보 조회 실패")
    void getMyInfo_noToken_fail() throws Exception {
        mockMvc.perform(get("/api/members/me")
                        .with(internalToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("전체 플로우: 회원가입 → 로그인 → 내 정보 조회")
    void fullFlow() throws Exception {
        // 1. 회원가입
        SignupRequest signupRequest = new SignupRequest("flow@example.com", "password123", "플로우");

        String signupResponse = mockMvc.perform(post("/api/members/signup")
                        .with(internalToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("flow@example.com"))
                .andReturn().getResponse().getContentAsString();

        Long memberId = objectMapper.readTree(signupResponse).get("id").asLong();

        // 2. 로그인 → JWT 토큰 발급 확인
        LoginRequest loginRequest = new LoginRequest("flow@example.com", "password123");

        mockMvc.perform(post("/api/members/login")
                        .with(internalToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 3. 내 정보 조회 — Gateway가 X-User-Id를 주입하는 것을 시뮬레이션
        mockMvc.perform(get("/api/members/me")
                        .with(internalTokenWithUser(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@example.com"))
                .andExpect(jsonPath("$.name").value("플로우"));
    }

    // 헬퍼 메서드
    private Long signup(String email, String password, String name) throws Exception {
        SignupRequest request = new SignupRequest(email, password, name);
        String response = mockMvc.perform(post("/api/members/signup")
                        .with(internalToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

}
