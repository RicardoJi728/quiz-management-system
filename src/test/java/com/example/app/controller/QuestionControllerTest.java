package com.example.app.controller;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.model.Question;
import com.example.app.model.Subject;
import com.example.app.model.User;
import com.example.app.repository.QuestionRepository;
import com.example.app.repository.SubjectRepository;
import com.example.app.repository.UserRepository;
import com.example.app.security.JwtTokenProvider;

@SpringBootTest
@AutoConfigureMockMvc
public class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionRepository questionRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private SubjectRepository subjectRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    public void testGetUserQuestions() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(questionRepository.findByOwner(mockUser)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/questions/my-questions"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    public void testCreateQuestion() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        
        Subject mockSubject = new Subject();
        mockSubject.setId(1L);
        mockSubject.setSubjectName("Test Subject");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(mockSubject));
        when(questionRepository.save(any(Question.class))).thenReturn(new Question());

        mockMvc.perform(post("/api/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Test?\",\"markScheme\":\"Answer\",\"points\":5,\"level\":\"Standard\",\"subject\":{\"id\":1}}"))
                .andExpect(status().isOk());
    }
} 