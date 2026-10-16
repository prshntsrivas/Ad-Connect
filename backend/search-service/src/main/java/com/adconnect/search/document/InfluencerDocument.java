package com.adconnect.search.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;
import java.util.List;

@Document(indexName = "influencers")
@Setting(shards = 1, replicas = 0)
@Getter
@Setter
@NoArgsConstructor
public class InfluencerDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long userId;

    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)})
    private String displayName;

    /** Edge n-gram style prefix matching for autocomplete. */
    @Field(type = FieldType.Search_As_You_Type)
    private String nameSuggest;

    @Field(type = FieldType.Text)
    private String bio;

    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)})
    private String city;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Keyword)
    private List<String> niches;

    @Field(type = FieldType.Keyword)
    private List<String> platforms;

    @Field(type = FieldType.Long)
    private long instagramFollowers;

    @Field(type = FieldType.Long)
    private long youtubeSubscribers;

    @Field(type = FieldType.Long)
    private long twitterFollowers;

    @Field(type = FieldType.Long)
    private long totalFollowers;

    @Field(type = FieldType.Double)
    private double engagementRate;

    @Field(type = FieldType.Double)
    private double pricePerPost;

    @Field(type = FieldType.Double)
    private double rating;

    @Field(type = FieldType.Integer)
    private int completedDeals;

    @Field(type = FieldType.Boolean)
    private boolean verified;

    @Field(type = FieldType.Date)
    private Instant updatedAt;
}
