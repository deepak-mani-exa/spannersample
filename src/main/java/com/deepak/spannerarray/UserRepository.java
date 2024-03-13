package com.deepak.spannerarray;

import com.google.cloud.spanner.Struct;
import com.google.cloud.spring.data.spanner.repository.SpannerRepository;
import com.google.cloud.spring.data.spanner.repository.query.Query;
import java.util.List;

public interface UserRepository extends SpannerRepository<User, String> {

  @Query(" SELECT * FROM user WHERE (state,status) IN ( @statesAndStatues ) ")
  List<User> getUsersBasedOnState(Struct[] statesAndStatues);

  @Query(" SELECT * FROM user WHERE (state,status) IN unnest ( @statesAndStatues ) ")
  List<User> getUsersBasedOnStateStructList(List<Struct> statesAndStatues);

}