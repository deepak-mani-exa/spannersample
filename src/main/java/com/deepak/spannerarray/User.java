package com.deepak.spannerarray;


import com.google.cloud.spring.data.spanner.core.mapping.PrimaryKey;
import com.google.cloud.spring.data.spanner.core.mapping.Table;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Table(name = "user")
@Data
@Builder
public class User {

    @Id
    @PrimaryKey
    private String id;
    private String name;
    private String state;
    private String status;

}