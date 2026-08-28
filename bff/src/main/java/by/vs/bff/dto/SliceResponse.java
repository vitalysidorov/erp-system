package by.vs.bff.dto;

import java.util.List;

public record SliceResponse<T>(
        List<T> context,
        boolean hasNext,
        int number,
        int size
) {}
