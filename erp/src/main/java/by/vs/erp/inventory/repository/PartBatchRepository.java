package by.vs.erp.inventory.repository;

import by.vs.erp.inventory.entity.PartBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartBatchRepository extends JpaRepository<PartBatch, Long> {

    @Query("SELECT pb FROM PartBatch pb " +
            "WHERE pb.part.id = :partId " +
            "AND pb.availableQuantity > 0 " +
            "ORDER BY pb.receivedAt ASC")
    List<PartBatch> findAvailableBatches(@Param("partId") Long partId);
}
