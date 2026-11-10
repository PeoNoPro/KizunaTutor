package com.kizuna.features.profile;

import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.JwtTokenVerifier;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.admin.service.OperationsActorResolver;
import com.kizuna.features.profile.controller.AvatarController;
import com.kizuna.features.profile.dto.response.AvatarUploadResponseDto;
import com.kizuna.features.profile.service.AvatarStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AvatarController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class})
class AvatarControllerTest {

    private static final String FILE_NAME = "3f2b1c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d.png";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AvatarStorageService avatarStorageService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JwtTokenVerifier jwtTokenVerifier;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private OperationsActorResolver operationsActorResolver;

    private static MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "avatar.png", "image/png", PNG);
    }

    @Test
    @DisplayName("Người dùng đã đăng nhập tải ảnh lên - 201 kèm avatarUrl, fileName, size")
    void authenticatedUserCanUpload() throws Exception {
        when(avatarStorageService.store(any()))
                .thenReturn(new AvatarUploadResponseDto("/api/profile/avatar/" + FILE_NAME, FILE_NAME, PNG.length));

        mockMvc.perform(multipart("/api/profile/avatar").file(pngFile())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                "learner@kizuna.vn", null, List.of(new SimpleGrantedAuthority("ROLE_LEARNER"))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Tải ảnh đại diện lên thành công."))
                .andExpect(jsonPath("$.data.avatarUrl").value("/api/profile/avatar/" + FILE_NAME))
                .andExpect(jsonPath("$.data.fileName").value(FILE_NAME))
                .andExpect(jsonPath("$.data.size").value(PNG.length));
    }

    @Test
    @DisplayName("Chưa đăng nhập tải ảnh lên - bị từ chối, không lưu tệp")
    void anonymousCannotUpload() throws Exception {
        mockMvc.perform(multipart("/api/profile/avatar").file(pngFile()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này."));
        verify(avatarStorageService, never()).store(any());
    }

    @Test
    @DisplayName("Xem ảnh đại diện công khai - trả nội dung nhị phân kèm Content-Type")
    void anyoneCanViewAvatar() throws Exception {
        when(avatarStorageService.load(FILE_NAME)).thenReturn(new AvatarStorageService.AvatarFile(PNG, "image/png"));

        mockMvc.perform(get("/api/profile/avatar/" + FILE_NAME))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
    }
}
