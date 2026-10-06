package com.peakui.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.peakui.common.search.SearchSourceItem;
import com.peakui.search.config.SearchProperties;
import com.peakui.search.model.IndexStatusVO;
import com.peakui.search.model.SearchDocument;
import com.peakui.search.model.SearchPageResponse;
import com.peakui.search.model.SearchResultItem;
import com.peakui.search.model.SearchType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ElasticsearchClient client;
    private final SearchProperties properties;

    public SearchPageResponse search(String keyword, String type, long pageNum, long pageSize) {
        String normalizedKeyword = validateKeyword(keyword);
        String normalizedType = validateType(type);
        validatePage(pageNum, pageSize);
        int from;
        try {
            from = Math.toIntExact(Math.multiplyExact(pageNum - 1, pageSize));
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "页码超出范围", exception);
        }
        try {
            String index = properties.getElasticsearch().getIndex();
            if (!client.indices().exists(ExistsRequest.of(request -> request.index(index))).value()) {
                return emptyPage(pageNum, pageSize);
            }
            var response = client.search(request -> request.index(index).from(from).size(Math.toIntExact(pageSize))
                    .query(buildQuery(normalizedKeyword, normalizedType)), SearchDocument.class);
            List<SearchResultItem> records = response.hits().hits().stream().map(hit -> hit.source())
                    .filter(item -> item != null).map(this::toResult).toList();
            long total = response.hits().total() == null ? records.size() : response.hits().total().value();
            return SearchPageResponse.builder().pageNum(pageNum).pageSize(pageSize).total(total)
                    .totalPages(total == 0 ? 0 : (total + pageSize - 1) / pageSize).records(records).build();
        } catch (IOException exception) {
            throw new SearchUnavailableException(exception);
        } catch (RuntimeException exception) {
            if (exception instanceof ResponseStatusException) throw exception;
            throw new SearchUnavailableException(exception);
        }
    }

    /** Reports whether the alias points at an index and how many documents it holds. */
    public IndexStatusVO getIndexStatus() {
        String alias = properties.getElasticsearch().getIndex();
        try {
            boolean exists = client.indices().exists(ExistsRequest.of(request -> request.index(alias))).value();
            long count = exists ? client.count(request -> request.index(alias)).count() : 0L;
            return IndexStatusVO.builder()
                    .alias(alias)
                    .indexExists(exists)
                    .documentCount(count)
                    .maxDocuments(properties.getSync().getMaxDocuments())
                    .build();
        } catch (IOException exception) {
            throw new SearchUnavailableException(exception);
        } catch (RuntimeException exception) {
            if (exception instanceof ResponseStatusException) throw exception;
            throw new SearchUnavailableException(exception);
        }
    }

    /** Writes a complete generation. The caller must only invoke this after all source pages succeeded. */
    public void replaceDocuments(List<SearchSourceItem> sourceItems) {
        List<SearchDocument> documents = sourceItems == null ? List.of() : sourceItems.stream()
                .map(this::toDocument).toList();
        String alias = properties.getElasticsearch().getIndex();
        String generation = alias + "_sync_" + System.currentTimeMillis();
        try {
            client.indices().create(request -> request.index(generation)
                    .withJson(getClass().getResourceAsStream("/search-index.json")));
            List<BulkOperation> operations = documents.stream().map(document -> BulkOperation.of(operation -> operation
                    .index(item -> item.index(generation).id(document.getType() + ":" + document.getId()).document(document)))).toList();
            if (!operations.isEmpty()) {
                var bulkResponse = client.bulk(request -> request.index(generation).operations(operations).refresh(co.elastic.clients.elasticsearch._types.Refresh.WaitFor));
                if (bulkResponse.errors()) {
                    throw new IllegalStateException("搜索索引批量写入包含失败项");
                }
            }
            boolean aliasExists = client.indices().existsAlias(request -> request.name(alias)).value();
            client.indices().updateAliases(request -> {
                if (aliasExists) {
                    request.actions(action -> action.remove(remove -> remove.alias(alias).index("*")));
                }
                return request.actions(action -> action.add(add -> add.alias(alias).index(generation)));
            });
        } catch (IOException | RuntimeException exception) {
            try {
                client.indices().delete(request -> request.index(generation));
            } catch (Exception cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new IllegalStateException("搜索索引写入失败", exception);
        }
    }

    private Query buildQuery(String keyword, String type) {
        BoolQuery.Builder bool = new BoolQuery.Builder()
                .filter(Query.of(query -> query.term(term -> term.field("status").value(FieldValue.of(1)))));
        if (!"all".equals(type)) {
            bool.filter(Query.of(query -> query.term(term -> term.field("type").value(type))));
        }
        bool.must(Query.of(query -> query.multiMatch(match -> match.query(keyword)
                .fields("title^4", "summary^2", "tags", "categoryName"))));
        return Query.of(query -> query.bool(bool.build()));
    }

    private SearchDocument toDocument(SearchSourceItem item) {
        SearchType type = SearchType.parse(item.getType());
        return SearchDocument.builder().id(item.getId()).type(type.value()).title(item.getTitle())
                .summary(item.getSummary()).tags(item.getTags()).categoryName(item.getCategoryName())
                .difficulty(item.getDifficulty()).coverUrl(item.getCoverUrl()).status(1)
                .targetPath(type.targetPath(item.getId())).publishedAt(item.getPublishedAt()).createdAt(item.getCreatedAt()).build();
    }

    private SearchResultItem toResult(SearchDocument item) {
        return SearchResultItem.builder().id(item.getId()).type(item.getType()).title(item.getTitle())
                .summary(item.getSummary()).tags(item.getTags()).categoryName(item.getCategoryName())
                .difficulty(item.getDifficulty()).coverUrl(item.getCoverUrl()).targetPath(item.getTargetPath())
                .publishedAt(item.getPublishedAt() == null ? null : item.getPublishedAt().toString())
                .createdAt(item.getCreatedAt() == null ? null : item.getCreatedAt().toString()).build();
    }

    private String validateKeyword(String keyword) {
        if (!StringUtils.hasText(keyword) || keyword.trim().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "关键词不能为空且长度不能超过100个字符");
        }
        return keyword.trim();
    }

    private String validateType(String type) {
        String normalized = type == null ? "all" : type.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type必须为all、article、course、interview或oj");
        }
        if (!"all".equals(normalized)) SearchType.parse(normalized);
        return normalized;
    }

    private void validatePage(long pageNum, long pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pageNum必须>=1，pageSize必须在1到50之间");
        }
    }

    private SearchPageResponse emptyPage(long pageNum, long pageSize) {
        return SearchPageResponse.builder().pageNum(pageNum).pageSize(pageSize).total(0).totalPages(0).records(List.of()).build();
    }
}
