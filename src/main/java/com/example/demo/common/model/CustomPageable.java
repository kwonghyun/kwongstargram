package com.example.demo.common.model;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.validation.constraints.NotBlank;

public class CustomPageable extends PageRequest {
    /**
     * Creates a new {@link PageRequest} with sort parameters applied.
     *
     * @param page zero-based page index, must not be negative.
     * @param size the size of the page to be returned, must be greater than 0.
     * @param sort must not be {@literal null}, use {@link Sort#unsorted()} instead.
     */
    public CustomPageable(@NotBlank Integer page, @NotBlank Integer size, Sort sort) {
        super(page, size, sort);
    }
}
