package mywild.core.rest;

import java.util.List;

public record Paged<T>(
    int pageNumber,
    int pageSize,
    long totalRecords,
    List<T> data, 
    boolean firstPage,
    boolean lastPage,
    String requestContinuation
) { }
