package by.vs.erp.inventory.repository;

import by.vs.erp.inventory.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    @EntityGraph(attributePaths = {"part"})
    @Query("SELECT s FROM Stock s WHERE s.quantity <= :minLimit")
    Slice<Stock> findDeficientParts(@Param("minLimit") int minLimit, Pageable pageable);

    @Query("SELECT s FROM Stock s JOIN FETCH s.part WHERE s.part.id = :partId")
    Optional<Stock> findByPartId(@Param("partId") Long partId);

    @EntityGraph(attributePaths = {"part"})
    @Query("SELECT s FROM Stock s")
    Page<Stock> findAllWithParts(Pageable pageable);

    @EntityGraph(attributePaths = {"part"})
    @Query("""
           SELECT s FROM Stock s 
           JOIN s.part p 
           WHERE LOWER(p.oemNumber) LIKE LOWER(CONCAT('%', :text, '%')) 
              OR LOWER(p.name) LIKE LOWER(CONCAT('%', :text, '%')) 
              OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :text, '%'))
           """)
    Page<Stock> searchFallback(@Param("text") String text, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"part"})
    Page<Stock> findAll(Pageable pageable);
}
