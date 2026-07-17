# taol

<p align="center">
	<a target="_blank" href="https://central.sonatype.com/artifact/io.github.taowater/taol-core">
		<img src="https://img.shields.io/maven-central/v/io.github.taowater/taol-core.svg?label=Maven%20Central" />
	</a>
	<a target="_blank" href="https://github.com/taowater/taol/blob/main/LICENSE">
		<img src="https://img.shields.io/github/license/taowater/taol.svg" />
	</a>
	<a target="_blank" href="https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html">
		<img src="https://img.shields.io/badge/JDK-8+-green.svg" />
	</a>
	<a target="_blank" href='https://github.com/taowater/taol'>
		<img src="https://img.shields.io/github/stars/taowater/taol.svg?style=social" alt="github star"/>
	</a>
</p>

轻量 Java 工具库，面向日常 CRUD 中的 Bean 拷贝、判空、集合与异步等常见操作。

### 🍊Maven

```xml
<dependency>
    <groupId>io.github.taowater</groupId>
    <artifactId>taol-core</artifactId>
    <version>LATEST</version>
</dependency>
```

### 废话

写业务时总会反复碰到这些事：DTO / VO / Entity 互相拷贝、各种判空、逗号分隔 ID 拆开、偶尔再来点异步拼装。

现成方案很多——Hutool、Spring `BeanUtils`、CGLIB、MapStruct……它们各有所长，但有时也会觉得：

* 反射式拷贝用起来顺手，类型转换与空值语义却不够统一
* 编译期方案（如 MapStruct）性能好，但要额外写 Mapper，改字段成本更高
* 判空、切割、异步这类小工具又散落在各处，每次都要自己拼一点样板代码

`taol` 就是在这些场景里攒出来的小工具箱。核心是带计划缓存的 Bean 拷贝：同名属性按 getter/setter 匹配，数值拓宽/窄化、集合元素转换、日期时间转换会尽量自动处理；其余则是一些在业务里高频出现的工具方法。

它没有什么很高深的东西，更多是封装与取舍。若能帮你少写几行样板、少踩几次拷贝坑，便已足够。

姊妹库：[ztream](https://github.com/taowater/ztream)（Java Stream 增强）

[![Star History Chart](https://api.star-history.com/svg?repos=taowater/taol&type=Date)](https://star-history.com/#taowater/taol&Date)

### 示例

#### Bean 拷贝 / 转换

```java
// 新建目标并拷贝同名属性
UserVO vo = ConvertUtil.convert(user, UserVO.class);

// 写入已有目标（source / target 为 null 时直接返回，不抛异常）
UserVO vo = new UserVO();
ConvertUtil.copy(user, vo);
```

同名属性会按 getter/setter 匹配。常见能力包括：

* 基本类型与包装类型、数值拓宽 / 窄化（窄化越界或无法安全转换时抛 `CopyException`）
* 集合 / 数组元素转换（如 `List<Long>` → `List<Integer>`）
* 日期时间互转（`Date` / `Timestamp` / `LocalDate` / `LocalDateTime` / `Instant` 等）

```java
// 例如：源字段 List<Long> ids，目标字段 List<Integer> ids
OrderVO vo = ConvertUtil.convert(order, OrderVO.class);
```

拷贝计划按「源类型 + 目标类型」缓存，同类型对重复拷贝时走预编译动作，适合 DTO / VO / Entity 之间的高频转换。

#### 判空

```java
// 支持 null、字符串、数组、Map、Iterable、Iterator，以及实现 Emptyable 的自定义类型
if (EmptyUtil.isEmpty(list)) {
    return;
}

if (EmptyUtil.isBlank(name)) {
    return;
}

// 是否存在空 / 是否全非空
boolean hadEmpty = EmptyUtil.isHadEmpty(name, ids, options);
boolean allPresent = EmptyUtil.isAllNotEmpty(name, ids);
```

#### 集合

```java
// 越界或不存在时返回 null，而不是抛异常
String first = CollUtil.get(names, 0);
Integer second = CollUtil.get(numbers, 1);

List<String> list = CollUtil.list("Ada", "Lin");
String[] defaults = CollUtil.arr("unknown", 3);
```

#### 字符串切割

```java
// 默认按逗号切割：去空白、去空串、去重
List<String> tags = SplitUtil.split("java, spring, java");
// ["java", "spring"]

List<Long> ids = SplitUtil.splitLong("1,2,3");

// 自定义分隔符、转换逻辑、是否去重
List<Integer> ports = SplitUtil.split("8080|8081", "\\|", Integer::valueOf, false);
```

#### 异步

```java
AsyncScope scope = AsyncScope.build()
        .executor(ForkJoinPool.commonPool())
        .timeout(5)
        .unit(TimeUnit.SECONDS)
        .returnNullIfEx(true)
        .build();

AsyncFuture<Integer> future = scope.supply(() -> loadCount());
Integer count = future.join(0);

// 并行执行多个任务并收集结果
List<Object> values = scope.all(
        () -> loadUser(),
        () -> loadOrders()
);
```

#### 反射 / Lambda

```java
// 含继承字段的反射获取（带缓存）
Field field = ReflectUtil.getField(User.class, "name");

// 通过可序列化函数式接口解析返回类型、参数类型
Class<String> returnType = LambdaUtil.getReturnClass((Function1<Object, String>) Object::toString);
```

#### 组合注解

类似 Spring 的元注解 / `@AliasFor` 能力，可解析直接注解或元注解上的属性，并处理别名合并：

```java
@Retention(RetentionPolicy.RUNTIME)
@Ann(attr1 = "fromMeta")
@interface ApiAnn {
    @AliasFor(annotation = Ann.class, value = "attr2")
    String value() default "";
}

@ApiAnn("create")
class CreateController {
}

Ann ann = AnnotationUtil.getAnnotation(CreateController.class, Ann.class);
```

#### 元组

```java
Tuple<String, Integer> result = Tuple.of("success", 200);
String message = result.getLeft();
Integer status = result.getRight();
```

其他更多方法可从对应工具类点出来看看：`ConvertUtil`、`EmptyUtil`、`CollUtil`、`SplitUtil`、`AsyncScope`、`ReflectUtil`、`LambdaUtil`……

### 模块说明

| 模块 | 说明 |
| --- | --- |
| `taol-core` | 正式发布的工具库 |
| `convert-test` | 拷贝正确性与性能粗测（开发参考，不发布） |

### License

[MIT](https://github.com/taowater/taol/blob/main/LICENSE)
