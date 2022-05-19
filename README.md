
### spring boot

安装 jdk、maven, `java -version` 和 `mvn -v` 有对应的值即可.  
安装 idea 并添加 lombok 和 mybatis 插件(如果使用 eclipse 只安装 lombok 插件就可以了).  
基于 [hosts 文件](document/hosts.md) 修改本机 hosts 文件.

将下面的内容保存成 `settings.xml` 并放到 `${user.home}/.m2`, 想要改变仓库地址, 注释解开并设置即可.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <!-- <localRepository>D:/repository</localRepository> -->

    <mirrors>
        <mirror>
            <id>aliyun</id>
            <name>aliyun maven</name>
            <mirrorOf>central</mirrorOf>
            <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
        </mirror>
    </mirrors>

    <profiles>
        <profile>
            <id>company</id>
            <repositories>
                <repository>
                    <id>aliyun-repository</id>
                    <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </repository>
            </repositories>
            
            <pluginRepositories>
                <pluginRepository>
                    <id>aliyun-plugin</id>
                    <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>company</activeProfile>
    </activeProfiles>
</settings>
```


### 分页示例

在 controller 中用 req 做入参, 用 res 做出参, 调用 service 的入参和返回使用「跟数据库对应的 model 实体」
```java
// 用构造函数来注入 service, 如果下面有用到 @Value 这样的数据就用此注解, 如果没有, 可以用 @AllArgsConstructor 注解
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo")
public class DemoController {

    @Value("${xxx:123}")
    private String xxx;

    private final UserService userService;
    private final ProductService productService;

    // 好的实践是每个接口都有各自的 req 和 res, 如果参数不多则不需要构建 req, 返回只有一个字段也不用新建 res
    @GetMapping
    public JsonResult<PageReturn<DemoRes>> demoList(DemoReq req, PageParam page) {
       PageInfo<User> userPageInfo = userService.pageList(req.userParam(), page);
       List<Product> productList = productService.xxx(req.productParam());
       return JsonResult.success("xxx", DemoRes.assemblyData(userPageInfo, productList));
    }
}
```

service 基于「跟数据库对应的 model 实体」做入参和返回
```java
public interface DemoService {
    /** 获取分页数据 */
    PageReturn<Demo> pageList(Demo param, PageParam page);
}
```

在 req 中做基础的入参校验, 并返回 service 中用到的「跟数据库对应的 model 实体」
```java
@Data
public class DemoReq {
    
    private Long userId;
    private Long productId;

    /** 操作用户模块时用到的参数 */
    public User userParam() {
        // 数据校验
        // ...
        
        // 返回 service 中用到的实体
        return new User(userId);
    }

    /** 操作商品模块时需要的参数 */
    public Product productParam() {
        // ...
    }
}
```

在 res 中组装数据
```java
@Data
public class DemoRes {

    private Long userId;
    private Long productId;
    // ...
    
