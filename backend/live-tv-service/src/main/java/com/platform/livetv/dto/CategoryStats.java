package com.platform.livetv.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CategoryStats {
    private String name;
    private Long channelCount;
}