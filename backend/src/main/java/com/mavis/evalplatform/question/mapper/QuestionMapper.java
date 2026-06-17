package com.mavis.evalplatform.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mavis.evalplatform.question.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/**
 * Question Mapper
 *
 * @author 向锏楠
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
    // BaseMapper 已提供 selectById / insert / updateById / selectPage / selectList
    // 如需自定义 SQL(如全文检索),在此处加方法并提供 XML
}
