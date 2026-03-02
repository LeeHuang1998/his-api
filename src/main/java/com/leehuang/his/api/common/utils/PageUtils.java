package com.leehuang.his.api.common.utils;

import lombok.Data;

import java.util.List;

@Data
public class PageUtils<T> {

    /**
     * 总记录数
     */
    private long totalCount;

    /**
     * 每页记录数
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int totalPage;

    /**
     * 当前页数
     */
    private int pageIndex;

    /**
     * 分页数据
     */
    private List<T> pageList;

    public PageUtils(long totalCount, int pageSize, int pageIndex, List<T> pageList) {
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.pageIndex = pageIndex;
        this.pageList = pageList;
        // 总页数 = 总记录数 / 每页记录数
        // （得到的结果为整数，将任意一个数转为浮点数得到的结果就是浮点数，再向上取整，得到的结果为浮点数，再强转为 int）
        this.totalPage = (int) Math.ceil((double) totalCount / pageSize);
    }
}
