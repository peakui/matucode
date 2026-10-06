package com.peakui.post.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.search.SearchSourceItem;
import com.peakui.common.search.SearchSyncAccess;
import com.peakui.common.search.SearchSyncControllerSupport;
import com.peakui.post.mapper.PostCategoryMapper;
import com.peakui.post.mapper.PostImageMapper;
import com.peakui.post.mapper.PostMapper;
import com.peakui.post.mapper.PostTagMapper;
import com.peakui.post.mapper.PostTagRelationMapper;
import com.peakui.post.model.entity.Post;
import com.peakui.post.model.entity.PostCategory;
import com.peakui.post.model.entity.PostImage;
import com.peakui.post.model.entity.PostTag;
import com.peakui.post.model.entity.PostTagRelation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class PostSearchSyncController extends SearchSyncControllerSupport {
    private final PostMapper postMapper;
    private final PostCategoryMapper categoryMapper;
    private final PostTagRelationMapper tagRelationMapper;
    private final PostTagMapper tagMapper;
    private final PostImageMapper imageMapper;

    @Value("${search.sync.token:}")
    private String syncToken;

    @GetMapping("/posts/internal/search-documents")
    public ApiResponse<List<SearchSourceItem>> searchDocuments(
            @RequestHeader(value = "X-Search-Sync-Token", required = false) String token,
            @RequestParam(value = "afterId", defaultValue = "0") Long afterId,
            @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        SearchSyncAccess.requireToken(syncToken, token);
        requirePage(afterId, pageSize);
        List<Post> posts = postMapper.selectList(new QueryWrapper<Post>()
                .select("id", "category_id", "title", "summary", "status", "published_at", "created_at")
                .gt("id", afterId).eq("status", 1).orderByAsc("id").last("LIMIT " + pageSize));
        if (posts.isEmpty()) return ApiResponse.success(List.of());

        List<Long> ids = posts.stream().map(Post::getId).toList();
        List<Long> categoryIds = posts.stream().map(Post::getCategoryId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> categories = new LinkedHashMap<>();
        if (!categoryIds.isEmpty()) {
            categories.putAll(categoryMapper.selectList(new QueryWrapper<PostCategory>()
                            .select("id", "category_name").in("id", categoryIds))
                    .stream().collect(Collectors.toMap(PostCategory::getId, PostCategory::getCategoryName)));
        }
        Map<Long, List<String>> tags = new LinkedHashMap<>();
        List<PostTagRelation> relations = tagRelationMapper.selectList(new QueryWrapper<PostTagRelation>()
                .select("post_id", "tag_id").in("post_id", ids));
        if (!relations.isEmpty()) {
            Map<Long, String> tagNames = tagMapper.selectList(new QueryWrapper<PostTag>()
                            .select("id", "tag_name").in("id", relations.stream().map(PostTagRelation::getTagId).toList()))
                    .stream().collect(Collectors.toMap(PostTag::getId, PostTag::getTagName));
            for (PostTagRelation relation : relations) {
                if (tagNames.containsKey(relation.getTagId()))
                    tags.computeIfAbsent(relation.getPostId(), key -> new java.util.ArrayList<>())
                            .add(tagNames.get(relation.getTagId()));
            }
        }
        Map<Long, String> covers = new LinkedHashMap<>();
        imageMapper.selectList(new QueryWrapper<PostImage>().select("post_id", "image_url", "image_type", "sort_order")
                        .in("post_id", ids).orderByAsc("sort_order", "id"))
                .stream().filter(image -> "灏侀潰".equals(image.getImageType()))
                .forEach(image -> covers.putIfAbsent(image.getPostId(), image.getImageUrl()));
        return ApiResponse.success(posts.stream().map(post -> SearchSourceItem.builder()
                .id(post.getId().toString()).title(post.getTitle()).summary(post.getSummary())
                .categoryName(categories.get(post.getCategoryId())).coverUrl(covers.get(post.getId()))
                .tags(tags.getOrDefault(post.getId(), Collections.emptyList())).status(post.getStatus())
                .publishedAt(post.getPublishedAt()).createdAt(post.getCreatedAt()).build()).toList());
    }
}
