package com.peakui.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.peakui.course.handler.GlobalExceptionHandler;
import com.peakui.course.mapper.CourseMapper;
import com.peakui.course.model.entity.Course;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseSearchSyncControllerTest {
    private CourseMapper mapper;
    private CourseSearchSyncController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mapper = mock(CourseMapper.class);
        controller = new CourseSearchSyncController(mapper);
        ReflectionTestUtils.setField(controller, "syncToken", "secret");
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void deniesMissingWrongAndUnconfiguredTokenBeforeQuery() throws Exception {
        mvc.perform(get("/courses/internal/search-documents")).andExpect(status().isForbidden());
        mvc.perform(get("/courses/internal/search-documents").header("X-Search-Sync-Token", "wrong"))
                .andExpect(status().isForbidden());
        ReflectionTestUtils.setField(controller, "syncToken", "");
        mvc.perform(get("/courses/internal/search-documents").header("X-Search-Sync-Token", "secret"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsInvalidPaginationDespiteGlobalCatchAll() throws Exception {
        for (String afterId : List.of("-1", "abc", "9223372036854775808")) {
            mvc.perform(get("/courses/internal/search-documents").header("X-Search-Sync-Token", "secret")
                    .param("afterId", afterId)).andExpect(status().isBadRequest());
        }
        for (String size : List.of("0", "101", "abc", "2147483648")) {
            mvc.perform(get("/courses/internal/search-documents").header("X-Search-Sync-Token", "secret")
                    .param("pageSize", size)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void exportsSafeFieldsWithDefaultCursorAndStringId() throws Exception {
        Course course = new Course();
        course.setId(9007199254740993L);
        course.setTitle("Course");
        course.setSubtitle("Public summary");
        course.setDescription("private body");
        course.setLevel(2);
        course.setStatus(1);
        when(mapper.selectList(any(QueryWrapper.class))).thenAnswer(invocation -> {
            QueryWrapper<Course> query = invocation.getArgument(0);
            assertFalse(query.getSqlSelect().contains("description"));
            assertEquals("id,title,subtitle,cover_url,level,status,published_at,created_at", query.getSqlSelect());
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("id >"));
            assertTrue(sql.contains("status ="));
            assertTrue(sql.contains("ORDER BY id ASC"));
            assertTrue(sql.endsWith("LIMIT 100"));
            assertTrue(query.getParamNameValuePairs().containsValue(0L));
            assertTrue(query.getParamNameValuePairs().containsValue(1));
            return List.of(course);
        });
        mvc.perform(get("/courses/internal/search-documents").header("X-Search-Sync-Token", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("9007199254740993"))
                .andExpect(jsonPath("$.data[0].summary").value("Public summary"))
                .andExpect(jsonPath("$.data[0].difficulty").value(2))
                .andExpect(jsonPath("$.data[0].status").value(1))
                .andExpect(jsonPath("$.data[0].description").doesNotExist());
    }
}