    public static PageReturn<ExampleRes> assemblyData(PageInfo<User> userPageInfo, List<Product> productList) {
        // 组装数据
        return new DemoRes();
    }
}
```


### 数据库相关的规范

1. 表名全部小写, 以 t_ 开头, 单词间用下划线隔开, 模块要包含在表名中, 如: 用户表 t_user, 用户信息表 t_user_info(一个库也很好区分模块)
2. 表要加上注释, 字符集用 utf8mb4, 使用 innodb 引擎, 如:  comment='xx' engine=InnoDB default charset=utf8mb4;
3. 字段要加上注释, 不允许为 null, 业务上可以为空的字段给定默认值(除了时间字段), 如: \`type\` int not null default 0 comment 'xxx'
4. 会用到 text 字段的尽量抽成一个单表

用这几种类型就可以了, 相关的表字段类型对应如下

| java 类型     | 数据库字段类型                                                                                            |
| ------------- | ------------------------------------------------------------------------------------------------------- |
| Long          | BIGINT        UNSIGNED NOT NULL DEFAULT '0'                                  主键或外键                  |
| Integer       | INT           UNSIGNED NOT NULL DEFAULT '0' COMMENT '购买数量'                                           |
| Integer、Enum | TINYINT(4)    UNSIGNED NOT NULL DEFAULT '0' COMMENT '1.x, 2.y, 3.z'          无符号的范围在 0~255 之间    |
| Boolean       | TINYINT(1)    UNSIGNED NOT NULL DEFAULT '0' COMMENT '1 表示已删除'            只需要用 true false 表示    |
| String        | VARCHAR(16)   NOT NULL DEFAULT '' COMMENT 'XX'                                                         |
| BigDecimal    | DECIMAL(10,2) NOT NULL DEFAULT '0' COMMENT 'XXXX 金额'                                                  |
| Date          | DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMPON UPDATE CURRENT_TIMESTAMP COMMENT 'XXXXX 时间'        |

注意事项:
1. 除日期字段外, 其他字段都设置为 NOT NULL DEFAULT 'xx', 日期字段使用 datetime 类型, 是否为空看具体业务
2. 映射的 enum 值从 1 开始, 0 在 enum 中定义一个 Nil(0) 当默认值(业务上等同于空)并在 from 方法(从其他地方映射时)上返回
3. 数字相关的都使用无符号 unsigned 不要存 < 0 的数, 金额除了可以用 DECIMAL 外, 还可以用 bigint, 存到最终的单位(比如分:「20.50 元」存成「2050」)
4. mysql 中没有 boolean, tinyint(1) 和 tinyint(4) 都是存储 -128 ~ 127 的数(unsigned 则是 0 ~ 255),  
   不要在 tinyint(1) 字段上存储 0 1 以外的值, 对应 Java 的 true 和 false,  
   如果不止用来描述 true false, 在 mysql 中用 tinyint(4), 在 Java 实体中用 Integer 或者枚举相对应
5. 日期长度默认是 0, 不会存储毫秒微秒, 驱动包 >= 5.1.23 时 mysql 会将毫秒四舍五入到秒  
   比如 1999-12-31 23:59:59.499 会存成 1999-12-31 23:59:59, 而 1999-12-31 23:59.59.500 却会存为 2000-01-01 00:00:00  
   将长度设置为 3 可以存储到毫秒, 6 可以存储到微妙, 比如  
     创建时间: DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),  
     更新时间: DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)  
   另外, 不要使用 not null default '0000-00-00 00:00:00' 这种做法, 这是一个无效日期,  
   在 mysql 8 中需要开启 `SET SQL_MODE='ALLOW_INVALID_DATES'` 才能写入, 此时这个字段允许 null 就好了(**NULL 并不会导致索引失效**)


### 推镜像到 docker 服务器

> 项目使用 [google-jib](https://github.com/GoogleContainerTools/jib) 进行打包构建,
> 可以使用 `mvn clean compile -DsendCredentialsOverHttp=true jib:build` 命令推到 docker 服务器,
> 默认的 url 地址是 `/项目包/项目名:版本`, 如果要自定义可以用命令行参数的方式, 建议用下面的 bash 脚本

```bash
# 用「/所在分支/项目名:时间:最新的 commit-hash」做为 docker 服务器的 url 地址
now="$(date '+%Y-%m-%d-%H-%M-%S')"
branch="$(git rev-parse --abbrev-ref HEAD)"
latest_commit="$(git log --format=format:'%H' | head -n 1)"
docker_image="ip:port/${branch}/项目名:${now}:${latest_commit}"
docker_need_pass="0"
docker_user="xx"
docker_pass="xxx"

if [ "${docker_need_pass}" == "1" ]; then
    mvn clean compile -DsendCredentialsOverHttp=true jib:build -Djib.to.image=${docker_image} \
        -Djib.to.auth.username=${docker_user} -Djib.to.auth.password=${docker_pass}
else
    mvn clean compile -DsendCredentialsOverHttp=true jib:build -Djib.to.image=${docker_image}
fi
```

~
