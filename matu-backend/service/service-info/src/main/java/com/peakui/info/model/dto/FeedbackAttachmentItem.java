package com.peakui.info.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAttachmentItem {

    private String name;
    private String url;
    private Long size;
}
