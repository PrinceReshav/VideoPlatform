package com.platform.livetv.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CountryStats {
    private String code;
    private String name;
    private Long channelCount;
}