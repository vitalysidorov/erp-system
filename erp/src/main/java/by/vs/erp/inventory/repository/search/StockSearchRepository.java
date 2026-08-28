package by.vs.erp.inventory.repository.search;

import by.vs.erp.inventory.document.StockIndexDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockSearchRepository extends ElasticsearchRepository<StockIndexDocument, String> {
}
