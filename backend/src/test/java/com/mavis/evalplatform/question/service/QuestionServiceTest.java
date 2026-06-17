package com.mavis.evalplatform.question.service;

import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.question.dto.QuestionRequest;
import com.mavis.evalplatform.question.entity.Question;
import com.mavis.evalplatform.question.mapper.QuestionMapper;
import com.mavis.evalplatform.question.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * QuestionService 单元测试 — 覆盖 TC-03-001 ~ TC-03-005
 *
 * @author 向锏楠
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService 单元测试(FR-03 TC-03-001~005)")
class QuestionServiceTest {

    @Mock private QuestionMapper questionMapper;
    @InjectMocks private QuestionServiceImpl service;

    private QuestionRequest validRequest() {
        QuestionRequest r = new QuestionRequest();
        r.setContent("什么是光合作用?");
        r.setCategory("科学");
        r.setDifficulty("中等");
        r.setType("事实");
        r.setExpectedAnswer("植物利用光能将二氧化碳和水转化为葡萄糖");
        r.setIsPublic(false);
        return r;
    }

    @BeforeEach
    void setUp() {
        lenient().when(questionMapper.insert(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(System.nanoTime() & 0xffff);
            return 1;
        });
    }

    // ============ TC-03-001 单题输入 ============
    @Test
    @DisplayName("TC-03-001:单题输入 → 正常入库")
    void t1_create_ok() {
        Question q = service.create(validRequest(), 1L);
        assertNotNull(q.getId());
        assertEquals("什么是光合作用?", q.getContent());
        assertEquals("科学", q.getCategory());
        assertEquals("中等", q.getDifficulty());
        assertEquals("事实", q.getType());
        verify(questionMapper).insert(any(Question.class));
    }

    // ============ TC-03-002 批量导入(简化版) ============
    @Test
    @DisplayName("TC-03-002:批量导入 CSV → 成功 N 条")
    void t2_import_batch_ok() throws Exception {
        String csv = "什么是光合作用?,科学,中等,事实,植物利用光能\n" +
                     "写一个 Python 斐波那契函数,编程,简单,代码,def fib(n)\n";
        MultipartFile file = new MockMultipartFile(
                "file", "q.csv", "text/csv", csv.getBytes());

        QuestionService.ImportResult r = service.importBatch(file);
        assertEquals(2, r.success());
        assertEquals(0, r.failed());
        assertTrue(r.errorMessages().isEmpty());
    }

    // ============ TC-03-003 批量导入格式错误 ============
    @Test
    @DisplayName("TC-03-003:批量导入含空行 → 失败有明细")
    void t3_import_format_error() throws Exception {
        String csv = "正常题目,科学,简单,事实,answer\n\n   \n,空题,简单,事实,answer\n";
        MultipartFile file = new MockMultipartFile(
                "file", "q.csv", "text/csv", csv.getBytes());
        QuestionService.ImportResult r = service.importBatch(file);
        assertTrue(r.success() >= 1);
        assertTrue(r.failed() >= 1, "空内容应该记为失败");
        assertFalse(r.errorMessages().isEmpty());
    }

    // ============ TC-03-004 软删除 ============
    @Test
    @DisplayName("TC-03-004:软删除 → DB deleted=1")
    void t4_soft_delete() {
        Question exist = new Question();
        exist.setId(1L);
        exist.setDeleted(0);
        when(questionMapper.selectById(1L)).thenReturn(exist);

        service.delete(1L);

        // 用 wrapper 方式绕过 MyBatis-Plus 逻辑删除拦截
        verify(questionMapper).update(eq(null), any(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class));
    }

    // ============ TC-03-005 超长问题 ============
    @Test
    @DisplayName("TC-03-005:超长问题(>4000 字) → 拒绝")
    void t5_too_long_rejected() {
        QuestionRequest r = validRequest();
        r.setContent("测".repeat(4001));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(r, 1L));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        verify(questionMapper, never()).insert(any(Question.class));
    }

    // ============ 边界用例:更新/详情/分页 ============
    @Test
    @DisplayName("更新已存在的问题")
    void update_existing_ok() {
        Question exist = new Question();
        exist.setId(10L);
        exist.setContent("old");
        exist.setDeleted(0);
        when(questionMapper.selectById(10L)).thenReturn(exist);

        QuestionRequest r = validRequest();
        r.setContent("new content");
        Question upd = service.update(10L, r);
        assertEquals("new content", upd.getContent());
        assertEquals(10L, upd.getId());
        verify(questionMapper).updateById(any(Question.class));
    }

    @Test
    @DisplayName("更新不存在的问题 → 抛 DATA_NOT_FOUND")
    void update_not_found() {
        when(questionMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(99L, validRequest()));
        assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除不存在的问题 → 抛 DATA_NOT_FOUND")
    void delete_not_found() {
        when(questionMapper.selectById(404L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(404L));
        assertEquals(ErrorCode.DATA_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("无效的 difficulty → 拒绝")
    void invalid_difficulty() {
        QuestionRequest r = validRequest();
        r.setDifficulty("极难");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(r, 1L));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("无效的 type → 拒绝")
    void invalid_type() {
        QuestionRequest r = validRequest();
        r.setType("翻译");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(r, 1L));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
    }
}
