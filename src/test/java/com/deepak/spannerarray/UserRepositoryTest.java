package com.deepak.spannerarray;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.InstanceNotFoundException;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spring.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import com.google.cloud.spring.data.spanner.core.admin.SpannerSchemaUtils;
import com.google.cloud.spring.data.spanner.core.mapping.SpannerMappingContext;
import com.google.cloud.spring.data.spanner.core.mapping.SpannerPersistentEntity;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.SpannerEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class UserRepositoryTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserRepositoryTest.class);


  private static final String PROJECT_ID = "test-project";
  private static final String INSTANCE_ID = "test-instance";

  @Container
  public static SpannerEmulatorContainer spannerEmulator = new SpannerEmulatorContainer(
      DockerImageName.parse("gcr.io/cloud-spanner-emulator/emulator:1.4.0")
  );

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.cloud.gcp.spanner.emulator-host", spannerEmulator::getEmulatorGrpcEndpoint);
  }

  @TestConfiguration
  static class EmulatorConfiguration {

    @Bean
    CredentialsProvider googleCredentials() {
      return NoCredentialsProvider.create();
    }
  }

  @Autowired
  Spanner spanner;
  @Autowired
  SpannerDatabaseAdminTemplate spannerAdmin;
  @Autowired
  SpannerSchemaUtils spannerSchemaUtils;
  @Autowired
  SpannerMappingContext spannerMappingContext;

  @Autowired
  UserRepository userRepository;


  @BeforeEach
  public void setup() throws ExecutionException, InterruptedException {
    // Create an instance
    InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();
    InstanceId instanceId = InstanceId.of(PROJECT_ID, INSTANCE_ID);

    // InstanceAdminClient already prefixes (unintuitively) the instance ID w/
    // `projects/{projectId}`.
    try {
      instanceAdminClient.getInstance(instanceId.getInstance());
    } catch (InstanceNotFoundException e) {
      // If instance doesn't exist, create a new Spanner instance in the emulator
      OperationFuture<Instance, CreateInstanceMetadata> operationFuture =
          instanceAdminClient.createInstance(
              InstanceInfo.newBuilder(InstanceId.of(PROJECT_ID, INSTANCE_ID))
                  // make sure to use the special `emulator-config`
                  .setInstanceConfigId(InstanceConfigId.of(PROJECT_ID, "emulator-config"))
                  .build());
      operationFuture.get();
    }

    // Create the tables
    SpannerPersistentEntity<?> persistentEntity =
        spannerMappingContext.getPersistentEntity(User.class);
    if (!spannerAdmin.tableExists(persistentEntity.tableName())) {
      spannerAdmin.executeDdlStrings(
          Arrays.asList(spannerSchemaUtils.getCreateTableDdlString(User.class)),
          !spannerAdmin.databaseExists());
    }
  }

  @Test
  void testContextLoads() {

    User user1 = User.builder().id("User-1").name("User-1").state("Activated").status("enabled")
        .build();

    User user2 = User.builder().id("User-2").name("User-2").state("Invited").status("disabled")
        .build();

    User user3 = User.builder().id("User-3").name("User-2").state("Deleted").status("disabled")
        .build();

    User user4 = User.builder().id("User-4").name("User-2").state("Locked").status("enabled")
        .build();

    userRepository.saveAll(List.of(user1, user2, user3, user4));

    userRepository.findAll().forEach(System.out::println);

    Struct[] statesAndStatues = {
        Struct.newBuilder().set("state").to("Activated").set("status").to("enabled").build(),
        Struct.newBuilder().set("state").to("Invited").set("status").to("disabled").build(),
        Struct.newBuilder().set("state").to("Deleted").set("status").to("disabled").build()
    };


    List<User> usersBasedOnState = userRepository.getUsersBasedOnState(statesAndStatues);

    System.out.println("Users are -- " + usersBasedOnState);

    List<User> usersBasedOnStateStructList = userRepository.getUsersBasedOnStateStructList(
        Arrays.stream(statesAndStatues).toList());

  }


}
