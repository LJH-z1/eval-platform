package com.mavis.evalplatform.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果
 *
 * @param <T> 元素类型
 * @author 刘家豪
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "数据列表")
    private List<T> list;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Long pageNum;

    @Schema(description = "每页大小")
    private Long pageSize;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        PageResult<T> p = new PageResult<>();
        p.list = list == null ? Collections.emptyList() : list;
        p.total = total;
        p.pageNum = pageNum;
        p.pageSize = pageSize;
        return p;
    }

    public static <T> PageResult<T> empty() {
        PageResult<T> p = new PageResult<>();
        p.list = Collections.emptyList();
        p.total = 0L;
        p.pageNum = 1L;
        p.pageSize = 10L;
        return p;
    }
}
