package com.lwz.ads.bean;

import lombok.Data;

import java.util.List;

/**
 * @author liweizhou 2020/6/8
 */
@Data
public class PageResponse<T> extends Response<List<T>> {

    private long pageIndex;

    private long pageSize;

    private long total;

    public static <T> PageResponse<T> success(long pageIndex, long pageSize, long total, List<T> data){
        PageResponse<T> response = new PageResponse<>();
        response.setPageIndex(pageIndex);
        response.setPageSize(pageSize);
        response.setTotal(total);
        response.setData(data);
        return response;
    }

    public static <T> PageResponse<T> empty(){
        return new PageResponse<>();
    }

}
