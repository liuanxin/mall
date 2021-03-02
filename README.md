
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

在 controller 中用 dto 做入参, 用 vo 做出参, 调用 service 的入参和返回使用「跟数据库对应的 model 实体」
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

    // 好的实践是每个接口都有各自的 dto 和 vo, 如果参数不多则不需要构建 dto, 返回只有一个字段也不用新建 vo
    @GetMapping
    public JsonResult<PageReturn<DemoVo>> demoList(DemoDto dto, PageParam page) {
       PageInfo<User> userPageInfo = userService.pageList(dto.userParam(), page);
       List<Product> productList = productService.xxx(dto.productParam());
       return JsonResult.success("xxx", DemoVo.assemblyData(userPageInfo, productList));
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

在 dto 中做基础的入参校验, 并返回 service 中用到的「跟数据库对应的 model 实体」
```java
@Data
public class DemoDto {
    
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

在 vo 中组装数据
```java
@Data
public class DemoVo {
    
    private Long userId;
    private Long productId;
    // ...
    
    public static PageReturn<ExampleVo> assemblyData(PageInfo<User> userPageInfo, List<Product> productList) {
        // 组装数据
        return new DemoVo();
    }
}
```


### 数据库相关的规范

1. 表名全部小写, 以 t_ 开头, 单词间用下划线隔开, 模块要包含在表名中, 如: 用户表 t_user, 用户信息表 t_user_info(一个库也很好区分模块)
2. 表要加上注释, 字符集用 utf8mb4, 使用 innodb 引擎, 如:  comment='xx' engine=InnoDB default charset=utf8mb4;
3. 字段要加上注释, 不允许为 null, 业务上可以为空的字段给定默认值, 如: \`type\` int not null default 0 comment 'xxx'
4. 会用到 text 字段的尽量抽成一个单表
5. 用这几种类型就可以了, 相关的表字段类型对应如下

| java 类型     | 数据库字段类型                                                                                       |
| ------------- | -------------------------------------------------------------------------------------------------- |
| Long          | BIGINT(20)    UNSIGNED NOT NULL DEFAULT '0'                            主键或外键                   |
| Integer、Enum | TINYINT(4)    UNSIGNED NOT NULL DEFAULT '0' COMMENT '1.x, 2.y, 3.z'    无符号的范围在 0~255 之间     |
| Boolean       | TINYINT(1)    UNSIGNED NOT NULL DEFAULT '0' COMMENT '1 表示已删除'      只需要用 true false 表示     |
| String        | VARCHAR(16)   NOT NULL DEFAULT '' COMMENT 'XX'                                                    |
| BigDecimal    | DECIMAL(10,2) NOT NULL DEFAULT '0' COMMENT 'XXXX 金额'                                            |
| Date          | DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'XXXXX 时间' |

注意事项:
1. 金额除了可以用 DECIMAL 外, 还可以用 bigint, 存到最终的单位(比如分,「20.50 元」存成「2050」即可)就行了
2. mysql 中没有 boolean, tinyint(1) 和 tinyint(4) 都是存储 -128 ~ 127 的数(unsigned 则是 0 ~ 255),
   不要在 tinyint(1) 字段上存储 0 1 以外的值, 对应 Java 的 true 和 false,
   如果不止用来描述 true false, 在 mysql 中用 tinyint(4), 在 Java 实体中用 Integer 或者枚举相对应
3. 日期长度默认是 0, 驱动包 >= 5.1.23 时 mysql 会将毫秒四舍五入到秒, 长度设置为 3 可以避免(但是 CURRENT_TIMESTAMP 将无法生效)
   比如 1999-12-31 23:59:59.499 会存成 1999-12-31 23:59:59, 而 1999-12-31 23:59.59.500 却会存为 2000-01-01 00:00:00
   创建时间用: DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
   更新时间用: DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
   另外, 当时间不确定, 业务上可以为空时, 尽量设置为不能为空且给一个默认值 NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT 'xxx 时间'

~
