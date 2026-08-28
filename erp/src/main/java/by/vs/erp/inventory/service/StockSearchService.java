package by.vs.erp.inventory.service;

import by.vs.erp.inventory.document.StockIndexDocument;
import by.vs.erp.inventory.entity.Stock;
import by.vs.erp.inventory.repository.StockRepository;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final StockRepository stockRepository;

    public Page<StockIndexDocument> search(String text, Pageable pageable) {
        if (text == null || text.trim().isEmpty()) {
            return Page.empty();
        }

        try {
            return searchInElastic(text, pageable);
        } catch (Exception e) {
            log.warn("Поиск через Elasticsearch дал сбой. Переключение на резервный поиск через СУБД");
            return searchInDatabaseFallback(text, pageable);
        }
    }

    private Page<StockIndexDocument> searchInElastic(String text, Pageable pageable) {
        if (text == null || text.isBlank()) {
            return Page.empty(pageable);
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(QueryBuilders.multiMatch(mm -> mm
                        .query(text)
                        .fields("oemNumber^3", "name^2", "brand")
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                ))
                .withPageable(pageable)
                .build();

        SearchHits<StockIndexDocument> searchHits = elasticsearchOperations.search(query, StockIndexDocument.class);
        List<StockIndexDocument> content = searchHits.get().map(SearchHit::getContent).collect(Collectors.toList());

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }


    private Page<StockIndexDocument> searchInDatabaseFallback(String text, Pageable pageable) {
        Page<Stock> jpaResult = stockRepository.searchFallback(text, pageable);

        List<StockIndexDocument> fallbackDocs = jpaResult.getContent().stream()
                .map(stock -> {
                    StockIndexDocument doc = new StockIndexDocument();
                    doc.setId(String.valueOf(stock.getId()));
                    doc.setQuantity(stock.getQuantity());
                    doc.setRetailPrice(stock.getRetailPrice());
                    if (stock.getPart() != null) {
                        doc.setOemNumber(stock.getPart().getOemNumber());
                        doc.setName(stock.getPart().getName());
                        doc.setBrand(stock.getPart().getBrand());
                    }
                    return doc;
                }).collect(Collectors.toList());

        return new PageImpl<>(fallbackDocs, pageable, jpaResult.getTotalElements());
    }
}
