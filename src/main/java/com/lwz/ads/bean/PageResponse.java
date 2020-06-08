package com.lwz.ads.bean;

import java.util.List;

/**
 * @author liweizhou 2020/6/8
 */
public class PageResponse<T> extends Response<List<T>> {

    private long pageIndex;

    private long pageSize;

    private long total;

    public long getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(long pageIndex) {
        this.pageIndex = pageIndex;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public static <T> PageResponse<T> success(long pageIndex, long pageSize, long total, List<T> data){
        PageResponse<T> response = new PageResponse<>();
        response.setPageIndex(pageIndex);
        response.setPageSize(pageSize);
        response.setTotal(total);
        response.setData(data);
        return response;
    }

}
