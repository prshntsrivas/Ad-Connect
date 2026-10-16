package com.adconnect.search.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;
import java.time.LocalDate;

@Document(indexName = "campaigns")
@Setting(shards = 1, replicas = 0)
@Getter
@Setter
@NoArgsConstructor
public class CampaignDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long campaignId;

    @Field(type = FieldType.Long)
    private Long brandUserId;

    @Field(type = FieldType.Text)
    private String brandName;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String niche;

    @Field(type = FieldType.Keyword)
    private String platform;

    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)})
    private String city;

    @Field(type = FieldType.Double)
    private double budget;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate deadline;

    @Field(type = FieldType.Date)
    private Instant updatedAt;
}
