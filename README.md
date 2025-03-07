# 前言

不管是`DDD`也好，`CQRS`架构也好，虽然都做到了让领域对象不仅有**状态**，而且有**行为**，但还不够彻底。因为对象的行为总是“**被调用**”的。因为贫血模型的情况下，对象是提供了数据让别人去操作或者说被别人使用；而充血模型的情况下，对象则是提供了数据和行为，但还是让别人去操作或者说被别人使用。

> 真正的面向对象编程中的对象应该是一个”活“的具有主观能动性的存在于内存中的客观存在，它们不仅有状态而且还有自主行为。

1. 对象的状态可以表现出来被别人看到，但是必须是只读的，没有人可以直接去修改一个对象的状态，它的状态必须是由它自己的行为导致自己的状态的改变。
1. 对象的行为就是对象所具有的某种功能。对象的行为本质上应该是对某个消息的主动响应，这里强调的是主动，就是说对象的行为不可以被别人使用，而只能自己主动的去表现出该行为。

## 框架特色

- 一个`DDD`开发框架，完美支持基于六边形架构思想开发
- 实现`CQRS`架构，解决`CQRS`架构的`C`端的高并发写的问题，以及`CQ`两端数据同步的顺序性保证和幂等性，支持`C`端完成后立即返回`Command`的结果，也支持`CQ`两端都完成后才返回`Command`的结果
- 聚合根常驻内存（`In-Memory Domain Model`），可以完全以`OO`的方式来设计实现聚合根，不必为`ORM`的阻抗失衡而烦恼；
- 基于聚合根`ID` + 事件版本号的唯一索引，实现聚合根的乐观并发控制
- 通过聚合根`ID`对命令或事件进行路由，聚合根的处理基于`Actor`思想，做到最小的并发冲突、最大的并行处理，`Group Commit Domain event`
- 架构层面严格规范了开发人员该如何写代码，和`DDD`开发紧密结合，严格遵守聚合内强一致性、聚合之间最终一致性的原则
- 先进的`Saga`机制，以事件驱动的流程管理器（`Process Manager`）的方式支持一个用户操作跨多个聚合根的业务场景，如订单处理，从而避免分布式事务的使用
- 基于`ES`（`Event Sourcing`）的思想持久化`C`端的聚合根的状态，让`C`端的数据持久化变得通用化，具有一切`ES`的优点
- 通过基于分布式消息队列横向扩展的方式实现系统的可伸缩性（基于队列的动态扩容/缩容）

## 整体架构

`enode`是一个基于【`DDD`】【`CQRS`】【`ES`】【`EDA`】【`In-Memory`】架构风格的应用框架，实现了`CQRS`架构面临的大部分技术问题，让开发者可以专注于业务逻辑和业务流程的开发，而无需关心纯技术问题。

![](enode-arch.png)

## 最佳实践

基于`CQRS`思想

