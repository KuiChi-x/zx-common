<div align="center">

# zx-common

**开箱即用的 Java 21 + Spring Boot 3 通用组件库**

分布式锁、注解式 HTTP 客户端、注解式爬虫、统一响应、JPA 通用查询，六个模块按需引入，零公司内部依赖。

[![Maven Central](https://img.shields.io/maven-central/v/cn.supzx.utils/zx-common?label=Maven%20Central&color=blue)](https://central.sonatype.com/artifact/cn.supzx.utils/zx-common)
[![Build](https://github.com/KuiChi-x/zx-common/actions/workflows/build.yml/badge.svg)](https://github.com/KuiChi-x/zx-common/actions/workflows/build.yml)
[![JDK](https://img.shields.io/badge/JDK-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-D22128)](https://www.apache.org/licenses/LICENSE-2.0)

</div>

---

## ✨ 特性

- **按需引入** — 六个模块相互独立，只有 `common-base` 是公共底座，不想要的功能不会进你的 classpath。
- **自动装配** — 需要 Spring 容器的模块都带 `AutoConfiguration.imports`，加依赖即生效，不用自己写 `@ComponentScan`。
- **注解优先** — 加锁 `@RedisLock`、调远程 `@RequestClient`、爬页面 `@FieldSelect`，业务代码里只留一行注解。
- **Spring 原生风格** — HTTP 客户端复用 `@GetMapping`、`@RequestBody` 等 Spring MVC 注解，不用学新的 DSL。
- **不抢宿主版本** — `spring-boot-autoconfigure` 与 `slf4j-api` 都以 `provided` 引入，Spring Boot 版本和日志实现由你的工程决定。

## 📦 模块

| 模块 | 说明 | 关键 API |
|---|---|---|
| [`common-base`](common-base) | JSON、集合、日期、反射、HTTP、AOP、重试工具集与统一响应模型 | `JsonUtils` `ListUtils` `HttpClientUtil` `SpringManager` `RetryMonitor` `@LogAround` `BaseResponse` |
| [`common-lock`](common-lock) | 基于 Jedis 的 Redlock 分布式锁切面，锁 key 支持 SpEL | `@RedisLock` `RedLock` |
| [`common-rpc`](common-rpc) | 注解式 HTTP 客户端，JDK 动态代理 + 域名轮询负载均衡 | `@EnableHttpRequest` `@RequestClient` `RequestConfig` |
| [`common-crawl`](common-crawl) | Jsoup / HtmlUnit 双策略抓取，CSS 选择器直接映射到实体字段 | `CrawlerActuator` `@RootSelect` `@FieldSelect` |
| [`common-web`](common-web) | 统一响应包装与全局异常处理 | `ResponseAdvice` `ServerExceptionHandler` |
| [`common-repository`](common-repository) | JPA 通用条件查询、分页、假删，以及零代码 CRUD Controller | `BaseRepository` `BaseControllerModel` `@ModelMapping` |

依赖关系很简单：`common-base` 是底座，其余五个模块各自依赖它，彼此之间不互相依赖。

## 🚀 快速开始

要求 JDK 21、Maven 3.9+、Spring Boot 3.5+。

```xml
<properties>
    <zx-common.version>2.0.0</zx-common.version>
</properties>

<dependencies>
    <dependency>
        <groupId>cn.supzx.utils</groupId>
        <artifactId>common-base</artifactId>
        <version>${zx-common.version}</version>
    </dependency>
    <!-- 按需追加 common-lock / common-rpc / common-crawl / common-web / common-repository -->
</dependencies>
```

> 本仓库源码当前版本为 `2.0.0`，Maven Central 上已发布的最新版本见顶部徽章。

加完依赖就能用，Controller 照常写：

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("/{id}")
    public OrderVO get(@PathVariable Long id) {
        return orderService.get(id);
    }
}
```

引入 `common-web` 后返回值被自动包装成统一结构，不用手写 `BaseResponse`：

```json
{ "code": 0, "message": "success", "data": { "id": 1 } }
```

异常也被 `ServerExceptionHandler` 统一接住：业务异常 `BaseException` 和参数校验失败返回 HTTP 200 加错误码，未捕获异常返回 500。

## 📖 模块用法

<details>
<summary><b>common-lock · 分布式锁</b></summary>

启动时把 Redis 节点交给切面，之后在方法上加注解即可。锁 key 支持 SpEL，可以直接引用方法参数：

```java
@RedisLock(lockKey = "#{#order.id}", leaseTime = 10_000L, waitTime = 3_000L)
public void pay(Order order) {
    // 同一个 order.id 串行执行
}
```

```java
@Component
public class RedisNodeInitializer implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        RedisLockAspect.jedisList = List.of(new Jedis("127.0.0.1", 6379));
    }
}
```

`leaseTime` 是锁租期（毫秒，默认 30000），`waitTime` 是最长等待时间（默认 0 表示不限制），超时抛 `RedLockException`。多节点时按 Redlock 思路要求过半节点加锁成功，解锁走 Lua 脚本校验持有者，不会误删别人的锁。

</details>

<details>
<summary><b>common-rpc · 注解式 HTTP 客户端</b></summary>

在启动类上开启扫描，扫描范围是该类所在的包：

```java
@SpringBootApplication
@EnableHttpRequest
public class Application { }
```

用 Spring MVC 注解声明接口，方法签名就是请求定义：

```java
@RequestClient(domainEnvironment = "user.service.url",
               domains = {"http://127.0.0.1:8080", "http://127.0.0.1:8081"})
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserVO getById(@PathVariable("id") Long id);

    @GetMapping("/api/users")
    List<UserVO> search(@RequestParam("keyword") String keyword);

    @PostMapping("/api/users")
    BaseResponse<Void> create(@RequestBody UserVO user);
}
```

注入后直接调用，返回值按方法泛型反序列化。

- `domainEnvironment` 指向配置项（例如 `user.service.url=http://a,http://b`），配置存在时覆盖 `domains`。
- 多个域名按接口维度轮询；域名不带协议时自动补 `http://`。
- 请求前的统一处理（加签、鉴权头等）实现 `RequestConfig`，再用 `@RequestClient(config = XxxConfig.class)` 指定。默认实现只设置 `Content-Type: application/json`。

</details>

<details>
<summary><b>common-crawl · 注解式爬虫</b></summary>

把页面结构用 CSS 选择器声明在实体上：

```java
@RootSelect(cssQuery = "#content")
public class Article {

    @FieldSelect(cssQuery = "h1.title")
    private String title;

    @FieldSelect(cssQuery = "span.views")
    private Integer views;

    @FieldSelect(cssQuery = "a.link", selectType = SelectTypeEnum.ATTR, selectAttr = "href")
    private List<String> links;
}
```

```java
CrawlerRequest request = CrawlerRequest.builder()
        .url("https://example.com/post/1")
        .strategy(ActuatorStrategyEnum.HTML_UNIT)   // 默认 JSOUP
        .headers(Map.of("Accept-Language", "zh-CN"))
        .timeoutMillis(8_000L)
        .build();

Article article = CrawlerActuator.crawl(request, Article.class);
```

`JSOUP` 直接取静态 HTML，`HTML_UNIT` 会执行 JavaScript 后再解析，适合前端渲染的页面。两种策略都支持 cookie、请求头、UA、referrer、代理和 POST。`selectType` 可选 `TEXT`（默认）、`VAL`、`ATTR`；`List` 字段自动收集全部匹配节点，字段类型转换由 `ReflectUtils` 处理。

这个模块只有静态方法，不依赖 Spring 容器，普通 main 方法里也能直接用。

</details>

<details>
<summary><b>common-repository · JPA 通用查询与零代码 CRUD</b></summary>

Repository 继承 `BaseRepository`，直接获得条件查询、分页和假删：

```java
public interface UserRepository extends BaseRepository<User, Long> { }
```

```java
// 字符串字段默认模糊匹配，excludeLikeAttr 里的字段走精确匹配
Page<User> page = userRepository.findByPage(
        Map.of("name", "张", "status", "1"),
        1, 20, List.of("status"), "id");

List<User> all = userRepository.findByConditions(Map.of("status", "1"), null, "id");
User       one = userRepository.findOneByAttr("code", "U1001");
userRepository.deleteValid("1,2,3");   // 把 valid 字段置 0
```

再往上一层，继承 `BaseControllerModel<VO, Entity>` 就能拿到一整套 REST 接口，方法体一行不用写：

```java
@RestController
public class UserController extends BaseControllerModel<UserVO, User> { }
```

路由前缀是 `common/` 加上 controller 名去掉 `Controller` 并首字母小写，上面这个类对应 `common/user`：

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/common/user` | 新增 |
| `PUT` | `/common/user` | 更新（需带 id） |
| `DELETE` | `/common/user/{ids}` | 假删，多个 id 逗号分隔 |
| `GET` | `/common/user/{attr}/{condition}` | 单条件精确查询，返回单个对象 |
| `GET` | `/common/user/list/{attr}/{condition}` | 单条件精确查询，返回列表 |
| `GET` | `/common/user/findAll` | 多条件组合查询 |
| `GET` | `/common/user/findByPage` | 多条件组合分页查询 |

分页参数 `current` 默认 1、`pageSize` 默认 20，排序用 `sorter={"id":"descend"}`（`ascend` 升序 / `descend` 降序）。接口自带 springdoc 注解，Swagger UI 上直接可读。

几个约定：假删依赖实体上的 `valid` 字段（1 有效 / 0 删除），没有该字段时记一条 warn 并跳过；仓库与实体扫描按包名 `**.repository` 和 `**.entity`；Repository bean 名按 `xxxRepository` 约定查找。

</details>

<details>
<summary><b>common-base · 工具集</b></summary>

```java
// JSON，内置 JavaTime / Jdk8 / ParameterNames 模块，忽略未知字段、不输出 null
String json = JsonUtils.toJson(user);
User   user = JsonUtils.fromJson(json, User.class);
List<User> users = JsonUtils.fromJson(json, new TypeReference<List<User>>() {});

// 集合
PageVO<User> page  = ListUtils.toPage(users, 1, 20);
List<List<User>> batches = ListUtils.partition(users, 500);
List<User> diff    = ListUtils.getListDiff(oldList, newList);

// HTTP
UserVO vo = HttpClientUtil.get("https://api.example.com/users/1", UserVO.class);
UserVO ok = HttpClientUtil.post(headers, url, body, UserVO.class);

// 在非 Spring 管理的类里取 Bean
UserService service = SpringManager.getBean(UserService.class);

// 失败后进重试队列，按固定间隔重试指定次数
RetryMonitor.registry(() -> notifyService.send(msg), 3);
```

方法级出入参日志与业务异常包装：

```java
@LogAround(detailMessage = "下单失败")
public Order create(OrderCmd cmd) { ... }
```

`@LogAround` 打印入参、返回值和耗时；抛异常时用 javassist 动态生成对应的业务异常类型（`XxxService` → `XxxException`）再抛出，方便上层按模块区分。不想用 AOP 的场景直接调 `MethodExecuteUtils.logAround(request, fn)`，用 lambda 包住要执行的逻辑即可。

另外还有 `BeanUtil`、`ClassUtils`、`ReflectUtils`、`AopHelper`、`SpelUtils`、`DateUtils`、`GzipUtils`、`BaseConverter` 等，按需翻源码。

</details>

## 🏗️ 构建

```bash
mvn -DskipTests verify              # 构建全部模块
mvn -pl common-base -am verify      # 只构建某个模块及其依赖
```

CI 在 `master` / `main` 的 push 和所有 PR 上跑同样的命令，见 `.github/workflows/build.yml`。

## 📤 发布到 Maven Central

版本号由人工维护，推 tag 触发 `.github/workflows/release.yml` 自动构建、GPG 签名并发布。

<details>
<summary><b>一次性配置</b></summary>

1. 在 [Sonatype Central Portal](https://central.sonatype.com/) 注册并验证邮箱。
2. 申请并验证 `cn.supzx.utils` namespace。该 namespace 需要能证明域名所有权；无法验证时把 `groupId` 换成能证明所有权的坐标，例如 `io.github.<GitHub 用户名>`。
3. 在账号设置中生成 User Token，记下 token username 和 token password。
4. 创建 GPG 签名密钥，公钥上传到公共 key server：

   ```bash
   gpg --full-generate-key
   gpg --list-secret-keys --keyid-format LONG
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```

   用 `pub` 行的主密钥（`[SC]`），不要用下面 `sub` 行的加密子密钥（`[E]`）。完整指纹或末尾 16 位长 ID 都可以。

   导出 ASCII 私钥，稍后粘到 GitHub Secret：

   ```bash
   gpg --armor --export-secret-keys <KEY_ID>
   ```

   Windows 装 Gpg4win 后在 PowerShell 或 Git Bash 里执行；PowerShell 下可以 `gpg --armor --export-secret-keys <KEY_ID> | Set-Clipboard` 直接复制。
   只把公钥传到 key server，私钥只粘到 GitHub Secret，不要提交进仓库。

5. 在仓库 **Settings → Secrets and variables → Actions** 添加：

   | Secret | 内容 |
   |---|---|
   | `MAVEN_CENTRAL_USERNAME` | Central User Token username |
   | `MAVEN_CENTRAL_TOKEN` | Central User Token password |
   | `MAVEN_GPG_PRIVATE_KEY` | `-----BEGIN PGP PRIVATE KEY BLOCK-----` 到结束的完整内容 |
   | `MAVEN_GPG_PASSPHRASE` | GPG 私钥密码 |

6. 确认两个 workflow 已在默认分支，并在 **Settings → Actions → General** 允许 Actions 运行。

</details>

<details>
<summary><b>每次发布</b></summary>

以发布 `2.0.1` 为例：

1. 把所有 `pom.xml` 里的项目版本 `2.0.0` 改成 `2.0.1`：根 POM、子模块 parent 版本、子模块显式版本，以及各模块对 `common-base` 的依赖版本。第三方依赖版本不动。
2. 提交并打同名 tag：

   ```bash
   git add pom.xml common-*/pom.xml
   git commit -m "chore: release 2.0.1"
   git tag v2.0.1
   git push origin master v2.0.1
   ```

3. tag 推上去后 `release.yml` 自动运行：先校验 tag 与所有 POM 版本一致，再用 JDK 21 构建、GPG 签名，发布父 POM 和六个模块。
4. 在 **Actions → Release to Maven Central** 看结果。成功后 Central Portal 一般还要几分钟同步到公共搜索。

版本不一致、namespace 未验证、GPG 公钥未同步或 Secrets 缺失时流水线会直接停下，不会发出半套产物。同一版本发布后不能覆盖，修完请递增版本号重发。

</details>

## 🤝 贡献

欢迎 Issue 和 PR。提 PR 前跑一下 `mvn -DskipTests verify`，代码风格跟着现有模块走。

## 📄 License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
