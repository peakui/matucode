package com.peakui.course.service;

import com.peakui.course.exception.CourseException;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VideoMetadataService {

    public Integer parseDurationSeconds(String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            throw new CourseException("视频地址为空，无法解析时长");
        }

        FFmpegFrameGrabber grabber = null;
        try {
            grabber = new FFmpegFrameGrabber(videoUrl);
            grabber.start();
            long durationMicros = grabber.getLengthInTime();
            if (durationMicros <= 0) {
                throw new CourseException("解析视频时长失败");
            }
            return Math.max(1, (int) Math.ceil(durationMicros / 1_000_000d));
        } catch (CourseException e) {
            throw e;
        } catch (Exception e) {
            throw new CourseException("解析视频时长失败");
        } finally {
            if (grabber != null) {
                try {
                    grabber.stop();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