> [conference](https://github.com/anruence/conference)

## 详细介绍

[wiki](https://github.com/anruence/enode/wiki)

## 使用说明

`enode`在使用便利性了做了很多尝试和努力，而且针对消息队列和`EventStore`的实现对开发者都是开放的，同时和`Spring`高度集成，开箱即用。

## 启动配置

新增`@EnableEnode`注解，可自动配置`Bean`，简化了接入方式。

### `enode`启动配置

```java
@SpringBootApplication
@EnableEnode(value = "org.enodeframework.tests")
@ComponentScan(value = "org.enodeframework.tests")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
```

### `Spring Boot`启动配置文件

如果需要使用`RokcetMQ`和`ONS`的`tag`功能，相应的配置`spring.enode.mq.tag.*`属性即可：

```properties
# enode eventstore (memory, mysql, tidb, pg, mongo)
spring.enode.eventstore=mongo
# enode messagequeue (kafka, rocketmq, ons)
spring.enode.mq=kafka
spring.enode.mq.topic.command=EnodeBankCommandTopic
spring.enode.mq.topic.event=EnodeBankEventTopic
spring.enode.mq.topic.application=EnodeBankApplicationMessageTopic
spring.enode.mq.topic.exception=EnodeBankExceptionTopic
```

### `kafka listener bean`配置

```java
@Value("${spring.enode.mq.topic.command}")
private String commandTopic;

@Value("${spring.enode.mq.topic.event}")
private String eventTopic;

@Value("${spring.enode.mq.topic.application}")
private String applicationTopic;

@Value("${spring.enode.mq.topic.exception}")
private String exceptionTopic;

@Bean
public ConsumerFactory<String, String> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Constants.KAFKA_SERVER);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, Constants.DEFAULT_PRODUCER_GROUP);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(props);
}

@Bean
public ProducerFactory<String, String> producerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Constants.KAFKA_SERVER);
    props.put(ProducerConfig.RETRIES_CONFIG, 1);
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 1024000);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(props);
}

@Bean
public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
}

@Bean
public KafkaMessageListenerContainer<String, String> commandListenerContainer(KafkaMessageListener commandListener, ConsumerFactory<String, String> consumerFactory) {
    ContainerProperties properties = new ContainerProperties(commandTopic);
    properties.setGroupId(Constants.DEFAULT_CONSUMER_GROUP);
    properties.setMessageListener(commandListener);
    properties.setMissingTopicsFatal(false);
    return new KafkaMessageListenerContainer<>(consumerFactory, properties);
}

@Bean
public KafkaMessageListenerContainer<String, String> domainEventListenerContainer(KafkaMessageListener domainEventListener, ConsumerFactory<String, String> consumerFactory) {
    ContainerProperties properties = new ContainerProperties(eventTopic);
    properties.setGroupId(Constants.DEFAULT_PRODUCER_GROUP);
    properties.setMessageListener(domainEventListener);
    properties.setMissingTopicsFatal(false);
    properties.setAckMode(ContainerProperties.AckMode.MANUAL);
    return new KafkaMessageListenerContainer<>(consumerFactory, properties);
}

@Bean
public KafkaMessageListenerContainer<String, String> applicationMessageListenerContainer(KafkaMessageListener applicationMessageListener, ConsumerFactory<String, String> consumerFactory) {
    ContainerProperties properties = new ContainerProperties(applicationTopic);
    properties.setGroupId(Constants.DEFAULT_PRODUCER_GROUP);
    properties.setMessageListener(applicationMessageListener);
    properties.setMissingTopicsFatal(false);
    properties.setAckMode(ContainerProperties.AckMode.MANUAL);
    return new KafkaMessageListenerContainer<>(consumerFactory, properties);
}

@Bean
public KafkaMessageListenerContainer<String, String> publishableExceptionListenerContainer(KafkaMessageListener publishableExceptionListener, ConsumerFactory<String, String> consumerFactory) {
    ContainerProperties properties = new ContainerProperties(exceptionTopic);
    properties.setGroupId(Constants.DEFAULT_PRODUCER_GROUP);
    properties.setMessageListener(publishableExceptionListener);
    properties.setMissingTopicsFatal(false);
    properties.setAckMode(ContainerProperties.AckMode.MANUAL);
    return new KafkaMessageListenerContainer<>(consumerFactory, properties);
}
```

### `eventstore`数据源配置，目前支持(`MySQL`, `MongoDB`, `PostgreSQL` ...）

```java
@Bean("enodeMongoClient")
@ConditionalOnProperty(prefix = "spring.enode", name = "eventstore", havingValue = "mongo")
public MongoClient mongoClient() {
    return MongoClients.create();
}

@Bean("enodeSQLClient")
@ConditionalOnProperty(prefix = "spring.enode", name = "eventstore", havingValue = "jdbc-mysql")
public JDBCClient enodeMySQLClient(Vertx enodeVertx) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(com.mysql.cj.jdbc.Driver.class.getName());
        JDBCClient client = JDBCClient.create(enodeVertx, dataSource);
        return client;
}

@Bean("enodeMySQLPool")
@ConditionalOnProperty(prefix = "spring.enode", name = "eventstore", havingValue = "mysql")
public MySQLPool enodeMySQLPool() {
        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
        .setPort(3306)
        .setHost("127.0.0.1")
        .setDatabase("enode")
        .setUser(username)
        .setPassword(password);
        PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(5);
        return MySQLPool.pool(connectOptions, poolOptions);
}

@Bean("enodePgPool")
@ConditionalOnProperty(prefix = "spring.enode", name = "eventstore", havingValue = "pg")
public PgPool pgPool() {
        PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(3306)
        .setHost("127.0.0.1")
        .setDatabase("enode")
        .setUser(username)
        .setPassword(password);
        PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(5);
        return PgPool.pool(connectOptions, poolOptions);
}
```

### 事件表新建

#### `MySQL` & `TiDB`

```sql
CREATE TABLE event_stream (
  id BIGINT AUTO_INCREMENT NOT NULL,
  aggregate_root_type_name VARCHAR(256) NOT NULL,
  aggregate_root_id VARCHAR(36) NOT NULL,
  version INT NOT NULL,
  command_id VARCHAR(36) NOT NULL,
  gmt_create DATETIME NOT NULL,
  events MEDIUMTEXT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_aggregate_root_id_version (aggregate_root_id, version),
  UNIQUE KEY uk_aggregate_root_id_command_id (aggregate_root_id, command_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE published_version (
  id BIGINT AUTO_INCREMENT NOT NULL,
  processor_name VARCHAR(128) NOT NULL,
  aggregate_root_type_name VARCHAR(256) NOT NULL,
  aggregate_root_id VARCHAR(36) NOT NULL,
  version INT NOT NULL,
  gmt_create DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_processor_name_aggregate_root_id (processor_name, aggregate_root_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

#### `postgresql`

```sql
CREATE TABLE event_stream (
  id bigserial,
  aggregate_root_type_name varchar(256),
  aggregate_root_id varchar(36),
  version integer,
  command_id varchar(36),
  gmt_create date,
  events text,
  PRIMARY KEY (id),
  CONSTRAINT uk_aggregate_root_id_version UNIQUE (aggregate_root_id, version),
  CONSTRAINT uk_aggregate_root_id_command_id UNIQUE (aggregate_root_id, command_id)
);

CREATE TABLE published_version (
  id bigserial,
  processor_name varchar(128),
  aggregate_root_type_name varchar(256),
  aggregate_root_id varchar(36),
  version integer,
  gmt_create date,
  PRIMARY KEY (id),
  CONSTRAINT uk_processor_name_aggregate_root_id UNIQUE (processor_name, aggregate_root_id)
);
```

#### `MongoDB`

```js
db.event_stream.createIndex({aggregateRootId:1,commandId:1},{unique:true})
db.event_stream.createIndex({aggregateRootId:1,version:1},{unique:true})
db.published_version.createIndex({processorName:1,aggregateRootId:1},{unique:true})
```

### 编程方式

新增了三个注解，系统限定了只扫描`@Command`和`@Event`标识的类，执行的方法上需要添加`@Subscribe`注解：

- `@Command`
- `@Event`
- `@Subscribe`

启动时会扫描包路径下的注解，注册成`Spring Bean`，和`@Component`作用相同。

### 消息

**_更新了`kotlin`支持，`@Subscribe` 方法体支持`suspend`标记。_**

发送命令消息代码：

```java
CompletableFuture<CommandResult> future = commandService.executeAsync(createNoteCommand, CommandReturnType.EventHandled);
```

消费命令消息：

```java
/**
 * 银行账户相关命令处理
 * ICommandHandler<CreateAccountCommand>,                       //开户
 * ICommandAsyncHandler<ValidateAccountCommand>,                //验证账户是否合法
 * ICommandHandler<AddTransactionPreparationCommand>,           //添加预操作
 * ICommandHandler<CommitTransactionPreparationCommand>         //提交预操作
 */
@Command
public class BankAccountCommandHandler {
    /**
     * 开户
     */
    @Subscribe
    public void handleAsync(ICommandContext context, CreateAccountCommand command) {
        context.addAsync(new BankAccount(command.getAggregateRootId(), command.owner));
    }

    /**
     * 添加预操作
     */
    @Subscribe
    public void handleAsync(ICommandContext context, AddTransactionPreparationCommand command) {
        CompletableFuture<BankAccount> future = context.getAsync(command.getAggregateRootId(), BankAccount.class);
        BankAccount account = Task.await(future);
        account.addTransactionPreparation(command.transactionId, command.transactionType, command.preparationType, command.amount);
    }

    /**
     * 验证账户是否合法
     */
    @Subscribe
    public void handleAsync(ICommandContext context, ValidateAccountCommand command) {
        IApplicationMessage applicationMessage = new AccountValidatePassedMessage(command.getAggregateRootId(), command.transactionId);
        //此处应该会调用外部接口验证账号是否合法，这里仅仅简单通过账号是否以INVALID字符串开头来判断是否合法；根据账号的合法性，返回不同的应用层消息
        if (command.getAggregateRootId().startsWith("INVALID")) {
            applicationMessage = new AccountValidateFailedMessage(command.getAggregateRootId(), command.transactionId, "账户不合法.");
        }
        context.setApplicationMessage(applicationMessage);
    }

    /**
     * 提交预操作
     */
    @Subscribe
    public void handleAsync(ICommandContext context, CommitTransactionPreparationCommand command) {
        CompletableFuture<BankAccount> future = context.getAsync(command.getAggregateRootId(), BankAccount.class);
        BankAccount account = Task.await(future);
        account.commitTransactionPreparation(command.transactionId);
    }
}
```

领域事件消费：

```java
/**
 * 银行存款交易流程管理器，用于协调银行存款交易流程中各个参与者聚合根之间的消息交互
 * IMessageHandler<DepositTransactionStartedEvent>,                    //存款交易已开始
 * IMessageHandler<DepositTransactionPreparationCompletedEvent>,       //存款交易已提交
 * IMessageHandler<TransactionPreparationAddedEvent>,                  //账户预操作已添加
 * IMessageHandler<TransactionPreparationCommittedEvent>               //账户预操作已提交
 */
@Event
public class DepositTransactionProcessManager {

    @Autowired
    private ICommandService commandService;

    @Subscribe
    public void handleAsync(DepositTransactionStartedEvent evnt) {
        AddTransactionPreparationCommand command = new AddTransactionPreparationCommand(
                evnt.accountId,
                evnt.getAggregateRootId(),
                TransactionType.DEPOSIT_TRANSACTION,
                PreparationType.CREDIT_PREPARATION,
                evnt.amount);
        command.setId(evnt.getId());
        Task.await(commandService.sendAsync(command));
    }

    @Subscribe
    public void handleAsync(TransactionPreparationAddedEvent evnt) {
        if (evnt.transactionPreparation.transactionType == TransactionType.DEPOSIT_TRANSACTION
                && evnt.transactionPreparation.preparationType == PreparationType.CREDIT_PREPARATION) {
            ConfirmDepositPreparationCommand command = new ConfirmDepositPreparationCommand(evnt.transactionPreparation.TransactionId);
            command.setId(evnt.getId());
            Task.await(commandService.sendAsync(command));
        }
    }

    @Subscribe
    public void handleAsync(DepositTransactionPreparationCompletedEvent evnt) {
        CommitTransactionPreparationCommand command = new CommitTransactionPreparationCommand(evnt.accountId, evnt.getAggregateRootId());
        command.setId(evnt.getId());
        Task.await(commandService.sendAsync(command));
    }

    @Subscribe
    public void handleAsync(TransactionPreparationCommittedEvent evnt) {
        if (evnt.transactionPreparation.transactionType == TransactionType.DEPOSIT_TRANSACTION &&
                evnt.transactionPreparation.preparationType == PreparationType.CREDIT_PREPARATION) {
            ConfirmDepositCommand command = new ConfirmDepositCommand(evnt.transactionPreparation.TransactionId);
            command.setId(evnt.getId());
            Task.await(commandService.sendAsync(command));
        }
    }
}
```

### `MQ`配置启动

多选

#### `Kafka`

https://kafka.apache.org/quickstart

```bash
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties
```

#### `RocketMQ`

https://rocketmq.apache.org/docs/quick-start/

启动`RocketMQ`服务：

```bash
nohup sh bin/mqnamesrv &
nohup sh bin/mqbroker -n 127.0.0.1:9876 &
```

### `command-web`启动

- `CQRS`架构中的`Command`端应用

> 主要用来接收`Command`，将`Command`发送到消息队列。

### `command-consumer`启动

- 消费`Command`队列中的消息的服务

> 将领域事件消息持久化才算是`Command`执行成功，`Command`执行的结果可以通过发送命令时注册的监听器获取。

### `event-consumer`启动

- 领域事件处理服务

> 事件可能会多次投递，所以需要消费端逻辑保证幂等处理，这里框架无法完成支持，需要开发者自己实现。

## 转账的例子

转账的业务场景，涉及了三个聚合根：

- 银行存款交易记录，表示一笔银行存款交易
- 银行转账交易记录，表示一笔银行内账户之间的转账交易
- 银行账户聚合根，封装银行账户余额变动的数据一致性

## 测试

- 接入了`OpenApi 3.0`，打开`swagger-ui`即可。  
  http://localhost:8080/swagger-ui.html

## FAQ

### 聚合根的定义

聚合根需要定义一个无参构造函数，因为聚合根初始化时使用了。

```java
aggregateRootType.getDeclaredConstructor().newInstance();
```

### 为什么采用异步单一长连接?

因为服务的现状大都是服务提供者少，通常只有几台机器，而服务的消费者多，可能整个网站都在访问该服务。
在我们的这个场景里面，`command-web`只需要很少的机器就能满足前端大量的请求，`command-consumer`和`event-consumer`的机器相对较多些。
如果采用常规的“单请求单连接”的方式，服务提供者很容易就被压跨，通过单一连接，保证单一消费者不会压死提供者，长连接，减少连接握手验证等，并使用异步`IO`，复用线程池，防止`C10K`问题。

### `ICommandHandler`和`ICommandAsyncHandler`区别 (现在合并成一个了，但处理思路没变)

`ICommandHandler`是为了操作内存中的聚合根的，所以不会有异步操作，但后来`ICommandHandler`的`Handle`方法也设计为了`handleAsync`了，目的是为了异步到底，否则异步链路中断的话，异步就没效果了
而`ICommandAsyncHandler`是为了让开发者调用外部系统的接口的，也就是访问外部`IO`，所以用了`Async
ICommandHandler`，`ICommandAsyncHandler`这两个接口是用于不同的业务场景，`ICommandHandler.handleAsync`方法执行完成后，框架要从`context`中获取当前修改的聚合根的领域事件，然后去提交。而`ICommandAsyncHandler.handleAsync`方法执行完成后，不会有这个逻辑，而是看一下`handleAsync`方法执行的异步消息结果是什么，也就是`IApplicationMessage`。
目前已经删除了`ICommandAsyncHandler`，统一使用`ICommandHandler`来处理，异步结果会放在`context`中

### `ICommandService` `sendAsync` 和 `executeAsync`的区别

`sendAsync`只关注发送消息的结果
`executeAsync`发送消息的同时，关注命令的返回结果，返回的时机如下：

- `CommandReturnType.CommandExecuted`：`Command`执行完成，`Event`发布成功后返回结果
- `CommandReturnType.EventHandled`：`Event`处理完成后才返回结果

### `event`使用哪个订阅者发送处理结果

`event`的订阅者可能有很多个，所以`enode`只要求有一个订阅者处理完事件后发送结果给发送命令的人即可，通过`defaultDomainEventMessageHandler`中`sendEventHandledMessage`参数来设置是否发送，最终来决定由哪个订阅者来发送命令处理结果。


## 参考项目

- https://github.com/tangxuehua/enode
- https://github.com/coffeewar/enode-master
