package by.vs.erp.inventory.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.math.BigDecimal;

@Getter
@Setter
@Document(indexName = "inventory_stocks")
public class StockIndexDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long partId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String oemNumber;

    @Field(type = FieldType.Text, analyzer = "russian")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String brand;

    @Field(type = FieldType.Integer)
    private Integer quantity;

    @Field(type = FieldType.Double)
    private BigDecimal retailPrice;

    @Field(type = FieldType.Double)
    private BigDecimal purchasePrice;
}
